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
assert.ok(Number.isInteger(metadata.versionCode) && metadata.versionCode >= 0);
assert.equal(typeof metadata.versionName, 'string');
assert.ok(metadata.versionName.length > 0);
assert.equal(typeof metadata.downloadUrl, 'string');
assert.equal(typeof metadata.sha256, 'string');

const sourceCodeMatch = gradle.match(/versionCode\s+(\d+)/);
const sourceNameMatch = gradle.match(/versionName\s+['"]([^'"]+)['"]/);
assert.ok(sourceCodeMatch, 'Nova Next source versionCode is required');
assert.ok(sourceNameMatch, 'Nova Next source versionName is required');
const sourceVersionCode = Number(sourceCodeMatch[1]);
const sourceVersionName = sourceNameMatch[1];

assert.match(gradle, /NOVA_OTA_APP_ID/);

// The checked-in metadata is either the bootstrap sentinel (before the first
// protected publication) or a real immutable release. It must never describe
// a version newer than the source that is being reviewed.
assert.ok(metadata.versionCode <= sourceVersionCode);
if (metadata.versionCode === 0 && metadata.versionName === '0.0.0') {
  assert.equal(metadata.downloadUrl, '');
  assert.equal(metadata.sha256, '');
} else {
  assert.ok(metadata.versionCode > 0);
  assert.match(metadata.versionName, /^[0-9A-Za-z][0-9A-Za-z._-]*$/);
  assert.match(metadata.downloadUrl, /^https:\/\/github\.com\/beaustwrt248-netizen\/Buys-Mock\/releases\/download\/nova-next-v[0-9A-Za-z._-]+\/Nova-Next-[0-9A-Za-z._-]+\.apk$/);
  assert.match(metadata.sha256, /^[0-9a-f]{64}$/);
  assert.ok(
    metadata.versionCode < sourceVersionCode ||
      (metadata.versionCode === sourceVersionCode && metadata.versionName === sourceVersionName),
    'published OTA metadata must not outrun or misidentify the reviewed source version'
  );
}

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
