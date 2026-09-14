import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../../', import.meta.url);

async function read(path) {
  return readFile(new URL(path, root), 'utf8');
}

test('login copy reflects connected Nova Next authentication state', async () => {
  const html = await read('nova-next/index.html');
  assert.doesNotMatch(html, /Bootstrap preview: authentication adapter not connected yet\./);
  assert.match(html, /Secure Admin authentication/i);
});

test('canonical Nova Next validation docs no longer describe the app as bootstrap-only', async () => {
  const visual = await read('docs/nova-next/VISUAL_VALIDATION.md');
  const live = await read('docs/nova-next/LIVE_ADAPTER_VALIDATION.md');

  assert.doesNotMatch(visual, /bootstrap shell/i);
  assert.match(visual, /completed Nova Next/i);
  assert.doesNotMatch(live, /PR #1784 remains high-risk/i);
  assert.match(live, /merged implementation/i);
});
