import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const workerPath = 'supabase/functions/nova-knowledge-maintenance/index.ts';
const migrationPath = 'supabase/migrations/20260913042000_nova_knowledge_maintenance_pipeline.sql';

const read = (path) => fs.readFileSync(path, 'utf8');

test('maintenance worker exists and fails closed before database work', () => {
  assert.equal(fs.existsSync(workerPath), true, `${workerPath} must exist`);
  const source = read(workerPath);
  assert.match(source, /NOVA_KNOWLEDGE_MAINTENANCE_SECRET/);
  assert.match(source, /Authorization/);
  assert.match(source, /Bearer/);
  const authIndex = source.indexOf('NOVA_KNOWLEDGE_MAINTENANCE_SECRET');
  const dbIndex = source.indexOf('.from(');
  assert.ok(authIndex >= 0 && dbIndex > authIndex, 'maintenance authorization must be established before database work');
  assert.doesNotMatch(source, /SUPABASE_SERVICE_ROLE_KEY\s*=\s*['"][^'"]+['"]/);
});

test('embedding work is bounded and uses gte-small with explicit ready/failed states', () => {
  const source = read(workerPath);
  assert.match(source, /gte-small/);
  assert.match(source, /384/);
  assert.match(source, /Math\.min\([^\n]*20/);
  assert.match(source, /embedding_status[^\n]*(pending|failed)/i);
  assert.match(source, /embedding_status[^\n]*ready/i);
  assert.match(source, /embedding_status[^\n]*failed/i);
  assert.match(source, /embedding_provider[^\n]*supabase-ai/i);
  assert.match(source, /embedding_model[^\n]*gte-small/i);
});

test('maintenance response never returns chunk content, vectors or credentials', () => {
  const source = read(workerPath);
  assert.match(source, /embedded_ready/);
  assert.match(source, /embedded_failed/);
  assert.match(source, /ingested_created/);
  assert.match(source, /ingested_updated/);
  assert.match(source, /duration_ms/);
  const responseSection = source.slice(source.lastIndexOf('return reply'));
  assert.doesNotMatch(responseSection, /content\s*:/);
  assert.doesNotMatch(responseSection, /embedding\s*:/);
  assert.doesNotMatch(responseSection, /secret/i);
  assert.doesNotMatch(responseSection, /service_role/i);
});

test('maintenance migration uses Vault-backed five-minute cron without literal secrets or security definer', () => {
  assert.equal(fs.existsSync(migrationPath), true, `${migrationPath} must exist`);
  const sql = read(migrationPath);
  assert.match(sql, /nova_knowledge_maintenance_runs/);
  assert.match(sql, /enable row level security/i);
  assert.match(sql, /service_role/i);
  assert.match(sql, /vault\.decrypted_secrets/i);
  assert.match(sql, /nova_knowledge_maintenance_secret/);
  assert.match(sql, /cron\.schedule/i);
  assert.match(sql, /\*\/5 \* \* \* \*/);
  assert.match(sql, /pg_net|net\.http_post/i);
  assert.doesNotMatch(sql, /security\s+definer/i);
  assert.doesNotMatch(sql, /Bearer\s+[A-Za-z0-9._~-]{16,}/);
});

test('maintenance migration preserves lexical fallback and exposes aggregate health only', () => {
  const sql = read(migrationPath);
  assert.match(sql, /nova_knowledge_health/i);
  assert.match(sql, /embedding_pending/i);
  assert.match(sql, /embedding_ready/i);
  assert.match(sql, /embedding_error/i);
  assert.match(sql, /latest_maintenance/i);
  assert.doesNotMatch(sql, /drop\s+(table|function).*nova_search_knowledge_chunks/i);
  assert.doesNotMatch(sql, /alter\s+table\s+nova_knowledge_chunks\s+drop/i);
});
