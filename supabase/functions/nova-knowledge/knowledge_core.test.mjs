import test from 'node:test';
import assert from 'node:assert/strict';

import {
  chunkKnowledgeDocument,
  fuseKnowledgeScores,
  normalizeKnowledgeDocument,
  sanitizeMetadata,
  sha256Hex,
} from './knowledge_core.mjs';

test('sanitizeMetadata removes secrets and sensitive device identifiers recursively', () => {
  const input = {
    safe: 'keep',
    api_key: 'remove',
    nested: {
      authorization: 'remove',
      model: 'Pixel',
      identifiers: [{ imei: '123456789012345', note: 'ok' }, { serial_number: 'ABC123' }],
    },
  };

  assert.deepEqual(sanitizeMetadata(input), {
    safe: 'keep',
    nested: {
      model: 'Pixel',
      identifiers: [{ note: 'ok' }, {}],
    },
  });
});

test('normalizeKnowledgeDocument is deterministic and strips unsafe metadata', async () => {
  const input = {
    category: ' Catalogue ',
    title: '  Pixel 10 Pro  ',
    content: 'Line one.\r\n\r\nLine two.  ',
    source_type: 'import',
    source_label: 'Manufacturer AU',
    trust_level: 'verified',
    metadata: { market: 'AU', access_token: 'remove-me' },
  };

  const first = await normalizeKnowledgeDocument(input);
  const second = await normalizeKnowledgeDocument(input);

  assert.equal(first.category, 'catalogue');
  assert.equal(first.title, 'Pixel 10 Pro');
  assert.equal(first.content, 'Line one.\n\nLine two.');
  assert.deepEqual(first.metadata, { market: 'AU' });
  assert.equal(first.content_hash, second.content_hash);
  assert.equal(first.source_key, second.source_key);
});

test('chunkKnowledgeDocument produces stable bounded chunks with exact overlap', async () => {
  const content = Array.from({ length: 40 }, (_, i) => `Paragraph ${i + 1}: ${'device evidence '.repeat(8)}`).join('\n\n');
  const doc = await normalizeKnowledgeDocument({ title: 'Long device evidence', content, source_type: 'import' });
  const first = await chunkKnowledgeDocument(doc, { maxChars: 520, overlapChars: 80 });
  const second = await chunkKnowledgeDocument(doc, { maxChars: 520, overlapChars: 80 });

  assert.ok(first.length > 2);
  assert.deepEqual(first, second);
  assert.ok(first.every((chunk) => chunk.content.length <= 520));
  assert.ok(first.every((chunk, index) => chunk.chunk_index === index));
  assert.ok(first.every((chunk) => /^[a-f0-9]{64}$/.test(chunk.content_hash)));
  for (let i = 1; i < first.length; i += 1) {
    assert.equal(first[i - 1].content.slice(-80), first[i].content.slice(0, 80));
  }
});

test('sha256Hex changes when content changes', async () => {
  assert.notEqual(await sha256Hex('alpha'), await sha256Hex('beta'));
  assert.equal(await sha256Hex('alpha'), await sha256Hex('alpha'));
});

test('hybrid ranking prefers strong verified fresh evidence over stale similarity-only evidence', () => {
  const now = Date.parse('2026-09-13T00:00:00Z');
  const ranked = fuseKnowledgeScores([
    {
      id: 'verified', lexical_score: 0.92, semantic_score: 0.80, trust_level: 'verified',
      confidence: 0.98, observed_at: '2026-09-12T12:00:00Z', stale_after: '2026-10-13T00:00:00Z',
    },
    {
      id: 'stale', lexical_score: 0.18, semantic_score: 0.99, trust_level: 'reference',
      confidence: 0.55, observed_at: '2025-01-01T00:00:00Z', stale_after: '2025-02-01T00:00:00Z',
    },
  ], { now });

  assert.equal(ranked[0].id, 'verified');
  assert.ok(ranked[0].hybrid_score > ranked[1].hybrid_score);
});

test('ranking degrades to lexical evidence when semantic scores are unavailable', () => {
  const ranked = fuseKnowledgeScores([
    { id: 'weak', lexical_score: 0.20, semantic_score: null, trust_level: 'verified', confidence: 1 },
    { id: 'strong', lexical_score: 0.95, semantic_score: null, trust_level: 'reference', confidence: 0.7 },
  ]);

  assert.equal(ranked[0].id, 'strong');
  assert.equal(ranked[0].semantic_score, null);
});
