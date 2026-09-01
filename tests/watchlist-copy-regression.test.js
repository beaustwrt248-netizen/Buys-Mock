const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const source = fs.readFileSync(path.resolve(__dirname, '..', 'smart-workspace-plus.js'), 'utf8');
const index = fs.readFileSync(path.resolve(__dirname, '..', 'index.html'), 'utf8');

assert.match(source, /active\.length===1\?'opportunity':'opportunities'/);
assert.doesNotMatch(source, /opportunity\$\{active\.length===1\?'':'ies'\}/);
assert.doesNotMatch(source, /opportunitiyes|opportunityies/i);
assert.match(source, /No watched valuations yet/);
assert.match(index, /smart-workspace-plus\.js\?v=3/);

console.log('Watchlist copy regression contract OK');
