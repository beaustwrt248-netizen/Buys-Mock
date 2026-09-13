(()=>{'use strict';
const REQUEST_TIMEOUT_MS=12000;
function classifyError(error){
  if(error?.code)return error;
  const message=String(error?.message||error||'Authentication request failed.');
  const classified=error instanceof Error?error:new Error(message);
  classified.code=(error?.name==='AbortError'||/timed out/i.test(message))?'AUTH_TIMEOUT':'AUTH_NETWORK';
  return classified;
}
function create({url,publishableKey,fetchImpl}={}){
  const base=String(url||'').replace(/\/+$/,'');
  const key=String(publishableKey||'');
  const fetcher=fetchImpl||(typeof window!=='undefined'&&window.fetch?window.fetch.bind(window):null);
  if(!base||!key||typeof fetcher!=='function'){const error=new Error('Morley auth configuration is incomplete.');error.code='AUTH_CONFIG';throw error}
  function headers(accessToken=''){
    const value={apikey:key,Accept:'application/json'};
    if(accessToken)value.Authorization='Bearer '+accessToken;
    return value;
  }
  async function request(input,init={}){
    if(init?.signal||typeof AbortController!=='function'){
      try{return await fetcher(input,init)}catch(error){throw classifyError(error)}
    }
    const controller=new AbortController();
    const timer=setTimeout(()=>controller.abort(),REQUEST_TIMEOUT_MS);
    try{return await fetcher(input,{...init,signal:controller.signal})}
    catch(error){
      if(controller.signal.aborted){const timeout=new Error('Authentication request timed out. Check your connection and try again.');timeout.code='AUTH_TIMEOUT';throw timeout}
      throw classifyError(error);
    }finally{clearTimeout(timer)}
  }
  async function readJson(response){
    let data={};
    try{data=await response.json()}catch(_e){}
    if(!response.ok){
      const detail=data.msg||data.message||data.error_description||data.error||('Request failed ('+response.status+')');
      const error=new Error(detail);error.code='AUTH_HTTP';error.status=response.status;throw error;
    }
    return data;
  }
  async function rest(path,accessToken=''){
    const response=await request(base+String(path||''),{headers:headers(accessToken),cache:'no-store'});
    return readJson(response);
  }
  async function validateProfile(session,{roles=[]}={}){
    if(!session?.access_token||!session?.user?.id||!Array.isArray(roles)||roles.length===0)return false;
    const allowed=new Set(roles.map(role=>String(role)));
    try{
      const userResponse=await request(base+'/auth/v1/user',{headers:headers(session.access_token),cache:'no-store'});
      if(!userResponse.ok)return false;
      const profileResponse=await request(base+'/rest/v1/profiles?select=role,is_enabled&id=eq.'+encodeURIComponent(session.user.id),{headers:headers(session.access_token),cache:'no-store'});
      if(!profileResponse.ok)return false;
      const rows=await profileResponse.json();
      const profile=Array.isArray(rows)?rows[0]:null;
      return !!profile?.is_enabled&&allowed.has(String(profile.role||''));
    }catch(error){
      if(error?.code==='AUTH_TIMEOUT'||error?.code==='AUTH_NETWORK')throw error;
      return false;
    }
  }
  async function refreshSession(session){
    if(!session?.refresh_token)return null;
    const response=await request(base+'/auth/v1/token?grant_type=refresh_token',{method:'POST',headers:{...headers(),'Content-Type':'application/json'},body:JSON.stringify({refresh_token:session.refresh_token}),cache:'no-store'});
    if(!response.ok){
      if([400,401,403].includes(Number(response.status)))return null;
      return readJson(response);
    }
    return response.json();
  }
  async function signInPassword({email,password,captchaToken}={}){
    if(!captchaToken)throw new Error('Security check is required.');
    const response=await request(base+'/auth/v1/token?grant_type=password',{method:'POST',headers:{...headers(),'Content-Type':'application/json'},body:JSON.stringify({email:String(email||''),password:String(password||''),gotrue_meta_security:{captcha_token:String(captchaToken)}}),cache:'no-store'});
    return readJson(response);
  }
  return Object.freeze({headers,readJson,rest,validateProfile,refreshSession,signInPassword});
}
window.MorleyAuthClient=Object.freeze({create,classifyError});
try{window.dispatchEvent(new CustomEvent('morley:auth-client-ready',{detail:window.MorleyAuthClient}))}catch(_e){}
})();