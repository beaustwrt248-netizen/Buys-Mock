(function attachMorleyUniversalSearchModes(root){
  'use strict';
  const VALUES=Object.freeze(['quick_search','manual_search','price_check','ai_scan']);
  const VALID=new Set(VALUES);
  const DEFINITIONS=Object.freeze({
    quick_search:Object.freeze({mode:'quick_search',flow:'instant_lookup',route:'universal_buy_search',catalogueAuthority:'shared',pricingAuthority:'protected_shared'}),
    manual_search:Object.freeze({mode:'manual_search',flow:'guided_lookup',route:'universal_buy_search',catalogueAuthority:'shared',pricingAuthority:'protected_shared'}),
    price_check:Object.freeze({mode:'price_check',flow:'valuation_shortcut',route:'universal_buy_search',catalogueAuthority:'shared',pricingAuthority:'protected_shared'}),
    ai_scan:Object.freeze({mode:'ai_scan',flow:'device_lens',route:'device_lens',catalogueAuthority:'shared',pricingAuthority:'protected_shared'}),
  });
  function normalize(value){const mode=String(value||'').trim().toLowerCase();return VALID.has(mode)?mode:'quick_search'}
  function describe(value){return DEFINITIONS[normalize(value)]}
  root.MorleyUniversalSearchModes=Object.freeze({version:'1.0.0',values:VALUES,normalize,describe});
})(typeof globalThis!=='undefined'?globalThis:window);
