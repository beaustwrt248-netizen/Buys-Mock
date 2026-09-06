import fs from 'node:fs';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync('nova/catalogue-legacy-console-source-provenance-audit.json', 'utf8'));
assert.equal(evidence.execution_authorized, false);
assert.equal(evidence.requires_explicit_human_authorization, true);
assert.equal(evidence.pre_execution_recheck_required, true);
assert.deepEqual(evidence.rows.map(row => row.id), [1030, 1031, 1035]);

const snes = evidence.rows.find(row => row.id === 1030);
assert.equal(snes.model_number, 'SNSP-001A');
assert.equal(snes.current_host_authority, 'reseller');
assert.match(snes.source_upgrade_status, /exact_first_party_identifier_source_not_found/);
assert.match(snes.recommended_action, /Do not replace exact identifier evidence/i);

const ps1 = evidence.rows.find(row => row.id === 1031);
assert.equal(ps1.model_number, 'SCPH-7502');
assert.equal(ps1.document_authority, 'Sony Computer Entertainment');
assert.equal(ps1.current_host_authority, 'third_party_manual_archive');
assert.deepEqual(ps1.dependency_refs, {inventory:0, pricing:1, pricing_history:0});

const ps3 = evidence.rows.find(row => row.id === 1035);
assert.equal(ps3.model_number, 'CECHH02');
assert.equal(ps3.current_host_authority, 'secondary_database');
assert.match(ps3.first_party_backstop_scope, /CECHH00-series/i);
assert.match(ps3.first_party_backstop_scope, /does not identify the Australian CECHH02/i);

assert.ok(evidence.audit_rules.some(rule => /source_name/i.test(rule) && /source_url/i.test(rule)));
assert.ok(evidence.audit_rules.some(rule => /broad first-party family page/i.test(rule)));
assert.ok(evidence.safety_notes.some(note => /No Supabase write/i.test(note)));
assert.ok(evidence.safety_notes.some(note => /protected pricing/i.test(note)));
console.log('nova legacy console source provenance audit: PASS');
