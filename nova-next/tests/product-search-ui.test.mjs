import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const ui = fs.readFileSync(new URL('../src/product-search-ui.mjs', import.meta.url), 'utf8');
const app = fs.readFileSync(new URL('../app.js', import.meta.url), 'utf8');
const sw = fs.readFileSync(new URL('../service-worker.js', import.meta.url), 'utf8');

await test('product search UI owns an explicit read-only search flow', () => {
  assert.match(ui, /createProductSearchUi/);
  assert.match(ui, /searchProducts/);
  assert.match(ui, /Search products & prices/);
  assert.match(ui, /read-only/i);
  assert.match(ui, /source/i);
});

await test('product search consumes the catalogue adapter result shape', () => {
  assert.match(ui, /response\?\.items/);
  assert.match(ui, /entry\?\.device/);
  assert.match(ui, /entry\?\.prices/);
});

await test('app wires Product Search without granting pricing mutation authority', () => {
  assert.match(app, /createProductSearchUi/);
  assert.match(app, /productSearchUi\.bind\(\)/);
  assert.doesNotMatch(app, /admin-pricing-control/);
  assert.doesNotMatch(ui, /admin-pricing-control/);
});

await test('offline shell precaches the product search UI module', () => {
  assert.match(sw, /src\/product-search-ui\.mjs/);
});

console.log('product-search-ui: ok');
