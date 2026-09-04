import fs from 'node:fs';
import assert from 'node:assert/strict';

const src = fs.readFileSync('android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt', 'utf8');
const gradle = fs.readFileSync('android/adminapp/build.gradle', 'utf8');

assert.match(src, /javaScriptEnabled = true/);
assert.match(src, /domStorageEnabled = true/);
assert.match(src, /loadWithOverviewMode = true/);
assert.match(src, /useWideViewPort = true/);
assert.match(src, /WebSettings\.getDefaultUserAgent\(this@AdminActivity\)/);
assert.doesNotMatch(src, /MorleyAdminAndroid/);
assert.match(src, /setAcceptThirdPartyCookies\(this, true\)/);
assert.match(src, /allowFileAccess = false/);
assert.match(src, /allowContentAccess = false/);
assert.match(src, /MIXED_CONTENT_NEVER_ALLOW/);
assert.match(gradle, /versionCode 25/);
assert.match(gradle, /versionName '0\.1\.24'/);
