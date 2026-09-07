import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!;
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
const BACKUP_MASTER_KEY_B64 = (Deno.env.get('MORLEY_USER_BACKUP_MASTER_KEY') || '').trim();
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false } });

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, content-type, x-google-access-token',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
};

function reply(body: unknown, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store' },
  });
}
function safeError(error: unknown) {
  if (error instanceof Error) return error.message;
  if (error && typeof error === 'object') {
    const value = error as Record<string, unknown>;
    return String(value.message || value.error_description || value.error || value.code || 'Unknown backend error');
  }
  return String(error);
}
function bytesToBase64(bytes: Uint8Array) {
  let binary = '';
  for (let i = 0; i < bytes.length; i += 0x8000) binary += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
  return btoa(binary);
}
function base64ToBytes(value: string) {
  const binary = atob(value);
  return Uint8Array.from(binary, c => c.charCodeAt(0));
}
async function sha256Bytes(bytes: Uint8Array) {
  return Array.from(new Uint8Array(await crypto.subtle.digest('SHA-256', bytes)))
    .map(b => b.toString(16).padStart(2, '0')).join('');
}
async function requireMorleyUser(req: Request) {
  const authHeader = req.headers.get('Authorization') || '';
  if (!authHeader.startsWith('Bearer ')) throw new Error('AUTH_REQUIRED');
  const token = authHeader.slice(7).trim();
  const { data, error } = await admin.auth.getUser(token);
  if (error || !data.user) throw new Error('AUTH_REQUIRED');
  return data.user;
}
async function requireGoogleIdentity(req: Request) {
  const token = (req.headers.get('X-Google-Access-Token') || '').trim();
  if (!token) throw new Error('GOOGLE_AUTH_REQUIRED');
  const res = await fetch('https://www.googleapis.com/drive/v3/about?fields=user(permissionId,emailAddress,displayName)', {
    headers: { Authorization: `Bearer ${token}` },
  });
  const json = await res.json();
  if (!res.ok || !json?.user?.permissionId) throw new Error(`GOOGLE_AUTH_INVALID:${json?.error?.message || res.status}`);
  return { token, permissionId: String(json.user.permissionId), email: json.user.emailAddress || null };
}
async function masterKeyBytes() {
  if (!BACKUP_MASTER_KEY_B64) throw new Error('BACKUP_MASTER_KEY_NOT_CONFIGURED');
  const raw = base64ToBytes(BACKUP_MASTER_KEY_B64);
  if (raw.byteLength !== 32) throw new Error('BACKUP_MASTER_KEY_INVALID');
  return raw;
}
async function deriveUserWrappingKey(userId: string) {
  const master = await masterKeyBytes();
  const baseKey = await crypto.subtle.importKey('raw', master, 'HKDF', false, ['deriveKey']);
  return crypto.subtle.deriveKey({
    name: 'HKDF',
    hash: 'SHA-256',
    salt: new TextEncoder().encode(userId),
    info: new TextEncoder().encode('morley-user-backup-v1'),
  }, baseKey, { name: 'AES-GCM', length: 256 }, false, ['encrypt', 'decrypt']);
}
async function encryptForUser(userId: string, plaintext: Uint8Array) {
  const dekRaw = crypto.getRandomValues(new Uint8Array(32));
  const dek = await crypto.subtle.importKey('raw', dekRaw, { name: 'AES-GCM' }, false, ['encrypt', 'decrypt']);
  const payloadIv = crypto.getRandomValues(new Uint8Array(12));
  const ciphertext = new Uint8Array(await crypto.subtle.encrypt({ name: 'AES-GCM', iv: payloadIv }, dek, plaintext));

  const wrappingKey = await deriveUserWrappingKey(userId);
  const wrapIv = crypto.getRandomValues(new Uint8Array(12));
  const wrappedKey = new Uint8Array(await crypto.subtle.encrypt({ name: 'AES-GCM', iv: wrapIv }, wrappingKey, dekRaw));
  dekRaw.fill(0);
  return { ciphertext, payloadIv, wrappedKey, wrapIv };
}
async function decryptForUser(userId: string, ciphertext: Uint8Array, payloadIv: Uint8Array, wrappedKey: Uint8Array, wrapIv: Uint8Array) {
  const wrappingKey = await deriveUserWrappingKey(userId);
  const dekRaw = new Uint8Array(await crypto.subtle.decrypt({ name: 'AES-GCM', iv: wrapIv }, wrappingKey, wrappedKey));
  const dek = await crypto.subtle.importKey('raw', dekRaw, { name: 'AES-GCM' }, false, ['decrypt']);
  dekRaw.fill(0);
  return new Uint8Array(await crypto.subtle.decrypt({ name: 'AES-GCM', iv: payloadIv }, dek, ciphertext));
}
async function collectUserData(userId: string) {
  const [{ data: profile, error: profileError }, { data: valuations, error: valuationError }] = await Promise.all([
    admin.from('profiles').select('id,display_name,created_at,updated_at').eq('id', userId).maybeSingle(),
    admin.from('valuation_history').select('*').eq('user_id', userId).order('created_at', { ascending: true }),
  ]);
  if (profileError) throw new Error(`PROFILE_EXPORT_FAILED:${safeError(profileError)}`);
  if (valuationError) throw new Error(`VALUATION_EXPORT_FAILED:${safeError(valuationError)}`);
  return {
    profile: profile ? { display_name: profile.display_name ?? null } : null,
    valuation_history: (valuations || []).map(({ user_id: _userId, ...row }) => row),
  };
}
async function uploadAppData(name: string, content: string, token: string) {
  const boundary = `morley_${crypto.randomUUID()}`;
  const meta = JSON.stringify({ name, parents: ['appDataFolder'], mimeType: 'application/json', appProperties: { app: 'morley-buys', kind: 'encrypted-user-backup' } });
  const body = `--${boundary}\r\nContent-Type: application/json; charset=UTF-8\r\n\r\n${meta}\r\n--${boundary}\r\nContent-Type: application/json\r\n\r\n${content}\r\n--${boundary}--`;
  const res = await fetch('https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart&fields=id,name,createdTime,size', {
    method: 'POST',
    headers: { Authorization: `Bearer ${token}`, 'Content-Type': `multipart/related; boundary=${boundary}` },
    body,
  });
  const json = await res.json();
  if (!res.ok || !json.id) throw new Error(`DRIVE_UPLOAD_FAILED:${json?.error?.message || res.status}`);
  return json;
}
async function downloadAppData(fileId: string, token: string) {
  const res = await fetch(`https://www.googleapis.com/drive/v3/files/${encodeURIComponent(fileId)}?alt=media`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok) throw new Error(`DRIVE_DOWNLOAD_FAILED:${res.status}`);
  return await res.text();
}
async function createBackup(userId: string, google: { token: string; permissionId: string; email: string | null }, reason = 'manual') {
  const backupId = crypto.randomUUID();
  const createdAt = new Date().toISOString();
  const data = await collectUserData(userId);
  const plaintext = new TextEncoder().encode(JSON.stringify({
    format: 'morley-user-backup-v1',
    format_version: 1,
    backup_id: backupId,
    owner_user_id: userId,
    google_permission_id: google.permissionId,
    created_at: createdAt,
    data,
  }));
  const encrypted = await encryptForUser(userId, plaintext);
  plaintext.fill(0);
  const envelope = {
    format: 'morley-user-backup-encrypted-v1',
    format_version: 1,
    backup_id: backupId,
    cipher: 'AES-256-GCM',
    iv: bytesToBase64(encrypted.payloadIv),
    ciphertext: bytesToBase64(encrypted.ciphertext),
  };
  const encoded = new TextEncoder().encode(JSON.stringify(envelope));
  const hash = await sha256Bytes(encrypted.ciphertext);
  const fileName = `morley-user-backup-${createdAt.replace(/[:.]/g, '-')}-${backupId.slice(0, 8)}.mbak`;

  let driveFile: Record<string, unknown> | null = null;
  try {
    driveFile = await uploadAppData(fileName, new TextDecoder().decode(encoded), google.token);
    const { error: metaError } = await admin.from('user_drive_backups').insert({
      id: backupId,
      user_id: userId,
      google_permission_id: google.permissionId,
      drive_file_id: String(driveFile.id),
      drive_file_name: fileName,
      format_version: 1,
      ciphertext_sha256: hash,
      byte_size: encoded.byteLength,
      status: 'ready',
    });
    if (metaError) throw new Error(`BACKUP_METADATA_FAILED:${safeError(metaError)}`);
    const { error: keyError } = await admin.from('user_drive_backup_keys').insert({
      backup_id: backupId,
      wrapped_key: bytesToBase64(encrypted.wrappedKey),
      wrap_iv: bytesToBase64(encrypted.wrapIv),
    });
    if (keyError) throw new Error(`BACKUP_KEY_STORE_FAILED:${safeError(keyError)}`);
    await admin.from('user_drive_backup_events').insert({ user_id: userId, backup_id: backupId, event_type: 'backup_created', detail: { reason, google_email: google.email } });
    return { id: backupId, created_at: createdAt, drive_file_name: fileName, byte_size: encoded.byteLength };
  } catch (error) {
    if (driveFile?.id) {
      await fetch(`https://www.googleapis.com/drive/v3/files/${encodeURIComponent(String(driveFile.id))}`, { method: 'DELETE', headers: { Authorization: `Bearer ${google.token}` } }).catch(() => undefined);
    }
    await admin.from('user_drive_backups').delete().eq('id', backupId).eq('user_id', userId).catch(() => undefined);
    await admin.from('user_drive_backup_events').insert({ user_id: userId, backup_id: null, event_type: 'backup_failed', detail: { reason, error: safeError(error) } });
    throw error;
  }
}
async function restoreBackup(userId: string, google: { token: string; permissionId: string; email: string | null }, backupId: string) {
  const { data: backup, error: backupError } = await admin.from('user_drive_backups')
    .select('id,user_id,google_permission_id,drive_file_id,ciphertext_sha256,status')
    .eq('id', backupId).eq('user_id', userId).maybeSingle();
  if (backupError) throw new Error(`BACKUP_LOOKUP_FAILED:${safeError(backupError)}`);
  if (!backup) throw new Error('BACKUP_NOT_FOUND_FOR_USER');
  if (backup.google_permission_id !== google.permissionId) throw new Error('GOOGLE_ACCOUNT_MISMATCH');
  if (backup.status !== 'ready') throw new Error('BACKUP_NOT_READY');

  const { data: keyRow, error: keyError } = await admin.from('user_drive_backup_keys').select('wrapped_key,wrap_iv').eq('backup_id', backupId).maybeSingle();
  if (keyError || !keyRow) throw new Error('BACKUP_KEY_NOT_FOUND');

  await admin.from('user_drive_backup_events').insert({ user_id: userId, backup_id: backupId, event_type: 'restore_started', detail: {} });
  await admin.from('user_drive_backups').update({ status: 'restoring' }).eq('id', backupId).eq('user_id', userId);
  try {
    const raw = await downloadAppData(backup.drive_file_id, google.token);
    const envelope = JSON.parse(raw);
    if (envelope?.format !== 'morley-user-backup-encrypted-v1' || envelope?.backup_id !== backupId || !envelope?.iv || !envelope?.ciphertext) throw new Error('BACKUP_ENVELOPE_INVALID');
    const ciphertext = base64ToBytes(envelope.ciphertext);
    if ((await sha256Bytes(ciphertext)) !== backup.ciphertext_sha256) throw new Error('BACKUP_HASH_MISMATCH');
    const plaintextBytes = await decryptForUser(userId, ciphertext, base64ToBytes(envelope.iv), base64ToBytes(keyRow.wrapped_key), base64ToBytes(keyRow.wrap_iv));
    const payload = JSON.parse(new TextDecoder().decode(plaintextBytes));
    plaintextBytes.fill(0);
    if (payload?.format !== 'morley-user-backup-v1' || payload?.owner_user_id !== userId || payload?.google_permission_id !== google.permissionId || payload?.backup_id !== backupId) throw new Error('BACKUP_OWNER_MISMATCH');

    // Always create a recovery point before changing user-owned data.
    const safetyBackup = await createBackup(userId, google, `pre-restore:${backupId}`);

    if (payload.data?.profile && Object.prototype.hasOwnProperty.call(payload.data.profile, 'display_name')) {
      const { error } = await admin.from('profiles').update({ display_name: payload.data.profile.display_name }).eq('id', userId);
      if (error) throw new Error(`PROFILE_RESTORE_FAILED:${safeError(error)}`);
    }

    const restoredValuations = Array.isArray(payload.data?.valuation_history) ? payload.data.valuation_history : [];
    const { error: deleteError } = await admin.from('valuation_history').delete().eq('user_id', userId);
    if (deleteError) throw new Error(`VALUATION_RESTORE_CLEAR_FAILED:${safeError(deleteError)}`);
    if (restoredValuations.length) {
      const rows = restoredValuations.map((row: Record<string, unknown>) => ({ ...row, user_id: userId }));
      const { error: insertError } = await admin.from('valuation_history').insert(rows);
      if (insertError) throw new Error(`VALUATION_RESTORE_WRITE_FAILED:${safeError(insertError)}`);
    }

    const restoredAt = new Date().toISOString();
    await admin.from('user_drive_backups').update({ status: 'ready', restored_at: restoredAt }).eq('id', backupId).eq('user_id', userId);
    await admin.from('user_drive_backup_events').insert({ user_id: userId, backup_id: backupId, event_type: 'restore_completed', detail: { safety_backup_id: safetyBackup.id } });
    return { restored_at: restoredAt, safety_backup_id: safetyBackup.id };
  } catch (error) {
    await admin.from('user_drive_backups').update({ status: 'ready' }).eq('id', backupId).eq('user_id', userId);
    await admin.from('user_drive_backup_events').insert({ user_id: userId, backup_id: backupId, event_type: 'restore_failed', detail: { error: safeError(error) } });
    throw error;
  }
}

