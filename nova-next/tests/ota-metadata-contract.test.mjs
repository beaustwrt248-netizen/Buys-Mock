import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const root = path.resolve(here, '..');
const metadata = JSON.parse(fs.readFileSync(path.join(root, 'nova-update.json'), 'utf8'));
const gradle = fs.readFileSync(path.join(root, 'android', 'app', 'build.gradle'), 'utf8');
const workflow = fs.readFileSync(path.join(root, '..', '.github', 'workflows', 'nova-next-apk-build.yml'), 'utf8');

assert.equal(metadata.appId, 'nova-next');
assert.equal(metadata.channel, 'stable');
assert.equal(metadata.packageName, 'com.buysloans.novanext');
assert.equal(metadata.versionCode, 0);
assert.equal(metadata.versionName, '0.0.0');
assert.equal(metadata.downloadUrl, '');
assert.equal(metadata.sha256, '');
assert.match(gradle, /versionCode\s+1/);
assert.match(gradle, /versionName\s+['"]0\.1\.0['"]/);
assert.match(gradle, /NOVA_OTA_APP_ID/);

// Publishing a signed OTA release is a protected action. A merge/push to main may
// build and validate Nova Next, but it must never publish an OTA release without
// an explicit manual workflow dispatch.
assert.match(
  workflow,
  /publish-nova-next-ota:\s*\n\s*if:\s*github\.ref == 'refs\/heads\/main' && github\.event_name == 'workflow_dispatch'/
);
assert.doesNotMatch(
  workflow,
  /publish-nova-next-ota:[\s\S]*?if:[^\n]*needs\.build-nova-next-apk\.outputs\.app_changed/
);

console.log('ota-metadata-contract: ok');
