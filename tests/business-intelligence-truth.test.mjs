import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const model=()=>readFile(new URL('../admin/business-intelligence-model.js',import.meta.url),'utf8');
const centre=()=>readFile(new URL('../admin/business-intelligence-centre.js',import.meta.url),'utf8');

test('Business Analytics can be available from non-catalogue authoritative evidence',async()=>{
  const code=await model();
  assert.match(code,/metrics\.some\(x=>x\.state==='confirmed'\)/);
  assert.doesNotMatch(code,/state:cat\.state==='confirmed'\?'confirmed':'unavailable'/);
});

test('Business Analytics does not claim unimplemented Nova effectiveness or inferred sales metrics',async()=>{
  const m=await model(),ui=await centre();
  assert.doesNotMatch(ui,/Nova-effectiveness signals/);
  assert.match(ui,/buy-pricing coverage/);
  assert.match(m,/No sales velocity, realised profit, CRM, Nova-effectiveness or inventory-lifecycle metric is inferred/);
});

test('Business Analytics keeps missing individual values explicitly unavailable',async()=>{
  const ui=await centre();
  assert.match(ui,/m\.value==null\?'Unavailable'/);
  assert.match(ui,/No authoritative value loaded/);
});
