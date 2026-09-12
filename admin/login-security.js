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

  let captchaToken='';
  let busy=false;
  let bootstrapRetries=0;
  let bootstrapTimer=0;
  const MAX_BOOTSTRAP_RETRIES=2;
  const challengeUrl=()=>`turnstile.html?v=8&browser=1&retry=${Date.now()}`;

  function credentialsReady(){return emailInput.value.trim().length>0&&emailInput.checkValidity()&&passwordInput.value.length>0;}
  function syncLoginEnabled(){loginBtn.disabled=busy||!captchaToken||!credentialsReady();}
  function setChallengeState(text,ok){challengeStatus.textContent=text;challengeStatus.style.color=ok?'#25d991':'#8fa6c6';}
  function describeChallengeFailure(code){
    const value=String(code||'').trim();
    if(value==='110200')return 'Security check configuration error 110200: Domain not authorised.';
    if(['110100','110110','400020','400070'].includes(value))return `Security check configuration error ${value}.`;
    if(value==='200500')return 'Security check error 200500: Cloudflare challenge could not load. Check browser or network blocking.';
    if(value==='api_unavailable')return 'Security check service unavailable (api_unavailable). Tap here to retry.';
    return value?`Security check failed (${value}). Tap here to retry.`:'Security check failed. Tap here to retry.';
  }
  function clearBootstrapTimer(){if(bootstrapTimer){clearTimeout(bootstrapTimer);bootstrapTimer=0;}}
  function armBootstrapWatchdog(){
    clearBootstrapTimer();
    bootstrapTimer=setTimeout(function(){
      if(captchaToken)return;
      if(bootstrapRetries<MAX_BOOTSTRAP_RETRIES){
        bootstrapRetries+=1;
        setChallengeState('Security check is taking too long. Retrying…',false);
        frame.src=challengeUrl();
        armBootstrapWatchdog();
      }else{
        setChallengeState('Security check unavailable. Tap here to retry.',false);
      }
    },8000);
  }
  function resetChallenge(reason){
    captchaToken='';
    syncLoginEnabled();
    if(reason)setChallengeState(reason,false);
    bootstrapRetries=0;
    frame.src=challengeUrl();
    armBootstrapWatchdog();
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

  window.addEventListener('message',function(event){
    const sameSource=event.source===frame.contentWindow;
    const sameOrigin=event.origin===window.location.origin;
    const opaqueLocalOrigin=window.location.origin==='null'&&event.origin==='null';
    if(!sameSource||(!sameOrigin&&!opaqueLocalOrigin))return;
    const payload=event.data;
    if(!payload||payload.source!=='morley-turnstile')return;
    if(payload.type==='ready'){
      clearBootstrapTimer();
      bootstrapRetries=0;
      if(!captchaToken)setChallengeState('Complete the security check to sign in.',false);
    }else if(payload.type==='token'&&payload.value){
      clearBootstrapTimer();
      bootstrapRetries=0;
      captchaToken=String(payload.value);
      syncLoginEnabled();
      setChallengeState('Security check complete.',true);
    }else if(payload.type==='expired'){
      captchaToken='';
      syncLoginEnabled();
      setChallengeState('Security check expired. Complete it again.',false);
    }else if(payload.type==='error'){
      clearBootstrapTimer();
      captchaToken='';
      syncLoginEnabled();
      setChallengeState(describeChallengeFailure(payload.code),false);
    }else if(payload.type==='bootstrap-error'){
      const code=String(payload.code||'');
      if(code&&code!=='api_unavailable'){
        clearBootstrapTimer();
        captchaToken='';
        syncLoginEnabled();
        setChallengeState(describeChallengeFailure(payload.code),false);
      }else if(bootstrapRetries<MAX_BOOTSTRAP_RETRIES){
        bootstrapRetries+=1;
        frame.src=challengeUrl();
        armBootstrapWatchdog();
      }else{
        clearBootstrapTimer();
        setChallengeState(describeChallengeFailure(payload.code),false);
      }
    }
  });

  frame.addEventListener('load',function(){
    if(frame.src==='about:blank')return;
    if(!captchaToken)setChallengeState('Security check loading…',false);
    syncLoginEnabled();
    armBootstrapWatchdog();
  });
  resetChallenge('Security check loading…');

  loginBtn.onclick=async function(){
    const email=emailInput.value.trim();
    const password=passwordInput.value;
    if(!credentialsReady()){loginStatus.textContent='Enter a valid email and password.';syncLoginEnabled();return;}
    if(!captchaToken){loginStatus.textContent='Complete the security check first.';syncLoginEnabled();return;}
    const token=captchaToken;
    busy=true;
    syncLoginEnabled();
    loginStatus.textContent='Signing in…';
    try{
      const {error}=await sb.auth.signInWithPassword({email,password,options:{captchaToken:token}});
      if(error){loginStatus.textContent=error.message;resetChallenge('Complete a new security check to retry.');return;}
      clearBootstrapTimer();
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
