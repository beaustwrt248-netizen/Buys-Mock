(()=>{
const $=id=>document.getElementById(id);
const ERROR_STATUS=/failed|error|expired|not authorised|not authorized|sign in again|no reliable|unavailable/i;

function decodeText(value){
  let text=String(value||'').trim();
  try{text=decodeURIComponent(text)}catch{}
  return text;
}

function compactModel(value){
  const text=decodeText(value);
  if(!text)return'';
  if(/^[a-z0-9-]{6,16}$/i.test(text)&&/[a-z]/i.test(text)&&/\d/.test(text))return text.toUpperCase();
  return'';
}

function extractExactModel(value){
  const text=decodeText(value).toUpperCase();
  const lenovo=text.match(/\b\d{2}[A-Z]\d{3}[A-Z0-9]{4}\b/);
  if(lenovo)return lenovo[0];
  return'';
}

function bestModel(){
  return extractExactModel($('lapModel')?.value)||
    compactModel($('lapModel')?.value)||
    extractExactModel($('lapSpecs')?.value)||'';
}

function setDisabled(el,disabled,reason='Run a successful laptop analysis first.'){
  if(!el)return;
  const next=!!disabled;
  if(el.disabled!==next)el.disabled=next;
  el.setAttribute('aria-disabled',String(next));
  if(next)el.title=reason;else if(el.title===reason)el.removeAttribute('title');
}

function laptopReady(){
  const add=$('lapAdd'),used=Number((($('lapUsed')?.textContent)||'').replace(/[^0-9.]/g,''))||0;
  const status=$('lapStatus')?.textContent||'';
  return !!add&&!add.disabled&&used>0&&!ERROR_STATUS.test(status);
}

function syncCombinedActions(){
  const ready=laptopReady();
  setDisabled($('lapSaveStock'),!ready);
  document.querySelectorAll('[data-buy-stock="laptop"]').forEach(el=>setDisabled(el,!ready));
}

function resetLaptopResult(){
  setDisabled($('lapAdd'),true);
  setDisabled($('lapSave'),true);
  setDisabled($('lapSaveStock'),true);
  document.querySelectorAll('[data-buy-stock="laptop"]').forEach(el=>setDisabled(el,true));
  const defaults={lapNew:'$0',lapUsed:'$0',lapMax:'$0',lapOffer:'$0',lapConf:'0%',lapVerdict:'—'};
  Object.entries(defaults).forEach(([id,value])=>{const el=$(id);if(el)el.textContent=value});
}

function installModelGuards(){
  const detect=$('lapDetect'),analyse=$('lapAnalyse');
  detect?.addEventListener('click',event=>{
    const model=bestModel();
    if(!model)return;
    event.preventDefault();
    event.stopImmediatePropagation();
    if($('lapModel'))$('lapModel').value=model;
    if($('lapStatus'))$('lapStatus').textContent='Detected exact model: '+model;
    resetLaptopResult();
  },true);
  analyse?.addEventListener('click',()=>{
    const model=bestModel();
    if(model&&$('lapModel'))$('lapModel').value=model;
    resetLaptopResult();
    [350,900,1800,3200,5200].forEach(ms=>setTimeout(syncCombinedActions,ms));
  },true);
  $('lapModel')?.addEventListener('blur',()=>{
    const model=bestModel();
    if(model&&$('lapModel'))$('lapModel').value=model;
  });
}

function installStatusSync(){
  const status=$('lapStatus');
  if(status)new MutationObserver(syncCombinedActions).observe(status,{subtree:true,childList:true,characterData:true});
  setInterval(syncCombinedActions,2500);
  syncCombinedActions();
}

function installMobileLayout(){
  if(document.getElementById('laptopMobileRepairStyle'))return;
  const style=document.createElement('style');
  style.id='laptopMobileRepairStyle';
  style.textContent=`
@media(max-width:760px){
  #laptop>.card:first-child{padding:14px!important}
  #laptop>.card:first-child .bar{display:grid!important;grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:8px!important}
  #laptop>.card:first-child .bar>button{width:100%!important;min-width:0!important;margin:0!important;min-height:48px!important;padding:10px 8px!important}
  #laptop>.card:first-child textarea{min-height:88px!important;max-height:150px!important}
  #laptop .conditionRow{grid-template-columns:repeat(2,minmax(0,1fr))!important;gap:8px!important;margin-top:8px!important}
  #laptop .conditionRow label{margin-top:5px!important}
}
@media(max-width:360px){
  #laptop .conditionRow{grid-template-columns:1fr!important}
}
`;
  document.head.appendChild(style);
}

function boot(){installMobileLayout();installModelGuards();installStatusSync()}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,650));else setTimeout(boot,650);
})();
