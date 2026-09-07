import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const backend = readFileSync('supabase/functions/user-google-drive-backup/index.ts','utf8');
const client = readFileSync('user-drive-backup.js','utf8');
const index = readFileSync('index.html','utf8');

function has(text, needle, message){assert.ok(text.includes(needle),message||`Missing ${needle}`)}

test('backup backend derives user identity from bearer session',()=>{
  has(backend,"admin.auth.getUser(token)");
  assert.ok(!backend.includes('body?.user_id'),'client must never choose backup owner');
});

test('backup lookup and destructive operations are scoped to current user',()=>{
  has(backend,".eq('id', backupId).eq('user_id', userId)");
  has(backend,"backup.google_permission_id !== google.permissionId");
  has(backend,"payload?.owner_user_id !== userId");
  has(backend,"payload?.google_permission_id !== google.permissionId");
});

test('Google Drive appData scope is narrow and per-user',()=>{
  has(client,'https://www.googleapis.com/auth/drive.appdata');
  has(backend,"parents: ['appDataFolder']");
});

test('backup payload encryption is authenticated AES-256-GCM',()=>{
  has(backend,"cipher: 'AES-256-GCM'");
  has(backend,"{ name: 'AES-GCM', length: 256 }");
  has(backend,'crypto.getRandomValues(new Uint8Array(32))');
  has(backend,'ciphertext_sha256');
  has(backend,'BACKUP_HASH_MISMATCH');
});

test('master key is server-only database material',()=>{
  has(backend,"from('user_drive_backup_master_keys')");
  assert.ok(!backend.includes('MORLEY_USER_BACKUP_MASTER_KEY'));
  assert.ok(!client.includes('user_drive_backup_master_keys'));
});

test('local backup whitelist excludes auth sessions and secrets',()=>{
  has(backend,"new Set(['bm_inv', 'bm_sales', 'bm_recent'])");
  has(client,"const CLIENT_KEYS=['bm_inv','bm_sales','bm_recent']");
  assert.ok(!/client_state[^\n]*morley_web_auth/.test(backend));
  assert.ok(!/clientState\([^)]*\)[\s\S]{0,300}morley_web_auth/.test(client));
});

test('restore is previewed, confirmed, and safety-backed up',()=>{
  has(backend,"action === 'preview_restore'");
  has(backend,"body?.confirm !== 'RESTORE'");
  has(backend,'pre-restore:');
  has(client,'preview_restore');
  has(client,"confirm:'RESTORE'");
  has(client,'current_client_state:clientState()');
});

test('retention and integrity verification are present',()=>{
  has(backend,'const MAX_BACKUPS_PER_USER = 30');
  has(backend,"action === 'verify'");
  has(backend,'backup_verified');
  has(client,'30 versions');
});

test('OTA workspace loads Drive backup client',()=>{
  has(index,"'user-drive-backup.js?v=1'");
});

test('legacy local import/export controls are repurposed',()=>{
  has(client,"exportBtn.textContent='Back Up to Drive'");
  has(client,"importBtn.textContent='Restore from Drive'");
  has(client,'importFile.disabled=true');
});
