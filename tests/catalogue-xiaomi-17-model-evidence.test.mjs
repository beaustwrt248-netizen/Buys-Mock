import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const evidence = JSON.parse(
  await readFile(new URL('../nova/catalogue-xiaomi-17-model-evidence.json', import.meta.url), 'utf8'),
);

assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
assert.equal(evidence.catalogue_row.device_catalog_id, 866);
assert.equal(evidence.catalogue_row.current_model_number, null);
assert.deepEqual(evidence.catalogue_row.dependency_refs, { inventory: 0, pricing: 0, pricing_history: 0 });
assert.equal(evidence.candidate.model_number, '25113PN0EG');
assert.equal(evidence.candidate.confidence, 'high');
assert.equal(evidence.candidate.assessment, 'model_number_correction_candidate');
assert.equal(evidence.candidate.active_model_number_collisions, 0);

const sourceTypes = new Set(evidence.sources.map((source) => source.type));
assert.ok(sourceTypes.has('manufacturer_au'));
assert.ok(sourceTypes.has('regulatory_global'));
assert.ok(sourceTypes.has('australian_network_identity_support'));
assert.ok(evidence.safety_notes.some((note) => /explicit human authorization/i.test(note)));

console.log('Xiaomi 17 model-number evidence contract passed');
