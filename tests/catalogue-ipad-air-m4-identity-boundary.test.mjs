import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const evidence = JSON.parse(
  await readFile(new URL('../nova/catalogue-ipad-air-m4-identity-boundary.json', import.meta.url), 'utf8'),
);

assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
assert.equal(evidence.catalogue_row.device_catalog_id, 1197);
assert.equal(evidence.catalogue_row.current_model_number, null);
assert.equal(evidence.catalogue_row.retail_sku, 'MH5QAX/A');
assert.deepEqual(evidence.catalogue_row.dependency_refs, { inventory: 0, pricing: 0, pricing_history: 0 });
assert.equal(evidence.assessment, 'identity_under_specified_do_not_guess');

const candidates = new Map(evidence.candidate_variants.map((variant) => [
  `${variant.size}:${variant.connectivity}`,
  variant.model_number,
]));
assert.equal(candidates.get('11-inch:Wi-Fi'), 'A3459');
assert.equal(candidates.get('11-inch:Wi-Fi + Cellular'), 'A3460');
assert.equal(candidates.get('13-inch:Wi-Fi'), 'A3461');
assert.equal(candidates.get('13-inch:Wi-Fi + Cellular'), 'A3462');
assert.equal(candidates.size, 4);

assert.ok(evidence.safety_notes.some((note) => /Keep model_number null/i.test(note)));
assert.ok(evidence.safety_notes.some((note) => /Do not treat Apple retail\/order SKU/i.test(note)));
assert.match(evidence.manufacturer_source.url, /^https:\/\/support\.apple\.com\/en-au\//);

console.log('iPad Air M4 identity ambiguity boundary contract passed');
