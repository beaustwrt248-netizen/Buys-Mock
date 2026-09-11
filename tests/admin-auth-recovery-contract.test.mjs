import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const webSecurity = readFileSync(new URL('../admin/login-security.js', import.meta.url), 'utf8');
const webApp = readFileSync(new URL('../admin/app.js', import.meta.url), 'utf8');
const adminActivity = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt', import.meta.url), 'utf8');

test('Admin web starts Turnstile without waiting for credentials', () => {
  assert.match(webSecurity, /loadChallenge\('Security check loading…'\);/);
  assert.match(webSecurity, /turnstile\.html\?v=6&load=/);
  assert.doesNotMatch(webSecurity, /Enter your email and password to begin/);
});

test('native Admin web mode waits for the verified native session instead of showing web login', () => {
  assert.match(webApp, /nativeAuthMode/);
  assert.match(webApp, /URLSearchParams\(location\.search\)/);
  assert.match(webApp, /window\.installNativeAdminSession\s*=\s*async/);
  assert.match(webApp, /if\s*\(!nativeAuthMode\)\s*loadSession\(\)/);
});

test('native Admin web mode does not start a second web Turnstile challenge', () => {
  assert.match(webSecurity, /nativeAuthMode/);
  assert.match(webSecurity, /if\s*\(nativeAuthMode\)\s*return/);
});

test('Admin Android delegates session installation to the web native-session contract', () => {
  assert.match(adminActivity, /window\.installNativeAdminSession/);
  assert.doesNotMatch(adminActivity, /window\.sb\.auth\.setSession/);
  assert.doesNotMatch(adminActivity, /typeof window\.loadSession==='function'/);
});
