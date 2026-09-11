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
  mobileStyle.textContent='@media(max-width:620px){#loginView.auth-card{margin:18px auto 24px!important;padding:18px!important}#loginView.auth-card h2{margin-bottom:10px!important}#loginView.auth-card p{margin-top:0!important;margin-bottom:12px!important}#email,#password{pointer-events:auto!important;user-select:text!important;-webkit-user-select:text!important;-webkit-text-fill-color:#1d2b26!important;caret-color:#0d8463!important;opacity:1!important}}';
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

  function restoreEditable(input){
    input.readOnly=false;
    input.disabled=false;
    input.removeAttribute('readonly');
    input.removeAttribute('disabled');
    input.style.pointerEvents='auto';
    input.style.userSelect='text';
    input.style.webkitUserSelect='text';
  }

  function installMobileInputRecovery(input){
    restoreEditable(input);
    input.addEventListener('focus',function(){restoreEditable(input);});
    input.addEventListener('touchstart',function(){restoreEditable(input);},{passive:true});
    input.addEventListener('beforeinput',function(event){
      if(event.isComposing||event.inputType!=='insertText'||typeof event.data!=='string'||!event.data)return;
      const before=input.value;
      const start=typeof input.selectionStart==='number'?input.selectionStart:before.length;
      const end=typeof input.selectionEnd==='number'?input.selectionEnd:start;
      const text=event.data;
      queueMicrotask(function(){
        if(input.value!==before)return;
        input.value=before.slice(0,start)+text+before.slice(end);
        const caret=start+text.length;
        try{input.setSelectionRange(caret,caret);}catch(_){}
        input.dispatchEvent(new Event('input',{bubbles:true}));
      });
    },true);
  }

  installMobileInputRecovery(emailInput);
  installMobileInputRecovery(passwordInput);

  function loadChallenge(reason){
    if(challengeLoaded&&frame.src&&frame.src!=='about:blank')return;
    challengeLoaded=true;
    captchaToken='';
    setChallengeVisible(true);
    syncLoginEnabled();
    setChallengeState(reason||'Security check loading…',false);
    frame.src='turnstile.html?v=5&load='+Date.now();
  }

  function maybeLoadChallenge(){
    restoreEditable(emailInput);
    restoreEditable(passwordInput);
    syncLoginEnabled();
    if(credentialsReady()&&!challengeLoaded)loadChallenge('Security check loading…');
  }

  function resetChallenge(reason){
    captchaToken='';
    challengeLoaded=true;
    setChallengeVisible(true);
    syncLoginEnabled();
    setChallengeState(reason||'Reloading security check…',false);
    frame.src='turnstile.html?v=5&retry='+Date.now();
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
