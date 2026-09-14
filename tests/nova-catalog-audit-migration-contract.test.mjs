import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const migrationUrl = new URL('../supabase/migrations/20260914123000_nova_catalog_audit_consumer.sql', import.meta.url);

test('catalogue audit claim is atomic, bounded, stale-lock aware and service-role only', async () => {
  const sql = await readFile(migrationUrl, 'utf8');
  assert.match(sql, /for update skip locked/i);
  assert.match(sql, /least\(coalesce\(p_limit, 10\), 25\)/i);
  assert.match(sql, /p_stale_seconds/i);
  assert.match(sql, /security invoker/i);
  assert.match(sql, /revoke execute[\s\S]*from public/i);
  assert.match(sql, /grant execute[\s\S]*to service_role/i);
});

test('completion is ownership-checked and finding + terminal transition are atomic', async () => {
  const sql = await readFile(migrationUrl, 'utf8');
  assert.match(sql, /nova_complete_catalog_audit/i);
  assert.match(sql, /for update/i);
  assert.match(sql, /locked_by\s*=\s*p_worker/i);
  assert.match(sql, /insert into public\.nova_catalog_audit_findings/i);
  assert.match(sql, /p_outcome\s+not in\s*\('verified','blocked','discrepancy','failed','retry'\)/i);
});
