import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..', 'android', 'app');
const updateManager = fs.readFileSync(path.join(root, 'src', 'main', 'java', 'com', 'buysloans', 'novanext', 'UpdateManager.java'), 'utf8');
const activity = fs.readFileSync(path.join(root, 'src', 'main', 'java', 'com', 'buysloans', 'novanext', 'MainActivity.java'), 'utf8');

assert.match(updateManager, /BuildConfig\.NOVA_OTA_APP_ID\.equals\(info\.appId\)/);
assert.match(updateManager, /BuildConfig\.NOVA_OTA_CHANNEL\.equals\(info\.channel\)/);
assert.match(updateManager, /activity\.getPackageName\(\)\.equals\(info\.packageName\)/);
assert.match(updateManager, /Downloaded APK failed SHA-256 verification/);
assert.match(updateManager, /Downloaded APK signing identity mismatch/);
assert.match(updateManager, /Downloaded APK version code mismatch/);
assert.match(updateManager, /getPackageName\(\) \+ "\\.files"/);
assert.match(activity, /new UpdateManager\(/);
assert.match(activity, /checkForUpdates\(\)/);
assert.match(activity, /resumePendingInstall\(this\)/);
assert.doesNotMatch(activity, /addJavascriptInterface/);

console.log('android-ota-contract: ok');
