import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source = fs.readFileSync(new URL('../admin/restore-centre.js', import.meta.url), 'utf8');

test('Admin exposes Backups & Restore as a preview-first surface', () => {
  assert.match(source, /Backups & Restore/);
  assert.match(source, /Preview restore/);
  assert.match(source, /restore_points/);
  assert.match(source, /restore_events/);
});

test('restore centre cannot directly delete or mutate production business data', () => {
  assert.doesNotMatch(source, /\.delete\s*\(/);
  assert.doesNotMatch(source, /restore_database|database_restore|rewind_database/i);
  assert.match(source, /Database recovery requires a separately approved recovery plan/);
});

test('restore execution remains explicitly approval gated', () => {
  assert.match(source, /requiresExplicitApproval/);
  assert.match(source, /restore_previewed/);
  assert.doesNotMatch(source, /restore_executed[^\n]*insert/i);
});
