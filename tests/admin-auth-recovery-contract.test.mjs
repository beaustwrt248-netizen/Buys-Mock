import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const webSecurity = readFileSync(new URL('../admin/login-security.js', import.meta.url), 'utf8');
const adminIndex = readFileSync(new URL('../admin/index.html', import.meta.url), 'utf8');
const adminActivity = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt', import.meta.url), 'utf8');
const adminLogin = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminLoginActivity.kt', import.meta.url), 'utf8');
const captchaChallenge = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/CaptchaChallenge.kt', import.meta.url), 'utf8');
const sessionStore = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminSessionStore.kt', import.meta.url), 'utf8');
const nativeDashboard = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminNativeDashboard.kt', import.meta.url), 'utf8');

test('Admin browser login renders Turnstile at top level and recovers on mobile and desktop', () => {
  assert.match(adminIndex, /id="adminTurnstileFrame"/);
  assert.match(webSecurity, /adminTurnstileWidget/);
  assert.match(webSecurity, /legacyFrame\.replaceWith\(widget\)/);
  assert.match(webSecurity, /challenges\.cloudflare\.com\/turnstile\/v0\/api\.js\?render=explicit/);
  assert.match(webSecurity, /window\.turnstile\.render\(/);
  assert.match(webSecurity, /captchaToken:token/);
  assert.match(webSecurity, /challengeWatchdog/);
  assert.match(webSecurity, /Security check unavailable\. Tap here to retry\./);
  assert.match(webSecurity, /visibilitychange/);
  assert.match(webSecurity, /pageshow/);
  assert.match(webSecurity, /event\.source===frame\.contentWindow/);
  assert.match(webSecurity, /event\.origin===window\.location\.origin/);
  assert.doesNotMatch(webSecurity, /payload\.type===['"]token['"]/);
});

test('native Admin login owns the authorized Android session before workspace navigation', () => {
  assert.match(adminLogin, /AdminSessionStore\.set\(session\)/);
  assert.match(adminLogin, /Intent\(this, AdminActivity::class\.java\)/);
  assert.ok(adminLogin.indexOf('AdminSessionStore.set(session)') < adminLogin.indexOf('Intent(this, AdminActivity::class.java)'));
  assert.doesNotMatch(adminLogin, /EXTRA_ACCESS_TOKEN|EXTRA_REFRESH_TOKEN|putExtra\(AdminActivity/);
  assert.match(sessionStore, /private var active: AdminSession\?/);
  assert.match(sessionStore, /fun clear\(\)/);
});

test('authenticated Admin workspace is fully native and cannot fall back to browser login', () => {
  assert.match(adminActivity, /AdminSessionStore\.current\(\)/);
  assert.match(adminActivity, /AdminNativeDashboard/);
  assert.doesNotMatch(adminActivity, /WebView|evaluateJavascript|loadUrl\(|installNativeAdminSession|native-logout|AdminWebParityPolicy/);
  assert.match(nativeDashboard, /SupportOperationsPanel/);
  assert.match(nativeDashboard, /GuardianPanel/);
  assert.match(nativeDashboard, /UserManagementPanel/);
  assert.match(nativeDashboard, /AuditTimelinePanel/);
  assert.doesNotMatch(nativeDashboard, /WebView|evaluateJavascript|installNativeAdminSession/);
});

test('the only Android JavaScript bridge remains the scoped Turnstile challenge', () => {
  assert.doesNotMatch(adminLogin, /addJavascriptInterface/);
  assert.equal((captchaChallenge.match(/addJavascriptInterface\(/g) || []).length, 1);
  assert.match(captchaChallenge, /"AndroidBridge"/);
  assert.match(captchaChallenge, /domStorageEnabled\s*=\s*false/);
  assert.match(captchaChallenge, /allowFileAccess\s*=\s*false/);
  assert.match(captchaChallenge, /allowContentAccess\s*=\s*false/);
  assert.doesNotMatch(adminActivity, /addJavascriptInterface/);
  assert.doesNotMatch(nativeDashboard, /addJavascriptInterface/);
});
