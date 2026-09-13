import test from 'node:test';
import assert from 'node:assert/strict';
import { normaliseKnowledgeContext } from '../src/research-metadata.mjs';

test('normalises grounded knowledge context without inventing evidence', () => {
  const result = normaliseKnowledgeContext({ used:true, count:2, semantic:true, degraded:false, items:[
    { id:'a', title:'Policy A', category:'policy', source_label:'Morley', trust_level:'verified', confidence:.91 },
    { id:'b', title:'Guide B', category:'guide', source:'Internal', trust:'trusted', score:.72 }
  ]});
  assert.equal(result.used, true);
  assert.equal(result.count, 2);
  assert.equal(result.semantic, true);
  assert.equal(result.degraded, false);
  assert.equal(result.items.length, 2);
  assert.equal(result.items[0].sourceLabel, 'Morley');
  assert.equal(Object.isFrozen(result), true);
});

test('absent or malformed metadata becomes an explicit empty context', () => {
  assert.deepEqual(normaliseKnowledgeContext(null), { used:false, count:0, semantic:false, degraded:false, items:[] });
  const result = normaliseKnowledgeContext({ used:true, count:'bad', items:'bad', degraded:true });
  assert.equal(result.used, false);
  assert.equal(result.count, 0);
  assert.equal(result.degraded, true);
  assert.deepEqual(result.items, []);
});
