import assert from 'node:assert/strict';
import { createProductSearchAdapter } from '../src/adapters/product-search-adapter.mjs';
import { SAFE_CLIENT_FUNCTIONS, isSafeClientFunction } from '../src/safe-client-functions.mjs';

const calls = [];
const edgeClient = {
  async invoke(name, body) {
    calls.push({ name, body });
    if (name === 'app-pricing-catalogue') {
      return {
        device_count: 3,
        devices: [
          { id: 'd1', brand: 'Google', model_name: 'Pixel 9 Pro', model_number: 'GEC77', storage_options: ['128GB', '256GB'] },
          { id: 'd2', brand: 'Samsung', model_name: 'Galaxy S25', model_number: 'SM-S931B', storage_options: ['256GB'] },
          { id: 'd3', brand: 'Google', model_name: 'Pixel 8', model_number: 'GKWS6', storage_options: ['128GB'] }
        ],
        prices: [
          { device_catalog_id: 'd1', storage: '256GB', price_aud: 700, authoritative: true },
          { device_catalog_id: 'd2', storage: '256GB', price_aud: 650, authoritative: true }
        ]
      };
    }
    if (name === 'market-search-v2') {
      return { success: true, query: body.query, currency: 'AUD', ebay: { items: [{ title: 'Pixel 9 Pro 256GB', price: 900, source: 'eBay AU' }] }, webRetail: { items: [] }, gumtree: { items: [] }, facebook: { items: [] } };
    }
    throw new Error(`unexpected function ${name}`);
  }
};

assert.equal(isSafeClientFunction('app-pricing-catalogue'), true);
assert.equal(isSafeClientFunction('market-search-v2'), true);
assert.equal(SAFE_CLIENT_FUNCTIONS.includes('admin-pricing-control'), false);

const adapter = createProductSearchAdapter({ edgeClient });
await assert.rejects(() => adapter.catalogue('   '), /QUERY_REQUIRED/);
await assert.rejects(() => adapter.market(''), /QUERY_REQUIRED/);

const catalogue = await adapter.catalogue('pixel 9 256');
assert.equal(catalogue.items.length, 1);
assert.equal(catalogue.items[0].device.id, 'd1');
assert.equal(catalogue.items[0].prices[0].price_aud, 700);
assert.deepEqual(calls[0], { name: 'app-pricing-catalogue', body: {} });

const market = await adapter.market('Pixel 9 Pro 256GB', { limit: 20 });
assert.equal(market.success, true);
assert.deepEqual(calls[1], { name: 'market-search-v2', body: { query: 'Pixel 9 Pro 256GB', limit: 20 } });

for (const forbidden of ['create', 'update', 'approve', 'writePrice', 'delete']) {
  assert.equal(forbidden in adapter, false, forbidden);
}

console.log('product-search-adapter: ok');
