import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const migrationPath = 'supabase/migrations/20260914040500_reconcile_nova_knowledge_maintenance_scheduler.sql';

test('Nova scheduler reconciliation keeps one canonical five-minute Vault-backed 4x4 trigger', () => {
  assert.equal(fs.existsSync(migrationPath), true, `${migrationPath} must exist`);
  const sql = fs.readFileSync(migrationPath, 'utf8');
  const lower = sql.toLowerCase();
  assert.match(lower, /nova-knowledge-maintenance-every-5-minutes/);
  assert.match(lower, /nova-knowledge-maintenance/);
  assert.match(lower, /cron\.unschedule/);
  assert.match(lower, /cron\.schedule/);
  assert.match(lower, /\*\/5 \* \* \* \*/);
  assert.match(lower, /vault\.decrypted_secrets/);
  assert.match(lower, /morley_backup_scheduler_secret/);
  assert.match(lower, /x-maintenance-secret/);
  assert.match(lower, /"embedding_limit"\s*:\s*4/);
  assert.match(lower, /"ingest_limit"\s*:\s*4/);
  assert.doesNotMatch(sql, /Bearer\s+[A-Za-z0-9._~-]{16,}/);
  assert.doesNotMatch(lower, /morley_backup_scheduler_secret\s*[:=]\s*['"][^'"]{8,}/i);
});
