import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const edge = fs.readFileSync('supabase/functions/ebay-search/index.ts', 'utf8');
const repair = fs.readFileSync('laptop-mobile-repair.js', 'utf8');
const index = fs.readFileSync('index.html', 'utf8');

test('pricing edge allows the production custom domain without wildcard CORS', () => {
  assert(edge.includes('"https://buyshub.me"'));
  assert(edge.includes('"https://www.buyshub.me"'));
  assert(edge.includes('ALLOWED_ORIGINS.has(origin)'));
  assert(!edge.includes('"Access-Control-Allow-Origin": "*"'));
});

test('mobile laptop repair canonicalises Lenovo MTM model numbers from pasted content', () => {
  assert(repair.includes('\\b\\d{2}[A-Z]\\d{3}[A-Z0-9]{4}\\b'));
  assert(repair.includes("$('lapModel').value=model"));
  assert(repair.includes("event.stopImmediatePropagation()"));
});

test('combined stock actions are gated until a successful valuation exists', () => {
  assert(repair.includes("document.querySelectorAll('[data-buy-stock=\"laptop\"]')"));
  assert(repair.includes("return !!add&&!add.disabled&&used>0&&!ERROR_STATUS.test(status)"));
  assert(repair.includes("setDisabled($('lapSaveStock'),!ready)"));
});

test('repair is loaded after the existing workspace scripts', () => {
  const repairAt=index.indexOf('laptop-mobile-repair.js?v=202609071');
  const parityAt=index.indexOf('mobile-app-parity-v5.js');
  assert(repairAt>parityAt);
});
