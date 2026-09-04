const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const read = file => fs.readFileSync(path.join(root, file), 'utf8');

const desktopLayout = read('desktop-mode-layout-fix.js');
const productParity = read('product-parity-v3.js');
const dashboard = read('morley-desktop-dashboard.js');

for (const source of [desktopLayout, productParity, dashboard]) {
  assert.match(source, /home\|categories\|general\|settings/);
  assert.doesNotMatch(source, /home\|computer\|console\|general/);
}

assert.match(desktopLayout, /categoryIds\.has\(active\)\?'categories':active/);
assert.match(productParity, /categoryIds\.has\(active\)\?'categories':active/);
assert.match(productParity, /data-page="categories"/);
assert.match(productParity, /data-page="settings"/);
assert.doesNotMatch(productParity, /t==='laptop'[\s\S]*data\.target='computer'/);
assert.doesNotMatch(productParity, /t==='desktop'[\s\S]*data\.target='console'/);

// Shared desktop layers must explicitly yield navigation ownership to the
// physical-phone/v5 mobile renderer instead of competing with it.
assert.match(desktopLayout, /morley-physical-phone/);
assert.match(productParity, /const mobileAndroidOwner=/);
assert.match(productParity, /if\(nav&&!mobileAndroidOwner\(\)\)/);
assert.match(productParity, /function syncActiveNav\(\)\{if\(mobileAndroidOwner\(\)\)return/);

for (const category of ['computer','laptop','desktop','mobilePhones','console']) {
  assert.ok(desktopLayout.includes(`'${category}'`), `desktop layout missing category child mapping: ${category}`);
  assert.ok(productParity.includes(`'${category}'`), `product parity missing category child mapping: ${category}`);
}

console.log('Desktop navigation single-owner contract OK');
