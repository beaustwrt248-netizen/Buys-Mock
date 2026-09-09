import "jsr:@supabase/functions-js/edge-runtime.d.ts";
import { createClient } from "jsr:@supabase/supabase-js@2";

const SUPABASE_URL = Deno.env.get('SUPABASE_URL')!;
const SERVICE_ROLE = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')!;
const admin = createClient(SUPABASE_URL, SERVICE_ROLE, { auth: { persistSession: false } });

const MAX_CLIENT_STATE_BYTES = 2_000_000;
const MAX_BACKUPS_PER_USER = 30;
const CLIENT_STATE_KEYS = new Set(['bm_inv', 'bm_sales', 'bm_recent']);

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
  return {
    token,
    permissionId: String(json.user.permissionId),
    email: json.user.emailAddress || null,
    displayName: json.user.displayName || null,
  };
}

async function masterKeyBytes() {
  const { data, error } = await admin
    .from('user_drive_backup_master_keys')
    .select('key_b64')
    .eq('key_id', 1)
    .single();
  if (error || !data?.key_b64) throw new Error('BACKUP_MASTER_KEY_NOT_CONFIGURED');
  const raw = base64ToBytes(String(data.key_b64));
  if (raw.byteLength !== 32) throw new Error('BACKUP_MASTER_KEY_INVALID');
  return raw;
}

async function deriveUserWrappingKey(userId: string) {
  const master = await masterKeyBytes();
  try {
    const baseKey = await crypto.subtle.importKey('raw', master, 'HKDF', false, ['deriveKey']);
    return await crypto.subtle.deriveKey({
      name: 'HKDF',
      hash: 'SHA-256',
      salt: new TextEncoder().encode(userId),
      info: new TextEncoder().encode('morley-user-backup-v1'),
    }, baseKey, { name: 'AES-GCM', length: 256 }, false, ['encrypt', 'decrypt']);
  } finally {
    master.fill(0);
  }
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
  try {
    const dek = await crypto.subtle.importKey('raw', dekRaw, { name: 'AES-GCM' }, false, ['decrypt']);
    return new Uint8Array(await crypto.subtle.decrypt({ name: 'AES-GCM', iv: payloadIv }, dek, ciphertext));
  } finally {
    dekRaw.fill(0);
  }
}

function sanitizeClientState(input: unknown) {
  if (input == null) return null;
  if (!input || typeof input !== 'object' || Array.isArray(input)) throw new Error('CLIENT_STATE_INVALID');
  const out: Record<string, unknown> = {};
  for (const [key, value] of Object.entries(input as Record<string, unknown>)) {
    if (!CLIENT_STATE_KEYS.has(key)) continue;
    out[key] = value;
  }
  const encoded = new TextEncoder().encode(JSON.stringify(out));
  if (encoded.byteLength > MAX_CLIENT_STATE_BYTES) throw new Error('CLIENT_STATE_TOO_LARGE');
  return out;
}

