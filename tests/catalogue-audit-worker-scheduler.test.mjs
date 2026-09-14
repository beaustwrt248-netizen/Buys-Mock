import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const migrationPath = 'supabase/migrations/20260914133000_stage_catalogue_audit_worker_scheduler.sql';

test('staged catalogue audit worker runs hourly in batches of 10 without re-enabling enqueue', () => {
  assert.equal(fs.existsSync(migrationPath), true, `${migrationPath} must exist`);
  const sql = fs.readFileSync(migrationPath, 'utf8');
  const lower = sql.toLowerCase();

  assert.match(lower, /nova-catalog-audit-worker-hourly/);
  assert.match(lower, /37 \* \* \* \*/);
  assert.match(lower, /cron\.unschedule/);
  assert.match(lower, /cron\.schedule/);
  assert.match(lower, /functions\/v1\/nova-catalog-audit/);
  assert.match(lower, /vault\.decrypted_secrets/);
  assert.match(lower, /morley_backup_scheduler_secret/);
  assert.match(lower, /x-maintenance-secret/);
  assert.match(lower, /"limit"\s*:\s*10/);
  assert.match(lower, /timeout_milliseconds\s*:=\s*120000/);

  assert.doesNotMatch(lower, /nova_enqueue_catalog_audits/);
  assert.doesNotMatch(lower, /catalog(?:ue)?-audit-enqueue/);
  assert.doesNotMatch(sql, /Bearer\s+[A-Za-z0-9._~-]{16,}/);
});
