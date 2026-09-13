import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here=path.dirname(fileURLToPath(import.meta.url));
const root=path.resolve(here,'..','android');
const gradle=fs.readFileSync(path.join(root,'app','build.gradle'),'utf8');
const manifest=fs.readFileSync(path.join(root,'app','src','main','AndroidManifest.xml'),'utf8');
const activity=fs.readFileSync(path.join(root,'app','src','main','java','com','buysloans','novanext','MainActivity.java'),'utf8');

assert.match(gradle,/applicationId\s+['"]com\.buysloans\.novanext['"]/);
assert.doesNotMatch(gradle,/applicationId\s+['"]com\.buysloans\.nova['"]/);
assert.match(gradle,/buildConfigField\s+['"]String['"],\s*['"]NOVA_NEXT_URL['"],\s*['"]\\?"https:\/\/buyshub\.me\/nova-next\/\\?"['"]/);
assert.match(manifest,/android\.permission\.INTERNET/);
assert.match(manifest,/android:usesCleartextTraffic="false"/);
assert.match(activity,/webView\.loadUrl\(BuildConfig\.NOVA_NEXT_URL\)/);
assert.match(activity,/setAllowFileAccess\(false\)/);
assert.match(activity,/setAllowContentAccess\(false\)/);
assert.match(activity,/MIXED_CONTENT_NEVER_ALLOW/);
assert.match(activity,/isAllowedTopLevelUrl/);
assert.doesNotMatch(activity,/addJavascriptInterface/);
assert.doesNotMatch(activity,/com\.buysloans\.nova(?!next)/);
console.log('android-wrapper-contract: ok');
