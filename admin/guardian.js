(()=>{
  const $=id=>document.getElementById(id);
  const esc=v=>String(v??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
  const label=s=>String(s||'').replaceAll('_',' ').replace(/\b\w/g,c=>c.toUpperCase());
  const canApprove=s=>['proposed','awaiting_approval'].includes(s);
  const canRetry=s=>['failed','ignored'].includes(s);

  async function decideGuardian(incidentId,decision){
    if(!window.sb) return;
    const status=$('guardianActionStatus');
    if(status) status.textContent=`${label(decision)} in progress…`;
    const {error}=await window.sb.rpc('guardian_decide_incident',{incident_id:incidentId,decision});
    if(error){ if(status) status.textContent=error.message; return; }
    if(status) status.textContent=decision==='approve'
      ? 'Approval recorded. Guardian may advance only through the guarded repair pipeline; this does not grant direct merge or deploy permission.'
      : decision==='retry'
        ? 'Incident requeued for immediate Guardian triage.'
        : 'Incident rejected and moved to ignored.';
    await loadGuardian();
  }

  async function loadGuardian(){
    if(!window.sb||!$('guardianStatus')) return;
    $('guardianStatus').textContent='Refreshing Guardian…';
    const [settingsResult,incidentsResult]=await Promise.all([
      window.sb.from('guardian_settings').select('*').eq('singleton',true).single(),
      window.sb.from('guardian_incidents').select('id,ticket_id,source,state,risk_level,classification,confidence,diagnosis_summary,proposed_action,auto_fix_eligible,requires_approval,github_branch,github_pr_number,last_error_code,worker_version,reproduction_summary,test_plan,resolution_summary,last_worker_at,created_at,updated_at').order('created_at',{ascending:false}).limit(100)
    ]);
    if(settingsResult.error){$('guardianStatus').textContent=settingsResult.error.message;return;}
    if(incidentsResult.error){$('guardianStatus').textContent=incidentsResult.error.message;return;}
    const s=settingsResult.data||{}, rows=incidentsResult.data||[];
    $('guardianEnabled').checked=!!s.enabled;
    $('guardianAutoFix').checked=!!s.auto_fix_enabled;
    $('guardianMaxRisk').value=s.max_auto_risk||'low';
    $('guardianHumanCode').checked=s.require_human_for_code!==false;
    $('guardianMetricOpen').textContent=rows.filter(x=>!['resolved','ignored'].includes(x.state)).length;
    $('guardianMetricApproval').textContent=rows.filter(x=>canApprove(x.state)).length;
    $('guardianMetricResolved').textContent=rows.filter(x=>x.state==='resolved').length;
    $('guardianMetricFailed').textContent=rows.filter(x=>x.state==='failed').length;
    $('guardianStatus').textContent=s.enabled
      ? `ONLINE • Report-a-Problem intake active • ${s.auto_fix_enabled?'allowlisted auto-fix permission enabled':'auto-fix permission locked'}`
      : 'PAUSED • new Report-a-Problem tickets will not enter Guardian';
    $('guardianList').innerHTML=rows.map(x=>{
      const confidence=x.confidence==null?'—':`${Math.round(Number(x.confidence)*100)}%`;
      const actions=[
        canApprove(x.state)?`<button class="guardian-action" data-id="${esc(x.id)}" data-decision="approve">Approve</button>`:'',
        canApprove(x.state)?`<button class="guardian-action ghost" data-id="${esc(x.id)}" data-decision="reject">Reject</button>`:'',
        canRetry(x.state)?`<button class="guardian-action ghost" data-id="${esc(x.id)}" data-decision="retry">Retry</button>`:''
      ].filter(Boolean).join('');
      return `<div class="row"><div class="row-main"><div class="row-title">${esc(label(x.classification||x.source))}</div><div class="muted">${esc(label(x.state))} • ${esc(String(x.risk_level||'unknown').toUpperCase())} risk • confidence ${esc(confidence)} • ${new Date(x.created_at).toLocaleString()}${x.ticket_id?` • ticket ${esc(x.ticket_id.slice(0,8))}`:''}${x.github_pr_number?` • PR #${x.github_pr_number}`:''}</div>${x.worker_version?`<div class="muted" style="margin-top:6px">Worker: ${esc(x.worker_version)}${x.last_worker_at?` • last run ${new Date(x.last_worker_at).toLocaleString()}`:''}</div>`:''}${x.diagnosis_summary?`<div class="muted" style="margin-top:6px"><strong>Diagnosis:</strong> ${esc(x.diagnosis_summary)}</div>`:''}${x.reproduction_summary?`<div class="muted" style="margin-top:6px"><strong>Reproduction:</strong> ${esc(x.reproduction_summary)}</div>`:''}${x.proposed_action?`<div class="muted" style="margin-top:6px"><strong>Proposed:</strong> ${esc(x.proposed_action)}</div>`:''}${x.test_plan?`<div class="muted" style="margin-top:6px"><strong>Test plan:</strong> ${esc(x.test_plan)}</div>`:''}${x.resolution_summary?`<div class="muted" style="margin-top:6px"><strong>Resolution:</strong> ${esc(x.resolution_summary)}</div>`:''}${x.last_error_code?`<div class="status">${esc(x.last_error_code)}</div>`:''}${actions?`<div class="actions" style="margin-top:10px">${actions}</div>`:''}</div><span class="pill ${x.state==='resolved'?'ok':''}">${esc(label(x.state))}</span></div>`;
    }).join('')||'<div class="muted">No Guardian incidents yet. New problem reports will appear here automatically.</div>';
    document.querySelectorAll('.guardian-action').forEach(btn=>btn.addEventListener('click',()=>decideGuardian(btn.dataset.id,btn.dataset.decision)));
  }

  async function saveGuardian(){
    if(!window.sb) return;
    $('guardianSaveStatus').textContent='Saving…';
    const {data:{user}}=await window.sb.auth.getUser();
    const payload={enabled:$('guardianEnabled').checked,auto_fix_enabled:$('guardianAutoFix').checked,max_auto_risk:$('guardianMaxRisk').value,require_human_for_code:$('guardianHumanCode').checked,updated_by:user?.id||null};
    if(!payload.require_human_for_code){$('guardianSaveStatus').textContent='Code-changing fixes must keep human approval enabled in Guardian Core.';$('guardianHumanCode').checked=true;return;}
    const {error}=await window.sb.from('guardian_settings').update(payload).eq('singleton',true);
    $('guardianSaveStatus').textContent=error?error.message:'Guardian controls saved and audited.';
    if(!error) await loadGuardian();
  }

  $('guardianSaveBtn')?.addEventListener('click',saveGuardian);
  $('guardianRefreshBtn')?.addEventListener('click',loadGuardian);
  window.loadGuardian=loadGuardian;
  window.sb?.auth?.onAuthStateChange?.((event,session)=>{if(session)setTimeout(loadGuardian,0)});
})();