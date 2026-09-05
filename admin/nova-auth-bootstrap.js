(()=>{'use strict';
const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co';
const SUPABASE_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
const $=id=>document.getElementById(id);
const withTimeout=(promise,ms,label)=>Promise.race([promise,new Promise((_,reject)=>setTimeout(()=>reject(new Error(`${label} timed out`)),ms))]);
const sleep=ms=>new Promise(resolve=>setTimeout(resolve,ms));
function status(message){const el=$('aiAuthStatus');if(el&&el.textContent!==message)el.textContent=message}
function loadScript(src){return new Promise((resolve,reject)=>{const s=document.createElement('script');s.src=src;s.async=false;s.onload=resolve;s.onerror=()=>reject(new Error(`Could not load ${src}`));document.body.appendChild(s)})}
async function getSessionSafely(){let lastError=null;for(let attempt=0;attempt<2;attempt++){try{return await withTimeout(window.sb.auth.getSession(),7000,'Admin session check')}catch(error){lastError=error;if(attempt===0)await sleep(250)}}throw lastError||new Error('Admin session check failed')}
async function authorise(){try{
 if(!window.supabase)throw new Error('Supabase client did not load');
 window.sb=window.supabase.createClient(SUPABASE_URL,SUPABASE_KEY);
 status('Checking your Admin Control session.');
 const {data:{session},error:sessionError}=await getSessionSafely();
 if(sessionError)throw sessionError;
 if(!session){status('Sign in through Admin Control first.');return}
 status('Verifying Admin access…');
 const profileResult=await withTimeout(window.sb.from('profiles').select('role,is_enabled').eq('id',session.user.id).single(),7000,'Admin profile check');
 const {data:profile,error:profileError}=profileResult;
 if(profileError||!profile?.is_enabled||!['admin','manager'].includes(profile.role)){status('This account is not authorised for Nova AI.');return}
 status('Starting Nova AI…');
 document.documentElement.dataset.novaAuthorised='true';
 $('aiAuth')?.classList.add('hidden');
 $('aiWorkspace')?.classList.remove('hidden');
 for(const src of ['morley-ai-core.js?v=1','nova-brand.js?v=1','morley-ai-ui.js?v=2','nova-training-ui.js?v=1','nova-lifecycle-ui.js?v=2','nova-knowledge-ui.js?v=1','nova-control-centre.js?v=3'])await loadScript(src);
 window.dispatchEvent(new Event('morley-ai-authorised'));
}catch(error){console.error('Nova authorisation failed',error);status('Nova could not start safely. Return to Admin Control and try again.')}}
if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',authorise,{once:true});else authorise();
})();