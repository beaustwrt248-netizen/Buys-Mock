import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const sql = fs.readFileSync(new URL('../supabase/migrations/20260912220500_morley_ai_scan_history.sql', import.meta.url), 'utf8');

test('device assessments gain durable scan source and checkpoint fields', () => {
  assert.match(sql, /alter table public\.device_assessments\s+add column if not exists source text/i);
  assert.match(sql, /add column if not exists checkpoint text/i);
  assert.match(sql, /add column if not exists checkpoint_metadata jsonb/i);
  assert.match(sql, /add column if not exists last_checkpoint_at timestamptz/i);
  assert.match(sql, /add column if not exists last_error_code text/i);
});

test('scan checkpoint vocabulary retains failed cancelled and completed sessions', () => {
  for (const value of ['capture_started','front_captured','rear_captured','analysis_failed','review_ready','cancelled','completed']) {
    assert.match(sql, new RegExp(`'${value}'`));
  }
});

test('scan history remains protected by existing assessment RLS and is never cascade-deleted by a history helper', () => {
  assert.doesNotMatch(sql, /delete\s+from\s+public\.device_assessments/i);
  assert.doesNotMatch(sql, /create\s+(or\s+replace\s+)?function[^\n]*(delete|purge).*assessment/i);
  assert.match(sql, /create index if not exists device_assessments_source_checkpoint_idx/i);
});
