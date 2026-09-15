import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const workflow = readFileSync('.github/workflows/auto-ota-release.yml', 'utf8');

test('Morley OTA refuses a release tag bound to a different source commit', () => {
  assert.match(workflow, /gh api [^\n]*commits\/\$TAG/);
  assert.match(workflow, /TAG_SHA/);
  assert.match(workflow, /HEAD_SHA/);
  assert.match(workflow, /Refusing[^\n]*tag[^\n]*source|tag[^\n]*does not match[^\n]*source/i);
});

test('Morley OTA verifies the release tag after immutable release creation or reuse', () => {
  const tagChecks = workflow.match(/commits\/\$TAG/g) ?? [];
  assert.ok(tagChecks.length >= 2, 'expected tag provenance checks before and after release publication');
});
