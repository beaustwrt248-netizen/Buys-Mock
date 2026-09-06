import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-asus-g835-yearly-identity-evidence.json', import.meta.url)));

test('ASUS G835 evidence preserves distinct yearly hardware identities', () => {
  const [y2025, y2026] = evidence.records;
  assert.equal(y2025.model_number, 'G835');
  assert.equal(y2026.model_number, 'G835');
  assert.equal(y2025.release_year, 2025);
  assert.equal(y2026.release_year, 2026);
  assert.notEqual(y2025.processor_family, y2026.processor_family);
  assert.notDeepEqual(y2025.manufacturer_variants, y2026.manufacturer_variants);
  assert.equal(evidence.assessment, 'distinct_yearly_hardware_sharing_family_code');
});

test('ASUS G835 evidence remains fail-closed against false deduplication', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization_for_production_write, true);
  assert.ok(evidence.records.every(record =>
    record.dependency_refs.inventory === 0 &&
    record.dependency_refs.pricing === 0 &&
    record.dependency_refs.pricing_history === 0
  ));
  assert.ok(evidence.manufacturer_sources.every(source => source.url.includes('rog.asus.com/au/')));
  assert.ok(evidence.safety_notes.some(note => /Do not deduplicate/i.test(note)));
  assert.ok(evidence.safety_notes.some(note => /Release year/i.test(note)));
});