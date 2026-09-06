import test from 'node:test';
import assert from 'node:assert/strict';
import {
  canonicalListingUrl,
  classifyMarketUrl,
  dedupeListings,
  isAllowedMarketplaceResult,
  isAllowedRetailResult,
} from '../supabase/functions/market-search-v2/source-policy.mjs';

test('blocks Reddit, editorial and price-comparison sources', () => {
  assert.equal(classifyMarketUrl('https://www.reddit.com/r/mac/comments/123').allowed, false);
  assert.equal(classifyMarketUrl('https://www.ozbargain.com.au/node/123').allowed, false);
  assert.equal(classifyMarketUrl('https://www.staticice.com.au/cgi-bin/search.cgi?q=macbook').allowed, false);
  assert.equal(classifyMarketUrl('https://www.jbhifi.com.au/blogs/tech/macbook-guide').allowed, false);
  assert.equal(classifyMarketUrl('https://lmc.com.au/blog/post/macbook-neo').allowed, false);
});

test('allows direct Australian sellers and manufacturer sale pages', () => {
  for (const url of [
    'https://www.jbhifi.com.au/products/apple-macbook-air-13-inch',
    'https://www.officeworks.com.au/shop/officeworks/p/macbook-air',
    'https://www.apple.com/au/shop/buy-mac/macbook-air',
    'https://www.bigw.com.au/product/example/p/123',
    'https://www.harveynorman.com.au/apple-macbook-air.html',
    'https://www.thegoodguys.com.au/apple-macbook-air',
  ]) {
    assert.equal(classifyMarketUrl(url).kind, 'retail-reference', url);
    assert.equal(isAllowedRetailResult({ title: 'Apple MacBook Air', price: 1599, url }), true, url);
  }
});

test('requires identifiable Amazon marketplace seller', () => {
  const base = { title: 'Apple MacBook Air', price: 1599, url: 'https://www.amazon.com.au/dp/B0TEST123' };
  assert.equal(isAllowedRetailResult(base), false);
  assert.equal(isAllowedRetailResult({ ...base, seller: 'Example Electronics AU' }), true);
  assert.equal(isAllowedRetailResult({ ...base, seller: 'Amazon AU' }), false);
});

test('accepts only actual marketplace listing URLs', () => {
  assert.equal(isAllowedMarketplaceResult({ title: 'MacBook Air', price: 700, url: 'https://www.facebook.com/marketplace/item/123456789/' }, 'facebook'), true);
  assert.equal(isAllowedMarketplaceResult({ title: 'MacBook Air', price: 700, url: 'https://www.facebook.com/groups/123/posts/456' }, 'facebook'), false);
  assert.equal(isAllowedMarketplaceResult({ title: 'MacBook Air', price: 700, url: 'https://www.gumtree.com.au/s-ad/perth/laptops/macbook-air/12345' }, 'gumtree'), true);
  assert.equal(isAllowedMarketplaceResult({ title: 'MacBook Air', price: 700, url: 'https://www.ebay.com.au/itm/123456789' }, 'ebay'), true);
});

test('canonical URL dedupe strips tracking and removes repeats', () => {
  const a = 'https://www.jbhifi.com.au/products/macbook-air?utm_source=google&gclid=abc';
  const b = 'https://jbhifi.com.au/products/macbook-air';
  assert.equal(canonicalListingUrl(a), canonicalListingUrl(b));
  const rows = dedupeListings([
    { title: 'MacBook Air', price: 1000, url: a },
    { title: 'MacBook Air', price: 1000, url: b },
  ]);
  assert.equal(rows.length, 1);
});
