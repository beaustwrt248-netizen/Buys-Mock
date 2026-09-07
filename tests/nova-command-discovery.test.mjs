import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source=fs.readFileSync('nova/command-discovery.js','utf8');

function boot(){
  const registry=[{id:'research.device.lookup',namespace:'research',description:'Research a device',aliases:['research device'],risk:'safe',requiresGuardian:false}];
  const base={
    resolve(input){const value=String(input).trim().toLowerCase();if(value.startsWith('research device '))return{command:registry[0],args:{remainder:value.slice('research device '.length)},confidence:1};if(/\b(?:not|dont|cannot|never|stop|avoid)\b/.test(value))return null;return null},
    async execute(input){const resolved=this.resolve(input);return resolved?{ok:true,type:'executed',command:resolved.command.id,message:resolved.args.remainder}:{ok:false,type:'unknown',message:'unknown'}},
    stats:()=>({canonicalCommands:1,understoodPhrases:1,namespaces:{research:1},collisions:[]}),
    registry,
    risk:{SAFE:'safe'},domainPacks:['research']
  };
  const document={readyState:'loading',addEventListener(){},getElementById:()=>null,querySelector:()=>null,createElement:()=>({setAttribute(){},addEventListener(){},appendChild(){},querySelector(){return null},style:{},dataset:{}}),head:{appendChild(){}},body:{appendChild(){}}};
  class Event{constructor(type){this.type=type}}
  const window={NovaCommands:base,dispatchEvent(){}};
  vm.runInNewContext(source,{window,document,Event,console},{filename:'nova/command-discovery.js'});
  return window.NovaCommands;
}

test('polite parameterised phrasing resolves without losing the subject',()=>{
  const commands=boot();
  const resolved=commands.resolve('Could you research device Pixel 10 Pro for me');
  assert.equal(resolved.command.id,'research.device.lookup');
  assert.equal(resolved.args.remainder,'pixel 10 pro');
});

test('polite parameterised phrasing executes through the existing guarded executor',async()=>{
  const commands=boot();
  const result=await commands.execute('I need you to research device Galaxy S25 now');
  assert.equal(result.ok,true);
  assert.equal(result.command,'research.device.lookup');
  assert.equal(result.message,'galaxy s25');
});

test('negated requests do not become executable when politeness is stripped',async()=>{
  const commands=boot();
  assert.equal(commands.resolve('Please do not research device Pixel 10'),null);
  const result=await commands.execute('Please do not research device Pixel 10');
  assert.equal(result.ok,false);
});

test('discovery UI escapes registry content and exposes accessible close behaviour',()=>{
  assert.match(source,/const esc=/);
  assert.match(source,/aria-modal/);
  assert.match(source,/e\.key==='Escape'/);
  assert.match(source,/data-nova-command-browser-button/);
});

test('recommendations loads registry, packs, then discovery robustly',()=>{
  const recommendations=fs.readFileSync('nova/recommendations.js','utf8');
  assert.match(recommendations,/command-registry\.js\?v=1/);
  assert.match(recommendations,/domain-command-packs\.js\?v=1/);
  assert.match(recommendations,/command-discovery\.js\?v=1/);
  assert.match(recommendations,/window\.NovaCommands\?\.domainPacks/);
});
