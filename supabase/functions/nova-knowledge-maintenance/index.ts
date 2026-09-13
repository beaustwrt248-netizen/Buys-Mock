import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "";
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
const MAINTENANCE_TOKEN = Deno.env.get("NOVA_KNOWLEDGE_MAINTENANCE_TOKEN") || "";
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false, autoRefreshToken: false } });
const MAX_EMBEDDING_BATCH = 20;

const clean = (value: unknown, max = 240) => String(value ?? "").trim().replace(/\s+/g, " ").slice(0, max);
const bounded = (value: unknown, fallback: number, max: number) => Math.min(Math.max(Number(value) || fallback, 1), max);

function constantTimeEqual(left: string, right: string) {
  const encoder = new TextEncoder();
  const a = encoder.encode(left);
  const b = encoder.encode(right);
  const length = Math.max(a.length, b.length);
  let mismatch = a.length ^ b.length;
  for (let i = 0; i < length; i++) mismatch |= (a[i] ?? 0) ^ (b[i] ?? 0);
  return mismatch === 0;
}

function retryDelayMinutes(attempt: number) {
  return Math.min(24 * 60, 5 * (2 ** Math.max(0, Math.min(attempt - 1, 8))));
}

function errorCode(error: unknown) {
  const message = clean(error instanceof Error ? error.message : error, 200).toLowerCase();
  if (message.includes("dimension")) return "INVALID_DIMENSIONS";
  if (message.includes("timeout")) return "EMBED_TIMEOUT";
  if (message.includes("rate") || message.includes("429")) return "EMBED_RATE_LIMIT";
  return "EMBED_FAILED";
}

async function markReady(id: string, vector: number[]) {
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

async function markError(id: string, attempt: number, error: unknown) {
  const now = Date.now();
  const nextRetry = new Date(now + retryDelayMinutes(attempt) * 60_000).toISOString();
  const { error: updateError } = await admin.from("nova_knowledge_chunks").update({
    embedding_status: "error",
    embedding_error: clean(error instanceof Error ? error.message : error, 500),
    embedding_last_error_code: errorCode(error),
    embedding_next_retry_at: nextRetry,
    updated_at: new Date(now).toISOString(),
  }).eq("id", id).eq("status", "active");
  if (updateError) throw updateError;
}

async function health() {
  const { data, error } = await admin.rpc("nova_knowledge_health");
  if (error) throw error;
  return data;
}

Deno.serve(async (req: Request) => {
  const headers = { "Content-Type": "application/json", "Cache-Control": "no-store", "X-Content-Type-Options": "nosniff" };
  const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers });
  if (req.method !== "POST") return reply({ error: "POST required" }, 405);
  if (!SUPABASE_URL || !SERVICE_ROLE || !MAINTENANCE_TOKEN) return reply({ error: "Maintenance backend unavailable" }, 503);

  const supplied = req.headers.get("x-nova-maintenance-token") || "";
  if (!supplied || !constantTimeEqual(supplied, MAINTENANCE_TOKEN)) return reply({ error: "Unauthorized" }, 401);

  let body: any = {};
  try { body = await req.json(); } catch { return reply({ error: "Invalid JSON request" }, 400); }
  if (clean(body.action, 40).toLowerCase() !== "embed_pending") return reply({ error: "Unsupported action" }, 400);

  const requestedLimit = bounded(body.limit, MAX_EMBEDDING_BATCH, MAX_EMBEDDING_BATCH);
  const limit = Math.min(requestedLimit, 20);
  const { data: claimed, error: claimError } = await admin.rpc("nova_claim_embedding_chunks", { p_limit: limit });
  if (claimError) return reply({ error: "Unable to claim embedding work" }, 500);

  const chunks = Array.isArray(claimed) ? claimed.slice(0, MAX_EMBEDDING_BATCH) : [];
  let processed = 0;
  let failed = 0;
  const model = new Supabase.ai.Session('gte-small');

  for (const chunk of chunks) {
    try {
      const result = await model.run(String(chunk.content || ""), { mean_pool: true, normalize: true });
      const vector = Array.from(result as ArrayLike<number>);
      if (vector.length !== 384) throw new Error(`Invalid embedding dimension: ${vector.length}`);
      await markReady(String(chunk.id), vector);
      processed += 1;
    } catch (error) {
      failed += 1;
      try {
        await markError(String(chunk.id), Number(chunk.embedding_attempt_count || 1), error);
      } catch (updateError) {
        console.error("[nova-knowledge-maintenance] failed to record embedding error", clean(updateError instanceof Error ? updateError.message : updateError));
      }
    }
  }

  let snapshot: unknown = null;
  try { snapshot = await health(); } catch (error) { console.warn("[nova-knowledge-maintenance] health unavailable", clean(error instanceof Error ? error.message : error)); }
  return reply({ ok: true, claimed: chunks.length, processed, failed, deferred: Math.max(0, chunks.length - processed - failed), health: snapshot });
});