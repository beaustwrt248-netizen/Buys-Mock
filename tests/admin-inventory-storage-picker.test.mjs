import fs from 'node:fs';
import assert from 'node:assert/strict';

const src=fs.readFileSync('admin/admin-inventory-catalogue-picker.js','utf8');
const css=fs.readFileSync('admin/admin-lifecycle-linking.css','utf8');

assert.match(src,/function storageOptions/);
assert.match(src,/invCatalogueStorage/);
assert.match(src,/data-storage-option/);
assert.match(src,/q\('#invStorage'\)/);
assert.match(src,/options\.length===1/);
assert.match(src,/Choose the actual storage/);
assert.doesNotMatch(src,/\.rpc\(/);
assert.doesNotMatch(src,/\.insert\(|\.update\(|\.delete\(/);
assert.match(css,/admin-catalogue-storage-options/);
assert.match(css,/button\.active\{background:#0d8463!important/);
assert.match(css,/@media\(max-width:560px\)[\s\S]*admin-catalogue-storage-options\{display:grid;grid-template-columns:repeat\(2,minmax\(0,1fr\)\)\}/);
