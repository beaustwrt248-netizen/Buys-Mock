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
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false, autoRefreshToken: false } });
const ingestion = createInternalIngestionEngine({ admin });
const ORIGINS = new Set(["https://buyshub.me", "https://www.buyshub.me", "https://beaustwrt248-netizen.github.io"]);
const ADAPTERS = new Set(["catalogue", "guardian", "support", "inventory", "sales", "valuation", "all"]);

const clean = (value: unknown, max = 220) => String(value ?? "").trim().replace(/\s+/g, " ").slice(0, max);
const bounded = (value: unknown, fallback: number, max: number) => Math.min(Math.max(Number(value) || fallback, 1), max);

function headers(req: Request) {
  const origin = req.headers.get("Origin") || "";
  return {
    "Content-Type": "application/json",
    "Access-Control-Allow-Origin": ORIGINS.has(origin) ? origin : "https://buyshub.me",
    "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
    "Access-Control-Allow-Methods": "POST, OPTIONS",
    "Cache-Control": "no-store",
    "Vary": "Origin",
    "X-Content-Type-Options": "nosniff",
  };
}

async function auth(req: Request) {
  if (!SUPABASE_URL || !SERVICE_ROLE) return { error: "Nova ingestion backend configuration unavailable", status: 503 } as const;
  const token = (req.headers.get("Authorization") || "").replace(/^Bearer\s+/i, "");
  if (!token) return { error: "Authentication required", status: 401 } as const;
  const { data: { user }, error } = await admin.auth.getUser(token);
  if (error || !user) return { error: "Invalid session", status: 401 } as const;
  const { data: p, error: profileError } = await admin.from("profiles").select("role,is_enabled").eq("id", user.id).maybeSingle();
  if (profileError) throw profileError;
  if (!p?.is_enabled || p.role !== "admin") return { error: "Admin access required", status: 403 } as const;
  return { user } as const;
}

async function beginRun(adapter: string, userId: string, offset: number, limit: number) {
  const { data, error } = await admin.from("nova_knowledge_ingestion_runs").insert({
    adapter,
    domain: adapter === "all" ? "multi" : adapter,
    status: "running",
    checkpoint: { offset, limit },
    created_by: userId,
    metadata: { bounded: true, source: "nova-knowledge-ingest" },
  }).select("id").single();
  if (error) throw error;
  return data.id as string;
}

async function finishRun(runId: string, status: "completed" | "partial" | "failed", stats: any, errorSummary: string | null = null) {
  const { error } = await admin.from("nova_knowledge_ingestion_runs").update({
    status,
    scanned_count: stats.scanned_count,
    created_count: stats.created_count,
    updated_count: stats.updated_count,
    skipped_count: stats.skipped_count,
    error_count: stats.error_count,
    error_summary: errorSummary,
    checkpoint: { ...stats.checkpoint, adopted_count: stats.adopted_count },
    completed_at: new Date().toISOString(),
  }).eq("id", runId);
  if (error) throw error;
}

async function fetchRows(table: string, columns: string, limit: number, offset: number, configure?: (query: any) => any) {
  let query: any = admin.from(table).select(columns).range(offset, offset + limit - 1);
  if (configure) query = configure(query);
  const { data, error } = await query;
  if (error) throw error;
  return data || [];
}

