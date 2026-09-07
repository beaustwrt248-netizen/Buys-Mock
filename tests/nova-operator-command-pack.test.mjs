import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source=fs.readFileSync('nova/operator-command-pack.js','utf8');

function boot(){
  const events=[];
  const base={
    risk:{SAFE:'safe'},
    resolve:()=>null,
    execute:async()=>({ok:false,type:'unknown',message:'unknown'}),
    stats:()=>({canonicalCommands:100,understoodPhrases:100000,namespaces:{nova:1},collisions:[]}),
    registry:[]
  };
  const analysis={
    open:[{id:1},{id:2}],capital:1500,sold:2200,profit:600,margin:27.2727,roi:40,profitDelta:20,
    aged:[{days:120,item_summary:'Galaxy S24',item_type:'mobile'},{days:45,item_summary:'Pixel 9',item_type:'mobile'}],
    cats:new Map([['Mobile',{count:3,sales:1800,profit:500,cost:1000}],['Console',{count:1,sales:400,profit:100,cost:250}]]),
    channels:new Map([['Store',{count:2,sales:1200,profit:350}],['Online',{count:2,sales:1000,profit:250}]])
  };
  const window={NovaCommands:base,NovaBusinessIntelligence:{load:async()=>({checked_at:'2026-09-08T00:00:00Z'}),analyse:()=>analysis},dispatchEvent:event=>events.push(event)};
  const document={
    querySelector(selector){if(selector==='[data-section="business"]')return{click(){}};return null},
    createElement(){return{dataset:{},addEventListener(){},set src(_v){},set async(_v){}}},
    head:{appendChild(){}}
  };
  class CustomEvent{constructor(type,init={}){this.type=type;this.detail=init.detail}}
  const context={window,document,CustomEvent,Intl,Map,Date,console};
  vm.runInNewContext(source,context,{filename:'nova/operator-command-pack.js'});
  return{commands:window.NovaCommands,events};
}

test('adds real business handlers without collisions',()=>{
  const {commands}=boot();
  const stats=commands.stats();
  assert.equal(stats.operatorCommands,6);
  assert.equal(stats.collisions.length,0,JSON.stringify(stats.collisions));
  assert.ok(stats.understoodPhrases>100500);
  assert.ok(commands.operatorCommands.includes('business.performance.read'));
});

test('business performance command executes against business intelligence',async()=>{
  const {commands}=boot();
  const result=await commands.execute('Nova please show business performance for me');
  assert.equal(result.ok,true);
  assert.equal(result.command,'business.performance.read');
  assert.match(result.message,/2 stock on hand/);
  assert.match(result.message,/30-day revenue/);
  assert.match(result.message,/ROI 40\.0%/);
});

test('business ageing and category commands report live analysis',async()=>{
  const {commands}=boot();
  const aged=await commands.execute('show ageing stock');
  assert.equal(aged.ok,true);
  assert.match(aged.message,/2 items are 30\+ days old/);
  assert.match(aged.message,/1 are 90\+ days old/);
  const cats=await commands.execute('show category performance');
  assert.equal(cats.ok,true);
  assert.match(cats.message,/Mobile/);
});

test('negated business requests do not resolve to operator commands',()=>{
  const {commands}=boot();
  assert.equal(commands.resolve('do not show business performance'),null);
});

test('operator commands stay safe and chainable metadata is explicit',()=>{
  const {commands}=boot();
  for(const id of commands.operatorCommands){
    const command=commands.registry.find(c=>c.id===id);
    assert.equal(command.risk,'safe');
    assert.equal(command.requiresGuardian,false);
    assert.equal(typeof command.handler,'function');
  }
});

test('discovery loads operator commands before command chains',()=>{
  const discovery=fs.readFileSync('nova/command-discovery.js','utf8');
  assert.match(discovery,/operator-command-pack\.js\?v=1/);
  assert.match(discovery,/data-nova-operator-commands/);
  const operatorAt=discovery.indexOf('loadOperatorCommands();');
  const chainLoader=discovery.indexOf("script.src='command-chains.js?v=1'");
  assert.ok(operatorAt>=0&&chainLoader>=0);
  assert.match(discovery,/script\.addEventListener\('load',loadChains/);
});
