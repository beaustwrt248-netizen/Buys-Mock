import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source=fs.readFileSync(new URL('../morley-core.js',import.meta.url),'utf8');
const architecture=fs.readFileSync(new URL('../docs/MORLEY_ECOSYSTEM_ARCHITECTURE.md',import.meta.url),'utf8');

test('ecosystem exposes exactly three user-facing product definitions',()=>{
  assert.match(source,/id:'morley-buys'/);
  assert.match(source,/id:'morley-admin'/);
  assert.match(source,/id:'nova'/);
  assert.match(source,/product:false/);
});

test('Guardian remains a Nova enforcement layer and fails closed',()=>{
  assert.match(source,/parent:'nova'/);
  assert.match(source,/Nova cannot disable, bypass or weaken Guardian\./);
  assert.match(source,/Missing enforcement evidence fails closed\./);
  assert.match(architecture,/Guardian is not a fourth product or competing assistant\./);
});

test('Morley Core owns the shared source-of-truth domains',()=>{
  for(const domain of ['catalogue','pricing','identity','roles','media','audit-events','search','integrations','notifications','realtime-events']){
    assert.ok(source.includes(`'${domain}'`),`missing canonical domain: ${domain}`);
  }
  assert.match(architecture,/Realtime is the default propagation mechanism/);
});

test('destructive and privileged actions remain human gated',()=>{
  assert.match(architecture,/destructive deletes/);
  assert.match(architecture,/user\/role changes/);
  assert.match(architecture,/release\/deployment/);
  assert.match(architecture,/protected pricing writes\/approval/);
});
