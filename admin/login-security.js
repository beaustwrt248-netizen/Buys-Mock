(function(){
  const frame=document.getElementById('adminTurnstileFrame');
  const loginBtn=document.getElementById('loginBtn');
  const loginStatus=document.getElementById('loginStatus');
  const challengeStatus=document.getElementById('challengeStatus');
  const emailInput=document.getElementById('email');
  const passwordInput=document.getElementById('password');
  if(!frame||!loginBtn||!loginStatus||!challengeStatus||!emailInput||!passwordInput)return;

  const nativeAuthMode=new URLSearchParams(location.search).get('nativeAuth')==='1';
  if(nativeAuthMode){
    frame.style.display='none';
    challengeStatus.textContent='Opening secure Admin session…';
    loginBtn.disabled=true;
    return;
  }

  const challengeHost=document.createElement('div');
  challengeHost.id='adminTurnstileWidget';
  challengeHost.style.minHeight='118px';
  challengeHost.style.display='flex';
  challengeHost.style.alignItems='center';
  challengeHost.style.justifyContent='center';
  frame.replaceWith(challengeHost);

  let captchaToken='';
  let busy=false;
  let widgetId=null;
  let apiLoading=false;
  let challengeWatchdog=null;

  function credentialsReady(){return emailInput.value.trim().length>0&&emailInput.checkValidity()&&passwordInput.value.length>0;}
  function syncLoginEnabled(){loginBtn.disabled=busy||!captchaToken||!credentialsReady();}
  function setChallengeState(text,ok){challengeStatus.textContent=text;challengeStatus.style.color=ok?'#25d991':'#8fa6c6';}
  function clearChallengeWatchdog(){if(challengeWatchdog){clearTimeout(challengeWatchdog);challengeWatchdog=null;}}
  function armChallengeWatchdog(){
    clearChallengeWatchdog();
    challengeWatchdog=setTimeout(function(){
      if(captchaToken)return;
      apiLoading=false;
      setChallengeState('Security check unavailable. Tap here to retry.',false);
      syncLoginEnabled();
    },12000);
  }

  function renderChallenge(){
    apiLoading=false;
    if(!window.turnstile){setChallengeState('Security check unavailable. Tap here to retry.',false);return;}
    captchaToken='';
    syncLoginEnabled();
    setChallengeState('Complete the security check to sign in.',false);
    try{
      if(widgetId!==null){try{window.turnstile.remove(widgetId);}catch(_){}widgetId=null;}
      challengeHost.innerHTML='';
      widgetId=window.turnstile.render(challengeHost,{
        sitekey:'0x4AAAAAAEZul-Qo6dqMim2U',
        theme:'dark',
        size:'flexible',
        action:'blmorley_auth',
        callback:function(token){
          clearChallengeWatchdog();
          captchaToken=String(token||'');
          syncLoginEnabled();
          setChallengeState('Security check complete.',true);
        },
        'expired-callback':function(){
          captchaToken='';
          syncLoginEnabled();
          setChallengeState('Security check expired. Tap here to retry.',false);
        },
        'error-callback':function(){
          clearChallengeWatchdog();
          captchaToken='';
          syncLoginEnabled();
          setChallengeState('Security check unavailable. Tap here to retry.',false);
          return false;
        }
      });
      armChallengeWatchdog();
    }catch(_){
      clearChallengeWatchdog();
      widgetId=null;
      setChallengeState('Security check unavailable. Tap here to retry.',false);
    }
  }

  function loadChallenge(reason,force){
    captchaToken='';
    syncLoginEnabled();
    setChallengeState(reason||'Security check loading…',false);
    if(window.turnstile&&!force){renderChallenge();return;}
    if(apiLoading)return;
    apiLoading=true;
    const old=document.getElementById('morleyAdminTurnstileApi');
    if(old)old.remove();
    const script=document.createElement('script');
    script.id='morleyAdminTurnstileApi';
    script.async=true;
    script.defer=true;
    script.src='https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit&_='+Date.now();
    script.onload=function(){window.turnstile?renderChallenge():setChallengeState('Security check unavailable. Tap here to retry.',false);};
    script.onerror=function(){
      apiLoading=false;
      clearChallengeWatchdog();
      setChallengeState('Security check unavailable. Tap here to retry.',false);
    };
    document.head.appendChild(script);
    armChallengeWatchdog();
  }

  function resetChallenge(reason){
    captchaToken='';
    syncLoginEnabled();
    setChallengeState(reason||'Reloading security check…',false);
    if(window.turnstile&&widgetId!==null){
      try{window.turnstile.reset(widgetId);armChallengeWatchdog();return;}catch(_){}
    }
    widgetId=null;
    loadChallenge(reason||'Reloading security check…',true);
  }

  emailInput.readOnly=false;
  emailInput.disabled=false;
  passwordInput.readOnly=false;
  passwordInput.disabled=false;
  emailInput.style.pointerEvents='auto';
  passwordInput.style.pointerEvents='auto';

  emailInput.addEventListener('input',syncLoginEnabled);
  passwordInput.addEventListener('input',syncLoginEnabled);
  emailInput.addEventListener('change',syncLoginEnabled);
  passwordInput.addEventListener('change',syncLoginEnabled);
  challengeStatus.addEventListener('click',function(){if(!captchaToken)resetChallenge('Retrying security check…');});

  syncLoginEnabled();
  loadChallenge('Security check loading…');

  loginBtn.onclick=async function(){
    const email=emailInput.value.trim();
    const password=passwordInput.value;
    if(!credentialsReady()){loginStatus.textContent='Enter a valid email and password.';syncLoginEnabled();return;}
    if(!captchaToken){loginStatus.textContent='Complete the security check first.';resetChallenge('Complete the security check to sign in.');return;}
    const token=captchaToken;
    busy=true;
    syncLoginEnabled();
    loginStatus.textContent='Signing in…';
    try{
      const {error}=await sb.auth.signInWithPassword({email,password,options:{captchaToken:token}});
      if(error){loginStatus.textContent=error.message;resetChallenge('Complete a new security check to retry.');return;}
      await loadSession();
    }catch(error){
      loginStatus.textContent=error&&error.message?error.message:'Sign in failed. Please retry.';
      resetChallenge('Complete a new security check to retry.');
    }finally{
      busy=false;
      syncLoginEnabled();
    }
  };
})();
