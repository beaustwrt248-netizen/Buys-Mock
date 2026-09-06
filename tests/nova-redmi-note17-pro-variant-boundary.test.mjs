import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-redmi-note17-pro-variant-boundary.json', import.meta.url), 'utf8'));

test('REDMI Note 17 Pro ambiguity evidence is non-executable and guarded', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization, true);
  assert.equal(evidence.pre_execution_recheck_required, true);
});

test('Australian row remains explicitly unresolved between certified regional codes', () => {
  assert.equal(evidence.explicitly_unresolved.length, 1);
  const row = evidence.explicitly_unresolved[0];
  assert.equal(row.device_catalog_id, 891);
  assert.equal(row.current_model_number, null);
  assert.deepEqual(row.candidate_model_numbers, ['2607DRA18G', '2607DRA18T']);
  assert.deepEqual(row.dependency_snapshot, { inventory_refs: 0, pricing_refs: 0, pricing_history_refs: 0 });
});

test('evidence requires Australian-specific resolution rather than regional inference', () => {
  const row = evidence.explicitly_unresolved[0];
  assert.ok(row.evidence.some((entry) => entry.authority === 'Xiaomi Australia'));
  assert.equal(row.evidence.filter((entry) => entry.authority === 'Global Certification Forum').length, 2);
  assert.match(row.required_to_resolve, /Australia-specific/i);
  assert.match(row.reason, /false precision/i);
});
