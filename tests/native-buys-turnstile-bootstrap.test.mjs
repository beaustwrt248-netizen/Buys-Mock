import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const source = await readFile(new URL('../admin/turnstile.html', import.meta.url), 'utf8');

test('native Morley Buys WebView boots Turnstile through AndroidBridge', () => {
  assert.match(source, /const nativeAndroid=!!window\.AndroidBridge;/);
  assert.match(source, /if\(nativeAndroid\|\|params\.has\('load'\)\|\|params\.has\('retry'\)\|\|version==='4'\|\|version==='5'\)loadApi\(\);/);
});

test('legacy Admin embed remains deferred unless explicitly started', () => {
  assert.match(source, /status\.textContent='Enter your email and password first\.';/);
  assert.match(source, /if\(payload\.type==='start'\)loadApi\(\);/);
});
