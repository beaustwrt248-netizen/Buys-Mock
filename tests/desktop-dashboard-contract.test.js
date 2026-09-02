const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const read = file => fs.readFileSync(path.join(root, file), 'utf8');
const css = read('morley-desktop-dashboard.css');
const dashboard = read('morley-desktop-dashboard.js');
const mobile = read('mobile-viewport-lock.css');
const index = read('index.html');

assert.match(css, /@media \(min-width:1000px\)/);
assert.match(css, /@media \(min-width:761px\) and \(max-width:999px\)/);
assert.doesNotMatch(css, /@media \(max-width:760px\)/);
assert.match(css, /#morleyDesktopShell/);
assert.match(css, /margin:0 0 0 272px/);
assert.match(css, /--morley-dash-emerald:#087a55/);
assert.match(css, /#home>\.stats/);
assert.match(css, /\.desktop-more-grid/);
assert.match(css, /table\{width:100%/);
assert.match(css, /#morleyMenuTrigger[\s\S]*display:none!important/);
assert.match(css, /\.desktop-header-actions\{display:none!important/);
assert.match(css, /\.morley-web-user\{top:24px!important;right:132px/);
assert.match(css, /\.msi-fill\{background:linear-gradient\(90deg,#087a55/);
assert.match(css, /\.desktop-side-status\{margin-top:auto!important/);
assert.match(css, /\.morley-dashboard-nav-icon svg\{display:block!important/);
assert.match(css, /#desktopPricingState[\s\S]*color:#7fe2bb!important/);
assert.match(css, /\.morley-web-signout\{background:#b44552!important/);
assert.match(css, /\.morley-menu-signout\{background:#b44552!important/);
assert.match(dashboard, /const icon=paths=>/);
assert.match(dashboard, /<svg viewBox="0 0 24 24"/);

for (const route of ['Dashboard','Computer Pricing','Console Pricing','General Buys','Saved Deals','Inventory','Scanner','Sales','More']) {
  assert.ok(dashboard.includes(route), `missing desktop route: ${route}`);
}
assert.match(dashboard, /window\.morleyDesktopGo/);
assert.match(dashboard, /\.section\.active/);
assert.match(mobile, /html\.morley-physical-phone/);
assert.match(mobile, /max-height:60px/);
assert.ok(index.indexOf('morley-desktop-dashboard.css') > index.indexOf('mobile-viewport-lock.css'));
assert.match(index, /morley-desktop-dashboard\.css\?v=2/);
assert.match(index, /morley-desktop-dashboard\.js\?v=1/);

console.log('Desktop finance dashboard and mobile boundary contract OK');
