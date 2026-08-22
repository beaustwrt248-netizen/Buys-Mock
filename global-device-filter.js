(()=>{
const originalFetch=window.fetch.bind(window);
const norm=s=>String(s||'').toLowerCase().replace(/[^a-z0-9]+/g,' ').replace(/\s+/g,' ').trim();
const median=a=>{if(!a.length)return 0;const s=[...a].sort((x,y)=>x-y),m=Math.floor(s.length/2);return s.length%2?s[m]:(s[m-1]+s[m])/2};
const pct=(a,p)=>{if(!a.length)return 0;const s=[...a].sort((x,y)=>x-y),i=(s.length-1)*p,l=Math.floor(i),h=Math.ceil(i);return l===h?s[l]:s[l]+(s[h]-s[l])*(i-l)};
function completeDeviceTitle(title){const t=norm(title);return /\b(macbook|laptop|notebook|ultrabook|chromebook|gaming laptop|desktop|desktop pc|desktop computer|gaming pc|gaming desktop|workstation|tower pc|prebuilt|all in one|aio|thinkcentre|thinkstation|optiplex|precision|elitedesk|prodesk|omen|legion|predator|alienware|imac|mac mini|surface laptop|trident|aegis|infinite|codex)\b/.test(t)}
function rejectWholeDeviceResult(title){const t=norm(title),complete=completeDeviceTitle(title);
 if(/\b(empty box|box only|for parts|parts only|not working|faulty|broken|repair only|job lot|lot of|dummy)\b/.test(t))return true;
 if(/\b(webcam|camera module|camera board|camera cable|logic board|daughter board|io board|replacement battery|battery only|charger only|power adapter|magsafe charger|usb c charger|replacement screen|lcd panel|display panel|screen assembly|replacement keyboard|keyboard assembly|trackpad|touchpad|hinge|hinges|caddy|enclosure|upgrade kit|drive kit|cooling fan|fan assembly|heatsink|case only|shell|flex cable|bracket|bezel|speaker|speakers|stand only|base only|wifi card|wireless card|antenna|keyboard cover|keyboard skin|screen protector|privacy screen|hard case|laptop case|macbook case|protective case|case cover|sleeve|carry bag|laptop bag|dock station|docking station|laptop stand|egpu enclosure|waterblock|water block|backplate|mounting bracket|adapter cable|riser cable)\b/.test(t))return true;
 if(/\b(memory module|ram module|sodimm|so dimm|motherboard|mainboard|solid state drive|hard drive|hdd|nvme|pcie ssd|ssd)\b/.test(t)&&!complete)return true;
 if(/\b(power supply|psu)\b/.test(t)&&!complete)return true;
 return false;
}
function cleanDevicePayload(d){if(!d||d.mode!=='device')return d;
 if(d.ebay){const items=(d.ebay.items||[]).filter(x=>!rejectWholeDeviceResult(x.title));const ps=items.map(x=>Number(x.deliveredPrice||x.price)||0).filter(Boolean);d.ebay.items=items;d.ebay.analysedListings=items.length;d.ebay.pricing=d.ebay.pricing||{};d.ebay.pricing.typicalUsed=median(ps);d.ebay.pricing.p25=pct(ps,.25);d.ebay.pricing.p75=pct(ps,.75);d.ebay.pricing.lowest=ps.length?Math.min(...ps):0;d.ebay.pricing.highest=ps.length?Math.max(...ps):0;if(!ps.length){d.ebay.pricing.noReliableComparables=true;d.ebay.pricing.estimatedFromNew=false}}
 if(d.google){const items=(d.google.items||[]).filter(x=>!rejectWholeDeviceResult(x.title));const ps=items.map(x=>Number(x.price)||0).filter(Boolean);d.google.items=items;d.google.analysedListings=items.length;d.google.pricing=d.google.pricing||{};d.google.pricing.competitiveLow=ps.length?Math.min(...ps):0;d.google.pricing.competitiveHigh=ps.length?Math.max(...ps):0;d.google.pricing.typicalNew=median(ps)}
 return d;
}
function msiTridentQuery(input,init){try{const body=JSON.parse(init?.body||'{}');const q=norm(body.query||'');return q.includes('trident')?q:''}catch{return''}}
function validMsiTrident(title,q){const t=norm(title);if(rejectWholeDeviceResult(title))return false;
 if(!/\bmsi\b/.test(t)||!/\btrident\b/.test(t))return false;
 if(q.includes('mpg')&&!/\bmpg\b/.test(t))return false;
 if(/\btrident as\b/.test(q)&&!/\btrident\s+as\b/.test(t))return false;
 if(/\b(fishing|fishing rod|rod|reel|lure|bait|aquarium|costume|cosplay|halloween|helmet|mask|statue|ornament|poster|book|novel|game|toy|prop|maple bonsai|bonsai|plant|gold trident|necklace|pendant)\b/.test(t))return false;
 return true;
}
function cleanMsiTridentPayload(d,q){if(!d?.success||!q)return d;
 const e=(d.ebay?.items||[]).filter(x=>validMsiTrident(x.title,q));
 const g=(d.google?.items||[]).filter(x=>validMsiTrident(x.title,q));
 const ep=e.map(x=>Number(x.deliveredPrice||x.price)||0).filter(Boolean),gp=g.map(x=>Number(x.price)||0).filter(Boolean);
 const newP=gp.length?Math.min(...gp):0;
 let used=median(ep),estimated=false;
 if(!used&&newP){used=Math.round(newP*.58);estimated=true}
 d.ebay=d.ebay||{};d.google=d.google||{};d.ebay.pricing=d.ebay.pricing||{};d.google.pricing=d.google.pricing||{};
 d.ebay.items=e;d.ebay.analysedListings=e.length;d.ebay.pricing.typicalUsed=used;
 d.google.items=g;d.google.analysedListings=g.length;d.google.pricing.competitiveLow=newP;d.google.pricing.typicalNew=median(gp);
 d.valuationBasis=d.valuationBasis||{};
 d.valuationBasis.used=estimated?'Estimated used value (58% of verified MSI Trident new retail; no exact used comps)':'Exact MSI Trident eBay AU used comparables';
 d.valuationBasis.estimatedUsed=estimated;
 d.valuationBasis.retail='Strict MSI Trident whole-device offers';
 d.filterNote='Strict MSI Trident whole-device filter applied';
 return d;
}
window.fetch=async function(input,init){const r=await originalFetch(input,init);const url=typeof input==='string'?input:input?.url||'';
 try{
  if(url.includes('/functions/v1/ebay-search')){const clone=r.clone(),d=await clone.json();if(!d?.success||d?.mode!=='device')return r;const cleaned=cleanDevicePayload(d);return new Response(JSON.stringify(cleaned),{status:r.status,statusText:r.statusText,headers:r.headers})}
  if(url.includes('/functions/v1/oem-search')){const q=msiTridentQuery(input,init);if(!q)return r;const clone=r.clone(),d=await clone.json();const cleaned=cleanMsiTridentPayload(d,q);return new Response(JSON.stringify(cleaned),{status:r.status,statusText:r.statusText,headers:r.headers})}
 }catch{return r}
 return r;
}
})();