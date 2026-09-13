import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

test('Morley workspace loads AI assessment core before assessment client', async () => {
  const src = await readFile(new URL('../index.html', import.meta.url), 'utf8');
  const core = './morley-ai-assessment-core.js';
  const client = './morley-ai-assessment-client.js';
  const coreIndex = src.indexOf(core);
  const clientIndex = src.indexOf(client);

  assert.notEqual(coreIndex, -1, 'index.html must load the Morley AI assessment core');
  assert.notEqual(clientIndex, -1, 'index.html must load the Morley AI assessment client');
  assert.ok(coreIndex < clientIndex, 'assessment core must load before assessment client');
});
