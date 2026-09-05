import fs from 'node:fs';
import assert from 'node:assert/strict';

const src = fs.readFileSync('admin/admin-inventory-catalogue-picker.js', 'utf8');

assert.match(src, /q\('#invModel'\)\.value=row\.model_number\|\|''/);
assert.match(src, /const nextStorage=options\.length===1\?options\[0\]:\(options\.includes\(currentStorage\)\?currentStorage:''\)/);
assert.match(src, /if\(options\.length>1&&storage&&!options\.includes\(storage\.value\.trim\(\)\)\)/);
assert.match(src, /Choose the actual catalogue storage before adding stock\./);
assert.match(src, /addEventListener\('click',validateSelection,true\)/);
assert.doesNotMatch(src, /if\(row\.model_number\)q\('#invModel'\)\.value=row\.model_number/);
