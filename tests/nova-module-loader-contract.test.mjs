import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const loader=fs.readFileSync('nova/module-loader.js','utf8');
const support=fs.readFileSync('nova/live-support.js','utf8');
const control=fs.readFileSync('nova/control-centre.js','utf8');

test('Nova has one shared dynamic module identity and injection implementation',()=>{
  assert.match(loader,/function identity\(src\)/);
  assert.match(loader,/function find\(src,id=''/);
  assert.match(loader,/function load\(src,options=\{\}\)/);
  assert.match(loader,/window\.NovaModuleLoader=Object\.freeze/);
  assert.doesNotMatch(support,/function modulePresent\(/);
  assert.doesNotMatch(control,/function modulePresent\(/);
});

test('shared loader detects modules by id, shared dataset identity and resolved path',()=>{
  assert.match(loader,/script\.id===id/);
  assert.match(loader,/script\.dataset\.novaModule===moduleSrc\|\|script\.dataset\.novaFeature===moduleSrc/);
  assert.match(loader,/pathname\.endsWith\('\/'\+moduleSrc\)/);
  assert.match(loader,/const existing=find\(src,id\);if\(existing\)return existing/);
});

test('support bootstraps the loader once before loading the intelligence suite',()=>{
  assert.match(support,/let moduleLoaderPromise=null/);
  assert.match(support,/id='nova-shared-module-loader'/);
  assert.match(support,/src='module-loader\.js\?v=1'/);
  assert.match(support,/const loader=await ensureModuleLoader\(\)/);
  assert.match(support,/loader\.load\('guardian-live-analysis\.js\?v=2'/);
  assert.match(support,/loader\.load\('control-centre\.js\?v=1'/);
});

test('Control Centre reuses the shared loader and preserves ordered feature loading',()=>{
  assert.match(control,/window\.NovaModuleLoader/);
  assert.match(control,/loader\.load\(src\+'\?v=1',\{ordered:true,feature:true\}\)/);
  assert.doesNotMatch(control,/document\.createElement\('script'\)/);
  assert.ok(control.indexOf("'live-intelligence.js'") < control.indexOf("'pricing-intelligence.js'"));
  assert.ok(control.indexOf("'conversation.js'") < control.indexOf("'voice-assistant.js'"));
});

test('Control Centre presents Guardian as Nova Security while retaining the protected route',()=>{
  assert.match(control,/item\('Nova Security','Protected Guardian enforcement evidence and repair workflow\.'/);
  assert.match(control,/root\+'\/admin\/guardian\.html'/);
  assert.match(control,/<b>Guardian enforcement<\/b>/);
});

test('support retries the shared loader after a transient script failure',async()=>{
  const listeners={};
  const scripts=[];
  const document={
    head:{appendChild(script){scripts.push(script)}},
    getElementById(id){return scripts.find(script=>script.id===id&&!script.removed)||null},
    createElement(){return{addEventListener(type,listener){this['on'+type]=listener},remove(){this.removed=true}}}
  };
  const window={fetch:async()=>{},NovaAuth:{getAccessToken:()=>null},addEventListener(type,listener){listeners[type]=listener}};
  vm.runInNewContext(support,{window,document,location:{origin:'https://nova.test',href:'https://nova.test/'},Request:class Request{},URL,Response,console:{error(){}}});
  listeners['nova:authenticated']();
  assert.equal(scripts.length,1);
  scripts[0].onerror();
  await new Promise(resolve=>setImmediate(resolve));
  assert.equal(scripts[0].removed,true);
  listeners['nova:authenticated']();
  assert.equal(scripts.length,2);
});
