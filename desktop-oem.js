(()=>{
const $=id=>document.getElementById(id);
const money=n=>new Intl.NumberFormat('en-AU',{style:'currency',currency:'AUD',maximumFractionDigits:0}).format(Number(n)||0);
let oemState=null;
function cleanText(s){return String(s||'').replace(/\s+/g,' ').trim()}
function detectOEM(text){const t=cleanText(text);const patterns=[
/\b(?:Lenovo\s+)?ThinkCentre\s+[A-Z0-9-]+(?:\s+Gen\s+\d+)?/i,
/\b(?:Lenovo\s+)?ThinkStation\s+[A-Z0-9-]+(?:\s+Gen\s+\d+)?/i,
/\b(?:Lenovo\s+)?Legion\s+(?:Tower|T)\s*[A-Z0-9-]*/i,
/\b(?:Dell\s+)?OptiPlex\s+[A-Z0-9-]+/i,
/\b(?:Dell\s+)?Precision\s+[A-Z0-9-]+/i,
/\b(?:Dell\s+)?XPS\s+(?:Desktop\s*)?[A-Z0-9-]+/i,
/\bAlienware\s+Aurora\s+R?\d+/i,
/\b(?:HP\s+)?(?:EliteDesk|ProDesk)\s+[A-Z0-9 -]+?(?=\s{2,}|,|;|$)/i,
/\b(?:HP\s+)?OMEN\s+(?:Desktop\s*)?[A-Z0-9-]+/i,
/\b(?:Acer\s+)?Predator\s+Orion\s+[A-Z0-9-]+/i,
/\b(?:Acer\s+)?Veriton\s+[A-Z0-9-]+/i,
/\b(?:ASUS\s+)?ROG\s+(?:G|Strix|Desktop)[A-Z0-9-]*/i,
/\b(?:MSI\s+)?(?:Aegis|Infinite|Trident|Codex)\s+[A-Z0-9-]+/i
];
let model='';for(const r of patterns){const m=t.match(r);if(m){model=m[0];break}}
if(!model)return null;
const cpu=t.match(/\b(?:Intel\s+)?Core\s+Ultra\s+[3579]\s+\d{3}[A-Z]{0,2}\b/i)?.[0]
||t.match(/\b(?:Intel\s+)?(?:Core\s+)?i[3579][ -]?\d{4,5}(?:K|KF|F|KS|T)?\b/i)?.[0]
||t.match(/\b(?:AMD\s+)?Ryzen\s+[3579]\s+\d{4,5}(?:X3D|X|G|GE)?\b/i)?.[0]||'';
const gpu=t.match(/\b(?:RTX|GTX)\s*\d{3,4}(?:\s*(?:Ti|SUPER))?\b/i)?.[0]
||t.match(/\bRX\s*\d{3,4}(?:\s*(?:XT|XTX|GRE))?\b/i)?.[0]||'';
const ram=t.match(/\b(?:8|12|16|24|32|48|64|96|128)\s*GB\s*(?:DDR[345])?\b/i)?.[0]||'';
const storage=t.match(/\b(?:128|256|512)\s*GB\b|\b(?:1|2|4|8)\s*TB\b/i)?.[0]||'';
return{model:cleanText(model),cpu:cleanText(cpu),gpu:cleanText(gpu),ram:cleanText(ram),storage:cleanText(storage)}
}
function queryFor(o){return[o.model,o.cpu,o.gpu,o.ram,o.storage].filter(Boolean).join(' ')}
function setStats(newP,used){const margin=Number($('deskMargin')?.value)||.30,ask=Number($('deskAsk')?.value)||0,max=used*(1-margin);if($('deskNew'))$('deskNew').textContent=money(newP);if($('deskUsed'))$('deskUsed').textContent=money(used);if($('deskMax'))$('deskMax').textContent=money(max);let v='—';if(used)v=ask?ask<=max?'BUY':ask<=max*1.10?'NEGOTIATE':'PASS':'PRICE READY';if($('deskVerdict')){$('deskVerdict').textContent=v;$('deskVerdict').className=v==='BUY'?'good':v==='NEGOTIATE'?'warn':v==='PASS'?'bad':'warn'}if($('deskAverage'))$('deskAverage').textContent=money(used);return{max,v}}
function showMode(o){const s=$('deskStatus');if(s)s.textContent=o?`OEM system detected: ${o.model}${o.cpu?' • '+o.cpu:''}${o.gpu?' • '+o.gpu:''}`:'Custom PC mode: component detection/pricing.'}
async function analyseOEM(o){const q=queryFor(o),btn=$('deskAnalyse');if(btn)btn.disabled=true;showMode(o);if($('deskStatus'))$('deskStatus').textContent=`Searching exact whole-system market: ${q}`;try{const d=await market(q,40,'device'),np=d.google?.pricing?.competitiveLow||d.google?.pricing?.typicalNew||0,ep=d.ebay?.pricing||{},used=ep.typicalUsed||0,gc=d.google?.analysedListings||0,ec=d.ebay?.analysedListings||0,{max,v}=setStats(np,used);oemState={o,q,newP:np,used,max,verdict:v,data:d};if($('deskStatus'))$('deskStatus').textContent=`OEM whole-device valuation • ${gc} Google new offers • ${ec} eBay used matches${ep.noReliableComparables?' • no reliable used comparables':''}`;if(typeof lastDesktop!=='undefined')lastDesktop=used?{name:o.model,cost:Number($('deskAsk')?.value)||Math.round(max),sell:Math.round(used),category:'Desktop PC',notes:q}:null;if($('deskAdd'))$('deskAdd').disabled=!used;if(typeof activity==='function'&&used)activity(`${o.model} valuation • ${v}`,money(used));if(typeof save==='function'&&used)save()}catch(e){if($('deskStatus'))$('deskStatus').textContent=String(e?.message||e)}finally{if(btn)btn.disabled=false}}
function boot(){const specs=$('deskSpecs'),detect=$('deskDetect'),analyse=$('deskAnalyse');if(!specs||!detect||!analyse)return;detect.onclick=()=>{const o=detectOEM(specs.value);oemState=o?{o}:null;if(o){showMode(o);const first=document.querySelector('#parts .pq');if(first){first.value=queryFor(o);first.closest('.part')?.querySelector('b')&&(first.closest('.part').querySelector('b').textContent='Whole OEM System')}}else if(typeof parseDesk==='function'){document.querySelectorAll('#parts .part b').forEach((b,i)=>{const names=['CPU','GPU','Motherboard','RAM','SSD','Power Supply','Case','CPU Cooler'];if(names[i])b.textContent=names[i]});parseDesk(specs.value)}};
analyse.onclick=async()=>{const o=detectOEM(specs.value);if(o)return analyseOEM(o);if(typeof parseDesk==='function')parseDesk(specs.value);analyse.disabled=true;let done=0;try{const qEls=[...document.querySelectorAll('#parts .pq')];for(let i=0;i<qEls.length;i++){if(!qEls[i].value.trim())continue;try{if(typeof pricePart==='function'){await pricePart(i);done++;if($('deskStatus'))$('deskStatus').textContent=`Pricing custom PC… ${done}`}}catch{}}if(typeof deskCalc==='function')deskCalc();if($('deskStatus'))$('deskStatus').textContent=`Priced ${done} custom-PC components.`}finally{analyse.disabled=false}};
['deskAsk','deskMargin'].forEach(id=>$(id)?.addEventListener('input',()=>{if(oemState?.used)setStats(oemState.newP,oemState.used)}));
}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(boot,140));else setTimeout(boot,140);
})();