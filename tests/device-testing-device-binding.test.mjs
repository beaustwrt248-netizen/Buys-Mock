import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const src=()=>readFile(new URL('../admin/device-testing-centre.js',import.meta.url),'utf8');

test('device testing requires a selected device before recording results',async()=>{
  const code=await src();
  assert.match(code,/if\(!activeDevice\.id\)return/);
  assert.match(code,/b\.disabled=!activeDevice\.id/);
  assert.match(code,/Select a device in Device Intelligence/);
});

test('switching selected device clears the previous unsaved session',async()=>{
  const code=await src();
  assert.match(code,/function bindDevice/);
  assert.match(code,/clearSession\(\);renderRows\(\);renderDevice\(\)/);
  assert.match(code,/closest\?\.\('\.mdi-row'\)/);
  assert.match(code,/automatically clears this unsaved session/);
});

test('testing stays staff verified and unsupported remains distinct',async()=>{
  const code=await src();
  assert.match(code,/automated:false,platformVerified:false/);
  assert.match(code,/unsupported/);
  assert.match(code,/staff-verified only/);
});
