import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const adminIndex = fs.readFileSync(new URL('../admin/index.html', import.meta.url), 'utf8');
const boundaryCss = fs.readFileSync(new URL('../admin/auth-boundary.css', import.meta.url), 'utf8');
const boundaryJs = fs.readFileSync(new URL('../admin/auth-boundary.js', import.meta.url), 'utf8');
const adminV2 = fs.readFileSync(new URL('../admin/admin-v2.js', import.meta.url), 'utf8');
const adminActivity = fs.readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt', import.meta.url), 'utf8');
const adminGradle = fs.readFileSync(new URL('../android/adminapp/build.gradle', import.meta.url), 'utf8');

test('Admin privileged workspace fails closed before authentication', () => {
  assert.match(adminIndex, /auth-boundary\.css\?v=1/);
  assert.match(adminIndex, /auth-boundary\.js\?v=1/);
  assert.match(boundaryCss, /html:not\(\.admin-authenticated\) #appView/);
  assert.match(boundaryCss, /html:not\(\.admin-authenticated\) #adminMoreMenu/);
  assert.match(boundaryJs, /classList\.toggle\('admin-authenticated',ok\)/);
});

test('enhanced mobile Admin navigation stays inside authenticated appView', () => {
  assert.match(adminIndex, /admin-v2\.js\?v=7/);
  assert.match(adminV2, /\(q\('#appView'\)\|\|nav\)\.appendChild\(menu\)/);
  assert.doesNotMatch(adminV2, /document\.body\.appendChild\(menu\)/);
  assert.match(adminV2, /menu\.dataset\.adminPrivilegedNav='true'/);
});

test('Admin Android WebView cannot preserve a stale hosted login shell', () => {
  assert.match(adminActivity, /cacheMode = WebSettings\.LOAD_NO_CACHE/);
  assert.match(adminActivity, /webView\.clearCache\(true\)/);
  assert.match(adminGradle, /versionCode 36/);
  assert.match(adminGradle, /versionName '0\.1\.35'/);
});
