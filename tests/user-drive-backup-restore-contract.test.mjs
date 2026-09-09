import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source=fs.readFileSync('supabase/functions/user-google-drive-backup/index.ts','utf8');
const helperStart=source.indexOf('async function restoreValuationHistory');
const restoreStart=source.indexOf('async function restoreBackup');
assert.ok(helperStart>=0,'restoreValuationHistory helper must exist');
assert.ok(restoreStart>helperStart,'restoreBackup must follow restoreValuationHistory');
const helper=source.slice(helperStart,restoreStart);
const restore=source.slice(restoreStart,source.indexOf('async function verifyBackup',restoreStart));

test('Drive restore remains explicitly human-confirmed and owner-bound',()=>{
  assert.match(source,/body\?\.confirm !== 'RESTORE'/);
  assert.match(source,/payload\?\.owner_user_id !== userId/);
  assert.match(source,/payload\?\.google_permission_id !== google\.permissionId/);
  assert.match(source,/BACKUP_HASH_MISMATCH/);
});

test('restore creates a fresh safety backup before production writes',()=>{
  const safety=restore.indexOf('createBackup(userId, google, `pre-restore:${backupId}`');
  const profile=restore.indexOf("admin.from('profiles').update");
  const valuation=restore.indexOf('restoreValuationHistory(userId, restoredValuations)');
  assert.ok(safety>=0,'restore must create a pre-restore safety backup');
  assert.ok(profile>safety,'profile restore must happen after safety backup');
  assert.ok(valuation>safety,'valuation restore must happen after safety backup');
});

test('non-empty valuation restore writes replacement rows before pruning old rows',()=>{
  assert.match(helper,/VALUATION_RESTORE_INVALID_ID/);
  assert.match(helper,/VALUATION_RESTORE_DUPLICATE_ID/);
  const upsert=helper.indexOf(".upsert(rows, { onConflict: 'id' })");
  const prune=helper.indexOf(".not('id', 'in'");
  assert.ok(upsert>=0,'non-empty restore must upsert restored rows');
  assert.ok(prune>upsert,'pruning must only occur after successful upsert');
});

test('legacy destructive clear-before-insert pattern is absent',()=>{
  assert.doesNotMatch(restore,/delete\(\)\.eq\('user_id', userId\)[\s\S]{0,600}insert\(rows\)/);
});

test('empty backup still intentionally restores an empty valuation history',()=>{
  assert.match(helper,/if \(!restoredValuations\.length\) \{[\s\S]*?delete\(\)\.eq\('user_id', userId\)/);
});
