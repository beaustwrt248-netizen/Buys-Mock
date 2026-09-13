import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";
import {
  chunkKnowledgeDocument,
  fuseKnowledgeScores,
  normalizeKnowledgeDocument,
  sanitizeMetadata,
} from "./knowledge_core.mjs";

const SUPABASE_URL = Deno.env.get("SUPABASE_URL") || "";
const SERVICE_ROLE = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") || "";
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false, autoRefreshToken: false } });
const ORIGINS = new Set(["https://buyshub.me", "https://www.buyshub.me", "https://beaustwrt248-netizen.github.io"]);
const CATEGORIES = new Set(["general", "catalogue", "pricing", "valuation", "inventory", "sales", "support", "guardian", "release", "admin"]);
const TRUST = new Set(["reference", "reviewed", "verified"]);
const TYPES = new Set(["manual", "file", "import"]);
const PLATFORM_MISSING = new Set(["42P01", "PGRST202", "PGRST204", "PGRST205"]);
const F = "id,category,title,content,source_type,source_label,source_filename,mime_type,version_label,trust_level,status,content_hash,revision,metadata,created_by,updated_by,created_at,updated_at";

const clean = (value: unknown, max = 220) => String(value ?? "").trim().replace(/\s+/g, " ").slice(0, max);
const bounded = (value: unknown, fallback: number, max: number) => Math.min(Math.max(Number(value) || fallback, 1), max);
const isPlatformMissing = (error: any) => Boolean(error && PLATFORM_MISSING.has(String(error.code || "")));
const authorityFor = (trust: string) => trust === "verified" ? 1 : trust === "reviewed" ? 0.82 : 0.65;
const validIso = (value: unknown) => {
  const text = clean(value, 80);
  if (!text) return null;
  const parsed = Date.parse(text);
  return Number.isFinite(parsed) ? new Date(parsed).toISOString() : null;
};

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
  if (!SUPABASE_URL || !SERVICE_ROLE) return { error: "Nova knowledge backend configuration unavailable", status: 503 } as const;
  const token = (req.headers.get("Authorization") || "").replace(/^Bearer\s+/i, "");
  if (!token) return { error: "Authentication required", status: 401 } as const;
  const { data: { user }, error } = await admin.auth.getUser(token);
  if (error || !user) return { error: "Invalid session", status: 401 } as const;
  const { data: p, error: profileError } = await admin.from("profiles").select("role,is_enabled").eq("id", user.id).maybeSingle();
  if (profileError) throw profileError;
  if (!p?.is_enabled || p.role !== "admin") return { error: "Admin access required", status: 403 } as const;
  return { user } as const;
}

function pub(row: any, full = false) {
  if (!row) return null;
  const output: any = {
    id: row.id,
    category: row.category,
    title: row.title,
    source_type: row.source_type,
    source_label: row.source_label,
    source_filename: row.source_filename,
    mime_type: row.mime_type,
    version_label: row.version_label,
    trust_level: row.trust_level,
    status: row.status,
    revision: row.revision,
    metadata: row.metadata || {},
    created_at: row.created_at,
    updated_at: row.updated_at,
  };
  if (full) output.content = row.content;
  else output.snippet = String(row.content || "").slice(0, 420);
  return output;
}

async function get(id: string) {
  const { data, error } = await admin.from("nova_knowledge_items").select(F).eq("id", id).maybeSingle();
  if (error) throw error;
  return data;
}

async function input(body: any) {
  const normalized = await normalizeKnowledgeDocument(body);
  if (!CATEGORIES.has(normalized.category)) throw new Error("Invalid knowledge category");
  if (!TRUST.has(normalized.trust_level)) throw new Error("Invalid trust level");
  if (!TYPES.has(normalized.source_type)) throw new Error("Invalid source type");
  if (!normalized.title || !normalized.content) throw new Error("Knowledge title and content are required");
  return {
    category: normalized.category,
    trust_level: normalized.trust_level,
    source_type: normalized.source_type,
    title: normalized.title,
    content: normalized.content,
    content_hash: normalized.content_hash,
    source_label: normalized.source_label,
    source_filename: normalized.source_filename,
    mime_type: clean(body.mime_type, 120) || null,
    version_label: normalized.version_label,
    metadata: normalized.metadata,
  };
}

