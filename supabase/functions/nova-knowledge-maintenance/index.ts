import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";
import {
  adaptDeviceCatalogRow,
  adaptGuardianIncidentRow,
  adaptGuardianLearningRow,
  adaptOperationalRows,
  adaptSupportTicketRow,
} from "../nova-knowledge/internal_adapters.mjs";
import { createInternalIngestionEngine } from "../nova-knowledge/internal_ingestion.mjs";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "";
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
const SCHEDULER_SECRET = Deno.env.get("MORLEY_BACKUP_SECRET") || "";
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false, autoRefreshToken: false } });
const ingestion = createInternalIngestionEngine({ admin });
const MAX_EMBEDDING_BATCH = 4;
const MAX_INGEST_BATCH = 4;
const SENSITIVE_DIAGNOSTIC = /(authorization|cookie|password|secret|token|api[_-]?key)\s*[:=]\s*([^\s,;]+)/gi;

type MaintenanceAuthReason = "missing_config" | "missing_header" | "env_match" | "rpc_match" | "rpc_error" | "mismatch";
type MaintenanceAuthResult = { ok: boolean; reason: MaintenanceAuthReason };

const clean = (value: unknown, max = 500) => String(value ?? "").trim().replace(/\s+/g, " ").slice(0, max);
const bounded = (value: unknown, fallback: number, max: number) => Math.min(Math.max(Number(value) || fallback, 1), max);

function normalizeMaintenanceError(error: unknown, max = 400) {
  const redact = (value: unknown) => clean(value, max).replace(SENSITIVE_DIAGNOSTIC, "$1=[redacted]");
  if (error instanceof Error) return redact(error.message) || error.name || "Maintenance error";
  if (error && typeof error === "object") {
    const record = error as Record<string, unknown>;
    const parts = ["code", "message", "details", "hint"]
      .map((key) => record[key] == null ? "" : `${key}=${redact(record[key])}`)
      .filter(Boolean);
    return redact(parts.join(" | ")) || "Structured maintenance error";
  }
  return redact(error) || "Unknown maintenance error";
}

function isOptionalSourcePermissionError(error: unknown) {
  if (!error || typeof error !== "object") return false;
  const record = error as Record<string, unknown>;
  const code = clean(record.code, 40);
  const message = clean(record.message, 240).toLowerCase();
  return code === "42501" || message.includes("permission denied");
}

function embeddingErrorCode(error: unknown) {
  const message = normalizeMaintenanceError(error, 180).toLowerCase();
  if (message.includes("dimension")) return "INVALID_DIMENSIONS";
  if (message.includes("timeout")) return "EMBED_TIMEOUT";
  if (message.includes("429") || message.includes("rate")) return "EMBED_RATE_LIMIT";
  return "EMBED_FAILED";
}

function embeddingRetryAt(attempt: number) {
  const minutes = Math.min(24 * 60, 5 * (2 ** Math.max(0, Math.min(attempt - 1, 8))));
  return new Date(Date.now() + minutes * 60_000).toISOString();
}

function reply(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
      "Cache-Control": "no-store",
      "X-Content-Type-Options": "nosniff",
    },
  });
}

function constantTimeEqual(left: string, right: string) {
  if (!left || !right || left.length !== right.length) return false;
  let diff = 0;
  for (let i = 0; i < left.length; i += 1) diff |= left.charCodeAt(i) ^ right.charCodeAt(i);
  return diff === 0;
}

async function authorized(req: Request): Promise<MaintenanceAuthResult> {
  if (!SUPABASE_URL || !SERVICE_ROLE) return { ok: false, reason: "missing_config" };
  const supplied = clean(req.headers.get("x-maintenance-secret"), 512);
  if (!supplied) return { ok: false, reason: "missing_header" };
  if (SCHEDULER_SECRET && constantTimeEqual(supplied, SCHEDULER_SECRET)) {
    return { ok: true, reason: "env_match" };
  }
  try {
    const { data, error } = await admin.rpc("morley_backup_scheduler_secret_matches", { candidate: supplied });
    if (error) return { ok: false, reason: "rpc_error" };
    if (data === true) return { ok: true, reason: "rpc_match" };
    return { ok: false, reason: "mismatch" };
  } catch {
    return { ok: false, reason: "rpc_error" };
  }
}

async function beginRun() {
  const { data, error } = await admin.from("nova_knowledge_maintenance_runs")
    .insert({ status: "running" })
    .select("id")
    .single();
  if (error) throw error;
  return data.id as string;
}

