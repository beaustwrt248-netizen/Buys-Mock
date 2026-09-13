import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../supabase/functions/recovery-readiness/index.ts', import.meta.url), 'utf8');

test('recovery readiness reports independent full-system backup freshness', () => {
  assert.match(source, /SYSTEM_BACKUP_STALE_HOURS\s*=\s*36/);
  assert.match(source, /function globalBackupFreshness/);
  assert.match(source, /state:\s*'healthy'/);
  assert.match(source, /state:\s*'stale'/);
  assert.match(source, /state:\s*'missing'/);
  assert.match(source, /threshold_hours:\s*SYSTEM_BACKUP_STALE_HOURS/);
  assert.match(source, /global_backup:\s*\{\.\.\.globalBackup/);
});

test('missing or invalid full-system backup evidence fails closed without claiming health', () => {
  assert.match(source, /if\(!lastSuccess\)return\{state:'missing',healthy:false,stale:true/);
  assert.match(source, /if\(!Number\.isFinite\(lastMs\)\)return\{state:'unknown',healthy:false,stale:true/);
});
