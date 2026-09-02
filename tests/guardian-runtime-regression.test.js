const test=require('node:test');
const assert=require('node:assert/strict');
const fs=require('node:fs');

const read=path=>fs.readFileSync(path,'utf8');
const staleCommit=['013aa357','7f8c714297eda86bfd2c1d49b3667eb1'].join('');

test('web runtime uses the repository-owned srcdoc base without a stale remote fallback',()=>{
  const shell=read('index.html');
  const workflow=read('.github/workflows/deploy-admin-pages.yml');
  assert.match(shell,/const CANDIDATE='web-base\.html';/);
  assert.doesNotMatch(shell,/REMOTE_CANDIDATE/);
  assert.ok(!shell.includes(staleCommit));
  assert.match(workflow,/cp web-base\.html site\//);
  assert.ok(!workflow.includes(staleCommit));
  assert.ok(fs.statSync('web-base.html').size>1000);
});

test('repository-owned srcdoc base binds recent activity to an explicit DOM target',()=>{
  const source=read('web-base.html');
  assert.match(source,/const recentEl=document\.getElementById\('recent'\);recentEl\.innerHTML=/);
  assert.match(source,/recentEl\.insertAdjacentHTML\('beforeend'/);
  assert.doesNotMatch(source,/(?<![A-Za-z0-9_$])recent\.(?:innerHTML|insertAdjacentHTML)/);
});

test('about:srcdoc repair discovery prioritises the generating sources and keeps protection gates',()=>{
  const worker=read('supabase/functions/guardian-repair-worker/index.ts');
  assert.match(worker,/isSrcdocIncident\(input\)\?\["web-base\.html","index\.html"/);
  assert.match(worker,/hints\.push\("web-base\.html","index\.html"\)/);
  assert.match(worker,/diagnostic_kind,diagnostic_message,diagnostic_metadata/);
  assert.match(worker,/if\(!allowed\.has\(f\.path\)\|\|!safeRepairPath\(f\.path\)\)/);
  assert.match(worker,/PROTECTED_CHANGE_BLOCKED/);
  assert.match(worker,/authorized=!!profile\?\.is_enabled&&\["admin","manager"\]\.includes\(profile\.role\)/);
  assert.match(worker,/state:"awaiting_approval"/);
});
