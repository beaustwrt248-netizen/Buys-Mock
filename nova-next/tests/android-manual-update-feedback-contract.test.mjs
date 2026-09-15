import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const mainActivity = fs.readFileSync(path.join(root, 'android/app/src/main/java/com/buysloans/novanext/MainActivity.java'), 'utf8');

assert.match(mainActivity, /private boolean manualUpdateCheckInProgress;/,
  'native shell must track whether an update check was explicitly requested by the user');
assert.match(mainActivity, /manualUpdateCheckInProgress\s*=\s*true;[\s\S]*updateManager\.checkForUpdates\(\);/,
  'manual update action must mark the request before starting the check');
assert.match(mainActivity, /onUpToDate\(\)[\s\S]*manualUpdateCheckInProgress[\s\S]*Nova Next is up to date/,
  'manual checks must give the user an explicit up-to-date result');
assert.match(mainActivity, /onError\(String message\)[\s\S]*manualUpdateCheckInProgress[\s\S]*Toast\.makeText/,
  'manual checks must surface failures instead of only logging them');

console.log('android-manual-update-feedback-contract: ok');
