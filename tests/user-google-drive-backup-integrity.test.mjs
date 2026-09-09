import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const source = await readFile(new URL('../supabase/functions/user-google-drive-backup/index.ts', import.meta.url), 'utf8');

test('remote read-back verification happens before backup metadata or ready state', () => {
  const upload = source.indexOf('driveFile = await uploadAppData(');
  const verify = source.indexOf('await verifyUploadedBackup(', upload);
  const metadata = source.indexOf("admin.from('user_drive_backups').insert({", verify);
  const creating = source.indexOf("status: 'creating'", metadata);
  const key = source.indexOf("admin.from('user_drive_backup_keys').insert({", metadata);
  const ready = source.indexOf(".update({ status: 'ready' })", key);
  assert.ok(upload >= 0);
  assert.ok(verify > upload, 'exact uploaded Drive file must be verified immediately');
  assert.ok(metadata > verify, 'metadata must not exist before remote verification');
  assert.ok(creating > metadata, 'post-verification metadata begins non-ready');
  assert.ok(key > creating, 'wrapped key is stored only after read-back verification');
  assert.ok(ready > key, 'ready transition must happen only after key persistence');
  assert.match(source, /integrity_verified:\s*true/);
});

test('upload verification checks exact remote envelope, size and ciphertext hash without decrypting', () => {
  const start = source.indexOf('async function verifyUploadedBackup(');
  const end = source.indexOf('\n}\n\nasync function pruneOldBackups', start);
  const verifier = source.slice(start, end);
  assert.match(verifier, /downloadAppData\(driveFileId, google\.token\)/);
  assert.match(verifier, /verifyRemoteEnvelope\(raw, backupId, expectedHash, expectedByteSize\)/);
  assert.doesNotMatch(verifier, /decryptForUser/);
  assert.match(source, /new TextEncoder\(\)\.encode\(raw\)\.byteLength !== expectedByteSize/);
  assert.match(source, /envelope\?\.format !== 'morley-user-backup-encrypted-v1'/);
  assert.match(source, /envelope\?\.format_version !== 1/);
  assert.match(source, /envelope\?\.backup_id !== backupId/);
  assert.match(source, /sha256Bytes\(ciphertext\)/);
});

test('corrupt remote envelope fails closed before readiness and cleans the just-uploaded file', () => {
  assert.match(source, /throw new Error\('BACKUP_ENVELOPE_INVALID'\)/);
  assert.match(source, /throw new Error\('BACKUP_HASH_MISMATCH'\)/);
  assert.match(source, /throw new Error\('BACKUP_SIZE_MISMATCH'\)/);
  assert.match(source, /deleteDriveFile\(String\(driveFile\.id\), google\.token\)/);
  assert.match(source, /backup_id: null,\n\s*event_type: 'backup_failed'/);
  assert.match(source, /BACKUP_INTEGRITY_VERIFICATION_FAILED/);
  assert.match(source, /Backup upload failed integrity verification and was not marked ready/);
});

test('restore path still decrypts and enforces owner/account-bound payload schema', () => {
  assert.match(source, /async function decodeRemoteBackup/);
  assert.match(source, /await decryptForUser\(/);
  assert.match(source, /payload\?\.owner_user_id !== userId/);
  assert.match(source, /payload\?\.google_permission_id !== permissionId/);
  assert.match(source, /payload\?\.backup_id !== backupId/);
  assert.match(source, /Array\.isArray\(payload\.data\.valuation_history\)/);
});
