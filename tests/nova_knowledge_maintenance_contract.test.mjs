import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const workerPath = 'supabase/functions/nova-knowledge-maintenance/index.ts';
const migrationPath = 'supabase/migrations/20260913042000_nova_knowledge_maintenance_pipeline.sql';
const sharedPath = 'supabase/functions/_shared/nova_internal_ingestion.mjs';
const read = (path) => fs.readFileSync(path, 'utf8');

test('maintenance worker exists and fails closed on the established scheduler credential', () => {
  const source = read(workerPath);
  assert.match(source, /MORLEY_BACKUP_SECRET/);
  assert.match(source, /x-maintenance-secret/i);
  assert.match(source, /morley_backup_scheduler_secret_matches/);
  assert.match(source, /await\s+authorized\s*\(req\)/);
  assert.doesNotMatch(source, /SUPABASE_SERVICE_ROLE_KEY\s*=\s*['"][^'"]+['"]/);
});

test('embedding work is bounded, concurrency safe and uses gte-small', () => {
  const source = read(workerPath);
  assert.match(source, /MAX_EMBEDDING_BATCH\s*=\s*20/);
  assert.match(source, /nova_claim_embedding_chunks/);
  assert.match(source, /gte-small/);
  assert.match(source, /384/);
  assert.match(source, /embedding_status:\s*["']ready["']/);
  assert.match(source, /embedding_status:\s*["']error["']/);
  assert.match(source, /embedding_provider:\s*["']supabase-ai["']/);
});

test('scheduled ingestion shares the manual ingester privacy and identity engine', () => {
  const source = `${read(workerPath)}\n${read(sharedPath)}`;
  assert.match(read(workerPath), /nova_internal_ingestion\.mjs/);
  assert.match(source, /managed_by:\s*["']nova_internal_adapter["']/);
  assert.match(source, /generated_from_live_catalogue/);
  assert.match(source, /nova_knowledge_revisions/);
  assert.doesNotMatch(source, /managed_by:\s*["']nova_maintenance["']/);
});

test('maintenance response never returns chunk content, vectors or credentials', () => {
  const source = read(workerPath);
  const successStart = source.indexOf('ok: true');
  assert.ok(successStart >= 0);
  const successWindow = source.slice(successStart, successStart + 700);
  assert.doesNotMatch(successWindow, /content\s*:/);
  assert.doesNotMatch(successWindow, /embedding\s*:/);
  assert.doesNotMatch(successWindow, /secret/i);
  assert.doesNotMatch(successWindow, /service_role/i);
});

test('maintenance migration uses existing Vault-backed scheduler credential and five-minute cron', () => {
  const sql = read(migrationPath);
  assert.match(sql, /nova_knowledge_maintenance_runs/);
  assert.match(sql, /enable row level security/i);
  assert.match(sql, /vault\.decrypted_secrets/i);
  assert.match(sql, /morley_backup_scheduler_secret/);
  assert.match(sql, /cron\.schedule/i);
  assert.match(sql, /\*\/5 \* \* \* \*/);
  assert.match(sql, /net\.http_post/i);
  assert.doesNotMatch(sql, /security\s+definer/i);
});

test('maintenance migration preserves lexical fallback and aggregate health', () => {
  const sql = read(migrationPath);
  assert.match(sql, /nova_knowledge_health/i);
  assert.match(sql, /embedding_pending/i);
  assert.match(sql, /latest_maintenance_at/i);
  assert.doesNotMatch(sql, /drop\s+(table|function).*nova_search_knowledge_chunks/i);
});
