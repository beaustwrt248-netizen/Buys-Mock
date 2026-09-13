import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";
import {
  chunkKnowledgeDocument,
  normalizeKnowledgeDocument,
  sanitizeMetadata,
  sha256Hex,
} from "../nova-knowledge/knowledge_core.mjs";
import {
  adaptDeviceCatalogRow,
  adaptGuardianIncidentRow,
  adaptGuardianLearningRow,
  adaptOperationalRows,
  adaptSupportTicketRow,
} from "../nova-knowledge/internal_adapters.mjs";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "";
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
const MAINTENANCE_SECRET = Deno.env.get("NOVA_KNOWLEDGE_MAINTENANCE_SECRET") || "";
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false, autoRefreshToken: false } });
const MAX_EMBEDDING_BATCH = 20;
const MAX_INGEST_BATCH = 20;
const RETRY_COOLDOWN_MS = 15 * 60 * 1000;
const F = "id,category,title,content,source_type,source_label,source_filename,mime_type,version_label,trust_level,status,content_hash,revision,metadata,created_by,updated_by,created_at,updated_at";

const clean = (value: unknown, max = 500) => String(value ?? "").trim().replace(/\s+/g, " ").slice(0, max);
const bounded = (value: unknown, fallback: number, max: number) => Math.min(Math.max(Number(value) || fallback, 1), max);
const authorityFor = (trust: string) => trust === "verified" ? 1 : trust === "reviewed" ? 0.82 : 0.65;
const validIso = (value: unknown) => {
  const raw = clean(value, 80);
  if (!raw) return null;
  const parsed = Date.parse(raw);
  return Number.isFinite(parsed) ? new Date(parsed).toISOString() : null;
};

function reply(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json", "Cache-Control": "no-store", "X-Content-Type-Options": "nosniff" },
  });
}

function authorized(req: Request) {
  if (!SUPABASE_URL || !SERVICE_ROLE || !MAINTENANCE_SECRET) return false;
  const header = req.headers.get("Authorization") || "";
  const supplied = header.replace(/^Bearer\s+/i, "");
  if (!supplied || supplied.length !== MAINTENANCE_SECRET.length) return false;
  let diff = 0;
  for (let i = 0; i < supplied.length; i += 1) diff |= supplied.charCodeAt(i) ^ MAINTENANCE_SECRET.charCodeAt(i);
  return diff === 0;
}

async function beginRun() {
  const { data, error } = await admin.from("nova_knowledge_maintenance_runs").insert({ status: "running" }).select("id").single();
  if (error) throw error;
  return data.id as string;
}

async function finishRun(runId: string, status: "completed" | "partial" | "failed", stats: Record<string, unknown>, errorSummary: string | null = null) {
  const { error } = await admin.from("nova_knowledge_maintenance_runs").update({
    status,
    embedded_ready: Number(stats.embedded_ready || 0),
    embedded_failed: Number(stats.embedded_failed || 0),
    ingested_created: Number(stats.ingested_created || 0),
    ingested_updated: Number(stats.ingested_updated || 0),
    ingested_skipped: Number(stats.ingested_skipped || 0),
    error_summary: errorSummary,
    completed_at: new Date().toISOString(),
  }).eq("id", runId);
  if (error) throw error;
}

async function fetchRows(table: string, columns: string, limit: number, configure?: (query: any) => any) {
  let query: any = admin.from(table).select(columns).limit(limit);
  if (configure) query = configure(query);
  const { data, error } = await query;
  if (error) throw error;
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

  const inventory = await fetchRows("inventory_items", "status,acquired_price,expected_sale_price,acquired_at,listed_at,retired_at,created_at,updated_at", Math.min(limit * 5, 100), (query) => query.order("updated_at", { ascending: false }));
  for (const document of adaptOperationalRows("inventory", inventory)) documents.push({ adapter: "inventory_aggregate", sourceIdentity: "latest", document });
  const sales = await fetchRows("sales_records", "acquired_cost,sold_price,fees,other_costs,realised_profit,sales_channel,sold_at,created_at", Math.min(limit * 5, 100), (query) => query.order("sold_at", { ascending: false }));
  for (const document of adaptOperationalRows("sales", sales)) documents.push({ adapter: "sales_aggregate", sourceIdentity: "latest", document });
  const history = await fetchRows("valuation_history", "asking_price,market_value,max_buy,expected_profit,status,bought_price,sold_price,actual_profit,created_at,updated_at", Math.min(limit * 5, 100), (query) => query.order("updated_at", { ascending: false }));
  const quotes = await fetchRows("valuation_quotes", "proposed_buy_cents,target_resale_cents,expected_margin_cents,confidence,recommendation,created_at", Math.min(limit * 5, 100), (query) => query.order("created_at", { ascending: false }));
  for (const document of adaptOperationalRows("valuation", [...history, ...quotes])) documents.push({ adapter: "valuation_aggregate", sourceIdentity: "latest", document });

  return documents.slice(0, limit);
}

