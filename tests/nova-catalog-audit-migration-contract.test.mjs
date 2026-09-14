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
