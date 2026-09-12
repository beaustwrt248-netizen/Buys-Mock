import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const workflow = readFileSync(
  new URL('../.github/workflows/morley-ecosystem-autopilot.yml', import.meta.url),
  'utf8',
);

test('recovery close verifies final issue state before failing on gh close transport errors', () => {
  assert.match(workflow, /if ! gh issue close \"\$ISSUE\"/);
  assert.match(workflow, /--json state --jq '\.state'/);
  assert.match(workflow, /already closed despite gh close returning an error/i);
});
