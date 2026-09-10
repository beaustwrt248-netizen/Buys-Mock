import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const gradle = await readFile(new URL('../android/adminapp/build.gradle', import.meta.url), 'utf8');
const manifest = await readFile(new URL('../android/adminapp/src/main/AndroidManifest.xml', import.meta.url), 'utf8');

test('production Admin keeps canonical package and advances release identity', () => {
  assert.match(gradle, /applicationId 'com\.buysloans\.admin'/);
  assert.match(gradle, /versionCode 28/);
  assert.match(gradle, /versionName '0\.1\.27'/);
});

test('debug builds cannot collide with installed production Admin', () => {
  assert.match(gradle, /debug\s*\{[\s\S]*applicationIdSuffix '\.debug'/);
  assert.match(gradle, /Morley Admin Debug/);
});

test('recovery build installs alongside a conflicting legacy Admin package', () => {
  assert.match(gradle, /recovery\s*\{[\s\S]*applicationIdSuffix '\.recovery'/);
  assert.match(gradle, /Morley Admin Recovery/);
  assert.match(gradle, /preRecoveryBuild/);
  assert.match(manifest, /android:label="\$\{appLabel\}"/);
  assert.match(manifest, /android:authorities="\$\{applicationId\}\.updates"/);
});
