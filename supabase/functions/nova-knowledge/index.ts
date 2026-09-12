import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get('SUPABASE_URL') || '';
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY') || '';
const OPENROUTER_API_KEY = Deno.env.get('OPENROUTER_API_KEY') || '';
const EMBEDDING_MODEL = Deno.env.get('NOVA_EMBEDDING_MODEL') || 'openai/text-embedding-3-small';
const EMBEDDING_DIMENSIONS = 1536;
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false, autoRefreshToken: false } });

const ORIGINS = new Set(['https://buyshub.me', 'https://www.buyshub.me', 'https://beaustwrt248-netizen.github.io']);
const CATEGORIES = new Set([
  'general', 'catalogue', 'pricing', 'valuation', 'inventory', 'sales', 'support', 'guardian', 'release', 'admin',
  'development', 'operations', 'research', 'policy', 'product'
]);
const TRUST = new Set(['reference', 'reviewed', 'verified']);
const TYPES = new Set(['manual', 'file', 'import', 'system', 'web', 'integration']);
const F = 'id,category,title,content,source_type,source_label,source_filename,mime_type,version_label,trust_level,status,content_hash,revision,metadata,source_uri,external_key,source_updated_at,fresh_until,confidence,authority_weight,created_by,updated_by,created_at,updated_at';

const clean = (value: unknown, max = 220) => String(value ?? '').trim().replace(/\s+/g, ' ').slice(0, max);
const knowledgeContent = (value: unknown) => String(value ?? '').replace(/\r\n/g, '\n').trim().slice(0, 250000);

function headers(req: Request) {
  const origin = req.headers.get('Origin') || '';
  return {
    'Content-Type': 'application/json',
    'Access-Control-Allow-Origin': ORIGINS.has(origin) ? origin : 'https://buyshub.me',
    'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
    'Access-Control-Allow-Methods': 'POST, OPTIONS',
    'Cache-Control': 'no-store',
    'Vary': 'Origin',
    'X-Content-Type-Options': 'nosniff'
  };
}

async function auth(req: Request) {
  if (!SUPABASE_URL || !SERVICE_ROLE) return { error: 'Nova knowledge backend configuration unavailable', status: 503 } as const;
  const token = (req.headers.get('Authorization') || '').replace(/^Bearer\s+/i, '');
  if (!token) return { error: 'Authentication required', status: 401 } as const;
  const { data: { user }, error } = await admin.auth.getUser(token);
  if (error || !user) return { error: 'Invalid session', status: 401 } as const;
  const { data: profile, error: profileError } = await admin.from('profiles').select('role,is_enabled').eq('id', user.id).maybeSingle();
  if (profileError) throw profileError;
  if (!profile?.is_enabled || profile.role !== 'admin') return { error: 'Admin access required', status: 403 } as const;
  return { user } as const;
}

function safeIso(value: unknown) {
  const raw = clean(value, 80);
  if (!raw) return null;
  const timestamp = Date.parse(raw);
  return Number.isFinite(timestamp) ? new Date(timestamp).toISOString() : null;
}

function bounded01(value: unknown, fallback: number) {
  const n = Number(value);
  return Number.isFinite(n) ? Math.min(1, Math.max(0, n)) : fallback;
}

function input(body: any, defaultType = 'manual') {
  const category = clean(body.category, 40).toLowerCase() || 'general';
  const trust_level = clean(body.trust_level, 40).toLowerCase() || 'reference';
  const source_type = clean(body.source_type, 40).toLowerCase() || defaultType;
  const title = clean(body.title, 180);
  const content = knowledgeContent(body.content);
  if (!CATEGORIES.has(category)) throw new Error('Invalid knowledge category');
  if (!TRUST.has(trust_level)) throw new Error('Invalid trust level');
  if (!TYPES.has(source_type)) throw new Error('Invalid source type');
  if (!title || !content) throw new Error('Knowledge title and content are required');
  return {
    category,
    trust_level,
    source_type,
    title,
    content,
    source_label: clean(body.source_label) || null,
    source_filename: clean(body.source_filename) || null,
    mime_type: clean(body.mime_type, 120) || null,
    version_label: clean(body.version_label, 120) || null,
    source_uri: clean(body.source_uri, 2000) || null,
    external_key: clean(body.external_key, 500) || null,
    source_updated_at: safeIso(body.source_updated_at),
    fresh_until: safeIso(body.fresh_until),
    confidence: bounded01(body.confidence, 0.5),
    authority_weight: bounded01(body.authority_weight, 0.5),
    metadata: body.metadata && typeof body.metadata === 'object' && !Array.isArray(body.metadata) ? body.metadata : {}
  };
}

