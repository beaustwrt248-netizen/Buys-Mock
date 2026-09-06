import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-poco-m8-model-evidence.json', import.meta.url), 'utf8'));

test('POCO M8 evidence remains non-executable and guarded', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization, true);
  assert.equal(evidence.pre_execution_recheck_required, true);
  assert.equal(evidence.candidate.production_action, 'none');
});

test('POCO M8 evidence preserves the global versus India model boundary', () => {
  const candidate = evidence.candidate;
  assert.equal(candidate.device_catalog_id, 889);
  assert.equal(candidate.current_model_number, null);
  assert.equal(candidate.proposed_model_number, '25118PC98G');
  assert.match(candidate.regional_boundary, /25118PC98I/);
  assert.match(candidate.regional_boundary, /Australian/);
  assert.deepEqual(candidate.dependency_snapshot, {
    inventory_refs: 0,
    pricing_refs: 0,
    pricing_history_refs: 0,
  });
  assert.equal(candidate.collision_snapshot.active_rows_using_25118PC98G, 0);
  assert.equal(candidate.collision_snapshot.active_rows_using_25118PC98I, 0);
});

test('POCO M8 evidence combines first-party product identity with regulatory model reporting', () => {
  const entries = evidence.candidate.evidence;
  assert.ok(entries.some((entry) => entry.authority === 'Xiaomi Australia'));
  assert.ok(entries.some((entry) => entry.authority === 'Xiaomi UK' && entry.supports.includes('512GB')));
  assert.ok(entries.some((entry) => entry.authority === 'NBTC certification reporting' && entry.supports.includes('25118PC98G')));
});
