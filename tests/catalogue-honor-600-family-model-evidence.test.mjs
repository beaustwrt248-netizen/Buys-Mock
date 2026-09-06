import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const evidence = JSON.parse(
  await readFile(new URL('../nova/catalogue-honor-600-family-model-evidence.json', import.meta.url), 'utf8'),
);

assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
assert.equal(evidence.market, 'AU');
assert.equal(evidence.records.length, 2);

const byId = new Map(evidence.records.map((record) => [record.device_catalog_id, record]));
const honor600 = byId.get(747);
const honor600Pro = byId.get(753);

assert.ok(honor600, 'HONOR 600 catalogue row 747 must remain represented');
assert.ok(honor600Pro, 'HONOR 600 Pro catalogue row 753 must remain represented');
assert.equal(honor600.current_model_number, null);
assert.equal(honor600.verified_model_number, 'VKJ-NX9');
assert.deepEqual(honor600.dependency_refs, { inventory: 0, pricing: 0, pricing_history: 0 });
assert.equal(honor600.active_model_number_collisions, 0);
assert.equal(honor600.assessment, 'model_number_correction_candidate');

assert.equal(honor600Pro.current_model_number, null);
assert.equal(honor600Pro.verified_model_number, 'VKP-NX9');
assert.deepEqual(honor600Pro.dependency_refs, { inventory: 0, pricing: 0, pricing_history: 0 });
assert.equal(honor600Pro.active_model_number_collisions, 0);
assert.equal(honor600Pro.assessment, 'model_number_correction_candidate');

for (const record of evidence.records) {
  assert.equal(record.market_region, 'AU');
  assert.equal(record.release_year, 2026);
  assert.equal(record.confidence, 'high');
}

console.log('HONOR 600 family evidence contract passed');
