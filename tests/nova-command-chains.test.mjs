import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source=fs.readFileSync('nova/command-chains.js','utf8');

function boot(){
  const safe=id=>({id,risk:'safe',requiresGuardian:false,handler(){}});
  const protectedCommand={id:'release.production.deploy',risk:'destructive',requiresGuardian:true,handler:null};
  const lookup={
    'show catalogue status':safe('status.catalogue.read'),
    'show support status':safe('status.support.read'),
    'deploy production':protectedCommand
  };
  const calls=[];
  const base={
    resolve(input){const command=lookup[String(input).trim().toLowerCase()];return command?{command,args:{},confidence:1}:null},
    async execute(input){const resolved=this.resolve(input);if(!resolved)return{ok:false,type:'unknown',message:'unknown'};if(resolved.command.risk!=='safe')return{ok:false,type:'protected',command:resolved.command.id,message:'protected'};calls.push(resolved.command.id);return{ok:true,type:'executed',command:resolved.command.id,message:resolved.command.id}},
    stats:()=>({}),registry:Object.values(lookup),risk:{SAFE:'safe'}
  };
  const store=new Map();
  const sessionStorage={getItem:k=>store.has(k)?store.get(k):null,setItem:(k,v)=>store.set(k,v)};
  class Event{constructor(type){this.type=type}}
  class CustomEvent extends Event{constructor(type,init={}){super(type);this.detail=init.detail}}
  const window={NovaCommands:base,dispatchEvent(){},addEventListener(){}};
  vm.runInNewContext(source,{window,sessionStorage,Event,CustomEvent,console},{filename:'nova/command-chains.js'});
  return{commands:window.NovaCommands,calls};
}

test('runs multiple safe read-only commands in order',async()=>{
  const {commands,calls}=boot();
  const result=await commands.execute('show catalogue status then show support status');
  assert.equal(result.ok,true);
  assert.equal(result.type,'chain');
  assert.deepEqual(calls,['status.catalogue.read','status.support.read']);
  assert.equal(result.results.length,2);
});

test('preflights the whole chain and blocks all steps if one is protected',async()=>{
  const {commands,calls}=boot();
  const result=await commands.execute('show catalogue status then deploy production');
  assert.equal(result.ok,false);
  assert.equal(result.type,'protected');
  assert.deepEqual(calls,[]);
  assert.match(result.message,/No step in this chain was run/);
});

test('preflights unknown chain members and runs nothing',async()=>{
  const {commands,calls}=boot();
  const result=await commands.execute('show catalogue status then invent a thing');
  assert.equal(result.ok,false);
  assert.equal(result.type,'unknown');
  assert.deepEqual(calls,[]);
});

test('records bounded session history without storing auth data',async()=>{
  const {commands}=boot();
  await commands.execute('show catalogue status');
  await commands.execute('show support status');
  const history=commands.getHistory();
  assert.equal(history.length,2);
  assert.equal(history[0].input,'show support status');
  assert.ok(history.every(row=>!('token' in row)&&!('access_token' in row)));
  assert.match(commands.historyText(),/show support status/);
});

test('negated text is never split into a chain',()=>{
  const {commands}=boot();
  assert.equal(commands.planChain('do not deploy production then show support status'),null);
});

test('command discovery loads chains after it installs',()=>{
  const discovery=fs.readFileSync('nova/command-discovery.js','utf8');
  assert.match(discovery,/command-chains\.js\?v=1/);
  assert.match(discovery,/data-nova-command-chains/);
  assert.match(discovery,/loadChains\(\)/);
});
