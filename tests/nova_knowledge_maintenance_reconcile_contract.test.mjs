import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const workerPath = 'supabase/functions/nova-knowledge-maintenance/index.ts';
const migrationPath = 'supabase/migrations/20260913090000_nova_knowledge_maintenance_reconcile.sql';
const read = (path) => fs.readFileSync(path, 'utf8');

test('embedding claims are atomic, retry aware and service-role only', () => {
  assert.equal(fs.existsSync(migrationPath), true, 'reconciliation migration must exist');
  const sql = read(migrationPath).toLowerCase();
  assert.match(sql, /add column if not exists embedding_attempt_count/);
  assert.match(sql, /add column if not exists embedding_last_attempt_at/);
  assert.match(sql, /add column if not exists embedding_next_retry_at/);
  assert.match(sql, /add column if not exists embedding_last_error_code/);
  assert.match(sql, /nova_claim_embedding_chunks/);
  assert.match(sql, /for update skip locked/);
  assert.match(sql, /security invoker/);
  assert.match(sql, /least\(greatest\(coalesce\(p_limit,\s*4\),\s*1\),\s*20\)/);
  assert.match(sql, /revoke all on function public\.nova_claim_embedding_chunks[\s\S]*from public, anon, authenticated/);
  assert.match(sql, /grant execute on function public\.nova_claim_embedding_chunks[\s\S]*to service_role/);
  assert.doesNotMatch(sql, /security definer/);
});

test('reconciliation does not restore recurring cron while scheduler auth is unresolved', () => {
  const sql = read(migrationPath).toLowerCase();
  assert.doesNotMatch(sql, /cron\.schedule/);
  assert.doesNotMatch(sql, /net\.http_post/);
  assert.doesNotMatch(sql, /vault\.decrypted_secrets/);
});

test('worker defaults to verified 4 by 4 envelope while retaining explicit bounded overrides', () => {
  const source = read(workerPath);
  assert.match(source, /DEFAULT_EMBEDDING_BATCH\s*=\s*4/);
  assert.match(source, /DEFAULT_INGEST_BATCH\s*=\s*4/);
  assert.match(source, /MAX_EMBEDDING_BATCH\s*=\s*20/);
  assert.match(source, /MAX_INGEST_BATCH\s*=\s*20/);
  assert.match(source, /bounded\(body\.embedding_limit,\s*DEFAULT_EMBEDDING_BATCH,\s*MAX_EMBEDDING_BATCH\)/);
  assert.match(source, /bounded\(body\.ingest_limit,\s*DEFAULT_INGEST_BATCH,\s*MAX_INGEST_BATCH\)/);
});

test('worker uses claim RPC instead of racing pending/error selects', () => {
  const source = read(workerPath);
  assert.match(source, /nova_claim_embedding_chunks/);
  assert.doesNotMatch(source, /pendingEmbeddingRows/);
  assert.doesNotMatch(source, /retryableEmbeddingRows/);
  assert.match(source, /embedding_next_retry_at/);
  assert.match(source, /embedding_last_error_code/);
});

test('scheduler auth diagnostics are bounded and never expose credentials', () => {
  const source = read(workerPath);
  assert.match(source, /type\s+MaintenanceAuthReason/);
  for (const reason of ['missing_config', 'missing_header', 'env_match', 'rpc_match', 'rpc_error', 'mismatch']) {
    assert.match(source, new RegExp(reason));
  }
  assert.match(source, /auth_reason/);
  assert.match(source, /console\.warn\([^\n]*authorization rejected/);
  assert.doesNotMatch(source, /console\.(?:log|warn|error)\([^\n]*(?:supplied|SCHEDULER_SECRET|MORLEY_BACKUP_SECRET)/i);
});

test('PR 1867 optional-source protections remain present', () => {
  const source = read(workerPath);
  assert.match(source, /function\s+isOptionalSourcePermissionError/);
  assert.match(source, /42501/);
  for (const table of ['inventory_items', 'sales_records', 'valuation_quotes']) {
    const start = source.indexOf(`\"${table}\"`);
    assert.ok(start >= 0, `${table} source must remain present`);
    assert.match(source.slice(start, start + 800), /optional:\s*true/);
  }
});

test('lexical fallback and knowledge content are untouched', () => {
  const sql = read(migrationPath).toLowerCase();
  assert.doesNotMatch(sql, /drop\s+(table|function).*nova_search_knowledge_chunks/);
  assert.doesNotMatch(sql, /delete\s+from\s+public\.nova_knowledge_(items|chunks|sources)/);
  assert.doesNotMatch(sql, /update\s+public\.nova_knowledge_chunks[\s\S]{0,300}content\s*=/);
});