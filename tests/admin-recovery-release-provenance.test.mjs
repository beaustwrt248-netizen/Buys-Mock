import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import test from 'node:test';

const workflow = readFileSync('.github/workflows/admin-recovery-apk.yml', 'utf8');

test('Admin recovery release is explicitly targeted at the exact tested source', () => {
  assert.match(workflow, /gh release create "\$ADMIN_RECOVERY_TAG"[\s\S]*--target "\$GITHUB_SHA"/);
});

test('Admin recovery release tag provenance is verified after publication', () => {
  assert.match(workflow, /gh api [^\n]*commits\/\$ADMIN_RECOVERY_TAG/);
  assert.match(workflow, /ADMIN_RECOVERY_TAG_SHA/);
  assert.match(workflow, /ADMIN_RECOVERY_TAG_SHA[^\n]*GITHUB_SHA|GITHUB_SHA[^\n]*ADMIN_RECOVERY_TAG_SHA/);
  assert.match(workflow, /recovery[^\n]*tag[^\n]*does not match[^\n]*source/i);
});