function pub(row: any, full = false) {
  if (!row) return null;
  const out: any = {
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
    source_uri: row.source_uri,
    external_key: row.external_key,
    source_updated_at: row.source_updated_at,
    fresh_until: row.fresh_until,
    confidence: row.confidence,
    authority_weight: row.authority_weight,
    created_at: row.created_at,
    updated_at: row.updated_at
  };
  if (full) out.content = row.content;
  else out.snippet = String(row.content || '').slice(0, 420);
  return out;
}

async function get(id: string) {
  const { data, error } = await admin.from('nova_knowledge_items').select(F).eq('id', id).maybeSingle();
  if (error) throw error;
  return data;
}

async function hash(text: string) {
  const digest = await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text));
  return [...new Uint8Array(digest)].map(byte => byte.toString(16).padStart(2, '0')).join('');
}

async function snap(row: any, userId: string) {
  const snapshot = {
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
    metadata: row.metadata || {},
    source_uri: row.source_uri,
    external_key: row.external_key,
    source_updated_at: row.source_updated_at,
    fresh_until: row.fresh_until,
    confidence: row.confidence,
    authority_weight: row.authority_weight,
    created_at: row.created_at,
    updated_at: row.updated_at
  };
  const { error } = await admin.from('nova_knowledge_revisions').upsert({
    knowledge_id: row.id,
    revision: row.revision,
    snapshot,
    changed_by: userId
  }, { onConflict: 'knowledge_id,revision', ignoreDuplicates: true });
  if (error) throw error;
}

function splitLongText(text: string, maxChars: number, overlap: number) {
  if (text.length <= maxChars) return [text];
  const parts: string[] = [];
  const step = Math.max(1, maxChars - overlap);
  for (let start = 0; start < text.length; start += step) {
    const part = text.slice(start, start + maxChars).trim();
    if (part) parts.push(part);
    if (start + maxChars >= text.length) break;
  }
  return parts;
}

function buildChunks(raw: string, maxChars = 3200, overlap = 320) {
  const normalized = knowledgeContent(raw);
  if (!normalized) return [];
  const paragraphs = normalized.split(/\n{2,}/).map(part => part.trim()).filter(Boolean);
  const chunks: string[] = [];
  let current = '';

  const flush = () => {
    const value = current.trim();
    if (value) chunks.push(value);
    current = '';
  };

  for (const paragraph of paragraphs.length ? paragraphs : [normalized]) {
    if (paragraph.length > maxChars) {
      flush();
      chunks.push(...splitLongText(paragraph, maxChars, overlap));
      continue;
    }
    const candidate = current ? `${current}\n\n${paragraph}` : paragraph;
    if (candidate.length <= maxChars) {
      current = candidate;
      continue;
    }
    const tail = current.slice(Math.max(0, current.length - overlap)).trim();
    flush();
    current = tail ? `${tail}\n\n${paragraph}` : paragraph;
    if (current.length > maxChars) {
      chunks.push(...splitLongText(current, maxChars, overlap));
      current = '';
    }
  }
  flush();
  return chunks;
}

