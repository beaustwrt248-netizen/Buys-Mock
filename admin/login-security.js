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
  let challengeWatchdog=null;
  let challengeAttempt=0;
  const challengeBase='turnstile.html?v=7&browser=1';

  function credentialsReady(){return emailInput.value.trim().length>0&&emailInput.checkValidity()&&passwordInput.value.length>0;}
  function syncLoginEnabled(){loginBtn.disabled=busy||!captchaToken||!credentialsReady();}
  function setChallengeState(text,ok){challengeStatus.textContent=text;challengeStatus.style.color=ok?'#25d991':'#8fa6c6';}
  function clearChallengeWatchdog(){if(challengeWatchdog){clearTimeout(challengeWatchdog);challengeWatchdog=null;}}
  function challengeUrl(reason){challengeAttempt+=1;return `${challengeBase}&${reason||'load'}=${Date.now()}-${challengeAttempt}`;}
  function armChallengeWatchdog(){
    clearChallengeWatchdog();
    challengeWatchdog=setTimeout(function(){
      if(captchaToken)return;
      setChallengeState('Security check unavailable. Tap here to retry.',false);
      syncLoginEnabled();
    },12000);
  }
  function loadChallenge(reason){
    captchaToken='';
    syncLoginEnabled();
    setChallengeState(reason||'Security check loading…',false);
    frame.src=challengeUrl('load');
    armChallengeWatchdog();
  }

  window.addEventListener('message',function(event){
    if(event.source!==frame.contentWindow)return;
    if(event.origin!==window.location.origin)return;
    const payload=event.data;
    if(!payload||payload.source!=='morley-turnstile')return;

    if(payload.type==='ready'){
      setChallengeState('Complete the security check to sign in.',false);
      return;
    }
    if(payload.type==='token'){
      clearChallengeWatchdog();
      captchaToken=String(payload.value||'');
      syncLoginEnabled();
      setChallengeState(captchaToken?'Security check complete.':'Complete the security check to sign in.',!!captchaToken);
      return;
    }
    if(payload.type==='expired'){
      clearChallengeWatchdog();
      captchaToken='';
      syncLoginEnabled();
      setChallengeState('Security check expired. Tap here to retry.',false);
      return;
    }
    if(payload.type==='error'||payload.type==='bootstrap-error'){
      clearChallengeWatchdog();
      captchaToken='';
      syncLoginEnabled();
      setChallengeState('Security check unavailable. Tap here to retry.',false);
    }
  });

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
  challengeStatus.addEventListener('click',function(){if(!captchaToken)loadChallenge('Retrying security check…');});

  syncLoginEnabled();
  loadChallenge('Security check loading…');

  loginBtn.onclick=async function(){
    const email=emailInput.value.trim();
    const password=passwordInput.value;
    if(!credentialsReady()){loginStatus.textContent='Enter a valid email and password.';syncLoginEnabled();return;}
    if(!captchaToken){loginStatus.textContent='Complete the security check first.';loadChallenge('Complete the security check to sign in.');return;}
    const token=captchaToken;
    busy=true;
    syncLoginEnabled();
    loginStatus.textContent='Signing in…';
    try{
      const {error}=await sb.auth.signInWithPassword({email,password,options:{captchaToken:token}});
      if(error){loginStatus.textContent=error.message;loadChallenge('Complete a new security check to retry.');return;}
      await loadSession();
    }catch(error){
      loginStatus.textContent=error&&error.message?error.message:'Sign in failed. Please retry.';
      loadChallenge('Complete a new security check to retry.');
    }finally{
      busy=false;
      syncLoginEnabled();
    }
  };
})();
