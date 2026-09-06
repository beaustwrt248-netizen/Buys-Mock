import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-xiaomi-17t-series-model-evidence.json', import.meta.url), 'utf8'));

test('Xiaomi 17T evidence remains non-executable and guarded', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization, true);
  assert.equal(evidence.pre_execution_recheck_required, true);
});

test('Xiaomi 17T and 17T Pro map to distinct certified model numbers', () => {
  assert.equal(evidence.verified_rows.length, 2);
  const byId = new Map(evidence.verified_rows.map((row) => [row.device_catalog_id, row]));
  assert.equal(byId.get(893).verified_model_number, '2602DPT53G');
  assert.equal(byId.get(894).verified_model_number, '2602EPTC0G');
  assert.notEqual(byId.get(893).verified_model_number, byId.get(894).verified_model_number);
});

test('both rows retain zero dependency snapshots and first-party Australian evidence', () => {
  for (const row of evidence.verified_rows) {
    assert.deepEqual(row.dependency_snapshot, { inventory_refs: 0, pricing_refs: 0, pricing_history_refs: 0 });
    assert.equal(row.collision_snapshot.active_rows_using_verified_model_number, 0);
    assert.ok(row.evidence.some((entry) => entry.authority === 'Xiaomi Australia'));
    assert.ok(row.evidence.some((entry) => entry.authority === 'Global Certification Forum'));
  }
});
