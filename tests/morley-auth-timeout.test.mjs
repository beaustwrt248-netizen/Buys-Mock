import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const auth=fs.readFileSync('morley-auth-client.js','utf8');

test('shared Morley auth client bounds network waits and fails closed',()=>{
  assert.match(auth,/const REQUEST_TIMEOUT_MS=12000/);
  assert.match(auth,/new AbortController\(\)/);
  assert.match(auth,/setTimeout\(\(\)=>controller\.abort\(\),REQUEST_TIMEOUT_MS\)/);
  assert.match(auth,/timeout\.code='AUTH_TIMEOUT'/);
  assert.match(auth,/async function validateProfile[\s\S]*catch\(_e\)\{return false\}/);
  assert.match(auth,/async function refreshSession[\s\S]*catch\(_e\)\{return null\}/);
  assert.match(auth,/signInPassword[\s\S]*await request\(base\+'\/auth\/v1\/token\?grant_type=password'/);
});
