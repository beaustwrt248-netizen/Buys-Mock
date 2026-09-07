(()=>{'use strict';
const SB='https://ghdhairijqjqivqriigi.supabase.co';
const KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
let guardianPatched=false;
function patchGuardian(){
  const api=window.NovaGuardianLive;
  if(!api||guardianPatched||typeof api.load!=='function'||typeof api.isCurrent!=='function')return false;
  const loadAll=api.load.bind(api);
  api.loadAll=loadAll;
  api.load=async()=>{const rows=await loadAll();return Array.isArray(rows)?rows.filter(api.isCurrent):[]};
  guardianPatched=true;
  return true;
}
function syncCopy(){
  document.querySelectorAll('.nova-conversation-message.assistant div').forEach(el=>{
    const value=el.textContent||'';
    if(value.includes('multi-step read-only research'))el.textContent=value.replace('multi-step read-only research','multi-step guarded research');
  });
  const hint=document.querySelector('.nova-conversation-hint');
  if(hint)hint.textContent='Live web + catalogue + operations evidence · guarded review actions only · protected authority remains human-gated';
}
function statusNode(){
  const card=document.getElementById('novaConversation');
  if(!card)return null;
  let node=document.getElementById('novaGuardedActionStatus');
  if(!node){node=document.createElement('div');node.id='novaGuardedActionStatus';node.className='callout';node.hidden=true;card.appendChild(node)}
  return node;
}
function command(raw){
  const m=String(raw||'').trim().match(/^(?:nova[,: ]*)?(?:queue|add|create)\s+(?:an?\s+)?(?:catalogue\s+)?audit(?:\s+review)?\s+(?:for\s+)?(?:device\s+)?#?(\d+)(?:\s+(.+))?$/i);
  if(!m)return null;
  const deviceId=Number(m[1]);
  if(!Number.isSafeInteger(deviceId)||deviceId<=0)return null;
  return{deviceId,note:String(m[2]||'').trim().slice(0,120)};
}
async function invokeQueue(cmd){
  const token=window.NovaAuth?.getAccessToken?.()||'';
  if(!token)throw new Error('Nova Admin authentication is required.');
  const reason=cmd.note?`conversation_admin_review: ${cmd.note}`:'conversation_admin_review';
  const r=await fetch(`${SB}/functions/v1/nova-actions`,{method:'POST',headers:{apikey:KEY,Authorization:`Bearer ${token}`,'Content-Type':'application/json',Accept:'application/json'},body:JSON.stringify({action:'catalogue_queue',device_id:cmd.deviceId,priority:65,reason}),cache:'no-store'});
  let data={};try{data=await r.json()}catch{}
  if(!r.ok)throw new Error(data.error||`Nova action returned HTTP ${r.status}`);
  return data;
}
async function execute(raw,input){
  const cmd=command(raw);if(!cmd)return false;
  const node=statusNode();
  if(input)input.value='';
  if(node){node.hidden=false;node.textContent=`Queueing an auditable catalogue review for device ${cmd.deviceId}…`}
  try{
    const data=await invokeQueue(cmd);
    if(node)node.textContent=`Queued review #${data.item?.id||'created'} for device ${cmd.deviceId}. This created a review job only; no catalogue field or protected price was changed.`;
    window.NovaCatalogueAudit?.refresh?.();
  }catch(error){if(node)node.textContent=`Audit queue request failed: ${error.message||String(error)}. No catalogue change was inferred or applied.`}
  return true;
}
function captureClick(event){const button=event.target.closest?.('#novaConversationSend');if(!button)return;const input=document.getElementById('novaConversationInput'),raw=input?.value||'';if(!command(raw))return;event.preventDefault();event.stopImmediatePropagation();execute(raw,input)}
function captureKey(event){if(event.key!=='Enter'||event.target?.id!=='novaConversationInput')return;const raw=event.target.value||'';if(!command(raw))return;event.preventDefault();event.stopImmediatePropagation();execute(raw,event.target)}
function install(){patchGuardian();syncCopy();statusNode();new MutationObserver(()=>{patchGuardian();syncCopy();statusNode()}).observe(document.body,{childList:true,subtree:true});document.addEventListener('click',captureClick,true);document.addEventListener('keydown',captureKey,true)}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',install,{once:true});else install();
window.addEventListener('nova:authenticated',()=>{patchGuardian();syncCopy()});
window.NovaConversationConsistency={command,patchGuardian};
})();
