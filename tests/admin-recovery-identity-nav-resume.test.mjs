import assert from 'node:assert/strict';
import fs from 'node:fs';

const gradle = fs.readFileSync('android/adminapp/build.gradle', 'utf8');
const activity = fs.readFileSync('android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt', 'utf8');
const styles = fs.readFileSync('android/adminapp/src/main/res/values/styles.xml', 'utf8');
const styles27 = fs.readFileSync('android/adminapp/src/main/res/values-v27/styles.xml', 'utf8');
const nav = fs.readFileSync('admin/mobile-bottom-nav-fix.css', 'utf8');

assert.match(gradle, /versionCode\s+31/);
assert.match(gradle, /versionName\s+'0\.1\.30'/);
assert.match(gradle, /manifestPlaceholders\s*=\s*\[appLabel:\s*'Morley Admin Recovery'\]/);
assert.match(gradle, /buildConfigField\s+'boolean',\s*'IS_RECOVERY_BUILD',\s*'true'/);
assert.match(gradle, /applicationIdSuffix\s+'\.recovery'/);

assert.match(activity, /BuildConfig\.IS_RECOVERY_BUILD/);
assert.match(activity, /RECOVERY ADMIN/);
assert.match(activity, /document\.title\s*=\s*'Morley Admin Recovery'/);
assert.match(activity, /pointerEvents:\s*'none'/);
assert.match(activity, /setBackgroundColor\(Color\.rgb\(4, 9, 18\)\)/);
assert.match(activity, /window\.decorView\.setBackgroundColor\(Color\.rgb\(4, 9, 18\)\)/);
assert.match(activity, /if \(savedInstanceState == null\) webView\.loadUrl/);

for (const source of [styles, styles27]) {
  assert.match(source, /<item name="android:windowBackground">#040912<\/item>/);
  assert.match(source, /<item name="android:colorBackground">#040912<\/item>/);
}

assert.match(nav, /--admin-mobile-safe-bottom:env\(safe-area-inset-bottom,0px\)/);
assert.match(nav, /--admin-mobile-nav-shell-height:calc\(var\(--admin-mobile-nav-height\) \+ var\(--admin-mobile-safe-bottom\)\)/);
assert.match(nav, /bottom:0!important/);
assert.match(nav, /padding:7px 7px calc\(7px \+ var\(--admin-mobile-safe-bottom\)\)!important/);
assert.match(nav, /padding-bottom:var\(--admin-mobile-bottom-clearance\)!important/);

console.log('Admin recovery identity, mobile dock and resume contracts verified.');
