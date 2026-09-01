(()=>{
'use strict';
const TOKENS={bg:'#080b0d',surface:'#101619',raised:'#151d20',accent:'#38d6a3',accentStrong:'#1fb887',accentSoft:'#173c32',text:'#f4f7f6',secondary:'#b2c0bc',muted:'#81918c',border:'#2b4540',good:'#63e6a6',warn:'#f5c76b',bad:'#ff7b86'};
const HELP_GENERAL_LABEL='General Buys / GP';
const $=(s,r=document)=>r.querySelector(s), $$=(s,r=document)=>[...r.querySelectorAll(s)];
function applyTokens(){
  const r=document.documentElement;
  Object.entries(TOKENS).forEach(([k,v])=>r.style.setProperty(`--morley-${k.replace(/[A-Z]/g,m=>'-'+m.toLowerCase())}`,v));
  let s=$('#morleyUltimateParityStyle');
  if(!s){s=document.createElement('style');s.id='morleyUltimateParityStyle';document.head.appendChild(s);}
  s.textContent=`:root{color-scheme:dark}.morley-ultimate-dialog{position:fixed;inset:0;z-index:2147483000;background:rgba(2,5,6,.76);backdrop-filter:blur(14px);display:grid;place-items:center;padding:18px}.morley-ultimate-card{width:min(760px,100%);max-height:88vh;overflow:auto;box-sizing:border-box;background:linear-gradient(180deg,${TOKENS.raised},${TOKENS.surface});border:1px solid ${TOKENS.border};border-radius:24px;box-shadow:0 24px 80px rgba(0,0,0,.45);padding:20px;color:${TOKENS.text}}.morley-ultimate-card h2{margin:0 44px 6px 0}.morley-ultimate-card h3{color:${TOKENS.accent};margin:22px 0 10px}.morley-ultimate-card p,.morley-ultimate-card li{color:${TOKENS.secondary};line-height:1.6}.morley-ultimate-close{position:sticky;float:right;top:0;width:42px;height:42px;border-radius:14px;border:1px solid ${TOKENS.border};background:${TOKENS.surface};color:${TOKENS.text};font-size:24px}.morley-help-section,.morley-help-faq{margin:9px 0;padding:13px 14px;border:1px solid ${TOKENS.border};border-radius:16px;background:${TOKENS.bg}}.morley-help-section b,.morley-help-faq summary{color:${TOKENS.text};font-weight:900}.morley-help-faq summary{cursor:pointer}.morley-menu-icon{color:${TOKENS.accent}!important}.morley-menu-row:focus-visible,.morley-menu-trigger:focus-visible{outline:3px solid ${TOKENS.accent}!important;outline-offset:2px}.morley-menu-row b{color:${TOKENS.text}!important}.morley-menu-row small{color:${TOKENS.secondary}!important}`;
}
const section=(t,b)=>`<section class="morley-help-section"><b>${t}</b><p>${b}</p></section>`;
const faq=(q,a)=>`<details class="morley-help-faq"><summary>${q}</summary><p>${a}</p></details>`;
function openHelp(){
  $('#morleyUltimateHelp')?.remove();
  const d=document.createElement('div');d.id='morleyUltimateHelp';d.className='morley-ultimate-dialog';d.setAttribute('role','dialog');d.setAttribute('aria-modal','true');d.setAttribute('aria-label','B&L Morley Help and FAQ');
  d.innerHTML=`<div class="morley-ultimate-card"><button class="morley-ultimate-close" aria-label="Close help">×</button><h2>Help & FAQ</h2><p>The same buying, valuation and pricing workflow used across B&L Morley Android and buyshub.me.</p><h3>How to use Morley</h3>${section('1. Choose the right pricing area','Laptop / MacBook is the guided exact-model workflow. Desktop / Gaming PC is the component-based computer workflow. General Buys / GP covers consoles and other merchandise using the appropriate grade targets.')}${section('2. Identify the item cleanly','Use the clearest model, model code, variant or hardware specification available so comparable matching stays relevant.')}${section('3. Enter Seller Ask','Seller Ask is the amount requested by the seller. Morley compares it with the protected Max Buy and does not invent it.')}${section('4. Select the correct grade','A targets 30% GP, B 50%, C 70%, and Luxury 30%. Grade changes the buying ceiling.')}${section('5. Review valuation evidence','Exact matches are strongest. Similar/spec matches are supporting evidence. Check confidence and comparable titles before buying.')}${section('6. Save and track the deal','Save useful valuations, add purchased items to Inventory and record completed sales so realised gross profit is tracked.')}${section('7. Android NFC','NFC is an Android-only hardware capability. Morley reports unavailable/disabled hardware, debounces repeated reads and supports common NDEF text/URI records. The website does not pretend to provide phone NFC.')}${section('8. Keep Morley current','Use Menu → Updates in Android for signed releases. buyshub.me updates from the production web build.')}<h3>Feature breakdown</h3>${section('Laptop / MacBook','Guided exact-model laptop and MacBook valuation with live Australian evidence.')}${section('Desktop / Gaming PC','Desktop and gaming PC valuation using component-based live pricing and protected buying targets.')}${section(HELP_GENERAL_LABEL,'A / B / C / Luxury protected buying targets for consoles and general merchandise.')}${section('Valuation engine','Model resolution, comparable quality, confidence and target-margin controls. If trustworthy evidence is insufficient, Morley should return unavailable rather than invent a value.')}${section('Quick Deal + Test & Buy','Fast protected buying guidance plus category-specific hardware checks when required.')}${section('Inventory, Scanner & Sales','Track stock, barcode/serial, cost, resale and realised gross profit.')}${section('Diagnostics, Notifications & Updates','Health/status tools plus verified update delivery and app messages.')}<h3>FAQ</h3>${faq('Why are Android and buyshub.me kept the same?','Morley is one product. Shared navigation, colours, wording, pricing workflows and business rules are kept in parity; only platform-specific capabilities differ.')}${faq('Where are Laptop and Desktop?','Laptop / MacBook and Desktop / Gaming PC are separate primary pricing areas so you can go directly to the correct valuation workflow.')}${faq('Where do I price consoles?','Use General Buys / GP and apply the appropriate item grade and buying target for the console.')}${faq('How is Max Buy calculated?','Market or expected sale value × (1 − target GP). A/Luxury use 70% of value, B 50% and C 30%.')}${faq('What do BUY, NEGOTIATE and PASS mean?','BUY is within target. NEGOTIATE means a lower ask may fit. PASS/REJECT means the ask or testing is outside the permitted buying boundary.')}${faq('Why can a valuation be unavailable?','There may not be enough trustworthy exact evidence. That is safer than manufacturing a price from weak matches.')}${faq('Does NFC work on the website?','No. NFC is kept as a real Android hardware feature rather than being falsely emulated in the web client.')}${faq('What if something looks wrong?','Check identity, grade, Seller Ask, evidence and confidence, then use Menu → Report an Issue with the model/search term and a screenshot where possible.')}
</div>`;
  document.body.appendChild(d);
  let closed=false;
  const close=()=>{if(closed)return;closed=true;d.remove();document.removeEventListener('keydown',esc);};
  const esc=e=>{if(e.key==='Escape')close();};
  $('.morley-ultimate-close',d).onclick=close;d.onclick=e=>{if(e.target===d)close();};document.addEventListener('keydown',esc);$('.morley-ultimate-close',d).focus();
}
function normaliseMenu(){
  $$('.morley-menu-row').forEach(b=>{const label=$('b',b);if(label?.textContent?.trim()==='How-to Guide & FAQ')label.textContent='Help & FAQ';});
  const help=$('.morley-menu-row[data-action="help"]');
  if(help&&!help.dataset.ultimateBound){help.dataset.ultimateBound='1';help.addEventListener('click',e=>{e.preventDefault();e.stopImmediatePropagation();openHelp();},true);}
}
let lastHealth='';
function integrity(){
  const required=['laptop','desktop','general buys / gp'];
  const scope=['#home .tiles','nav','.desktop-side-nav','#laptop','#desktop','#general'].map(s=>$(s)?.textContent||'').join(' ').toLowerCase();
  const health={theme:true,menu:!!$('#morleyMenuTrigger'),help:!!$('.morley-menu-row[data-action="help"]'),navigation:required.every(v=>scope.includes(v)),checkedAt:new Date().toISOString()};
  const signature=`${health.menu}|${health.help}|${health.navigation}`;
  window.morleyParityHealth=health;
  if(signature!==lastHealth){lastHealth=signature;window.dispatchEvent(new CustomEvent('morley-parity-health',{detail:health}));}
}
let scheduled=false;
function run(){scheduled=false;normaliseMenu();integrity();}
function schedule(){if(scheduled)return;scheduled=true;(window.requestAnimationFrame||window.setTimeout)(run);}
applyTokens();
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',run,{once:true});else run();
new MutationObserver(mutations=>{if(mutations.some(m=>m.addedNodes.length||m.removedNodes.length))schedule();}).observe(document.body||document.documentElement,{childList:true,subtree:true});
setInterval(integrity,60000);
})();
