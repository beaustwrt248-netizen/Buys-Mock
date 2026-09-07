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
  assert.match(worker,/isSrcdocIncident\(input\)\?\["web-base\.html","web-admin-mode\.js","index\.html","tests\/web-admin-srcdoc-regression\.test\.js","tests\/guardian-runtime-regression\.test\.js"/);
  assert.match(worker,/hints\.push\("web-base\.html","web-admin-mode\.js","index\.html","tests\/web-admin-srcdoc-regression\.test\.js","tests\/guardian-runtime-regression\.test\.js"\)/);
  assert.match(worker,/diagnostic_kind,diagnostic_message,diagnostic_metadata/);
  assert.match(worker,/decode\(bytes\)\.slice\(0,90000\)/);
  assert.match(worker,/JSON\.stringify\(repo\)\.slice\(0,180000\)/);
  assert.match(worker,/Number\(x\.size\|\|0\)<=100000/);
  assert.match(worker,/if\(!allowed\.has\(f\.path\)\|\|!safeRepairPath\(f\.path\)\)/);
  assert.match(worker,/PROTECTED_CHANGE_BLOCKED/);
  assert.match(worker,/authorized=!!profile\?\.is_enabled&&\["admin","manager"\]\.includes\(profile\.role\)/);
  assert.match(worker,/state:"awaiting_approval"/);
});

test('Guardian canonicalises volatile runtime fingerprints and suppresses same-build terminal replay',()=>{
  const migration=read('supabase/migrations/20260903161500_guardian_runtime_diagnostic_dedup.sql');
  assert.match(migration,/regexp_replace\(coalesce\(trim\(p_route\),''\), '\\\?\.\*\$', ''\)/);
  assert.match(migration,/about:srcdoc\\\?\[\^:@\[:space:\]\]\*/);
  assert.match(migration,/v_fingerprint := left\('srv-' \|\| md5\(v_kind \|\| '\|' \|\| v_normalized_message \|\| '\|' \|\| v_normalized_route\),128\)/);
  assert.match(migration,/state not in \('resolved','ignored'\)/);
  assert.doesNotMatch(migration,/last_seen_at > now\(\) - interval '24 hours'/);
  assert.match(migration,/state in \('resolved','ignored'\)/);
  assert.match(migration,/app_version = left\(p_app_version,80\)/);
  assert.match(migration,/interval '30 minutes'/);
  assert.match(migration,/Superseded by the canonical Guardian incident/);
  assert.match(migration,/grant execute on function public\.guardian_report_diagnostic[^;]+to authenticated;/s);
  assert.match(migration,/if auth\.uid\(\) is null then raise exception 'Authentication required'; end if;/);
});

test('Nova security presentation keeps Guardian compatibility without transferring execution authority',()=>{
  const live=read('guardian-live.js');
  assert.ok(live.includes('window.MorleyGuardianLive=api;window.MorleyNovaSecurity=api;'));
  assert.ok(live.includes('Nova Security Activity'));
  assert.ok(live.includes('Guardian independently enforces approvals and safety boundaries'));
  for(const method of ['approve','merge','deploy','execute','disable','bypass','setRole','setPrice']){
    assert.ok(!live.includes(`MorleyNovaSecurity.${method}=`),`MorleyNovaSecurity must not define ${method} authority`);
  }
});

test('Nova Security & Guardian evidence remains fail-closed when enforcement evidence is incomplete',()=>{
  const status=read('nova/guardian-status.js');
  for(const token of [
    'Security & Guardian',
    'Guardian is Nova’s independent security and governance layer',
    'cannot bypass, weaken or inherit Guardian authority',
    "running.length?'INCOMPLETE'",
    'Security enforcement evidence is temporarily unavailable. Nova did not attempt any protected action.',
    'Guardian authority remains independent, fail-closed and protected.',
    'cannot approve, merge, deploy, execute, disable or bypass protected Guardian decisions'
  ]) assert.ok(status.includes(token),`Missing fail-closed Guardian contract: ${token}`);
});

test('Nova permission contract keeps protected production authority Guardian-enforced and human-gated',()=>{
  const permissions=read('nova/permissions.js');
  for(const token of [
    'this page reports authority and cannot elevate, approve or broaden it',
    'Guardian independently enforces protected production authority',
    'Protected pricing and approval state remain outside Nova authority',
    'cannot self-approve, disable, bypass or execute Guardian work',
    'Release, deployment, OTA and signing authority remain protected',
    'authorization/RLS changes require explicit protected implementation and review',
    'Permanent deletes and destructive production operations are prohibited through Nova actions',
    'Any .github/workflows change is high risk and requires explicit conversational authorization',
    'Governance evidence is temporarily unavailable. Nova did not broaden any permission.'
  ]) assert.ok(permissions.includes(token),`Missing Nova permission boundary: ${token}`);
});

test('persisted Guardian contracts and realtime compatibility names remain unchanged after Nova-facing unification',()=>{
  const live=read('guardian-live.js');
  const adminLive=read('admin/guardian-live.js');
  const worker=read('supabase/functions/guardian-worker/index.ts');
  const repairWorker=read('supabase/functions/guardian-repair-worker/index.ts');
  for(const token of ['guardian_activity','morley-guardian-user-activity']) assert.ok(live.includes(token),`Missing user Guardian compatibility contract: ${token}`);
  for(const token of ['guardian_incidents','guardian_activity']) assert.ok(adminLive.includes(token),`Missing admin Guardian compatibility contract: ${token}`);
  for(const token of ['guardian_settings','guardian_incidents','guardian_activity']) assert.ok(worker.includes(token),`Missing Guardian worker contract: ${token}`);
  for(const token of ['guardian_repairs','guardian_incidents']) assert.ok(repairWorker.includes(token),`Missing Guardian repair contract: ${token}`);
});
