import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source = fs.readFileSync('supabase/functions/nova-orchestrator/index.ts', 'utf8');

test('Nova orchestrator bounds provider and fusion latency', () => {
  assert.match(source, /timeoutMs=18000/);
  assert.match(source, /max_tokens:1400/);
  assert.match(source, /OPENROUTER_TIMEOUT/);
  assert.match(source, /fusion judge/);
});

test('Nova orchestrator preserves guarded authority', () => {
  assert.match(source, /Protected actions remain human-gated/);
  assert.match(source, /profile\.role!=='admin'/);
  assert.match(source, /OPENROUTER_API_KEY/);
});
