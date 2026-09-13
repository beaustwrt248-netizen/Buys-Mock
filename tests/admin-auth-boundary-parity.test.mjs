import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const adminIndex = fs.readFileSync(new URL('../admin/index.html', import.meta.url), 'utf8');
const boundaryCss = fs.readFileSync(new URL('../admin/auth-boundary.css', import.meta.url), 'utf8');
const boundaryJs = fs.readFileSync(new URL('../admin/auth-boundary.js', import.meta.url), 'utf8');
const adminWorkspace = fs.readFileSync(new URL('../admin/workspace.html', import.meta.url), 'utf8');
const adminActivity = fs.readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt', import.meta.url), 'utf8');

test('Admin privileged workspace fails closed before authentication', () => {
  assert.match(adminIndex, /auth-boundary\.css\?v=1/);
  assert.doesNotMatch(adminIndex, /auth-boundary\.js|admin-v2\.js|app\.js/);
  assert.match(adminWorkspace, /adminAuthBoundary','auth-boundary\.js\?v=2/);
  assert.match(boundaryCss, /html:not\(\.admin-authenticated\) #appView/);
  assert.match(boundaryCss, /html:not\(\.admin-authenticated\) #adminMoreMenu/);
  assert.match(boundaryJs, /classList\.toggle\('admin-authenticated',ok\)/);
});

test('enhanced mobile Admin navigation stays inside authenticated appView', () => {
  assert.match(adminWorkspace, /adminV2Script','admin-v2\.js\?v=8/);
  assert.ok(adminWorkspace.indexOf('window.__morleyAdminAuthContext') < adminWorkspace.indexOf("'admin-v2.js?v=8'"));
});

test('Admin Android uses the authorised native dashboard instead of a hosted login shell', () => {
  assert.match(adminActivity, /AdminSessionStore\.hasAuthorizedSession\(\)/);
  assert.match(adminActivity, /AdminNativeDashboard\(/);
  assert.doesNotMatch(adminActivity, /WebView|loadUrl\(/);
});
