import assert from 'node:assert/strict';
import fs from 'node:fs';

const gradle = fs.readFileSync('android/adminapp/build.gradle', 'utf8');
const activity = fs.readFileSync('android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt', 'utf8');
const gate = fs.readFileSync('android/adminapp/src/main/java/com/buysloans/admin/AdminUpdateGateActivity.kt', 'utf8');
const styles = fs.readFileSync('android/adminapp/src/main/res/values/styles.xml', 'utf8');
const styles27 = fs.readFileSync('android/adminapp/src/main/res/values-v27/styles.xml', 'utf8');
const nav = fs.readFileSync('admin/mobile-bottom-nav-fix.css', 'utf8');

assert.match(gradle, /versionCode\s+33/);
assert.match(gradle, /versionName\s+'0\.1\.32'/);
assert.match(gradle, /manifestPlaceholders\s*=\s*\[appLabel:\s*'Morley Admin Recovery'\]/);
assert.match(gradle, /buildConfigField\s+'boolean',\s*'IS_RECOVERY_BUILD',\s*'true'/);
assert.match(gradle, /applicationIdSuffix\s+'\.recovery'/);

assert.match(activity, /BuildConfig\.IS_RECOVERY_BUILD/);
assert.match(activity, /RECOVERY ADMIN/);
assert.match(activity, /createRecoveryIdentityBadge/);
assert.match(activity, /FrameLayout/);
assert.match(activity, /cacheMode = WebSettings\.LOAD_DEFAULT/);
assert.doesNotMatch(activity, /cacheMode = WebSettings\.LOAD_NO_CACHE/);
assert.doesNotMatch(activity, /Cache-Control/);
assert.doesNotMatch(activity, /Pragma/);
assert.match(activity, /if \(savedInstanceState == null\) webView\.loadUrl\(AdminWebParityPolicy\.HOME_URL\)/);
assert.match(activity, /onRenderProcessGone/);
assert.match(activity, /rendererGone = true/);
assert.match(activity, /view\.destroy\(\)/);
assert.match(activity, /recreate\(\)/);
assert.match(activity, /setBackgroundColor\(Color\.rgb\(4, 9, 18\)\)/);
assert.match(activity, /window\.decorView\.setBackgroundColor\(Color\.rgb\(4, 9, 18\)\)/);

assert.match(gate, /if \(BuildConfig\.IS_RECOVERY_BUILD\)/);
assert.match(gate, /startActivity\(Intent\(this, AdminActivity::class\.java\)\)/);
assert.match(gate, /darkColorScheme/);
assert.match(gate, /Color\(0xFF040912\)/);
assert.match(gate, /window\.decorView\.setBackgroundColor\(AndroidColor\.rgb\(4, 9, 18\)\)/);

for (const source of [styles, styles27]) {
  assert.match(source, /<item name="android:windowBackground">#040912<\/item>/);
  assert.match(source, /<item name="android:colorBackground">#040912<\/item>/);
}

assert.match(nav, /--admin-mobile-safe-bottom:env\(safe-area-inset-bottom,0px\)/);
assert.match(nav, /--admin-mobile-nav-shell-height:calc\(var\(--admin-mobile-nav-height\) \+ var\(--admin-mobile-safe-bottom\)\)/);
assert.match(nav, /bottom:0!important/);
assert.match(nav, /padding:7px 7px calc\(7px \+ var\(--admin-mobile-safe-bottom\)\)!important/);
assert.match(nav, /padding-bottom:var\(--admin-mobile-bottom-clearance\)!important/);

console.log('Admin recovery identity, stable login navigation, renderer crash containment and mobile dock contracts verified.');
