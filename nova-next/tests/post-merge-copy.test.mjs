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

test('copy cleanup preserves the merged Nova shell structure', async () => {
  const html = await read('nova-next/index.html');

  assert.match(html, /data-route="help"/);
  assert.match(html, /id="novaNextHelpRoot"/);
  assert.match(html, /id="bottomNav"/);
  assert.match(html, /id="allSetView"/);
  assert.match(html, /data-action="finish-intro"/);
});

test('canonical Nova Next live validation no longer describes the auth connection as unmerged', async () => {
  const live = await read('docs/nova-next/LIVE_ADAPTER_VALIDATION.md');

  assert.doesNotMatch(live, /PR #1784 remains high-risk/i);
  assert.match(live, /merged implementation/i);
  assert.match(live, /production promotion/i);
});
