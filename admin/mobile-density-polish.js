(()=>{'use strict';
const $=id=>document.getElementById(id);
const activityLabels={
  guardian_incident:'Guardian incident updated',guardian_incident_updated:'Guardian incident updated',guardian_controls_updated:'Guardian safety controls updated',guardian_settings:'Guardian settings updated',guardian_settings_singleton:'Guardian settings updated',
  config_current_release:'Published OTA release updated',config_minimum_supported_version:'Minimum supported version updated',config_feature_flags:'Feature controls updated',
  notification_queued:'Notification queued',google_drive_backup_created:'Google Drive backup created',support_ticket_updated:'Support ticket updated',support_ticket_reply:'Support reply sent',pricing_updated:'Protected pricing updated',user_disabled:'Account disabled',user_enabled:'Account enabled',user_force_signout:'Account signed out',user_role_updated:'Account role updated'
};
function humaniseAction(raw){const key=String(raw||'').trim();if(!key)return'Admin activity';if(activityLabels[key])return activityLabels[key];return key.replace(/^admin_/, '').replace(/_/g,' ').replace(/\b\w/g,c=>c.toUpperCase())}
function humaniseRecent(){
  const hosts=[$('controlCentreRecent'),$('auditList')].filter(Boolean);
  hosts.forEach(host=>{
    host.querySelectorAll('.row-title,strong').forEach(el=>{
      if(el.dataset.humanisedAction)return;
      const text=(el.textContent||'').trim();
      if(!text||!/[_-]/.test(text))return;
      el.dataset.rawAction=text;
      el.textContent=humaniseAction(text);
      el.dataset.humanisedAction='true';
      el.title=`Internal event: ${text}`;
    });
    host.querySelectorAll('.muted').forEach(el=>{
      if(el.dataset.humanisedInline)return;
      let text=el.textContent||'';
      const match=text.match(/\b([a-z][a-z0-9_]{4,})\b/);
      if(match&&match[1].includes('_')){text=text.replace(match[1],humaniseAction(match[1]));el.textContent=text;el.dataset.humanisedInline='true'}
    });
  });
}
function groupExactPricingDuplicates(){
  const host=$('pricingList');if(!host)return;
  const rows=[...host.querySelectorAll('.pricing-row[data-pricing-id]')];if(!rows.length)return;
  const groups=new Map();
  rows.forEach(row=>{
    row.hidden=false;row.querySelector('.pricing-duplicate-note')?.remove();
    const title=(row.querySelector('.row-title')?.textContent||'').trim().replace(/\s+/g,' ');
    const bits=[...row.querySelectorAll('.muted')].map(x=>(x.textContent||'').trim().replace(/\s+/g,' ')).join('|');
    const price=(row.querySelector(':scope > div:last-child strong')?.textContent||'').trim();
    const key=[title,bits,price].join('||').toLowerCase();
    if(!groups.has(key))groups.set(key,[]);groups.get(key).push(row);
  });
  groups.forEach(group=>{
    if(group.length<2)return;
    const first=group[0];group.slice(1).forEach(row=>row.hidden=true);
    const note=document.createElement('span');note.className='pricing-duplicate-note';note.textContent=`${group.length} identical slots grouped`;note.title='Exact duplicate presentation rows are grouped. Distinct storage/model variants remain separate.';
    (first.querySelector('.row-main')||first).appendChild(note);
  });
}
function polish(){humaniseRecent();groupExactPricingDuplicates()}
let raf=0;function schedule(){if(raf)return;raf=requestAnimationFrame(()=>{raf=0;polish()})}
const root=$('appView')||document.body;new MutationObserver(schedule).observe(root,{subtree:true,childList:true,characterData:true});
document.addEventListener('click',e=>{if(e.target.closest?.('[data-tab="pricing"],[data-tab="overview"],[data-tab="audit"]'))setTimeout(schedule,0)});
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',schedule,{once:true});else schedule();
setTimeout(schedule,500);setTimeout(schedule,1500);
})();
