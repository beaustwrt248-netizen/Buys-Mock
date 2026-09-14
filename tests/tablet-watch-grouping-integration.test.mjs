import fs from 'node:fs';
import assert from 'node:assert/strict';

const index=fs.readFileSync('index.html','utf8');
const universal=fs.readFileSync('web-universal-buy.js','utf8');
const grouping=fs.readFileSync('tablet-watch-grouping.js','utf8');
const css=fs.readFileSync('catalogue-connectivity-grouping.css','utf8');

assert.match(index,/catalogue-connectivity-grouping\.css\?v=20260914/);
assert.match(index,/catalogue-connectivity-core\.js\?v=20260914/);
assert.match(index,/tablet-watch-grouping\.js\?v=20260914/);
assert.ok(index.indexOf('catalogue-connectivity-core.js?v=20260914') < index.indexOf('tablet-watch-grouping.js?v=20260914'));

assert.match(universal,/id='tablets'|id="tablets"|tablets\.id='tablets'/);
assert.match(universal,/morleyTabletGrid/);
assert.match(universal,/morleyTabletCount/);
assert.match(universal,/id='smartwatches'|id="smartwatches"|watches\.id='smartwatches'/);
assert.match(universal,/morleyWatchGrid/);
assert.match(universal,/morleyWatchCount/);
assert.match(universal,/Mobile Phones/);
assert.match(universal,/Tablets/);
assert.match(universal,/Smartwatches/);
assert.match(universal,/rowSearchText/);

assert.match(grouping,/categoryFromCard/);
assert.match(grouping,/p\.includes\('tablet'\)/);
assert.match(grouping,/p\.includes\('wearable'\)/);
assert.match(grouping,/morleyTabletGrid/);
assert.match(grouping,/morleyWatchGrid/);
assert.match(grouping,/storageLabel\.textContent='Storage'/);
assert.match(grouping,/connectivityLabel\.textContent='Connectivity'/);
assert.match(grouping,/core\(\)\.groupKey/);
assert.match(grouping,/\.morley-open-buy',selected\.card/);
assert.match(grouping,/\.morley-favourite',selected\.card/);
assert.match(grouping,/catalogue device/);
assert.match(css,/morleyTabletGrid/);
assert.match(css,/morleyWatchGrid/);
assert.match(css,/\.morley-variant-chip\.active/);
assert.match(css,/\.morley-group-source-bin\{display:none!important\}/);

console.log('tablet/watch grouping integration: PASS');
