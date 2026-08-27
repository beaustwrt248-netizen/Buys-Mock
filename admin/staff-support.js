const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co';
const SUPABASE_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
const sb=window.supabase.createClient(SUPABASE_URL,SUPABASE_KEY);
const $=id=>document.getElementById(id);
const esc=v=>String(v??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
let me=null,myProfile=null,currentTicket=null;
const statusLabel=s=>({open:'Open',in_progress:'In Progress',waiting_on_user:'Waiting on User',resolved:'Resolved',closed:'Closed'})[s]||s;
const isClosed=t=>['resolved','closed'].includes(t?.status);
const slaLabel=t=>{if(!t?.sla_due_at||isClosed(t))return '';const due=new Date(t.sla_due_at),ms=due-Date.now(),h=Math.max(1,Math.round(Math.abs(ms)/36e5));return ms<0?`SLA overdue ${h}h`:`SLA due in ${h}h`};
function showLogin(){ $('loginView').classList.remove('hidden');$('appView').classList.add('hidden') }
function showApp(){ $('loginView').classList.add('hidden');$('appView').classList.remove('hidden');$('whoami').textContent=myProfile.display_name||myProfile.email||'Support staff';$('roleText').textContent=`STAFF • assigned tickets only • ${myProfile.email||''}` }
async function loadSession(){
 const {data:{session}}=await sb.auth.getSession();if(!session)return showLogin();me=session.user;
 const {data:p,error}=await sb.from('profiles').select('id,display_name,email,role,is_enabled').eq('id',me.id).single();
 if(error||!p||!p.is_enabled||p.role!=='staff'){await sb.auth.signOut();$('loginStatus').textContent=error?.message||'This workspace is restricted to enabled support staff accounts.';return showLogin()}
 myProfile=p;showApp();await loadTickets();
}
$('logoutBtn').onclick=async()=>{await sb.auth.signOut();location.reload()};
async function loadTickets(){
 const status=$('ticketStatusFilter').value,term=$('ticketSearch').value.trim().toLowerCase();
 let req=sb.from('support_tickets').select('*').order('updated_at',{ascending:false}).limit(100);
 if(status!=='all')req=req.eq('status',status);
 const {data,error}=await req;if(error){$('ticketsList').textContent=error.message;return}
 const rows=(data||[]).filter(t=>!term||[t.subject,t.description,t.category].some(v=>String(v||'').toLowerCase().includes(term)));
 $('ticketsList').innerHTML=rows.map(t=>{const sla=slaLabel(t);return `<button class="row ticket-row" data-ticket-id="${t.id}"><div class="row-main"><div class="row-title">${esc(t.subject)}</div><div class="muted">${esc(t.category)} • ${new Date(t.updated_at).toLocaleString()}${sla?` • ${esc(sla)}`:''}</div></div><span class="pill ${isClosed(t)?'ok':''}">${esc(statusLabel(t.status))}</span></button>`}).join('')||'<div class="muted">No tickets are currently assigned to you.</div>';
 document.querySelectorAll('[data-ticket-id]').forEach(b=>b.onclick=()=>openTicket(b.dataset.ticketId));
}
async function openTicket(id){
 const [{data:t,error},{data:m,error:messagesError}]=await Promise.all([
  sb.from('support_tickets').select('*').eq('id',id).single(),
  sb.from('support_ticket_messages').select('id,body,author_role,created_at,author_user_id').eq('ticket_id',id).order('created_at')
 ]);
 if(error||messagesError){$('ticketStatusText').textContent=(error||messagesError).message;return}
 currentTicket=t;$('ticketDetail').classList.remove('hidden');$('ticketDetailTitle').textContent=t.subject;
 $('ticketDetailMeta').textContent=`${t.category} • created ${new Date(t.created_at).toLocaleString()} • ${t.app_version||'unknown app'} • ${t.device_model||'unknown device'}`;
 $('ticketDescription').textContent=t.description;$('ticketStatus').value=t.status;$('ticketPriority').value=t.priority;
 const first=t.first_response_at?`First response ${new Date(t.first_response_at).toLocaleString()}`:'No support response yet';
 const due=t.sla_due_at?`SLA target ${new Date(t.sla_due_at).toLocaleString()}`:'SLA target not set';$('ticketSla').textContent=`${due} • ${first}${slaLabel(t)?` • ${slaLabel(t)}`:''}`;
 $('ticketDiagnostics').textContent=t.diagnostics_opt_in?JSON.stringify(t.diagnostics||{},null,2):'Diagnostics not shared by user.';
 $('ticketMessages').innerHTML=(m||[]).map(x=>`<div class="row"><div><strong>${x.author_role==='admin'?'Support':'User'}</strong><div>${esc(x.body)}</div><div class="muted">${new Date(x.created_at).toLocaleString()}</div></div></div>`).join('')||'<div class="muted">No replies yet.</div>';
}
async function saveTicket(){
 if(!currentTicket)return;$('ticketStatusText').textContent='Saving…';
 const updates={status:$('ticketStatus').value,priority:$('ticketPriority').value};
 const {error}=await sb.from('support_tickets').update(updates).eq('id',currentTicket.id).eq('assigned_to',me.id);
 $('ticketStatusText').textContent=error?error.message:'Ticket updated.';if(!error){await openTicket(currentTicket.id);await loadTickets()}
}
async function reply(){
 if(!currentTicket)return;const body=$('ticketReply').value.trim();if(!body)return;$('ticketStatusText').textContent='Sending…';
 const {error}=await sb.from('support_ticket_messages').insert({ticket_id:currentTicket.id,author_user_id:me.id,author_role:'admin',body});
 if(error){$('ticketStatusText').textContent=error.message;return}
 $('ticketReply').value='';if(currentTicket.status==='open')await sb.from('support_tickets').update({status:'in_progress'}).eq('id',currentTicket.id).eq('assigned_to',me.id);
 $('ticketStatusText').textContent='Reply added.';await openTicket(currentTicket.id);await loadTickets();
}
$('ticketStatusFilter').addEventListener('change',loadTickets);$('ticketSearch').addEventListener('input',()=>{clearTimeout(window.__staffTicketSearch);window.__staffTicketSearch=setTimeout(loadTickets,200)});$('ticketRefreshBtn').onclick=loadTickets;$('ticketSaveBtn').onclick=saveTicket;$('ticketReplyBtn').onclick=reply;
loadSession();