async function upsertSourceAndChunks(row: any, normalized: any, adapterKey: string) {
  const metadata = normalized.metadata || {};
  const observedAt = validIso(metadata.observed_at) || row.updated_at || new Date().toISOString();
  const staleAfter = validIso(metadata.stale_after);
  const confidence = Math.min(Math.max(Number(metadata.confidence) || 0.7, 0), 1);
  const sourcePayload = {
    source_key: `maintenance:${adapterKey}`,
    domain: clean(metadata.domain, 80).toLowerCase() || normalized.category,
    source_type: "import",
    source_label: normalized.source_label,
    source_uri: clean(metadata.source_uri, 1000) || null,
    trust_level: normalized.trust_level,
    authority_score: authorityFor(normalized.trust_level),
    content_hash: normalized.content_hash,
    status: "active",
    observed_at: observedAt,
    retrieved_at: new Date().toISOString(),
    stale_after: staleAfter,
    metadata: sanitizeMetadata(metadata),
    updated_at: new Date().toISOString(),
  };
  const { data: source, error: sourceError } = await admin.from("nova_knowledge_sources").upsert(sourcePayload, { onConflict: "source_key" }).select("id").single();
  if (sourceError) throw sourceError;

  const chunks = await chunkKnowledgeDocument(normalized);
  const { error: supersedeError } = await admin.from("nova_knowledge_chunks").update({ status: "superseded", updated_at: new Date().toISOString() }).eq("knowledge_id", row.id).eq("status", "active");
  if (supersedeError) throw supersedeError;
  if (!chunks.length) return;
  const rows = chunks.map((chunk: any) => ({
    knowledge_id: row.id,
    source_id: source.id,
    knowledge_revision: Number(row.revision || 1),
    chunk_index: chunk.chunk_index,
    content: chunk.content,
    content_hash: chunk.content_hash,
    char_start: chunk.char_start,
    char_end: chunk.char_end,
    token_estimate: chunk.token_estimate,
    trust_level: normalized.trust_level,
    confidence,
    observed_at: observedAt,
    stale_after: staleAfter,
    status: "active",
    embedding_status: "pending",
    embedding_error: null,
    metadata: { domain: sourcePayload.domain, adapter_key: adapterKey },
    updated_at: new Date().toISOString(),
  }));
  const { error } = await admin.from("nova_knowledge_chunks").upsert(rows, { onConflict: "knowledge_id,knowledge_revision,chunk_index" });
  if (error) throw error;
}

async function persistDocument(entry: { adapter: string; sourceIdentity: string; document: any }) {
  const adapterKey = await sha256Hex(`${entry.adapter}:${entry.sourceIdentity}`);
  const normalized = await normalizeKnowledgeDocument({
    ...entry.document,
    metadata: { ...(entry.document.metadata || {}), managed_by: "nova_maintenance", adapter: entry.adapter, adapter_key: adapterKey },
  });
  if (!normalized.title || !normalized.content) return "skipped" as const;
  const { data: existing, error: findError } = await admin.from("nova_knowledge_items").select(F).eq("source_type", "import").contains("metadata", { managed_by: "nova_maintenance", adapter_key: adapterKey }).limit(1).maybeSingle();
  if (findError) throw findError;
  const now = new Date().toISOString();
  if (existing && existing.content_hash === normalized.content_hash && existing.status === "active") return "skipped" as const;

  if (existing) {
    const { data: updated, error } = await admin.from("nova_knowledge_items").update({
      category: normalized.category,
      title: normalized.title,
      content: normalized.content,
      source_label: normalized.source_label,
      trust_level: normalized.trust_level,
      content_hash: normalized.content_hash,
      revision: Number(existing.revision || 1) + 1,
      metadata: normalized.metadata,
      updated_at: now,
    }).eq("id", existing.id).select(F).single();
    if (error) throw error;
    await upsertSourceAndChunks(updated, normalized, adapterKey);
    return "updated" as const;
  }

  const { data: created, error } = await admin.from("nova_knowledge_items").insert({
    category: normalized.category,
    title: normalized.title,
    content: normalized.content,
    source_type: "import",
    source_label: normalized.source_label,
    trust_level: normalized.trust_level,
    status: "active",
    content_hash: normalized.content_hash,
    revision: 1,
    metadata: normalized.metadata,
    created_at: now,
    updated_at: now,
  }).select(F).single();
  if (error) throw error;
  await upsertSourceAndChunks(created, normalized, adapterKey);
  return "created" as const;
}

