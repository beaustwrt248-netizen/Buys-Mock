import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-huawei-watch-fit4-variant-boundary.json', import.meta.url)));

test('Huawei Watch Fit4 evidence preserves unresolved manufacturer variants', () => {
  assert.equal(evidence.record.device_catalog_id, 954);
  assert.equal(evidence.record.current_model_number, null);
  assert.deepEqual(evidence.candidate_model_numbers, ['SYA-B09', 'SYA-B19']);
  assert.equal(evidence.assessment, 'variant_unresolved_do_not_guess');
  assert.ok(evidence.manufacturer_sources.every(source => source.url.includes('consumer.huawei.com')));
});

test('Huawei Watch Fit4 evidence remains fail-closed without inventing identity', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.equal(evidence.record.dependency_refs.inventory, 0);
  assert.equal(evidence.record.dependency_refs.pricing, 0);
  assert.equal(evidence.record.dependency_refs.pricing_history, 0);
  assert.ok(evidence.safety_notes.some(note => /Do not populate a model number/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /exact Australian configuration evidence/i.test(note)));
});