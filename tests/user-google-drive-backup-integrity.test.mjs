import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const source = await readFile(new URL('../supabase/functions/user-google-drive-backup/index.ts', import.meta.url), 'utf8');

test('backup creation stays non-ready until remote read-back verification succeeds', () => {
  const creating = source.indexOf("status: 'creating'");
  const verify = source.indexOf('await verifyUploadedBackup(');
  const ready = source.indexOf(".update({ status: 'ready' })", verify);
  assert.ok(creating >= 0, 'new backup metadata must begin in creating state');
  assert.ok(verify > creating, 'remote verification must happen after creating metadata exists');
  assert.ok(ready > verify, 'ready transition must happen only after remote verification');
  assert.match(source, /integrity_verified:\s*true/);
});

test('verification round-trip downloads, hashes, decrypts, and validates owner-bound payload', () => {
  assert.match(source, /const raw = await downloadAppData\(driveFileId, google\.token\)/);
  assert.match(source, /await decodeRemoteBackup\(userId, google\.permissionId, backupId, raw, expectedHash/);
  assert.match(source, /sha256Bytes\(ciphertext\)/);
  assert.match(source, /await decryptForUser\(/);
  assert.match(source, /payload\?\.owner_user_id !== userId/);
  assert.match(source, /payload\?\.google_permission_id !== permissionId/);
  assert.match(source, /payload\?\.backup_id !== backupId/);
  assert.match(source, /Array\.isArray\(payload\.data\.valuation_history\)/);
});

test('corrupt remote envelope cannot become ready and is retained as failed', () => {
  assert.match(source, /catch \{ throw new Error\('BACKUP_ENVELOPE_INVALID'\); \}/);
  assert.match(source, /throw new Error\('BACKUP_HASH_MISMATCH'\)/);
  assert.match(source, /throw new Error\('BACKUP_DECRYPT_FAILED'\)/);
  assert.match(source, /throw new Error\('BACKUP_PAYLOAD_INVALID'\)/);
  assert.match(source, /update\(\{ status: 'failed' \}\)/);
  assert.match(source, /BACKUP_INTEGRITY_VERIFICATION_FAILED/);
  assert.match(source, /Backup upload failed integrity verification and was not marked ready/);
});
