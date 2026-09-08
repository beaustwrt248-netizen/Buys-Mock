(()=>{'use strict';
const q=(s,r=document)=>r.querySelector(s),qa=(s,r=document)=>[...r.querySelectorAll(s)];
const auditLabels={
  guardian_incident_updated:'Guardian incident updated',guardian_controls_updated:'Guardian controls updated',google_drive_backup_created:'Google Drive backup created',notification_queued:'Notification queued',release_policy_updated:'Release policy updated',pricing_updated:'Pricing updated',user_disabled:'Account disabled',user_enabled:'Account enabled',user_deleted:'Account deleted',user_role_updated:'Account role updated',support_ticket_updated:'Support ticket updated',announcement_published:'Announcement published'
};
function labelAction(raw){const key=String(raw||'').trim();if(auditLabels[key])return auditLabels[key];return key.replace(/[_-]+/g,' ').replace(/\b\w/g,c=>c.toUpperCase())||'Admin activity'}
function syncActiveTab(){const active=q('.tabs .tab.active[data-tab]');const id=active?.dataset.tab||'overview';document.body.dataset.adminActiveTab=id;const more=q('#adminMoreBtn');if(!more)return;const secondary=active?.classList.contains('admin-secondary');more.dataset.currentSecondary=secondary?'true':'false';if(secondary){const meta=active.textContent.trim().replace(/^[^A-Za-z0-9]+/,'').trim();more.innerHTML=`<span aria-hidden="true">•••</span> ${meta||'More'}`;}else more.innerHTML='<span aria-hidden="true">•••</span> More';}
function protectFeatureFlags(){qa('#featureFlags .switchrow').forEach(row=>{const input=q('input[data-flag]',row);if(!input)return;const editable=input.dataset.flag==='maintenanceMode';row.dataset.readonly=editable?'false':'true';if(!editable){input.disabled=true;input.setAttribute('aria-readonly','true')}})}
function humanizeAudit(){qa('#auditList>.row').forEach(row=>{if(row.dataset.auditHumanized)return;const strong=q('strong',row),muted=q('.muted',row);if(!strong)return;const raw=strong.textContent.trim();strong.textContent=labelAction(raw);strong.classList.add('row-title');if(raw){const technical=document.createElement('div');technical.className='admin-audit-technical';technical.textContent=`Event: ${raw}${muted?.textContent?` • ${muted.textContent}`:''}`;row.appendChild(technical);if(muted)muted.style.display='none'}row.dataset.auditHumanized='1'})}
function improveDeviceLabels(){qa('#devicesList .pill').forEach(p=>{const t=p.textContent.trim().toUpperCase();if(t==='NOTIFY ON')p.textContent='Notifications enabled';else if(t==='NOTIFY OFF')p.textContent='Notifications off'});qa('#deviceVersionAdoption .pill').forEach(p=>{if(p.textContent.trim().toUpperCase()==='OBSERVED')p.textContent='Installed version'})}
function reconcileReleaseTruth(){
  const metric=q('#metricVersion'),metricCard=metric?.closest('.metric');const metricLabel=metricCard?.querySelector('span');if(metricLabel&&/current release/i.test(metricLabel.textContent||''))metricLabel.textContent='Published OTA';
  const release=q('#releaseStatus');if(!release)return;const text=release.textContent||'';const failed=/locked|failed|unavailable|error/i.test(text);release.dataset.state=failed?'error':'ok';
  const src=q('#sourceBuildStatus');if(src)src.dataset.state=/unavailable|failed|warning/i.test(src.textContent||'')?'warning':'ok';
  if(failed){
    for(const id of ['releaseName','releaseCode','releaseSha','releaseUrl','releaseNotes']){const el=q('#'+id);if(el)el.value=''}
    if(metric)metric.textContent='Unverified';
    const summary=q('#deviceAdoptionSummary');if(summary)summary.textContent='Published OTA verification is unavailable. Installed device versions are shown below without classifying them as current or outdated.';
    qa('#deviceVersionAdoption .pill').forEach(p=>{p.textContent='Installed version';p.classList.remove('ok')});
  }
}
function run(){syncActiveTab();protectFeatureFlags();humanizeAudit();improveDeviceLabels();reconcileReleaseTruth()}
function boot(){run();qa('.tabs .tab[data-tab]').forEach(b=>b.addEventListener('click',()=>requestAnimationFrame(run)));new MutationObserver(()=>requestAnimationFrame(run)).observe(q('#appView')||document.body,{subtree:true,childList:true,characterData:true});}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',boot,{once:true});else boot();
})();