async function embedBatch(texts: string[]) {
  if (!texts.length) return [] as (number[] | null)[];
  if (!OPENROUTER_API_KEY) return texts.map(() => null);
  try {
    const response = await fetch('https://openrouter.ai/api/v1/embeddings', {
      method: 'POST',
      headers: {
        'Authorization': `Bearer ${OPENROUTER_API_KEY}`,
        'Content-Type': 'application/json',
        'HTTP-Referer': 'https://buyshub.me',
        'X-Title': 'Morley Buys Nova Knowledge'
      },
      body: JSON.stringify({
        model: EMBEDDING_MODEL,
        input: texts,
        dimensions: EMBEDDING_DIMENSIONS,
        encoding_format: 'float'
      })
    });
    if (!response.ok) {
      console.error(`[nova-knowledge] embedding request failed HTTP ${response.status}`);
      return texts.map(() => null);
    }
    const json = await response.json().catch(() => ({}));
    const byIndex = new Map<number, number[]>();
    for (const item of Array.isArray(json?.data) ? json.data : []) {
      if (Number.isInteger(item?.index) && Array.isArray(item?.embedding) && item.embedding.length === EMBEDDING_DIMENSIONS) {
        byIndex.set(item.index, item.embedding.map(Number));
      }
    }
    return texts.map((_, index) => byIndex.get(index) || null);
  } catch (error) {
    console.error('[nova-knowledge] embedding request failed', clean(error instanceof Error ? error.message : error, 240));
    return texts.map(() => null);
  }
}

async function embedTexts(texts: string[]) {
  const vectors: (number[] | null)[] = [];
  for (let offset = 0; offset < texts.length; offset += 16) {
    vectors.push(...await embedBatch(texts.slice(offset, offset + 16)));
  }
  return vectors;
}

async function syncChunks(knowledgeId: string, content: string, active = true) {
  const now = new Date().toISOString();
  if (!active) {
    const { error } = await admin.from('nova_knowledge_chunks').update({ active: false, updated_at: now }).eq('knowledge_id', knowledgeId);
    if (error) throw error;
    return { chunks: 0, embedded: 0, active: false };
  }

  const parts = buildChunks(content);
  const vectors = await embedTexts(parts);
  const hashes = await Promise.all(parts.map(part => hash(part)));
  const rows = parts.map((part, chunk_index) => ({
    knowledge_id: knowledgeId,
    chunk_index,
    content: part,
    content_hash: hashes[chunk_index],
    token_estimate: Math.max(1, Math.ceil(part.length / 4)),
    embedding: vectors[chunk_index],
    embedding_model: vectors[chunk_index] ? EMBEDDING_MODEL : null,
    embedded_at: vectors[chunk_index] ? now : null,
    active: true,
    metadata: { chunking: 'paragraph-overlap-v1' },
    updated_at: now
  }));
  if (rows.length) {
    const { error } = await admin.from('nova_knowledge_chunks').upsert(rows, { onConflict: 'knowledge_id,chunk_index' });
    if (error) throw error;
  }
  const { error: staleError } = await admin.from('nova_knowledge_chunks')
    .update({ active: false, updated_at: now })
    .eq('knowledge_id', knowledgeId)
    .gte('chunk_index', rows.length);
  if (staleError) throw staleError;
  return { chunks: rows.length, embedded: vectors.filter(Boolean).length, active: true };
}

async function safeSyncChunks(knowledgeId: string, content: string, active = true) {
  try {
    return { ok: true, ...(await syncChunks(knowledgeId, content, active)) };
  } catch (error) {
    const message = clean(error instanceof Error ? error.message : error, 300);
    console.error(`[nova-knowledge] chunk sync degraded: ${message}`);
    return { ok: false, error: message, chunks: 0, embedded: 0, active };
  }
}

async function exactCount(table: string, filters: Record<string, string | boolean> = {}, notNull?: string) {
  let query: any = admin.from(table).select('id', { count: 'exact', head: true });
  for (const [column, value] of Object.entries(filters)) query = query.eq(column, value);
  if (notNull) query = query.not(notNull, 'is', null);
  const { count, error } = await query;
  if (error) throw error;
  return Number(count || 0);
}

async function summary() {
  const categories = [...CATEGORIES];
  const trustLevels = [...TRUST];
  const [count, active_count, archived_count, categoryCounts, trustCounts] = await Promise.all([
    exactCount('nova_knowledge_items'),
    exactCount('nova_knowledge_items', { status: 'active' }),
    exactCount('nova_knowledge_items', { status: 'archived' }),
    Promise.all(categories.map(async category => [category, await exactCount('nova_knowledge_items', { status: 'active', category })] as const)),
    Promise.all(trustLevels.map(async trust_level => [trust_level, await exactCount('nova_knowledge_items', { status: 'active', trust_level })] as const))
  ]);
  let chunk_count = 0;
  let embedded_chunk_count = 0;
  try {
    [chunk_count, embedded_chunk_count] = await Promise.all([
      exactCount('nova_knowledge_chunks', { active: true }),
      exactCount('nova_knowledge_chunks', { active: true }, 'embedding')
    ]);
  } catch (error) {
    console.error('[nova-knowledge] chunk summary unavailable', clean(error instanceof Error ? error.message : error, 200));
  }
  const by_category: Record<string, number> = {};
  const by_trust: Record<string, number> = {};
  for (const [category, total] of categoryCounts) if (total > 0) by_category[category] = total;
  for (const [trust, total] of trustCounts) if (total > 0) by_trust[trust] = total;
  return { count, active_count, archived_count, chunk_count, embedded_chunk_count, by_category, by_trust };
}

