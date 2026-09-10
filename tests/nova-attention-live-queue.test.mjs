import test from 'node:test';
import assert from 'node:assert/strict';
import vm from 'node:vm';
import {readFile} from 'node:fs/promises';

async function source(path){return readFile(new URL(`../${path}`,import.meta.url),'utf8')}
async function attentionModel(){const code=await source('admin/nova-attention-model.js');const sandbox={};sandbox.globalThis=sandbox;vm.runInNewContext(code,sandbox);return sandbox.MorleyNovaAttention}

test('Nova attention Edge Function uses the database queue status contract',async()=>{
  const code=await source('supabase/functions/nova-attention-control/index.ts');
  for(const status of ['pending','in_progress','verified','discrepancy','blocked','failed']) assert.match(code,new RegExp(`['\"]${status}['\"]`));
  assert.doesNotMatch(code,/['"]processing['"]/);
  assert.match(code,/select\('id',\{count:'exact',head:true\}\)/);
  assert.match(code,/queue_statuses:statusCounts/);
});

test('Nova attention model preserves exact active, blocked, discrepancy and verified counts',async()=>{
  const model=await attentionModel();
  const built=model.build({
    findings:[{id:'11111111-1111-1111-1111-111111111111',device_id:7,field_name:'model_number',status:'confirmed',requires_approval:true}],
    queue:[
      {id:1,device_id:7,status:'discrepancy',priority:80,reason:'missing_model_number'},
      {id:2,device_id:8,status:'blocked',priority:50,reason:'scheduled_recheck'},
    ],
    counts:{pending_work:122,queue_statuses:{pending:0,in_progress:0,verified:713,discrepancy:51,blocked:71,failed:0}},
    checked_at:'2026-09-09T11:00:00.000Z',
  });
  assert.equal(built.pendingWork,122);
  assert.equal(built.verifiedWork,713);
  assert.equal(built.discrepancyWork,51);
  assert.equal(built.blockedWork,71);
  assert.equal(built.confirmed.length,1);
});

test('Catalogue Autopilot consumes protected Nova audit evidence without adding an apply action',async()=>{
  const code=await source('admin/catalogue-autopilot.js');
  assert.match(code,/MorleyNovaAttentionSnapshot/);
  assert.match(code,/morley:nova-attention-updated/);
  assert.match(code,/Confirmation alone does not change catalogue data/);
  assert.doesNotMatch(code,/data-apply=/);
  assert.doesNotMatch(code,/deleteListing|autoDelete|applyPatch/);
});
