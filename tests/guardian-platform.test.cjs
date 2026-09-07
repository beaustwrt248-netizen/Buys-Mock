'use strict';
const assert=require('node:assert/strict');
const fs=require('node:fs');
const g=require('../admin/guardian-platform.js');
const release=require('../admin/guardian-release-evidence.js');

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
  const main='abc123';
  const runs=[
    {id:1,name:release.DEPLOY_WORKFLOW,head_sha:main,status:'completed',conclusion:'success',run_number:44,updated_at:'2026-09-07T10:00:00Z'},
    {id:2,name:release.DEPLOY_WORKFLOW,head_sha:'older',status:'completed',conclusion:'success',run_number:43,updated_at:'2026-09-07T09:50:00Z'},
    {id:3,name:release.DEPLOY_WORKFLOW,head_sha:main,status:'completed',conclusion:'failure',run_number:45,updated_at:'2026-09-07T10:05:00Z'}
  ];
  const incidents=[
    incident({id:'inside',created_at:'2026-09-07T10:30:00Z'}),
    incident({id:'outside',created_at:'2026-09-07T11:01:00Z'}),
    incident({id:'cancelled',state:'cancelled',created_at:'2026-09-07T10:20:00Z'})
  ];
  const exact=release.correlateExactDeployments(incidents,runs,main);
  assert.deepEqual(exact.map(x=>x.id),['inside'],'only open incidents inside the successful exact-head deployment window should correlate');
  assert.equal(exact[0].release_evidence.run_number,44);
  assert.equal(release.successfulDeployments(runs,main).length,1,'failed and wrong-head deployments must never count as authoritative success');
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
  const cancelled=incident({id:'cancelled',state:'cancelled',risk_level:'critical',diagnosis_summary:'runtime crash',updated_at:'2020-01-01T00:00:00Z'});
  const snapshot=g.buildSnapshot({settings:{enabled:true,operating_mode:'assist'},incidents:[cancelled],repairs:[],activity:[]});
  assert.equal(snapshot.openCount,0,'cancelled incidents are terminal');
  assert.equal(snapshot.criticalCount,0,'cancelled critical incidents must not remain critical-open');
  assert.equal(snapshot.approvalCount,0,'cancelled incidents must not remain approval-bound');
  assert.equal(snapshot.stale.length,0,'cancelled incidents must not be reported stale');
  const runtime=snapshot.domains.find(x=>x.key==='runtime');
  assert.equal(runtime.open,0);
  assert.equal(runtime.critical,0);
  assert.equal(runtime.status,'healthy');
}

{
  const live=fs.readFileSync('admin/guardian-live.js','utf8');
  assert.match(live,/resolved','ignored','cancelled/,'live Guardian sessions must use the same terminal-state contract');
  assert.match(live,/guardian-release-evidence\.js/,'authoritative release evidence must be loaded by Guardian');
}

{
  const nova=g.buildSnapshot({incidents:[],repairs:[],activity:[{actor:'nova',summary:'Nova proposed catalogue edit',created_at:new Date().toISOString()}]});
  assert.equal(nova.novaEventCount,1);
}

console.log('Guardian platform tests passed');