async function legacySearch(queryText: string, category: string, limit: number) {
  let query: any = admin.from('nova_knowledge_items').select(F).eq('status', 'active').order('updated_at', { ascending: false }).limit(limit);
  if (category && category !== 'all') query = query.eq('category', category);
  if (queryText) query = query.textSearch('search_vector', queryText, { type: 'websearch', config: 'english' });
  const { data, error } = await query;
  if (error) throw error;
  return data || [];
}

async function hybridSearch(queryText: string, category: string, limit: number) {
  const [vector] = await embedTexts([queryText]);
  const { data, error } = await admin.rpc('nova_match_knowledge_hybrid', {
    query_text: queryText,
    query_embedding: vector,
    match_count: limit,
    category_filter: category && category !== 'all' ? category : null,
    full_text_weight: 1.0,
    semantic_weight: vector ? 1.0 : 0.0,
    rrf_k: 50
  });
  if (!error) return { mode: vector ? 'hybrid' : 'lexical', embedding_used: !!vector, items: data || [] };
  console.error('[nova-knowledge] hybrid RPC unavailable, using lexical fallback', clean(error.message, 240));
  const fallback = await legacySearch(queryText, category, limit);
  return { mode: 'lexical-fallback', embedding_used: false, items: fallback.map((row: any) => pub(row, false)) };
}

async function createItem(payload: ReturnType<typeof input>, userId: string) {
  const now = new Date().toISOString();
  const content_hash = await hash(payload.content);
  const { data, error } = await admin.from('nova_knowledge_items').insert({
    ...payload,
    content_hash,
    revision: 1,
    status: 'active',
    created_by: userId,
    updated_by: userId,
    created_at: now,
    updated_at: now
  }).select(F).single();
  if (error) throw error;
  const retrieval = await safeSyncChunks(data.id, data.content, true);
  return { data, retrieval };
}

