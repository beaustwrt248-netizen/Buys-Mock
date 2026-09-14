import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

async function read(relativePath) {
  return readFile(new URL(relativePath, root), 'utf8');
}

test('Nova Next login fields expose meaningful accessible names', async () => {
  const html = await read('index.html');

  assert.match(
    html,
    /<input\s+name="identity"[^>]*aria-label="Email or Username"[^>]*>/,
    'identity input must expose the intended accessible name',
  );
  assert.match(
    html,
    /<input\s+name="password"[^>]*aria-label="Password"[^>]*>/,
    'password input must expose the intended accessible name',
  );
});

test('Nova Next loading state is announced as a polite status', async () => {
  const html = await read('index.html');

  assert.match(
    html,
    /<div\s+class="entry-footer"[^>]*role="status"[^>]*aria-live="polite"[^>]*>/,
    'loading copy must be exposed as a polite status region',
  );
});
