(()=>{'use strict';
const REPO='beaustwrt248-netizen/Buys-Mock';
const API='https://api.github.com/repos/'+REPO;
const $=id=>document.getElementById(id);
const esc=s=>String(s??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
const state={main:null,prs:[],commits:[]};
function activate(name){document.querySelectorAll('.page').forEach(x=>x.classList.toggle('active',x.dataset.page===name));document.querySelectorAll('.nav button').forEach(x=>x.classList.toggle('active',x.dataset.section===name))}
document.addEventListener('click',e=>{const b=e.target.closest('[data-section]');if(b)activate(b.dataset.section)});
async function api(path){const r=await fetch(API+path,{headers:{Accept:'application/vnd.github+json'},cache:'no-store'});if(!r.ok)throw new Error(`GitHub ${r.status}`);return r.json()}
function riskFor(pr){const text=`${pr.title||''} ${(pr.body||'')}`.toLowerCase();return /auth|permission|security|pricing|supabase|migration|release|deploy|workflow|guardian|androidmanifest|signing/.test(text)?'high':'normal'}
function item(title,detail,tag='',tone=''){return `<div class="item"><div><b>${esc(title)}</b><small>${esc(detail)}</small></div>${tag?`<span class="tag ${tone}">${esc(tag)}</span>`:''}</div>`}
function render(){const novaPrs=state.prs.filter(p=>String(p.head?.ref||'').startsWith('nova/'));const attention=novaPrs.filter(p=>riskFor(p)==='high');$('metricPrs').textContent=novaPrs.length;$('metricAttention').textContent=attention.length;$('metricMain').textContent=state.main?.commit?.sha?state.main.commit.sha.slice(0,7):'—';
const queue=$('workQueue');queue.innerHTML=novaPrs.length?novaPrs.map(p=>item(`#${p.number} ${p.title}`,`${p.head.ref} • updated ${new Date(p.updated_at).toLocaleString()}`,riskFor(p)==='high'?'HIGH RISK':'ACTIVE',riskFor(p)==='high'?'high':'ok')).join(''):'<div class="empty">No open Nova pull requests.</div>';
const att=$('attentionList');att.innerHTML=attention.length?attention.map(p=>item(`#${p.number} ${p.title}`,'High-risk Nova work stays unmerged until explicit human authorisation.','REVIEW','high')).join(''):'<div class="empty">No current Nova item requires human attention.</div>';
$('developmentList').innerHTML=novaPrs.length?novaPrs.map(p=>item(`#${p.number} ${p.title}`,`${p.head.ref} → ${p.base.ref}`,p.draft?'DRAFT':'OPEN',p.draft?'':'ok')).join(''):'<div class="empty">No active Nova development PRs.</div>';
$('activityList').innerHTML=state.commits.length?state.commits.slice(0,8).map(c=>item(c.commit?.message?.split('\n')[0]||c.sha.slice(0,7),`${c.sha.slice(0,7)} • ${new Date(c.commit?.committer?.date||0).toLocaleString()}`)).join(''):'<div class="empty">No recent activity loaded.</div>';
if(attention.length){$('coreState').textContent='ATTENTION';$('coreTitle').textContent='Nova needs a review';$('coreSummary').textContent=`${attention.length} high-risk Nova pull request${attention.length===1?'':'s'} currently require human review before merge.`;$('coreOrb').style.borderColor='rgba(255,140,140,.7)'}else{$('coreState').textContent='HEALTHY';$('coreTitle').textContent='Nova is online';$('coreSummary').textContent='Standalone Nova is reading current development state. Protected write authority remains disabled in this phase.'}
}
async function refresh(){const label=$('connectionLabel');label.textContent='Connecting';try{const [main,prs,commits]=await Promise.all([api('/branches/main'),api('/pulls?state=open&per_page=50'),api('/commits?per_page=20')]);state.main=main;state.prs=prs;state.commits=commits;render();label.textContent='GitHub connected'}catch(e){console.error(e);label.textContent='Read degraded';$('coreState').textContent='DEGRADED';$('coreTitle').textContent='Nova cannot read GitHub';$('coreSummary').textContent='The standalone shell is running, but its public GitHub read is temporarily unavailable. No protected action was attempted.'}}
$('refreshBtn')?.addEventListener('click',refresh);refresh();
})();
