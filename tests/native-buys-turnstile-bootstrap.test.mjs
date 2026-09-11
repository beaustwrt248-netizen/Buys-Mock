import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const source = await readFile(new URL('../admin/turnstile.html', import.meta.url), 'utf8');

test('native Morley Buys direct navigation boots Turnstile without relying on AndroidBridge timing', () => {
  assert.match(source, /if\(params\.get\('defer'\)==='1'\)/);
  assert.match(source, /else\{\s*loadApi\(\);\s*\}/);
  assert.doesNotMatch(source, /const nativeAndroid=!!window\.AndroidBridge;/);
});

test('explicit defer mode remains available for protected Admin-style startup', () => {
  assert.match(source, /status\.textContent='Enter your email and password first\.';/);
  assert.match(source, /if\(payload\.type==='start'\)loadApi\(\);/);
});
