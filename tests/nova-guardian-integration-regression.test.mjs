import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read = (path) => fs.readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
const guardian = read('supabase/functions/guardian-worker/index.ts');
const nova = read('supabase/functions/nova-guardian-intelligence/index.ts');
const repair = read('supabase/functions/guardian-repair-worker/index.ts');
const executor = read('supabase/functions/guardian-repair-executor/index.ts');

test('Guardian delegates intelligence to Nova and keeps policy ownership', () => {
  assert.match(guardian, /functions\/v1\/nova-guardian-intelligence/);
  assert.match(guardian, /x-guardian-source/);
  assert.match(guardian, /guardianPolicy\(/);
  assert.match(guardian, /requires_approval:true/);
  assert.doesNotMatch(guardian, /api\.openai\.com\/v1\/responses/);
});

test('Nova Guardian intelligence is internal and cannot authorize repairs', () => {
  assert.match(nova, /token===SERVICE_ROLE/);
  assert.match(nova, /x-guardian-source/);
  assert.match(nova, /never authorize a repair/i);
  assert.match(nova, /sensitive_change_likely/);
  assert.match(nova, /service_role\/database grants/);
});

test('repair generator includes Edge Functions but isolates migrations', () => {
  assert.match(repair, /p\.startsWith\("supabase\/functions\/"\)/);
  assert.match(repair, /supabase\\\/migrations/);
  assert.match(repair, /never edit an existing migration/i);
  assert.match(repair, /No code, Edge Function or migration has been written or deployed/);
  assert.match(repair, /requires_approval:true/);
});

test('executor rechecks real approval before any GitHub write', () => {
  assert.match(executor, /approved_by/);
  assert.match(executor, /approved_at/);
  assert.match(executor, /incident_approval_required/);
  assert.match(executor, /new_files_limited_to_migrations/);
  assert.match(executor, /production_changed:false/);
  assert.match(executor, /Guardian cannot merge, deploy an Edge Function, or apply a migration/);
});
