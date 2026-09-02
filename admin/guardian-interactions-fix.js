// Guardian repair dispatch recovery v3
(()=>{
 const label=s=>String(s||'').replaceAll('_',' ').replace(/\b\w/g,c=>c.toUpperCase());
 let busy=false,recovering=false;
 const dispatching=new Set();
 function rowStatus(btn){const row=btn.closest('.row,.guardian-history-detail,.guardian-quarantine-item')||btn.parentElement;if(!row)return null;let el=row.querySelector('.guardian-row-status');if(!el){el=document.createElement('div');el.className='guardian-row-status';const host=row.querySelector('.row-main,.guardian-history-actions')||row;host.appendChild(el)}return el}
 function setRowState(btn,message,kind=''){const el=rowStatus(btn);if(!el)return;el.className=`guardian-row-status${kind?` ${kind}`:''}`;el.textContent=message}
 async function dispatchRepair(incidentId){if(!window.sb||dispatching.has(incidentId))return;dispatching.add(incidentId);try{const {data,error}=await window.sb.functions.invoke('guardian-repair-worker',{body:{incident_id:incidentId}});if(error)throw error;if(data?.error)throw new Error(data.detail||data.error);return data}finally{dispatching.delete(incidentId)}}
 async function decide(btn){if(busy||btn.disabled||!window.sb)return;const id=btn.dataset.id,decision=btn.dataset.decision;if(!id||!['approve','reject','retry'].includes(decision))return;busy=true;const row=btn.closest('.row,.guardian-history-detail,.guardian-quarantine-item');const buttons=row?.querySelectorAll('.guardian-action')||[];buttons.forEach(b=>b.disabled=true);setRowState(btn,`${label(decision)} in progress…`);
  try{
   const {error}=await window.sb.rpc('guardian_decide_incident',{incident_id:id,decision});
   if(error)throw error;
   if(decision==='approve'){
    setRowState(btn,'Approved. Dispatching Guardian candidate generation…');
    await dispatchRepair(id);
    setRowState(btn,'Candidate generation dispatched. Guardian will return this incident for review when the protected patch is ready.','ok');
   }else setRowState(btn,decision==='retry'?'Requeued for Guardian triage.':'Rejected. Incident moved to ignored.','ok');
   await window.loadGuardian?.();
  }catch(e){setRowState(btn,e?.message||String(e),'error');buttons.forEach(b=>b.disabled=false);await window.loadGuardian?.()}finally{busy=false}
 }
 async function recoverRequestedRepairs(){if(recovering||!window.sb)return;recovering=true;try{const {data:{session}}=await window.sb.auth.getSession();if(!session)return;const {data,error}=await window.sb.from('guardian_repairs').select('incident_id,status,requested_at').eq('status','requested').order('requested_at',{ascending:true}).limit(10);if(error)throw error;for(const repair of data||[]){try{await dispatchRepair(repair.incident_id)}catch(e){console.error('Guardian repair recovery failed',repair.incident_id,e)}}if((data||[]).length)await window.loadGuardian?.()}catch(e){console.error('Guardian repair recovery scan failed',e)}finally{recovering=false}}
 document.addEventListener('click',e=>{const btn=e.target.closest?.('.guardian-action');if(!btn)return;e.preventDefault();e.stopImmediatePropagation();decide(btn)},true);
 window.recoverGuardianRepairs=recoverRequestedRepairs;
 window.addEventListener('load',()=>setTimeout(recoverRequestedRepairs,1200),{once:true});
 setInterval(recoverRequestedRepairs,60000);
})();
