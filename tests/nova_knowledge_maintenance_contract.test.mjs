import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const migrationPath = new URL('../supabase/migrations/20260913043000_nova_knowledge_maintenance.sql', import.meta.url);

test('Nova maintenance migration is additive, bounded, concurrency safe and service-role only', () => {
  assert.equal(fs.existsSync(migrationPath), true, 'maintenance migration must exist');
  const sql = fs.readFileSync(migrationPath, 'utf8').toLowerCase();

  for (const column of ['embedding_attempt_count', 'embedding_last_attempt_at', 'embedding_next_retry_at', 'embedding_last_error_code']) {
    assert.match(sql, new RegExp(`add column if not exists ${column}`));
  }
  assert.match(sql, /nova_claim_embedding_chunks/);
  assert.match(sql, /least\(greatest\(coalesce\(p_limit,\s*20\),\s*1\),\s*20\)/);
  assert.match(sql, /for update skip locked/);
  assert.match(sql, /security invoker/);
  assert.match(sql, /embedding_status\s*<>\s*'ready'/);
  assert.match(sql, /status\s*=\s*'active'/);
  assert.match(sql, /revoke all on function public\.nova_claim_embedding_chunks[\s\S]*from public, anon, authenticated/);
  assert.match(sql, /grant execute on function public\.nova_claim_embedding_chunks[\s\S]*to service_role/);

  assert.doesNotMatch(sql, /security definer/);
  assert.doesNotMatch(sql, /drop\s+table/);
  assert.doesNotMatch(sql, /truncate\s+/);
  assert.doesNotMatch(sql, /delete\s+from\s+public\.nova_knowledge_(items|chunks|sources)/);
});

test('Nova maintenance health exposes counters without knowledge content', () => {
  assert.equal(fs.existsSync(migrationPath), true, 'maintenance migration must exist');
  const sql = fs.readFileSync(migrationPath, 'utf8').toLowerCase();
  assert.match(sql, /embedding_retryable/);
  assert.match(sql, /embedding_attempted/);
  assert.match(sql, /latest_embedding_attempt_at/);
  assert.doesNotMatch(sql, /jsonb_build_object\([\s\S]*'content'\s*,/);
});

test('scheduled maintenance is fixed, Vault-backed and contains no literal credential', () => {
  assert.equal(fs.existsSync(migrationPath), true, 'maintenance migration must exist');
  const sql = fs.readFileSync(migrationPath, 'utf8');
  const lower = sql.toLowerCase();
  assert.match(lower, /cron\.schedule/);
  assert.match(lower, /nova-knowledge-maintenance/);
  assert.match(lower, /vault\.decrypted_secrets/);
  assert.match(lower, /nova_knowledge_maintenance_token/);
  assert.match(lower, /project_url/);
  assert.match(lower, /net\.http_post/);
  assert.match(lower, /functions\/v1\/nova-knowledge-maintenance/);
  assert.match(lower, /x-nova-maintenance-token/);
  assert.doesNotMatch(lower, /service_role_key/);
  assert.doesNotMatch(lower, /eyj[a-z0-9_-]{20,}/i);
});