function validatePayloadSchema(payload: any, userId: string, permissionId: string, backupId: string) {
  if (payload?.format !== 'morley-user-backup-v1' || payload?.format_version !== 1 || payload?.backup_id !== backupId) {
    throw new Error('BACKUP_PAYLOAD_INVALID');
  }
  if (payload?.owner_user_id !== userId || payload?.google_permission_id !== permissionId) {
    throw new Error('BACKUP_OWNER_MISMATCH');
  }
  if (!payload?.data || typeof payload.data !== 'object' || Array.isArray(payload.data)) throw new Error('BACKUP_PAYLOAD_INVALID');
  if (payload.data.profile != null && (typeof payload.data.profile !== 'object' || Array.isArray(payload.data.profile))) throw new Error('BACKUP_PAYLOAD_INVALID');
  if (!Array.isArray(payload.data.valuation_history)) throw new Error('BACKUP_PAYLOAD_INVALID');
  if (payload.client_state != null && (typeof payload.client_state !== 'object' || Array.isArray(payload.client_state))) throw new Error('BACKUP_PAYLOAD_INVALID');
  return payload;
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
  const meta = JSON.stringify({
    name,
    parents: ['appDataFolder'],
    mimeType: 'application/json',
    appProperties: { app: 'morley-buys', kind: 'encrypted-user-backup' },
  });
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

async function deleteDriveFile(fileId: string, token: string) {
  const res = await fetch(`https://www.googleapis.com/drive/v3/files/${encodeURIComponent(fileId)}`, {
    method: 'DELETE',
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!res.ok && res.status !== 404) throw new Error(`DRIVE_DELETE_FAILED:${res.status}`);
}

async function decodeRemoteBackup(
  userId: string,
  permissionId: string,
  backupId: string,
  raw: string,
  expectedHash: string,
  wrappedKeyB64: string,
  wrapIvB64: string,
) {
  let envelope: any;
  try { envelope = JSON.parse(raw); } catch { throw new Error('BACKUP_ENVELOPE_INVALID'); }
  if (envelope?.format !== 'morley-user-backup-encrypted-v1' || envelope?.format_version !== 1 || envelope?.backup_id !== backupId || envelope?.cipher !== 'AES-256-GCM' || !envelope?.iv || !envelope?.ciphertext) {
    throw new Error('BACKUP_ENVELOPE_INVALID');
  }
  let ciphertext: Uint8Array;
  try { ciphertext = base64ToBytes(String(envelope.ciphertext)); } catch { throw new Error('BACKUP_ENVELOPE_INVALID'); }
  if ((await sha256Bytes(ciphertext)) !== expectedHash) throw new Error('BACKUP_HASH_MISMATCH');
  let plaintextBytes: Uint8Array;
  try {
    plaintextBytes = await decryptForUser(
      userId,
      ciphertext,
      base64ToBytes(String(envelope.iv)),
      base64ToBytes(wrappedKeyB64),
      base64ToBytes(wrapIvB64),
    );
  } catch {
    throw new Error('BACKUP_DECRYPT_FAILED');
  }
  try {
    let payload: any;
    try { payload = JSON.parse(new TextDecoder().decode(plaintextBytes)); } catch { throw new Error('BACKUP_PAYLOAD_INVALID'); }
    return validatePayloadSchema(payload, userId, permissionId, backupId);
  } finally {
    plaintextBytes.fill(0);
  }
}

async function verifyUploadedBackup(
  userId: string,
  google: { token: string; permissionId: string },
  backupId: string,
  driveFileId: string,
  expectedHash: string,
  wrappedKeyB64: string,
  wrapIvB64: string,
) {
  const raw = await downloadAppData(driveFileId, google.token);
  await decodeRemoteBackup(userId, google.permissionId, backupId, raw, expectedHash, wrappedKeyB64, wrapIvB64);
}

async function pruneOldBackups(userId: string, google: { token: string; permissionId: string }) {
  const { data, error } = await admin.from('user_drive_backups')
    .select('id,drive_file_id')
    .eq('user_id', userId)
    .eq('google_permission_id', google.permissionId)
    .eq('status', 'ready')
    .order('created_at', { ascending: false });
  if (error) return;
  const stale = (data || []).slice(MAX_BACKUPS_PER_USER);
  for (const backup of stale) {
    try { await deleteDriveFile(backup.drive_file_id, google.token); } catch {}
    await admin.from('user_drive_backups').delete().eq('id', backup.id).eq('user_id', userId);
  }
}

async function createBackup(
  userId: string,
  google: { token: string; permissionId: string; email: string | null },
  reason = 'manual',
  clientState: unknown = null,
) {
  const backupId = crypto.randomUUID();
  const createdAt = new Date().toISOString();
  const data = await collectUserData(userId);
  const sanitizedClientState = sanitizeClientState(clientState);
  const plaintext = new TextEncoder().encode(JSON.stringify({
    format: 'morley-user-backup-v1',
    format_version: 1,
    backup_id: backupId,
    owner_user_id: userId,
    google_permission_id: google.permissionId,
    created_at: createdAt,
    data,
    client_state: sanitizedClientState,
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
  const wrappedKeyB64 = bytesToBase64(encrypted.wrappedKey);
  const wrapIvB64 = bytesToBase64(encrypted.wrapIv);
  const fileName = `morley-user-backup-${createdAt.replace(/[:.]/g, '-')}-${backupId.slice(0, 8)}.mbak`;

  let driveFile: Record<string, unknown> | null = null;
  let metadataCreated = false;
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
      status: 'creating',
    });
    if (metaError) throw new Error(`BACKUP_METADATA_FAILED:${safeError(metaError)}`);
    metadataCreated = true;

    const { error: keyError } = await admin.from('user_drive_backup_keys').insert({
      backup_id: backupId,
      wrapped_key: wrappedKeyB64,
      wrap_iv: wrapIvB64,
    });
    if (keyError) throw new Error(`BACKUP_KEY_STORE_FAILED:${safeError(keyError)}`);

    await verifyUploadedBackup(userId, google, backupId, String(driveFile.id), hash, wrappedKeyB64, wrapIvB64);
    const { error: readyError } = await admin.from('user_drive_backups')
      .update({ status: 'ready' }).eq('id', backupId).eq('user_id', userId).eq('status', 'creating');
    if (readyError) throw new Error(`BACKUP_READY_FAILED:${safeError(readyError)}`);

    await admin.from('user_drive_backup_events').insert({
      user_id: userId,
      backup_id: backupId,
      event_type: 'backup_created',
      detail: { reason, google_email: google.email, client_state_included: Boolean(sanitizedClientState), integrity_verified: true },
    });
    await pruneOldBackups(userId, google);
    return { id: backupId, created_at: createdAt, drive_file_name: fileName, byte_size: encoded.byteLength, integrity_verified: true };
  } catch (error) {
    if (metadataCreated) {
      try { await admin.from('user_drive_backups').update({ status: 'failed' }).eq('id', backupId).eq('user_id', userId); } catch {}
    } else if (driveFile?.id) {
      try { await deleteDriveFile(String(driveFile.id), google.token); } catch {}
    }
    await admin.from('user_drive_backup_events').insert({
      user_id: userId,
      backup_id: metadataCreated ? backupId : null,
      event_type: 'backup_failed',
      detail: { reason, error: safeError(error), integrity_verified: false },
    });
    const message = safeError(error);
    if (message.startsWith('BACKUP_ENVELOPE_') || message.startsWith('BACKUP_HASH_') || message.startsWith('BACKUP_DECRYPT_') || message.startsWith('BACKUP_PAYLOAD_') || message === 'BACKUP_OWNER_MISMATCH') {
      throw new Error(`BACKUP_INTEGRITY_VERIFICATION_FAILED:${message}`);
    }
    throw error;
  }
}

async function loadBackupPayload(userId: string, google: { token: string; permissionId: string }, backupId: string) {
  const { data: backup, error: backupError } = await admin.from('user_drive_backups')
    .select('id,user_id,google_permission_id,drive_file_id,ciphertext_sha256,status,created_at,byte_size,drive_file_name')
    .eq('id', backupId).eq('user_id', userId).maybeSingle();
  if (backupError) throw new Error(`BACKUP_LOOKUP_FAILED:${safeError(backupError)}`);
  if (!backup) throw new Error('BACKUP_NOT_FOUND_FOR_USER');
  if (backup.google_permission_id !== google.permissionId) throw new Error('GOOGLE_ACCOUNT_MISMATCH');
  if (!['ready', 'restoring'].includes(backup.status)) throw new Error('BACKUP_NOT_READY');

  const { data: keyRow, error: keyError } = await admin.from('user_drive_backup_keys')
    .select('wrapped_key,wrap_iv').eq('backup_id', backupId).maybeSingle();
  if (keyError || !keyRow) throw new Error('BACKUP_KEY_NOT_FOUND');
  const raw = await downloadAppData(backup.drive_file_id, google.token);
  const payload = await decodeRemoteBackup(
    userId,
    google.permissionId,
    backupId,
    raw,
    String(backup.ciphertext_sha256),
    String(keyRow.wrapped_key),
    String(keyRow.wrap_iv),
  );
  return { backup, payload };
}

async function restorePreview(userId: string, google: { token: string; permissionId: string }, backupId: string) {
  const { backup, payload } = await loadBackupPayload(userId, google, backupId);
  return {
    id: backup.id,
    created_at: backup.created_at,
    byte_size: backup.byte_size,
    drive_file_name: backup.drive_file_name,
    changes: {
      profile_display_name: Object.prototype.hasOwnProperty.call(payload.data?.profile || {}, 'display_name'),
      valuation_history_rows: Array.isArray(payload.data?.valuation_history) ? payload.data.valuation_history.length : 0,
      local_inventory: Array.isArray(payload.client_state?.bm_inv) ? payload.client_state.bm_inv.length : 0,
      local_sales: Array.isArray(payload.client_state?.bm_sales) ? payload.client_state.bm_sales.length : 0,
      local_recent: Array.isArray(payload.client_state?.bm_recent) ? payload.client_state.bm_recent.length : 0,
    },
  };
}

async function restoreValuationHistory(userId: string, restoredValuations: Record<string, unknown>[]) {
  if (!restoredValuations.length) {
    const { error } = await admin.from('valuation_history').delete().eq('user_id', userId);
    if (error) throw new Error(`VALUATION_RESTORE_CLEAR_FAILED:${safeError(error)}`);
    return;
  }

  const rows = restoredValuations.map((row) => {
    const id = String(row?.id || '').trim();
    if (!id) throw new Error('VALUATION_RESTORE_INVALID_ID');
    return { ...row, id, user_id: userId };
  });
  const ids = [...new Set(rows.map(row => row.id))];
  if (ids.length !== rows.length) throw new Error('VALUATION_RESTORE_DUPLICATE_ID');

  const { error: upsertError } = await admin.from('valuation_history').upsert(rows, { onConflict: 'id' });
  if (upsertError) throw new Error(`VALUATION_RESTORE_WRITE_FAILED:${safeError(upsertError)}`);

  const quotedIds = ids.map(id => `"${id.replaceAll('"', '\\"')}"`).join(',');
  const { error: pruneError } = await admin.from('valuation_history')
    .delete()
    .eq('user_id', userId)
    .not('id', 'in', `(${quotedIds})`);
  if (pruneError) throw new Error(`VALUATION_RESTORE_PRUNE_FAILED:${safeError(pruneError)}`);
}

async function restoreBackup(
  userId: string,
  google: { token: string; permissionId: string; email: string | null },
  backupId: string,
  currentClientState: unknown,
) {
  await admin.from('user_drive_backup_events').insert({ user_id: userId, backup_id: backupId, event_type: 'restore_started', detail: {} });
  await admin.from('user_drive_backups').update({ status: 'restoring' }).eq('id', backupId).eq('user_id', userId);
  try {
    const { payload } = await loadBackupPayload(userId, google, backupId);
    const safetyBackup = await createBackup(userId, google, `pre-restore:${backupId}`, currentClientState);

    if (payload.data?.profile && Object.prototype.hasOwnProperty.call(payload.data.profile, 'display_name')) {
      const { error } = await admin.from('profiles').update({ display_name: payload.data.profile.display_name }).eq('id', userId);
      if (error) throw new Error(`PROFILE_RESTORE_FAILED:${safeError(error)}`);
    }

    const restoredValuations = Array.isArray(payload.data?.valuation_history)
      ? payload.data.valuation_history as Record<string, unknown>[]
      : [];
    await restoreValuationHistory(userId, restoredValuations);

    const restoredAt = new Date().toISOString();
    await admin.from('user_drive_backups').update({ status: 'ready', restored_at: restoredAt }).eq('id', backupId).eq('user_id', userId);
    await admin.from('user_drive_backup_events').insert({
      user_id: userId,
      backup_id: backupId,
      event_type: 'restore_completed',
      detail: { safety_backup_id: safetyBackup.id },
    });
    return {
      restored_at: restoredAt,
      safety_backup_id: safetyBackup.id,
      client_state: sanitizeClientState(payload.client_state),
    };
  } catch (error) {
    await admin.from('user_drive_backups').update({ status: 'ready' }).eq('id', backupId).eq('user_id', userId);
    await admin.from('user_drive_backup_events').insert({
      user_id: userId,
      backup_id: backupId,
      event_type: 'restore_failed',
      detail: { error: safeError(error) },
    });
    throw error;
  }
}

async function verifyBackup(userId: string, google: { token: string; permissionId: string }, backupId: string) {
  await loadBackupPayload(userId, google, backupId);
  await admin.from('user_drive_backup_events').insert({ user_id: userId, backup_id: backupId, event_type: 'backup_verified', detail: {} });
  return { verified: true, verified_at: new Date().toISOString() };
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
        .eq('user_id', user.id)
        .eq('google_permission_id', google.permissionId)
        .order('created_at', { ascending: false })
        .limit(50);
      if (error) throw new Error(`BACKUP_LIST_FAILED:${safeError(error)}`);
      return reply({ backups: data || [], google: { email: google.email, display_name: google.displayName } });
    }

    if (action === 'health') {
      const { data, error } = await admin.from('user_drive_backups')
        .select('id,created_at,status,byte_size')
        .eq('user_id', user.id)
        .eq('google_permission_id', google.permissionId)
        .order('created_at', { ascending: false })
        .limit(1)
        .maybeSingle();
      if (error) throw new Error(`BACKUP_HEALTH_FAILED:${safeError(error)}`);
      return reply({
        encryption: 'AES-256-GCM',
        retention: MAX_BACKUPS_PER_USER,
        last_backup: data || null,
        google: { email: google.email, display_name: google.displayName },
      });
    }

    if (action === 'backup') {
      const reason = String(body?.reason || 'manual').slice(0, 100);
      const result = await createBackup(user.id, google, reason, body?.client_state ?? null);
      return reply({ backup: result }, 201);
    }

    if (action === 'preview_restore') {
      const backupId = String(body?.backup_id || '').trim();
      if (!backupId) return reply({ error: 'backup_id is required' }, 400);
      return reply({ preview: await restorePreview(user.id, google, backupId) });
    }

    if (action === 'restore') {
      if (body?.confirm !== 'RESTORE') return reply({ error: 'Restore confirmation required' }, 400);
      const backupId = String(body?.backup_id || '').trim();
      if (!backupId) return reply({ error: 'backup_id is required' }, 400);
      return reply({ restore: await restoreBackup(user.id, google, backupId, body?.current_client_state ?? null) });
    }

    if (action === 'verify') {
      const backupId = String(body?.backup_id || '').trim();
      if (!backupId) return reply({ error: 'backup_id is required' }, 400);
      return reply(await verifyBackup(user.id, google, backupId));
    }

    if (action === 'delete') {
      const backupId = String(body?.backup_id || '').trim();
      if (!backupId) return reply({ error: 'backup_id is required' }, 400);
      const { data: backup, error } = await admin.from('user_drive_backups')
        .select('id,drive_file_id,google_permission_id')
        .eq('id', backupId).eq('user_id', user.id).maybeSingle();
      if (error) throw new Error(`BACKUP_LOOKUP_FAILED:${safeError(error)}`);
      if (!backup || backup.google_permission_id !== google.permissionId) throw new Error('BACKUP_NOT_FOUND_FOR_USER');
      await deleteDriveFile(backup.drive_file_id, google.token);
      await admin.from('user_drive_backups').delete().eq('id', backupId).eq('user_id', user.id);
      await admin.from('user_drive_backup_events').insert({ user_id: user.id, backup_id: null, event_type: 'backup_deleted', detail: { backup_id: backupId } });
      return reply({ deleted: true });
    }

    return reply({ error: 'Unsupported action' }, 400);
  } catch (error) {
    const message = safeError(error);
    if (message === 'AUTH_REQUIRED') return reply({ error: 'Authentication required' }, 401);
    if (message === 'GOOGLE_AUTH_REQUIRED') return reply({ error: 'Google Drive authorization required' }, 401);
    if (message.startsWith('GOOGLE_AUTH_INVALID')) return reply({ error: 'Google Drive authorization is invalid or expired' }, 401);
    if (message === 'GOOGLE_ACCOUNT_MISMATCH' || message === 'BACKUP_NOT_FOUND_FOR_USER' || message === 'BACKUP_OWNER_MISMATCH') {
      return reply({ error: 'This backup is not available to the logged-in user' }, 403);
    }
    if (message === 'CLIENT_STATE_INVALID' || message === 'CLIENT_STATE_TOO_LARGE') return reply({ error: 'Backup data is invalid or too large' }, 400);
    if (message === 'BACKUP_MASTER_KEY_NOT_CONFIGURED' || message === 'BACKUP_MASTER_KEY_INVALID') {
      return reply({ error: 'Encrypted backup service is not configured' }, 503);
    }
    if (message.startsWith('BACKUP_INTEGRITY_VERIFICATION_FAILED')) {
      return reply({ error: 'Backup upload failed integrity verification and was not marked ready' }, 502);
    }
    console.error('user-google-drive-backup', message);
    return reply({ error: 'Backup operation failed' }, 500);
  }
});