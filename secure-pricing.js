(()=>{
  const API='https://ghdhairijqjqivqriigi.supabase.co/functions/v1/ebay-search';
  const STORE='morley_web_auth';

  function session(){
    try{return JSON.parse(localStorage.getItem(STORE)||'null')}catch{return null}
  }

  async function secureMarket(q,limit=30,mode='component'){
    const s=session(),token=s?.access_token||'';
    if(!token) throw new Error('Your secure session has expired. Sign in again.');
    const r=await fetch(API,{
      method:'POST',
      headers:{'Content-Type':'application/json','Authorization':'Bearer '+token},
      body:JSON.stringify({query:q,limit,australiaOnly:true,mode})
    });
    let d={};
    try{d=await r.json()}catch{}
    const status=document.getElementById('apiStatus');
    if(status) status.textContent=r.ok?'READY':'ERROR';
    if(r.status===401||r.status===403) throw new Error('Your secure session has expired or is not authorised. Sign in again.');
    if(!r.ok||!d.success) throw new Error(d?.error||`HTTP ${r.status}`);
    return d;
  }

  // The production shell currently loads the mature calculator core from a pinned
  // candidate snapshot. Replace only its pricing transport so existing workflow
  // logic remains untouched while paid API calls require the signed-in session.
  window.market=secureMarket;
})();
