import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-poco-m8-model-number-evidence.json', import.meta.url)));

test('POCO M8 evidence records the verified hardware identifier', () => {
  assert.equal(evidence.records.length, 1);
  assert.equal(evidence.records[0].device_catalog_id, 889);
  assert.equal(evidence.records[0].brand, 'POCO');
  assert.equal(evidence.records[0].model_name, 'POCO M8 5G');
  assert.equal(evidence.records[0].verified_model_number, '25118PC98G');
});

test('POCO M8 evidence remains read-only and does not infer sibling identifiers', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /Do not infer POCO X8 Pro/i.test(note)));
  assert.ok(evidence.records[0].evidence_urls.some(url => /\/au\/product\/poco-m8-5g\/specs\//.test(url)));
});
