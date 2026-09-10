import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const src=()=>readFile(new URL('../nova/app.js',import.meta.url),'utf8');

test('Nova auth bootstrap bounds external loader waits',async()=>{
  const code=await src();
  assert.match(code,/const SCRIPT_TIMEOUT_MS=12000/);
  assert.match(code,/Morley auth client timed out\. Refresh and try again\./);
  assert.match(code,/Security check timed out\. Refresh and try again\./);
  assert.match(code,/clearTimeout\(timer\)/);
});

test('Nova auth bootstrap failure remains locked and exposes recovery UI',async()=>{
  const code=await src();
  assert.match(code,/function showBootFailure\(error\)/);
  assert.match(code,/Nova sign-in unavailable/);
  assert.match(code,/Access remains locked\./);
  assert.match(code,/id="nova-auth-reload"/);
  assert.match(code,/boot\(\)\.catch\(error=>/);
  assert.doesNotMatch(code,/showBootFailure[\s\S]*unlockNetwork\(\)/);
});
