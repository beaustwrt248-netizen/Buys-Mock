import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
const source = await readFile(new URL('../src/voice-ui.mjs', import.meta.url), 'utf8');

test('voice UI uses explicit mic control and review-before-send copy', () => {
  assert.match(source, /aria-pressed/);
  assert.match(source, /Listening…/);
  assert.match(source, /review before sending/i);
  assert.match(source, /Voice input is not supported/i);
});

test('voice UI only writes transcript into composer and never sends chat', () => {
  assert.match(source, /composer\.value/);
  assert.doesNotMatch(source, /sendChat|submit\(|requestSubmit/);
});

test('route/signout cleanup can stop recognition', () => {
  assert.match(source, /stop/);
  assert.match(source, /destroy/);
});
