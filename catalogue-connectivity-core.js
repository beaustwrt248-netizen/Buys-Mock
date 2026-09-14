(function(root,factory){
  const api=factory();
  if(typeof module==='object'&&module.exports)module.exports=api;
  else root.MorleyCatalogueConnectivity=api;
})(typeof globalThis!=='undefined'?globalThis:this,function(){
  'use strict';

  const clean=value=>String(value||'').trim().replace(/\s+/g,' ');
  const lower=value=>clean(value).toLowerCase();
  const escapeRe=value=>String(value).replace(/[.*+?^${}()|[\]\\]/g,'\\$&');

  const DEFINITIONS={
    tablet:[
      {label:'Wi-Fi + Cellular',re:/\s*(?:[-–—,/]|\()?\s*wi[ -]?fi\s*\+\s*(?:cellular|lte|4g|5g)\s*\)?\s*$/i},
      {label:'Wi-Fi',re:/\s*(?:[-–—,/]|\()?\s*wi[ -]?fi\s*\)?\s*$/i},
      {label:'5G',re:/\s*(?:[-–—,/]|\()?\s*5g\s*\)?\s*$/i},
      {label:'4G',re:/\s*(?:[-–—,/]|\()?\s*4g\s*\)?\s*$/i},
      {label:'LTE',re:/\s*(?:[-–—,/]|\()?\s*lte\s*\)?\s*$/i},
      {label:'Cellular',re:/\s*(?:[-–—,/]|\()?\s*cellular\s*\)?\s*$/i}
    ],
    wearable:[
      {label:'GPS + Cellular',re:/\s*(?:[-–—,/]|\()?\s*gps\s*\+\s*(?:cellular|lte)\s*\)?\s*$/i},
      {label:'Bluetooth + LTE',re:/\s*(?:[-–—,/]|\()?\s*bluetooth\s*\+\s*lte\s*\)?\s*$/i},
      {label:'GPS',re:/\s*(?:[-–—,/]|\()?\s*gps\s*\)?\s*$/i},
      {label:'LTE',re:/\s*(?:[-–—,/]|\()?\s*lte\s*\)?\s*$/i},
      {label:'Cellular',re:/\s*(?:[-–—,/]|\()?\s*cellular\s*\)?\s*$/i},
      {label:'Bluetooth',re:/\s*(?:[-–—,/]|\()?\s*bluetooth\s*\)?\s*$/i}
    ]
  };

  const aliases={
    'Wi-Fi':['wifi','wi-fi'],
    'Wi-Fi + Cellular':['wifi cellular','wi-fi cellular','wifi+cellular','wi-fi+cellular','wifi lte','wifi+lte','cellular','lte'],
    '5G':['5g','cellular','mobile data'],
    '4G':['4g','cellular','mobile data','lte'],
    'LTE':['lte','cellular','mobile data'],
    'Cellular':['cellular','lte','mobile data'],
    'GPS':['gps'],
    'GPS + Cellular':['gps cellular','gps+cellular','gps lte','gps+lte','cellular','lte','gps'],
    'Bluetooth':['bluetooth','bt'],
    'Bluetooth + LTE':['bluetooth lte','bluetooth+lte','bt lte','lte','cellular','bluetooth']
  };

  function category(value){
    const v=lower(value);
    if(v==='tablet'||v==='tablets')return'tablet';
    if(v==='wearable'||v==='wearables'||v==='watch'||v==='smartwatch'||v==='smart watch')return'wearable';
    return v;
  }

  function parseVariant(rawCategory,rawModel){
    const kind=category(rawCategory),model=clean(rawModel),defs=DEFINITIONS[kind]||[];
    for(const def of defs){
      if(!def.re.test(model))continue;
      let baseModel=clean(model.replace(def.re,''));
      baseModel=baseModel.replace(/\s*[-–—,/]+\s*$/,'').trim();
      return{category:kind,baseModel:baseModel||model,connectivity:def.label,sourceModel:model};
    }
    return{category:kind,baseModel:model,connectivity:'',sourceModel:model};
  }

  function groupKey(row){
    const parsed=parseVariant(row?.category,row?.model||row?.title||row?.query);
    return[lower(row?.brand),lower(parsed.baseModel)].join('|');
  }

  function searchTerms(rawCategory,connectivity){
    const kind=category(rawCategory),label=clean(connectivity);
    const values=new Set([label,...(aliases[label]||[])]);
    if(kind==='wearable'&&label==='GPS + Cellular')values.add('gps+lte');
    if(kind==='tablet'&&label==='Wi-Fi + Cellular')values.add('wifi+lte');
    return[...values].map(lower).filter(Boolean);
  }

  function connectivityMatches(rawCategory,connectivity,query){
    const needle=lower(query).replace(/\s+/g,' ');
    if(!needle)return true;
    const compact=needle.replace(/\s+/g,'');
    return searchTerms(rawCategory,connectivity).some(term=>{
      const t=lower(term);return t.includes(needle)||t.replace(/\s+/g,'').includes(compact);
    });
  }

  function rowSearchText(row){
    const parsed=parseVariant(row?.category,row?.model||row?.title||row?.query);
    return clean([
      row?.brand,parsed.baseModel,row?.model,row?.modelNumber,row?.storage,
      parsed.connectivity,...searchTerms(row?.category,parsed.connectivity)
    ].filter(Boolean).join(' ')).toLowerCase();
  }

  return{category,parseVariant,groupKey,searchTerms,connectivityMatches,rowSearchText};
});
