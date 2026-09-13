(()=>{
'use strict';
const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co';
const API_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
const STORE='morley_web_auth';
const CATALOGUE_STORE='morley-central-pricing-v2';
const SITE_BASE=new URL('./',document.baseURI).href;
const ROTATE_MS=6000;
const IMAGE_TIMEOUT_MS=3000;
const MAX_SLIDES=12;
const CHALLENGE_DELAY_MS=450;
const $=(s,r=document)=>r.querySelector(s);
let captchaToken='';

function loadSession(){try{return JSON.parse(localStorage.getItem(STORE)||'null')}catch{return null}}
function saveSession(s){localStorage.setItem(STORE,JSON.stringify(s))}
function clearSession(){localStorage.removeItem(STORE)}
function jwtSub(token){try{const p=token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/');return JSON.parse(atob(p+'='.repeat((4-p.length%4)%4))).sub||''}catch{return''}}
async function jsonFetch(url,opt={}){const r=await fetch(url,opt);let body={};try{body=await r.json()}catch{}if(!r.ok){const msg=body.error_description||body.msg||body.message||body.error||`Request failed (${r.status})`;throw new Error(msg)}return body}
async function verifyAuthorised(token){const id=jwtSub(token);if(!id)return false;const r=await fetch(`${SUPABASE_URL}/rest/v1/profiles?select=is_enabled&id=eq.${encodeURIComponent(id)}`,{headers:{apikey:API_KEY,Authorization:`Bearer ${token}`}});if(!r.ok)return false;const a=await r.json();return !!(a[0]&&a[0].is_enabled)}
async function refreshSession(s){if(!s?.refresh_token)return null;const b=await jsonFetch(`${SUPABASE_URL}/auth/v1/token?grant_type=refresh_token`,{method:'POST',headers:{'Content-Type':'application/json',apikey:API_KEY},body:JSON.stringify({refresh_token:s.refresh_token})});if(!(await verifyAuthorised(b.access_token)))throw new Error('This account is no longer authorised for B&L Morley.');const n={access_token:b.access_token,refresh_token:b.refresh_token||s.refresh_token,expires_at:Date.now()+((b.expires_in||3600)*1000),email:s.email||''};saveSession(n);return n}
async function validSession(){let s=loadSession();if(!s)return null;if(s.access_token&&(!s.expires_at||s.expires_at>Date.now()+60000)){if(await verifyAuthorised(s.access_token))return s;clearSession();return null}try{return await refreshSession(s)}catch{clearSession();return null}}
function authPayload(obj){if(!captchaToken)throw new Error('Complete the security check first.');return {...obj,gotrue_meta_security:{captcha_token:captchaToken}}}
async function signIn(email,password){const b=await jsonFetch(`${SUPABASE_URL}/auth/v1/token?grant_type=password`,{method:'POST',headers:{'Content-Type':'application/json',apikey:API_KEY},body:JSON.stringify(authPayload({email:email.trim(),password}))});if(!(await verifyAuthorised(b.access_token)))throw new Error('This account is not authorised for B&L Morley. Contact an administrator.');saveSession({access_token:b.access_token,refresh_token:b.refresh_token||'',expires_at:Date.now()+((b.expires_in||3600)*1000),email:email.trim().toLowerCase()})}
async function signUp(email,password,inviteCode){if(password.length<10)throw new Error('Password must be at least 10 characters.');if(!captchaToken)throw new Error('Complete the security check first.');await jsonFetch(`${SUPABASE_URL}/functions/v1/redeem-app-invite`,{method:'POST',headers:{'Content-Type':'application/json',apikey:API_KEY},body:JSON.stringify({email:email.trim(),password,inviteCode:inviteCode.trim(),captchaToken})})}
async function resetPassword(email){await jsonFetch(`${SUPABASE_URL}/auth/v1/recover?redirect_to=${encodeURIComponent(SITE_BASE)}`,{method:'POST',headers:{'Content-Type':'application/json',apikey:API_KEY},body:JSON.stringify(authPayload({email:email.trim()}))})}
function friendly(e){const m=String(e?.message||e||'Something went wrong.');const n=m.toLowerCase();if(n.includes('invalid login credentials'))return'Incorrect email address or password.';if(n.includes('captcha'))return'Security check failed or expired. Complete it again.';if(n.includes('rate')||n.includes('too many'))return'Too many attempts. Please wait a few minutes and try again.';return m}
function cachedCatalogueImages(){try{const rows=JSON.parse(localStorage.getItem(CATALOGUE_STORE)||'[]');if(!Array.isArray(rows))return[];const seen=new Set(),images=[];for(const row of rows){const url=String(row?.imageUrl||'').trim();if(!url||seen.has(url)||!/^https:\/\//i.test(url))continue;seen.add(url);images.push(url);if(images.length>=MAX_SLIDES)break}return images}catch{return[]}}
function preload(url){return new Promise(resolve=>{const img=new Image();let done=false;const timer=setTimeout(()=>finish(false),IMAGE_TIMEOUT_MS);const finish=ok=>{if(done)return;done=true;clearTimeout(timer);img.onload=null;img.onerror=null;resolve(ok)};img.onload=()=>finish(true);img.onerror=()=>finish(false);img.decoding='async';img.src=url;if(img.complete)finish(img.naturalWidth>0)})}
function prefersReducedMotion(){return !!window.matchMedia?.('(prefers-reduced-motion: reduce)').matches}

function styles(){
  const s=document.createElement('style');
  s.textContent=`
#morleyWebAuth{position:fixed;inset:0;z-index:99999;display:flex;align-items:flex-end;justify-content:center;min-height:100dvh;background:#030712;color:#fff;overflow:hidden;font-family:Inter,system-ui,-apple-system,Segoe UI,sans-serif;padding:clamp(16px,3vw,28px)}
#morleyWebAuth .wa-media{position:absolute;inset:0;background:#030712;overflow:hidden}
#morleyWebAuth .wa-slide,#morleyWebAuth .wa-video{position:absolute;inset:0;width:100%;height:100%;object-fit:cover;object-position:center;opacity:0;transition:opacity 1.05s ease;transform:scale(1.015);will-change:opacity}
#morleyWebAuth .wa-slide.active,#morleyWebAuth .wa-video.active{opacity:1}
#morleyWebAuth .wa-video{filter:brightness(.82) saturate(.96)}
#morleyWebAuth .wa-shade{position:absolute;inset:0;background:linear-gradient(180deg,rgba(3,7,18,.14) 0%,rgba(3,7,18,.24) 36%,rgba(3,7,18,.74) 72%,rgba(3,7,18,.94) 100%);pointer-events:none}
#morleyWebAuth .wa-brand-lockup{position:absolute;top:max(28px,env(safe-area-inset-top));left:50%;transform:translateX(-50%);z-index:2;text-align:center;text-shadow:0 2px 18px rgba(0,0,0,.45);white-space:nowrap}
#morleyWebAuth .wa-brand{font-size:clamp(30px,7vw,42px);font-weight:950;letter-spacing:-1.2px;line-height:1}
#morleyWebAuth .wa-brand-sub{margin-top:7px;font-size:11px;font-weight:800;letter-spacing:.16em;text-transform:uppercase;color:rgba(255,255,255,.82)}
#morleyWebAuth .wa-card{position:relative;z-index:3;width:min(460px,100%);max-height:min(72dvh,690px);overflow:auto;overscroll-behavior:contain;padding:22px 20px calc(18px + env(safe-area-inset-bottom));border-radius:26px;background:linear-gradient(160deg,rgba(8,17,32,.70),rgba(5,12,25,.84));border:1px solid rgba(255,255,255,.15);box-shadow:0 24px 72px rgba(0,0,0,.48);backdrop-filter:blur(20px) saturate(1.12);-webkit-backdrop-filter:blur(20px) saturate(1.12)}
#morleyWebAuth .wa-title{font-size:27px;font-weight:900;margin:0 0 15px;letter-spacing:-.5px}
#morleyWebAuth label{display:block;margin:10px 0 6px;color:#d7e0ee;font-size:12px;font-weight:800}
#morleyWebAuth input{box-sizing:border-box;width:100%;min-height:49px;padding:13px 14px;background:rgba(4,16,36,.82);border:1px solid rgba(255,255,255,.17);border-radius:14px;color:#fff;outline:none;font:inherit}
#morleyWebAuth input::placeholder{color:#8290a5}
#morleyWebAuth input:focus{border-color:#63d9ff;box-shadow:0 0 0 3px rgba(18,201,255,.13)}
#morleyWebAuth .wa-security{display:none;margin-top:10px;border-radius:14px;overflow:hidden;background:#111;min-height:0}
#morleyWebAuth .wa-security.active{display:block}
#morleyWebAuth iframe{display:block;width:100%;height:104px;border:0;background:#111;margin:0}
#morleyWebAuth button{width:100%;min-height:48px;padding:13px 15px;border-radius:14px;border:1px solid #2f7cff;background:linear-gradient(120deg,#1f5fd8,#2f7cff 55%,#12c9ff);color:#fff;font-weight:950;cursor:pointer;margin-top:9px;font-size:15px;box-shadow:0 8px 24px rgba(47,124,255,.18)}
#morleyWebAuth button:disabled{opacity:.5;cursor:not-allowed}
#morleyWebAuth button.secondary{background:rgba(10,27,51,.78);color:#e8f2ff;border-color:rgba(255,255,255,.16);box-shadow:none}
#morleyWebAuth button.link{min-height:40px;background:transparent;border:0;color:#d6e3f4;margin-top:3px;padding:8px;box-shadow:none}
#morleyWebAuth .wa-msg{min-height:20px;margin-top:7px;font-size:13px;color:#ffabb5}
#morleyWebAuth .wa-msg.ok{color:#73efb0}
#morleyWebAuth .wa-hint{font-size:12px;color:#9eb0c8;margin-top:8px;line-height:1.35}
#morleyWebAuth .wa-note{font-size:11px;color:#aebbd0;margin-top:10px;line-height:1.45;text-align:center}
#morleyWebAuth .wa-dots{display:flex;justify-content:center;gap:6px;margin:2px 0 12px;min-height:5px}
#morleyWebAuth .wa-dot{width:5px;height:5px;border-radius:999px;background:rgba(255,255,255,.38);transition:width .3s ease,background .3s ease}
#morleyWebAuth .wa-dot.active{width:18px;background:#fff}
#morleyWebAuth .hide{display:none!important}
.morley-web-signout{position:fixed!important;top:18px!important;right:22px!important;z-index:99990!important;width:auto!important;border:1px solid rgba(18,201,255,.55)!important;background:#081a31!important;color:#dff8ff!important;border-radius:999px!important;padding:10px 16px!important;font-size:13px!important;font-weight:900!important;box-shadow:0 8px 24px rgba(0,0,0,.3)!important;cursor:pointer!important}
@media(min-width:760px){#morleyWebAuth{align-items:center;justify-content:flex-end;padding-right:6vw}#morleyWebAuth .wa-brand-lockup{left:6vw;top:50%;transform:translateY(-50%);text-align:left}#morleyWebAuth .wa-brand{font-size:52px}#morleyWebAuth .wa-brand-sub{font-size:12px}#morleyWebAuth .wa-card{padding:28px;max-height:calc(100dvh - 56px)}#morleyWebAuth .wa-shade{background:linear-gradient(90deg,rgba(3,7,18,.20),rgba(3,7,18,.30) 44%,rgba(3,7,18,.82) 76%,rgba(3,7,18,.94))}}
@media(max-width:600px){#morleyWebAuth{padding:12px 12px max(12px,env(safe-area-inset-bottom))}#morleyWebAuth .wa-card{width:100%;max-height:78dvh;padding:18px 17px calc(15px + env(safe-area-inset-bottom));border-radius:23px}#morleyWebAuth .wa-title{font-size:25px;margin-bottom:10px}#morleyWebAuth iframe{height:100px}#morleyWebAuth .wa-slide,#morleyWebAuth .wa-video{transform:none}.morley-web-signout{top:10px!important;right:10px!important;padding:8px 12px!important}}
@media(max-height:700px) and (max-width:600px){#morleyWebAuth .wa-brand-lockup{top:max(15px,env(safe-area-inset-top))}#morleyWebAuth .wa-brand{font-size:28px}#morleyWebAuth .wa-brand-sub{display:none}#morleyWebAuth .wa-card{max-height:82dvh;padding-top:15px}#morleyWebAuth iframe{height:96px}}
@media(prefers-reduced-motion:reduce){#morleyWebAuth .wa-slide,#morleyWebAuth .wa-video,#morleyWebAuth .wa-dot{transition:none!important;transform:none!important}}
`;
  document.head.appendChild(s);
}

function markup(){
  const root=document.createElement('div');
  root.id='morleyWebAuth';
  root.innerHTML=`<div class="wa-media" aria-hidden="true"><img class="wa-slide active" id="waSlideA" alt=""><img class="wa-slide" id="waSlideB" alt=""><video id="waVideo" class="wa-video active" autoplay muted loop playsinline preload="metadata" src="${SITE_BASE}web-assets/morley_buys_login_bg_app.mp4?v=2"></video></div><div class="wa-shade"></div><div class="wa-brand-lockup"><div class="wa-brand">B&L Morley</div><div class="wa-brand-sub">Morley Buys</div></div><div class="wa-card" role="dialog" aria-labelledby="waTitle"><div class="wa-dots" id="waDots" aria-hidden="true"></div><div class="wa-title" id="waTitle">Sign in</div><div id="waInvite" class="hide"><label for="waInviteCode">Invite code</label><input id="waInviteCode" autocomplete="one-time-code"></div><label for="waEmail">Email</label><input id="waEmail" type="email" inputmode="email" autocomplete="email" autocapitalize="none" spellcheck="false"><div id="waPasswords"><label for="waPassword">Password</label><input id="waPassword" type="password" autocomplete="current-password"></div><div id="waConfirm" class="hide"><label for="waConfirmPassword">Confirm password</label><input id="waConfirmPassword" type="password" autocomplete="new-password"></div><div id="waSecurityHint" class="wa-hint">Enter your email and password to continue.</div><div id="waSecurity" class="wa-security" aria-live="polite"><iframe id="waTurnstile" src="about:blank" title="Security check"></iframe></div><div id="waMsg" class="wa-msg" role="status" aria-live="polite"></div><button id="waPrimary" type="button" disabled>Sign in</button><button id="waSignup" type="button" class="secondary">Sign Up with Invite</button><button id="waForgot" type="button" class="link">Forgot Password</button><button id="waBack" type="button" class="link hide">Back to Sign in</button><div class="wa-note">Private B&L Morley system. Access is limited to authorised accounts.</div></div>`;
  document.body.appendChild(root);
  return root;
}

function startCarousel(root){
  const images=cachedCatalogueImages(),video=$('#waVideo',root),a=$('#waSlideA',root),b=$('#waSlideB',root),dots=$('#waDots',root);
  let index=0,current=a,next=b,timer=0,stopped=false,ready=[];
  video.play?.().catch(()=>{});
  const stop=()=>{stopped=true;if(timer)clearInterval(timer);video?.pause?.()};
  if(!images.length)return stop;
  (async()=>{
    const checked=await Promise.all(images.map(async url=>({url,ok:await preload(url)})));
    if(stopped)return;
    ready=checked.filter(x=>x.ok).map(x=>x.url);
    if(!ready.length)return;
    a.src=ready[0];a.classList.add('active');video.classList.remove('active');
    dots.innerHTML=ready.length>1?ready.map((_,i)=>`<span class="wa-dot${i===0?' active':''}"></span>`).join(''):'';
    setTimeout(()=>{if(!stopped)video.pause?.()},1100);
    const syncDots=()=>[...dots.children].forEach((dot,i)=>dot.classList.toggle('active',i===index));
    const advance=async()=>{if(stopped||ready.length<2)return;const nextIndex=(index+1)%ready.length;next.src=ready[nextIndex];const ok=await preload(ready[nextIndex]);if(!ok||stopped)return;next.classList.add('active');current.classList.remove('active');index=nextIndex;syncDots();const old=current;current=next;next=old};
    if(ready.length>1&&!prefersReducedMotion())timer=setInterval(advance,ROTATE_MS);
  })();
  return stop;
}

function addSignOut(){if($('.morley-web-signout'))return;const b=document.createElement('button');b.className='morley-web-signout';b.type='button';b.textContent='Sign out';b.setAttribute('aria-label','Sign out of B&L Morley');b.onclick=()=>{clearSession();location.reload()};document.body.appendChild(b)}

async function init(){
  styles();
  const existing=await validSession();
  if(existing){addSignOut();return}

  const root=markup();
  const stopCarousel=startCarousel(root);
  const title=$('#waTitle',root),msg=$('#waMsg',root),primary=$('#waPrimary',root),signup=$('#waSignup',root),forgot=$('#waForgot',root),back=$('#waBack',root),invite=$('#waInvite',root),confirm=$('#waConfirm',root),passwords=$('#waPasswords',root),security=$('#waSecurity',root),securityHint=$('#waSecurityHint',root),turnstileFrame=$('#waTurnstile',root);
  const emailInput=$('#waEmail',root),passwordInput=$('#waPassword',root),confirmInput=$('#waConfirmPassword',root),inviteInput=$('#waInviteCode',root);
  let mode='signin',challengeTimer=0,challengeFingerprint='',challengeStarted=false,busy=false;

  const requiredReady=()=>{
    const email=emailInput.value.trim();
    if(!email)return false;
    if(mode==='reset')return true;
    if(!passwordInput.value)return false;
    if(mode==='signup')return !!inviteInput.value.trim()&&!!confirmInput.value;
    return true;
  };
  const fingerprint=()=>mode==='signin'?`${mode}|${emailInput.value.trim().toLowerCase()}|${passwordInput.value}`:mode==='signup'?`${mode}|${emailInput.value.trim().toLowerCase()}|${passwordInput.value}|${confirmInput.value}|${inviteInput.value.trim()}`:`${mode}|${emailInput.value.trim().toLowerCase()}`;
  const refreshPrimary=()=>{primary.disabled=busy||!requiredReady()||!captchaToken};
  const hideChallenge=()=>{security.classList.remove('active');turnstileFrame.src='about:blank';challengeStarted=false;challengeFingerprint='';captchaToken='';refreshPrimary()};
  const startChallenge=()=>{
    if(!requiredReady()){hideChallenge();securityHint.textContent=mode==='reset'?'Enter your email address to continue.':mode==='signup'?'Complete the invite and password fields to continue.':'Enter your email and password to continue.';return}
    const current=fingerprint();
    if(challengeStarted&&challengeFingerprint===current)return;
    captchaToken='';challengeFingerprint=current;challengeStarted=true;
    securityHint.textContent='Complete the security check.';
    security.classList.add('active');
    const url=new URL('admin/turnstile.html',SITE_BASE);
    url.searchParams.set('v','5');
    url.searchParams.set('load','1');
    url.searchParams.set('web_auth',String(Date.now()));
    turnstileFrame.src=url.href;
    refreshPrimary();
  };
  const scheduleChallenge=()=>{
    clearTimeout(challengeTimer);
    if(challengeStarted&&fingerprint()!==challengeFingerprint)hideChallenge();
    refreshPrimary();
    if(!requiredReady()){securityHint.textContent=mode==='reset'?'Enter your email address to continue.':mode==='signup'?'Complete the invite and password fields to continue.':'Enter your email and password to continue.';return}
    challengeTimer=setTimeout(startChallenge,CHALLENGE_DELAY_MS);
  };
  const resetCaptcha=()=>{captchaToken='';challengeStarted=false;challengeFingerprint='';turnstileFrame.src='about:blank';security.classList.remove('active');scheduleChallenge()};

  const setMode=m=>{
    mode=m;msg.textContent='';msg.className='wa-msg';invite.classList.toggle('hide',m!=='signup');confirm.classList.toggle('hide',m!=='signup');passwords.classList.toggle('hide',m==='reset');signup.classList.toggle('hide',m!=='signin');forgot.classList.toggle('hide',m!=='signin');back.classList.toggle('hide',m==='signin');title.textContent=m==='signin'?'Sign in':m==='signup'?'Private sign up':'Forgot password';primary.textContent=m==='signin'?'Sign in':m==='signup'?'Create authorised account':'Send reset email';passwordInput.autocomplete=m==='signup'?'new-password':'current-password';resetCaptcha();
  };

  window.addEventListener('message',e=>{
    if(e.source!==turnstileFrame.contentWindow)return;
    const d=e.data||{};
    if(d.source!=='morley-turnstile')return;
    if(d.type==='token'&&d.value){captchaToken=d.value;msg.textContent='Security check complete.';msg.className='wa-msg ok';securityHint.textContent='Security check complete.'}
    else if(d.type==='expired'){captchaToken='';msg.textContent='Security check expired. Complete it again.';msg.className='wa-msg';}
    else if(d.type==='error'||d.type==='bootstrap-error'){captchaToken='';msg.textContent='Security check unavailable. Tap the security check to retry.';msg.className='wa-msg';}
    refreshPrimary();
  });

  [emailInput,passwordInput,confirmInput,inviteInput].forEach(input=>input.addEventListener('input',scheduleChallenge));
  signup.onclick=()=>setMode('signup');
  forgot.onclick=()=>setMode('reset');
  back.onclick=()=>setMode('signin');

  const submit=async()=>{
    const email=emailInput.value,password=passwordInput.value,confirmPassword=confirmInput.value,code=inviteInput.value;
    if(!email){msg.textContent='Enter your email address.';emailInput.focus();return}
    if(mode!=='reset'&&!password){msg.textContent='Enter your password.';passwordInput.focus();return}
    if(mode==='signup'&&!code.trim()){msg.textContent='Enter your invite code.';inviteInput.focus();return}
    if(mode==='signup'&&password!==confirmPassword){msg.textContent='Passwords do not match.';confirmInput.focus();return}
    if(!captchaToken){msg.textContent='Complete the security check first.';startChallenge();return}
    busy=true;refreshPrimary();msg.className='wa-msg';msg.textContent='Please wait…';
    try{
      if(mode==='signin'){await signIn(email,password);stopCarousel();root.remove();addSignOut()}
      else if(mode==='signup'){await signUp(email,password,code);setMode('signin');msg.className='wa-msg ok';msg.textContent='Account created. You can sign in now.'}
      else{await resetPassword(email);setMode('signin');msg.className='wa-msg ok';msg.textContent='Password reset email sent. Check your inbox.'}
    }catch(e){msg.textContent=friendly(e);msg.className='wa-msg';resetCaptcha()}
    finally{busy=false;refreshPrimary()}
  };

  primary.onclick=submit;
  root.addEventListener('keydown',e=>{if(e.key==='Enter'&&e.target?.tagName==='INPUT'){e.preventDefault();if(!primary.disabled)submit();else scheduleChallenge()}});
  scheduleChallenge();
}

if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();
