(()=>{
  const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co';
  const API_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
  const STORE='morley_web_auth';
  const LOGOUT_TIMEOUT_MS=12000;
  let handling=false;
  function session(){try{return JSON.parse(localStorage.getItem(STORE)||'null')}catch{return null}}
  function emitError(error){
    const detail={message:String(error?.message||'Sign out failed. Check your connection and try again.'),code:String(error?.code||'AUTH_SIGNOUT_FAILED')};
    try{window.dispatchEvent(new CustomEvent('morley:signout-error',{detail}))}catch(_e){}
  }
  async function revokeCurrent(){
    if(handling)return false;handling=true;
    const s=session();
    try{
      if(s?.access_token){
        const controller=typeof AbortController==='function'?new AbortController():null;
        const timer=controller?setTimeout(()=>controller.abort(),LOGOUT_TIMEOUT_MS):0;
        let response;
        try{
          response=await fetch(`${SUPABASE_URL}/auth/v1/logout?scope=local`,{
            method:'POST',
            headers:{apikey:API_KEY,Authorization:`Bearer ${s.access_token}`},
            ...(controller?{signal:controller.signal}:{})
          });
        }catch(error){
          if(controller?.signal.aborted){const timeout=new Error('Sign out timed out. Check your connection and try again.');timeout.code='AUTH_TIMEOUT';throw timeout}
          const network=error instanceof Error?error:new Error(String(error||'Sign out failed.'));network.code=network.code||'AUTH_NETWORK';throw network;
        }finally{if(timer)clearTimeout(timer)}
        if(!response.ok&&![401,403].includes(Number(response.status))){const error=new Error(`Sign out failed (${response.status}). Please try again.`);error.code='AUTH_HTTP';throw error}
      }
      localStorage.removeItem(STORE);
      location.reload();
      return true;
    }catch(error){
      emitError(error);
      handling=false;
      return false;
    }
  }
  document.addEventListener('click',event=>{
    const button=event.target?.closest?.('.morley-web-signout');
    if(!button)return;
    event.preventDefault();
    event.stopImmediatePropagation();
    revokeCurrent();
  },true);
})();
