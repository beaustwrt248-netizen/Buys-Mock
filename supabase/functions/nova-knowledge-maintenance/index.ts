import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";
import {
  MAX_INGESTION_BATCH,
  SAFE_INGESTION_ADAPTERS,
  latestIngestion,
  runInternalIngestion,
} from "../_shared/nova_internal_ingestion.mjs";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "";
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
const SCHEDULER_SECRET = Deno.env.get("MORLEY_BACKUP_SECRET") || "";
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false, autoRefreshToken: false } });
const MAX_EMBEDDING_BATCH = 20;
const MAX_INGEST_BATCH = 20;
const INGESTION_INTERVAL_MS = 60 * 60 * 1000;
const INGESTION_ADAPTERS = SAFE_INGESTION_ADAPTERS.filter((adapter) => adapter !== "all");

const clean = (value: unknown, max = 500) => String(value ?? "").trim().replace(/\s+/g, " ").slice(0, max);
const bounded = (value: unknown, fallback: number, max: number) => Math.min(Math.max(Number(value) || fallback, 1), max);

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

async function authorized(req: Request) {
  if (!SUPABASE_URL || !SERVICE_ROLE) return false;
  const supplied = clean(req.headers.get("x-maintenance-secret"), 512);
  if (!supplied) return false;
  if (SCHEDULER_SECRET && constantTimeEqual(supplied, SCHEDULER_SECRET)) return true;
  try {
    const { data, error } = await admin.rpc("morley_backup_scheduler_secret_matches", { candidate: supplied });
    return !error && data === true;
  } catch {
    return false;
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

async function maybeIngest(limit: number, stats: any) {
  const now = Date.now();
  for (const adapter of INGESTION_ADAPTERS) {
    const latest = await latestIngestion(admin, adapter);
    const previous = latest?.completed_at || latest?.started_at || null;
    const previousMs = previous ? Date.parse(previous) : 0;
    if (Number.isFinite(previousMs) && previousMs > 0 && now - previousMs < INGESTION_INTERVAL_MS) continue;

    const nextOffset = Number(latest?.checkpoint?.next_offset);
    const offset = Number.isFinite(nextOffset) && nextOffset >= 0 ? nextOffset : 0;
    const safeLimit = Math.min(Math.min(limit, MAX_INGEST_BATCH), MAX_INGESTION_BATCH);
    const result = await runInternalIngestion({ admin, adapter, limit: safeLimit, offset, actorId: null });
    stats.ingested_created += Number(result.created_count || 0);
    stats.ingested_updated += Number(result.updated_count || 0);
    stats.ingested_skipped += Number(result.skipped_count || 0);
    stats.ingested_error += Number(result.error_count || 0);
    return;
  }
}

function retryDelayMinutes(attempt: number) {
  return Math.min(24 * 60, 15 * (2 ** Math.max(0, Math.min(attempt - 1, 7))));
}

function embeddingErrorCode(error: unknown) {
  const message = clean(error instanceof Error ? error.message : error, 200).toLowerCase();
  if (message.includes("dimension")) return "INVALID_DIMENSIONS";
  if (message.includes("timeout")) return "EMBED_TIMEOUT";
  if (message.includes("429") || message.includes("rate")) return "EMBED_RATE_LIMIT";
  return "EMBED_FAILED";
}

async function markEmbeddingReady(id: string, vector: number[]) {
  const { error } = await admin.from("nova_knowledge_chunks").update({
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
  }).eq("id", id).eq("status", "active");
  if (error) throw error;
}

async function markEmbeddingError(id: string, attempt: number, error: unknown) {
  const now = Date.now();
  const nextRetry = new Date(now + retryDelayMinutes(attempt) * 60_000).toISOString();
  const { error: updateError } = await admin.from("nova_knowledge_chunks").update({
    embedding_status: "error",
    embedding_error: clean(error instanceof Error ? error.message : error, 400),
    embedding_last_error_code: embeddingErrorCode(error),
    embedding_next_retry_at: nextRetry,
    updated_at: new Date(now).toISOString(),
  }).eq("id", id).eq("status", "active");
  if (updateError) throw updateError;
}

async function embedClaimed(limit: number, stats: any) {
  const boundedLimit = Math.min(limit, MAX_EMBEDDING_BATCH);
  const { data, error } = await admin.rpc("nova_claim_embedding_chunks", { p_limit: boundedLimit });
  if (error) throw error;
  const rows = Array.isArray(data) ? data.slice(0, boundedLimit) : [];
  const model = new Supabase.ai.Session("gte-small");

  for (const chunk of rows) {
    try {
      const result = await model.run(String(chunk.content || ""), { mean_pool: true, normalize: true });
      const vector = Array.from(result as ArrayLike<number>);
      if (vector.length !== 384) throw new Error(`Unexpected embedding dimensions: ${vector.length}`);
      await markEmbeddingReady(String(chunk.id), vector);
      stats.embedded_ready += 1;
    } catch (error) {
      try {
        await markEmbeddingError(String(chunk.id), Number(chunk.embedding_attempt_count || 1), error);
      } catch (updateError) {
        console.error("[nova-knowledge-maintenance] failed to persist embedding error state", clean(updateError instanceof Error ? updateError.message : updateError, 240));
      }
      stats.embedded_error += 1;
      console.error("[nova-knowledge-maintenance] embedding failed", clean(error instanceof Error ? error.message : error, 240));
    }
  }
}

Deno.serve(async (req: Request) => {
  if (req.method !== "POST") return reply({ error: "POST required" }, 405);
  if (!(await authorized(req))) return reply({ error: "Maintenance authorization required" }, 401);

  let body: any = {};
  try { body = await req.json(); } catch { return reply({ error: "Invalid JSON request" }, 400); }
  if (clean(body.action, 40).toLowerCase() !== "run") return reply({ error: "Unsupported action" }, 400);

  const embeddingLimit = bounded(body.embedding_limit, 20, MAX_EMBEDDING_BATCH);
  const ingestLimit = bounded(body.ingest_limit, 12, MAX_INGEST_BATCH);
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
  let ingestionFailure: string | null = null;

  try {
    runId = await beginRun();
    try {
      await maybeIngest(ingestLimit, stats);
    } catch (error) {
      ingestionFailure = clean(error instanceof Error ? error.message : error, 300);
      stats.ingested_error += 1;
      console.warn("[nova-knowledge-maintenance] bounded ingestion failed; continuing embeddings", ingestionFailure);
    }

    await embedClaimed(embeddingLimit, stats);
    const partial = stats.embedded_error > 0 || stats.ingested_error > 0;
    await finishRun(runId, partial ? "partial" : "completed", stats, ingestionFailure);
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
    const message = clean(error instanceof Error ? error.message : error, 400);
    if (runId) {
      try { await finishRun(runId, "failed", stats, message); } catch { /* preserve original error */ }
    }
    console.error("[nova-knowledge-maintenance] run failed", message);
    return reply({ error: "Nova knowledge maintenance failed safely", duration_ms: Date.now() - started }, 500);
  }
});