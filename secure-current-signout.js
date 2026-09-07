(()=>{
  const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co';
  const API_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
  const STORE='morley_web_auth';
  let handling=false;
  function session(){try{return JSON.parse(localStorage.getItem(STORE)||'null')}catch{return null}}
  async function revokeCurrent(){
    if(handling)return;handling=true;
    const s=session();
    try{
      if(s?.access_token){
        await fetch(`${SUPABASE_URL}/auth/v1/logout?scope=local`,{
          method:'POST',
          headers:{apikey:API_KEY,Authorization:`Bearer ${s.access_token}`}
        });
      }
    }catch{}
    localStorage.removeItem(STORE);
    location.reload();
  }
  document.addEventListener('click',event=>{
    const button=event.target?.closest?.('.morley-web-signout');
    if(!button)return;
    event.preventDefault();
    event.stopImmediatePropagation();
    revokeCurrent();
  },true);
})();
