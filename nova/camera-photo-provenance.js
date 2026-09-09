(()=>{'use strict';
let observer=null,lastSignature='';
const esc=s=>String(s??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
function rows(){const assessment=window.NovaCameraAssistant?.getLastAssessment?.();return Array.isArray(assessment?.evidence_by_photo)?assessment.evidence_by_photo:[]}
function signature(groups){return JSON.stringify(groups.map(group=>[group?.photo_index,Array.isArray(group?.evidence)?group.evidence:[]]))}
function render(){const host=document.querySelector('#cameraVision');if(!host)return;const groups=rows(),sig=signature(groups);if(sig===lastSignature&&host.querySelector('[data-camera-photo-provenance]'))return;host.querySelectorAll('[data-camera-photo-provenance]').forEach(node=>node.remove());lastSignature=sig;if(!groups.length)return;for(const group of groups){const photo=Number(group?.photo_index),evidence=Array.isArray(group?.evidence)?group.evidence:[];if(!Number.isInteger(photo)||photo<1||photo>6)continue;for(const value of evidence){const text=String(value||'').trim();if(!text)continue;const row=document.createElement('div');row.className='item';row.dataset.cameraPhotoProvenance='1';row.innerHTML=`<div><b>Photo ${photo}</b><small>${esc(text)}</small></div><span class="tag">SOURCE</span>`;host.appendChild(row)}}}
function install(){const host=document.querySelector('#cameraVision');if(!host||observer)return;observer=new MutationObserver(()=>queueMicrotask(render));observer.observe(host,{childList:true,subtree:true});render()}
if(document.readyState==='loading')addEventListener('DOMContentLoaded',()=>setTimeout(install,0),{once:true});else setTimeout(install,0);addEventListener('nova:authenticated',()=>setTimeout(install,0));
window.NovaCameraPhotoProvenance={render};
})();
