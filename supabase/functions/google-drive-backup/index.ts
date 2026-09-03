import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!;
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
const DRIVE_FOLDER_ID = (Deno.env.get('GOOGLE_DRIVE_BACKUP_FOLDER_ID') || '').trim();
const BACKUP_SECRET = (Deno.env.get('MORLEY_BACKUP_SECRET') || '').trim();
const DRIVE_OAUTH_CLIENT_ID = (Deno.env.get('GOOGLE_DRIVE_OAUTH_CLIENT_ID') || '').trim();
const DRIVE_OAUTH_CLIENT_SECRET = (Deno.env.get('GOOGLE_DRIVE_OAUTH_CLIENT_SECRET') || '').trim();
const DRIVE_OAUTH_REFRESH_TOKEN = (Deno.env.get('GOOGLE_DRIVE_OAUTH_REFRESH_TOKEN') || '').trim();
const RETENTION_DAYS = 30;
const RETENTION_MIN_KEEP = 7;

const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false } });
const DEFAULT_TABLES = [
  'profiles','app_config','valuation_history','devices','laptop_models',
  'support_tickets','support_ticket_messages','support_ticket_events',
  'support_ticket_internal_notes','support_ticket_attachments','admin_audit_log',
  'announcements','notification_jobs'
];

function safeError(error: unknown) {
  if (error instanceof Error) return error.message;
  if (error && typeof error === 'object') {
    const c = error as Record<string, unknown>;
    return String(c.message || c.error_description || c.error || c.code || 'Unknown backend error');
  }
  return String(error);
}
async function sha256(text: string) {
  return Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', new TextEncoder().encode(text))))
    .map(b => b.toString(16).padStart(2,'0')).join('');
}
async function oauthUserToken() {
  if (!DRIVE_OAUTH_CLIENT_ID || !DRIVE_OAUTH_CLIENT_SECRET || !DRIVE_OAUTH_REFRESH_TOKEN) {
    throw new Error('Google Drive user OAuth is not configured');
  }
  const res = await fetch('https://oauth2.googleapis.com/token', {
    method: 'POST',
    headers: {'Content-Type':'application/x-www-form-urlencoded'},
    body: new URLSearchParams({
      client_id: DRIVE_OAUTH_CLIENT_ID,
      client_secret: DRIVE_OAUTH_CLIENT_SECRET,
      refresh_token: DRIVE_OAUTH_REFRESH_TOKEN,
      grant_type: 'refresh_token'
    })
  });
  const json = await res.json();
  if (!res.ok || !json.access_token) throw new Error(`Google OAuth refresh failed: ${json.error_description || json.error || res.status}`);
  return json.access_token as string;
}
async function collectTable(table: string) {
  const rows: unknown[] = []; let from = 0; const size = 1000;
  while (true) {
    const { data, error } = await admin.from(table).select('*').range(from, from + size - 1);
    if (error) throw new Error(`Export ${table} failed: ${safeError(error)}`);
    rows.push(...(data || []));
    if (!data || data.length < size) break;
    from += size;
  }
  return { table, rows };
}
async function upload(name: string, payload: string, token: string) {
  const boundary = `morley_${crypto.randomUUID()}`;
  const meta = JSON.stringify({ name, parents: [DRIVE_FOLDER_ID], mimeType: 'application/json' });
  const body = `--${boundary}\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n${meta}\r\n--${boundary}\r\nContent-Type: application/json\r\n\r\n${payload}\r\n--${boundary}--`;
  const res = await fetch('https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&supportsAllDrives=true&fields=id,name,createdTime,size', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': `multipart/related; boundary=${boundary}` },
    body
  });
  const json = await res.json();
  if (!res.ok) throw new Error(`Drive upload failed: ${json.error?.message || res.status}`);
  return json;
}
async function cleanupOldBackups(token: string) {
  const q = `'${DRIVE_FOLDER_ID}' in parents and trashed = false and name contains 'morley-backup-'`;
  const url = new URL('https://www.googleapis.com/drive/v3/files');
  url.searchParams.set('q', q);
  url.searchParams.set('orderBy', 'createdTime desc');
  url.searchParams.set('pageSize', '1000');
  url.searchParams.set('fields', 'files(id,name,createdTime)');
  const res = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
  const json = await res.json();
  if (!res.ok) throw new Error(`Drive retention scan failed: ${json.error?.message || res.status}`);
  const files = (json.files || []).filter((f: any) => /^morley-backup-.*\.json$/.test(String(f.name || '')));
  const cutoff = Date.now() - RETENTION_DAYS * 24 * 60 * 60 * 1000;
  let trashed = 0;
  for (let i = RETENTION_MIN_KEEP; i < files.length; i++) {
    const f = files[i];
    const created = Date.parse(String(f.createdTime || ''));
    if (!Number.isFinite(created) || created >= cutoff) continue;
    const del = await fetch(`https://www.googleapis.com/drive/v3/files/${encodeURIComponent(f.id)}?supportsAllDrives=true&fields=id,trashed`, {
      method: 'PATCH',
      headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' },
      body: JSON.stringify({ trashed: true })
    });
    if (!del.ok) {
      const d = await del.json().catch(() => ({}));
      throw new Error(`Drive retention cleanup failed: ${d.error?.message || del.status}`);
    }
    trashed++;
  }
  return { trashed, scanned: files.length };
}