async function finishRun(
  runId: string,
  status: "completed" | "partial" | "failed",
  stats: Record<string, unknown>,
  errorSummary: string | null = null,
) {
  const { error } = await admin.from("nova_knowledge_maintenance_runs").update({
    status,
    embedded_ready: Number(stats.embedded_ready || 0),
    embedded_error: Number(stats.embedded_error || 0),
    ingested_created: Number(stats.ingested_created || 0),
    ingested_updated: Number(stats.ingested_updated || 0),
    ingested_skipped: Number(stats.ingested_skipped || 0),
    ingested_error: Number(stats.ingested_error || 0),
    error_summary: errorSummary,
    completed_at: new Date().toISOString(),
  }).eq("id", runId);
  if (error) throw error;
}

async function fetchRows(
  table: string,
  columns: string,
  limit: number,
  configure?: (query: any) => any,
  options: { optional?: boolean } = {},
) {
  let query: any = admin.from(table).select(columns).limit(limit);
  if (configure) query = configure(query);
  const { data, error } = await query;
  if (error) {
    if (options.optional && isOptionalSourcePermissionError(error)) {
      console.warn(`[nova-knowledge-maintenance] optional source unavailable: ${table}`);
      return [];
    }
    throw error;
  }
  return data || [];
}

async function safeDocuments(limit: number) {
  const documents: Array<{ adapter: string; sourceIdentity: string; document: any }> = [];
  const perSource = Math.max(1, Math.floor(limit / 4));

  const devices = await fetchRows(
    "device_catalog",
    "id,category,brand,family,model_name,model_number,release_year,release_date,ram_options,storage_options,key_specs,aliases,source_url,source_name,source_checked_at,active,market_region,sim_configuration,physical_sim_slots,esim_supported,dual_sim_supported,created_at,updated_at",
    perSource,
    (query) => query.eq("active", true).order("updated_at", { ascending: false }),
  );
  for (const row of devices) documents.push({ adapter: "device_catalog", sourceIdentity: String(row.id), document: adaptDeviceCatalogRow(row) });

  const incidents = await fetchRows(
    "guardian_incidents",
    "id,source,state,risk_level,classification,confidence,diagnosis_summary,proposed_action,auto_fix_eligible,requires_approval,attempt_count,github_branch,github_pr_number,last_error_code,applied_at,verified_at,created_at,updated_at,worker_version,reproduction_summary,test_plan,resolution_summary,occurrence_count,first_seen_at,last_seen_at,app_version,route,diagnostic_kind,diagnostic_message",
    perSource,
    (query) => query.order("updated_at", { ascending: false }),
  );
  for (const row of incidents) documents.push({ adapter: "guardian_incidents", sourceIdentity: String(row.id), document: adaptGuardianIncidentRow(row) });

  const lessons = await fetchRows(
    "nova_learning_experiences",
    "id,domain,lesson_key,lesson_type,summary,source_type,evidence,outcome,confidence,verified,active,observed_at,created_at,updated_at",
    perSource,
    (query) => query.eq("active", true).order("updated_at", { ascending: false }),
  );
  for (const row of lessons) documents.push({ adapter: "nova_learning_experiences", sourceIdentity: String(row.id), document: adaptGuardianLearningRow(row) });

  const tickets = await fetchRows(
    "support_tickets",
    "id,category,status,priority,app_version,app_version_code,device_model,android_version,created_at,updated_at,resolved_at,closed_at",
    perSource,
    (query) => query.order("updated_at", { ascending: false }),
  );
  for (const row of tickets) documents.push({ adapter: "support_tickets", sourceIdentity: String(row.id), document: adaptSupportTicketRow(row) });

  const inventory = await fetchRows(
    "inventory_items",
    "status,acquired_price,expected_sale_price,acquired_at,listed_at,retired_at,created_at,updated_at",
    Math.min(limit * 5, 100),
    (query) => query.order("updated_at", { ascending: false }),
    { optional: true },
  );
  for (const document of adaptOperationalRows("inventory", inventory)) documents.push({ adapter: "inventory_aggregate", sourceIdentity: "latest", document });

  const sales = await fetchRows(
    "sales_records",
    "acquired_cost,sold_price,fees,other_costs,realised_profit,sales_channel,sold_at,created_at",
    Math.min(limit * 5, 100),
    (query) => query.order("sold_at", { ascending: false }),
    { optional: true },
  );
  for (const document of adaptOperationalRows("sales", sales)) documents.push({ adapter: "sales_aggregate", sourceIdentity: "latest", document });

  const history = await fetchRows(
    "valuation_history",
    "asking_price,market_value,max_buy,expected_profit,status,bought_price,sold_price,actual_profit,created_at,updated_at",
    Math.min(limit * 5, 100),
    (query) => query.order("updated_at", { ascending: false }),
  );
  const quotes = await fetchRows(
    "valuation_quotes",
    "proposed_buy_cents,target_resale_cents,expected_margin_cents,confidence,recommendation,created_at",
    Math.min(limit * 5, 100),
    (query) => query.order("created_at", { ascending: false }),
    { optional: true },
  );
  for (const document of adaptOperationalRows("valuation", [...history, ...quotes])) documents.push({ adapter: "valuation_aggregate", sourceIdentity: "latest", document });

  return documents.slice(0, limit);
}

