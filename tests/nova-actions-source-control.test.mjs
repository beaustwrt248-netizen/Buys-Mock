import test from 'node:test';
import assert from 'node:assert/strict';
import {readFile} from 'node:fs/promises';

const path=new URL('../supabase/functions/nova-actions/index.ts',import.meta.url);

test('Nova actions remains Admin-only and audited',async()=>{
  const code=await readFile(path,'utf8');
  assert.match(code,/p\.role!==['"]admin['"]/);
  assert.match(code,/is_enabled/);
  assert.match(code,/admin_audit_log/);
  assert.match(code,/source:'nova-actions'/);
});

test('Nova catalogue proposals remain approval-required and are not applied',async()=>{
  const code=await readFile(path,'utf8');
  assert.match(code,/requires_approval:true/);
  assert.match(code,/status:'open'/);
  assert.match(code,/catalogue_finding/);
  assert.match(code,/catalogue_queue/);
  assert.doesNotMatch(code,/\.from\(['"]device_catalog['"]\)\.update/);
});

test('Nova cannot self-authorize protected or destructive execution',async()=>{
  const code=await readFile(path,'utf8');
  for(const action of ['pricing_write','guardian_approve','guardian_repair','guardian_decide','release','deploy','ota','role_change','user_change','delete','catalogue_apply','support_send','github_merge','github_release']){
    assert.match(code,new RegExp(`['\"]${action}['\"]`));
  }
  assert.match(code,/human-gated or prohibited for Nova execution/);
});

test('support and engineering outputs remain proposals/drafts rather than external execution',async()=>{
  const code=await readFile(path,'utf8');
  assert.match(code,/\[Nova draft — not sent\]/);
  assert.match(code,/proposal_only:true/);
  assert.match(code,/requires_human_merge:true/);
  assert.match(code,/merge:false,deploy:false/);
});
