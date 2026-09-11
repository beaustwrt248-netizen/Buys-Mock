import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const source = await readFile(new URL('../admin/turnstile.html', import.meta.url), 'utf8');

test('embedded Turnstile does not duplicate the parent success message', () => {
  assert.match(source, /#status:empty\{display:none;margin:0\}/);
  assert.match(source, /callback:function\(token\)\{status\.textContent='';send\('onToken',token\);\}/);
  assert.doesNotMatch(source, /callback:function\(token\)\{status\.textContent='Security check complete\.';/);
});
