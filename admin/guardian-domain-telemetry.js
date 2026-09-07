// Guardian direct admin-readable domain telemetry v1
(()=>{
'use strict';
const $=id=>document.getElementById(id),esc=v=>String(v??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]));
async function count(table,configure){let q=window.sb.from(table).select('id',{count:'exact',head:true});q=configure?configure(q):q;const {count,error}=await q;if(error)throw error;return Number(count||0)}
function shell(){const parent=$('guardianPlatformPanel');if(!parent||$('guardianDirectTelemetry'))return;const el=document.createElement('div');el.id='guardianDirectTelemetry';el.style.marginTop='14px';el.innerHTML='<div class="muted"><strong>Direct domain signals</strong> • loading admin-readable catalogue and support telemetry…</div>';parent.appendChild(el)}
function card(title,status,detail,bad=false){return `<div class="row" style="margin-top:8px"><div class="row-main"><div class="row-title">${esc(title)}</div><div class="muted">${esc(detail)}</div></div><span class="pill${bad?'':' ok'}">${esc(status)}</span></div>`}
async function catalogue(){const [active,missingYear,missingImage,missingModel,staleSource]=await Promise.all([
 count('device_catalog',q=>q.eq('active',true)),
 count('device_catalog',q=>q.eq('active',true).is('release_year',null)),
 count('device_catalog',q=>q.eq('active',true).is('image_reference_url',null)),
 count('device_catalog',q=>q.eq('active',true).is('model_number',null)),
 count('device_catalog',q=>q.eq('active',true).lt('source_checked_at',new Date(Date.now()-90*86400000).toISOString()))
]);const issues=missingYear+missingImage+missingModel;return {title:'Catalogue integrity',status:issues?'ATTENTION':'VERIFIED',bad:issues>0,detail:`${active} active devices • ${missingYear} missing release year • ${missingModel} missing model number • ${missingImage} missing image reference • ${staleSource} source checks older than 90 days`}}
async function support(){const now=new Date().toISOString();const [open,urgent,overdue]=await Promise.all([
 count('support_tickets',q=>q.not('status','in','(resolved,closed)')),
 count('support_tickets',q=>q.not('status','in','(resolved,closed)').in('priority',['urgent','critical','high'])),
 count('support_tickets',q=>q.not('status','in','(resolved,closed)').not('sla_due_at','is',null).lt('sla_due_at',now))
]);return {title:'Support / SLA',status:overdue||urgent?'ATTENTION':'VERIFIED',bad:overdue>0||urgent>0,detail:`${open} open tickets • ${urgent} high/urgent/critical • ${overdue} past SLA`}}
async function load(){shell();if(!window.sb||!$('guardianDirectTelemetry'))return;const host=$('guardianDirectTelemetry');const probes=await Promise.allSettled([catalogue(),support()]);host.innerHTML='<div class="muted"><strong>Direct domain signals</strong> • read-only checks use existing RLS; restricted telemetry remains unverified rather than bypassed.</div>'+probes.map((p,i)=>p.status==='fulfilled'?card(p.value.title,p.value.status,p.value.detail,p.value.bad):card(i===0?'Catalogue integrity':'Support / SLA','UNVERIFIED',`Telemetry unavailable: ${p.reason?.message||p.reason}`,true)).join('')}
window.loadGuardianDirectTelemetry=load;window.addEventListener('guardian:platform-snapshot',load);window.addEventListener('load',()=>setTimeout(load,700),{once:true});setTimeout(load,1000);
})();
