(()=>{
 const AU_MANUFACTURERS=['apple.com/au','samsung.com/au','google.com/intl/en_au','motorola.com.au','oppo.com/au','xiaomi.com/au','sony.com.au','lenovo.com/au'];
 const GLOBAL_MANUFACTURERS=['apple.com','samsung.com','google.com','motorola.com','oppo.com','xiaomi.com','sony.com','lenovo.com','playstation.com','nintendo.com','xbox.com'];
 const AU_CARRIERS=['telstra.com.au','optus.com.au','vodafone.com.au'];
 const AU_RETAILERS=['jbhifi.com.au','officeworks.com.au','bigw.com.au','kmart.com.au','cashconverters.com.au'];
 const cleanHost=url=>{try{return new URL(url).hostname.replace(/^www\./,'').toLowerCase()}catch{return''}};
 const hostMatches=(host,list)=>list.some(d=>host===d||host.endsWith('.'+d));
 function classifySource(url){const h=cleanHost(url);if(!h)return{class:'unknown',priority:99};if(hostMatches(h,AU_MANUFACTURERS))return{class:'manufacturer-au',priority:1};if(hostMatches(h,GLOBAL_MANUFACTURERS))return{class:'manufacturer-global',priority:2};if(hostMatches(h,AU_CARRIERS))return{class:'carrier-au',priority:3};if(hostMatches(h,AU_RETAILERS))return{class:'retailer-au',priority:4};if(h.endsWith('.au'))return{class:'secondary-au',priority:5};return{class:'secondary-global',priority:6}}
 function rankEvidence(items=[]){return items.map(x=>({...x,...classifySource(x.url||'')})).sort((a,b)=>a.priority-b.priority||String(b.retrievedAt||'').localeCompare(String(a.retrievedAt||'')))}
 function confidence(items=[]){const ranked=rankEvidence(items),best=ranked[0]?.priority??99,distinct=new Set(ranked.map(x=>cleanHost(x.url))).size;if(!ranked.length)return 0;let score=best===1?0.72:best===2?0.64:best===3?0.55:best===4?0.48:0.35;score+=Math.min(0.2,Math.max(0,distinct-1)*0.06);return Math.max(0,Math.min(0.98,Number(score.toFixed(2))))}
 const norm=v=>String(v??'').trim().replace(/\s+/g,' ').toLowerCase();
 function auditCatalogue(records=[]){
  const findings=[],seen=new Map();
  records.forEach((r,i)=>{
   const id=r.id??r.device_id??i,brand=r.brand??'',name=r.device??r.name??r.model_name??'',model=r.model_number??r.modelNumber??'',storage=r.storage??'',year=r.release_year??r.releaseYear??'',image=r.image_url??r.image??'';
   const key=[brand,name,model,storage].map(norm).join('|');
   if(seen.has(key)&&key.replace(/\|/g,''))findings.push({type:'duplicate',severity:'high',entityId:id,duplicateOf:seen.get(key),field:null,current:key});else seen.set(key,id);
   if(!norm(model))findings.push({type:'missing-model-number',severity:'medium',entityId:id,field:'model_number',current:model});
   if(!norm(storage))findings.push({type:'missing-storage',severity:'medium',entityId:id,field:'storage',current:storage});
   if(!norm(year))findings.push({type:'missing-release-year',severity:'medium',entityId:id,field:'release_year',current:year});
   if(!norm(image))findings.push({type:'missing-image',severity:'low',entityId:id,field:'image_url',current:image});
   if(/\b(refurb|renewed|excellent condition|grade [abc]|bundle|unlocked phone)\b/i.test(String(name)))findings.push({type:'reseller-generated-name',severity:'medium',entityId:id,field:'name',current:name});
   const text=[name,model].join(' ');if(/\b(3g only|3g-only)\b/i.test(text))findings.push({type:'3g-only',severity:'high',entityId:id,field:'connectivity',current:text});
  });
  return findings;
 }
 function compareCatalogue(record,evidence=[]){const ranked=rankEvidence(evidence);return{record,evidence:ranked,confidence:confidence(ranked),freshest:ranked.map(x=>x.retrievedAt).filter(Boolean).sort().at(-1)||null}}
 window.NovaIntelligence=Object.freeze({classifySource,rankEvidence,confidence,auditCatalogue,compareCatalogue,version:'1.0.0'});
 window.dispatchEvent(new CustomEvent('nova-intelligence-ready',{detail:{version:'1.0.0'}}));
})();