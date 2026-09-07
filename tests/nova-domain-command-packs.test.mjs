import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source=fs.readFileSync('nova/domain-command-packs.js','utf8');

function boot(){
  const events=[];
  const base={
    risk:{SAFE:'safe',WRITE:'write',SENSITIVE:'sensitive',DESTRUCTIVE:'destructive'},
    resolve:()=>null,
    execute:async()=>({ok:false,type:'unknown',message:'unknown'}),
    stats:()=>({canonicalCommands:0,understoodPhrases:0,namespaces:{},collisions:[]}),
    registry:[],
    help:()=>'',
    getAudit:()=>[]
  };
  const window={NovaCommands:base,dispatchEvent:event=>events.push(event)};
  const document={querySelector:()=>null,getElementById:()=>null};
  class CustomEvent{constructor(type,init={}){this.type=type;this.detail=init.detail}}
  const context={window,document,CustomEvent,CSS:{escape:value=>String(value)},console,setTimeout,clearTimeout};
  vm.runInNewContext(source,context,{filename:'nova/domain-command-packs.js'});
  return{commands:window.NovaCommands,events};
}

test('Nova domain packs add broad canonical coverage without alias collisions',()=>{
  const {commands}=boot();
  const stats=commands.stats();
  assert.ok(stats.domainPacks>=15,`expected >=15 packs, got ${stats.domainPacks}`);
  assert.ok(stats.domainCommands>=125,`expected >=125 domain commands, got ${stats.domainCommands}`);
  assert.ok(stats.parameterisedCommands>=5,`expected parameterised commands, got ${stats.parameterisedCommands}`);
  assert.ok(stats.understoodPhrases>=100000,`expected >=100000 phrases, got ${stats.understoodPhrases}`);
  assert.equal(stats.collisions.length,0,JSON.stringify(stats.collisions.slice(0,5)));
});

test('Natural read-only domain commands execute and protected commands remain blocked',async()=>{
  const {commands}=boot();
  const safe=await commands.execute('Nova please show missing device images for me');
  assert.equal(safe.ok,true);
  assert.match(safe.command,/^images\./);
  const protected=await commands.execute('please deploy production now');
  assert.equal(protected.ok,false);
  assert.equal(protected.type,'protected');
  assert.match(protected.message,/human\/Guardian approval boundary/);
});

test('Parameterised commands preserve the requested subject',()=>{
  const {commands}=boot();
  const research=commands.resolve('Nova please research device Motorola Edge 60 Fusion');
  assert.equal(research.command.id,'research.device.lookup');
  assert.equal(research.args.remainder,'motorola edge 60 fusion');
  const market=commands.resolve('compare market price for Galaxy S25 Ultra');
  assert.equal(market.command.id,'pricing.market.compare');
  assert.equal(market.args.remainder,'galaxy s25 ultra');
});

test('Domain packs preserve negation safety and publish readiness event',()=>{
  const {commands,events}=boot();
  assert.equal(commands.resolve('do not deploy production now'),null);
  assert.equal(commands.resolve('do not research device Pixel 10'),null);
  assert.ok(events.some(event=>event.type==='nova:command-packs-ready'));
  assert.ok(commands.domainPacks.includes('catalogue'));
  assert.ok(commands.domainPacks.includes('guardian'));
  assert.ok(commands.domainPacks.includes('security'));
});

test('Loader waits for authenticated command registry and then loads domain packs',()=>{
  const recommendations=fs.readFileSync('nova/recommendations.js','utf8');
  assert.match(recommendations,/command-registry\.js\?v=1/);
  assert.match(recommendations,/domain-command-packs\.js\?v=1/);
  assert.match(recommendations,/data-nova-command-packs/);
});
