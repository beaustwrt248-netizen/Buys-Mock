(function(){
  const frame=document.getElementById('adminTurnstileFrame');
  const loginBtn=document.getElementById('loginBtn');
  const loginStatus=document.getElementById('loginStatus');
  const challengeStatus=document.getElementById('challengeStatus');
  const emailInput=document.getElementById('email');
  const passwordInput=document.getElementById('password');
  if(!frame||!loginBtn||!loginStatus||!challengeStatus||!emailInput||!passwordInput)return;

  const nativeAuthMode=new URLSearchParams(location.search).get('nativeAuth')==='1';
  if(nativeAuthMode)return;

  let captchaToken='';
  let busy=false;
  let challengeLoaded=false;

  function credentialsReady(){return emailInput.value.trim().length>0&&emailInput.checkValidity()&&passwordInput.value.length>0;}
  function syncLoginEnabled(){loginBtn.disabled=busy||!captchaToken||!credentialsReady();}
  function setChallengeState(text,ok){challengeStatus.textContent=text;challengeStatus.style.color=ok?'#25d991':'#8fa6c6';}

  function loadChallenge(reason,force){
    if(challengeLoaded&&!force)return;
    challengeLoaded=true;
    captchaToken='';
    frame.style.display='block';
    syncLoginEnabled();
    setChallengeState(reason||'Security check loading…',false);
    frame.src='turnstile.html?v=6&load='+Date.now();
  }

  function resetChallenge(reason){
    captchaToken='';
    syncLoginEnabled();
    setChallengeState(reason||'Reloading security check…',false);
    frame.style.display='block';
    frame.src='turnstile.html?v=6&retry='+Date.now();
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
  challengeStatus.addEventListener('click',function(){
    if(!captchaToken)resetChallenge('Retrying security check…');
  });

  window.addEventListener('message',function(event){
    const sameSource=event.source===frame.contentWindow;
    const sameOrigin=event.origin===window.location.origin;
    const opaqueLocalOrigin=window.location.origin==='null'&&event.origin==='null';
    if(!sameSource||(!sameOrigin&&!opaqueLocalOrigin))return;
    const payload=event.data;
    if(!payload||payload.source!=='morley-turnstile')return;
    if(payload.type==='ready'){
      if(!captchaToken)setChallengeState('Complete the security check to sign in.',false);
    }else if(payload.type==='token'&&payload.value){
      captchaToken=String(payload.value);
      syncLoginEnabled();
      setChallengeState('Security check complete.',true);
    }else if(payload.type==='expired'){
      captchaToken='';
      syncLoginEnabled();
      setChallengeState('Security check expired. Tap here to retry.',false);
    }else if(payload.type==='error'||payload.type==='bootstrap-error'){
      captchaToken='';
      syncLoginEnabled();
      setChallengeState('Security check unavailable. Tap here to retry.',false);
    }
  });

  frame.addEventListener('load',function(){
    if(challengeLoaded&&!captchaToken)setChallengeState('Security check loading…',false);
    syncLoginEnabled();
  });

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
