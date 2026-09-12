import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync, existsSync } from 'node:fs';

const webSecurity = readFileSync(new URL('../admin/login-security.js', import.meta.url), 'utf8');
const adminIndex = readFileSync(new URL('../admin/index.html', import.meta.url), 'utf8');
const adminApp = readFileSync(new URL('../admin/app.js', import.meta.url), 'utf8');
const workspaceLoaderUrl = new URL('../admin/workspace-loader.js', import.meta.url);
const workspaceLoader = existsSync(workspaceLoaderUrl) ? readFileSync(workspaceLoaderUrl, 'utf8') : '';
const adminActivity = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt', import.meta.url), 'utf8');
const adminLogin = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminLoginActivity.kt', import.meta.url), 'utf8');
const captchaChallenge = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/CaptchaChallenge.kt', import.meta.url), 'utf8');
const sessionStore = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminSessionStore.kt', import.meta.url), 'utf8');
const nativeDashboard = readFileSync(new URL('../android/adminapp/src/main/java/com/buysloans/admin/AdminNativeDashboard.kt', import.meta.url), 'utf8');

test('Admin browser login has direct and isolated fallback Turnstile recovery on mobile and desktop', () => {
  assert.match(adminIndex, /id="adminTurnstileFrame"/);
  assert.match(webSecurity, /adminTurnstileWidget/);
  assert.match(webSecurity, /legacyFrame\.replaceWith\(widget\)/);
  assert.match(webSecurity, /challenges\.cloudflare\.com\/turnstile\/v0\/api\.js\?render=explicit/);
  assert.match(webSecurity, /window\.turnstile\.render\(/);
  assert.match(webSecurity, /function startFallback/);
  assert.match(webSecurity, /turnstile\.html\?v=7&browser=1/);
  assert.match(webSecurity, /captchaToken:token/);
  assert.match(webSecurity, /challengeWatchdog/);
  assert.match(webSecurity, /Security check unavailable\. Tap here to retry\./);
  assert.match(webSecurity, /visibilitychange/);
  assert.match(webSecurity, /pageshow/);
  assert.match(webSecurity, /event\.source===frame\.contentWindow/);
  assert.match(webSecurity, /event\.origin===window\.location\.origin/);
  assert.match(webSecurity, /payload\.source!==['"]morley-turnstile['"]/);
  assert.match(webSecurity, /payload\.type===['"]token['"]/);
});

test('logged-out Admin browser keeps workspace code off the authentication critical path', () => {
  const workspaceScripts = [
    'user-management-policy.js',
    'release-control.js',
    'targeted-notifications.js',
    'invites.js',
    'download-invites.js',
    'support-tickets.js',
    'audit-triage.js',
    'control-governance.js',
    'pricing-management.js',
    'admin-v2.js',
    'admin-home.js'
  ];
  assert.match(adminIndex, /workspace-loader\.js\?v=1/);
  for (const script of workspaceScripts) {
    assert.doesNotMatch(adminIndex, new RegExp(`<script[^>]+src=["'][^"']*${script.replaceAll('.', '\\.')}`), `${script} must not execute before Admin authorization`);
    assert.match(workspaceLoader, new RegExp(script.replaceAll('.', '\\.')), `${script} must remain available through the authenticated workspace loader`);
  }
  assert.match(adminApp, /await\s+window\.loadAdminWorkspace\(\)/);
  assert.ok(adminApp.indexOf('await window.loadAdminWorkspace()') < adminApp.indexOf('await refreshAll()'), 'workspace must finish loading before the first privileged refresh');
  assert.match(workspaceLoader, /window\.loadAdminWorkspace\s*=/);
  assert.match(workspaceLoader, /workspacePromise/);
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
