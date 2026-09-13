import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const migrationUrl = new URL('../supabase/migrations/20260914003000_rls_initplan_hardening.sql', import.meta.url);

const policies = [
  ['staff_overrides', 'Morley staff append overrides'],
  ['device_passports', 'Morley staff create passports'],
  ['restore_points', 'restore_points_admin_insert'],
  ['restore_events', 'restore_events_admin_insert'],
  ['device_assessments', 'Morley staff create device assessments'],
  ['assessment_evidence', 'Morley staff create assessment evidence'],
  ['diagnostic_results', 'Morley staff create diagnostics'],
  ['valuation_quotes', 'Morley staff create valuation quotes'],
];

const withoutComments = (sql) => sql
  .replace(/--.*$/gm, '')
  .replace(/\/\*[\s\S]*?\*\//g, '');

test('RLS init-plan migration alters exactly the eight advisor-confirmed policies', async () => {
  const sql = withoutComments((await readFile(migrationUrl, 'utf8')).toLowerCase());
  for (const [tableName, policyName] of policies) {
    const escaped = policyName.toLowerCase().replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
    assert.match(sql, new RegExp(`alter\\s+policy\\s+"${escaped}"\\s+on\\s+public\\.${tableName}\\s+with\\s+check`, 'i'));
  }
  assert.equal([...sql.matchAll(/alter\s+policy\s+/g)].length, policies.length);
});

test('RLS init-plan migration preserves policy shape and only wraps auth.uid in SELECT', async () => {
  const sql = withoutComments((await readFile(migrationUrl, 'utf8')).toLowerCase());
  assert.doesNotMatch(sql, /(?<!select\s)auth\.uid\s*\(\s*\)/);
  assert.equal([...sql.matchAll(/\(select\s+auth\.uid\s*\(\s*\)\s*\)/g)].length, 8);
  assert.doesNotMatch(sql, /\b(create\s+policy|drop\s+policy|grant|revoke|alter\s+table|insert|update|delete|truncate|create\s+or\s+replace\s+function|drop\s+function)\b/);
});
