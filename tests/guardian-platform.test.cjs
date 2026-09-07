'use strict';
const assert=require('node:assert/strict');
const g=require('../admin/guardian-platform.js');

function incident(overrides={}){return {id:'i1',source:'runtime_crash',classification:'javascript_error',state:'diagnosing',risk_level:'low',confidence:0.95,auto_fix_eligible:true,created_at:'2026-09-07T10:10:00Z',updated_at:'2026-09-07T10:10:00Z',...overrides}}

{
  const rows=[incident({id:'a',last_error_code:'ReferenceError: recent is not defined',occurrence_count:2}),incident({id:'b',last_error_code:'ReferenceError: recent is not defined',occurrence_count:3})];
  const groups=g.groupIncidents(rows);
  assert.equal(groups.length,1,'duplicate runtime failures should collapse to one pattern');
  assert.equal(groups[0].occurrences,5);
  assert.match(g.rootCauseHint(rows[0]),/symbol\/scope regression/i);
}

{
  const safe=incident({diagnosis_summary:'Fix null guard in non-sensitive UI formatter'});
  const protectedIncident=incident({diagnosis_summary:'Change RLS authentication policy',risk_level:'low'});
  const settings={enabled:true,kill_switch:false,operating_mode:'guarded_auto',auto_fix_enabled:true,confidence_threshold:0.85};
  assert.equal(g.policyDecision(safe,settings).eligible,true,'allowlisted low-risk repair may be eligible');
  assert.equal(g.policyDecision(safe,settings).requiresApproval,false);
  assert.equal(g.policyDecision(protectedIncident,settings).eligible,false,'protected boundary must never auto-expand authority');
  assert.equal(g.policyDecision(protectedIncident,settings).requiresApproval,true);
}

{
  const incidents=[incident({id:'after-release',risk_level:'high',created_at:'2026-09-07T10:30:00Z'})];
  const activity=[{phase:'release',summary:'Production deploy completed',created_at:'2026-09-07T10:00:00Z'}];
  assert.equal(g.correlateDeployments(incidents,activity).length,1,'incident inside one-hour release window should be correlated');
}

{
  const snapshot=g.buildSnapshot({settings:{enabled:true,operating_mode:'assist'},incidents:[incident({risk_level:'critical',diagnosis_summary:'runtime crash'})],repairs:[],activity:[]});
  assert.ok(snapshot.criticalCount===1);
  assert.ok(snapshot.findings.some(x=>x.level==='critical'));
  const backup=snapshot.domains.find(x=>x.key==='backup');
  assert.equal(backup.status,'unverified','missing telemetry must not be reported healthy');
  assert.equal(backup.score,null);
  assert.ok(snapshot.coverage<100);
}

{
  const nova=g.buildSnapshot({incidents:[],repairs:[],activity:[{actor:'nova',summary:'Nova proposed catalogue edit',created_at:new Date().toISOString()}]});
  assert.equal(nova.novaEventCount,1);
}

console.log('Guardian platform tests passed');
