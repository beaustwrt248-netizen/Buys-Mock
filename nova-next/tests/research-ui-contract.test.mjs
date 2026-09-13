import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
const source = await readFile(new URL('../src/research-ui.mjs', import.meta.url), 'utf8');

test('research workspace exposes the four approved presets', () => {
  for (const preset of ['compare','investigate','summarise','scenario']) assert.match(source, new RegExp(preset));
});

test('research handoff always requires user review before send', () => {
  assert.match(source, /review before sending/i);
  assert.doesNotMatch(source, /sendChat\(|\.submit\(/);
});

test('research evidence distinguishes grounded, semantic and degraded states', () => {
  assert.match(source, /Evidence used/);
  assert.match(source, /Semantic retrieval/);
  assert.match(source, /degraded/i);
});
