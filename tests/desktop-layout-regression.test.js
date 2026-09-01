const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const layout = fs.readFileSync(path.join(root, 'desktop-mode-layout-fix.js'), 'utf8');
const parity = fs.readFileSync(path.join(root, 'product-parity-v3.js'), 'utf8');

assert.match(layout, /body\.morley-desktop-menu-open #morleyDesktopShell/);
assert.match(layout, /aria-expanded/);
assert.match(layout, /morleyDesktopMenuScrim/);
assert.doesNotMatch(layout, /observe\(main,\{subtree:true,attributes:true/);
assert.match(parity, /records\.some\(record=>record\.target\.classList\?\.contains\('section'\)\)/);

console.log('Desktop layout regression contract OK');
