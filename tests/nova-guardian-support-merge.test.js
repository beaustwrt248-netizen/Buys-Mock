const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');
const vm=require('node:vm');

const read=path=>fs.readFileSync(path,'utf8');

test('Nova merges Guardian and support evidence into one attention surface without inheriting protected authority',()=>{
  const source=read('nova/guardian-status.js');
  assert.match(source,/UNIFIED TRIAGE/);
  assert.match(source,/supportChecked/);
  assert.match(source,/supportSla/);
  assert.match(source,/supportHigh/);
  assert.match(source,/guardianState/);
  assert.match(source,/guardianOpen/);
  assert.match(source,/Support queue pressure/);
  assert.match(source,/Guardian enforcement needs review/);
  assert.match(source,/data-nova-merged-attention/);
  assert.match(source,/metricAttention/);
  assert.match(source,/window\.NovaGuardianSupportMerge=\{render:renderUnifiedAttention\}/);
  assert.match(source,/protected Guardian decisions remain human-controlled/);
  assert.match(source,/cannot approve, merge, deploy, execute, disable or bypass protected Guardian decisions/);
});

test('merged attention fails closed when authoritative support or Guardian evidence is unavailable',()=>{
  const source=read('nova/guardian-status.js');
  assert.match(source,/supportChecked==='UNAVAILABLE'/);
  assert.match(source,/Support evidence unavailable/);
  assert.match(source,/\['BLOCKED','DEGRADED','INCOMPLETE'\]\.includes\(guardianState\)/);
  assert.match(source,/Guardian authority remains independent, fail-closed and protected/);
  assert.doesNotMatch(source,/supportChecked==='UNAVAILABLE'.*CLEAR/);
  assert.doesNotMatch(source,/guardianState.*DEGRADED.*HEALTHY/);
});

test('merged attention is advisory navigation, not a protected write executor',()=>{
  const source=read('nova/guardian-status.js');
  assert.match(source,/data-attention-section/);
  assert.match(source,/document\.querySelector\(`\[data-section=/);
  assert.doesNotMatch(source,/fetch\([^\n]*(?:POST|PATCH|DELETE)/i);
  assert.doesNotMatch(source,/method\s*:\s*['"](?:POST|PATCH|PUT|DELETE)['"]/i);
});

test('merged attention clears stale GitHub rows when the source queue becomes empty',()=>{
  const source=read('nova/guardian-status.js');
  const elements={
    attentionList:{
      children:[{
        outerHTML:'<a class="item">stale protected PR</a>',
        hasAttribute:()=>false,
        classList:{contains:()=>false},
      }],
      innerHTML:'',
    },
    metricAttention:{textContent:'1'},
    supportChecked:{textContent:'CHECKED 2026-09-08'},
    supportSla:{textContent:'0'},
    supportHigh:{textContent:'0'},
    guardianState:{textContent:'ENFORCED'},
    guardianOpen:{textContent:'0'},
  };
  const document={
    readyState:'loading',
    getElementById:id=>elements[id]||null,
    querySelector:()=>null,
    querySelectorAll:()=>[],
    addEventListener:()=>{},
  };
  const window={addEventListener:()=>{}};
  vm.runInNewContext(source,{
    window,
    document,
    console,
    clearTimeout,
    setTimeout,
    MutationObserver:class{observe(){}},
  });

  window.NovaGuardianSupportMerge.render();
  assert.match(elements.attentionList.innerHTML,/stale protected PR/);

  elements.attentionList.children=[{
    outerHTML:'<div class="empty">No current item requires human attention.</div>',
    hasAttribute:()=>false,
    classList:{contains:name=>name==='empty'},
  }];
  window.NovaGuardianSupportMerge.render();

  assert.doesNotMatch(elements.attentionList.innerHTML,/stale protected PR/);
  assert.match(elements.attentionList.innerHTML,/No current item requires human attention/);
  assert.equal(elements.metricAttention.textContent,'0');
});

