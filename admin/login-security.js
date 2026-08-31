(function(){
  const frame=document.getElementById('adminTurnstileFrame');
  const loginBtn=document.getElementById('loginBtn');
  const loginStatus=document.getElementById('loginStatus');
  const challengeStatus=document.getElementById('challengeStatus');
  if(!frame||!loginBtn||!loginStatus||!challengeStatus)return;

  let captchaToken='';
  loginBtn.disabled=true;

  function setChallengeState(text,ok){
    challengeStatus.textContent=text;
    challengeStatus.style.color=ok?'#25d991':'#8fa6c6';
  }

  function resetChallenge(reason){
    captchaToken='';
    loginBtn.disabled=true;
    if(reason)setChallengeState(reason,false);
    try{frame.contentWindow.location.reload();}catch(_){frame.src='turnstile.html?retry='+Date.now();}
  }

  window.addEventListener('message',function(event){
    const sameSource=event.source===frame.contentWindow;
    const sameOrigin=event.origin===window.location.origin;
    const opaqueLocalOrigin=window.location.origin==='null'&&event.origin==='null';
    if(!sameSource||(!sameOrigin&&!opaqueLocalOrigin))return;
    const payload=event.data;
    if(!payload||payload.source!=='morley-turnstile')return;
    if(payload.type==='token'&&payload.value){
      captchaToken=String(payload.value);
      loginBtn.disabled=false;
      setChallengeState('Security check complete.',true);
    }else if(payload.type==='expired'){
      captchaToken='';
      loginBtn.disabled=true;
      setChallengeState('Security check expired. Complete it again.',false);
    }else if(payload.type==='error'){
      captchaToken='';
      loginBtn.disabled=true;
      setChallengeState('Security check failed. Reloading…',false);
      setTimeout(function(){resetChallenge('Complete the security check to sign in.');},700);
    }
  });

  frame.addEventListener('load',function(){
    if(!captchaToken)setChallengeState('Complete the security check to sign in.',false);
  });

  loginBtn.onclick=async function(){
    const email=document.getElementById('email').value.trim();
    const password=document.getElementById('password').value;
    if(!email||!password){loginStatus.textContent='Enter your email and password.';return;}
    if(!captchaToken){loginStatus.textContent='Complete the security check first.';return;}

    const token=captchaToken;
    loginBtn.disabled=true;
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
      if(captchaToken)loginBtn.disabled=false;
    }
  };
})();