Deno.serve(async (req: Request) => {
  const h = headers(req);
  const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), { status, headers: h });
  if (req.method === 'OPTIONS') return new Response('ok', { headers: h });
  if (req.method !== 'POST') return reply({ error: 'POST required' }, 405);
  try {
    const caller = await auth(req);
    if ('error' in caller) return reply({ error: caller.error }, caller.status);
    let body: any = {};
    try { body = await req.json(); } catch { return reply({ error: 'Invalid JSON request' }, 400); }
    const action = clean(body.action, 40).toLowerCase();

    if (action === 'summary') return reply({ ok: true, ...await summary(), access: 'admin-only' });

    if (action === 'list' || action === 'search') {
      const q = clean(body.q ?? body.query, 180);
      const category = clean(body.category, 40).toLowerCase();
      const limit = Math.min(Math.max(Number(body.limit) || 50, 1), action === 'search' ? 20 : 100);
      if (category && category !== 'all' && !CATEGORIES.has(category)) return reply({ error: 'Invalid knowledge category' }, 400);
      const items = await legacySearch(q, category, limit);
      return reply({ ok: true, query: q, items: items.map((row: any) => pub(row, false)) });
    }

    if (action === 'hybrid-search') {
      const q = clean(body.q ?? body.query, 500);
      const category = clean(body.category, 40).toLowerCase();
      const limit = Math.min(Math.max(Number(body.limit) || 20, 1), 100);
      if (!q) return reply({ error: 'Search query is required' }, 400);
      if (category && category !== 'all' && !CATEGORIES.has(category)) return reply({ error: 'Invalid knowledge category' }, 400);
      return reply({ ok: true, query: q, ...(await hybridSearch(q, category, limit)) });
    }

    if (action === 'get') {
      const row = await get(clean(body.id, 80));
      return row ? reply({ ok: true, item: pub(row, true) }) : reply({ error: 'Knowledge item not found' }, 404);
    }

    if (action === 'create') {
      const created = await createItem(input(body), caller.user.id);
      return reply({ ok: true, item: pub(created.data, true), retrieval: created.retrieval }, 201);
    }

    if (action === 'ingest') {
      const payload = input({ ...body, source_type: body.source_type || 'integration' }, 'integration');
      if (!payload.external_key) return reply({ error: 'external_key is required for ingestion' }, 400);
      const { data: existing, error: lookupError } = await admin.from('nova_knowledge_items').select(F)
        .eq('category', payload.category)
        .eq('source_type', payload.source_type)
        .eq('external_key', payload.external_key)
        .maybeSingle();
      if (lookupError) throw lookupError;
      if (!existing) {
        const created = await createItem(payload, caller.user.id);
        return reply({ ok: true, created: true, changed: true, item: pub(created.data, true), retrieval: created.retrieval }, 201);
      }

      const nextHash = await hash(payload.content);
      const now = new Date().toISOString();
      const materialChanged = existing.content_hash !== nextHash || existing.title !== payload.title;
      if (!materialChanged) {
        const { data, error } = await admin.from('nova_knowledge_items').update({
          title: payload.title,
          source_label: payload.source_label,
          source_filename: payload.source_filename,
          mime_type: payload.mime_type,
          version_label: payload.version_label,
          trust_level: payload.trust_level,
          metadata: payload.metadata,
          source_uri: payload.source_uri,
          source_updated_at: payload.source_updated_at,
          fresh_until: payload.fresh_until,
          confidence: payload.confidence,
          authority_weight: payload.authority_weight,
          status: 'active',
          updated_by: caller.user.id,
          updated_at: now
        }).eq('id', existing.id).select(F).single();
        if (error) throw error;
        const retrieval = existing.status === 'active' ? { ok: true, unchanged: true } : await safeSyncChunks(existing.id, existing.content, true);
        return reply({ ok: true, created: false, changed: false, item: pub(data, true), retrieval });
      }

      await snap(existing, caller.user.id);
      const { data, error } = await admin.from('nova_knowledge_items').update({
        ...payload,
        content_hash: nextHash,
        status: 'active',
        revision: Number(existing.revision || 1) + 1,
        updated_by: caller.user.id,
        updated_at: now
      }).eq('id', existing.id).select(F).single();
      if (error) throw error;
      const retrieval = await safeSyncChunks(existing.id, data.content, true);
      return reply({ ok: true, created: false, changed: true, item: pub(data, true), retrieval });
    }

    if (['update', 'archive', 'restore'].includes(action)) {
      const id = clean(body.id, 80);
      const old = await get(id);
      if (!old) return reply({ error: 'Knowledge item not found' }, 404);
      await snap(old, caller.user.id);
      const now = new Date().toISOString();
      const patch: any = action === 'update'
        ? { ...input({ ...old, ...body, metadata: body.metadata ?? old.metadata }), content_hash: await hash(knowledgeContent(body.content ?? old.content)) }
        : { status: action === 'archive' ? 'archived' : 'active' };
      patch.revision = Number(old.revision || 1) + 1;
      patch.updated_by = caller.user.id;
      patch.updated_at = now;
      const { data, error } = await admin.from('nova_knowledge_items').update(patch).eq('id', id).select(F).single();
      if (error) throw error;
      const retrieval = action === 'archive'
        ? await safeSyncChunks(id, data.content, false)
        : await safeSyncChunks(id, data.content, true);
      return reply({ ok: true, item: pub(data, true), retrieval });
    }

    if (action === 'delete') return reply({ error: 'Permanent knowledge deletion is disabled through Nova. Archive instead.' }, 403);
    return reply({ error: 'Unsupported action' }, 400);
  } catch (error) {
    const message = error instanceof Error ? error.message : clean(error, 500);
    console.error(`[nova-knowledge] ${message}`);
    return new Response(JSON.stringify({ error: message }), { status: 500, headers: headers(req) });
  }
});
