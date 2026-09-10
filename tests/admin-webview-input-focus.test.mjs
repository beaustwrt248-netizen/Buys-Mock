import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const activity = fs.readFileSync('android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt', 'utf8');
const manifest = fs.readFileSync('android/adminapp/src/main/AndroidManifest.xml', 'utf8');

test('Admin WebView remains focusable for HTML email/password fields', () => {
  assert.match(activity, /isFocusable\s*=\s*true/);
  assert.match(activity, /isFocusableInTouchMode\s*=\s*true/);
  assert.match(activity, /requestFocusFromTouch\(\)/);
  assert.match(activity, /event\.action\s*==\s*MotionEvent\.ACTION_DOWN/);
});

test('touch focus handoff does not consume taps or Turnstile interaction', () => {
  assert.match(activity, /setOnTouchListener\s*\{[\s\S]*?requestFocusFromTouch\(\)[\s\S]*?false\s*\}/);
});

test('software keyboard resizes the Admin activity instead of obscuring login inputs', () => {
  assert.match(activity, /SOFT_INPUT_ADJUST_RESIZE/);
  assert.match(manifest, /android:name="\.AdminActivity"[\s\S]*?android:windowSoftInputMode="adjustResize"/);
});

test('WebView input fix does not relax privileged WebView security controls', () => {
  assert.match(activity, /allowFileAccess\s*=\s*false/);
  assert.match(activity, /allowContentAccess\s*=\s*false/);
  assert.match(activity, /mixedContentMode\s*=\s*WebSettings\.MIXED_CONTENT_NEVER_ALLOW/);
  assert.match(activity, /AdminWebParityPolicy\.isTrustedAdminUrl/);
});
