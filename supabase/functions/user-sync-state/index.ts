import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!;
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false } });
const MAX_BYTES = 2_000_000;
const KEYS = ['bm_inv', 'bm_sales', 'bm_recent'] as const;
const cors = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
};

function reply(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...cors, 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' },
  });
}

function msg(e: unknown) {
  return e instanceof Error ? e.message : String(e);
}

function safeError(e: unknown) {
  if (!e || typeof e !== 'object') return { message: msg(e) };
  const r = e as Record<string, unknown>;
  return {
    message: String(r.message || 'unknown'),
    code: r.code ? String(r.code) : undefined,
    details: r.details ? String(r.details).slice(0, 300) : undefined,
    hint: r.hint ? String(r.hint).slice(0, 300) : undefined,
  };
}

async function user(req: Request) {
  const h = req.headers.get('Authorization') || '';
  if (!h.startsWith('Bearer ')) throw new Error('AUTH_REQUIRED');
  const token = h.slice(7).trim();
  try {
    const { data, error } = await admin.auth.getUser(token);
    if (error || !data.user) throw new Error('AUTH_REQUIRED');
    return data.user;
  } catch (e) {
    console.error('user-sync-state', { stage: 'auth_lookup', error: safeError(e) });
    throw e;
  }
}

function cleanState(input: unknown) {
  if (!input || typeof input !== 'object' || Array.isArray(input)) throw new Error('STATE_INVALID');
  const out: Record<string, unknown[]> = { bm_inv: [], bm_sales: [], bm_recent: [] };
  for (const key of KEYS) {
    const v = (input as Record<string, unknown>)[key];
    if (v != null && !Array.isArray(v)) throw new Error('STATE_INVALID');
    out[key] = Array.isArray(v) ? v : [];
  }
  const bytes = new TextEncoder().encode(JSON.stringify(out)).byteLength;
  if (bytes > MAX_BYTES) throw new Error('STATE_TOO_LARGE');
  return out;
}

function identity(row: unknown, index: number) {
  if (row && typeof row === 'object') {
    const r = row as Record<string, unknown>;
    for (const k of ['id', 'uuid', 'stock_number', 'stockNumber', 'barcode']) {
      if (r[k] != null && String(r[k])) return `${k}:${String(r[k])}`;
    }
  }
  return `anon:${index}:${JSON.stringify(row)}`;
}

function timestamp(row: unknown) {
  if (!row || typeof row !== 'object') return 0;
  const r = row as Record<string, unknown>;
  for (const k of ['updated_at', 'updatedAt', 'modified_at', 'modifiedAt', 'created_at', 'createdAt', 'date', 'time']) {
    const t = Date.parse(String(r[k] || ''));
    if (Number.isFinite(t)) return t;
  }
  return 0;
}

function mergeArray(server: unknown[], client: unknown[]) {
  const map = new Map<string, unknown>();
  server.forEach((r, i) => map.set(identity(r, i), r));
  client.forEach((r, i) => {
    const k = identity(r, i);
    const old = map.get(k);
    if (old === undefined || timestamp(r) >= timestamp(old)) map.set(k, r);
  });
  return Array.from(map.values());
}

function mergeState(server: Record<string, unknown[]>, client: Record<string, unknown[]>) {
  return {
    bm_inv: mergeArray(server.bm_inv || [], client.bm_inv || []),
    bm_sales: mergeArray(server.bm_sales || [], client.bm_sales || []),
    bm_recent: mergeArray(server.bm_recent || [], client.bm_recent || []).slice(-250),
  };
}

async function current(uid: string) {
  try {
    const { data, error } = await admin.from('user_sync_state')
      .select('revision,state,updated_by_installation,updated_at')
      .eq('user_id', uid)
      .maybeSingle();
    if (error) throw error;
    return data || { revision: 0, state: { bm_inv: [], bm_sales: [], bm_recent: [] }, updated_by_installation: null, updated_at: null };
  } catch (e) {
    console.error('user-sync-state', { stage: 'state_read', error: safeError(e) });
    throw e;
  }
}

async function log(uid: string, type: string, revision: number | null, installation: string, detail: Record<string, unknown> = {}) {
  const { error } = await admin.from('user_sync_events').insert({
    user_id: uid,
    event_type: type,
    revision,
    installation_id: installation || null,
    detail,
  });
  if (error) console.error('user-sync-state', { stage: 'event_log', type, error: safeError(error) });
}

async function compareAndSet(uid: string, expected: number, state: Record<string, unknown[]>, installation: string) {
  try {
    const { data, error } = await admin.rpc('user_sync_compare_and_set', {
      p_user_id: uid,
      p_expected_revision: expected,
      p_state: state,
      p_installation_id: installation || null,
    }).single();
    if (error) throw error;
    return data as {
      revision: number;
      state: Record<string, unknown[]>;
      updated_by_installation: string | null;
      updated_at: string | null;
      conflict: boolean;
    };
  } catch (e) {
    console.error('user-sync-state', { stage: 'state_compare_and_set', error: safeError(e) });
    throw e;
  }
}

Deno.serve(async req => {
  if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: cors });
  if (req.method !== 'POST') return reply({ error: 'Method not allowed' }, 405);

  try {
    const u = await user(req);
    const b = await req.json().catch(() => ({}));
    const action = String(b?.action || 'pull');
    const installation = String(b?.installation_id || '').slice(0, 200);

    if (action === 'pull') {
      const row = await current(u.id);
      await log(u.id, 'pull', Number(row.revision), installation);
      return reply(row);
    }

    if (action === 'push' || action === 'offline_replay') {
      const state = cleanState(b?.state);
      const expected = Math.max(0, Number(b?.base_revision) || 0);
      const result = await compareAndSet(u.id, expected, state, installation);
      if (result.conflict) {
        await log(u.id, 'conflict', Number(result.revision), installation, { client_revision: expected });
        return reply({ error: 'SYNC_CONFLICT', server: result }, 409);
      }
      await log(u.id, action === 'offline_replay' ? 'offline_replay' : 'push', Number(result.revision), installation);
      return reply(result);
    }

    if (action === 'merge') {
      const client = cleanState(b?.state);
      const row = await current(u.id);
      const server = cleanState(row.state);
      const merged = mergeState(server, client);
      const result = await compareAndSet(u.id, Number(row.revision), merged, installation);
      if (result.conflict) {
        await log(u.id, 'conflict', Number(result.revision), installation, { client_revision: Number(row.revision), source: 'merge' });
        return reply({ error: 'SYNC_CONFLICT', server: result }, 409);
      }
      await log(u.id, 'merge', Number(result.revision), installation, { from_revision: Number(row.revision) });
      return reply(result);
    }

    return reply({ error: 'Unsupported action' }, 400);
  } catch (e) {
    const m = msg(e);
    if (m === 'AUTH_REQUIRED') return reply({ error: 'Authentication required' }, 401);
    if (m === 'STATE_INVALID' || m === 'STATE_TOO_LARGE') return reply({ error: 'Sync state invalid or too large' }, 400);
    console.error('user-sync-state', { stage: 'request', error: safeError(e) });
    return reply({ error: 'Sync operation failed', code: 'SYNC_OPERATION_FAILED' }, 500);
  }
});
