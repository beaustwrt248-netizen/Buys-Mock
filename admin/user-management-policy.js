(()=>{
'use strict';
const PRIVILEGED_ROLES=new Set(['admin','manager']);
const MUTATING_ACTIONS=new Set(['disable','enable','force_signout','delete','set_role','set_display_name']);
function normalizeRole(role){return String(role||'').trim().toLowerCase()}
function canManageUser(actor,target,action,nextRole){
 const actorRole=normalizeRole(actor?.role),targetRole=normalizeRole(target?.role),next=normalizeRole(nextRole);
 if(!actor?.id||!target?.id||!MUTATING_ACTIONS.has(action))return {allowed:false,reason:'Invalid user-management request.'};
 if(actorRole!=='admin')return {allowed:false,reason:'Only admins can change user accounts.'};
 if(actor.id===target.id&&['disable','force_signout','delete','set_role'].includes(action))return {allowed:false,reason:'You cannot perform that action on your own admin account.'};
 if(action==='set_role'&&!['staff','manager','admin'].includes(next))return {allowed:false,reason:'Select a valid staff role.'};
 if(action==='delete'&&PRIVILEGED_ROLES.has(targetRole))return {allowed:false,reason:'Privileged accounts must be demoted before deletion.'};
 return {allowed:true,reason:''};
}
function auditSummary(action,target,extra={}){
 const details={action,target_user_id:target?.id||null,target_role:normalizeRole(target?.role)};
 if(action==='set_role')details.new_role=normalizeRole(extra.role);
 if(action==='set_display_name')details.display_name=String(extra.display_name||'').trim();
 return details;
}
window.AdminUserManagementPolicy=Object.freeze({canManageUser,auditSummary});
})();
