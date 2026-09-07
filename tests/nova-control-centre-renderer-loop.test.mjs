import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const src=fs.readFileSync('nova/app-core.js','utf8');

test('Nova refresh rendering rejects stale asynchronous results',()=>{
  assert.match(src,/let refreshVersion=0/);
  assert.match(src,/const requestVersion=\+\+refreshVersion/);
  assert.ok((src.match(/if\(requestVersion!==refreshVersion\)return/g)||[]).length>=2);
});

test('Nova renderer avoids unbounded DOM mutation observation',()=>{
  assert.doesNotMatch(src,/MutationObserver/);
  assert.doesNotMatch(src,/setInterval\(/);
  assert.match(src,/\$\('refreshBtn'\)\?\.addEventListener\('click',refresh\)/);
});
