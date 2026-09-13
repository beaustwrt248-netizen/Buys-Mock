import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const sourceUrl = new URL('../supabase/functions/nova-knowledge-ingest/index.ts', import.meta.url);
const sharedUrl = new URL('../supabase/functions/_shared/nova_internal_ingestion.mjs', import.meta.url);
const wrapper = () => readFile(sourceUrl, 'utf8');
const source = async () => `${await wrapper()}\n${await readFile(sharedUrl, 'utf8')}`;

test('internal ingestion remains admin-only and hashes source identities', async () => {
  const wrapperText = await wrapper();
  const text = await source();
  assert.match(wrapperText, /Authorization/);
  assert.match(wrapperText, /admin\.auth\.getUser\(token\)/);
  assert.match(wrapperText, /p\.role\s*!==\s*['"]admin['"]/);
  assert.match(wrapperText, /!p\?\.is_enabled/);
  assert.match(text, /sha256Hex/);
  assert.match(text, /managed_by:\s*['"]nova_internal_adapter['"]/);
  assert.match(text, /nova_knowledge_ingestion_runs/);
  assert.doesNotMatch(wrapperText, /x-maintenance-secret/i);
  assert.doesNotMatch(wrapperText, /morley_backup_scheduler_secret_matches/);
});

test('support ingestion selects structured safe fields only', async () => {
  const text = await source();
  const supportSelect = text.match(/fetchRows\(admin,\s*['"]support_tickets['"]\s*,\s*['"]([^'"]+)['"]/m)?.[1] || '';
  assert.match(supportSelect, /category/);
  assert.match(supportSelect, /app_version/);
  assert.match(supportSelect, /device_model/);
  assert.doesNotMatch(supportSelect, /user_id|assigned_to|subject|description|diagnostics/);
});

test('catalogue ingestion adopts existing live-catalogue seeds instead of duplicating them', async () => {
  const text = await source();
  assert.match(text, /generated_from_live_catalogue/);
  assert.match(text, /device_catalog_id/);
  assert.match(text, /adopted_count/);
  assert.match(text, /return\s+['"]adopted['"]/);
});

test('internal ingestion never invents empty operational datasets', async () => {
  const text = await source();
  assert.match(text, /adaptOperationalRows/);
  assert.match(text, /documents\.length/);
  assert.match(text, /created_count/);
});
