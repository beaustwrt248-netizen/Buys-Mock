(function(){
  const frame=document.getElementById('adminTurnstileFrame');
  const loginBtn=document.getElementById('loginBtn');
  const loginStatus=document.getElementById('loginStatus');
  const challengeStatus=document.getElementById('challengeStatus');
  const emailInput=document.getElementById('email');
  const passwordInput=document.getElementById('password');
  if(!frame||!loginBtn||!loginStatus||!challengeStatus||!emailInput||!passwordInput)return;

  const challengeShell=frame.parentElement;
  const loginView=document.getElementById('loginView');
  const mobileStyle=document.createElement('style');
  mobileStyle.textContent='@media(max-width:620px){#loginView.auth-card{margin:18px auto 24px!important;padding:18px!important}#loginView.auth-card h2{margin-bottom:10px!important}#loginView.auth-card p{margin-top:0!important;margin-bottom:12px!important}}';
  document.head.appendChild(mobileStyle);

  let captchaToken='';
  let busy=false;
  let challengeLoaded=false;

  function credentialsReady(){return emailInput.value.trim().length>0&&emailInput.checkValidity()&&passwordInput.value.length>0;}
  function syncLoginEnabled(){loginBtn.disabled=busy||!captchaToken||!credentialsReady();}
  function setChallengeState(text,ok){challengeStatus.textContent=text;challengeStatus.style.color=ok?'#25d991':'#8fa6c6';}
  function setChallengeVisible(visible){
    if(challengeShell)challengeShell.style.display=visible?'block':'none';
    frame.style.display=visible?'block':'none';
  }

  function loadChallenge(reason){
    if(challengeLoaded&&frame.src&&frame.src!=='about:blank')return;
    challengeLoaded=true;
    captchaToken='';
    setChallengeVisible(true);
    syncLoginEnabled();
    setChallengeState(reason||'Security check loading…',false);
    frame.src='turnstile.html?v=4&load='+Date.now();
  }

  function maybeLoadChallenge(){
    syncLoginEnabled();
    if(credentialsReady()&&!challengeLoaded)loadChallenge('Security check loading…');
  }

  function resetChallenge(reason){
    captchaToken='';
    challengeLoaded=true;
    setChallengeVisible(true);
    syncLoginEnabled();
    setChallengeState(reason||'Reloading security check…',false);
    frame.src='turnstile.html?v=4&retry='+Date.now();
  }

  // Keep Cloudflare completely out of the WebView while credentials are being
  // entered, but do not leave an empty black challenge box on screen.
  try{frame.src='about:blank';}catch(_){}
  setChallengeVisible(false);
  setChallengeState('Enter your email and password first.',false);
  if(loginView)loginView.style.scrollMarginTop='12px';

  emailInput.addEventListener('input',maybeLoadChallenge);
  passwordInput.addEventListener('input',maybeLoadChallenge);
  emailInput.addEventListener('change',maybeLoadChallenge);
  passwordInput.addEventListener('change',maybeLoadChallenge);
  challengeStatus.addEventListener('click',function(){
    if(!credentialsReady()){
      setChallengeVisible(false);
      setChallengeState('Enter your email and password first.',false);
      return;
    }
    if(!captchaToken)resetChallenge('Retrying security check…');
  });
  syncLoginEnabled();

  window.addEventListener('message',function(event){
    const sameSource=event.source===frame.contentWindow;
    const sameOrigin=event.origin===window.location.origin;
    const opaqueLocalOrigin=window.location.origin==='null'&&event.origin==='null';
    if(!sameSource||(!sameOrigin&&!opaqueLocalOrigin))return;
    const payload=event.data;
    if(!payload||payload.source!=='morley-turnstile')return;
    if(payload.type==='ready'){
      setChallengeVisible(true);
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
    if(challengeLoaded){
      setChallengeVisible(true);
      if(!captchaToken)setChallengeState('Security check loading…',false);
    }
    syncLoginEnabled();
  });

  // Browser/password-manager autofill may not emit input immediately.
  setTimeout(maybeLoadChallenge,350);

  loginBtn.onclick=async function(){
    const email=emailInput.value.trim();
    const password=passwordInput.value;
    if(!credentialsReady()){loginStatus.textContent='Enter a valid email and password.';syncLoginEnabled();return;}
    if(!captchaToken){
      loginStatus.textContent='Complete the security check first.';
      if(!challengeLoaded)loadChallenge('Security check loading…');
      syncLoginEnabled();
      return;
    }
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
