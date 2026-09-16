import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const workflow = fs.readFileSync('.github/workflows/auto-ota-release.yml', 'utf8');

test('missing OTA tag is treated as absent rather than a conflicting commit', () => {
  assert.match(
    workflow,
    /if TAG_SHA="\$\(gh api[^\n]*commits\/\$TAG[^\n]*--jq ['"]?\.sha['"]? 2>\/dev\/null\)"; then\n\s*:\n\s*else\n\s*TAG_SHA=""\n\s*fi/,
    'a failed GitHub tag lookup must explicitly discard any API error body captured on stdout',
  );
  assert.match(
    workflow,
    /if \[ -n "\$TAG_SHA" \] && \[ "\$TAG_SHA" != "\$HEAD_SHA" \]/,
    'a successfully resolved existing tag must still enforce immutable provenance',
  );
  assert.doesNotMatch(
    workflow,
    /TAG_SHA="\$\(gh api[^\n]*commits\/\$TAG[^\n]*\|\| true\)"/,
    'do not hide lookup failure inside command substitution because its stdout can be mistaken for a tag SHA',
  );
});
