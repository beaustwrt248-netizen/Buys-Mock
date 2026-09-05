import fs from 'node:fs';
import assert from 'node:assert/strict';

const src = fs.readFileSync('admin/admin-inventory-catalogue-picker.js', 'utf8');

assert.match(src, /let rows=\[\],timer=null,selected=null,searchVersion=0/);
assert.match(src, /function invalidateSelectedForSearch\(\)/);
assert.match(src, /String\(id\.value\)===String\(selected\.id\)\)id\.value=''/);
assert.match(src, /selected=null;renderStorage\(\)/);
assert.match(src, /searchVersion\+\+;invalidateSelectedForSearch\(\);clearTimeout\(timer\)/);
assert.match(src, /const version=searchVersion;timer=setTimeout\(\(\)=>search\(version\),250\)/);
assert.match(src, /if\(version!==searchVersion\|\|input\.value\.trim\(\)!==term\)return/);
assert.match(src, /function clearSelection\([^)]*\)\{searchVersion\+\+;clearTimeout\(timer\);timer=null;/);
assert.match(src, /clearSelection\('Manual Catalogue ID active\. Search again to relink a verified catalogue device\.'\)/);
assert.match(src, /clearSelection\('Inventory item added\. Search for the next catalogue device when ready\.'\)/);
assert.doesNotMatch(src, /invalidateSelectedForSearch\(\)[\s\S]{0,180}q\('#invStorage'\)\.value=''/);
assert.doesNotMatch(src, /invalidateSelectedForSearch\(\)[\s\S]{0,180}q\('#invCostInput'\)\.value=''/);
