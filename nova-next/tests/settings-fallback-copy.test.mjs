import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const appUrl = new URL('../app.js', import.meta.url);

test('settings fallback copy does not claim connected settings are unimplemented', async () => {
  const source = await readFile(appUrl, 'utf8');

  assert.doesNotMatch(source, /Account settings are not connected in Nova Next yet\./);
  assert.doesNotMatch(source, /Appearance settings are not connected in Nova Next yet\./);
  assert.doesNotMatch(source, /Notification settings are not connected in Nova Next yet\./);
  assert.match(source, /Settings control is temporarily unavailable\. Reopen Settings and try again\./);
});
