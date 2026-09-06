import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const evidence = JSON.parse(
  await readFile(new URL('../nova/catalogue-honor-v6-x6d-model-boundary.json', import.meta.url), 'utf8'),
);

assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
assert.equal(evidence.records.length, 2);

const records = new Map(evidence.records.map((record) => [record.device_catalog_id, record]));
const magicV6 = records.get(930);
const x6d = records.get(931);

assert.ok(magicV6, 'Magic V6 row 930 must stay covered');
assert.ok(x6d, 'X6d 5G row 931 must stay covered');

for (const record of [magicV6, x6d]) {
  assert.equal(record.current_model_number, null);
  assert.equal(record.assessment, 'model_number_unresolved_do_not_guess');
  assert.deepEqual(record.dependency_refs, { inventory: 0, pricing: 0, pricing_history: 0 });
  assert.match(record.manufacturer_source.url, /^https:\/\/www\.honor\.com\/au\//);
}

assert.deepEqual(x6d.storage_options, ['128GB']);
assert.match(x6d.regional_variation_note, /global.*256GB.*Australian.*128GB/i);
assert.ok(evidence.safety_notes.some((note) => /Keep model_number null/i.test(note)));

console.log('HONOR unresolved model-code boundary contract passed');
