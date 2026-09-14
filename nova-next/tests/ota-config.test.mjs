import assert from 'node:assert/strict';
import { NOVA_OTA_CONFIG, validateNovaRelease } from '../src/ota-config.mjs';

const release = {
  appId: 'nova-next',
  channel: 'stable',
  packageName: 'com.buysloans.novanext',
  versionCode: 12,
  versionName: '1.2.0',
  sha256: 'a'.repeat(64),
  downloadUrl: 'https://buyshub.me/downloads/nova-next/stable/nova-next-1.2.0.apk',
  releaseNotes: 'Reliability and layout fixes.'
};

assert.equal(NOVA_OTA_CONFIG.appId, 'nova-next');
assert.equal(NOVA_OTA_CONFIG.packageName, 'com.buysloans.novanext');
assert.equal(NOVA_OTA_CONFIG.channel, 'stable');
assert.equal(validateNovaRelease(release, { currentVersionCode: 11 }).versionCode, 12);

for (const [field, value] of [
  ['appId', 'morley'],
  ['channel', 'beta'],
  ['packageName', 'com.example.other'],
  ['sha256', 'bad-hash'],
  ['downloadUrl', 'http://buyshub.me/nova.apk']
]) {
  assert.throws(
    () => validateNovaRelease({ ...release, [field]: value }, { currentVersionCode: 11 }),
    /NOVA_OTA_/
  );
}

assert.throws(() => validateNovaRelease({ ...release, versionCode: 11 }, { currentVersionCode: 11 }), /NOVA_OTA_NOT_NEWER/);
assert.throws(() => validateNovaRelease({ ...release, versionCode: 10 }, { currentVersionCode: 11 }), /NOVA_OTA_NOT_NEWER/);
assert.throws(() => validateNovaRelease(null, { currentVersionCode: 11 }), /NOVA_OTA_INVALID_RELEASE/);

console.log('ota-config: ok');
