(()=>{
  const $=id=>document.getElementById(id);
  const esc=v=>String(v??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
  const label=s=>String(s||'').replaceAll('_',' ').replace(/\b\w/g,c=>c.toUpperCase());
  const canApprove=s=>['proposed','awaiting_approval'].includes(s);
  const canRetry=s=>['failed','ignored'].includes(s);
  let currentSettings=null;

  function settingsPayload(killOverride){
    const kill=killOverride??$('guardianKillSwitch').checked;
    return {
      p_enabled:kill?false:$('guardianEnabled').checked,
      p_auto_fix_enabled:kill?false:$('guardianAutoFix').checked,
      p_max_auto_risk:$('guardianMaxRisk').value,
      p_operating_mode:$('guardianMode').value,
      p_learning_enabled:$('guardianLearning').checked,
      p_evolution_enabled:$('guardianEvolution').checked,
      p_confidence_threshold:Number($('guardianConfidence').value||0.85),
      p_max_parallel_repairs:Number($('guardianParallel').value||1),
      p_quarantine_on_repeated_failure:$('guardianQuarantine').checked,
      p_kill_switch:kill,
      p_kill_switch_reason:kill?$('guardianKillReason').value.trim():null
    };
  }

  async function setControls(payload,statusEl){
    const status=$(statusEl);
    if(status) status.textContent='Saving…';
    const {error}=await window.sb.rpc('guardian_set_controls',payload);
    if(error){if(status)status.textContent=error.message;return false;}
    if(status)status.textContent='Guardian controls saved and audited.';
    await loadGuardian();
    return true;
  }

  async function decideGuardian(incidentId,decision){
    if(!window.sb) return;
    const status=$('guardianActionStatus');
    if(status) status.textContent=`${label(decision)} in progress…`;
    const {error}=await window.sb.rpc('guardian_decide_incident',{incident_id:incidentId,decision});
    if(error){if(status)status.textContent=error.message;return;}
    if(status) status.textContent=decision==='approve'
      ? 'Approval recorded. Guardian may advance only through the guarded repair pipeline; direct merge or deploy remains prohibited.'
      : decision==='retry'
        ? 'Incident requeued for immediate Guardian triage.'
        : 'Incident rejected and moved to ignored.';
    await loadGuardian();
  }

  async function loadAudit(){
    if(!window.sb||!$('guardianAuditList')) return;
    const {data,error}=await window.sb.from('admin_audit_log').select('actor_user_id,action,target_type,target_id,details,created_at').order('created_at',{ascending:false}).limit(200);
    if(error){$('guardianAuditList').textContent=error.message;return;}
    const rows=(data||[]).filter(x=>String(x.action||'').startsWith('guardian_')||String(x.target_type||'').startsWith('guardian'));
    $('guardianAuditList').innerHTML=rows.map(x=>`<div class="row"><div class="row-main"><div class="row-title">${esc(label(x.action))}</div><div class="muted">${new Date(x.created_at).toLocaleString()} • ${esc(x.target_type||'guardian')} ${esc(x.target_id||'')}</div>${x.details?`<div class="muted" style="margin-top:6px">${esc(JSON.stringify(x.details))}</div>`:''}</div></div>`).join('')||'<div class="muted">No Guardian audit activity yet.</div>';
  }

  async function loadGuardian(){
    if(!window.sb||!$('guardianStatus')) return;
    $('guardianStatus').textContent='Refreshing Guardian…';
    const [settingsResult,incidentsResult]=await Promise.all([
      window.sb.from('guardian_settings').select('*').eq('singleton',true).single(),
      window.sb.from('guardian_incidents').select('id,ticket_id,source,state,risk_level,classification,confidence,diagnosis_summary,proposed_action,auto_fix_eligible,requires_approval,github_branch,github_pr_number,last_error_code,worker_version,reproduction_summary,test_plan,resolution_summary,last_worker_at,attempt_count,created_at,updated_at').order('created_at',{ascending:false}).limit(100)
    ]);
    if(settingsResult.error){$('guardianStatus').textContent=settingsResult.error.message;return;}
    if(incidentsResult.error){$('guardianStatus').textContent=incidentsResult.error.message;return;}
    const s=settingsResult.data||{}, rows=incidentsResult.data||[];
    currentSettings=s;
    $('guardianEnabled').checked=!!s.enabled;
    $('guardianAutoFix').checked=!!s.auto_fix_enabled;
    $('guardianMaxRisk').value=s.max_auto_risk||'low';
    $('guardianMode').value=s.operating_mode||'observe';
    $('guardianLearning').checked=s.learning_enabled!==false;
    $('guardianEvolution').checked=!!s.evolution_enabled;
    $('guardianConfidence').value=Number(s.confidence_threshold??0.85).toFixed(2);
    $('guardianParallel').value=s.max_parallel_repairs||1;
    $('guardianQuarantine').checked=s.quarantine_on_repeated_failure!==false;
    $('guardianHumanCode').checked=true;
    $('guardianKillSwitch').checked=!!s.kill_switch;
    $('guardianKillReason').value=s.kill_switch_reason||'';
    $('guardianMetricOpen').textContent=rows.filter(x=>!['resolved','ignored'].includes(x.state)).length;
    $('guardianMetricApproval').textContent=rows.filter(x=>canApprove(x.state)).length;
    $('guardianMetricResolved').textContent=rows.filter(x=>x.state==='resolved').length;
    $('guardianMetricFailed').textContent=rows.filter(x=>x.state==='failed').length;
    $('guardianStatus').textContent=s.kill_switch
      ? `KILL SWITCH ENGAGED • Guardian stopped${s.kill_switch_reason?` • ${s.kill_switch_reason}`:''}`
      : s.enabled
        ? `ONLINE • ${label(s.operating_mode||'observe')} mode • ${s.learning_enabled!==false?'learning on':'learning off'} • ${s.evolution_enabled?'evolution proposals on':'evolution proposals off'}`
        : 'PAUSED • Guardian intake disabled';
    $('guardianList').innerHTML=rows.map(x=>{
      const confidence=x.confidence==null?'—':`${Math.round(Number(x.confidence)*100)}%`;
      const actions=[
        canApprove(x.state)?`<button class="guardian-action" data-id="${esc(x.id)}" data-decision="approve">Approve</button>`:'',
        canApprove(x.state)?`<button class="guardian-action ghost" data-id="${esc(x.id)}" data-decision="reject">Reject</button>`:'',
        canRetry(x.state)?`<button class="guardian-action ghost" data-id="${esc(x.id)}" data-decision="retry">Retry</button>`:''
      ].filter(Boolean).join('');
      return `<div class="row"><div class="row-main"><div class="row-title">${esc(label(x.classification||x.source))}</div><div class="muted">${esc(label(x.state))} • ${esc(String(x.risk_level||'unknown').toUpperCase())} risk • confidence ${esc(confidence)} • attempts ${Number(x.attempt_count||0)} • ${new Date(x.created_at).toLocaleString()}${x.ticket_id?` • ticket ${esc(x.ticket_id.slice(0,8))}`:''}${x.github_pr_number?` • PR #${x.github_pr_number}`:''}</div>${x.worker_version?`<div class="muted" style="margin-top:6px">Worker: ${esc(x.worker_version)}${x.last_worker_at?` • last run ${new Date(x.last_worker_at).toLocaleString()}`:''}</div>`:''}${x.diagnosis_summary?`<div class="muted" style="margin-top:6px"><strong>Diagnosis:</strong> ${esc(x.diagnosis_summary)}</div>`:''}${x.reproduction_summary?`<div class="muted" style="margin-top:6px"><strong>Reproduction:</strong> ${esc(x.reproduction_summary)}</div>`:''}${x.proposed_action?`<div class="muted" style="margin-top:6px"><strong>Proposed:</strong> ${esc(x.proposed_action)}</div>`:''}${x.test_plan?`<div class="muted" style="margin-top:6px"><strong>Test plan:</strong> ${esc(x.test_plan)}</div>`:''}${x.resolution_summary?`<div class="muted" style="margin-top:6px"><strong>Resolution:</strong> ${esc(x.resolution_summary)}</div>`:''}${x.last_error_code?`<div class="status">${esc(x.last_error_code)}</div>`:''}${actions?`<div class="actions" style="margin-top:10px">${actions}</div>`:''}</div><span class="pill ${x.state==='resolved'?'ok':''}">${esc(label(x.state))}</span></div>`;
    }).join('')||'<div class="muted">No Guardian incidents yet. New problem reports will appear here automatically.</div>';
    document.querySelectorAll('.guardian-action').forEach(btn=>btn.addEventListener('click',()=>decideGuardian(btn.dataset.id,btn.dataset.decision)));
    await loadAudit();
  }

  async function saveGuardian(){
    if(!window.sb) return;
    if(currentSettings?.kill_switch){$('guardianSaveStatus').textContent='Disengage the kill switch first before changing active operating controls.';return;}
    await setControls(settingsPayload(false),'guardianSaveStatus');
  }

  async function applyKillSwitch(){
    if(!window.sb) return;
    const engaging=$('guardianKillSwitch').checked;
    if(engaging&&!$('guardianKillReason').value.trim()){$('guardianKillStatus').textContent='Enter a reason before engaging the kill switch.';return;}
    if(engaging&&!confirm('Engage Guardian kill switch now? New intake and automatic repairs will stop immediately.')){$('guardianKillSwitch').checked=!!currentSettings?.kill_switch;return;}
    if(!engaging&&currentSettings?.kill_switch&&!confirm('Disengage Guardian kill switch and restore the saved operating controls?')){$('guardianKillSwitch').checked=true;return;}
    const payload=settingsPayload(engaging);
    if(!engaging){payload.p_enabled=false;payload.p_auto_fix_enabled=false;}
    const ok=await setControls(payload,'guardianKillStatus');
    if(ok)$('guardianKillStatus').textContent=engaging?'Kill switch engaged. Guardian intake and automatic repairs are stopped.':'Kill switch disengaged. Guardian remains paused until operating controls are explicitly saved.';
  }

  $('guardianSaveBtn')?.addEventListener('click',saveGuardian);
  $('guardianKillBtn')?.addEventListener('click',applyKillSwitch);
  $('guardianRefreshBtn')?.addEventListener('click',loadGuardian);
  $('guardianAuditRefreshBtn')?.addEventListener('click',loadAudit);
  window.loadGuardian=loadGuardian;
  window.sb?.auth?.onAuthStateChange?.((event,session)=>{if(session)setTimeout(loadGuardian,0)});
})();