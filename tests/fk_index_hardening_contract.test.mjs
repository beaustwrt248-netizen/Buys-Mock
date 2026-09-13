import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const migrationUrl = new URL('../supabase/migrations/20260914002000_fk_index_hardening.sql', import.meta.url);

const expectedIndexes = [
  ['ai_decision_audit_actor_user_id_idx', 'ai_decision_audit', 'actor_user_id'],
  ['assessment_evidence_created_by_idx', 'assessment_evidence', 'created_by'],
  ['damage_findings_evidence_id_idx', 'damage_findings', 'evidence_id'],
  ['damage_findings_reviewed_by_idx', 'damage_findings', 'reviewed_by'],
  ['deal_risk_flags_resolved_by_idx', 'deal_risk_flags', 'resolved_by'],
  ['device_assessments_buy_price_confirmed_by_idx', 'device_assessments', 'buy_price_confirmed_by'],
  ['device_assessments_grade_confirmed_by_idx', 'device_assessments', 'grade_confirmed_by'],
  ['device_assessments_repair_decision_confirmed_by_idx', 'device_assessments', 'repair_decision_confirmed_by'],
  ['device_passport_events_actor_user_id_idx', 'device_passport_events', 'actor_user_id'],
  ['device_passport_events_assessment_id_idx', 'device_passport_events', 'assessment_id'],
  ['device_passports_created_by_idx', 'device_passports', 'created_by'],
  ['diagnostic_results_created_by_idx', 'diagnostic_results', 'created_by'],
  ['nova_knowledge_ingestion_runs_created_by_idx', 'nova_knowledge_ingestion_runs', 'created_by'],
  ['staff_overrides_actor_user_id_idx', 'staff_overrides', 'actor_user_id'],
  ['valuation_quotes_created_by_idx', 'valuation_quotes', 'created_by'],
];

const withoutComments = (sql) => sql
  .replace(/--.*$/gm, '')
  .replace(/\/\*[\s\S]*?\*\//g, '');

test('FK hardening migration adds exactly the advisor-confirmed leading indexes', async () => {
  const sql = (await readFile(migrationUrl, 'utf8')).toLowerCase();
  for (const [indexName, tableName, columnName] of expectedIndexes) {
    assert.match(
      sql,
      new RegExp(`create\\s+index\\s+if\\s+not\\s+exists\\s+${indexName}\\s+on\\s+public\\.${tableName}\\s*\\(\\s*${columnName}\\s*\\)`, 'i'),
      `missing ${indexName}`,
    );
  }
  const creates = [...sql.matchAll(/create\s+index\s+if\s+not\s+exists\s+/g)];
  assert.equal(creates.length, expectedIndexes.length, 'migration must stay limited to the 15 confirmed FK indexes');
});

test('FK hardening migration does not change data, RLS, grants, functions or constraints', async () => {
  const sql = withoutComments((await readFile(migrationUrl, 'utf8')).toLowerCase());
  assert.doesNotMatch(sql, /\b(insert|update|delete|truncate|alter\s+table|create\s+policy|drop\s+policy|grant|revoke|create\s+or\s+replace\s+function|drop\s+function)\b/);
});
