import fs from 'node:fs';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync('nova/catalogue-apple-mobile-reseller-name-cleanup-evidence.json', 'utf8'));
assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.requires_explicit_human_authorization, true);
assert.equal(evidence.pre_execution_recheck_required, true);
assert.equal(evidence.rows.length, 7);
assert.match(evidence.evidence_url, /^https:\/\/support\.apple\.com\/en-au\//);
const expected = new Map([
  [1052,['iPhone 12 Pro Max','A2342',2020]],[1053,['iPhone 13 mini','A2481',2021]],
  [1055,['iPhone 13 Pro Max','A2644',2021]],[1056,['iPhone 13 Pro','A2636',2021]],
  [1057,['iPhone 14','A2649',2022]],[1068,['iPhone 6s Plus','A1687',2015]],
  [1074,['iPhone SE (3rd generation)','A2782',2022]]
]);
for (const row of evidence.rows) {
  const expectedRow = expected.get(row.id);
  assert.ok(expectedRow, `unexpected row ${row.id}`);
  assert.equal(row.verified_model_name, expectedRow[0]);
  assert.equal(row.verified_model_number, expectedRow[1]);
  assert.equal(row.release_year, expectedRow[2]);
  assert.doesNotMatch(row.verified_model_name, /\bM[A-Z0-9]{4,}.*\/A\b/i);
}
assert.deepEqual(evidence.rows.find(row => row.id===1068).dependency_refs,{inventory:0,pricing:1,pricing_history:0});
assert.deepEqual(evidence.rows.find(row => row.id===1074).dependency_refs,{inventory:0,pricing:1,pricing_history:0});
assert.ok(evidence.normalization_rules.some(rule => /aliases/i.test(rule)));
assert.ok(evidence.safety_notes.some(note => /protected pricing/i.test(note)));
assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
console.log('nova Apple mobile reseller-name cleanup evidence: PASS');
