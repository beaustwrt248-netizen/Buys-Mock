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

for (const label of ['Home', 'Computer', 'Console', 'GP']) assert.match(layout, new RegExp(`'${label}'`));
assert.match(workflow, /window\.show\?\.\('laptop'\)/);
assert.match(workflow, /window\.show\?\.\('desktop'\)/);
assert.match(workflow, /window\.show\?\.\('general'\)/);
assert.match(workspace, /morley-watchlist-updated/);
assert.match(watchlist, /localStorage\.setItem\(W/);
assert.match(deals, /localStorage\.setItem\(KEY/);
assert.doesNotMatch(layout, /observe\(main,\{subtree:true,attributes:true/);
assert.match(index, /smart-workspace-plus\.js\?v=3/);

console.log('Web release readiness contract OK');
