import test from 'node:test';
import assert from 'node:assert/strict';
import { createAutomationRuntime } from '../src/automation-runtime.mjs';

function fakeStore(){ let jobs=[]; return { list:()=>jobs, create:i=>{const j={id:'1',...i};jobs=[j];return j}, update:(id,p)=>{jobs=jobs.map(j=>j.id===id?{...j,...p}:j);return jobs.find(j=>j.id===id)}, remove:()=>true }; }

test('local automation rejects protected execution intent', () => {
  const runtime=createAutomationRuntime({ store:fakeStore(), workspaceRuntime:{ snapshot:()=>({tasks:[],projects:[]}) } });
  for(const prompt of ['deploy production','approve pricing change','run guardian repair','publish OTA','change user role']) {
    assert.throws(()=>runtime.save({title:'Unsafe',prompt}),/AUTOMATION_PROTECTED_INTENT/);
  }
});

test('enabling a job changes metadata only and reports no background runner', () => {
  const runtime=createAutomationRuntime({ store:fakeStore(), workspaceRuntime:{ snapshot:()=>({tasks:[],projects:[]}) } });
  const job=runtime.save({title:'Summary',prompt:'Summarise my local tasks'});
  const result=runtime.setEnabled(job.id,true);
  assert.equal(result.job.state,'enabled');
  assert.match(result.message,/no background runner is connected/i);
  assert.equal(runtime.status(result.job).badge,'local-only');
});
