import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const workflow = () => readFile(new URL('../.github/workflows/ota-version-policy.yml', import.meta.url), 'utf8');

test('Morley Android source changes must mint a fresh identity from the PR base', async () => {
  const code = await workflow();
  assert.match(code, /BASE_SHA: \$\{\{ github\.event\.pull_request\.base\.sha \}\}/);
  assert.match(code, /git', 'show', f'\{base_sha\}:android\/app\/build\.gradle'/);
  assert.match(code, /source_code != base_code \+ 1/);
  assert.match(code, /source_semver <= base_semver/);
  assert.match(code, /Do not reuse an already-built version for dependency or runtime changes\./);
});

test('fresh source identity must also remain exactly one release ahead of live OTA', async () => {
  const code = await workflow();
  assert.match(code, /source_code != published_code \+ 1/);
  assert.match(code, /Publish the already-prepared predecessor before merging another Android change\./);
  assert.match(code, /source_semver <= published_semver/);
});
