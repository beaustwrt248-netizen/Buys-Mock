import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../../', import.meta.url);

async function read(path) {
  return readFile(new URL(path, root), 'utf8');
}

test('canonical Nova Next status records audited completion state', async () => {
  const status = await read('docs/nova-next/STATUS.md');
  assert.match(status, /GO FOR PROMOTION REVIEW/);
  assert.match(status, /2026-09-13-nova-next-promotion-readiness\.md/);
  assert.doesNotMatch(status, /Bootstrap Status/);
});

test('canonical feature parity map no longer labels verified capabilities as registry-only', async () => {
  const parity = await read('docs/nova-next/FEATURE_PARITY.md');
  assert.match(parity, /ready-with-boundary/);
  assert.match(parity, /Conversation \/ AI chat[^\n]*ready/i);
  assert.match(parity, /Voice assistant[^\n]*ready-with-boundary/i);
  assert.match(parity, /Live web research[^\n]*ready-with-boundary/i);
  assert.doesNotMatch(parity, /Registry \+ UI shell/);
});