async function snap(row: any, userId: string) {
  const { error } = await admin.from("nova_knowledge_revisions").upsert({
    knowledge_id: row.id,
    revision: row.revision,
    snapshot: {
      category: row.category,
      title: row.title,
      content: row.content,
      source_type: row.source_type,
      source_label: row.source_label,
      source_filename: row.source_filename,
      mime_type: row.mime_type,
      version_label: row.version_label,
      trust_level: row.trust_level,
      status: row.status,
      content_hash: row.content_hash,
      revision: row.revision,
      metadata: sanitizeMetadata(row.metadata || {}),
      created_at: row.created_at,
      updated_at: row.updated_at,
    },
    changed_by: userId,
  }, { onConflict: "knowledge_id,revision", ignoreDuplicates: true });
  if (error) throw error;
}

async function exactCount(table = "nova_knowledge_items", filters: Record<string, string> = {}) {
  let query = admin.from(table).select("id", { count: "exact", head: true });
  for (const [column, value] of Object.entries(filters)) query = query.eq(column, value);
  const { count, error } = await query;
  if (error) throw error;
  return Number(count || 0);
}

async function optionalCount(table: string, filters: Record<string, string> = {}) {
  try {
    return { available: true, count: await exactCount(table, filters) };
  } catch (error: any) {
    if (isPlatformMissing(error)) return { available: false, count: 0 };
    throw error;
  }
}

async function summary() {
  const categories = [...CATEGORIES];
  const trustLevels = [...TRUST];
  const [count, active_count, archived_count, categoryCounts, trustCounts, chunks, pendingEmbeddings, ingestionRuns] = await Promise.all([
    exactCount(),
    exactCount("nova_knowledge_items", { status: "active" }),
    exactCount("nova_knowledge_items", { status: "archived" }),
    Promise.all(categories.map(async (category) => [category, await exactCount("nova_knowledge_items", { status: "active", category })] as const)),
    Promise.all(trustLevels.map(async (trust_level) => [trust_level, await exactCount("nova_knowledge_items", { status: "active", trust_level })] as const)),
    optionalCount("nova_knowledge_chunks", { status: "active" }),
    optionalCount("nova_knowledge_chunks", { status: "active", embedding_status: "pending" }),
    optionalCount("nova_knowledge_ingestion_runs"),
  ]);
  const by_category: Record<string, number> = {};
  const by_trust: Record<string, number> = {};
  for (const [category, total] of categoryCounts) if (total > 0) by_category[category] = total;
  for (const [trust, total] of trustCounts) if (total > 0) by_trust[trust] = total;
  return {
    count,
    active_count,
    archived_count,
    by_category,
    by_trust,
    platform: {
      schema_available: chunks.available,
      active_chunks: chunks.count,
      pending_embeddings: pendingEmbeddings.count,
      ingestion_runs: ingestionRuns.count,
    },
  };
}

async function syncChunks(row: any) {
  const normalized = await normalizeKnowledgeDocument(row);
  const chunks = await chunkKnowledgeDocument(normalized);
  const metadata: any = normalized.metadata || {};
  const observedAt = validIso(metadata.observed_at) || row.updated_at || new Date().toISOString();
  const staleAfter = validIso(metadata.stale_after);
  const confidence = Math.min(Math.max(Number(metadata.confidence) || 0.7, 0), 1);
  const sourcePayload = {
    source_key: normalized.source_key,
    domain: clean(metadata.domain, 80).toLowerCase() || normalized.category,
    source_type: normalized.source_type,
    source_label: normalized.source_label,
    source_uri: clean(metadata.source_uri, 1000) || null,
    trust_level: normalized.trust_level,
    authority_score: authorityFor(normalized.trust_level),
    content_hash: normalized.content_hash,
    status: "active",
    observed_at: observedAt,
    retrieved_at: new Date().toISOString(),
    stale_after: staleAfter,
    metadata,
    updated_at: new Date().toISOString(),
  };
  const { data: source, error: sourceError } = await admin.from("nova_knowledge_sources")
    .upsert(sourcePayload, { onConflict: "source_key" })
    .select("id")
    .single();
  if (sourceError) {
    if (isPlatformMissing(sourceError)) return { available: false, chunks: 0 };
    throw sourceError;
  }

  const { error: supersedeError } = await admin.from("nova_knowledge_chunks")
    .update({ status: "superseded", updated_at: new Date().toISOString() })
    .eq("knowledge_id", row.id)
    .eq("status", "active");
  if (supersedeError) throw supersedeError;

  if (!chunks.length) return { available: true, chunks: 0 };
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
    metadata: { domain: sourcePayload.domain },
    updated_at: new Date().toISOString(),
  }));
  const { error: chunkError } = await admin.from("nova_knowledge_chunks")
    .upsert(rows, { onConflict: "knowledge_id,knowledge_revision,chunk_index" });
  if (chunkError) throw chunkError;
  return { available: true, chunks: rows.length };
}

