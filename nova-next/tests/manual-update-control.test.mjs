import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const index = fs.readFileSync(path.join(root, 'index.html'), 'utf8');
const settingsUi = fs.readFileSync(path.join(root, 'src', 'settings-ui.mjs'), 'utf8');
const activity = fs.readFileSync(path.join(root, 'android', 'app', 'src', 'main', 'java', 'com', 'buysloans', 'novanext', 'MainActivity.java'), 'utf8');

assert.match(index, /data-action="check-updates"[\s\S]*?<strong>Check for Updates<\/strong>/, 'Settings must expose a manual update control');
assert.match(settingsUi, /bindAction\('check-updates'/, 'the Settings update control must be wired');
assert.match(settingsUi, /novanext:\/\/check-updates/, 'manual checks must invoke the narrow native OTA route');
assert.match(activity, /UPDATE_SCHEME = "novanext"/);
assert.match(activity, /UPDATE_HOST = "check-updates"/);
assert.match(activity, /checkForUpdatesNow\(\)/, 'the native route must invoke the existing verified UpdateManager');
assert.doesNotMatch(activity, /addJavascriptInterface/, 'manual OTA checks must not introduce a broad JavaScript bridge');

console.log('manual-update-control: ok');
