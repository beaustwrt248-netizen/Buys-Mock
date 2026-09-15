import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const workflow = readFileSync('.github/workflows/admin-apk-build.yml', 'utf8');

test('Admin immutable release tag is bound to the exact build source before reuse', () => {
  assert.match(workflow, /gh api [^\n]*commits\/\$ADMIN_RELEASE_TAG/);
  assert.match(workflow, /ADMIN_TAG_SHA/);
  assert.match(workflow, /GITHUB_SHA/);
  assert.match(workflow, /Refusing[^\n]*Admin[^\n]*tag[^\n]*source|Admin[^\n]*tag[^\n]*does not match[^\n]*source/i);
});

test('Admin immutable release provenance is re-verified before OTA metadata publication', () => {
  const tagChecks = workflow.match(/commits\/\$ADMIN_RELEASE_TAG/g) ?? [];
  assert.ok(tagChecks.length >= 2, 'expected Admin tag provenance checks before release reuse/create and before OTA metadata publication');
});
