import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source = fs.readFileSync('android/adminapp/src/main/java/com/buysloans/admin/AdminActivity.kt', 'utf8');

test('Admin Android keeps reusable web resources cached without weakening auth freshness', () => {
  assert.match(source, /cacheMode = WebSettings\.LOAD_DEFAULT/);
  assert.doesNotMatch(source, /clearCache\(true\)/);
  assert.doesNotMatch(source, /ServiceWorkerController/);
  assert.doesNotMatch(source, /serviceWorkerWebSettings\.cacheMode = WebSettings\.LOAD_NO_CACHE/);
  assert.match(source, /CookieManager\.getInstance\(\)\.setAcceptCookie\(true\)/);
  assert.match(source, /setAcceptThirdPartyCookies\(this, true\)/);
  assert.match(source, /mixedContentMode = WebSettings\.MIXED_CONTENT_NEVER_ALLOW/);
  assert.match(source, /allowFileAccess = false/);
  assert.match(source, /allowContentAccess = false/);
});
