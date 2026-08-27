(()=>{
  const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co';
  const API_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
  const API=`${SUPABASE_URL}/functions/v1/ebay-search`;
  const CATALOG_API=`${SUPABASE_URL}/functions/v1/model-catalog-search`;
  const STORE='morley_web_auth';
  let refreshPromise=null;

  function session(){try{return JSON.parse(localStorage.getItem(STORE)||'null')}catch{return null}}
  function saveSession(s){localStorage.setItem(STORE,JSON.stringify(s))}
  function clearSession(){localStorage.removeItem(STORE)}

  async function refreshSession(current){
    if(refreshPromise)return refreshPromise;
    refreshPromise=(async()=>{
      if(!current?.refresh_token)throw new Error('Your secure session has expired. Sign in again.');
      const r=await fetch(`${SUPABASE_URL}/auth/v1/token?grant_type=refresh_token`,{
        method:'POST',headers:{'Content-Type':'application/json',apikey:API_KEY},
        body:JSON.stringify({refresh_token:current.refresh_token})
      });
      let body={};try{body=await r.json()}catch{}
      if(!r.ok||!body?.access_token){clearSession();throw new Error('Your secure session has expired. Sign in again.');}
      const next={...current,access_token:body.access_token,refresh_token:body.refresh_token||current.refresh_token,expires_at:Date.now()+((body.expires_in||3600)*1000)};
      saveSession(next);return next;
    })();
    try{return await refreshPromise}finally{refreshPromise=null}
  }

  async function validSession(forceRefresh=false){
    let s=session();
    if(!s?.access_token)throw new Error('Your secure session has expired. Sign in again.');
    if(forceRefresh||(s.expires_at&&s.expires_at<=Date.now()+60000))s=await refreshSession(s);
    return s;
  }

  async function secureJsonFetch(url,body,retry=true){
    const s=await validSession(false);
    let r=await fetch(url,{method:'POST',headers:{'Content-Type':'application/json','Authorization':'Bearer '+s.access_token},body:JSON.stringify(body)});
    if(r.status===401&&retry){
      const fresh=await validSession(true);
      r=await fetch(url,{method:'POST',headers:{'Content-Type':'application/json','Authorization':'Bearer '+fresh.access_token},body:JSON.stringify(body)});
    }
    let data={};try{data=await r.json()}catch{}
    if(r.status===401||r.status===403){clearSession();throw new Error('Your secure session has expired or is not authorised. Sign in again.');}
    if(!r.ok)throw new Error(data?.error||`HTTP ${r.status}`);
    return data;
  }

  function looksLikeDeviceIdentifier(q){
    const x=String(q||'').trim().toLowerCase().replace(/[^a-z0-9-]+/g,' ');
    return /^(?:a\d{4}|mac\d{1,2}\s*\d{1,2}|[a-z]{1,8}\d[a-z0-9-]{2,})$/.test(x);
  }

  async function resolveDeviceQuery(q){
    const clean=String(q||'').trim();
    if(!looksLikeDeviceIdentifier(clean))return {query:clean,resolved:false};
    try{
      const d=await secureJsonFetch(CATALOG_API,{query:clean});
      const canonical=String(d?.canonicalQuery||'').trim();
      const score=Number(d?.best?.similarity_score||0);
      if(d?.success&&score>=0.42&&canonical)return {query:canonical,resolved:true,original:clean,best:d.best};
    }catch{}
    return {query:clean,resolved:false};
  }

  async function secureMarket(q,limit=30,mode='component'){
    const status=document.getElementById('apiStatus');if(status)status.textContent='...';
    try{
      const resolved=mode==='device'?await resolveDeviceQuery(q):{query:q,resolved:false};
      const d=await secureJsonFetch(API,{query:resolved.query,limit,australiaOnly:true,mode});
      if(!d.success)throw new Error(d?.error||'Pricing request failed');
      if(resolved.resolved){d.originalQuery=resolved.original;d.resolvedQuery=resolved.query;d.catalogMatch=resolved.best||null}
      if(status)status.textContent='READY';
      return d;
    }catch(e){if(status)status.textContent='ERROR';throw e}
  }

  window.morleySecureJsonFetch=secureJsonFetch;
  window.morleyResolveDeviceQuery=resolveDeviceQuery;
  window.market=secureMarket;
})();
