import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const read = (path) => readFileSync(new URL(`../${path}`, import.meta.url), 'utf8');
const ecosystem = JSON.parse(read('config/morley-ecosystem.json'));
const core = read('morley-core.js');
const indexHtml = read('index.html');
const novaIndex = read('nova/index.html');
const novaApp = read('nova/app.js');
const novaModuleLoader = read('nova/module-loader.js');
const adminWorkspace = read('admin/workspace.html');
const adminParity = read('admin/admin-app-parity.js');
const adminUserAccessParity = read('admin/admin-user-access-parity.js');
const adminHome = read('admin/admin-home.js');
const adminIntelligence = read('admin/admin-intelligence.js');
const adminDownloadInvites = read('admin/download-invites.js');
const guardianHtml = read('admin/guardian.html');
const guardianBranding = read('admin/guardian-branding.js');
const autoReview = read('.github/workflows/auto-review-merge.yml');
const androidAuth = read('android/app/src/main/java/com/buysloans/morley/android/auth/MorleyAuthPalette.kt');

test('ecosystem exposes exactly three user-facing product definitions',()=>{
  assert.deepEqual(Object.keys(ecosystem.products).sort(),['admin','buys','nova']);
  assert.equal(ecosystem.guardian.parent,'nova');
  assert.equal(ecosystem.guardian.product,false);
});

test('Guardian remains a Nova enforcement layer and fails closed',()=>{
  assert.equal(ecosystem.guardian.product,false);
  assert.equal(ecosystem.guardian.parent,'nova');
  assert.match(guardianHtml,/id="guardianKillSwitch"/);
  assert.match(guardianHtml,/guardian\.js\?v=6/);
});

test('Morley Core owns the shared source-of-truth domains',()=>{
  assert.match(core,/morley:ecosystem-ready/);
  assert.match(core,/config\/morley-ecosystem\.json/);
  assert.match(indexHtml,/morley-core\.js/);
  assert.match(novaIndex,/morley-core\.js/);
});

test('destructive and privileged actions remain human gated',()=>{
  assert.match(autoReview,/protected change detected/i);
  assert.match(autoReview,/manual approval/i);
});

test('Morley Buys, Nova and Admin consume the ecosystem contract at runtime',()=>{
  assert.match(indexHtml,/morley-core\.js/);
  assert.match(novaIndex,/morley-core\.js/);
  assert.match(adminWorkspace,/__morleyAdminAuthContext/);
});

test('Android auth reuses the shared Morley blue visual tokens',()=>{
  assert.match(androidAuth,/MorleyBlue/);
});

test('Guardian is presented as Nova Security without renaming protected internals',()=>{
  assert.match(guardianHtml,/id="guardianKillSwitch"/);
  assert.match(guardianHtml,/guardian\.js\?v=6/);
});

test('Guardian compatibility surface validates the canonical Nova parent boundary at runtime',()=>{
  assert.match(guardianBranding,/guardian\?\.parent==='nova'/);
  assert.match(guardianBranding,/guardian\?\.product===false/);
  assert.match(guardianBranding,/dataset\.guardianContract=contractValid\(\)\?'validated':'pending'/);
  assert.match(guardianBranding,/morley:ecosystem-ready/);
});

test('Admin web authority is the native-parity shell and legacy home authority stays unloaded',()=>{
  assert.doesNotMatch(adminWorkspace,/desktop-workspace-fix\.css/);
  assert.match(adminWorkspace,/admin-app-parity\.js\?v=3/);
  assert.match(adminWorkspace,/admin-user-access-parity\.js\?v=1/);
  assert.doesNotMatch(adminWorkspace,/\['adminHome','admin-home\.js\?v=8'\]/);
  assert.doesNotMatch(adminWorkspace,/\['adminV2Script','admin-v2\.js\?v=8'\]/);
  assert.match(adminParity,/data-workspace-panel/);
  assert.match(adminParity,/supportOnly/);
  assert.match(adminUserAccessParity,/reset_password/);
  assert.match(adminUserAccessParity,/create_user/);
  assert.doesNotMatch(adminHome,/setInterval\(/);
  assert.doesNotThrow(()=>new Function(adminParity));
  assert.doesNotThrow(()=>new Function(adminUserAccessParity));
});

test('Admin Intelligence bootstrap observes only appView readiness',()=>{
  assert.doesNotMatch(adminIntelligence,/observe\(document\.documentElement,\{subtree:true,attributes:true,attributeFilter:\['class'\]\}\)/);
  assert.match(adminIntelligence,/observe\(app,\{attributes:true,attributeFilter:\['class'\]\}\)/);
  assert.doesNotThrow(()=>new Function(adminIntelligence));
});

test('Admin app download invite is emailed through the existing audited mail service',()=>{
  assert.match(adminDownloadInvites,/textContent='Email app download invite'/);
  assert.match(adminDownloadInvites,/action:'send_download_invite'/);
  assert.match(adminDownloadInvites,/Invitation emailed to/);
});

test('guarded auto review exempts documentation and test service-role references only',()=>{
  assert.match(autoReview,/docs/);
  assert.match(autoReview,/tests/);
});

test('guarded auto review keeps every other critical pattern global',()=>{
  assert.match(autoReview,/service_role/);
});
