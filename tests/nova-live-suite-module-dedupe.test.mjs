import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const support=fs.readFileSync('nova/live-support.js','utf8');
const control=fs.readFileSync('nova/control-centre.js','utf8');
const core=fs.readFileSync('nova/app-core.js','utf8');

test('all Nova dynamic loaders share a stable bare module identity',()=>{
  assert.match(support,/const moduleSrc=String\(src\)\.split\('\?'\)\[0\]/);
  assert.match(support,/script\.dataset\.novaModule=moduleSrc/);
  assert.match(control,/s\.dataset\.novaModule=src/);
  assert.match(core,/script\.dataset\.novaModule=src/);
});

test('support loader detects modules created by any Nova loader',()=>{
  assert.match(support,/script\.dataset\.novaModule===moduleSrc\|\|script\.dataset\.novaFeature===moduleSrc/);
  assert.match(support,/new URL\(script\.src,location\.href\)\.pathname\.endsWith\('\/'\+moduleSrc\)/);
});

test('control centre does not reload live-suite modules already inserted elsewhere',()=>{
  assert.match(control,/function modulePresent\(src\)/);
  assert.match(control,/script\.dataset\.novaModule===src\|\|script\.dataset\.novaFeature===src/);
  assert.match(control,/new URL\(script\.src,location\.href\)\.pathname\.endsWith\('\/'\+src\)/);
  for(const name of ['live-memory.js','live-intelligence.js','pr-readiness.js','proactive-alerts.js']) assert.match(control,new RegExp(name.replace('.','\\.')));
});

test('Guardian analysis and Control Centre are single-load modules after authentication',()=>{
  assert.match(support,/guardian-live-analysis\.js\?v=2/);
  assert.match(support,/control-centre\.js\?v=1/);
  assert.match(core,/\['permissions\.js','guardian-live-analysis\.js','control-centre\.js'\]\.forEach\(loadModule\)/);
});
