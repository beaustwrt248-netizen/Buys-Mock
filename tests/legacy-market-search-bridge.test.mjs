import test from 'node:test';
import assert from 'node:assert/strict';
import { mergeUsedSources, toLegacyMarketResponse } from '../supabase/functions/ebay-search/compat.mjs';

test('merges eBay, Gumtree and Facebook into legacy used evidence', () => {
  const payload = {
    query: 'Apple MacBook Air M3',
    currency: 'AUD',
    ebay: { items: [{ title: 'MacBook Air M3', price: 1000, deliveredPrice: 1020, url: 'https://www.ebay.com.au/itm/1', source: 'eBay AU' }] },
    gumtree: { items: [{ title: 'MacBook Air M3', price: 900, url: 'https://www.gumtree.com.au/s-ad/1', source: 'Gumtree' }] },
    facebook: { items: [{ title: 'MacBook Air M3', price: 950, url: 'https://www.facebook.com/marketplace/item/1', source: 'Facebook Marketplace' }] },
  };
  const items = mergeUsedSources(payload);
  assert.equal(items.length, 3);
  assert.deepEqual(new Set(items.map(x => x.source)), new Set(['eBay AU', 'Gumtree', 'Facebook Marketplace']));
});

test('deduplicates repeated used listings', () => {
  const item = { title: 'MacBook Air M3', price: 900, url: 'https://example.invalid/item/1', source: 'Gumtree' };
  const items = mergeUsedSources({ ebay: { items: [] }, gumtree: { items: [item] }, facebook: { items: [item] } });
  assert.equal(items.length, 1);
});

test('maps trusted retail results into legacy google contract', () => {
  const out = toLegacyMarketResponse({
    success: true,
    query: 'MacBook Air',
    currency: 'AUD',
    webRetail: { provider: 'brave', items: [{ title: 'MacBook Air', price: 1599, source: 'jbhifi.com.au', url: 'https://www.jbhifi.com.au/products/test' }] },
    ebay: { items: [] }, gumtree: { items: [] }, facebook: { items: [] },
    sourcePolicy: { mode: 'trusted-sellers-only', rejectsEditorial: true, rejectsReddit: true, retailIsReferenceOnly: true },
  }, 'MacBook Air');
  assert.equal(out.success, true);
  assert.equal(out.google.items.length, 1);
  assert.equal(out.google.items[0].source, 'jbhifi.com.au');
  assert.equal(out.google.pricing.typicalNew, 1599);
  assert.equal(out.google.pricing.competitiveLow, 1599);
  assert.equal(out.sourcePolicy.rejectsReddit, true);
});

test('computes non-zero used pricing from retained marketplace evidence', () => {
  const out = toLegacyMarketResponse({
    success: true,
    query: '83F500KAAU',
    ebay: { items: [
      { title: 'Lenovo 83F500KAAU', price: 800, deliveredPrice: 820, url: 'https://www.ebay.com.au/itm/1' },
      { title: 'Lenovo 83F500KAAU', price: 900, deliveredPrice: 920, url: 'https://www.ebay.com.au/itm/2' },
    ] },
    gumtree: { items: [{ title: 'Lenovo 83F500KAAU', price: 850, url: 'https://www.gumtree.com.au/s-ad/1' }] },
    facebook: { items: [] },
    webRetail: { items: [] },
  }, '83F500KAAU');
  assert.equal(out.ebay.pricing.typicalUsed, 850);
  assert.equal(out.ebay.pricing.lowest, 820);
  assert.equal(out.ebay.pricing.highest, 920);
});
