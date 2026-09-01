const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const layout = fs.readFileSync(path.join(root, 'desktop-mode-layout-fix.js'), 'utf8');
const parity = fs.readFileSync(path.join(root, 'product-parity-v3.js'), 'utf8');
const desktop = fs.readFileSync(path.join(root, 'desktop-parity.js'), 'utf8');

assert.match(layout, /body\.morley-desktop-menu-open #morleyDesktopShell/);
assert.match(layout, /aria-expanded/);
assert.match(layout, /morleyDesktopMenuScrim/);
assert.match(layout, /background:rgba\(0,0,0,\.48\)!important/);
assert.match(layout, /desktop-side-nav\{display:grid!important;grid-template-columns:minmax\(0,1fr\)!important/);
assert.match(layout, /\$\('body>nav'\)/);
assert.doesNotMatch(layout, /observe\(main,\{subtree:true,attributes:true/);
assert.match(parity, /records\.some\(record=>record\.target\.classList\?\.contains\('section'\)\)/);
assert.match(parity, /\$\('body>nav'\)/);
assert.match(desktop, /if\(canonical\)\{\$\('#desktopQuickDeal'\)\?\.remove\(\)/);

console.log('Desktop layout regression contract OK');
