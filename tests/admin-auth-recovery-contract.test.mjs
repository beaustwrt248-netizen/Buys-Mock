import test from 'node:test';
import assert from 'node:assert/strict';
import { existsSync, readFileSync } from 'node:fs';

const webSecurity = readFileSync(new URL('../admin/login-security.js', import.meta.url), 'utf8');
const adminActivity = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt', import.meta.url), 'utf8');
const bootstrapUrl = new URL('../admin/native-session-bootstrap.html', import.meta.url);

test('Admin web starts Turnstile without waiting for credentials', () => {
  assert.match(webSecurity, /loadChallenge\('Security check loading…'\);/);
  assert.match(webSecurity, /turnstile\.html\?v=6&load=/);
  assert.doesNotMatch(webSecurity, /Enter your email and password to begin/);
});

test('native Admin mode does not start a second web Turnstile challenge', () => {
  assert.match(webSecurity, /nativeAuthMode/);
  assert.match(webSecurity, /if\s*\(nativeAuthMode\)\s*return/);
});

test('native Admin session is established before the privileged workspace loads', () => {
  assert.equal(existsSync(bootstrapUrl), true, 'native session bootstrap page must exist');
  const bootstrap = readFileSync(bootstrapUrl, 'utf8');
  assert.match(bootstrap, /installNativeAdminSession/);
  assert.match(bootstrap, /auth\.setSession/);
  assert.match(bootstrap, /location\.replace/);
  assert.match(bootstrap, /nativeAuth=1/);
});

test('Admin Android loads the session bootstrap before loading the workspace', () => {
  assert.match(adminActivity, /AdminWebParityPolicy\.nativeSessionBootstrapUrl/);
  assert.match(adminActivity, /window\.installNativeAdminSession/);
  assert.doesNotMatch(adminActivity, /window\.sb\.auth\.setSession/);
  assert.doesNotMatch(adminActivity, /typeof window\.loadSession==='function'/);
});
