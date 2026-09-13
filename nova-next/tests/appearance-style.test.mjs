import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const css = fs.readFileSync(new URL('../styles.css', import.meta.url), 'utf8');

await test('local appearance preference produces a real light theme override', () => {
  assert.match(css, /data-nova-appearance=["']light["']/);
  assert.match(css, /--bg:/);
  assert.match(css, /--text:/);
  assert.match(css, /color-scheme:\s*light/);
});

console.log('appearance-style: ok');
