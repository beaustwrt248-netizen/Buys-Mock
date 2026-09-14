import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const workerPath = 'supabase/functions/nova-knowledge-maintenance/index.ts';
const sharedIngestionPath = 'supabase/functions/nova-knowledge/internal_ingestion.mjs';
const baseMigrationPath = 'supabase/migrations/20260913042000_nova_knowledge_maintenance_pipeline.sql';
const hardeningMigrationPath = 'supabase/migrations/20260913050000_nova_knowledge_maintenance_hardening.sql';
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

test('maintenance uses the production-proven 4x4 ceiling and gte-small embeddings', () => {
  const source = read(workerPath);
  assert.match(source, /MAX_EMBEDDING_BATCH\s*=\s*4/);
  assert.match(source, /MAX_INGEST_BATCH\s*=\s*4/);
  assert.match(source, /bounded\(body\.embedding_limit,\s*4,\s*MAX_EMBEDDING_BATCH\)/);
  assert.match(source, /bounded\(body\.ingest_limit,\s*4,\s*MAX_INGEST_BATCH\)/);
  assert.match(source, /gte-small/);
  assert.match(source, /384/);
  assert.match(source, /embedding_status[^\n]*ready/i);
  assert.match(source, /embedding_status[^\n]*error/i);
  assert.match(source, /embedding_provider[^\n]*supabase-ai/i);
  assert.match(source, /embedding_model[^\n]*gte-small/i);
  assert.doesNotMatch(source, /embedding_status[^\n]*failed/i);
});

