(()=>{
const q=id=>document.getElementById(id), esc2=v=>String(v??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
let currentTicket=null;
const statusLabel=s=>({open:'Open',in_progress:'In Progress',waiting_on_user:'Waiting on User',resolved:'Resolved',closed:'Closed'})[s]||s;
async function loadTickets(){
 if(!window.sb||!q('ticketsList'))return;
 const status=q('ticketStatusFilter')?.value||'all',category=q('ticketCategoryFilter')?.value||'all',term=(q('ticketSearch')?.value||'').trim().toLowerCase();
 let req=sb.from('support_tickets').select('*,profiles:user_id(display_name,email)').order('updated_at',{ascending:false}).limit(100);
 if(status!=='all')req=req.eq('status',status); if(category!=='all')req=req.eq('category',category);
 const {data,error}=await req;if(error){q('ticketsList').textContent=error.message;return}
 const rows=(data||[]).filter(t=>!term||[t.subject,t.description,t.category,t.profiles?.display_name,t.profiles?.email].some(v=>String(v||'').toLowerCase().includes(term)));
 q('ticketsList').innerHTML=rows.map(t=>`<button class="row ticket-row" data-ticket-id="${t.id}"><div class="row-main"><div class="row-title">${esc2(t.subject)}</div><div class="muted">${esc2(t.profiles?.display_name||t.profiles?.email||t.user_id)} • ${esc2(t.category)} • ${new Date(t.updated_at).toLocaleString()}</div></div><span class="pill ${t.status==='resolved'||t.status==='closed'?'ok':''}">${esc2(statusLabel(t.status))}</span></button>`).join('')||'<div class="muted">No tickets match these filters.</div>';
 document.querySelectorAll('[data-ticket-id]').forEach(b=>b.onclick=()=>openTicket(b.dataset.ticketId));
}
async function openTicket(id){
 const [{data:t,error},{data:m}]=await Promise.all([sb.from('support_tickets').select('*,profiles:user_id(display_name,email)').eq('id',id).single(),sb.from('support_ticket_messages').select('*').eq('ticket_id',id).order('created_at')]);
 if(error){alert(error.message);return} currentTicket=t;
 q('ticketDetail').classList.remove('hidden'); q('ticketDetailTitle').textContent=t.subject;
 q('ticketDetailMeta').textContent=`${t.profiles?.display_name||t.profiles?.email||t.user_id} • ${t.category} • ${new Date(t.created_at).toLocaleString()} • ${t.app_version||'unknown app'} • ${t.device_model||'unknown device'}`;
 q('ticketDescription').textContent=t.description; q('ticketStatus').value=t.status;q('ticketPriority').value=t.priority;
 q('ticketDiagnostics').textContent=t.diagnostics_opt_in?JSON.stringify(t.diagnostics||{},null,2):'Diagnostics not shared by user.';
 q('ticketMessages').innerHTML=(m||[]).map(x=>`<div class="row"><div><strong>${x.author_role==='admin'?'Admin':'User'}</strong><div>${esc2(x.body)}</div><div class="muted">${new Date(x.created_at).toLocaleString()}</div></div></div>`).join('')||'<div class="muted">No replies yet.</div>';
}
async function saveTicket(){if(!currentTicket)return;const updates={status:q('ticketStatus').value,priority:q('ticketPriority').value};const {error}=await sb.from('support_tickets').update(updates).eq('id',currentTicket.id);q('ticketAdminStatus').textContent=error?error.message:'Ticket updated.';if(!error){await openTicket(currentTicket.id);await loadTickets();if(window.loadAudit)await loadAudit()}}
async function reply(){if(!currentTicket)return;const body=q('ticketReply').value.trim();if(!body)return;const {error}=await sb.from('support_ticket_messages').insert({ticket_id:currentTicket.id,author_role:'admin',body});q('ticketAdminStatus').textContent=error?error.message:'Reply added.';if(!error){q('ticketReply').value='';if(currentTicket.status==='open')await sb.from('support_tickets').update({status:'in_progress'}).eq('id',currentTicket.id);await openTicket(currentTicket.id);await loadTickets();}}
document.addEventListener('click',e=>{const tab=e.target.closest?.('[data-tab="tickets"]');if(tab)setTimeout(loadTickets,0)});
['ticketStatusFilter','ticketCategoryFilter'].forEach(id=>q(id)?.addEventListener('change',loadTickets));q('ticketSearch')?.addEventListener('input',()=>{clearTimeout(window.__ticketSearchTimer);window.__ticketSearchTimer=setTimeout(loadTickets,200)});q('ticketRefreshBtn')?.addEventListener('click',loadTickets);q('ticketSaveBtn')?.addEventListener('click',saveTicket);q('ticketReplyBtn')?.addEventListener('click',reply);
window.loadSupportTickets=loadTickets;
})();