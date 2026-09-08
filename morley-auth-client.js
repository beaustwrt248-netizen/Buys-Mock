(()=>{'use strict';
function create({url,publishableKey,fetchImpl}={}){
  const base=String(url||'').replace(/\/+$/,'');
  const key=String(publishableKey||'');
  const fetcher=fetchImpl||(typeof window!=='undefined'&&window.fetch?window.fetch.bind(window):null);
  if(!base||!key||typeof fetcher!=='function')throw new Error('Morley auth configuration is incomplete.');
  function headers(accessToken=''){
    const value={apikey:key,Accept:'application/json'};
    if(accessToken)value.Authorization='Bearer '+accessToken;
    return value;
  }
  async function readJson(response){
    let data={};
    try{data=await response.json()}catch(_e){}
    if(!response.ok){
      const detail=data.msg||data.message||data.error_description||data.error||('Request failed ('+response.status+')');
      throw new Error(detail);
    }
    return data;
  }
  async function rest(path,accessToken=''){
    const response=await fetcher(base+String(path||''),{headers:headers(accessToken),cache:'no-store'});
    return readJson(response);
  }
  async function validateProfile(session,{roles=[]}={}){
    if(!session?.access_token||!session?.user?.id||!Array.isArray(roles)||roles.length===0)return false;
    const allowed=new Set(roles.map(role=>String(role)));
    const userResponse=await fetcher(base+'/auth/v1/user',{headers:headers(session.access_token),cache:'no-store'});
    if(!userResponse.ok)return false;
    const profileResponse=await fetcher(base+'/rest/v1/profiles?select=role,is_enabled&id=eq.'+encodeURIComponent(session.user.id),{headers:headers(session.access_token),cache:'no-store'});
    if(!profileResponse.ok)return false;
    const rows=await profileResponse.json();
    const profile=Array.isArray(rows)?rows[0]:null;
    return !!profile?.is_enabled&&allowed.has(String(profile.role||''));
  }
  async function refreshSession(session){
    if(!session?.refresh_token)return null;
    try{
      const response=await fetcher(base+'/auth/v1/token?grant_type=refresh_token',{method:'POST',headers:{...headers(),'Content-Type':'application/json'},body:JSON.stringify({refresh_token:session.refresh_token}),cache:'no-store'});
      if(!response.ok)return null;
      return await response.json();
    }catch(_e){return null}
  }
  async function signInPassword({email,password,captchaToken}={}){
    if(!captchaToken)throw new Error('Security check is required.');
    const response=await fetcher(base+'/auth/v1/token?grant_type=password',{method:'POST',headers:{...headers(),'Content-Type':'application/json'},body:JSON.stringify({email:String(email||''),password:String(password||''),gotrue_meta_security:{captcha_token:String(captchaToken)}}),cache:'no-store'});
    return readJson(response);
  }
  return Object.freeze({headers,readJson,rest,validateProfile,refreshSession,signInPassword});
}
window.MorleyAuthClient=Object.freeze({create});
try{window.dispatchEvent(new CustomEvent('morley:auth-client-ready',{detail:window.MorleyAuthClient}))}catch(_e){}
})();