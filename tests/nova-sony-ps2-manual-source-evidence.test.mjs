import fs from 'node:fs';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync('nova/catalogue-sony-ps2-manual-source-evidence.json', 'utf8'));
assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.requires_explicit_human_authorization, true);
assert.equal(evidence.pre_execution_recheck_required, true);
assert.equal(evidence.rows.length, 2);

const expected = new Map([
  [1032, ['SCPH-39002', 2002, '3-077-227-41(1)']],
  [1033, ['SCPH-77002', 2006, '2-697-297-21(1)']]
]);
for (const row of evidence.rows) {
  const want = expected.get(row.id);
  assert.ok(want, `unexpected row ${row.id}`);
  assert.equal(row.verified_model_number, want[0]);
  assert.equal(row.verified_release_year, want[1]);
  assert.equal(row.document_number, want[2]);
  assert.equal(row.verified_storage_status, 'no_internal_user_storage_uses_memory_card');
  assert.equal(row.current_source_url_is_reseller, true);
  assert.match(row.manufacturer_document, /Sony Computer Entertainment/i);
}
assert.deepEqual(evidence.rows.find(row => row.id === 1033).dependency_refs, {inventory:0, pricing:1, pricing_history:0});
assert.ok(evidence.source_quality_notes.some(note => /manufacturer-authored/i.test(note)));
assert.ok(evidence.source_quality_notes.some(note => /hosting authority/i.test(note)));
assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
assert.ok(evidence.safety_notes.some(note => /protected pricing/i.test(note)));
console.log('nova Sony PS2 manual source evidence: PASS');
