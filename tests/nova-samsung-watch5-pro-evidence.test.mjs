import fs from 'node:fs';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync('nova/catalogue-samsung-watch5-pro-evidence.json', 'utf8'));

assert.equal(evidence.classification, 'read_only_catalogue_evidence');
assert.equal(evidence.risk, 'low');
assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.candidate.device_catalog_id, 1151);
assert.equal(evidence.candidate.model_number, 'SM-925F');
assert.equal(evidence.candidate.pricing_refs, 0);
assert.equal(evidence.canonical_evidence.device_catalog_id, 971);
assert.equal(evidence.canonical_evidence.model_number, 'SM-R925F');
assert.deepEqual(evidence.canonical_evidence.storage_options, ['16GB']);
assert.equal(evidence.canonical_evidence.pricing_refs, 1);
assert.match(evidence.canonical_evidence.official_reference, /^https:\/\/www\.samsung\.com\//);
assert.match(evidence.protected_boundary, /explicitly unauthorized/i);
assert.match(evidence.protected_boundary, /human approval/i);

console.log('nova samsung watch5 pro evidence regression: PASS');
