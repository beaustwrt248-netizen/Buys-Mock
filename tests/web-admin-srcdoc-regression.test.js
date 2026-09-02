const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const source = fs.readFileSync(path.resolve(__dirname, '..', 'web-admin-mode.js'), 'utf8');

assert.match(source, /function adminHref\(\)/);
assert.match(source, /\^https\?:\$/i);
assert.match(source, /location\.origin/);
assert.match(source, /catch\{return '\/admin\/'\}/);
assert.match(source, /location\.assign\(adminHref\(\)\)/);
assert.doesNotMatch(source, /new URL\('admin\/',location\.href\)/);

console.log('Embedded Admin srcdoc URL regression contract OK');
