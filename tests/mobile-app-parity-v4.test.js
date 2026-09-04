const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');

const js=fs.readFileSync('mobile-app-parity-v4.js','utf8');
const css=fs.readFileSync('mobile-app-parity-v4.css','utf8');
const legacy=fs.readFileSync('mobile-layout-fix.js','utf8');
const index=fs.readFileSync('index.html','utf8');

test('mobile navigation matches Android compact destinations',()=>{
  for(const label of ['Home','Categories','General Buys','More']) assert.match(js,new RegExp(`<small>${label.replace(' ','\\s?')}</small>`));
  assert.match(js,/home\|categories\|general\|settings/);
  assert.doesNotMatch(js,/<small>Computer<\/small>/);
  assert.doesNotMatch(js,/<small>Console<\/small>/);
});

test('Categories merges the four Android pricing categories',()=>{
  for(const title of ['Laptops','Desktops','Mobile Phones','Gaming Consoles']) assert.match(js,new RegExp(`<b>${title}</b>`));
  assert.match(js,/data-target="laptop"/);
  assert.match(js,/data-target="desktop"/);
  assert.match(js,/data-target="mobilePhones"/);
  assert.match(js,/data-target="console"/);
  assert.match(js,/‹  Categories/);
});

test('phone pricing and cache-safe final parity assets load in the production bootstrap',()=>{
  assert.match(index,/morley-central-pricing\.js\?v=1/);
  assert.match(index,/web-universal-buy\.js\?v=2/);
  assert.match(index,/web-universal-buy\.css\?v=2/);
  assert.match(index,/mobile-layout-fix\.js\?v=20260904\.2/);
  assert.match(index,/mobile-app-parity-v4\.css\?v=20260904\.2/);
  assert.match(index,/mobile-app-parity-v4\.js\?v=20260904\.2/);
  assert.ok(index.indexOf('mobile-app-parity-v4.css?v=20260904.2')>index.indexOf('morley-desktop-dashboard.css?v=2'));
  assert.ok(index.indexOf('mobile-app-parity-v4.js?v=20260904.2')>index.indexOf('morley-desktop-dashboard.js?v=1'));
});

test('legacy mobile layout yields navigation ownership to parity v4',()=>{
  assert.match(legacy,/function parityV4OwnsNav\(\)/);
  assert.match(legacy,/data-morley-mobile-app-parity/);
  assert.match(legacy,/if\(!isMobile\(\)\|\|parityV4OwnsNav\(\)\) return/);
});

test('authoritative mobile theme is emerald-only and fixes reported home states',()=>{
  assert.match(css,/--morley-v4-accent:#167a5a/);
  assert.match(css,/--morley-v4-strong:#0f684c/);
  assert.match(css,/body>nav button\.active\{background:var\(--morley-v4-soft\)!important/);
  assert.match(css,/\.morley-menu-trigger/);
  assert.match(css,/background-image:none!important/);
  assert.doesNotMatch(css,/#2875ff|#2f7cff|#16c7ff|#12c9ff|linear-gradient\([^)]*(?:blue|#1f5fd8|#2f7cff)/i);
  assert.match(js,/NFC scanning is available in the Android app\./);
  assert.match(js,/Open Valuations & Deals/);
});