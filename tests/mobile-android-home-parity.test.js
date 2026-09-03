const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const read = file => fs.readFileSync(path.resolve(__dirname, '..', file), 'utf8');
const webHome = read('web-android-home-parity.js');
const webCss = read('web-android-home-parity.css');
const baseline = read('morley-ui-baseline.js');
const quickDeal = read('quick-deal-grade.js');
const androidHome = read('android/app/src/main/java/com/buysloans/hub/SmartWorkspaceSection.kt');
const androidTheme = read('android/app/src/main/java/com/buysloans/hub/MorleyVisualTheme.kt');

for (const label of ['VALUATIONS', 'WATCHLIST', 'OPPORTUNITIES', 'POTENTIAL GROSS MARGIN', 'Quick Deal Mode', 'Open Valuations & Deals', 'Refresh Smart Workspace']) {
  assert.match(androidHome, new RegExp(label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `Android home must contain ${label}`);
  assert.match(webHome, new RegExp(label.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')), `Mobile web parity layer must contain ${label}`);
}

assert.match(webHome, /NFC Scanner · Android only/, 'Web must identify NFC as Android-only rather than emulate it');
assert.match(baseline, /web-android-home-parity\.css\?v=1/);
assert.match(baseline, /web-android-home-parity\.js\?v=1/);
assert.doesNotMatch(baseline, /web-apk-home-parity/, 'Dead APK parity asset loader must stay removed');

for (const grade of ['A', 'B', 'C', 'Luxury']) {
  assert.match(quickDeal, new RegExp(`value="${grade}"`), `Quick Deal must offer ${grade} grade`);
}
assert.match(quickDeal, /Luxury:\.70/);

for (const token of ['#f5f7f4', '#ffffff', '#167a5a', '#0f684c', '#1c2b26', '#52645d', '#71827b', '#cedbd5', '#238a63', '#a86a12', '#c74755']) {
  assert.ok(androidTheme.toLowerCase().includes(token.replace('#', '0xff')), `Android theme must define ${token}`);
  assert.ok(webCss.toLowerCase().includes(token), `Mobile web theme must mirror ${token}`);
}

console.log('Mobile Android home parity contract OK');
