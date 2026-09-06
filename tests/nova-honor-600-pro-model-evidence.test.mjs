import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const evidence = JSON.parse(
  fs.readFileSync(new URL('../nova/catalogue-honor-600-pro-model-evidence.json', import.meta.url), 'utf8'),
);

test('HONOR 600 Pro evidence is read-only and targets the intended AU row', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.catalogue_row.id, 753);
  assert.equal(evidence.catalogue_row.brand, 'HONOR');
  assert.equal(evidence.catalogue_row.model_name, 'HONOR 600 Pro 5G');
  assert.equal(evidence.catalogue_row.market_region, 'AU');
  assert.equal(evidence.catalogue_row.active, true);
  assert.equal(evidence.catalogue_row.model_number, null);
});

test('HONOR 600 Pro candidate is strongly corroborated without a live mutation', () => {
  assert.equal(evidence.candidate.model_number, 'VKP-NX9');
  assert.equal(evidence.candidate.confidence, 'high');
  assert.ok(evidence.sources.length >= 3);
  assert.ok(evidence.sources.some((source) => source.type === 'australian_review'));
  assert.ok(evidence.sources.some((source) => source.type === 'certification_reporting'));
  assert.equal(evidence.safety.production_write_performed, false);
  assert.equal(evidence.safety.pricing_changed, false);
  assert.equal(evidence.safety.requires_recheck_before_any_future_write, true);
});

test('HONOR 600 Pro evidence snapshot has no current dependency or collision blocker', () => {
  const snapshot = evidence.fresh_dependency_snapshot;
  assert.equal(snapshot.inventory_refs, 0);
  assert.equal(snapshot.pricing_refs, 0);
  assert.equal(snapshot.pricing_history_refs, 0);
  assert.equal(snapshot.active_model_number_collisions, 0);
});
