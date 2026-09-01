const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const source = fs.readFileSync(path.resolve(__dirname, '..', 'smart-workspace-plus.js'), 'utf8');

assert.match(source, /active\.length===1\?'opportunity':'opportunities'/);
assert.doesNotMatch(source, /opportunity\$\{active\.length===1\?'':'ies'\}/);
assert.doesNotMatch(source, /opportunitiyes|opportunityies/i);

console.log('Watchlist copy regression contract OK');
