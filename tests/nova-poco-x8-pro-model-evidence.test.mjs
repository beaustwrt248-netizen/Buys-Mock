import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const evidence = JSON.parse(fs.readFileSync(new URL('../nova/catalogue-poco-x8-pro-model-evidence.json', import.meta.url), 'utf8'));

test('POCO X8 Pro evidence remains non-executable and guarded', () => {
  assert.equal(evidence.execution_authorized, false);
  assert.equal(evidence.requires_explicit_human_authorization, true);
  assert.equal(evidence.pre_execution_recheck_required, true);
});

test('POCO X8 Pro row records exact verified model and zero-reference snapshot', () => {
  assert.equal(evidence.verified_rows.length, 1);
  const row = evidence.verified_rows[0];
  assert.equal(row.device_catalog_id, 890);
  assert.equal(row.catalogue_name, 'POCO X8 Pro');
  assert.equal(row.market_region, 'Australia');
  assert.equal(row.verified_model_number, '2511FPC34G');
  assert.deepEqual(row.dependency_snapshot, { inventory_refs: 0, pricing_refs: 0, pricing_history_refs: 0 });
  assert.equal(row.collision_snapshot.active_rows_using_verified_model_number, 0);
});

test('POCO X8 Pro evidence includes Australian manufacturer identity and regulatory model proof', () => {
  const urls = evidence.verified_rows[0].evidence.map((entry) => entry.url);
  assert.ok(urls.includes('https://www.mi.com/au/product/poco-x8-pro/'));
  assert.ok(urls.includes('https://www.mi.com/au/product/poco-x8-pro/specs/'));
  assert.ok(urls.some((url) => url.includes('/ANATEL/08779-25-09185')));
});
