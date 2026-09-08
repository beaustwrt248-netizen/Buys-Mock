import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const cssUrl = new URL('../admin/mobile-bottom-nav-fix.css', import.meta.url);

test('Admin mobile layout has one fixed bottom-nav owner and content clearance', async () => {
  const css = await readFile(cssUrl, 'utf8');
  assert.match(css, /--admin-mobile-bottom-clearance/);
  assert.match(css, /admin-nav-content-docked #appView\{padding-bottom:var\(--admin-mobile-bottom-clearance\)!important\}/);
  assert.match(css, /admin-nav-content-docked \.tabs\{position:fixed!important;top:auto!important/);
  assert.match(css, /grid-template-columns:repeat\(5,minmax\(0,1fr\)\)!important/);
  assert.doesNotMatch(css, /top:var\(--admin-nav-dock-top\)!important/);
});

test('Admin mobile panels share one gutter and dense rows stack safely', async () => {
  const css = await readFile(cssUrl, 'utf8');
  assert.match(css, /--admin-mobile-gutter:14px/);
  assert.match(css, /padding:12px var\(--admin-mobile-gutter\) 20px!important/);
  assert.match(css, /\.row\{align-items:stretch!important;flex-direction:column!important/);
  assert.match(css, /\.actions\{width:100%!important;display:grid!important/);
  assert.match(css, /\.mi-grid\{grid-template-columns:minmax\(0,1fr\)!important/);
});