async function ingestInternal(limit: number, stats: any) {
  const documents = await safeDocuments(limit);
  for (const entry of documents) {
    try {
      const result = await ingestion.persistDocument(entry, { actorId: null });
      if (result === "created") stats.ingested_created += 1;
      else if (result === "updated") stats.ingested_updated += 1;
      else stats.ingested_skipped += 1;
    } catch (error) {
      stats.ingested_error += 1;
      console.error("[nova-knowledge-maintenance] ingestion item failed", normalizeMaintenanceError(error, 240));
    }
  }
}

async function embedPending(limit: number, stats: any) {
  const boundedLimit = Math.min(limit, MAX_EMBEDDING_BATCH);
  const { data, error } = await admin.rpc("nova_claim_embedding_chunks", { p_limit: boundedLimit });
  if (error) throw error;
  const rows = Array.isArray(data) ? data.slice(0, MAX_EMBEDDING_BATCH) : [];
  const model = new Supabase.ai.Session("gte-small");

  for (const chunk of rows) {
    try {
      const result = await model.run(String(chunk.content || ""), { mean_pool: true, normalize: true });
      const vector = Array.from(result as ArrayLike<number>);
      if (vector.length !== 384) throw new Error(`Unexpected embedding dimensions: ${vector.length}`);
      const { error: updateError } = await admin.from("nova_knowledge_chunks").update({
        embedding: vector,
        embedding_provider: "supabase-ai",
        embedding_model: "gte-small",
        embedding_dimensions: 384,
        embedding_status: "ready",
        embedding_error: null,
        embedding_last_error_code: null,
        embedding_next_retry_at: null,
        embedded_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      }).eq("id", chunk.id).eq("status", "active");
      if (updateError) throw updateError;
      stats.embedded_ready += 1;
    } catch (error) {
      const message = normalizeMaintenanceError(error, 400);
      const attempt = Number(chunk.embedding_attempt_count || 1);
      const { error: updateError } = await admin.from("nova_knowledge_chunks").update({
        embedding_status: "error",
        embedding_error: message,
        embedding_last_error_code: embeddingErrorCode(error),
        embedding_next_retry_at: embeddingRetryAt(attempt),
        updated_at: new Date().toISOString(),
      }).eq("id", chunk.id).eq("status", "active");
      if (updateError) console.error("[nova-knowledge-maintenance] failed to persist embedding error state", normalizeMaintenanceError(updateError, 240));
      stats.embedded_error += 1;
      console.error("[nova-knowledge-maintenance] embedding failed", message);
    }
  }
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return reply({ error: "POST required" }, 405);
  const auth = await authorized(req);
  if (!auth.ok) {
    console.warn("[nova-knowledge-maintenance] authorization rejected", { auth_reason: auth.reason });
    return reply({ error: "Maintenance authorization required" }, 401);
  }

  let body: any = {};
  try { body = await req.json(); } catch { return reply({ error: "Invalid JSON request" }, 400); }
  if (clean(body.action, 40).toLowerCase() !== "run") return reply({ error: "Unsupported action" }, 400);

  const embeddingLimit = bounded(body.embedding_limit, 4, MAX_EMBEDDING_BATCH);
  const ingestLimit = bounded(body.ingest_limit, 4, MAX_INGEST_BATCH);
  const started = Date.now();
  const stats = {
    embedded_ready: 0,
    embedded_error: 0,
    ingested_created: 0,
    ingested_updated: 0,
    ingested_skipped: 0,
    ingested_error: 0,
  };
  let runId: string | null = null;

  try {
    runId = await beginRun();
    await ingestInternal(ingestLimit, stats);
    await embedPending(embeddingLimit, stats);
    const partial = stats.embedded_error > 0 || stats.ingested_error > 0;
    await finishRun(runId, partial ? "partial" : "completed", stats);
    return reply({
      ok: true,
      embedded_ready: stats.embedded_ready,
      embedded_error: stats.embedded_error,
      ingested_created: stats.ingested_created,
      ingested_updated: stats.ingested_updated,
      ingested_skipped: stats.ingested_skipped,
      duration_ms: Date.now() - started,
    });
  } catch (error) {
    const message = normalizeMaintenanceError(error, 400);
    if (runId) {
      try { await finishRun(runId, "failed", stats, message); } catch { /* preserve original error */ }
    }
    console.error("[nova-knowledge-maintenance] run failed", message);
    return reply({ error: "Nova knowledge maintenance failed safely", duration_ms: Date.now() - started }, 500);
  }
});
