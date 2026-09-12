import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const migrationUrl = new URL('../supabase/migrations/20260912215000_morley_restore_points.sql', import.meta.url);

function migration() {
  return fs.readFileSync(migrationUrl, 'utf8');
}

test('restore schema creates append-only restore points and events with RLS', () => {
  const sql = migration();
  assert.match(sql, /create table if not exists public\.restore_points/i);
  assert.match(sql, /create table if not exists public\.restore_events/i);
  assert.match(sql, /alter table public\.restore_points enable row level security/i);
  assert.match(sql, /alter table public\.restore_events enable row level security/i);
  assert.match(sql, /private\.is_admin_or_manager\(\)/i);
});

test('restore schema exposes no generic destructive database rewind function', () => {
  const sql = migration();
  assert.doesNotMatch(sql, /create\s+(or\s+replace\s+)?function\s+[^\n]*(rollback|rewind|restore_database|database_restore)/i);
  assert.doesNotMatch(sql, /grant\s+delete\s+on\s+public\.restore_(points|events)/i);
});

test('restore rows are immutable from authenticated clients', () => {
  const sql = migration();
  assert.doesNotMatch(sql, /grant\s+update\s+on\s+public\.restore_(points|events)/i);
  assert.match(sql, /grant select, insert on public\.restore_points to authenticated/i);
  assert.match(sql, /grant select, insert on public\.restore_events to authenticated/i);
});
