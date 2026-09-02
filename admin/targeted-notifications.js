async function refreshNotificationTargets(){
  const target=$('notifTarget'); if(!target) return;
  const [profilesRes,devicesRes]=await Promise.all([
    sb.from('profiles').select('id,email,display_name,is_enabled').order('display_name'),
    sb.from('devices').select('installation_id,device_name,user_id,notifications_enabled').order('last_seen_at',{ascending:false})
  ]);
  const options=[
    '<option value="audience:all">All users</option>',
    '<option value="audience:admin">Admins</option>',
    '<option value="audience:manager">Managers</option>',
    '<option value="audience:staff">Staff</option>'
  ];
  (profilesRes.data||[]).filter(p=>p.is_enabled).forEach(p=>{
    options.push(`<option value="user:${p.id}">User — ${esc(p.display_name||p.email||p.id)}</option>`)
  });
  (devicesRes.data||[]).filter(d=>d.notifications_enabled).forEach(d=>{
    options.push(`<option value="device:${d.installation_id}">Device — ${esc(d.device_name||d.installation_id)}</option>`)
  });
  target.innerHTML=options.join('');
}

const originalRefreshAll=refreshAll;
refreshAll=async function(){ await originalRefreshAll(); await refreshNotificationTargets(); };

async function notificationErrorMessage(error){let msg=error?.message||'Delivery failed.';try{const ctx=await error?.context?.json();if(ctx?.error)msg=ctx.error}catch{}return msg}

$('queueNotifBtn').onclick=async()=>{
  const title=$('notifTitle').value.trim(),body=$('notifBody').value.trim(),target=$('notifTarget').value;
  if(!title||!body){$('notifStatus').textContent='Enter a title and message.';return}
  let audience='all',target_user_id=null,target_installation_id=null;
  if(target.startsWith('audience:')) audience=target.split(':')[1];
  if(target.startsWith('user:')) target_user_id=target.slice(5);
  if(target.startsWith('device:')) target_installation_id=target.slice(7);
  $('notifStatus').textContent='Preparing notification…';
  const payload={title,body,audience,requested_by:me.id};
  if(target_user_id) payload.target_user_id=target_user_id;
  if(target_installation_id) payload.target_installation_id=target_installation_id;
  const {data:job,error}=await sb.from('notification_jobs').insert(payload).select('id').single();
  if(error){$('notifStatus').textContent=error.message;return}
  await sb.from('admin_audit_log').insert({actor_user_id:me.id,action:'notification_queued',target_type:target_user_id?'user':target_installation_id?'device':'audience',target_id:target_user_id||target_installation_id||audience,details:{title}});
  $('notifStatus').textContent='Sending notification…';
  const {data:delivery,error:deliveryError}=await sb.functions.invoke('send-admin-notification',{body:{job_id:job.id}});
  let pushText=deliveryError?`Push not sent: ${await notificationErrorMessage(deliveryError)}`:`Push sent to ${delivery?.sent??0} device${delivery?.sent===1?'':'s'}${delivery?.failed?` (${delivery.failed} failed)`:''}.`;
  let emailText='';
  if(!target_installation_id){
    const {data:mail,error:mailError}=await sb.functions.invoke('send-morley-email',{body:{action:'notification_job',job_id:job.id}});
    emailText=mailError?` Email not sent: ${await notificationErrorMessage(mailError)}`:mail?.ok?` Email sent to ${mail.recipients??0} recipient${mail.recipients===1?'':'s'}.`:' Email delivery was not confirmed.';
  }else emailText=' Device-only targets are push-only.';
  $('notifStatus').textContent=pushText+emailText;
  if(!deliveryError)$('notifBody').value='';
  await Promise.all([loadMetrics(),loadNotifications(),loadAudit(),loadDevices(),refreshNotificationTargets()]);
};
