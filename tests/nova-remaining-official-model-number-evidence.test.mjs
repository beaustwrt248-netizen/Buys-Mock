import fs from 'node:fs';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync('nova/remaining-official-model-number-evidence.json', 'utf8'));

assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.requires_explicit_human_authorization, true);
assert.equal(evidence.pre_execution_recheck_required, true);
assert.equal(evidence.rows.length, 4);

const expected = new Map([
  [950, 'OPD2415'],
  [957, 'OPWWE251'],
  [958, 'M2425W1'],
  [959, 'LRT-W09']
]);

for (const row of evidence.rows) {
  assert.equal(row.verified_model_number, expected.get(row.id), `unexpected model number for ${row.id}`);
  assert.deepEqual(row.dependency_refs, {inventory:0, pricing:0, pricing_history:0});
  assert.match(row.evidence_url, /^https:\/\//);
}

assert.deepEqual(evidence.explicitly_unresolved.map(x=>x.id).sort((a,b)=>a-b), [880,951]);
assert.match(evidence.explicitly_unresolved.find(x=>x.id===880).reason, /do not collapse regions/i);
assert.match(evidence.explicitly_unresolved.find(x=>x.id===951).reason, /did not expose/i);
assert.equal(evidence.explicitly_unresolved.some(x=>x.id===1367), false);
assert.ok(evidence.safety_notes.some(note => /PR #870.*OXY-001/i.test(note)));
assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));

console.log('nova remaining official model-number evidence: PASS');