async function fetchDocuments(adapter: string, limit: number, offset: number) {
  const documents: Array<{ adapter: string; sourceIdentity: string; document: any }> = [];

  if (adapter === "catalogue" || adapter === "all") {
    const rows = await fetchRows(
      "device_catalog",
      "id,category,brand,family,model_name,model_number,release_year,release_date,ram_options,storage_options,key_specs,aliases,source_url,source_name,source_checked_at,active,market_region,sim_configuration,physical_sim_slots,esim_supported,dual_sim_supported,created_at,updated_at",
      limit,
      offset,
      (query) => query.eq("active", true).order("id", { ascending: true }),
    );
    for (const row of rows) documents.push({ adapter: "device_catalog", sourceIdentity: String(row.id), document: adaptDeviceCatalogRow(row) });
  }

  if (adapter === "guardian" || adapter === "all") {
    const incidents = await fetchRows(
      "guardian_incidents",
      "id,source,state,risk_level,classification,confidence,diagnosis_summary,proposed_action,auto_fix_eligible,requires_approval,attempt_count,github_branch,github_pr_number,last_error_code,applied_at,verified_at,created_at,updated_at,worker_version,reproduction_summary,test_plan,resolution_summary,occurrence_count,first_seen_at,last_seen_at,app_version,route,diagnostic_kind,diagnostic_message",
      limit,
      offset,
      (query) => query.order("updated_at", { ascending: false }),
    );
    for (const row of incidents) documents.push({ adapter: "guardian_incidents", sourceIdentity: String(row.id), document: adaptGuardianIncidentRow(row) });

    const lessons = await fetchRows(
      "nova_learning_experiences",
      "id,domain,lesson_key,lesson_type,summary,source_type,evidence,outcome,confidence,verified,active,observed_at,created_at,updated_at",
      limit,
      offset,
      (query) => query.eq("active", true).order("updated_at", { ascending: false }),
    );
    for (const row of lessons) documents.push({ adapter: "nova_learning_experiences", sourceIdentity: String(row.id), document: adaptGuardianLearningRow(row) });
  }

  if (adapter === "support" || adapter === "all") {
    const tickets = await fetchRows(
      "support_tickets",
      "id,category,status,priority,app_version,app_version_code,device_model,android_version,created_at,updated_at,resolved_at,closed_at",
      limit,
      offset,
      (query) => query.order("updated_at", { ascending: false }),
    );
    for (const row of tickets) documents.push({ adapter: "support_tickets", sourceIdentity: String(row.id), document: adaptSupportTicketRow(row) });
  }

  if (adapter === "inventory" || adapter === "all") {
    const rows = await fetchRows(
      "inventory_items",
      "status,acquired_price,expected_sale_price,acquired_at,listed_at,retired_at,created_at,updated_at",
      Math.min(limit * 10, 500),
      offset,
      (query) => query.order("updated_at", { ascending: false }),
    );
    for (const document of adaptOperationalRows("inventory", rows)) documents.push({ adapter: "inventory_aggregate", sourceIdentity: `window:${offset}`, document });
  }

  if (adapter === "sales" || adapter === "all") {
    const rows = await fetchRows(
      "sales_records",
      "acquired_cost,sold_price,fees,other_costs,realised_profit,sales_channel,sold_at,created_at",
      Math.min(limit * 10, 500),
      offset,
      (query) => query.order("sold_at", { ascending: false }),
    );
    for (const document of adaptOperationalRows("sales", rows)) documents.push({ adapter: "sales_aggregate", sourceIdentity: `window:${offset}`, document });
  }

  if (adapter === "valuation" || adapter === "all") {
    const history = await fetchRows(
      "valuation_history",
      "asking_price,market_value,max_buy,expected_profit,status,bought_price,sold_price,actual_profit,created_at,updated_at",
      Math.min(limit * 10, 500),
      offset,
      (query) => query.order("updated_at", { ascending: false }),
    );
    const quotes = await fetchRows(
      "valuation_quotes",
      "proposed_buy_cents,target_resale_cents,expected_margin_cents,confidence,recommendation,created_at",
      Math.min(limit * 10, 500),
      offset,
      (query) => query.order("created_at", { ascending: false }),
    );
    const rows = [...history, ...quotes];
    for (const document of adaptOperationalRows("valuation", rows)) documents.push({ adapter: "valuation_aggregate", sourceIdentity: `window:${offset}`, document });
  }

  return documents;
}

Deno.serve(async (req: Request) => {
  const h = headers(req);
  const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: h });
  if (req.method === "OPTIONS") return new Response("ok", { headers: h });
  if (req.method !== "POST") return reply({ error: "POST required" }, 405);

  const authorized = await auth(req);
  if ("error" in authorized) return reply({ error: authorized.error }, authorized.status);

  let body: any = {};
  try { body = await req.json(); } catch { return reply({ error: "Invalid JSON request" }, 400); }
  if (clean(body.action, 40).toLowerCase() !== "ingest_internal") return reply({ error: "Unsupported action" }, 400);
  const adapter = clean(body.adapter, 40).toLowerCase() || "all";
  if (!ADAPTERS.has(adapter)) return reply({ error: "Invalid ingestion adapter" }, 400);
  const limit = bounded(body.limit, 20, 50);
  const offset = Math.max(Number(body.offset) || 0, 0);

  let runId: string | null = null;
  const stats = {
    scanned_count: 0,
    created_count: 0,
    updated_count: 0,
    skipped_count: 0,
    adopted_count: 0,
    error_count: 0,
    checkpoint: { offset, limit, next_offset: null as number | null },
  };
  try {
    runId = await beginRun(adapter, authorized.user.id, offset, limit);
    const documents = await fetchDocuments(adapter, limit, offset);
    stats.scanned_count = documents.length;
    for (const entry of documents) {
      try {
        const outcome = await ingestion.persistDocument(entry, { actorId: authorized.user.id });
        if (outcome === "created") stats.created_count += 1;
        else if (outcome === "updated") stats.updated_count += 1;
        else if (outcome === "adopted") {
          stats.adopted_count += 1;
          stats.skipped_count += 1;
        } else stats.skipped_count += 1;
      } catch (error) {
        stats.error_count += 1;
        console.error("[nova-knowledge-ingest] document failed", error);
      }
    }
    if (documents.length) stats.checkpoint.next_offset = offset + limit;
    const status = stats.error_count > 0 ? "partial" : "completed";
    await finishRun(runId, status, stats);
    return reply({ ok: true, run_id: runId, adapter, ...stats, documents: documents.length });
  } catch (error) {
    const message = error instanceof Error ? error.message.slice(0, 500) : clean(error, 500);
    if (runId) {
      try { await finishRun(runId, "failed", { ...stats, error_count: Math.max(1, stats.error_count) }, message); } catch { /* preserve original failure */ }
    }
    console.error(`[nova-knowledge-ingest] ${message}`);
    return reply({ error: message }, 500);
  }
});
