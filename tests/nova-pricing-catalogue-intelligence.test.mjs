import fs from 'node:fs';
import test from 'node:test';
import assert from 'node:assert/strict';

const pricing = fs.readFileSync('supabase/functions/nova-pricing-intelligence/index.ts', 'utf8');
const catalogue = fs.readFileSync('supabase/functions/nova-catalogue-intelligence/index.ts', 'utf8');

test('pricing intelligence stays advisory and uses Australian market evidence', () => {
  assert.match(pricing, /market-search-v2/);
  assert.match(pricing, /pricingAnalysis/);
  assert.match(pricing, /retained_listings/);
  assert.match(pricing, /outliers_removed/);
  assert.match(pricing, /retail_reference/);
  assert.match(pricing, /excluded_from_used_market_median: true/);
  assert.match(pricing, /cannot approve or write protected Morley buy\/sell prices/);
  assert.doesNotMatch(pricing, /admin-pricing-control/);
  assert.doesNotMatch(pricing, /from\(['"]device_buy_prices['"]\).*\.(insert|update|upsert|delete)/s);
});

test('pricing intelligence is admin authenticated', () => {
  assert.match(pricing, /admin\.auth\.getUser\(token\)/);
  assert.match(pricing, /profile\.role !== 'admin'/);
  assert.match(pricing, /profile\?\.is_enabled/);
});

test('catalogue intelligence summarises audit evidence without applying findings', () => {
  assert.match(catalogue, /nova_catalog_audit_queue/);
  assert.match(catalogue, /nova_catalog_audit_findings/);
  assert.match(catalogue, /image_reference_url/);
  assert.match(catalogue, /awaiting_human_approval/);
  assert.match(catalogue, /applying protected changes remains subject to the existing approval controls/);
  assert.doesNotMatch(catalogue, /\.(insert|update|upsert|delete)\(/);
});

test('catalogue gap logic avoids treating every category as requiring storage or RAM', () => {
  assert.match(catalogue, /requiresStorage=new Set|requiresStorage = new Set/);
  assert.match(catalogue, /category === 'mobile_phone'|category==='mobile_phone'/);
  assert.match(catalogue, /brand\.toLowerCase\(\) !== 'apple'|brand\.toLowerCase\(\)!=='apple'/);
});
