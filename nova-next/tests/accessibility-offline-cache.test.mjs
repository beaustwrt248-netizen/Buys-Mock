import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const root = new URL('../', import.meta.url);

test('Nova Next precaches the reduced-motion stylesheet for offline use', async () => {
  const serviceWorker = await readFile(new URL('service-worker.js', root), 'utf8');

  assert.match(
    serviceWorker,
    /`\$\{APP_PREFIX\}accessibility\.css`/,
    'service worker CORE cache must include accessibility.css',
  );
});
