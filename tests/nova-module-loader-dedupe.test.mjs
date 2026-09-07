import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const support=fs.readFileSync('nova/live-support.js','utf8');
const core=fs.readFileSync('nova/app-core.js','utf8');

test('Nova intelligence loader publishes the shared module marker used by app-core',()=>{
  assert.match(support,/const moduleSrc=String\(src\)\.split\('\?'\)\[0\]/);
  assert.match(support,/script\.dataset\.novaModule=moduleSrc/);
  assert.match(core,/script\[data-nova-module=/);
});

test('Nova intelligence loader refuses duplicate modules across both loader identities',()=>{
  assert.match(support,/document\.getElementById\(id\)\|\|document\.querySelector\(`script\[data-nova-module=/);
  assert.match(support,/guardian-live-analysis\.js\?v=2/);
  assert.match(support,/control-centre\.js\?v=1/);
  assert.match(core,/\['permissions\.js','guardian-live-analysis\.js','control-centre\.js'\]\.forEach\(loadModule\)/);
});
