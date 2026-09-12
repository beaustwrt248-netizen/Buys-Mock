(function(){
  const legacyFrame=document.getElementById('adminTurnstileFrame');
  const frame=legacyFrame;
  const loginBtn=document.getElementById('loginBtn');
  const loginStatus=document.getElementById('loginStatus');
  const challengeStatus=document.getElementById('challengeStatus');
  const emailInput=document.getElementById('email');
  const passwordInput=document.getElementById('password');
  if(!legacyFrame||!loginBtn||!loginStatus||!challengeStatus||!emailInput||!passwordInput)return;

  const nativeAuthMode=new URLSearchParams(location.search).get('nativeAuth')==='1';
  if(nativeAuthMode){
    legacyFrame.style.display='none';
    challengeStatus.textContent='Opening secure Admin session…';
    loginBtn.disabled=true;
    return;
  }

  const widget=document.createElement('div');
  widget.id='adminTurnstileWidget';
  widget.style.cssText='min-height:86px;display:flex;align-items:center;justify-content:center;padding:8px;box-sizing:border-box;background:#111';
  legacyFrame.replaceWith(widget);

  const sitekey='0x4AAAAAAEZul-Qo6dqMim2U';
  const apiSrc='https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
  let captchaToken='';
  let busy=false;
  let challengeWatchdog=null;
  let widgetId=null;
  let loadingApi=false;

  function credentialsReady(){return emailInput.value.trim().length>0&&emailInput.checkValidity()&&passwordInput.value.length>0;}
  function syncLoginEnabled(){loginBtn.disabled=busy||!captchaToken||!credentialsReady();}
  function setChallengeState(text,ok){challengeStatus.textContent=text;challengeStatus.style.color=ok?'#25d991':'#8fa6c6';}
  function clearChallengeWatchdog(){if(challengeWatchdog){clearTimeout(challengeWatchdog);challengeWatchdog=null;}}
  function armChallengeWatchdog(){clearChallengeWatchdog();challengeWatchdog=setTimeout(function(){if(captchaToken)return;setChallengeState('Security check unavailable. Tap here to retry.',false);syncLoginEnabled();},12000);}
  function removeRenderedChallenge(){if(widgetId!==null&&window.turnstile&&typeof window.turnstile.remove==='function'){try{window.turnstile.remove(widgetId);}catch(_){}}widgetId=null;widget.innerHTML='';}
  function renderChallenge(){
    if(!window.turnstile||widgetId!==null)return;
    removeRenderedChallenge();
    try{
      widgetId=window.turnstile.render(widget,{sitekey,theme:'dark',size:'flexible',action:'blmorley_auth',callback:function(token){clearChallengeWatchdog();captchaToken=String(token||'');syncLoginEnabled();setChallengeState(captchaToken?'Security check complete.':'Complete the security check to sign in.',!!captchaToken);},'expired-callback':function(){clearChallengeWatchdog();captchaToken='';syncLoginEnabled();setChallengeState('Security check expired. Tap here to retry.',false);},'error-callback':function(){clearChallengeWatchdog();captchaToken='';syncLoginEnabled();setChallengeState('Security check unavailable. Tap here to retry.',false);return false;}});
      setChallengeState('Complete the security check to sign in.',false);armChallengeWatchdog();
    }catch(_){widgetId=null;setChallengeState('Security check unavailable. Tap here to retry.',false);syncLoginEnabled();}
  }
  function loadApi(forceReload){
    if(window.turnstile){renderChallenge();return;}if(loadingApi)return;
    let script=document.getElementById('morleyAdminTurnstileApi');
    if(forceReload&&script){script.remove();script=null;}
    if(script){armChallengeWatchdog();return;}
    loadingApi=true;script=document.createElement('script');script.id='morleyAdminTurnstileApi';script.async=true;script.defer=true;script.src=apiSrc;
    script.onload=function(){loadingApi=false;window.turnstile?renderChallenge():setChallengeState('Security check unavailable. Tap here to retry.',false);};
    script.onerror=function(){loadingApi=false;clearChallengeWatchdog();setChallengeState('Security check unavailable. Tap here to retry.',false);syncLoginEnabled();};
    document.head.appendChild(script);armChallengeWatchdog();
  }
  function resetChallenge(message,forceReload){clearChallengeWatchdog();captchaToken='';removeRenderedChallenge();syncLoginEnabled();setChallengeState(message||'Security check loading…',false);loadApi(!!forceReload);}

  window.addEventListener('message',function(event){
    const trustedSource=event.source===frame.contentWindow;
    const trustedOrigin=event.origin===window.location.origin||(window.location.origin==='null'&&event.origin==='null');
    if(!trustedSource||!trustedOrigin)return;
    return;
  });

  emailInput.readOnly=false;emailInput.disabled=false;passwordInput.readOnly=false;passwordInput.disabled=false;emailInput.style.pointerEvents='auto';passwordInput.style.pointerEvents='auto';
  emailInput.addEventListener('input',syncLoginEnabled);passwordInput.addEventListener('input',syncLoginEnabled);emailInput.addEventListener('change',syncLoginEnabled);passwordInput.addEventListener('change',syncLoginEnabled);
  challengeStatus.addEventListener('click',function(){if(!captchaToken)resetChallenge('Retrying security check…',!window.turnstile);});
  document.addEventListener('visibilitychange',function(){if(document.visibilityState==='visible'&&!captchaToken&&widgetId===null)resetChallenge('Security check loading…',!window.turnstile);});
  window.addEventListener('pageshow',function(event){if(event.persisted&&!captchaToken)resetChallenge('Security check loading…',!window.turnstile);});
  syncLoginEnabled();resetChallenge('Security check loading…',false);

  loginBtn.onclick=async function(){
    const email=emailInput.value.trim();const password=passwordInput.value;
    if(!credentialsReady()){loginStatus.textContent='Enter a valid email and password.';syncLoginEnabled();return;}
    if(!captchaToken){loginStatus.textContent='Complete the security check first.';resetChallenge('Complete the security check to sign in.',false);return;}
    const token=captchaToken;busy=true;syncLoginEnabled();loginStatus.textContent='Signing in…';
    try{const {error}=await sb.auth.signInWithPassword({email,password,options:{captchaToken:token}});if(error){loginStatus.textContent=error.message;resetChallenge('Complete a new security check to retry.',false);return;}await loadSession();}
    catch(error){loginStatus.textContent=error&&error.message?error.message:'Sign in failed. Please retry.';resetChallenge('Complete a new security check to retry.',false);}
    finally{busy=false;syncLoginEnabled();}
  };
})();
