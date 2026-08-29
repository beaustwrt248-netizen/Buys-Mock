(()=>{
  const $=id=>document.getElementById(id);
  const esc=v=>String(v??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
  const label=s=>String(s||'').replaceAll('_',' ').replace(/\b\w/g,c=>c.toUpperCase());

  async function loadGuardian(){
    if(!window.sb||!$('guardianStatus')) return;
    $('guardianStatus').textContent='Refreshing Guardian…';
    const [settingsResult,incidentsResult]=await Promise.all([
      window.sb.from('guardian_settings').select('*').eq('singleton',true).single(),
      window.sb.from('guardian_incidents').select('id,ticket_id,source,state,risk_level,classification,confidence,diagnosis_summary,proposed_action,auto_fix_eligible,requires_approval,github_branch,github_pr_number,last_error_code,created_at,updated_at').order('created_at',{ascending:false}).limit(100)
    ]);
    if(settingsResult.error){$('guardianStatus').textContent=settingsResult.error.message;return;}
    if(incidentsResult.error){$('guardianStatus').textContent=incidentsResult.error.message;return;}
    const s=settingsResult.data||{}, rows=incidentsResult.data||[];
    $('guardianEnabled').checked=!!s.enabled;
    $('guardianAutoFix').checked=!!s.auto_fix_enabled;
    $('guardianMaxRisk').value=s.max_auto_risk||'low';
    $('guardianHumanCode').checked=s.require_human_for_code!==false;
    $('guardianMetricOpen').textContent=rows.filter(x=>!['resolved','ignored'].includes(x.state)).length;
    $('guardianMetricApproval').textContent=rows.filter(x=>x.state==='awaiting_approval').length;
    $('guardianMetricResolved').textContent=rows.filter(x=>x.state==='resolved').length;
    $('guardianMetricFailed').textContent=rows.filter(x=>x.state==='failed').length;
    $('guardianStatus').textContent=s.enabled
      ? `ONLINE • Report-a-Problem intake active • ${s.auto_fix_enabled?'allowlisted auto-fix permission enabled':'auto-fix permission locked'}`
      : 'PAUSED • new Report-a-Problem tickets will not enter Guardian';
    $('guardianList').innerHTML=rows.map(x=>`<div class="row"><div class="row-main"><div class="row-title">${esc(label(x.classification||x.source))}</div><div class="muted">${esc(label(x.state))} • ${esc(x.risk_level.toUpperCase())} risk • ${new Date(x.created_at).toLocaleString()}${x.ticket_id?` • ticket ${esc(x.ticket_id.slice(0,8))}`:''}${x.github_pr_number?` • PR #${x.github_pr_number}`:''}</div>${x.diagnosis_summary?`<div class="muted" style="margin-top:6px">${esc(x.diagnosis_summary)}</div>`:''}${x.proposed_action?`<div class="muted" style="margin-top:6px">Proposed: ${esc(x.proposed_action)}</div>`:''}${x.last_error_code?`<div class="status">${esc(x.last_error_code)}</div>`:''}</div><span class="pill ${x.state==='resolved'?'ok':''}">${esc(label(x.state))}</span></div>`).join('')||'<div class="muted">No Guardian incidents yet. New problem reports will appear here automatically.</div>';
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