(()=>{'use strict';
const q=id=>document.getElementById(id);
const context=window.__morleyAdminAuthContext;
const profile=context?.profile;
const role=profile?.role||'';
const isAdmin=role==='admin';
const canManageTeam=['admin','manager'].includes(role);
let revealedPassword='';
let resetTargetLabel='';

function cleanName(value){return String(value||'').trim().replace(/\s+/g,' ')}
function cleanEmail(value){return String(value||'').trim().toLowerCase()}
function allowedInviteRoles(){return isAdmin?['staff','manager']:['staff']}
function generatePassword(length=16){const chars='ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#%+-_';const bytes=crypto.getRandomValues(new Uint8Array(length));let out='Aa1!';for(let i=4;i<length;i++)out+=chars[bytes[i]%chars.length];return out.split('').sort(()=>crypto.getRandomValues(new Uint8Array(1))[0]-.5).join('')}
function errorMessage(error,fallback){return error?.message||fallback}
function removeNonNativeUserActions(){document.querySelectorAll('[data-user-action="force_signout"],[data-user-action="delete"]').forEach(node=>node.remove())}

async function loadProfiles(){
  const reset=q('adminResetUser');if(!reset||!window.sb)return;
  if(!isAdmin){q('adminPasswordResetCard')?.classList.add('hidden');return;}
  const current=reset.value;
  const {data,error}=await sb.from('profiles').select('id,email,display_name,role,is_enabled').order('display_name');
  if(error){q('adminResetStatus').textContent=error.message;return;}
  const rows=(data||[]).filter(x=>x.id&&x.id!==context?.user?.id);
  reset.innerHTML='<option value="">Choose an account</option>'+rows.map(x=>`<option value="${x.id}">${String(x.display_name||x.email||x.id).replace(/[<>&]/g,'')} • ${String(x.role||'user').replace(/[<>&]/g,'')}</option>`).join('');
  if(rows.some(x=>x.id===current))reset.value=current;
}

async function loadTeamInvites(){
  const list=q('invitesList');if(!list||!window.sb)return;
  if(!canManageTeam){list.innerHTML='<div class="muted">Admin or Manager access required.</div>';return;}
  const {data,error}=await sb.from('app_invites').select('id,email,display_name,role,expires_at,used_at,created_at').order('created_at',{ascending:false}).limit(100);
  if(error){list.textContent=error.message;return;}
  const allowed=new Set(allowedInviteRoles());
  list.innerHTML=(data||[]).map(invite=>{const used=!!invite.used_at,expired=!used&&invite.expires_at&&new Date(invite.expires_at)<=new Date(),state=used?'USED':expired?'EXPIRED':'ACTIVE',manageable=!used&&allowed.has(invite.role);return `<div class="row"><div class="row-main"><div class="row-title">${String(invite.display_name||invite.email||'Invite').replace(/[<>&]/g,'')}</div><div class="muted">${String(invite.email||'').replace(/[<>&]/g,'')} • ${String(invite.role||'').toUpperCase()} • ${state}${invite.expires_at?` • expires ${new Date(invite.expires_at).toLocaleString()}`:''}</div></div>${manageable?`<div class="actions"><button type="button" class="small ghost" data-team-reissue="${invite.id}">New code</button><button type="button" class="small ghost" data-team-revoke="${invite.id}">Revoke</button></div>`:''}</div>`}).join('')||'<div class="muted">No team invitations yet.</div>';
  list.querySelectorAll('[data-team-reissue]').forEach(button=>button.onclick=()=>reissueInvite(button.dataset.teamReissue));
  list.querySelectorAll('[data-team-revoke]').forEach(button=>button.onclick=()=>revokeInvite(button.dataset.teamRevoke));
}

async function reissueInvite(inviteId){
  const status=q('inviteStatus');if(status)status.textContent='Issuing a new private invite code…';
  const {data,error}=await sb.functions.invoke('send-morley-email',{body:{action:'reissue_invite',invite_id:inviteId}});
  if(error||!data?.ok){if(status)status.textContent=errorMessage(error,data?.error||'Invite could not be reissued.');return;}
  if(status)status.textContent='New private invite emailed.';await loadTeamInvites();
}
async function revokeInvite(inviteId){
  const status=q('inviteStatus');if(status)status.textContent='Revoking invite…';
  const {error}=await sb.rpc('admin_revoke_team_invite',{invite_id:inviteId});
  if(error){if(status)status.textContent=error.message;return;}
  if(status)status.textContent='Invite revoked.';await loadTeamInvites();
}

function configureTeamRole(){
  const select=q('inviteRole');if(!select)return;
  const current=select.value;select.innerHTML=allowedInviteRoles().map(value=>`<option value="${value}">${value==='staff'?'Staff':'Manager'}</option>`).join('');
  if([...select.options].some(o=>o.value===current))select.value=current;
  const help=q('teamInviteHelp');if(help)help.textContent=isAdmin?'Invite Staff or Managers, or provision an account with a temporary password.':'Managers can invite or provision Staff only. Existing account access remains Admin-controlled.';
}

async function createTeamAccess(){
  const button=q('createInviteBtn'),status=q('inviteStatus');if(!button||!status||!canManageTeam)return;
  const name=cleanName(q('inviteName')?.value),email=cleanEmail(q('inviteEmail')?.value),requestedRole=q('inviteRole')?.value||'staff',temporary=!!q('teamInviteTemporary')?.checked,password=q('teamInvitePassword')?.value||'';
  if(name.length<3||!name.includes(' ')){status.textContent='Enter the user’s first and last name.';return;}
  if(!email||!email.includes('@')){status.textContent='Enter a valid email address.';return;}
  if(!allowedInviteRoles().includes(requestedRole)){status.textContent='You are not allowed to create that role.';return;}
  if(temporary&&password.length<10){status.textContent='Temporary password must be at least 10 characters.';return;}
  button.disabled=true;status.textContent=temporary?'Creating account with temporary password…':'Creating and emailing private invite…';
  try{
    if(temporary){
      const {data,error}=await sb.functions.invoke('admin-user-control',{body:{action:'create_user',email,display_name:name,role:requestedRole,temporary_password:password,skip_email_verification:true}});
      if(error||!data?.ok||!data?.requires_password_change)throw new Error(errorMessage(error,data?.error||'Temporary-password account creation was not confirmed.'));
      revealedPassword=password;resetTargetLabel=name;q('adminTempPassword').textContent=password;q('adminResetResult').classList.remove('hidden');status.textContent='Account created. Copy the temporary password now.';
    }else{
      const {data,error}=await sb.functions.invoke('send-morley-email',{body:{action:'create_invite',email,display_name:name,role:requestedRole}});
      if(error||!data?.ok)throw new Error(errorMessage(error,data?.error||'Invite delivery was not confirmed.'));
      status.textContent=`Private invite emailed to ${email}.`;
    }
    q('inviteName').value='';q('inviteEmail').value='';if(q('teamInvitePassword'))q('teamInvitePassword').value=generatePassword();await Promise.all([loadTeamInvites(),loadProfiles()]);
  }catch(error){status.textContent=error.message||String(error)}finally{button.disabled=false;}
}

async function resetPassword(){
  const select=q('adminResetUser'),status=q('adminResetStatus'),button=q('adminResetBtn');if(!isAdmin||!select||!button)return;
  if(!select.value){status.textContent='Choose an account first.';return;}
  button.disabled=true;status.textContent='Generating secure temporary password…';q('adminResetResult')?.classList.add('hidden');
  try{
    const {data,error}=await sb.functions.invoke('admin-user-control',{body:{action:'reset_password',target_user_id:select.value}});
    if(error||!data?.ok||!data?.temporary_password)throw new Error(errorMessage(error,data?.error||'Password reset failed.'));
    revealedPassword=data.temporary_password;resetTargetLabel=select.options[select.selectedIndex]?.textContent?.split(' • ')[0]||'user';q('adminTempPassword').textContent=revealedPassword;q('adminResetResult').classList.remove('hidden');status.textContent='Temporary password created. The user must change it at next sign-in.';
  }catch(error){status.textContent=error.message||String(error)}finally{button.disabled=false;}
}
function resetMessage(){return revealedPassword?`B&L Morley temporary password for ${resetTargetLabel||'your account'}: ${revealedPassword}\n\nSign in with this password, then choose a new password.`:''}
async function copyPassword(){if(!revealedPassword)return;await navigator.clipboard?.writeText(revealedPassword);q('adminResetStatus').textContent='Temporary password copied.'}
async function sharePassword(){const text=resetMessage();if(!text)return;if(navigator.share)await navigator.share({title:'B&L Morley temporary password',text});else{await navigator.clipboard?.writeText(text);q('adminResetStatus').textContent='Message copied for sharing.'}}
function openSms(){const phone=String(q('adminResetPhone')?.value||'').replace(/[^+\d]/g,''),text=resetMessage();if(!phone||!text){q('adminResetStatus').textContent='Enter the mobile number and create a temporary password first.';return;}location.href=`sms:${encodeURIComponent(phone)}?body=${encodeURIComponent(text)}`;}

function wire(){
  configureTeamRole();
  if(!canManageTeam){q('createInviteBtn')?.setAttribute('disabled','disabled');}
  const toggle=q('teamInviteTemporary');if(toggle)toggle.onchange=()=>{q('teamInvitePasswordFields')?.classList.toggle('hidden',!toggle.checked);q('createInviteBtn').textContent=toggle.checked?'Create account with temporary password':'Email private invite';if(toggle.checked&&!q('teamInvitePassword').value)q('teamInvitePassword').value=generatePassword();};
  q('teamInviteGeneratePassword')?.addEventListener('click',()=>{q('teamInvitePassword').value=generatePassword();});
  q('createInviteBtn')?.addEventListener('click',createTeamAccess);
  q('adminResetBtn')?.addEventListener('click',resetPassword);q('adminCopyPassword')?.addEventListener('click',copyPassword);q('adminSharePassword')?.addEventListener('click',sharePassword);q('adminOpenSms')?.addEventListener('click',openSms);
  const users=q('usersList');if(users){removeNonNativeUserActions();new MutationObserver(removeNonNativeUserActions).observe(users,{childList:true,subtree:true});}
  document.querySelector('[data-workspace="users-devices"]')?.addEventListener('click',()=>{loadProfiles();loadTeamInvites();removeNonNativeUserActions();});
  loadProfiles();loadTeamInvites();removeNonNativeUserActions();
}
window.MorleyAdminUserAccessParity=Object.freeze({loadProfiles,loadTeamInvites,removeNonNativeUserActions});
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',wire,{once:true});else wire();
})();
