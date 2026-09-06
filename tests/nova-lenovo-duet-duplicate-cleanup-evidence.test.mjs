import fs from 'node:fs';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync('nova/catalogue-lenovo-duet-duplicate-cleanup-evidence.json', 'utf8'));

assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
assert.equal(evidence.canonical_row.id, 1266);
assert.equal(evidence.canonical_row.verified_model_number, 'CT-X636F');
assert.equal(evidence.canonical_row.proposed_model_name, 'IdeaPad Duet Chromebook');
assert.equal(evidence.canonical_row.verified_release_year, 2020);
assert.deepEqual(evidence.canonical_row.dependency_refs, {inventory:0, pricing:2, pricing_history:0});
assert.equal(evidence.duplicate_row.id, 1268);
assert.equal(evidence.duplicate_row.normalized_identity, 'IdeaPad Duet Chromebook (CT-X636F)');
assert.deepEqual(evidence.duplicate_row.dependency_refs, {inventory:0, pricing:0, pricing_history:0});
assert.ok(evidence.proposed_cleanup.some(note => /protected-pricing/i.test(note)));
assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
assert.ok(evidence.safety_notes.some(note => /dependency drift/i.test(note)));
console.log('nova Lenovo Duet duplicate cleanup evidence: PASS');
