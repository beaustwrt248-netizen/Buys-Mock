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

  function credentialsReady(){
    return emailInput.value.trim().length>0&&emailInput.checkValidity()&&passwordInput.value.length>0;
  }

  function syncLoginEnabled(){
    loginBtn.disabled=busy||!captchaToken||!credentialsReady();
  }

  function setChallengeState(text,ok){
    challengeStatus.textContent=text;
    challengeStatus.style.color=ok?'#25d991':'#8fa6c6';
  }

  function resetChallenge(reason){
    captchaToken='';
    syncLoginEnabled();
    if(reason)setChallengeState(reason,false);
    try{frame.contentWindow.location.reload();}catch(_){frame.src='turnstile.html?retry='+Date.now();}
  }

  emailInput.addEventListener('input',syncLoginEnabled);
  passwordInput.addEventListener('input',syncLoginEnabled);
  syncLoginEnabled();

  window.addEventListener('message',function(event){
    const sameSource=event.source===frame.contentWindow;
    const sameOrigin=event.origin===window.location.origin;
    const opaqueLocalOrigin=window.location.origin==='null'&&event.origin==='null';
    if(!sameSource||(!sameOrigin&&!opaqueLocalOrigin))return;
    const payload=event.data;
    if(!payload||payload.source!=='morley-turnstile')return;
    if(payload.type==='token'&&payload.value){
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
      setTimeout(function(){resetChallenge('Complete the security check to sign in.');},700);
    }
  });

  frame.addEventListener('load',function(){
    if(!captchaToken)setChallengeState('Complete the security check to sign in.',false);
    syncLoginEnabled();
  });

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
      if(error){
        loginStatus.textContent=error.message;
        resetChallenge('Complete a new security check to retry.');
        return;
      }
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
