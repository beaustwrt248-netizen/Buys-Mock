import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const migrationUrl = new URL('../supabase/migrations/20260912030000_morley_ai_assessment_core.sql', import.meta.url);

function migrationText() {
  return fs.readFileSync(migrationUrl, 'utf8').toLowerCase();
}

const tables = [
  'device_assessments',
  'assessment_evidence',
  'damage_findings',
  'diagnostic_results',
  'valuation_quotes',
  'deal_risk_flags',
  'staff_overrides',
  'device_passports',
  'device_passport_events',
  'ai_model_versions',
  'ai_decision_audit',
];

test('creates every assessment, audit and passport table', () => {
  const sql = migrationText();
  for (const table of tables) {
    assert.match(sql, new RegExp(`create\\s+table\\s+(if\\s+not\\s+exists\\s+)?public\\.${table}\\b`), table);
  }
});

test('enables RLS on every new public table', () => {
  const sql = migrationText();
  for (const table of tables) {
    assert.match(sql, new RegExp(`alter\\s+table\\s+public\\.${table}\\s+enable\\s+row\\s+level\\s+security`), table);
  }
});

test('does not introduce broad or privileged client grants', () => {
  const sql = migrationText();
  assert.doesNotMatch(sql, /grant\s+all/i);
  assert.doesNotMatch(sql, /service[_-]?role/i);
  assert.doesNotMatch(sql, /to\s+anon\b/i);
});

test('keeps consequential commercial decisions explicitly staff-confirmed', () => {
  const sql = migrationText();
  for (const column of [
    'grade_confirmed_by',
    'grade_confirmed_at',
    'buy_price_confirmed_by',
    'buy_price_confirmed_at',
    'repair_decision_confirmed_by',
    'repair_decision_confirmed_at',
  ]) {
    assert.match(sql, new RegExp(`\\b${column}\\b`), column);
  }
});

test('uses staff/admin authorization helpers for assessment access', () => {
  const sql = migrationText();
  assert.match(sql, /private\.is_admin_or_manager\s*\(\s*\)/);
  assert.match(sql, /private\.is_support_staff\s*\(\s*\)/);
});

test('keeps audit and passport event tables append-only to authenticated clients', () => {
  const sql = migrationText();
  for (const table of ['staff_overrides', 'device_passport_events', 'ai_decision_audit']) {
    assert.match(sql, new RegExp(`grant\\s+select\\s*,\\s*insert\\s+on\\s+table\\s+public\\.${table}\\s+to\\s+authenticated`), table);
    assert.doesNotMatch(sql, new RegExp(`grant[^;]*update[^;]*public\\.${table}`, 'i'), table);
    assert.doesNotMatch(sql, new RegExp(`grant[^;]*delete[^;]*public\\.${table}`, 'i'), table);
    assert.doesNotMatch(sql, new RegExp(`create\\s+policy[^;]*on\\s+public\\.${table}[^;]*for\\s+(update|delete)`, 'i'), table);
  }
});

test('diagnostics and risk states are constrained to review-safe values', () => {
  const sql = migrationText();
  for (const status of ['pass', 'fail', 'unknown', 'not_tested']) {
    assert.match(sql, new RegExp(`'${status}'`), status);
  }
  for (const status of ['open', 'resolved', 'dismissed']) {
    assert.match(sql, new RegExp(`'${status}'`), status);
  }
  assert.doesNotMatch(sql, /auto[_-]?reject/i);
});

test('does not expose raw secret identifiers as general assessment fields', () => {
  const sql = migrationText();
  assert.doesNotMatch(sql, /\bimei\s+(text|varchar|character varying)\b/i);
  assert.doesNotMatch(sql, /\bserial(_number)?\s+(text|varchar|character varying)\b/i);
});
