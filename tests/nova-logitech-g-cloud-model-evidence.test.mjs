import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-logitech-g-cloud-model-evidence.json', import.meta.url)));

test('Logitech G CLOUD evidence records the manufacturer model number', () => {
  assert.equal(evidence.record.device_catalog_id, 1385);
  assert.equal(evidence.record.model_name, 'Logitech G CLOUD Gaming Handheld');
  assert.equal(evidence.record.verified_model_number, 'GR0006');
  assert.deepEqual(evidence.record.dependency_refs, {inventory:0, pricing:0, pricing_history:0});
});

test('Logitech G CLOUD evidence remains fail-closed', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /explicit human authorization/i.test(note)));
});