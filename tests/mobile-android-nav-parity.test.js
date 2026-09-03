const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const web = fs.readFileSync(path.resolve(__dirname, '..', 'mobile-layout-fix.js'), 'utf8');
const android = fs.readFileSync(
  path.resolve(__dirname, '..', 'android', 'app', 'src', 'main', 'java', 'com', 'buysloans', 'hub', 'DashboardActivity.kt'),
  'utf8'
);

for (const label of ['Home', 'Categories', 'General Buys', 'More']) {
  assert.match(android, new RegExp(`\\(\\"${label}\\"`), `Android bottom nav must expose ${label}`);
  assert.match(web, new RegExp(`navButton\\([^\\n]*'${label}'`), `Mobile web bottom nav must expose ${label}`);
}

assert.match(web, /signature='home\|laptop\|general\|settings'/);
assert.doesNotMatch(web, /navButton\('computer','Computer'\)/);
assert.doesNotMatch(web, /navButton\('console','Console'\)/);
assert.doesNotMatch(web, /navButton\('gp','GP'\)/);

console.log('Mobile Android navigation parity contract OK');
