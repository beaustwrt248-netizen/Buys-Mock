import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('Nova Next loads a dedicated reduced-motion accessibility stylesheet', async () => {
  const html = await read('index.html');

  assert.match(
    html,
    /<link\s+rel="stylesheet"\s+href="\.\/accessibility\.css"\s*\/?>/,
    'index.html must load the reduced-motion accessibility stylesheet',
  );
});

test('reduced-motion users do not receive route or decorative animation', async () => {
  const css = await read('accessibility.css');

  assert.match(css, /@media\s*\(prefers-reduced-motion:\s*reduce\)/);
  assert.match(css, /\.page\s*\{[^}]*animation:\s*none\s*!important;[^}]*\}/s);
  assert.match(css, /\.ambient\s*\{[^}]*transition:\s*none\s*!important;[^}]*\}/s);
  assert.match(css, /html:\s*focus-within\s*\{[^}]*scroll-behavior:\s*auto\s*!important;[^}]*\}/s);
});
