import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const webSecurity = readFileSync(new URL('../admin/login-security.js', import.meta.url), 'utf8');
const preAppPolicy = readFileSync(new URL('../admin/user-management-policy.js', import.meta.url), 'utf8');
const adminActivity = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt', import.meta.url), 'utf8');
const adminWebPolicy = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminWebParityPolicy.kt', import.meta.url), 'utf8');

test('Admin web renders Turnstile directly instead of nesting the challenge in another iframe', () => {
  assert.match(webSecurity, /challenges\.cloudflare\.com\/turnstile\/v0\/api\.js\?render=explicit/);
  assert.match(webSecurity, /turnstile\.render\(challengeHost/);
  assert.match(webSecurity, /challengeWatchdog/);
  assert.doesNotMatch(webSecurity, /frame\.src=['"]turnstile\.html/);
});

test('native Admin mode suppresses web Turnstile and waits for Android session installation', () => {
  assert.match(webSecurity, /nativeAuthMode/);
  assert.match(webSecurity, /if\s*\(nativeAuthMode\)/);
  assert.match(preAppPolicy, /nativeAuthMode/);
  assert.match(preAppPolicy, /window\.supabase\.createClient/);
  assert.match(preAppPolicy, /window\.installNativeAdminSession/);
  assert.match(preAppPolicy, /auth\.setSession/);
  assert.match(preAppPolicy, /nativeSessionGate/);
  assert.match(preAppPolicy, /__morleyNativeSessionState\s*=\s*['"]ready['"]/);
});

test('Admin Android injects the verified session into the final workspace without bootstrap navigation', () => {
  assert.match(adminWebPolicy, /nativeWorkspaceUrl/);
  assert.match(adminActivity, /AdminWebParityPolicy\.nativeWorkspaceUrl/);
  assert.match(adminActivity, /window\.installNativeAdminSession/);
  assert.match(adminActivity, /__morleyNativeSessionState/);
  assert.doesNotMatch(adminActivity, /AdminWebParityPolicy\.nativeSessionBootstrapUrl/);
  assert.doesNotMatch(adminActivity, /window\.sb\.auth\.setSession/);
  assert.doesNotMatch(adminActivity, /addJavascriptInterface/);
});
