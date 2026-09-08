import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const read=p=>fs.readFileSync(p,'utf8');
const home=read('admin/admin-home.js');
const css=read('admin/mobile-system-parity.css');
const js=read('admin/mobile-system-parity.js');
const guardian=read('admin/guardian.html');
const guardianCss=read('admin/guardian-admin-parity.css');

test('Admin loads the authoritative mobile parity layer last',()=>{
  assert.match(home,/mobile-system-parity\.css\?v=1/);
  assert.match(home,/mobile-system-parity\.js\?v=1/);
  assert.ok(home.indexOf('mobile-system-parity.css?v=1')>home.indexOf('mobile-workspace-fix.css?v=2'));
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

test('overview version evidence is labelled rather than decorative bars',()=>{
  assert.match(js,/admin-version-evidence-row/);
  assert.match(js,/admin-version-evidence-label/);
  assert.match(css,/admin-version-evidence-track/);
});

test('Guardian shares the Admin Control dark shell and structured domain layout',()=>{
  assert.match(guardian,/guardian-admin-parity\.css\?v=1/);
  assert.match(guardian,/theme-color" content="#050a14"/);
  assert.match(guardianCss,/\.guardian-platform-domain\{display:grid!important/);
  assert.match(guardianCss,/--gp-bg:#050a14/);
});
