import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read=p=>fs.readFileSync(p,'utf8');
const home=read('admin/admin-home.js');
const css=read('admin/mobile-system-parity.css');
const followupCss=read('admin/mobile-screenshot-followup.css');
const js=read('admin/mobile-system-parity.js');
const controlCentre=read('admin/control-centre-visual.js');
const releaseFallback=read('admin/release-fetch-fallback.js');
const guardian=read('admin/guardian.html');
const guardianCss=read('admin/guardian-admin-parity.css');

test('Admin loads the authoritative mobile parity and authenticated screenshot layers last',()=>{
  assert.match(home,/control-centre-visual\.js\?v=2/);
  assert.match(home,/mobile-system-parity\.css\?v=1/);
  assert.match(home,/mobile-system-parity\.js\?v=2/);
  assert.match(home,/mobile-screenshot-followup\.css\?v=3/);
  assert.match(home,/release-fetch-fallback\.js\?v=1/);
  assert.ok(home.indexOf('mobile-screenshot-followup.css?v=3')>home.indexOf('mobile-system-parity.css?v=1'));
});

test('mobile navigation cannot regress to clipped secondary tabs',()=>{
  assert.match(css,/grid-template-columns:repeat\(5,minmax\(0,1fr\)\)/);
  assert.match(css,/\.tabs \.admin-secondary[^\n]*display:none!important/);
  assert.match(js,/data\.currentSecondary/);
});

test('mobile controls distinguish read-only feature state from maintenance editability',()=>{
  assert.match(js,/input\.dataset\.flag==='maintenanceMode'/);
  assert.match(js,/input\.disabled=true/);
  assert.match(css,/Read-only/);
});

test('release verification failure cannot leave stale values looking verified',()=>{
  assert.match(js,/\['releaseName','releaseCode','releaseSha','releaseUrl','releaseNotes'\]/);
  assert.match(js,/metric\.textContent='Unverified'/);
  assert.match(js,/Published OTA verification is unavailable/);
});

test('release fetch retries repository assets from the same origin in Android WebView',()=>{
  assert.match(releaseFallback,/\.\.\/ota\/latest\.json/);
  assert.match(releaseFallback,/\.\.\/android\/app\/build\.gradle/);
  assert.match(releaseFallback,/refreshVerifiedOtaRelease\(true\)/);
});

test('overview version evidence is labelled or explicitly unavailable, never decorative bars',()=>{
  assert.match(js,/admin-version-evidence-row/);
  assert.match(js,/admin-version-evidence-label/);
  assert.match(js,/Installed-version evidence is unavailable/);
  assert.match(controlCentre,/admin-version-evidence-row/);
  assert.match(controlCentre,/admin-version-evidence-label/);
  assert.doesNotMatch(controlCentre,/target\.innerHTML=rows\.slice\(0,8\)\.map\(x=>`<i style=/);
  assert.match(css,/admin-version-evidence-track/);
});

test('release adoption dashboard is derived from the same rollout counters shown in Release',()=>{
  assert.match(controlCentre,/rolloutValue\('rolloutCurrent'\)/);
  assert.match(controlCentre,/rolloutValue\('rolloutOutdated'\)/);
  assert.match(controlCentre,/rolloutValue\('rolloutAhead'\)/);
  assert.match(controlCentre,/rolloutValue\('rolloutUnknown'\)/);
  assert.match(controlCentre,/const total=current\+outdated\+ahead\+unknown/);
});

test('authenticated screenshot regressions keep dark readable support, pricing, invites and checkbox controls',()=>{
  assert.match(followupCss,/#ticketsList \.ticket-row \.row-title\{color:#f7faff!important/);
  assert.match(followupCss,/#pricingList \.pricing-row \.row-title\{color:#f7faff!important/);
  assert.match(followupCss,/#tab-pricing input\[type="checkbox"\]/);
  assert.match(followupCss,/#invitesList \.row-title/);
  assert.match(followupCss,/#downloadInvitesList \.row-title/);
  assert.match(followupCss,/#tab-release input\[type="checkbox"\]/);
  assert.match(followupCss,/width:22px!important;height:22px!important/);
  assert.match(js,/cleanPricingNames/);
});

test('Guardian shares the Admin Control dark shell and structured domain layout',()=>{
  assert.match(guardian,/guardian-admin-parity\.css\?v=1/);
  assert.match(guardian,/theme-color" content="#050a14"/);
  assert.match(guardianCss,/\.guardian-platform-domain\{display:grid!important/);
  assert.match(guardianCss,/--gp-bg:#050a14/);
});
