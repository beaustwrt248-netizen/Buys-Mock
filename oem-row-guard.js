(()=>{
const $=id=>document.getElementById(id);
function clean(s){return String(s||'').replace(/\s+/g,' ').trim()}
function detectOEM(text){const t=clean(text);const pats=[/\b(?:Lenovo\s+)?ThinkCentre\s+[A-Z0-9-]+(?:\s+Gen\s+\d+)?/i,/\b(?:Lenovo\s+)?ThinkStation\s+[A-Z0-9-]+(?:\s+Gen\s+\d+)?/i,/\b(?:Dell\s+)?OptiPlex\s+[A-Z0-9-]+/i,/\b(?:Dell\s+)?Precision\s+[A-Z0-9-]+/i,/\b(?:HP\s+)?(?:EliteDesk|ProDesk)\s+[A-Z0-9 -]+?(?=\s{2,}|,|;|$)/i,/\b(?:HP\s+)?OMEN\s+(?:Desktop\s*)?[A-Z0-9-]+/i,/\b(?:Acer\s+)?Predator\s+Orion\s+[A-Z0-9-]+/i,/\b(?:ASUS\s+)?ROG\s+(?:G|Strix|Desktop)[A-Z0-9-]*/i,/\b(?:MSI\s+)?(?:Aegis|Infinite|Trident|Codex)\s+[A-Z0-9-]+/i];
 let model='';for(const r of pats){const m=t.match(r);if(m){model=m[0];break}}if(!model)return null;
 const cpu=t.match(/\b(?:Intel\s+)?Core\s+Ultra\s+[3579]\s+\d{3}[A-Z]{0,2}\b/i)?.[0]||t.match(/\b(?:Intel\s+)?(?:Core\s+)?i[3579][ -]?\d{4,5}(?:K|KF|F|KS|T)?\b/i)?.[0]||t.match(/\b(?:AMD\s+)?Ryzen\s+[3579]\s+\d{4,5}(?:X3D|X|G|GE)?\b/i)?.[0]||'';
 const gpu=t.match(/\b(?:RTX|GTX)\s*\d{3,4}(?:\s*(?:Ti|SUPER))?\b/i)?.[0]||t.match(/\bRX\s*\d{3,4}(?:\s*(?:XT|XTX|GRE))?\b/i)?.[0]||'';
 const ram=t.match(/\b(?:8|12|16|24|32|48|64|96|128)\s*GB\s*(?:DDR[345])?\b/i)?.[0]||'';
 const storage=t.match(/\b(?:128|256|512)\s*GB\b|\b(?:1|2|4|8)\s*TB\b/i)?.[0]||'';
 return {model:clean(model),query:[model,cpu,gpu,ram,storage].filter(Boolean).join(' ')};
}
function apply(){const specs=$('deskSpecs');if(!specs)return;const o=detectOEM(specs.value);if(!o)return;const first=document.querySelector('#parts .part');if(!first)return;const title=first.querySelector('b'),q=first.querySelector('.pq'),live=first.querySelector('.psearch');if(title)title.textContent='Whole OEM System';if(q)q.value=o.query;if(live){live.dataset.oem='1';live.title='Price whole OEM system';}}
$('deskDetect')?.addEventListener('click',()=>setTimeout(apply,30));$('deskAnalyse')?.addEventListener('click',()=>setTimeout(apply,30));
document.addEventListener('click',e=>{const live=e.target?.closest?.('.psearch[data-oem="1"]');if(!live)return;e.preventDefault();e.stopPropagation();e.stopImmediatePropagation();const a=$('deskAnalyse');if(a&&!a.disabled)a.click();},true);
setTimeout(apply,250);
})();