import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const workspaceActivity = fs.readFileSync('android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt', 'utf8');
const nativeDashboard = fs.readFileSync('android/adminapp/src/main/java/com/buysloans/admin/AdminNativeDashboard.kt', 'utf8');
const loginActivity = fs.readFileSync('android/adminapp/src/main/java/com/buysloans/admin/AdminLoginActivity.kt', 'utf8');
const captchaChallenge = fs.readFileSync('android/adminapp/src/main/java/com/buysloans/admin/CaptchaChallenge.kt', 'utf8');
const gateActivity = fs.readFileSync('android/adminapp/src/main/java/com/buysloans/admin/AdminUpdateGateActivity.kt', 'utf8');
const manifest = fs.readFileSync('android/adminapp/src/main/AndroidManifest.xml', 'utf8');

test('Admin Android credential entry is native instead of HTML WebView input', () => {
  assert.match(loginActivity, /OutlinedTextField\(/);
  assert.match(loginActivity, /label\s*=\s*\{\s*Text\("Email"\)/);
  assert.match(loginActivity, /label\s*=\s*\{\s*Text\("Password"\)/);
  assert.match(loginActivity, /PasswordVisualTransformation\(\)/);
  assert.doesNotMatch(workspaceActivity, /MotionEvent|setOnTouchListener|requestFocusFromTouch/);
});

test('Admin native login uses one dedicated mandatory Turnstile WebView', () => {
  assert.match(loginActivity, /CaptchaChallenge\(/);
  assert.match(captchaChallenge, /TURNSTILE_URL\s*=\s*"https:\/\/buyshub\.me\/admin\/turnstile\.html"/);
  assert.equal((captchaChallenge.match(/addJavascriptInterface\(/g) || []).length, 1);
  assert.match(captchaChallenge, /addJavascriptInterface\([\s\S]*?"AndroidBridge"/);
  assert.match(loginActivity, /isAdminLoginReady\(email, password, captchaToken, busy\)/);
  assert.match(loginActivity, /AdminApi\.signIn\(email, password, token\)/);
});

test('launcher and keyboard route through the native Admin login activity', () => {
  assert.match(gateActivity, /AdminLoginActivity::class\.java/);
  assert.match(manifest, /android:name="\.AdminLoginActivity"[\s\S]*?android:windowSoftInputMode="adjustResize"/);
  assert.match(manifest, /android:name="\.AdminActivity"[\s\S]*?android:exported="false"/);
});

test('authenticated workspace contains no privileged WebView or browser session handoff', () => {
  assert.match(workspaceActivity, /AdminSessionStore\.current\(\)/);
  assert.match(workspaceActivity, /AdminNativeDashboard/);
  assert.doesNotMatch(workspaceActivity, /android\.webkit|WebView|CookieManager|evaluateJavascript|loadUrl\(|AdminWebParityPolicy|installNativeAdminSession|native-logout/);
  assert.doesNotMatch(nativeDashboard, /android\.webkit|WebView|evaluateJavascript|installNativeAdminSession/);
  assert.match(nativeDashboard, /NATIVE CONTROL MODE/);
});
