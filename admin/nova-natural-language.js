(()=>{'use strict';
const $=id=>document.getElementById(id);
const esc=v=>String(v??'').replace(/[&<>"']/g,m=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#039;"}[m]));
const money=v=>`AUD ${Number(v||0).toLocaleString(undefined,{minimumFractionDigits:0,maximumFractionDigits:2})}`;
let busy=false;
function bubble(role,text){const host=$('morleyAiConversation');if(!host)return;const div=document.createElement('div');div.className='card';div.style.margin='10px 0';div.innerHTML=`<strong>${role==='user'?'You':'Nova AI'}</strong><div style="margin-top:6px;white-space:pre-wrap">${esc(text)}</div>`;host.appendChild(div);host.scrollTop=host.scrollHeight}
function normalise(text){return String(text||'').toLowerCase().replace(/[’']/g,"'").replace(/[^a-z0-9$%+.' -]+/g,' ').replace(/\s+/g,' ').trim()}
function intentFor(text){const x=normalise(text);if(!x)return null;
 if(/^(hi|hey|hello|hiya|yo|good (morning|afternoon|evening)|howdy)( nova| there)?[!. ]*$/.test(x))return'greeting';
 if(/what can you do|what do you know|your capabilities|what are your capabilities|help me use nova/.test(x))return'capabilities';
 if(/\b(your performance|performance like|how (are|is) (we|morley|business) doing|business performance|overall performance|sales performance|how are sales|how('?s| is) sales|sell[- ]?through|sales results|profitability|profit margin|profits?)\b/.test(x))return'performance';
 if(/\b(inventory health|inventory status|stock health|stock levels?|what('?s| is) in stock|inventory performance)\b/.test(x))return'inventory';
 return null}
async function performanceSummary(){
 const [sales,inventory,valuations,incidents]=await Promise.all([
  sb.from('sales_records').select('id,acquired_cost,sold_price,fees,other_costs,realised_profit,sold_at').order('sold_at',{ascending:false}).limit(100),
  sb.from('inventory_items').select('id,status,acquired_cost,asking_price,acquired_at').order('acquired_at',{ascending:false}).limit(500),
  sb.from('valuation_history').select('id,expected_profit,actual_profit,status,created_at').order('created_at',{ascending:false}).limit(250),
  sb.from('guardian_incidents').select('id,state,risk_level,requires_approval,updated_at').order('updated_at',{ascending:false}).limit(100)
 ]);
 for(const r of [sales,inventory,valuations,incidents])if(r.error)throw r.error;
 const s=sales.data||[],inv=inventory.data||[],vals=valuations.data||[],gi=incidents.data||[];
 const profits=s.map(x=>Number(x.realised_profit)).filter(Number.isFinite),totalProfit=profits.reduce((a,b)=>a+b,0),avgProfit=profits.length?totalProfit/profits.length:0;
 const positive=profits.filter(x=>x>0).length,winRate=profits.length?positive/profits.length*100:0;
 const available=inv.filter(x=>!['sold','closed','disposed'].includes(String(x.status||'').toLowerCase())).length;
 const realised=vals.filter(x=>x.expected_profit!=null&&x.actual_profit!=null),mae=realised.length?realised.reduce((n,x)=>n+Math.abs(Number(x.actual_profit)-Number(x.expected_profit)),0)/realised.length:null;
 const openGuardian=gi.filter(x=>!['resolved','ignored','closed'].includes(String(x.state||'').toLowerCase()));
 const approvals=openGuardian.filter(x=>x.requires_approval).length;
 return `Here’s Morley’s current performance from the live records I can read:\n\nSales: ${s.length} recent realised sale${s.length===1?'':'s'} • total realised profit ${money(totalProfit)} • average profit ${money(avgProfit)} • profitable-sale rate ${profits.length?winRate.toFixed(0)+'%':'not enough data'}.\nInventory: ${available} active/available item${available===1?'':'s'} across ${inv.length} recent inventory records.\nValuations: ${realised.length} realised forecast outcome${realised.length===1?'':'s'}${mae==null?'':` • average absolute profit forecast error ${money(mae)}`}.\nGuardian: ${openGuardian.length} open signal${openGuardian.length===1?'':'s'} • ${approvals} requiring approval.\n\nI can drill into sales, profit, inventory, valuations or Guardian if you want a specific breakdown.`
}
async function inventorySummary(){const r=await sb.from('inventory_items').select('id,status,acquired_cost,asking_price,acquired_at').order('acquired_at',{ascending:false}).limit(500);if(r.error)throw r.error;const rows=r.data||[],groups={};for(const x of rows){const k=String(x.status||'unknown').replaceAll('_',' ');groups[k]=(groups[k]||0)+1}const active=rows.filter(x=>!['sold','closed','disposed'].includes(String(x.status||'').toLowerCase())).length;return `Inventory health: ${active} active/available item${active===1?'':'s'} across ${rows.length} recent records.\n\n`+Object.entries(groups).sort((a,b)=>b[1]-a[1]).map(([k,v])=>`• ${k}: ${v}`).join('\n')}
function capabilities(){const caps=window.MorleyAI?.list?.()||[];if(!caps.length)return'I am still loading my connected capabilities. Try again in a moment.';return `I currently have ${caps.length} registered Morley capabilities. I can read and reason across pricing, Guardian, learning, support, releases, the device catalogue and valuation intelligence, while protected writes still require the existing approval path.\n\n`+caps.map(c=>`• ${c.label}`).join('\n')}
async function handle(text,intent){if(busy)return;busy=true;const status=$('morleyAiStatus'),input=$('morleyAiInput');bubble('user',text);if(input)input.value='';if(status)status.textContent='Thinking with live Morley data…';try{let answer='';if(intent==='greeting')answer='Hi! I’m Nova AI. I’m ready — ask me naturally about Morley performance, sales, profit, inventory, pricing, devices, valuations, Guardian, support or releases.';else if(intent==='capabilities')answer=capabilities();else if(intent==='performance')answer=await performanceSummary();else if(intent==='inventory')answer=await inventorySummary();bubble('ai',answer);if(status)status.textContent='Ready.'}catch(e){console.error('Nova natural-language intent failed',e);bubble('ai','I understood the question, but the live data needed for that answer could not be read just now. I did not make any protected change.');if(status)status.textContent=e?.message||'Live data unavailable.'}finally{busy=false}}
function intercept(e){if(e.type==='keydown'&&(e.key!=='Enter'||e.shiftKey))return;const input=$('morleyAiInput');const text=input?.value.trim();const intent=intentFor(text);if(!intent)return;e.preventDefault();e.stopImmediatePropagation();handle(text,intent)}
function wire(){const btn=$('morleyAiSendBtn'),input=$('morleyAiInput');btn?.addEventListener('click',intercept,true);input?.addEventListener('keydown',intercept,true);document.documentElement.dataset.novaNaturalLanguage='ready'}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',wire,{once:true});else wire();
})();