test('embedding work is atomically claimed instead of selected by competing workers', () => {
  const source = read(workerPath);
  assert.match(source, /admin\.rpc\(["']nova_claim_embedding_chunks["']/);
  assert.doesNotMatch(source, /function\s+pendingEmbeddingRows\s*\(/);
  assert.doesNotMatch(source, /function\s+retryableEmbeddingRows\s*\(/);
});

test('scheduled ingestion shares the manual ingester identity space and protects legacy catalogue seeds', () => {
  const source = read(workerPath);
  const shared = read(sharedIngestionPath);
  assert.match(source, /createInternalIngestionEngine/);
  assert.match(source, /actorId:\s*null/);
  assert.match(shared, /managed_by:\s*["']nova_internal_adapter["']/);
  assert.match(shared, /`internal:\$\{adapterKey\}`/);
  assert.match(shared, /hasLegacyCatalogueCoverage/);
  assert.match(shared, /generated_from_live_catalogue/);
  assert.match(shared, /nova_knowledge_revisions/);
  assert.doesNotMatch(shared, /managed_by:\s*["']nova_maintenance["']/);
  assert.doesNotMatch(shared, /`maintenance:\$\{adapterKey\}`/);
});

test('maintenance response never returns chunk content, vectors or credentials', () => {
  const source = read(workerPath);
  assert.match(source, /embedded_ready/);
  assert.match(source, /embedded_error/);
  assert.match(source, /ingested_created/);
  assert.match(source, /ingested_updated/);
  assert.match(source, /duration_ms/);
  const successStart = source.lastIndexOf('ok: true');
  assert.ok(successStart >= 0, 'success response must exist');
  const successWindow = source.slice(successStart, successStart + 700);
  assert.doesNotMatch(successWindow, /content\s*:/);
  assert.doesNotMatch(successWindow, /embedding\s*:/);
  assert.doesNotMatch(successWindow, /secret/i);
  assert.doesNotMatch(successWindow, /service_role/i);
});

test('corrective migration owns atomic claims, retries and the verified five-minute 4x4 schedule', () => {
  assert.equal(fs.existsSync(baseMigrationPath), true, `${baseMigrationPath} must exist`);
  assert.equal(fs.existsSync(hardeningMigrationPath), true, `${hardeningMigrationPath} must exist`);
  const sql = read(hardeningMigrationPath);
  const lower = sql.toLowerCase();

  for (const column of ['embedding_attempt_count', 'embedding_last_attempt_at', 'embedding_next_retry_at', 'embedding_last_error_code']) {
    assert.match(lower, new RegExp(`add column if not exists ${column}`));
  }
  assert.match(lower, /nova_claim_embedding_chunks/);
  assert.match(lower, /least\(greatest\(coalesce\(p_limit,\s*4\),\s*1\),\s*4\)/);
  assert.match(lower, /for update skip locked/);
  assert.match(lower, /security invoker/);
  assert.match(lower, /revoke all on function public\.nova_claim_embedding_chunks[\s\S]*from public, anon, authenticated/);
  assert.match(lower, /grant execute on function public\.nova_claim_embedding_chunks[\s\S]*to service_role/);

  assert.match(lower, /vault\.decrypted_secrets/);
  assert.match(lower, /morley_backup_scheduler_secret/);
  assert.match(lower, /x-maintenance-secret/);
  assert.match(lower, /cron\.schedule/);
  assert.match(lower, /\*\/5 \* \* \* \*/);
  assert.match(lower, /net\.http_post/);
  assert.match(lower, /"embedding_limit"\s*:\s*4/);
  assert.match(lower, /"ingest_limit"\s*:\s*4/);
  assert.doesNotMatch(lower, /security\s+definer/);
  assert.doesNotMatch(sql, /Bearer\s+[A-Za-z0-9._~-]{16,}/);
  assert.doesNotMatch(lower, /morley_backup_scheduler_secret\s*[:=]\s*['"][^'"]{8,}/i);
});

test('claim migration never claims ready chunks and leaves lexical content untouched', () => {
  const sql = read(hardeningMigrationPath).toLowerCase();
  assert.match(sql, /status\s*=\s*'active'/);
  assert.match(sql, /embedding_status\s*<>\s*'ready'/);
  assert.match(sql, /embedding_status\s*=\s*'pending'/);
  assert.match(sql, /embedding_status\s*=\s*'error'/);
  assert.doesNotMatch(sql, /set\s+content\s*=/);
  assert.doesNotMatch(sql, /drop\s+(table|function).*nova_search_knowledge_chunks/);
  assert.doesNotMatch(sql, /alter\s+table\s+(?:public\.)?nova_knowledge_chunks\s+drop/);
});

test('maintenance migration preserves lexical fallback and extends aggregate health', () => {
  const sql = read(hardeningMigrationPath);
  assert.match(sql, /nova_knowledge_health/i);
  assert.match(sql, /embedding_pending/i);
  assert.match(sql, /embedding_ready/i);
  assert.match(sql, /embedding_error/i);
  assert.match(sql, /embedding_retryable/i);
  assert.match(sql, /latest_maintenance_at/i);
  assert.match(sql, /latest_maintenance_status/i);
  assert.match(sql, /maintenance_failures_24h/i);
});

test('maintenance serializes structured failures without leaking secret-bearing fields', () => {
  const source = read(workerPath);
  assert.match(source, /function\s+normalizeMaintenanceError\s*\(/);
  assert.match(source, /code/);
  assert.match(source, /details/);
  assert.match(source, /message/);
  assert.match(source, /hint/);
  assert.match(source, /password|secret|token|authorization|cookie/i);
  assert.doesNotMatch(source, /error instanceof Error \? error\.message : error/);
  assert.match(source, /normalizeMaintenanceError\(error/);
});

test('optional operational sources cannot abort maintenance when service-role SELECT is unavailable', () => {
  const source = read(workerPath);
  const sourceWindow = (table) => {
    const start = source.indexOf(`"${table}"`);
    assert.ok(start >= 0, `${table} source must exist`);
    return source.slice(start, start + 700);
  };
  assert.match(source, /function\s+isOptionalSourcePermissionError\s*\(/);
  assert.match(source, /42501/);
  assert.match(sourceWindow('inventory_items'), /optional:\s*true/);
  assert.match(sourceWindow('sales_records'), /optional:\s*true/);
  assert.match(sourceWindow('valuation_quotes'), /optional:\s*true/);
  assert.doesNotMatch(source, /grant\s+select\s+on\s+(?:public\.)?(?:inventory_items|sales_records|valuation_quotes)/i);
});

test('scheduler authorization emits bounded reason codes without exposing credential material', () => {
  const source = read(workerPath);
  assert.match(source, /type\s+MaintenanceAuthReason/);
  assert.match(source, /missing_header/);
  assert.match(source, /env_match/);
  assert.match(source, /rpc_match/);
  assert.match(source, /rpc_error/);
  assert.match(source, /mismatch/);
  assert.match(source, /auth_reason/);
  assert.match(source, /console\.warn\([^\n]*auth/i);
  assert.doesNotMatch(source, /console\.(?:log|warn|error)\([^\n]*(?:supplied|SCHEDULER_SECRET|MORLEY_BACKUP_SECRET)/i);
  assert.doesNotMatch(source, /auth_reason[^\n]*(?:secret|token|authorization|cookie)/i);
});
