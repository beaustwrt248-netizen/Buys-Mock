import test from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';

const LEDGER = 'catalogue/shared-model-number-classifications.json';

function loadLedger() {
  assert.ok(existsSync(LEDGER), 'missing governed shared-model-number classification ledger');
  return JSON.parse(readFileSync(LEDGER, 'utf8'));
}

test('manufacturer-verified shared model numbers are explicitly classified and preserved', () => {
  const ledger = loadLedger();
  const byKey = new Map((ledger.classifications || []).map((entry) => [`${entry.brand}|${entry.model_number}`, entry]));
  const expected = [
    ['ASUS', 'AI2501', 'shared_family_identifier'],
    ['ASUS', 'AI2401', 'shared_family_identifier'],
    ['ASUS', 'G835', 'shared_family_identifier'],
    ['ASUS', 'NUC16JNK', 'shared_family_identifier'],
    ['OPPO', 'CPH2859', 'regional_market_name_variant'],
    ['Microsoft', '1882', 'shared_hardware_model'],
    ['Samsung', 'SM-L705F', 'intentional_pricing_configuration'],
  ];
  for (const [brand, model, classification] of expected) {
    const entry = byKey.get(`${brand}|${model}`);
    assert.ok(entry, `missing classification for ${brand} ${model}`);
    assert.equal(entry.classification, classification);
    assert.equal(entry.auto_merge_allowed, false);
    assert.equal(entry.auto_deactivate_allowed, false);
  }
});

test('unresolved and possible-duplicate groups remain non-destructive', () => {
  const ledger = loadLedger();
  const byKey = new Map((ledger.classifications || []).map((entry) => [`${entry.brand}|${entry.model_number}`, entry]));
  const a2724 = byKey.get('Apple|A2724');
  assert.ok(a2724, 'Apple A2724 must remain explicitly unresolved');
  assert.equal(a2724.classification, 'unresolved_retail_configuration');
  assert.equal(a2724.auto_merge_allowed, false);
  const a3461 = byKey.get('Apple|A3461');
  assert.ok(a3461, 'Apple A3461 candidate must be tracked');
  assert.equal(a3461.classification, 'possible_true_duplicate');
  assert.equal(a3461.requires_dependency_pricing_inspection, true);
  assert.equal(a3461.auto_merge_allowed, false);
  assert.equal(a3461.auto_deactivate_allowed, false);
});

test('ledger cannot authorize destructive catalogue reconciliation', () => {
  const ledger = loadLedger();
  assert.equal(ledger.schema_version, 1);
  assert.equal(ledger.destructive_reconciliation_requires_explicit_approval, true);
  for (const entry of ledger.classifications || []) {
    assert.notEqual(entry.auto_merge_allowed, true, `${entry.brand} ${entry.model_number} unexpectedly allows auto-merge`);
    assert.notEqual(entry.auto_deactivate_allowed, true, `${entry.brand} ${entry.model_number} unexpectedly allows auto-deactivation`);
  }
});