async function setChunkStatus(knowledgeId: string, status: "active" | "archived") {
  const { error } = await admin.from("nova_knowledge_chunks")
    .update({ status, updated_at: new Date().toISOString() })
    .eq("knowledge_id", knowledgeId)
    .neq("status", "superseded");
  if (error && !isPlatformMissing(error)) throw error;
}

async function createQueryEmbedding(query: string) {
  if (!query) return null;
  try {
    const model = new Supabase.ai.Session("gte-small");
    const result = await model.run(query, { mean_pool: true, normalize: true });
    const vector = Array.from(result as ArrayLike<number>);
    return vector.length === 384 ? vector : null;
  } catch (error) {
    console.warn("[nova-knowledge] semantic query embedding unavailable", error);
    return null;
  }
}

async function hybridSearch(query: string, category: string, limit: number, semantic: boolean) {
  const queryEmbedding = semantic ? await createQueryEmbedding(query) : null;
  const { data, error } = await admin.rpc("nova_search_knowledge_chunks", {
    p_query: query,
    p_query_embedding: queryEmbedding,
    p_limit: Math.min(limit * 4, 100),
    p_category: category && category !== "all" ? category : null,
    p_trust_level: null,
  });
  if (error) {
    if (!isPlatformMissing(error)) console.warn("[nova-knowledge] hybrid RPC degraded to legacy full-text", error);
    return null;
  }
  if (!data?.length) return null;
  const ranked = fuseKnowledgeScores(data).slice(0, limit);
  return {
    items: ranked.map((row: any) => ({
      id: row.knowledge_id,
      chunk_id: row.id,
      category: row.category,
      title: row.title,
      snippet: String(row.content || "").slice(0, 520),
      source_label: row.source_label,
      trust_level: row.trust_level,
      confidence: row.confidence,
      lexical_score: row.lexical_score,
      semantic_score: row.semantic_score,
      hybrid_score: row.hybrid_score,
      observed_at: row.observed_at,
      stale_after: row.stale_after,
    })),
    semantic: Boolean(queryEmbedding),
  };
}

async function legacySearch(query: string, category: string, limit: number) {
  let request = admin.from("nova_knowledge_items").select(F).eq("status", "active").order("updated_at", { ascending: false }).limit(limit);
  if (category && category !== "all") request = request.eq("category", category);
  if (query) request = request.textSearch("search_vector", query, { type: "websearch", config: "english" });
  const { data, error } = await request;
  if (error) throw error;
  return (data || []).map((row) => pub(row, false));
}

async function reindex(body: any) {
  const limit = bounded(body.limit, 25, 50);
  const offset = Math.max(Number(body.offset) || 0, 0);
  const { data, error } = await admin.from("nova_knowledge_items")
    .select(F)
    .eq("status", "active")
    .order("id", { ascending: true })
    .range(offset, offset + limit - 1);
  if (error) throw error;
  const results = [];
  for (const row of data || []) results.push({ id: row.id, ...(await syncChunks(row)) });
  return { offset, processed: results.length, next_offset: results.length === limit ? offset + limit : null, results };
}

