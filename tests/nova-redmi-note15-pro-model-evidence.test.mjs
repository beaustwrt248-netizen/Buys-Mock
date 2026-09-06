import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-redmi-note15-pro-model-evidence.json', import.meta.url), 'utf8'));

test('REDMI Note 15 Pro evidence remains non-executable and guarded', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization, true);
  assert.equal(evidence.pre_execution_recheck_required, true);
});

test('REDMI Note 15 Pro row records the certified model number and zero-reference snapshot', () => {
  assert.equal(evidence.verified_rows.length, 1);
  const row = evidence.verified_rows[0];
  assert.equal(row.device_catalog_id, 927);
  assert.equal(row.verified_model_number, '25080RABDG');
  assert.deepEqual(row.dependency_snapshot, { inventory_refs: 0, pricing_refs: 0, pricing_history_refs: 0 });
  assert.equal(row.collision_snapshot.active_rows_using_verified_model_number, 0);
});

test('evidence includes Australian Xiaomi identity and GCF certification', () => {
  const row = evidence.verified_rows[0];
  assert.ok(row.evidence.some((entry) => entry.authority === 'Xiaomi Australia'));
  assert.ok(row.evidence.some((entry) => entry.authority === 'Global Certification Forum' && entry.url.endsWith('/13223.html')));
});
