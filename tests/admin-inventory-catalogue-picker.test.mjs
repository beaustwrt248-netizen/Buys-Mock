import fs from 'node:fs';
import assert from 'node:assert/strict';

const picker=fs.readFileSync('admin/admin-inventory-catalogue-picker.js','utf8');
const linking=fs.readFileSync('admin/admin-lifecycle-linking.js','utf8');
const css=fs.readFileSync('admin/admin-lifecycle-linking.css','utf8');

assert.match(picker,/\.from\('device_catalog'\)/);
assert.match(picker,/\.eq\('active',true\)/);
assert.match(picker,/model_name\.ilike/);
assert.match(picker,/model_number\.ilike/);
assert.match(picker,/brand\.ilike/);
assert.match(picker,/invCatalogId/);
assert.match(picker,/invSummary/);
assert.match(picker,/invModel/);
assert.match(picker,/function clearSelectedCatalogue\(\)/);
assert.match(picker,/String\(id\.value\)===String\(selected\.id\)\)id\.value=''/);
assert.match(picker,/selected=null;renderStorage\(\)/);
assert.match(picker,/searchVersion\+\+;clearSelectedCatalogue\(\);clearTimeout\(timer\)/);
assert.match(picker,/const version=searchVersion;timer=setTimeout\(\(\)=>search\(version\),250\)/);
assert.match(picker,/if\(version!==searchVersion\|\|input\.value\.trim\(\)!==term\)return/);
assert.doesNotMatch(picker,/\.rpc\(/);
assert.doesNotMatch(picker,/\.insert\(|\.update\(|\.delete\(/);
assert.match(linking,/adminInventoryCataloguePickerScript/);
assert.match(linking,/admin-inventory-catalogue-picker\.js\?v=1/);
assert.match(css,/admin-catalogue-results/);
assert.match(css,/@media\(max-width:560px\)[\s\S]*admin-catalogue-results\{grid-template-columns:1fr\}/);
