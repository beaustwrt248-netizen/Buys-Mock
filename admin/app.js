const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co';
const SUPABASE_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
const sb=window.supabase.createClient(SUPABASE_URL,SUPABASE_KEY);

const $=id=>document.getElementById(id);
const esc=v=>String(v??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
let me=null,myProfile=null;

async function loadSession(){const {data:{session}}=await sb.auth.getSession();if(!session)return showLogin();me=session.user;const {data:p,error}=await sb.from('profiles').select('*').eq('id',me.id).single();if(error||!p||!p.is_enabled||!['admin','manager'].includes(p.role)){await sb.auth.signOut();$('loginStatus').textContent='This account is not authorised for Admin Control.';return showLogin();}myProfile=p;showApp();await refreshAll();}
function showLogin(){$('loginView').classList.remove('hidden');$('appView').classList.add('hidden')}
function showApp(){$('loginView').classList.add('hidden');$('appView').classList.remove('hidden');$('whoami').textContent=myProfile.display_name||myProfile.email||'Admin';$('roleText').textContent=`${myProfile.role.toUpperCase()} • ${myProfile.email||''}`}

$('loginBtn').onclick=async()=>{const email=$('email').value.trim(),password=$('password').value;$('loginStatus').textContent='Signing in…';const {error}=await sb.auth.signInWithPassword({email,password});if(error){$('loginStatus').textContent=error.message;return}await loadSession()};
$('logoutBtn').onclick=async()=>{await sb.auth.signOut();location.reload()};

document.querySelectorAll('.tab').forEach(b=>b.onclick=()=>{document.querySelectorAll('.tab').forEach(x=>x.classList.remove('active'));b.classList.add('active');document.querySelectorAll('.panel').forEach(x=>x.classList.add('hidden'));$('tab-'+b.dataset.tab).classList.remove('hidden')});

async function refreshAll(){await Promise.all([loadUsers(),loadDevices(),loadConfig(),loadAudit(),loadMetrics()])}
async function loadMetrics(){const [u,d,n,c]=await Promise.all([sb.from('profiles').select('id',{count:'exact',head:true}),sb.from('devices').select('id',{count:'exact',head:true}),sb.from('notification_jobs').select('id',{count:'exact',head:true}).eq('status','queued'),sb.from('app_config').select('value').eq('key','current_release').single()]);$('metricUsers').textContent=u.count??0;$('metricDevices').textContent=d.count??0;$('metricQueued').textContent=n.count??0;$('metricVersion').textContent=c.data?.value?.versionName||'—'}

async function loadUsers(){const {data,error}=await sb.from('profiles').select('*').order('created_at',{ascending:false});if(error){$('usersList').textContent=error.message;return}$('usersList').innerHTML=(data||[]).map(p=>`<div class="row"><div class="row-main"><div class="row-title">${esc(p.display_name||p.email||p.id)}</div><div class="muted">${esc(p.email||p.id)} • ${esc(p.role)}</div></div><span class="pill ${p.is_enabled?'ok':'danger'}">${p.is_enabled?'ENABLED':'DISABLED'}</span></div>`).join('')||'<div class="muted">No accounts yet.</div>'}
async function loadDevices(){const {data,error}=await sb.from('devices').select('*').order('last_seen_at',{ascending:false});if(error){$('devicesList').textContent=error.message;return}$('devicesList').innerHTML=(data||[]).map(d=>`<div class="row"><div class="row-main"><div class="row-title">${esc(d.device_name||d.installation_id)}</div><div class="muted">${esc(d.platform)} • ${esc(d.app_version||'unknown version')} • last seen ${new Date(d.last_seen_at).toLocaleString()}</div></div><span class="pill">${d.notifications_enabled?'NOTIFY ON':'NOTIFY OFF'}</span></div>`).join('')||'<div class="muted">No devices registered yet.</div>'}
async function loadConfig(){const {data,error}=await sb.from('app_config').select('*').order('key');if(error){$('configList').textContent=error.message;return}$('configList').innerHTML=(data||[]).map(c=>`<div class="row"><div class="row-main"><div class="row-title">${esc(c.key)}</div><div class="muted">${esc(JSON.stringify(c.value))}</div></div></div>`).join('')}
async function loadAudit(){const {data,error}=await sb.from('admin_audit_log').select('*').order('created_at',{ascending:false}).limit(50);if(error){$('auditList').textContent=error.message;return}$('auditList').innerHTML=(data||[]).map(a=>`<div class="row"><div class="row-main"><div class="row-title">${esc(a.action)}</div><div class="muted">${new Date(a.created_at).toLocaleString()} • ${esc(a.target_type||'')} ${esc(a.target_id||'')}</div></div></div>`).join('')||'<div class="muted">No audit activity yet.</div>'}

$('queueNotifBtn').onclick=async()=>{const title=$('notifTitle').value.trim(),body=$('notifBody').value.trim(),audience=$('notifAudience').value;if(!title||!body){$('notifStatus').textContent='Enter a title and message.';return}$('notifStatus').textContent='Queueing…';const {error}=await sb.from('notification_jobs').insert({title,body,audience,requested_by:me.id});if(error){$('notifStatus').textContent=error.message;return}await sb.from('admin_audit_log').insert({actor_user_id:me.id,action:'notification_queued',target_type:'audience',target_id:audience,details:{title}});$('notifStatus').textContent='Notification queued. FCM delivery worker is the next integration step.';$('notifBody').value='';await Promise.all([loadMetrics(),loadAudit()])};

loadSession();