const assert = require('node:assert/strict');
const fs = require('node:fs');

const index = fs.readFileSync('index.html', 'utf8');
const notes = fs.readFileSync('ADMIN_MODE_REMOVAL_NOTES.md', 'utf8');

assert.equal(fs.existsSync('web-admin-mode.js'), false);
assert.doesNotMatch(index, /web-admin-mode\.js/);
assert.match(notes, /dedicated Morley Admin website under `admin\/`/);
assert.match(notes, /does not remove server-side role enforcement/);
assert.match(index, /profileNetworkGuard/);
assert.match(index, /morley:profile-network/);
assert.match(index, /Profile bootstrap request timed out after/);
assert.match(index, /html=html\.replace\('<\/head>',phoneProbe\+profileNetworkGuard\+styleTags\+'<\/head>'\)/);

console.log('Embedded Admin removal and srcdoc profile bootstrap regression contract OK');