Deno.serve(async (req: Request) => {
  const reply = (body: unknown, status = 200) => new Response(JSON.stringify(body), {
    status,
    headers: { 'Content-Type':'application/json', 'Cache-Control':'no-store', 'X-Content-Type-Options':'nosniff' }
  });
  if (req.method !== 'POST') return reply({ error: 'POST required' }, 405);
  let phase = 'authorize';
  try {
    const supplied = (req.headers.get('x-morley-backup-secret') || '').trim();
    let actor: string | null = null;
    if (BACKUP_SECRET && supplied && supplied === BACKUP_SECRET) {
      actor = 'scheduler';
    } else if (supplied) {
      const { data: matches, error: matchError } = await admin.rpc('morley_backup_scheduler_secret_matches', { candidate: supplied });
      if (!matchError && matches === true) actor = 'scheduler';
      else if (!(req.headers.get('Authorization') || '')) return reply({ error: 'Invalid backup secret' }, 401);
    }
    if (!actor) {
      const token = (req.headers.get('Authorization') || '').replace(/^Bearer\s+/i, '');
      if (!token) return reply({ error: 'Authentication required' }, 401);
      const { data: { user }, error } = await admin.auth.getUser(token);
      if (error || !user) return reply({ error: 'Invalid session' }, 401);
      const { data: profile } = await admin.from('profiles').select('role,is_enabled').eq('id', user.id).single();
      if (!profile?.is_enabled || profile.role !== 'admin') return reply({ error: 'Administrator access required' }, 403);
      actor = user.id;
    }
    const body = await req.json().catch(() => ({}));
    if (body?.action && body.action !== 'backup') return reply({ error: 'Restore is deliberately not available from this function' }, 400);
    const tables = Array.isArray(body?.tables) && body.tables.length
      ? body.tables.filter((x: unknown) => DEFAULT_TABLES.includes(String(x)))
      : DEFAULT_TABLES;
    phase = 'export';
    const exported = [];
    for (const table of tables) exported.push(await collectTable(table));
    const createdAt = new Date().toISOString();
    const document = {
      format: 'morley-backup-v1', created_at: createdAt,
      source_project: new URL(SUPABASE_URL).hostname,
      tables: Object.fromEntries(exported.map(x => [x.table, x.rows]))
    };
    const canonical = JSON.stringify(document);
    const digest = await sha256(canonical);
    const envelope = JSON.stringify({ ...document, sha256: digest }, null, 2);
    const name = `morley-backup-${createdAt.replace(/[:.]/g,'-')}.json`;
    phase = 'google-auth';
    if (!DRIVE_FOLDER_ID) throw new Error('Google Drive backup folder is not configured');
    const token = await oauthUserToken();
    phase = 'drive-upload';
    const drive = await upload(name, envelope, token);
    let retention = { trashed: 0, scanned: 0 };
    let retentionWarning: string | null = null;
    phase = 'retention';
    try { retention = await cleanupOldBackups(token); }
    catch (e) { retentionWarning = safeError(e); }
    phase = 'audit';
    const { error: auditError } = await admin.from('admin_audit_log').insert({
      actor_user_id: actor === 'scheduler' ? null : actor,
      action: 'google_drive_backup_created', target_type: 'backup', target_id: drive.id,
      details: { name, sha256: digest, tables: tables.length, trigger: actor, google_auth_mode: 'user-oauth', retention }
    });
    if (auditError) console.error('Backup audit insert failed', safeError(auditError));
    return reply({ ok: true, name, file_id: drive.id, sha256: digest, created_at: createdAt, google_auth_mode: 'user-oauth', retention, retention_warning: retentionWarning });
  } catch (error) {
    return reply({ error: safeError(error), phase }, 500);
  }
});
