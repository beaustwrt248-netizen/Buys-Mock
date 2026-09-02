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
