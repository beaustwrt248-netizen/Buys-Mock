const sb=window.sb;
const $=id=>document.getElementById(id);
const esc=value=>String(value??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
const authContext=window.__morleyAdminAuthContext;
let me=authContext?.user||null;
let myProfile=authContext?.profile||null;
let config={};
let managedUsers=new Map();
let logoutInFlight=false;

function requireFullAccess(){
  if(!sb||authContext?.state!=='authenticated_ready'||!me||!myProfile||!['admin','manager'].includes(myProfile.role))throw new Error('Authorised Admin or Manager session required.');
}
function showIdentity(){
  if($('whoami'))$('whoami').textContent=myProfile?.display_name||myProfile?.email||'Admin';
  if($('roleText'))$('roleText').textContent=`${String(myProfile?.role||'').toUpperCase()} • ${myProfile?.email||''}`;
}
function status(id,message){const node=$(id);if(node)node.textContent=message;}
function rowError(id,error,fallback){const node=$(id);if(node)node.textContent=error?.message||fallback;}

async function secureUserAction(action,target,extra={}){
  requireFullAccess();
  if(!['enable','disable','set_role'].includes(action))throw new Error('That user action is not exposed by native Admin parity.');
  const targetProfile=managedUsers.get(target);
  const policy=window.AdminUserManagementPolicy?.canManageUser?.(myProfile,targetProfile,action,extra.role);
  if(!policy?.allowed){alert(policy?.reason||'This user-management action is not allowed.');return false;}
  const {data,error}=await sb.functions.invoke('admin-user-control',{body:{action,target_user_id:target,...extra}});
  if(error||!data?.ok){let message=error?.message||data?.error||'User action failed.';try{const ctx=await error?.context?.json();if(ctx?.error)message=ctx.error}catch{}alert(message);return false;}
  await Promise.all([loadUsers(),loadAudit(),loadMetrics()]);
  window.MorleyAdminUserAccessParity?.loadProfiles?.();
  return true;
}

async function loadUsers(){
  const list=$('usersList');if(!list)return;
  const {data,error}=await sb.from('profiles').select('id,email,display_name,role,is_enabled,created_at').order('created_at',{ascending:false});
  if(error){rowError('usersList',error,'Users could not be loaded.');return;}
  managedUsers=new Map((data||[]).map(profile=>[profile.id,profile]));
  window.managedUsers=managedUsers;
  list.innerHTML=(data||[]).map(profile=>{
    const adminCanManage=myProfile.role==='admin';
    const roleDisabled=!adminCanManage||profile.id===me.id;
    return `<div class="row"><div class="row-main"><div class="row-title">${esc(profile.display_name||profile.email||profile.id)}</div><div class="muted">${esc(profile.email||profile.id)} • ${profile.is_enabled?'ACTIVE':'DISABLED'}</div></div><div class="actions"><select data-role="${esc(profile.id)}" ${roleDisabled?'disabled':''}><option value="staff" ${profile.role==='staff'?'selected':''}>staff</option><option value="manager" ${profile.role==='manager'?'selected':''}>manager</option><option value="admin" ${profile.role==='admin'?'selected':''}>admin</option></select><button class="small ${profile.is_enabled?'ghost':'okbtn'}" data-user-native-action="${profile.is_enabled?'disable':'enable'}" data-user="${esc(profile.id)}" ${adminCanManage?'':'disabled'}>${profile.is_enabled?'Disable':'Enable'}</button></div></div>`;
  }).join('')||'<div class="muted">No accounts yet.</div>';
  list.querySelectorAll('[data-role]').forEach(select=>select.onchange=async()=>{select.disabled=true;try{await secureUserAction('set_role',select.dataset.role,{role:select.value});}finally{select.disabled=false;}});
  list.querySelectorAll('[data-user-native-action]').forEach(button=>button.onclick=()=>secureUserAction(button.dataset.userNativeAction,button.dataset.user));
}

async function loadDevices(){
  const list=$('devicesList');if(!list)return;
  const [deviceResult,releaseResult]=await Promise.all([
    sb.from('devices').select('*').order('last_seen_at',{ascending:false}),
    sb.from('app_config').select('value').eq('key','current_release').single()
  ]);
  if(deviceResult.error){rowError('devicesList',deviceResult.error,'Devices could not be loaded.');return;}
  const rows=deviceResult.data||[],currentVersion=releaseResult.data?.value?.versionName||'';
  const counts=new Map();for(const device of rows){const version=String(device.app_version||'').trim()||'Unknown';counts.set(version,(counts.get(version)||0)+1);}
  const currentCount=currentVersion?(counts.get(currentVersion)||0):0,unknownCount=counts.get('Unknown')||0,otherCount=Math.max(0,rows.length-currentCount-unknownCount);
  if($('releaseAdoption'))$('releaseAdoption').textContent=currentVersion?`${currentCount} of ${rows.length} registered devices on ${currentVersion} • ${otherCount} other known • ${unknownCount} unknown`:`${rows.length} registered devices • current release unavailable`;
  list.innerHTML=rows.map(device=>`<div class="row"><div class="row-main"><div class="row-title">${esc(device.device_name||device.installation_id)}</div><div class="muted">${esc(device.platform)} • ${esc(device.app_version||'unknown')} • last seen ${device.last_seen_at?new Date(device.last_seen_at).toLocaleString():'unknown'}</div></div><span class="pill ${device.notifications_enabled?'ok':''}">${device.notifications_enabled?'NOTIFY ON':'NOTIFY OFF'}</span></div>`).join('')||'<div class="muted">No devices registered yet.</div>';
}

function featureFlags(){return config.feature_flags||{};}
function renderConfig(){
  const flags=featureFlags();
  const maintenance=!!flags.maintenanceMode;
  const ota=config.admin_ota_enabled!==false;
  if($('maintenanceMode'))$('maintenanceMode').checked=maintenance;
  if($('maintenanceMessage'))$('maintenanceMessage').value=flags.maintenanceMessage||'';
  if($('otaEnabled'))$('otaEnabled').checked=ota;
  if($('featureFlags'))$('featureFlags').innerHTML=['deviceScanner','googlePricing','ebayPricing','valuationHistory'].map(key=>`<div class="row"><div class="row-main"><div class="row-title">${esc(key)}</div><div class="muted">Read-only legacy feature state</div></div><span class="pill ${flags[key]?'ok':''}">${flags[key]?'ON':'OFF'}</span></div>`).join('');
  const current=config.current_release||{},minimum=config.minimum_supported_version||{};
  if($('releaseSummary'))$('releaseSummary').innerHTML=`<div class="row"><div class="row-main"><div class="row-title">Current ${esc(current.versionName||'unavailable')}</div><div class="muted">Version code ${esc(current.versionCode??'—')}</div></div></div><div class="row"><div class="row-main"><div class="row-title">Minimum supported ${esc(minimum.versionName||'unavailable')}</div><div class="muted">Version code ${esc(minimum.versionCode??'—')} • force update ${minimum.forceUpdate?'ON':'OFF'} • read-only</div></div></div>`;
}
async function loadConfig(){
  const {data,error}=await sb.from('app_config').select('key,value').order('key');
  if(error){status('controlsStatus',error.message);return;}
  config=Object.fromEntries((data||[]).map(row=>[row.key,row.value]));
  window.config=config;
  renderConfig();
}
async function saveSafeControls(){
  requireFullAccess();
  const button=$('saveControlsBtn');if(button)button.disabled=true;status('controlsStatus','Saving native-safe controls…');
  try{
    const currentFlags={...featureFlags(),maintenanceMode:!!$('maintenanceMode')?.checked,maintenanceMessage:String($('maintenanceMessage')?.value||'').trim()};
    const otaEnabled=!!$('otaEnabled')?.checked;
    const first=await sb.rpc('admin_set_config',{config_key:'feature_flags',config_value:currentFlags});if(first.error)throw first.error;
    const second=await sb.rpc('admin_set_config',{config_key:'admin_ota_enabled',config_value:otaEnabled});if(second.error)throw second.error;
    status('controlsStatus','Safe controls saved.');await Promise.all([loadConfig(),loadAudit()]);
  }catch(error){status('controlsStatus',error?.message||'Safe controls could not be saved.');}finally{if(button)button.disabled=false;}
}

async function loadMetrics(){
  const [users,devices,queued,current,tickets,errors]=await Promise.all([
    sb.from('profiles').select('id',{count:'exact',head:true}),sb.from('devices').select('id',{count:'exact',head:true}),sb.from('notification_jobs').select('id',{count:'exact',head:true}).eq('status','queued'),sb.from('app_config').select('value').eq('key','current_release').single(),sb.from('support_tickets').select('id',{count:'exact',head:true}),sb.from('admin_error_events').select('id',{count:'exact',head:true})
  ]);
  if($('metricUsers'))$('metricUsers').textContent=users.count??0;if($('metricDevices'))$('metricDevices').textContent=devices.count??0;if($('metricQueued'))$('metricQueued').textContent=queued.count??0;if($('metricVersion'))$('metricVersion').textContent=current.data?.value?.versionName||'—';if($('metricTickets'))$('metricTickets').textContent=tickets.count??0;if($('metricErrors'))$('metricErrors').textContent=errors.count??0;
}
async function loadNotifications(){const list=$('notificationList');if(!list)return;const {data,error}=await sb.from('notification_jobs').select('*').order('created_at',{ascending:false}).limit(30);if(error){list.textContent=error.message;return;}list.innerHTML=(data||[]).map(job=>`<div class="row"><div><strong>${esc(job.title)}</strong><div class="muted">${esc(job.body)} • ${esc(job.audience)} • ${new Date(job.created_at).toLocaleString()}${job.error?` • ${esc(job.error)}`:''}</div></div><span class="pill ${job.status==='sent'?'ok':''}">${esc(job.status)}</span></div>`).join('')||'<div class="muted">No notifications yet.</div>';}
async function loadAnnouncements(){const list=$('annList');if(!list)return;const {data,error}=await sb.from('announcements').select('*').order('created_at',{ascending:false}).limit(30);if(error){list.textContent=error.message;return;}list.innerHTML=(data||[]).map(item=>`<div class="row"><div><strong>${esc(item.title)}</strong><div class="muted">${esc(item.body)} • ${esc(item.audience)} • ${item.created_at?new Date(item.created_at).toLocaleString():''}</div></div><span class="pill ${item.is_active?'ok':''}">${item.is_active?'ACTIVE':'OFF'}</span></div>`).join('')||'<div class="muted">No announcements yet.</div>';}
async function loadAudit(){if(typeof window.loadAudit==='function'&&window.loadAudit!==loadAudit){await window.loadAudit();return;}const list=$('auditList');if(!list)return;const {data,error}=await sb.from('admin_audit_log').select('created_at,action,target_type,target_id,details').order('created_at',{ascending:false}).limit(75);if(error){list.textContent=error.message;return;}list.innerHTML=(data||[]).map(item=>`<div class="row"><div><strong>${esc(item.action)}</strong><div class="muted">${new Date(item.created_at).toLocaleString()} • ${esc(item.target_type||'')} ${esc(item.target_id||'')}</div></div></div>`).join('')||'<div class="muted">No audit activity yet.</div>';}
async function refreshAll(){requireFullAccess();await Promise.all([loadUsers(),loadDevices(),loadConfig(),loadAudit(),loadMetrics(),loadNotifications(),loadAnnouncements()]);window.MorleyAdminCatalogueParity?.refresh?.();}

async function signOut(){if(logoutInFlight)return;logoutInFlight=true;const button=$('logoutBtn');if(button)button.disabled=true;try{const {error}=await sb.auth.signOut();if(error)throw error;window.__morleyAdminAuthContext=null;location.replace('./');}catch(error){if($('roleText'))$('roleText').textContent=`Sign out failed: ${error?.message||'Check your connection and try again.'}`;logoutInFlight=false;if(button)button.disabled=false;}}

requireFullAccess();showIdentity();if($('logoutBtn'))$('logoutBtn').onclick=signOut;if($('saveControlsBtn'))$('saveControlsBtn').onclick=saveSafeControls;
window.refreshAll=refreshAll;window.loadUsers=loadUsers;window.loadDevices=loadDevices;window.loadConfig=loadConfig;window.loadAudit=loadAudit;window.loadMetrics=loadMetrics;window.loadNotifications=loadNotifications;window.loadAnnouncements=loadAnnouncements;window.me=me;window.myProfile=myProfile;window.managedUsers=managedUsers;window.$=$;window.esc=esc;
refreshAll().catch(error=>{const node=$('adminLiveStatus');if(node){node.dataset.state='error';node.textContent=error?.message||'Admin data could not be loaded.';}});
