import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-redmi-note17-promax-variant-boundary.json', import.meta.url), 'utf8'));

test('REDMI Note 17 Pro Max boundary remains non-executable and guarded', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization, true);
  assert.equal(evidence.pre_execution_recheck_required, true);
});

test('Australian row remains unresolved despite a credible regional candidate', () => {
  assert.equal(evidence.explicitly_unresolved.length, 1);
  const row = evidence.explicitly_unresolved[0];
  assert.equal(row.device_catalog_id, 892);
  assert.equal(row.current_model_number, null);
  assert.deepEqual(row.candidate_model_numbers, ['2609FRA74G']);
  assert.deepEqual(row.dependency_snapshot, { inventory_refs: 0, pricing_refs: 0, pricing_history_refs: 0 });
  assert.equal(row.collision_snapshot.active_rows_using_candidate_model_number, 0);
});

test('regional hardware divergence is preserved as a no-guess boundary', () => {
  const row = evidence.explicitly_unresolved[0];
  assert.ok(row.evidence.some((entry) => entry.authority === 'Xiaomi Australia' && entry.supports.includes('9210mAh')));
  assert.ok(row.evidence.some((entry) => entry.authority === 'Xiaomi Global' && entry.supports.includes('10000mAh')));
  assert.ok(row.evidence.some((entry) => entry.authority === 'Xiaomi Taiwan' && entry.supports.includes('2609FRA74G')));
  assert.match(row.required_to_resolve, /Australia-specific/i);
  assert.match(row.reason, /false precision/i);
});
