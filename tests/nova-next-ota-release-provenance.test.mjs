import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const workflow = readFileSync('.github/workflows/nova-next-apk-build.yml', 'utf8');

test('Nova Next release verifies an existing tag is bound to the exact release source', () => {
  assert.match(workflow, /gh api [^\n]*commits\/\$NOVA_NEXT_RELEASE_TAG/);
  assert.match(workflow, /NOVA_NEXT_TAG_SHA/);
  assert.match(workflow, /GITHUB_SHA/);
  assert.match(workflow, /Refusing[^\n]*tag[^\n]*source|tag[^\n]*does not match[^\n]*source/i);
});

test('Nova Next release re-verifies tag provenance after immutable release publication', () => {
  const tagChecks = workflow.match(/commits\/\$NOVA_NEXT_RELEASE_TAG/g) ?? [];
  assert.ok(tagChecks.length >= 2, 'expected Nova Next tag provenance checks before and after release publication');
});
