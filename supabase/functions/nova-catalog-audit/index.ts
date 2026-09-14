import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";
import {
  boundedBatch,
  classifySourceEvidence,
  isSafePublicSourceUrl,
  normalizePageText,
  retryDelaySeconds,
  sanitizeError,
  sourceTier,
} from "./logic.mjs";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "";
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
const SCHEDULER_SECRET = Deno.env.get("MORLEY_BACKUP_SECRET") || "";
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false, autoRefreshToken: false } });
const MAX_ATTEMPTS = 5;
const SOURCE_TIMEOUT_MS = 8_000;
const MAX_SOURCE_BODY = 500_000;

const clean = (value: unknown, max = 500) => String(value ?? "").trim().replace(/\s+/g, " ").slice(0, max);

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

async function fetchSource(url: string) {
  if (!isSafePublicSourceUrl(url)) return { blocked: "unsafe_or_invalid_source_url", text: "", contentType: "" };
  const response = await fetch(url, {
    method: "GET",
    redirect: "follow",
    signal: AbortSignal.timeout(SOURCE_TIMEOUT_MS),
    headers: {
      "User-Agent": "Morley-Nova-Catalog-Audit/1.0",
      "Accept": "text/html,text/plain,application/json;q=0.9,*/*;q=0.2",
    },
  });
  const contentType = clean(response.headers.get("content-type"), 120).toLowerCase();
  if (response.status === 429 || response.status >= 500) throw new Error(`source_retryable_http_${response.status}`);
  if (!response.ok) return { blocked: `source_http_${response.status}`, text: "", contentType };
  if (!/(text|html|json|xml)/.test(contentType)) return { blocked: "source_content_type_unsupported", text: "", contentType };
  const text = (await response.text()).slice(0, MAX_SOURCE_BODY);
  return { blocked: null, text, contentType };
}

function findingPayload(row: any, field: string, code: string, excerpt: string) {
  return {
    field_name: field,
    current_value: field === "identity"
      ? { brand: row.brand, model_name: row.model_name, model_number: row.model_number }
      : row[field] ?? null,
    observed_value: null,
    severity: "medium",
    confidence: 0,
    source_tier: sourceTier(row.source_url, row.source_name, row.brand),
    evidence_excerpt: clean(`${code}: ${excerpt}`, 1000),
  };
}

async function completeItem(
  row: any,
  workerId: string,
  outcome: "verified" | "blocked" | "discrepancy" | "failed" | "retry",
  error: string | null = null,
  retrySeconds: number | null = null,
  finding: Record<string, unknown> | null = null,
) {
  const { data, error: rpcError } = await admin.rpc("nova_complete_catalog_audit", {
    p_queue_id: row.id,
    p_worker: workerId,
    p_outcome: outcome,
    p_error: error,
    p_retry_seconds: retrySeconds,
    p_finding: finding,
  });
  if (rpcError) throw rpcError;
  return data === true;
}

async function blockItem(row: any, workerId: string, field: string, code: string, excerpt = "") {
  return completeItem(row, workerId, "blocked", code, null, findingPayload(row, field, code, excerpt));
}

async function retryItem(row: any, workerId: string, error: unknown) {
  const message = sanitizeError(error);
  if (Number(row.attempt_count) >= MAX_ATTEMPTS) return completeItem(row, workerId, "failed", message);
  return completeItem(row, workerId, "retry", message, retryDelaySeconds(row.attempt_count));
}

async function processItem(row: any, workerId: string) {
  try {
    const source = await fetchSource(row.source_url);
    if (source.blocked) return blockItem(row, workerId, "source_url", source.blocked, source.contentType);
    const evidence = classifySourceEvidence(row, source.text);
    if (evidence.outcome === "verified") return completeItem(row, workerId, "verified");
    if (evidence.outcome === "blocked") {
      return blockItem(row, workerId, evidence.field || "identity", evidence.code, clean(normalizePageText(source.text), 700));
    }
    return retryItem(row, workerId, evidence.code);
  } catch (error) {
    return retryItem(row, workerId, error);
  }
}

async function reconcileRun(runId: string) {
  const { data, error } = await admin.from("nova_catalog_audit_queue").select("status").eq("run_id", runId);
  if (error) throw error;
  const rows = data || [];
  if (!rows.length) return;
  const counts = rows.reduce((acc: Record<string, number>, row: any) => {
    acc[row.status] = (acc[row.status] || 0) + 1;
    return acc;
  }, {});
  const nonTerminal = (counts.pending || 0) + (counts.in_progress || 0);
  const failed = counts.failed || 0;
  const discrepancies = (counts.discrepancy || 0) + (counts.blocked || 0);
  const patch: Record<string, unknown> = {
    status: nonTerminal > 0 ? "running" : (failed > 0 ? "failed" : "completed"),
    scanned_count: rows.length,
    verified_count: counts.verified || 0,
    discrepancy_count: discrepancies,
    error_count: failed,
    notes: `verified=${counts.verified || 0}; blocked=${counts.blocked || 0}; discrepancy=${counts.discrepancy || 0}; failed=${failed}; pending=${counts.pending || 0}; in_progress=${counts.in_progress || 0}`,
  };
  if (nonTerminal === 0) patch.finished_at = new Date().toISOString();
  const { error: updateError } = await admin.from("nova_catalog_audit_runs").update(patch).eq("id", runId);
  if (updateError) throw updateError;
}

Deno.serve(async (req) => {
  if (req.method !== "POST") return reply({ error: "method_not_allowed" }, 405);
  if (!(await authorized(req))) return reply({ error: "unauthorized" }, 401);

  let payload: Record<string, unknown> = {};
  try { payload = await req.json(); } catch { payload = {}; }
  const limit = boundedBatch(payload.limit, 10, 25);
  const workerId = `catalog-audit:${crypto.randomUUID()}`;

  try {
    const { data, error } = await admin.rpc("nova_claim_catalog_audits", {
      p_worker: workerId,
      p_limit: limit,
      p_stale_seconds: 900,
    });
    if (error) throw error;
    const rows = Array.isArray(data) ? data.slice(0, limit) : [];
    const stats = { claimed: rows.length, verified: 0, blocked: 0, retried: 0, failed: 0, ownership_lost: 0 };
    const runIds = new Set<string>();

    for (const row of rows) {
      if (row.run_id) runIds.add(String(row.run_id));
      const processed = await processItem(row, workerId);
      if (!processed) {
        stats.ownership_lost += 1;
        continue;
      }
      const { data: state } = await admin.from("nova_catalog_audit_queue").select("status").eq("id", row.id).maybeSingle();
      if (state?.status === "verified") stats.verified += 1;
      else if (state?.status === "blocked" || state?.status === "discrepancy") stats.blocked += 1;
      else if (state?.status === "failed") stats.failed += 1;
      else if (state?.status === "pending") stats.retried += 1;
    }

    for (const runId of runIds) await reconcileRun(runId);
    return reply({ ok: true, worker: workerId, stats });
  } catch (error) {
    console.error("[nova-catalog-audit] batch failed", sanitizeError(error));
    return reply({ error: "catalog_audit_batch_failed", detail: sanitizeError(error) }, 500);
  }
});
