(()=>{
'use strict';
const esc=v=>String(v??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
const label=s=>String(s||'').replaceAll('_',' ').replace(/\b\w/g,c=>c.toUpperCase());
document.addEventListener('click',async e=>{
  const btn=e.target.closest?.('.guardian-quarantine-actions .guardian-view-toggle');
  if(!btn||!window.sb)return;
  const key=String(btn.dataset.detail||'');
  if(!key.startsWith('q-'))return;
  const id=key.slice(2),host=document.querySelector(`[data-detail-id="${CSS.escape(key)}"]`);
  if(!host||host.dataset.loaded==='1')return;
  host.innerHTML='<div class="muted">Loading quarantined incident…</div>';
  const {data:x,error}=await window.sb.from('guardian_incidents').select('id,ticket_id,source,state,risk_level,classification,confidence,diagnosis_summary,proposed_action,reproduction_summary,test_plan,resolution_summary,last_error_code,worker_version,last_worker_at,attempt_count,created_at,updated_at').eq('id',id).single();
  if(error){host.innerHTML=`<div class="guardian-row-status error">${esc(error.message)}</div>`;return}
  const confidence=x.confidence==null?'—':`${Math.round(Number(x.confidence)*100)}%`;
  host.innerHTML=`<strong>${esc(label(x.classification||x.source||'Guardian incident'))}</strong><p>${esc(label(x.state))} • ${esc(label(x.risk_level||'unknown'))} risk • confidence ${esc(confidence)} • attempts ${Number(x.attempt_count||0)}</p>${x.worker_version?`<p>Worker: ${esc(x.worker_version)}${x.last_worker_at?` • last run ${new Date(x.last_worker_at).toLocaleString()}`:''}</p>`:''}${x.diagnosis_summary?`<p><strong>Diagnosis:</strong> ${esc(x.diagnosis_summary)}</p>`:''}${x.reproduction_summary?`<p><strong>Reproduction:</strong> ${esc(x.reproduction_summary)}</p>`:''}${x.proposed_action?`<p><strong>Proposed:</strong> ${esc(x.proposed_action)}</p>`:''}${x.test_plan?`<p><strong>Test plan:</strong> ${esc(x.test_plan)}</p>`:''}${x.resolution_summary?`<p><strong>Resolution:</strong> ${esc(x.resolution_summary)}</p>`:''}${x.last_error_code?`<div class="guardian-row-status error">${esc(x.last_error_code)}</div>`:''}`;
  host.dataset.loaded='1';
},true);
})();