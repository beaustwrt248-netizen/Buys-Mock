(()=>{'use strict';
const EDGE='https://ghdhairijqjqivqriigi.supabase.co/functions/v1/nova-orchestrator';
const PUBLISHABLE_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
const STORE='nova_conversation_v1';
const text=v=>String(v??'').trim();
let busy=false;

function specialistIntent(raw){
  const x=text(raw).toLowerCase();
  if(!x)return true;
  return /\b(research|look up|search the web|verify (?:this|the|a|an)?\s*(?:device|model|spec)|official source|manufacturer source|release year for|model number for|market price|marketplace|ebay|gumtree|facebook marketplace|price compare|pricing intelligence|compare price|check all|audit all|scan all|verify all|deep audit|missing specs|catalogue audit|what should i do|what next|next step|priority|prioritise|recommend|needs attention|attention|blocker|blocked|release|deploy|deployment|ship|ota|apk|build|checks?|ci|workflow|ready|version|support|ticket|tickets|sla|customer issue|catalogue|catalog|device|devices|model number|model-number|storage|phone|tablet|watch|console|laptop|desktop|guardian|repair|incident|approval|protected|memory|learn|learning|knowledge|remember|context|changed|recent|activity|commit|commits|pr|pull request|working on|hello|hey|hi|who are you|what can you do|help)\b/.test(x);
}

function explicitEnsemble(raw){
  return /\b(ask (?:all|multiple) models|use (?:all|multiple) models|ensemble|second opinion|cross[- ]check with (?:gpt|gemini|claude)|compare (?:gpt|gemini|claude)|deep consensus)\b/i.test(raw);
}

function loadHistory(){try{const v=JSON.parse(sessionStorage.getItem(STORE)||'[]');return Array.isArray(v)?v.slice(-10):[]}catch{return[]}}
function saveHistory(history){try{sessionStorage.setItem(STORE,JSON.stringify(history.slice(-10)))}catch{}}
function esc=s=>String(s??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
function render(history){const log=document.querySelector('#novaConversationLog');if(!log)return;log.innerHTML=history.length?history.map(m=>`<div class="nova-conversation-message ${m.role}"><b>${m.role==='user'?'You':'Nova'}</b><div>${esc(m.text)}</div></div>`).join(''):'<div class="nova-conversation-empty">Ask Nova anything. Specialist Morley questions use live first-party tools; broader reasoning can use Nova’s guarded multi-model ensemble.</div>';log.scrollTop=log.scrollHeight}

async function ask(raw){
  const token=window.NovaAuth?.getAccessToken?.();
  if(!token)throw new Error('Nova authentication is not available.');
  const mode=explicitEnsemble(raw)?'ensemble':'auto';
  const response=await fetch(EDGE,{method:'POST',headers:{Authorization:`Bearer ${token}`,apikey:PUBLISHABLE_KEY,'Content-Type':'application/json'},body:JSON.stringify({prompt:raw,mode})});
  const data=await response.json().catch(()=>({}));
  if(!response.ok){
    if(data?.code==='OPENROUTER_API_KEY_MISSING')throw new Error('Nova multi-model intelligence is installed but its provider key has not been configured yet.');
    throw new Error(text(data?.error)||`Nova multi-model request failed (${response.status}).`);
  }
  if(!text(data?.answer))throw new Error('Nova multi-model intelligence returned no answer.');
  const used=Array.isArray(data.models_used)?data.models_used.filter(Boolean):[];
  window.dispatchEvent(new CustomEvent('nova:multi-model-response',{detail:{mode:data.mode||mode,models:used,guarded:data.guarded===true}}));
  return text(data.answer);
}

async function handle(raw,input,button){
  if(busy)return;
  busy=true;
  let history=loadHistory();
  history.push({role:'user',text:raw});
  saveHistory(history);render(history);
  if(input)input.value='';
  if(button){button.disabled=true;button.textContent=explicitEnsemble(raw)?'Consulting models…':'Thinking…'}
  try{history.push({role:'assistant',text:await ask(raw)})}
  catch(error){console.error('nova-multi-model',error);history.push({role:'assistant',text:`I could not complete the multi-model reasoning request: ${error?.message||String(error)} I will not guess.`})}
  finally{history=history.slice(-10);saveHistory(history);render(history);if(button){button.disabled=false;button.textContent='Ask Nova'}if(input)input.focus();busy=false}
}

function shouldIntercept(raw){return explicitEnsemble(raw)||!specialistIntent(raw)}

document.addEventListener('click',event=>{
  const button=event.target?.closest?.('#novaConversationSend');
  if(!button)return;
  const input=document.querySelector('#novaConversationInput'),raw=text(input?.value);
  if(!raw||!shouldIntercept(raw))return;
  event.preventDefault();event.stopImmediatePropagation();handle(raw,input,button);
},true);

document.addEventListener('keydown',event=>{
  const input=event.target?.closest?.('#novaConversationInput');
  if(!input||event.key!=='Enter')return;
  const raw=text(input.value);if(!raw||!shouldIntercept(raw))return;
  event.preventDefault();event.stopImmediatePropagation();handle(raw,input,document.querySelector('#novaConversationSend'));
},true);

window.NovaMultiModel=Object.freeze({ask,shouldIntercept,explicitEnsemble,specialistIntent});
})();
