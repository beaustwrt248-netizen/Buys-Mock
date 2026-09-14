import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';

const sourceUrl = new URL('../supabase/functions/nova-knowledge-maintenance/index.ts', import.meta.url);

test('transient source failures are recorded without skipping embedding work', async () => {
  const source = await readFile(sourceUrl, 'utf8');
  assert.match(source, /classifySourceFailure/);
  assert.match(source, /classification\.retryable/);
  assert.match(source, /sourceFailures\?\.push/);
  assert.match(source, /await ingestInternal\(ingestLimit, stats, sourceFailures\);[\s\S]*await embedPending\(embeddingLimit, stats\);/);
  assert.match(source, /summarizeSourceFailures\(sourceFailures\)/);
  assert.match(source, /partial \? "partial" : "completed"/);
});

test('hard source failures still escape the source boundary', async () => {
  const source = await readFile(sourceUrl, 'utf8');
  assert.match(source, /if \(options\.transientTolerant && classification\.retryable\)[\s\S]*return \[\];[\s\S]*throw error;/);
});
