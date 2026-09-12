import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const workspaceActivity = fs.readFileSync('android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt', 'utf8');
const loginActivity = fs.readFileSync('android/adminapp/src/main/java/com/buysloans/admin/AdminLoginActivity.kt', 'utf8');
const gateActivity = fs.readFileSync('android/adminapp/src/main/java/com/buysloans/admin/AdminUpdateGateActivity.kt', 'utf8');
const manifest = fs.readFileSync('android/adminapp/src/main/AndroidManifest.xml', 'utf8');

test('Admin Android credential entry is native instead of HTML WebView input', () => {
  assert.match(loginActivity, /OutlinedTextField\(/);
  assert.match(loginActivity, /label\s*=\s*\{\s*Text\("Email"\)/);
  assert.match(loginActivity, /label\s*=\s*\{\s*Text\("Password"\)/);
  assert.match(loginActivity, /PasswordVisualTransformation\(\)/);
  assert.doesNotMatch(workspaceActivity, /MotionEvent|setOnTouchListener|requestFocusFromTouch/);
});

test('Admin native login uses a dedicated mandatory Turnstile WebView', () => {
  assert.match(loginActivity, /ADMIN_TURNSTILE_PAGE\s*=\s*"https:\/\/buyshub\.me\/admin\/turnstile\.html"/);
  assert.match(loginActivity, /addJavascriptInterface\([\s\S]*?"AndroidBridge"/);
  assert.match(loginActivity, /isAdminLoginReady\(email, password, captchaToken, busy\)/);
  assert.match(loginActivity, /AdminApi\.signIn\(email, password, token\)/);
});

test('launcher and keyboard route through the native Admin login activity', () => {
  assert.match(gateActivity, /AdminLoginActivity::class\.java/);
  assert.match(manifest, /android:name="\.AdminLoginActivity"[\s\S]*?android:windowSoftInputMode="adjustResize"/);
  assert.match(manifest, /android:name="\.AdminActivity"[\s\S]*?android:exported="false"/);
});

test('authenticated workspace remains pinned to trusted Admin web origin and installs native session there', () => {
  assert.match(workspaceActivity, /allowFileAccess\s*=\s*false/);
  assert.match(workspaceActivity, /allowContentAccess\s*=\s*false/);
  assert.match(workspaceActivity, /mixedContentMode\s*=\s*WebSettings\.MIXED_CONTENT_NEVER_ALLOW/);
  assert.match(workspaceActivity, /AdminWebParityPolicy\.isTrustedAdminUrl/);
  assert.match(workspaceActivity, /JSONObject\.quote\(accessToken\)/);
  assert.match(workspaceActivity, /AdminWebParityPolicy\.nativeWorkspaceUrl/);
  assert.match(workspaceActivity, /window\.installNativeAdminSession/);
  assert.match(workspaceActivity, /__morleyNativeSessionState/);
  assert.doesNotMatch(workspaceActivity, /window\.sb\.auth\.setSession/);
});
