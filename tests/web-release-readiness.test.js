const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const read = file => fs.readFileSync(path.join(root, file), 'utf8');
const index = read('index.html');
const workspace = read('smart-workspace.js');
const watchlist = read('smart-workspace-plus.js');
const workflow = read('workflow.js');
const deals = read('deal-workflow.js');
const layout = read('desktop-mode-layout-fix.js');
const desktop = read('desktop-parity.js');

for (const label of ['Home', 'Computer', 'Console', 'GP']) assert.match(layout, new RegExp(`'${label}'`));
assert.match(desktop, /const VALID=\['home','computer','console','laptop','desktop','general','settings'/);
assert.match(workflow, /window\.show\?\.\('laptop'\)/);
assert.match(workflow, /window\.show\?\.\('desktop'\)/);
assert.match(workflow, /window\.show\?\.\('general'\)/);
assert.match(workspace, /morley-watchlist-updated/);
assert.match(watchlist, /localStorage\.setItem\(W/);
assert.match(deals, /localStorage\.setItem\(KEY/);
assert.doesNotMatch(layout, /observe\(main,\{subtree:true,attributes:true/);
assert.match(index, /smart-workspace-plus\.js\?v=3/);
assert.match(index, /desktop-parity\.js\?v=5/);
assert.match(index, /morley-light-web\.css\?v=1/);
assert.ok(index.lastIndexOf('morley-light-web.css') > index.lastIndexOf('mobile-parity-v3.css'));

const lightTheme = read('morley-light-web.css');
for (const token of ['color-scheme:light', '#f5f7f4', '#ffffff', '#167a5a', '#1c2b26', '#cedbd5']) {
  assert.ok(lightTheme.toLowerCase().includes(token), `missing light theme token ${token}`);
}
assert.match(lightTheme, /#morleyWebAuth/);

console.log('Web release readiness contract OK');
