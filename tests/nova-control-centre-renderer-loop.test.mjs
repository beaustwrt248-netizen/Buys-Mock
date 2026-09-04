import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const src=fs.readFileSync('admin/nova-control-centre.js','utf8');

test('Nova legacy branding updates are idempotent',()=>{
  assert.match(src,/if\(next!==current\)el\.textContent=next/);
  assert.match(src,/if\(e\.textContent!==next\)e\.textContent=next/);
});

test('Nova mutation observation is scoped and debounced',()=>{
  assert.match(src,/const root=\$\('aiWorkspace'\)/);
  assert.match(src,/requestAnimationFrame/);
  assert.match(src,/renameScheduled/);
  assert.doesNotMatch(src,/observe\(document\.documentElement/);
});
