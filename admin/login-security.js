(function(){
  const frame=document.getElementById('adminTurnstileFrame');
  const loginBtn=document.getElementById('loginBtn');
  const loginStatus=document.getElementById('loginStatus');
  const challengeStatus=document.getElementById('challengeStatus');
  const emailInput=document.getElementById('email');
  const passwordInput=document.getElementById('password');
  if(!frame||!loginBtn||!loginStatus||!challengeStatus||!emailInput||!passwordInput)return;

  let captchaToken='';
  let busy=false;
  let bootstrapRetries=0;
  let bootstrapTimer=0;
  const MAX_BOOTSTRAP_RETRIES=2;

  function credentialsReady(){return emailInput.value.trim().length>0&&emailInput.checkValidity()&&passwordInput.value.length>0;}
  function syncLoginEnabled(){loginBtn.disabled=busy||!captchaToken||!credentialsReady();}
  function setChallengeState(text,ok){challengeStatus.textContent=text;challengeStatus.style.color=ok?'#25d991':'#8fa6c6';}
  function clearBootstrapTimer(){if(bootstrapTimer){clearTimeout(bootstrapTimer);bootstrapTimer=0;}}
  function armBootstrapWatchdog(){
    clearBootstrapTimer();
    bootstrapTimer=setTimeout(function(){
      if(captchaToken)return;
      if(bootstrapRetries<MAX_BOOTSTRAP_RETRIES){
        bootstrapRetries+=1;
        setChallengeState('Security check is taking too long. Retrying…',false);
        frame.src='turnstile.html?v=3&retry='+Date.now();
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
    frame.src='turnstile.html?v=3&retry='+Date.now();
    armBootstrapWatchdog();
  }

  emailInput.addEventListener('input',syncLoginEnabled);
  passwordInput.addEventListener('input',syncLoginEnabled);
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
      captchaToken='';
      syncLoginEnabled();
      setChallengeState('Security check failed. Reloading…',false);
      setTimeout(function(){resetChallenge('Complete a new security check to retry.');},700);
    }else if(payload.type==='bootstrap-error'){
      if(bootstrapRetries<MAX_BOOTSTRAP_RETRIES){
        bootstrapRetries+=1;
        frame.src='turnstile.html?v=3&retry='+Date.now();
        armBootstrapWatchdog();
      }else{
        clearBootstrapTimer();
        setChallengeState('Security check unavailable. Tap here to retry.',false);
      }
    }
  });

  frame.addEventListener('load',function(){
    if(!captchaToken)setChallengeState('Security check loading…',false);
    syncLoginEnabled();
    armBootstrapWatchdog();
  });
  armBootstrapWatchdog();

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
