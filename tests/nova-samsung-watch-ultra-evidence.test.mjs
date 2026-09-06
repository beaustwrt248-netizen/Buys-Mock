import fs from 'node:fs';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync('nova/catalogue-samsung-watch-ultra-evidence.json', 'utf8'));

assert.equal(evidence.classification, 'read_only_catalogue_evidence');
assert.equal(evidence.risk, 'low');
assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.candidate.device_catalog_id, 1152);
assert.equal(evidence.candidate.model_number, 'SM-1705F');
assert.equal(evidence.candidate.pricing_refs, 1);
assert.equal(evidence.canonical_evidence.device_catalog_id, 279);
assert.equal(evidence.canonical_evidence.model_number, 'SM-L705F');
assert.deepEqual(evidence.canonical_evidence.storage_options, ['32GB']);
assert.equal(evidence.canonical_evidence.pricing_refs, 1);
assert.match(evidence.canonical_evidence.official_reference, /^https:\/\/www\.samsung\.com\/au\//);
assert.match(evidence.finding, /digit 1 for the letter L/i);
assert.match(evidence.protected_boundary, /both the candidate and canonical rows have active pricing references/i);
assert.match(evidence.protected_boundary, /explicitly unauthorized/i);
assert.match(evidence.protected_boundary, /human approval/i);

console.log('nova samsung watch ultra evidence regression: PASS');
