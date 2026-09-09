(()=>{'use strict';
const $=id=>document.getElementById(id);
const STATUS_ORDER={healthy:0,unknown:1,stale:2,degraded:3,failing:4};
const STALE_MS=15*60*1000;
const esc=s=>String(s??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
const text=id=>String($(id)?.textContent||'').trim();
const upper=id=>text(id).toUpperCase();
const now=()=>Date.now();
let refreshTimer=0;
function classifyFreshness(value,maxAgeMs=STALE_MS,at=now()){
  if(!value)return'unknown';
  const ts=typeof value==='number'?value:Date.parse(value);
  if(!Number.isFinite(ts))return'unknown';
  return at-ts>maxAgeMs?'stale':'fresh';
}
function normalizeStatus(value){
  const s=String(value||'').trim().toUpperCase();
  if(!s||['—','LOADING','UNKNOWN','UNAVAILABLE','INCOMPLETE','CHECKING','HOLD','REVIEW'].includes(s))return'unknown';
  if(['FAILED','FAILURE','BLOCKED','ERROR','CRITICAL'].includes(s))return'failing';
  if(['DEGRADED','WARNING','WARN'].includes(s))return'degraded';
  if(['STALE'].includes(s))return'stale';
  if(['HEALTHY','READY','ENFORCED','CONNECTED','PASS','SUCCESS','OK'].includes(s))return'healthy';
  return'unknown';
}
function normalizeRecord(input={}){
  const checkedAt=input.checkedAt||null;
  let status=normalizeStatus(input.status);
  const freshness=classifyFreshness(checkedAt,input.maxAgeMs||STALE_MS,input.now||now());
  if(status==='healthy'&&freshness==='stale')status='stale';
  return Object.freeze({
    id:String(input.id||'unknown'),label:String(input.label||input.id||'Unknown signal'),status,
    source:String(input.source||'runtime evidence'),checkedAt,freshness,
    details:String(input.details||''),nextAction:String(input.nextAction||''),protected:!!input.protected
  });
}
function aggregate(records=[]){
  const list=records.map(normalizeRecord);
  const worst=list.reduce((acc,r)=>STATUS_ORDER[r.status]>STATUS_ORDER[acc]?r.status:acc,'healthy');
  const counts={healthy:0,unknown:0,stale:0,degraded:0,failing:0};for(const r of list)counts[r.status]++;
  return{status:list.length?worst:'unknown',counts,records:list};
}
function latestTimestampFrom(root){
  if(!root)return null;let latest=0;
  for(const el of root.querySelectorAll('small,time')){const raw=String(el.textContent||el.getAttribute('datetime')||'');const matches=raw.match(/(?:\d{1,2}\/\d{1,2}\/\d{4}[^•\n]*|\d{4}-\d{2}-\d{2}T[^\s•]+)/g)||[];for(const m of matches){const ts=Date.parse(m);if(Number.isFinite(ts)&&ts>latest)latest=ts}}
  return latest||null;
}
function collectWorkflow(){
  const state=upper('monitorState');const root=$('monitoringList');const checkedAt=latestTimestampFrom(root);
  const details=[text('monitorWorkflows')&&`${text('monitorWorkflows')} workflows`,text('monitorFailed')&&`${text('monitorFailed')} failing`].filter(Boolean).join(' • ');
  return normalizeRecord({id:'workflows',label:'GitHub workflows',status:state,source:'Monitoring / exact workflow evidence',checkedAt,details,nextAction:normalizeStatus(state)==='failing'?'Inspect the newest failing workflow before release.':'Review current workflow evidence.'});
}
function collectRelease(){
  const state=upper('releaseOverall');const root=$('releaseEvidence');return normalizeRecord({id:'release',label:'Release readiness',status:state,source:'Release readiness / exact-main evidence',checkedAt:latestTimestampFrom(root),details:text('releaseSummary'),nextAction:text('releaseSummary'),protected:true});
}
function collectGuardian(){
  const state=upper('guardianState');const root=$('guardianEvidence');const open=text('guardianOpen');return normalizeRecord({id:'guardian',label:'Security & Guardian',status:state,source:'Guardian enforcement evidence',checkedAt:latestTimestampFrom(root),details:[open&&`${open} open repair(s)`,text('guardianBoundary')].filter(Boolean).join(' • '),nextAction:'Review Guardian evidence; any protected repair or approval remains human-controlled.',protected:true});
}
function collectCatalogue(){
  const checked=text('catalogueCheckedAt');const note=text('catalogueNote')||text('healthNote');let state='unknown';
  const signal=`${checked} ${note}`.toUpperCase();if(/HEALTHY|CONNECTED|LIVE|CURRENT/.test(signal))state='healthy';if(/DEGRADED|WARNING|STALE/.test(signal))state=/STALE/.test(signal)?'stale':'degraded';if(/FAILED|ERROR|BLOCKED|UNAVAILABLE/.test(signal))state='failing';
  return normalizeRecord({id:'catalogue',label:'Catalogue live data',status:state,source:'Catalogue runtime evidence',checkedAt:checked||null,details:note,nextAction:'Inspect live catalogue health before changing catalogue data.'});
}
function collectSupport(){
  const checked=upper('supportChecked');const signal=checked==='UNAVAILABLE'?'failing':checked&&checked!=='LOADING'?'healthy':'unknown';return normalizeRecord({id:'support',label:'Support health',status:signal,source:'Privacy-safe support aggregate',checkedAt:text('supportCheckedAt')||null,details:[text('supportSignal'),text('supportHigh')&&`${text('supportHigh')} high/urgent`].filter(Boolean).join(' • '),nextAction:'Review privacy-safe support aggregates; do not expose ticket content.'});
}
function collectMemory(){
  const state=upper('memoryState');return normalizeRecord({id:'memory',label:'Nova memory',status:state,source:'Nova memory runtime state',checkedAt:text('memoryCheckedAt')||null,details:text('memoryRetention'),nextAction:'Review memory evidence and retention boundary.'});
}
function collectRuntime(){
  const state=upper('connectionLabel');return normalizeRecord({id:'runtime',label:'Nova runtime',status:state,source:'Authenticated Nova runtime',checkedAt:null,details:text('connectionLabel'),nextAction:'Verify authenticated runtime connectivity.'});
}
function collectBackup(){
  const candidate=upper('backupHealth')||upper('backupState');return normalizeRecord({id:'backup',label:'Backup & recovery',status:candidate||'unknown',source:'Recovery health evidence',checkedAt:text('backupCheckedAt')||null,details:candidate?text('backupNote'):'No current recovery-health signal is exposed to this dashboard.',nextAction:'Validate backup evidence without executing a destructive restore.',protected:true});
}
function collect(){return[collectRuntime(),collectWorkflow(),collectRelease(),collectGuardian(),collectCatalogue(),collectSupport(),collectMemory(),collectBackup()]}
function badge(status){return status.toUpperCase()}
function tone(status){return status==='healthy'?'ok':status==='unknown'?'':'high'}
function install(){
  const page=document.querySelector('[data-page="monitoring"]');if(!page||$('novaObservability'))return;
  const card=document.createElement('article');card.className='card';card.id='novaObservability';card.innerHTML='<div class="section-head"><div><div class="eyebrow">UNIFIED HEALTH</div><h2>Operational observability</h2></div><span id="observabilityState" class="tag">LOADING</span></div><p>One read-only view across Nova runtime, workflows, release, Guardian, catalogue, support, memory and recovery evidence. Missing evidence is never treated as healthy.</p><div id="observabilityMetrics" class="metrics"></div><div id="observabilityList" class="stack"><div class="empty">Collecting current evidence…</div></div><div class="callout" id="observabilityBoundary">Observability is advisory only. It cannot deploy, restore, approve, merge, change permissions or bypass Guardian.</div>';
  page.appendChild(card);render();
}
function render(){
  if(!$('novaObservability'))return;const summary=aggregate(collect());const state=$('observabilityState');state.textContent=badge(summary.status);state.className=`tag ${tone(summary.status)}`;
  $('observabilityMetrics').innerHTML=`<article class="card metric"><span>Healthy</span><strong>${summary.counts.healthy}</strong><small>confirmed signals</small></article><article class="card metric"><span>Needs attention</span><strong>${summary.counts.failing+summary.counts.degraded}</strong><small>failing + degraded</small></article><article class="card metric"><span>Stale</span><strong>${summary.counts.stale}</strong><small>aged evidence</small></article><article class="card metric"><span>Unknown</span><strong>${summary.counts.unknown}</strong><small>missing/unverified evidence</small></article>`;
  $('observabilityList').innerHTML=summary.records.map(r=>`<div class="item"><div><b>${esc(r.label)}</b><small>${esc(r.details||'No verified detail available.')} • source: ${esc(r.source)}${r.checkedAt?` • checked ${esc(new Date(r.checkedAt).toLocaleString())}`:''}${r.protected?' • protected':''}</small></div><span class="tag ${tone(r.status)}">${esc(badge(r.status))}</span></div>`).join('');
  window.dispatchEvent(new CustomEvent('nova:observability',{detail:summary}));
}
function schedule(){clearTimeout(refreshTimer);refreshTimer=setTimeout(render,40)}
function bind(){
  const ids=['connectionLabel','monitorState','monitorWorkflows','monitorFailed','releaseOverall','releaseSummary','guardianState','guardianOpen','catalogueCheckedAt','catalogueNote','healthNote','supportChecked','supportSignal','supportHigh','memoryState','memoryRetention','backupHealth','backupState'];
  for(const id of ids){const el=$(id);if(el)new MutationObserver(schedule).observe(el,{childList:true,subtree:true,characterData:true,attributes:true})}
  document.addEventListener('click',e=>{if(e.target.closest('[data-section="monitoring"]')||e.target?.id==='refreshBtn')schedule()});window.addEventListener('nova:authenticated',schedule);
}
window.NovaObservability={STATUS_ORDER,STALE_MS,classifyFreshness,normalizeStatus,normalizeRecord,aggregate,collect,render};
window.addEventListener('DOMContentLoaded',()=>{install();bind()});
})();