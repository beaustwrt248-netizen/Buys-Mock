import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const workflow = fs.readFileSync('.github/workflows/auto-ota-release.yml', 'utf8');

test('missing OTA tag is treated as absent rather than a conflicting commit', () => {
  assert.match(
    workflow,
    /gh api[^\n]*commits\/\$TAG[^\n]*--jq ['"]?\.sha['"]?[^\n]*2>\/dev\/null[^\n]*\|\| true/,
    'workflow should probe an existing tag without failing when the tag is absent',
  );
  assert.match(
    workflow,
    /TAG_SHA=.*\n\s*if \[ -n "\$TAG_SHA" \]/,
    'only a real resolved tag SHA should enter the provenance-conflict check',
  );
  assert.doesNotMatch(
    workflow,
    /TAG_SHA="\$\(gh api "repos\/\$GITHUB_REPOSITORY\/commits\/\$TAG" --jq \.sha 2>\/dev\/null \|\| true\)"/,
    'GitHub CLI error JSON must not be captured as TAG_SHA',
  );
});
