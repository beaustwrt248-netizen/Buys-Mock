import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!;
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
const ANON_KEY = Deno.env.get('SUPABASE_ANON_KEY')!;
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false } });

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, content-type',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
};

function reply(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' },
  });
}

function message(error: unknown) {
  return error instanceof Error ? error.message : String(error);
}

function decodeJwtPayload(token: string) {
  const part = token.split('.')[1] || '';
  const normalized = part.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized + '='.repeat((4 - normalized.length % 4) % 4);
  return JSON.parse(atob(padded));
}

async function requireUser(req: Request) {
  const auth = req.headers.get('Authorization') || '';
  if (!auth.startsWith('Bearer ')) throw new Error('AUTH_REQUIRED');
  const token = auth.slice(7).trim();
  const { data, error } = await admin.auth.getUser(token);
  if (error || !data.user) throw new Error('AUTH_REQUIRED');
  const claims = decodeJwtPayload(token);
  const sessionId = String(claims?.session_id || '').trim();
  if (!sessionId) throw new Error('SESSION_ID_REQUIRED');
  return { user: data.user, token, sessionId };
}

function cleanText(value: unknown, max = 160) {
  return String(value || '').trim().slice(0, max);
}

async function heartbeat(userId: string, sessionId: string, body: Record<string, unknown>) {
  const installationId = cleanText(body.installation_id, 200);
  if (!installationId) throw new Error('INSTALLATION_ID_REQUIRED');
  const platform = cleanText(body.platform || 'web', 40) || 'web';
  const deviceName = cleanText(body.device_name, 160) || null;
  const appVersion = cleanText(body.app_version, 80) || null;

  const { data: existing, error: lookupError } = await admin.from('user_session_devices')
    .select('id,trusted_at,revoked_at,first_seen_at')
    .eq('user_id', userId)
    .eq('session_id', sessionId)
    .maybeSingle();
  if (lookupError) throw lookupError;
  const isNew = !existing;

  const row = {
    user_id: userId,
    session_id: sessionId,
    installation_id: installationId,
    platform,
    device_name: deviceName,
    app_version: appVersion,
    last_seen_at: new Date().toISOString(),
    revoked_at: null,
  };
  const { data, error } = await admin.from('user_session_devices')
    .upsert(row, { onConflict: 'user_id,session_id' })
    .select('id,session_id,installation_id,platform,device_name,app_version,first_seen_at,last_seen_at,trusted_at,revoked_at')
    .single();
  if (error) throw error;

  if (isNew) {
    await admin.from('user_security_events').insert({
      user_id: userId,
      session_id: sessionId,
      event_type: 'new_session',
      detail: { installation_id: installationId, platform, device_name: deviceName, app_version: appVersion },
    });
  }
  return { device: data, is_new: isNew };
}

async function listCenter(userId: string, sessionId: string) {
  const [{ data: devices, error: deviceError }, { data: events, error: eventError }] = await Promise.all([
    admin.from('user_session_devices')
      .select('id,session_id,installation_id,platform,device_name,app_version,first_seen_at,last_seen_at,trusted_at,revoked_at')
      .eq('user_id', userId)
      .order('last_seen_at', { ascending: false })
      .limit(50),
    admin.from('user_security_events')
      .select('id,session_id,event_type,detail,created_at')
      .eq('user_id', userId)
      .order('created_at', { ascending: false })
      .limit(100),
  ]);
  if (deviceError) throw deviceError;
  if (eventError) throw eventError;
  return {
    current_session_id: sessionId,
    devices: (devices || []).map(d => ({ ...d, current: d.session_id === sessionId })),
    events: events || [],
  };
}

async function trustCurrent(userId: string, sessionId: string) {
  const trustedAt = new Date().toISOString();
  const { data, error } = await admin.from('user_session_devices')
    .update({ trusted_at: trustedAt })
    .eq('user_id', userId)
    .eq('session_id', sessionId)
    .select('id,trusted_at')
    .maybeSingle();
  if (error) throw error;
  if (!data) throw new Error('SESSION_DEVICE_NOT_FOUND');
  await admin.from('user_security_events').insert({
    user_id: userId,
    session_id: sessionId,
    event_type: 'trusted_device',
    detail: {},
  });
  return { trusted_at: trustedAt };
}

async function signOutOthers(userId: string, sessionId: string, token: string) {
  const response = await fetch(`${SUPABASE_URL}/auth/v1/logout?scope=others`, {
    method: 'POST',
    headers: { apikey: ANON_KEY, Authorization: `Bearer ${token}` },
  });
  if (!response.ok) throw new Error(`AUTH_SIGNOUT_OTHERS_FAILED:${response.status}`);
  const now = new Date().toISOString();
  const { error } = await admin.from('user_session_devices')
    .update({ revoked_at: now })
    .eq('user_id', userId)
    .neq('session_id', sessionId)
    .is('revoked_at', null);
  if (error) throw error;
  await admin.from('user_security_events').insert({
    user_id: userId,
    session_id: sessionId,
    event_type: 'signout_others',
    detail: { note: 'Other refresh sessions revoked; outstanding access JWTs expire normally.' },
  });
  return { signed_out_others: true, revoked_at: now };
}

Deno.serve(async req => {
  if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: corsHeaders });
  if (req.method !== 'POST') return reply({ error: 'Method not allowed' }, 405);
  try {
    const { user, token, sessionId } = await requireUser(req);
    const body = await req.json().catch(() => ({}));
    const action = String(body?.action || 'list');
    if (action === 'heartbeat') return reply(await heartbeat(user.id, sessionId, body));
    if (action === 'list') return reply(await listCenter(user.id, sessionId));
    if (action === 'trust_current') return reply(await trustCurrent(user.id, sessionId));
    if (action === 'signout_others') return reply(await signOutOthers(user.id, sessionId, token));
    return reply({ error: 'Unsupported action' }, 400);
  } catch (error) {
    const m = message(error);
    if (m === 'AUTH_REQUIRED' || m === 'SESSION_ID_REQUIRED') return reply({ error: 'Authentication required' }, 401);
    if (m === 'INSTALLATION_ID_REQUIRED') return reply({ error: 'Installation identifier required' }, 400);
    console.error('user-security-center', m);
    return reply({ error: 'Security operation failed' }, 500);
  }
});
