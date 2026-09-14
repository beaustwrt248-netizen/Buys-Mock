import assert from 'node:assert/strict';
import { filterToolRecords } from '../src/completion-ui.mjs';

const tools = [
  { category: 'productivity', text: 'AI Chat General assistant' },
  { category: 'content', text: 'Translate Multiple languages' },
  { category: 'analysis', text: 'Data Analysis Analyse and visualise data' }
];

assert.deepEqual(filterToolRecords(tools, { category: 'all', query: '' }), tools);
assert.deepEqual(filterToolRecords(tools, { category: 'content', query: '' }), [tools[1]]);
assert.deepEqual(filterToolRecords(tools, { category: 'analysis', query: 'visualise' }), [tools[2]]);
assert.deepEqual(filterToolRecords(tools, { category: 'productivity', query: 'translate' }), []);
assert.deepEqual(filterToolRecords(tools, { category: 'ALL', query: 'assistant' }), [tools[0]]);

console.log('tool-filter: ok');
