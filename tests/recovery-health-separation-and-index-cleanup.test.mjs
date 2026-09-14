import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const readiness = fs.readFileSync('supabase/functions/recovery-readiness/index.ts', 'utf8');
const migrations = fs.existsSync('supabase/migrations/20260914143000_recovery_health_index_cleanup.sql')
  ? fs.readFileSync('supabase/migrations/20260914143000_recovery_health_index_cleanup.sql', 'utf8')
  : '';

test('recovery readiness separates per-user backup findings from global recovery health', () => {
  assert.match(readiness, /user_backup_findings/);
  assert.match(readiness, /global_findings/);
  assert.match(readiness, /kind\s*!==?\s*['"]stale_backup['"]/);
  assert.match(readiness, /kind\s*===?\s*['"]stale_backup['"]/);
});

test('cleanup removes only the redundant non-unique invite index', () => {
  assert.match(migrations, /drop index if exists public\.idx_app_invites_email_unused/i);
  assert.doesNotMatch(migrations, /drop index[^;]*app_invites_active_email_idx/i);
  assert.match(migrations, /app_invites_active_email_idx/);
});
