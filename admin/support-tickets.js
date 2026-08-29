(()=>{
const q=id=>document.getElementById(id), esc2=v=>String(v??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
let currentTicket=null,supportAgents=[];
const statusLabel=s=>({open:'Open',in_progress:'In Progress',waiting_on_user:'Waiting on User',resolved:'Resolved',closed:'Closed'})[s]||s;
const isClosed=t=>['resolved','closed'].includes(t?.status);
const role=()=>String(window.adminRole||'').toLowerCase();
const isStaff=()=>role()==='staff';
const canAssign=()=>['admin','manager'].includes(role());
const slaLabel=t=>{if(!t?.sla_due_at||isClosed(t))return '';const due=new Date(t.sla_due_at),ms=due-Date.now(),abs=Math.abs(ms),h=Math.max(1,Math.round(abs/36e5));return ms<0?`SLA overdue ${h}h`:`SLA due in ${h}h`};
function ensureTicketControls(){
 if(!q('ticketAssignee')){
  const priority=q('ticketPriority'),grid=priority?.closest('.grid');if(!grid)return;
  const label=document.createElement('label');label.textContent='Assigned to';
  const select=document.createElement('select');select.id='ticketAssignee';select.innerHTML='<option value="">Unassigned</option>';label.appendChild(select);grid.appendChild(label);
  const sla=document.createElement('div');sla.id='ticketSla';sla.className='muted';sla.style.margin='10px 0';grid.insertAdjacentElement('afterend',sla);
 }
 if(q('ticketAssignee'))q('ticketAssignee').disabled=!canAssign();
}
function renderAgents(){const sel=q('ticketAssignee');if(!sel)return;const selected=sel.value;sel.innerHTML='<option value="">Unassigned</option>'+supportAgents.map(p=>`<option value="${p.id}">${esc2(p.display_name||p.email||p.id)} · ${esc2(p.role)}</option>`).join('');sel.value=selected;sel.disabled=!canAssign();}
async function loadAgents(){
 if(!window.sb)return;ensureTicketControls();
 if(isStaff()){supportAgents=window.adminProfile?.id?[window.adminProfile]:[];renderAgents();return}
 const {data,error}=await sb.from('profiles').select('id,display_name,email,role,is_enabled').in('role',['admin','manager','staff']).eq('is_enabled',true).order('display_name');
 if(error){supportAgents=[];renderAgents();return}
 supportAgents=data||[];renderAgents();
}
async function loadTickets(){
 if(!window.sb||!q('ticketsList'))return;
 const status=q('ticketStatusFilter')?.value||'all',category=q('ticketCategoryFilter')?.value||'all',term=(q('ticketSearch')?.value||'').trim().toLowerCase();
 let req=sb.from('support_tickets').select('*,profiles:user_id(display_name,email)').order('updated_at',{ascending:false}).limit(100);
 if(isStaff()&&window.adminProfile?.id)req=req.eq('assigned_to',window.adminProfile.id);
 if(status!=='all')req=req.eq('status',status); if(category!=='all')req=req.eq('category',category);
 const {data,error}=await req;if(error){q('ticketsList').textContent=error.message;return}
 const rows=(data||[]).filter(t=>!term||[t.subject,t.description,t.category,t.profiles?.display_name,t.profiles?.email].some(v=>String(v||'').toLowerCase().includes(term)));
 q('ticketsList').innerHTML=rows.map(t=>{const sla=slaLabel(t),assignee=supportAgents.find(p=>p.id===t.assigned_to);return `<button class="row ticket-row" data-ticket-id="${t.id}"><div class="row-main"><div class="row-title">${esc2(t.subject)}</div><div class="muted">${esc2(t.profiles?.display_name||t.profiles?.email||t.user_id)} • ${esc2(t.category)} • ${new Date(t.updated_at).toLocaleString()}</div><div class="muted">${assignee?`Assigned: ${esc2(assignee.display_name||assignee.email||assignee.id)}`:'Assigned support agent'}${sla?` • ${esc2(sla)}`:''}</div></div><span class="pill ${t.status==='resolved'||t.status==='closed'?'ok':''}">${esc2(statusLabel(t.status))}</span></button>`}).join('')||'<div class="muted">No tickets match these filters.</div>';
 document.querySelectorAll('[data-ticket-id]').forEach(b=>b.onclick=()=>openTicket(b.dataset.ticketId));
}
async function openTicket(id){
 ensureTicketControls();
 const [{data:t,error},{data:m}]=await Promise.all([sb.from('support_tickets').select('*,profiles:user_id(display_name,email)').eq('id',id).single(),sb.from('support_ticket_messages').select('*').eq('ticket_id',id).order('created_at')]);
 if(error){alert(error.message);return} currentTicket=t;
 if(isStaff()&&t.assigned_to!==window.adminProfile?.id){currentTicket=null;alert('This ticket is not assigned to your staff account.');return}
 q('ticketDetail').classList.remove('hidden'); q('ticketDetailTitle').textContent=t.subject;
 q('ticketDetailMeta').textContent=`${t.profiles?.display_name||t.profiles?.email||t.user_id} • ${t.category} • ${new Date(t.created_at).toLocaleString()} • ${t.app_version||'unknown app'} • ${t.device_model||'unknown device'}`;
 q('ticketDescription').textContent=t.description; q('ticketStatus').value=t.status;q('ticketPriority').value=t.priority;
 q('ticketAssignee').value=t.assigned_to||'';q('ticketAssignee').disabled=!canAssign();
 const sla=q('ticketSla');if(sla){const first=t.first_response_at?`First response ${new Date(t.first_response_at).toLocaleString()}`:'No support response yet';const due=t.sla_due_at?`SLA target ${new Date(t.sla_due_at).toLocaleString()}`:'SLA target not set';sla.textContent=`${due} • ${first}${slaLabel(t)?` • ${slaLabel(t)}`:''}`;}
 q('ticketDiagnostics').textContent=t.diagnostics_opt_in?JSON.stringify(t.diagnostics||{},null,2):'Diagnostics not shared by user.';
 q('ticketMessages').innerHTML=(m||[]).map(x=>`<div class="row"><div><strong>${x.author_role==='admin'?'Support':'User'}</strong><div>${esc2(x.body)}</div><div class="muted">${new Date(x.created_at).toLocaleString()}</div></div></div>`).join('')||'<div class="muted">No replies yet.</div>';
}
async function saveTicket(){if(!currentTicket)return;ensureTicketControls();const updates={status:q('ticketStatus').value,priority:q('ticketPriority').value};if(canAssign())updates.assigned_to=q('ticketAssignee').value||null;const {error}=await sb.from('support_tickets').update(updates).eq('id',currentTicket.id);q('ticketAdminStatus').textContent=error?error.message:'Ticket updated.';if(!error){await openTicket(currentTicket.id);await loadTickets();if(!isStaff()&&window.loadAudit)await loadAudit()}}
async function reply(){if(!currentTicket)return;const body=q('ticketReply').value.trim();if(!body)return;const {error}=await sb.from('support_ticket_messages').insert({ticket_id:currentTicket.id,author_role:'admin',body});q('ticketAdminStatus').textContent=error?error.message:'Reply added.';if(!error){q('ticketReply').value='';if(currentTicket.status==='open')await sb.from('support_tickets').update({status:'in_progress'}).eq('id',currentTicket.id);await openTicket(currentTicket.id);await loadTickets();}}
ensureTicketControls();
document.addEventListener('click',e=>{const tab=e.target.closest?.('[data-tab="tickets"]');if(tab)setTimeout(async()=>{await loadAgents();await loadTickets()},0)});
['ticketStatusFilter','ticketCategoryFilter'].forEach(id=>q(id)?.addEventListener('change',loadTickets));q('ticketSearch')?.addEventListener('input',()=>{clearTimeout(window.__ticketSearchTimer);window.__ticketSearchTimer=setTimeout(loadTickets,200)});q('ticketRefreshBtn')?.addEventListener('click',async()=>{await loadAgents();await loadTickets()});q('ticketSaveBtn')?.addEventListener('click',saveTicket);q('ticketReplyBtn')?.addEventListener('click',reply);
window.loadSupportTickets=async()=>{await loadAgents();await loadTickets()};
if(window.adminRole==='staff')setTimeout(()=>window.loadSupportTickets(),0);
})();