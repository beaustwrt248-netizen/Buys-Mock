import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const migrationPath = 'supabase/migrations/20260913043000_nova_knowledge_maintenance_hardening.sql';
const workerPath = 'supabase/functions/nova-knowledge-maintenance/index.ts';
const adminIngestPath = 'supabase/functions/nova-knowledge-ingest/index.ts';
const sharedPath = 'supabase/functions/_shared/nova_internal_ingestion.mjs';

const read = (path) => fs.readFileSync(path, 'utf8');

test('embedding claims are concurrency safe, bounded, retry-aware and service-role only', () => {
  assert.equal(fs.existsSync(migrationPath), true, 'hardening migration must exist');
  const sql = read(migrationPath).toLowerCase();
  assert.match(sql, /add column if not exists embedding_attempt_count/);
  assert.match(sql, /add column if not exists embedding_last_attempt_at/);
  assert.match(sql, /add column if not exists embedding_next_retry_at/);
  assert.match(sql, /add column if not exists embedding_last_error_code/);
  assert.match(sql, /nova_claim_embedding_chunks/);
  assert.match(sql, /for update skip locked/);
  assert.match(sql, /least\(greatest\(coalesce\(p_limit,\s*20\),\s*1\),\s*20\)/);
  assert.match(sql, /security invoker/);
  assert.match(sql, /revoke all on function public\.nova_claim_embedding_chunks[\s\S]*from public, anon, authenticated/);
  assert.match(sql, /grant execute on function public\.nova_claim_embedding_chunks[\s\S]*to service_role/);
  assert.doesNotMatch(sql, /security definer/);
});

test('maintenance worker consumes the claim RPC rather than racing direct selects', () => {
  const source = read(workerPath);
  assert.match(source, /nova_claim_embedding_chunks/);
  assert.doesNotMatch(source, /pendingEmbeddingRows/);
  assert.doesNotMatch(source, /retryableEmbeddingRows/);
  assert.match(source, /embedding_last_error_code/);
  assert.match(source, /embedding_next_retry_at/);
});

test('admin and scheduled ingestion share one privacy-preserving ingestion engine', () => {
  assert.equal(fs.existsSync(sharedPath), true, 'shared ingestion engine must exist');
  const shared = read(sharedPath);
  const admin = read(adminIngestPath);
  const worker = read(workerPath);
  assert.match(admin, /nova_internal_ingestion\.mjs/);
  assert.match(worker, /nova_internal_ingestion\.mjs/);
  assert.match(shared, /MAX_INGESTION_BATCH\s*=\s*50/);
  const supportSelect = shared.match(/fetchRows\(admin,\s*["']support_tickets["']\s*,\s*["']([^"']+)["']/m)?.[1] || '';
  assert.match(supportSelect, /category/);
  assert.match(supportSelect, /device_model/);
  assert.doesNotMatch(supportSelect, /user_id|assigned_to|subject|description|diagnostics/);
  assert.match(shared, /generated_from_live_catalogue/);
  assert.match(shared, /adaptOperationalRows/);
});

test('embedding hardening does not remove lexical fallback or knowledge content', () => {
  const sql = read(migrationPath).toLowerCase();
  assert.doesNotMatch(sql, /drop\s+(table|function).*nova_search_knowledge_chunks/);
  assert.doesNotMatch(sql, /delete\s+from\s+public\.nova_knowledge_(items|chunks|sources)/);
  assert.doesNotMatch(sql, /update\s+public\.nova_knowledge_chunks[\s\S]{0,300}content\s*=/);
});