async function embedPending(body: any) {
  const limit = bounded(body.limit, 8, 20);
  const { data, error } = await admin.from("nova_knowledge_chunks")
    .select("id,content")
    .eq("status", "active")
    .eq("embedding_status", "pending")
    .order("updated_at", { ascending: true })
    .limit(limit);
  if (error) {
    if (isPlatformMissing(error)) return { available: false, processed: 0, ready: 0, failed: 0 };
    throw error;
  }
  const model = new Supabase.ai.Session("gte-small");
  let ready = 0;
  let failed = 0;
  for (const row of data || []) {
    try {
      const result = await model.run(String(row.content || ""), { mean_pool: true, normalize: true });
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
      }).eq("id", row.id);
      if (updateError) throw updateError;
      ready += 1;
    } catch (embeddingError) {
      failed += 1;
      const message = embeddingError instanceof Error ? embeddingError.message.slice(0, 500) : "Embedding failed";
      await admin.from("nova_knowledge_chunks").update({
        embedding_status: "error",
        embedding_error: message,
        updated_at: new Date().toISOString(),
      }).eq("id", row.id);
    }
  }
  return { available: true, processed: (data || []).length, ready, failed };
}

Deno.serve(async (req: Request) => {
  const h = headers(req);
  const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: h });
  if (req.method === "OPTIONS") return new Response("ok", { headers: h });
  if (req.method !== "POST") return reply({ error: "POST required" }, 405);

  try {
    const authorized = await auth(req);
    if ("error" in authorized) return reply({ error: authorized.error }, authorized.status);
    let body: any = {};
    try { body = await req.json(); } catch { return reply({ error: "Invalid JSON request" }, 400); }
    const action = clean(body.action, 40).toLowerCase();

    if (action === "summary") return reply({ ok: true, ...(await summary()), access: "admin-only" });

    if (action === "list" || action === "search") {
      const query = clean(body.q ?? body.query, 180);
      const category = clean(body.category, 40).toLowerCase();
      const limit = bounded(body.limit, action === "search" ? 20 : 50, action === "search" ? 20 : 100);
      if (category && category !== "all" && !CATEGORIES.has(category)) return reply({ error: "Invalid knowledge category" }, 400);
      if (action === "search" && query) {
        const hybrid = await hybridSearch(query, category, limit, body.semantic !== false);
        if (hybrid) return reply({ ok: true, query, items: hybrid.items, retrieval: "hybrid", semantic: hybrid.semantic });
      }
      return reply({ ok: true, query, items: await legacySearch(query, category, limit), retrieval: "legacy-full-text", semantic: false });
    }

    if (action === "get") {
      const row = await get(clean(body.id, 80));
      return row ? reply({ ok: true, item: pub(row, true) }) : reply({ error: "Knowledge item not found" }, 404);
    }

    if (action === "create") {
      const payload = await input(body);
      const now = new Date().toISOString();
      const { data, error } = await admin.from("nova_knowledge_items").insert({
        ...payload,
        revision: 1,
        status: "active",
        created_by: authorized.user.id,
        updated_by: authorized.user.id,
        created_at: now,
        updated_at: now,
      }).select(F).single();
      if (error) throw error;
      const platform = await syncChunks(data);
      return reply({ ok: true, item: pub(data, true), platform }, 201);
    }

    if (["update", "archive", "restore"].includes(action)) {
      const id = clean(body.id, 80);
      const old = await get(id);
      if (!old) return reply({ error: "Knowledge item not found" }, 404);
      await snap(old, authorized.user.id);
      const patch: any = action === "update" ? await input(body) : { status: action === "archive" ? "archived" : "active" };
      patch.revision = Number(old.revision || 1) + 1;
      patch.updated_by = authorized.user.id;
      patch.updated_at = new Date().toISOString();
      const { data, error } = await admin.from("nova_knowledge_items").update(patch).eq("id", id).select(F).single();
      if (error) throw error;
      let platform: any = { available: false };
      if (action === "archive") await setChunkStatus(id, "archived");
      else platform = await syncChunks(data);
      return reply({ ok: true, item: pub(data, true), platform });
    }

    if (action === "reindex") return reply({ ok: true, ...(await reindex(body)) });
    if (action === "embed_pending") return reply({ ok: true, ...(await embedPending(body)) });
    if (action === "delete") return reply({ error: "Permanent knowledge deletion is disabled through Nova. Archive instead." }, 403);
    return reply({ error: "Unsupported action" }, 400);
  } catch (error) {
    const message = error instanceof Error ? error.message : clean(error, 500);
    console.error(`[nova-knowledge] ${message}`);
    return new Response(JSON.stringify({ error: message }), { status: 500, headers: headers(req) });
  }
});