Deno.serve(async (req) => {
  if (req.method === 'OPTIONS') return new Response(null, { status: 204, headers: corsHeaders });
  if (req.method !== 'POST') return reply({ error: 'Method not allowed' }, 405);
  try {
    const user = await requireMorleyUser(req);
    const google = await requireGoogleIdentity(req);
    const body = await req.json().catch(() => ({}));
    const action = String(body?.action || 'list');

    if (action === 'list') {
      const { data, error } = await admin.from('user_drive_backups')
        .select('id,drive_file_name,byte_size,status,created_at,restored_at')
        .eq('user_id', user.id).eq('google_permission_id', google.permissionId)
        .order('created_at', { ascending: false }).limit(50);
      if (error) throw new Error(`BACKUP_LIST_FAILED:${safeError(error)}`);
      return reply({ backups: data || [] });
    }
    if (action === 'backup') {
      const result = await createBackup(user.id, google, 'manual');
      return reply({ backup: result }, 201);
    }
    if (action === 'restore') {
      if (body?.confirm !== 'RESTORE') return reply({ error: 'Restore confirmation required' }, 400);
      const backupId = String(body?.backup_id || '').trim();
      if (!backupId) return reply({ error: 'backup_id is required' }, 400);
      const result = await restoreBackup(user.id, google, backupId);
      return reply({ restore: result });
    }
    return reply({ error: 'Unsupported action' }, 400);
  } catch (error) {
    const message = safeError(error);
    if (message === 'AUTH_REQUIRED') return reply({ error: 'Authentication required' }, 401);
    if (message === 'GOOGLE_AUTH_REQUIRED') return reply({ error: 'Google Drive authorization required' }, 401);
    if (message.startsWith('GOOGLE_AUTH_INVALID')) return reply({ error: 'Google Drive authorization is invalid or expired' }, 401);
    if (message === 'GOOGLE_ACCOUNT_MISMATCH' || message === 'BACKUP_NOT_FOUND_FOR_USER' || message === 'BACKUP_OWNER_MISMATCH') return reply({ error: 'This backup is not available to the logged-in user' }, 403);
    if (message === 'BACKUP_MASTER_KEY_NOT_CONFIGURED' || message === 'BACKUP_MASTER_KEY_INVALID') return reply({ error: 'Encrypted backup service is not configured' }, 503);
    console.error('user-google-drive-backup', message);
    return reply({ error: 'Backup operation failed' }, 500);
  }
});