async function ingestInternal(limit: number, stats: any) {
  const documents = await safeDocuments(limit);
  for (const entry of documents) {
    try {
      const result = await persistDocument(entry);
      if (result === "created") stats.ingested_created += 1;
      else if (result === "updated") stats.ingested_updated += 1;
      else stats.ingested_skipped += 1;
    } catch (error) {
      stats.ingested_failed += 1;
      console.error("[nova-knowledge-maintenance] ingestion item failed", clean(error instanceof Error ? error.message : error, 240));
    }
  }
}

async function embedPending(limit: number, stats: any) {
  const retryBefore = new Date(Date.now() - RETRY_COOLDOWN_MS).toISOString();
  const { data, error } = await admin.from("nova_knowledge_chunks")
    .select("id,content,embedding_status,updated_at")
    .eq("status", "active")
    .or(`embedding_status.eq.pending,and(embedding_status.eq.failed,updated_at.lt.${retryBefore})`)
    .order("updated_at", { ascending: true })
    .limit(Math.min(limit, MAX_EMBEDDING_BATCH));
  if (error) throw error;

  const model = new Supabase.ai.Session("gte-small");
  for (const chunk of data || []) {
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
        embedded_at: new Date().toISOString(),
        updated_at: new Date().toISOString(),
      }).eq("id", chunk.id);
      if (updateError) throw updateError;
      stats.embedded_ready += 1;
    } catch (error) {
      const message = clean(error instanceof Error ? error.message : error, 400);
      await admin.from("nova_knowledge_chunks").update({
        embedding_status: "failed",
        embedding_error: message,
        updated_at: new Date().toISOString(),
      }).eq("id", chunk.id);
      stats.embedded_failed += 1;
      console.error("[nova-knowledge-maintenance] embedding failed", message);
    }
  }
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return reply({ error: "POST required" }, 405);
  if (!authorized(req)) return reply({ error: "Maintenance authorization required" }, 401);

  let body: any = {};
  try { body = await req.json(); } catch { return reply({ error: "Invalid JSON request" }, 400); }
  if (clean(body.action, 40).toLowerCase() !== "run") return reply({ error: "Unsupported action" }, 400);

  const embeddingLimit = Math.min(bounded(body.embedding_limit, 20, MAX_EMBEDDING_BATCH), 20);
  const ingestLimit = bounded(body.ingest_limit, 12, MAX_INGEST_BATCH);
  const started = Date.now();
  const stats = {
    embedded_ready: 0,
    embedded_failed: 0,
    ingested_created: 0,
    ingested_updated: 0,
    ingested_skipped: 0,
    ingested_failed: 0,
  };
  let runId: string | null = null;

  try {
    runId = await beginRun();
    await ingestInternal(ingestLimit, stats);
    await embedPending(embeddingLimit, stats);
    const partial = stats.embedded_failed > 0 || stats.ingested_failed > 0;
    await finishRun(runId, partial ? "partial" : "completed", stats);
    return reply({
      ok: true,
      embedded_ready: stats.embedded_ready,
      embedded_failed: stats.embedded_failed,
      ingested_created: stats.ingested_created,
      ingested_updated: stats.ingested_updated,
      ingested_skipped: stats.ingested_skipped,
      duration_ms: Date.now() - started,
    });
  } catch (error) {
    const message = clean(error instanceof Error ? error.message : error, 400);
    if (runId) {
      try { await finishRun(runId, "failed", stats, message); } catch { /* preserve original error */ }
    }
    console.error("[nova-knowledge-maintenance] run failed", message);
    return reply({ error: "Nova knowledge maintenance failed safely", duration_ms: Date.now() - started }, 500);
  }
});
