(()=>{
  const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co';
  const API_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
  const API=`${SUPABASE_URL}/functions/v1/ebay-search`;
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

  async function secureMarket(q,limit=30,mode='component'){
    const status=document.getElementById('apiStatus');if(status)status.textContent='...';
    try{
      const d=await secureJsonFetch(API,{query:q,limit,australiaOnly:true,mode});
      if(!d.success)throw new Error(d?.error||'Pricing request failed');
      if(status)status.textContent='READY';
      return d;
    }catch(e){if(status)status.textContent='ERROR';throw e}
  }

  // Shared authenticated transport for additional pricing tools loaded after this file.
  window.morleySecureJsonFetch=secureJsonFetch;
  // Replace the pinned calculator core's anonymous pricing transport without changing
  // its mature valuation/inventory workflow.
  window.market=secureMarket;
})();
