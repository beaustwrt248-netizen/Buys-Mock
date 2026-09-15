import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const activity = fs.readFileSync(path.join(root, 'android/app/src/main/java/com/buysloans/novanext/MainActivity.java'), 'utf8');

assert.match(activity, /Log\.w\(TAG,\s*detail\)/,
  'manual update failures must retain detailed diagnostics in the Android log');
assert.doesNotMatch(activity, /Toast\.makeText\(MainActivity\.this,\s*detail\s*,/,
  'manual update failures must not expose raw internal updater diagnostics to users');
assert.match(activity, /Nova Next couldn[^"\n]*check for updates\. Try again shortly\./,
  'manual update failures must show a stable actionable user-facing message');

console.log('android-manual-update-error-boundary-contract: ok');
