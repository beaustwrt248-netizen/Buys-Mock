import fs from 'node:fs';
import assert from 'node:assert/strict';

const src = fs.readFileSync('admin/admin-inventory-catalogue-picker.js', 'utf8');

assert.match(src, /q\('#invModel'\)\.value=row\.model_number\|\|''/);
assert.match(src, /const nextStorage=options\.length===1\?options\[0\]:\(options\.includes\(currentStorage\)\?currentStorage:''\)/);
assert.match(src, /if\(options\.length>1&&storage&&!options\.includes\(storage\.value\.trim\(\)\)\)/);
assert.match(src, /Choose the actual catalogue storage before adding stock\./);
assert.match(src, /addEventListener\('click',validateSelection,true\)/);
assert.match(src, /Manual Catalogue ID active\. Search again to relink a verified catalogue device\./);
assert.match(src, /Inventory item added\. Search for the next catalogue device when ready\./);
assert.match(src, /new MutationObserver\(\(\)=>\{if\(createStatus\.textContent\.startsWith\('Inventory item added:'\)\)clearSelection/);
assert.match(src, /idInput\.addEventListener\('input'/);
assert.doesNotMatch(src, /if\(row\.model_number\)q\('#invModel'\)\.value=row\.model_number/);
