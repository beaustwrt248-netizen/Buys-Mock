import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const workerPath = 'supabase/functions/nova-knowledge-maintenance/index.ts';
const migrationPath = 'supabase/migrations/20260913042000_nova_knowledge_maintenance_pipeline.sql';
const read = (path) => fs.readFileSync(path, 'utf8');

test('maintenance worker exists and fails closed on the established scheduler credential', () => {
  assert.equal(fs.existsSync(workerPath), true, `${workerPath} must exist`);
  const source = read(workerPath);
  assert.match(source, /MORLEY_BACKUP_SECRET/);
  assert.match(source, /x-maintenance-secret/i);
  assert.match(source, /morley_backup_scheduler_secret_matches/);
  assert.match(source, /await\s+authorized\s*\(req\)/);
  assert.doesNotMatch(source, /NOVA_KNOWLEDGE_MAINTENANCE_SECRET/);
  assert.doesNotMatch(source, /SUPABASE_SERVICE_ROLE_KEY\s*=\s*['"][^'"]+['"]/);
});

test('embedding work is bounded and uses gte-small with production ready/error states', () => {
  const source = read(workerPath);
  assert.match(source, /MAX_EMBEDDING_BATCH\s*=\s*20/);
  assert.match(source, /gte-small/);
  assert.match(source, /384/);
  assert.match(source, /embedding_status[^\n]*(pending|error)/i);
  assert.match(source, /embedding_status[^\n]*ready/i);
  assert.match(source, /embedding_status[^\n]*error/i);
  assert.match(source, /embedding_provider[^\n]*supabase-ai/i);
  assert.match(source, /embedding_model[^\n]*gte-small/i);
  assert.doesNotMatch(source, /embedding_status[^\n]*failed/i);
});

test('scheduled ingestion shares the manual ingester identity space and protects legacy catalogue seeds', () => {
  const source = read(workerPath);
  assert.match(source, /managed_by:\s*["']nova_internal_adapter["']/);
  assert.match(source, /`internal:\$\{adapterKey\}`/);
  assert.match(source, /hasLegacyCatalogueCoverage/);
  assert.match(source, /generated_from_live_catalogue/);
  assert.match(source, /nova_knowledge_revisions/);
  assert.doesNotMatch(source, /managed_by:\s*["']nova_maintenance["']/);
  assert.doesNotMatch(source, /`maintenance:\$\{adapterKey\}`/);
});

test('maintenance response never returns chunk content, vectors or credentials', () => {
  const source = read(workerPath);
  assert.match(source, /embedded_ready/);
  assert.match(source, /embedded_error/);
  assert.match(source, /ingested_created/);
  assert.match(source, /ingested_updated/);
  assert.match(source, /duration_ms/);
  const successStart = source.indexOf('ok: true');
  assert.ok(successStart >= 0, 'success response must exist');
  const successWindow = source.slice(successStart, successStart + 700);
  assert.doesNotMatch(successWindow, /content\s*:/);
  assert.doesNotMatch(successWindow, /embedding\s*:/);
  assert.doesNotMatch(successWindow, /secret/i);
  assert.doesNotMatch(successWindow, /service_role/i);
});

test('maintenance migration uses existing Vault-backed scheduler credential and five-minute cron', () => {
  assert.equal(fs.existsSync(migrationPath), true, `${migrationPath} must exist`);
  const sql = read(migrationPath);
  assert.match(sql, /nova_knowledge_maintenance_runs/);
  assert.match(sql, /enable row level security/i);
  assert.match(sql, /service_role/i);
  assert.match(sql, /vault\.decrypted_secrets/i);
  assert.match(sql, /morley_backup_scheduler_secret/);
  assert.match(sql, /x-maintenance-secret/i);
  assert.match(sql, /cron\.schedule/i);
  assert.match(sql, /\*\/5 \* \* \* \*/);
  assert.match(sql, /net\.http_post/i);
  assert.doesNotMatch(sql, /security\s+definer/i);
  assert.doesNotMatch(sql, /Bearer\s+[A-Za-z0-9._~-]{16,}/);
  assert.doesNotMatch(sql, /morley_backup_scheduler_secret\s*[:=]\s*['"][^'"]{8,}/i);
});

test('maintenance migration preserves lexical fallback and extends aggregate health', () => {
  const sql = read(migrationPath);
  assert.match(sql, /nova_knowledge_health/i);
  assert.match(sql, /embedding_pending/i);
  assert.match(sql, /embedding_ready/i);
  assert.match(sql, /embedding_error/i);
  assert.match(sql, /latest_maintenance_at/i);
  assert.match(sql, /latest_maintenance_status/i);
  assert.match(sql, /maintenance_failures_24h/i);
  assert.doesNotMatch(sql, /drop\s+(table|function).*nova_search_knowledge_chunks/i);
  assert.doesNotMatch(sql, /alter\s+table\s+nova_knowledge_chunks\s+drop/i);
});
