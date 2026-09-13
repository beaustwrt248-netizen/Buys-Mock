import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const sourceUrl = new URL('../supabase/functions/nova-knowledge/index.ts', import.meta.url);
const source = () => readFile(sourceUrl, 'utf8');

test('expanded Nova knowledge endpoint preserves admin-only authorization and no-delete boundary', async () => {
  const text = await source();
  assert.match(text, /p\.role\s*!==\s*['"]admin['"]/);
  assert.match(text, /Permanent knowledge deletion is disabled through Nova/);
  assert.doesNotMatch(text, /from\(['"]nova_knowledge_items['"]\)\.delete\(/);
});

test('expanded Nova knowledge endpoint wires deterministic chunks, semantic fallback, reindex and bounded embedding', async () => {
  const text = await source();
  assert.match(text, /knowledge_core\.mjs/);
  assert.match(text, /nova_search_knowledge_chunks/);
  assert.match(text, /action\s*===\s*['"]reindex['"]/);
  assert.match(text, /action\s*===\s*['"]embed_pending['"]/);
  assert.match(text, /gte-small/);
  assert.match(text, /bounded\(body\.limit,\s*8,\s*20\)/);
  assert.match(text, /textSearch\(['"]search_vector['"]/);
});
