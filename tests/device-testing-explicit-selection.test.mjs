import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const src=()=>readFile(new URL('../admin/device-intelligence-centre.js',import.meta.url),'utf8');

test('default detail preview does not become a staff testing selection',async()=>{
  const code=await src();
  assert.match(code,/selectedExplicit=false/);
  assert.match(code,/publishSelection\(selectedExplicit\?d:null\)/);
  assert.match(code,/selected=rows\[0\]\.id;selectedExplicit=false/);
});

test('direct Device Intelligence row choice marks testing selection explicit',async()=>{
  const code=await src();
  assert.match(code,/b\.onclick=\(\)=>\{selected=d\.id;selectedExplicit=true;renderList\(\)\}/);
  assert.match(code,/selected=null;selectedExplicit=false/);
});
