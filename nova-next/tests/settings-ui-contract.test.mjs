import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const source = await readFile(new URL('../src/settings-ui.mjs', import.meta.url), 'utf8');

test('settings UI owns all formerly staged safe settings actions', () => {
  for (const action of ['settings-account', 'settings-appearance', 'settings-notifications', 'settings-privacy', 'settings-about']) {
    assert.match(source, new RegExp(action));
  }
});

test('settings UI exposes read-only account and local-only notification boundaries', () => {
  assert.match(source, /Admin session/i);
  assert.match(source, /read-only/i);
  assert.match(source, /local preference/i);
  assert.doesNotMatch(source, /change password|update email|role mutation|pricing approval/i);
});

test('settings UI supports system, dark and light appearance', () => {
  assert.match(source, /system/);
  assert.match(source, /dark/);
  assert.match(source, /light/);
  assert.match(source, /data-appearance/);
});
