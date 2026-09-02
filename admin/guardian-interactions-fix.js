(()=>{
 const label=s=>String(s||'').replaceAll('_',' ').replace(/\b\w/g,c=>c.toUpperCase());
 let busy=false;
 function rowStatus(btn){const row=btn.closest('.row');if(!row)return null;let el=row.querySelector('.guardian-row-status');if(!el){el=document.createElement('div');el.className='guardian-row-status';const host=row.querySelector('.row-main')||row;host.appendChild(el)}return el}
 function setRowState(btn,message,kind=''){const el=rowStatus(btn);if(!el)return;el.className=`guardian-row-status${kind?` ${kind}`:''}`;el.textContent=message}
 async function decide(btn){if(busy||btn.disabled||!window.sb)return;const id=btn.dataset.id,decision=btn.dataset.decision;if(!id||!['approve','reject','retry'].includes(decision))return;busy=true;const row=btn.closest('.row');const buttons=row?.querySelectorAll('.guardian-action')||[];buttons.forEach(b=>b.disabled=true);setRowState(btn,`${label(decision)} in progress…`);
  try{
   const {error}=await window.sb.rpc('guardian_decide_incident',{incident_id:id,decision});
   if(error)throw error;
   setRowState(btn,decision==='approve'?'Approved. Guardian is preparing the protected repair candidate.':decision==='retry'?'Requeued for Guardian triage.':'Rejected. Incident moved to ignored.','ok');
   await window.loadGuardian?.();
  }catch(e){setRowState(btn,e?.message||String(e),'error');buttons.forEach(b=>b.disabled=false)}finally{busy=false}
 }
 document.addEventListener('click',e=>{const btn=e.target.closest?.('.guardian-action');if(!btn)return;e.preventDefault();e.stopImmediatePropagation();decide(btn)},true);
})();
