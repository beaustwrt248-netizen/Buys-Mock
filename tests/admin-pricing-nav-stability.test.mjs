import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const css = fs.readFileSync(new URL('../admin/admin-photo-parity-v2.css', import.meta.url), 'utf8');
const pricing = fs.readFileSync(new URL('../admin/pricing-management.js', import.meta.url), 'utf8');

test('Pricing Assistant cannot widen the Admin viewport or move the mobile navigation', () => {
  assert.match(css, /body\.admin-v2 #tab-pricing\{overflow-x:clip!important;width:100%!important\}/);
  assert.match(css, /body\.admin-v2 #pricingList \.pricing-row\{display:grid!important;grid-template-columns:minmax\(0,1fr\) auto!important/);
  assert.match(css, /@media\(max-width:900px\)[\s\S]*body\.admin-v2 \.tabs\{position:fixed!important;left:12px!important;right:12px!important;bottom:max\(10px,env\(safe-area-inset-bottom\)\)!important;top:auto!important;width:auto!important;max-width:none!important;margin:0!important;box-sizing:border-box!important;transform:none!important;translate:none!important;contain:layout paint!important\}/);
  assert.match(css, /@media\(max-width:430px\)\{body\.admin-v2 \.tabs\{left:8px!important;right:8px!important\}\}/);
});

test('desktop navigation keeps an invariant sidebar width while Pricing grows vertically', () => {
  assert.match(css, /@media\(min-width:901px\)\{html\{scrollbar-gutter:stable\}body\.admin-v2 \.tabs\{align-self:start!important;width:245px!important;min-width:245px!important;max-width:245px!important/);
});

test('Pricing Assistant remains dynamically installed without replacing navigation ownership', () => {
  assert.match(pricing, /function installAssistant\(\)/);
  assert.match(pricing, /panel\.insertBefore\(card,panel\.firstElementChild\)/);
  assert.doesNotMatch(pricing, /\.tabs\s*=/);
});
