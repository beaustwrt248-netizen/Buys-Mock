(()=>{
const q=id=>document.getElementById(id), esc2=v=>String(v??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
let currentTicket=null,supportAgents=[],ticketFreshnessTimer=null,lastTicketRefreshAt=null;
const statusLabel=s=>({open:'Open',in_progress:'In Progress',waiting_on_user:'Waiting on User',resolved:'Resolved',closed:'Closed'})[s]||s;
const isClosed=t=>['resolved','closed'].includes(t?.status);
const slaLabel=t=>{if(!t?.sla_due_at||isClosed(t))return '';const due=new Date(t.sla_due_at),ms=due-Date.now(),abs=Math.abs(ms),h=Math.max(1,Math.round(abs/36e5));return ms<0?`SLA overdue ${h}h`:`SLA due in ${h}h`};
function ensureQueueControls(){
 const grid=q('ticketSearch')?.closest('.grid');if(!grid)return;
 if(!q('ticketPriorityFilter')){const select=document.createElement('select');select.id='ticketPriorityFilter';select.innerHTML='<option value="all">All priorities</option><option value="low">Low</option><option value="normal">Normal</option><option value="high">High</option><option value="urgent">Urgent</option>';select.addEventListener('change',loadTickets);grid.insertBefore(select,q('ticketRefreshBtn')||null);}
 if(!q('ticketAssigneeFilter')){const select=document.createElement('select');select.id='ticketAssigneeFilter';select.innerHTML='<option value="all">All assignees</option><option value="unassigned">Unassigned</option>';select.addEventListener('change',loadTickets);grid.insertBefore(select,q('ticketRefreshBtn')||null);}
 if(!q('ticketFreshness')){const freshness=document.createElement('div');freshness.id='ticketFreshness';freshness.className='status';freshness.setAttribute('role','status');freshness.setAttribute('aria-live','polite');freshness.textContent='Support queue has not been refreshed yet.';grid.insertAdjacentElement('afterend',freshness);}
}
function updateFreshness(stale=false){const el=q('ticketFreshness');if(!el||!lastTicketRefreshAt)return;const stamp=lastTicketRefreshAt.toLocaleString();el.textContent=stale?`Data may be stale • last refreshed ${stamp}`:`Last refreshed ${stamp}`;}
function markFresh(){lastTicketRefreshAt=new Date();updateFreshness(false);clearTimeout(ticketFreshnessTimer);ticketFreshnessTimer=setTimeout(()=>updateFreshness(true),5*60*1000);}
function ensureTicketControls(){
 if(q('ticketAssignee'))return;
 const priority=q('ticketPriority'),grid=priority?.closest('.grid');if(!grid)return;
 const label=document.createElement('label');label.textContent='Assigned to';
 const select=document.createElement('select');select.id='ticketAssignee';select.innerHTML='<option value="">Unassigned</option>';label.appendChild(select);grid.appendChild(label);
 const sla=document.createElement('div');sla.id='ticketSla';sla.className='muted';sla.style.margin='10px 0';grid.insertAdjacentElement('afterend',sla);
}
async function loadAgents(){
 if(!window.sb)return;ensureQueueControls();ensureTicketControls();
 const {data,error}=await sb.from('profiles').select('id,display_name,email,role,is_enabled').in('role',['admin','manager','staff']).eq('is_enabled',true).order('display_name');
 if(error){supportAgents=[];return}
 supportAgents=data||[];
 const options=supportAgents.map(p=>`<option value="${p.id}">${esc2(p.display_name||p.email||p.id)} · ${esc2(p.role)}</option>`).join('');
 const sel=q('ticketAssignee');if(sel){const selected=sel.value;sel.innerHTML='<option value="">Unassigned</option>'+options;sel.value=selected;}
 const filter=q('ticketAssigneeFilter');if(filter){const selected=filter.value;filter.innerHTML='<option value="all">All assignees</option><option value="unassigned">Unassigned</option>'+options;filter.value=[...filter.options].some(o=>o.value===selected)?selected:'all';}
}
async function attachTicketProfiles(tickets){
 const rows=tickets||[],ids=[...new Set(rows.map(t=>t?.user_id).filter(Boolean))];
 if(!ids.length)return rows.map(t=>({...t,profiles:null}));
 const {data,error}=await sb.from('profiles').select('id,display_name,email').in('id',ids);
 if(error)return rows.map(t=>({...t,profiles:null}));
 const profilesById=new Map((data||[]).map(p=>[p.id,p]));
 return rows.map(t=>({...t,profiles:profilesById.get(t.user_id)||null}));
}
async function loadTickets(){
 if(!window.sb||!q('ticketsList'))return;ensureQueueControls();
 const status=q('ticketStatusFilter')?.value||'all',category=q('ticketCategoryFilter')?.value||'all',priority=q('ticketPriorityFilter')?.value||'all',assignee=q('ticketAssigneeFilter')?.value||'all',term=(q('ticketSearch')?.value||'').trim().toLowerCase();
 const {data,error}=await sb.from('support_tickets').select('*').order('updated_at',{ascending:false}).limit(100);
 if(error){q('ticketsList').textContent=`Support tickets could not be refreshed${error.message?`: ${error.message}`:'.'}`;return}
 const tickets=await attachTicketProfiles(data);
 markFresh();
 const rows=tickets.filter(t=>(status==='all'||t.status===status)&&(category==='all'||t.category===category)&&(priority==='all'||t.priority===priority)&&(assignee==='all'||(assignee==='unassigned'?!t.assigned_to:t.assigned_to===assignee))&&(!term||[t.subject,t.description,t.category,t.priority,t.profiles?.display_name,t.profiles?.email].some(v=>String(v||'').toLowerCase().includes(term))));
 q('ticketsList').innerHTML=rows.map(t=>{const sla=slaLabel(t),assigned=supportAgents.find(p=>p.id===t.assigned_to);return `<button class="row ticket-row" data-ticket-id="${t.id}"><div class="row-main"><div class="row-title">${esc2(t.subject)}</div><div class="muted">${esc2(t.profiles?.display_name||t.profiles?.email||t.user_id)} • ${esc2(t.category)} • ${esc2(t.priority||'normal')} • ${new Date(t.updated_at).toLocaleString()}</div><div class="muted">${assigned?`Assigned: ${esc2(assigned.display_name||assigned.email||assigned.id)}`:'Unassigned'}${sla?` • ${esc2(sla)}`:''}</div></div><span class="pill ${t.status==='resolved'||t.status==='closed'?'ok':''}">${esc2(statusLabel(t.status))}</span></button>`}).join('')||'<div class="muted">No tickets match these filters.</div>';
 document.querySelectorAll('[data-ticket-id]').forEach(b=>b.onclick=()=>openTicket(b.dataset.ticketId));
}
async function openTicket(id){
 ensureTicketControls();
 const [{data:rawTicket,error},{data:m}]=await Promise.all([sb.from('support_tickets').select('*').eq('id',id).single(),sb.from('support_ticket_messages').select('*').eq('ticket_id',id).order('created_at')]);
 if(error){alert(error.message);return}
 const [t]=await attachTicketProfiles(rawTicket?[rawTicket]:[]);if(!t){alert('Support ticket could not be loaded.');return}currentTicket=t;
 q('ticketDetail').classList.remove('hidden'); q('ticketDetailTitle').textContent=t.subject;
 q('ticketDetailMeta').textContent=`${t.profiles?.display_name||t.profiles?.email||t.user_id} • ${t.category} • ${new Date(t.created_at).toLocaleString()} • ${t.app_version||'unknown app'} • ${t.device_model||'unknown device'}`;
 q('ticketDescription').textContent=t.description; q('ticketStatus').value=t.status;q('ticketPriority').value=t.priority;
 q('ticketAssignee').value=t.assigned_to||'';
 const sla=q('ticketSla');if(sla){const first=t.first_response_at?`First response ${new Date(t.first_response_at).toLocaleString()}`:'No support response yet';const due=t.sla_due_at?`SLA target ${new Date(t.sla_due_at).toLocaleString()}`:'SLA target not set';sla.textContent=`${due} • ${first}${slaLabel(t)?` • ${slaLabel(t)}`:''}`;}
 q('ticketDiagnostics').textContent=t.diagnostics_opt_in?JSON.stringify(t.diagnostics||{},null,2):'Diagnostics not shared by user.';
 q('ticketMessages').innerHTML=(m||[]).map(x=>`<div class="row"><div><strong>${x.author_role==='admin'?'Support':'User'}</strong><div>${esc2(x.body)}</div><div class="muted">${new Date(x.created_at).toLocaleString()}</div></div></div>`).join('')||'<div class="muted">No replies yet.</div>';
}
async function saveTicket(){if(!currentTicket)return;ensureTicketControls();const updates={status:q('ticketStatus').value,priority:q('ticketPriority').value,assigned_to:q('ticketAssignee').value||null};const {error}=await sb.from('support_tickets').update(updates).eq('id',currentTicket.id);q('ticketAdminStatus').textContent=error?'Ticket update failed.':'Ticket updated.';if(!error){await openTicket(currentTicket.id);await loadTickets();if(window.loadAudit)await loadAudit()}}
async function emailErrorMessage(error){let msg=error?.message||'Email delivery failed.';try{const ctx=await error?.context?.json();if(ctx?.error)msg=ctx.error}catch{}return msg}
async function reply(){if(!currentTicket)return;const body=q('ticketReply').value.trim();if(!body)return;const {data:{user}}=await sb.auth.getUser();const actor=user?.id;if(!actor){q('ticketAdminStatus').textContent='Support session identity is unavailable.';return}const {data:message,error}=await sb.from('support_ticket_messages').insert({ticket_id:currentTicket.id,author_user_id:actor,author_role:'admin',body}).select('id').single();q('ticketAdminStatus').textContent=error?'Reply could not be sent.':'Reply added. Emailing user…';if(!error){q('ticketReply').value='';if(currentTicket.status==='open')await sb.from('support_tickets').update({status:'in_progress'}).eq('id',currentTicket.id);const {data:mail,error:mailError}=await sb.functions.invoke('send-morley-email',{body:{action:'support_ticket_reply',ticket_id:currentTicket.id,message_id:message.id}});q('ticketAdminStatus').textContent=mailError?`Reply saved, but email was not sent: ${await emailErrorMessage(mailError)}`:mail?.ok?'Reply sent and user emailed.':'Reply saved, but email delivery was not confirmed.';await openTicket(currentTicket.id);await loadTickets();}}
ensureQueueControls();ensureTicketControls();
document.addEventListener('click',e=>{const tab=e.target.closest?.('[data-tab="tickets"]');if(tab)setTimeout(async()=>{await loadAgents();await loadTickets()},0)});
['ticketStatusFilter','ticketCategoryFilter'].forEach(id=>q(id)?.addEventListener('change',loadTickets));q('ticketSearch')?.addEventListener('input',()=>{clearTimeout(window.__ticketSearchTimer);window.__ticketSearchTimer=setTimeout(loadTickets,200)});q('ticketRefreshBtn')?.addEventListener('click',async()=>{await loadAgents();await loadTickets()});q('ticketSaveBtn')?.addEventListener('click',saveTicket);q('ticketReplyBtn')?.addEventListener('click',reply);
window.loadSupportTickets=async()=>{await loadAgents();await loadTickets()};
})();