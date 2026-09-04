const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');

const js=fs.readFileSync('mobile-app-parity-v5.js','utf8');
const css=fs.readFileSync('mobile-app-parity-v5.css','utf8');
const index=fs.readFileSync('index.html','utf8');
const product=fs.readFileSync('product-parity-v3.js','utf8');
const layout=fs.readFileSync('mobile-layout-fix.js','utf8');
const viewport=fs.readFileSync('mobile-viewport-lock.js','utf8');
const premium=fs.readFileSync('premium-motion.css','utf8');

test('v5 is the final production mobile CSS and JS authority',()=>{
  assert.match(index,/mobile-app-parity-v5\.css\?v=202609041/);
  assert.match(index,/mobile-app-parity-v5\.js\?v=202609041&route=2/);
  assert.ok(index.indexOf('mobile-app-parity-v5.css?v=202609041')>index.indexOf('mobile-app-parity-v4.css?v=202609044'));
  assert.ok(index.indexOf('mobile-app-parity-v5.js?v=202609041&route=2')>index.indexOf('mobile-app-parity-v4.js?v=202609044'));
});

test('v5 validates actual nav markup instead of trusting a stale dataset marker',()=>{
  assert.match(js,/const NAV_SIGNATURE='home\|categories\|general\|settings'/);
  assert.match(js,/function navSignature\(nav\)/);
  assert.match(js,/navSignature\(nav\)===NAV_SIGNATURE/);
  assert.match(js,/morley-bottom-nav-item/);
  assert.doesNotMatch(js,/<small>Computer<\/small>/);
  assert.doesNotMatch(js,/<small>Console<\/small>/);
});

test('v5 explicitly defeats the legacy premium blue button selector',()=>{
  assert.match(premium,/#2f7cff/);
  assert.match(css,/body#morleyMobileParityRoot button\{background-image:none!important\}/);
  assert.match(js,/document\.body\.id='morleyMobileParityRoot'/);
  assert.match(css,/nav#morleyBottomNav/);
  assert.doesNotMatch(css,/#2875ff|#2f7cff|#16c7ff|#12c9ff|#1f5fd8|#1f63e2|#12aeea/i);
});

test('mobile navigation matches the current Android destinations',()=>{
  for(const label of ['Home','Categories','General Buys','More']) assert.match(js,new RegExp(`'${label}'`));
  for(const category of ['Laptops','Desktops','Mobile Phones','Gaming Consoles']) assert.match(js,new RegExp(`<b>${category}</b>`));
  assert.match(js,/CATEGORY_IDS/);
});

test('phone navigation bypasses the desktop route allow-list',()=>{
  assert.match(js,/function mobileShow\(page\)/);
  assert.match(js,/if\(typeof window\.show==='function'\)window\.show\(page\)/);
  assert.match(js,/const go=page=>\{if\(mobile\(\)\)mobileShow\(page\);else if\(typeof window\.morleyDesktopGo==='function'\)window\.morleyDesktopGo\(page\)/);
  assert.match(js,/go\(b\.dataset\.page\)/);
  assert.match(js,/go\(b\.dataset\.target\)/);
});

test('mobile Home mirrors Android ParityHome structure',()=>{
  assert.match(js,/function alignWorkspace\(\)/);
  assert.match(js,/morley-potential-margin/);
  assert.match(js,/Quick Deal Mode/);
  assert.match(js,/NFC scanning is available in the Android app\./);
  assert.match(js,/Open Valuations & Deals/);
  assert.match(js,/morleyAndroidHomeExtras/);
  assert.match(js,/LIVE PRICING/);
  assert.match(js,/ONLINE STATUS/);
  assert.match(js,/A \/ B \/ C \/ Luxury buying targets/);
});

test('legacy product navigation yields to the v5 mobile owner',()=>{
  assert.match(product,/const mobileAndroidOwner=/);
  assert.match(product,/if\(nav&&!mobileAndroidOwner\(\)\)/);
  assert.match(product,/function syncActiveNav\(\)\{if\(mobileAndroidOwner\(\)\)return/);
  assert.match(product,/home\|computer\|console\|general/);
});

test('legacy mobile layout stops injecting and hiding navigation under v5',()=>{
  assert.match(layout,/function parityV5OwnsMobile\(\)/);
  assert.match(layout,/if\(parityV5OwnsMobile\(\)\)\{/);
  assert.match(layout,/morleyMobileLayoutFinal/);
  assert.match(layout,/style\.removeProperty\('display'\)/);
});

test('viewport lock remains geometry-only below the v5 visual authority',()=>{
  assert.match(viewport,/data-morley-mobile-app-parity="5"/);
  assert.match(viewport,/document\.head\.insertBefore\(link,parity\)/);
  assert.doesNotMatch(viewport,/Moving the link to the end makes this the authoritative layer/);
});

test('v5 remains defensive if any future legacy writer mutates the DOM or head',()=>{
  assert.match(js,/MutationObserver/);
  assert.match(js,/attributeFilter:\['class','style','data-page'\]/);
  assert.match(js,/headObserver\.observe\(document\.head/);
});
