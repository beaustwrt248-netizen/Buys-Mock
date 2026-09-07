import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

function loadRegistry(){
  const source=fs.readFileSync('nova/command-registry.js','utf8');
  const events=[];
  const window={
    addEventListener(){},
    dispatchEvent(event){events.push(event);return true;}
  };
  class CustomEvent{
    constructor(type,options={}){this.type=type;this.detail=options.detail;}
  }
  const context={window,document:{},CustomEvent,console,Date,CSS:{escape:value=>String(value)},setTimeout,clearTimeout};
  vm.runInNewContext(source,context,{filename:'nova/command-registry.js'});
  return{api:window.NovaCommands,events};
}

test('Nova command registry scales to thousands of understood command phrases without alias collisions',()=>{
  const {api}=loadRegistry();
  const stats=api.stats();
  assert.ok(stats.canonicalCommands>=25,`expected at least 25 canonical commands, got ${stats.canonicalCommands}`);
  assert.ok(stats.understoodPhrases>=10000,`expected at least 10,000 understood phrases, got ${stats.understoodPhrases}`);
  assert.equal(stats.collisions.length,0,`unexpected alias collisions: ${JSON.stringify(stats.collisions.slice(0,5))}`);
});

test('Nova resolves natural polite variants to the same canonical command',()=>{
  const {api}=loadRegistry();
  assert.equal(api.resolve('show catalogue status')?.command?.id,'status.catalogue.read');
  assert.equal(api.resolve('Nova please show catalogue status for me')?.command?.id,'status.catalogue.read');
  assert.equal(api.resolve('could you open guardian please')?.command?.id,'navigation.guardian.open');
  assert.equal(api.resolve('how many commands do you know')?.command?.id,'nova.command.stats');
});

test('Nova does not turn negated requests into affirmative fuzzy commands',()=>{
  const {api}=loadRegistry();
  assert.equal(api.resolve("don't open guardian"),null);
  assert.equal(api.resolve('do not refresh everything'),null);
  assert.equal(api.resolve('never show support status'),null);
  assert.equal(api.resolve('what can nova not do')?.command?.id,'security.boundaries.explain');
});

test('protected commands are recognised but cannot bypass Guardian or human approval',async()=>{
  const {api,events}=loadRegistry();
  const deploy=api.resolve('deploy production');
  assert.equal(deploy?.command?.id,'release.deploy.production');
  assert.equal(deploy?.command?.requiresGuardian,true);
  const response=await api.execute('deploy production');
  assert.equal(response.ok,false);
  assert.equal(response.type,'protected');
  assert.match(response.message,/will not execute/i);
  assert.match(response.message,/approval boundary/i);
  assert.equal(events.at(-1)?.type,'nova:command-audit');
  assert.equal(events.at(-1)?.detail?.allowed,false);
});

test('safe command help executes through the same audited dispatcher',async()=>{
  const {api,events}=loadRegistry();
  const response=await api.execute('Nova help please');
  assert.equal(response.ok,true);
  assert.equal(response.command,'nova.help');
  assert.match(response.message,/show catalogue status/i);
  assert.equal(events.at(-1)?.detail?.allowed,true);
});

test('authenticated Nova loader includes the command registry without changing auth logic',()=>{
  const recommendations=fs.readFileSync('nova/recommendations.js','utf8');
  assert.match(recommendations,/command-registry\.js\?v=1/);
  assert.match(recommendations,/nova:authenticated/);
  assert.match(recommendations,/if\(window\.NovaAuth\)load\(\)/);
});
