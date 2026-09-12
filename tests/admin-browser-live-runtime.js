'use strict';
const assert=require('node:assert/strict');
const cp=require('node:child_process');
const fs=require('node:fs');
const http=require('node:http');
const os=require('node:os');
const path=require('node:path');

const CDP_TIMEOUT=3500;
function sleep(ms){return new Promise(r=>setTimeout(r,ms))}
function chromeBinary(){for(const name of ['google-chrome','google-chrome-stable','chromium','chromium-browser']){const r=cp.spawnSync('which',[name],{encoding:'utf8'});if(r.status===0&&r.stdout.trim())return r.stdout.trim()}return null}
function withTimeout(promise,ms,label){let timer;return Promise.race([promise,new Promise((_,reject)=>{timer=setTimeout(()=>reject(new Error(`${label} timed out after ${ms}ms`)),ms);timer.unref?.()})]).finally(()=>clearTimeout(timer))}
function json(url,options={}){return withTimeout(new Promise((resolve,reject)=>{const u=new URL(url);const req=http.request({hostname:u.hostname,port:u.port,path:u.pathname+u.search,method:options.method||'GET',headers:{Connection:'close'}},res=>{let body='';res.setEncoding('utf8');res.on('data',c=>body+=c);res.on('end',()=>{if((res.statusCode||500)>=400)return reject(new Error(`${res.statusCode} ${url}`));try{resolve(JSON.parse(body))}catch(e){reject(e)}})});req.on('error',reject);req.setTimeout(3000,()=>req.destroy(new Error(`socket timeout ${url}`)));req.end()}),5000,`HTTP ${url}`)}
async function jsonRetry(url,options={},attempts=8){let last;for(let i=0;i<attempts;i++){try{return await json(url,options)}catch(e){last=e;await sleep(200)}}throw last}
class Cdp{constructor(url){this.url=url;this.ws=null;this.id=0;this.pending=new Map();this.events=[]}async open(){this.ws=new WebSocket(this.url);await withTimeout(new Promise((resolve,reject)=>{this.ws.addEventListener('open',resolve,{once:true});this.ws.addEventListener('error',reject,{once:true})}),CDP_TIMEOUT,'CDP open');this.ws.addEventListener('message',e=>{const msg=JSON.parse(String(e.data));if(msg.id){const p=this.pending.get(msg.id);if(!p)return;this.pending.delete(msg.id);clearTimeout(p.timer);msg.error?p.reject(new Error(msg.error.message||JSON.stringify(msg.error))):p.resolve(msg.result||{})}else if(msg.method)this.events.push(msg)})}call(method,params={}){const id=++this.id;return new Promise((resolve,reject)=>{const timer=setTimeout(()=>{this.pending.delete(id);reject(new Error(`${method} timeout`))},CDP_TIMEOUT);timer.unref?.();this.pending.set(id,{resolve,reject,timer});this.ws.send(JSON.stringify({id,method,params}))})}async close(){try{this.ws?.close()}catch{}}}
function startChrome(chrome){const profile=fs.mkdtempSync(path.join(os.tmpdir(),'morley-admin-live-'));const proc=cp.spawn(chrome,['--remote-debugging-port=0','--remote-debugging-address=127.0.0.1','--headless=new','--no-sandbox','--disable-gpu','--disable-dev-shm-usage','--no-first-run','--no-default-browser-check',`--user-data-dir=${profile}`,'about:blank'],{stdio:['ignore','ignore','pipe']});let stderr='';proc.stderr.on('data',d=>stderr+=String(d));return {profile,proc,getStderr:()=>stderr}}
async function waitPort(started){const begin=Date.now();while(Date.now()-begin<10000){const m=started.getStderr().match(/DevTools listening on ws:\/\/(?:127\.0\.0\.1|localhost):(\d+)\//);if(m)return Number(m[1]);if(started.proc.exitCode!==null)throw new Error(`Chrome exited early: ${started.getStderr().slice(-1000)}`);await sleep(80)}throw new Error(`DevTools startup timeout: ${started.getStderr().slice(-1000)}`)}
async function stop(started){if(started.proc.exitCode===null){started.proc.kill('SIGTERM');await sleep(250);if(started.proc.exitCode===null)started.proc.kill('SIGKILL')}try{fs.rmSync(started.profile,{recursive:true,force:true})}catch{}}
async function state(cdp){const r=await cdp.call('Runtime.evaluate',{returnByValue:true,expression:`(()=>({readyState:document.readyState,status:document.getElementById('challengeStatus')?.textContent||null,legacyFrame:!!document.getElementById('adminTurnstileFrame'),widget:!!document.getElementById('adminTurnstileWidget'),fallback:!!document.getElementById('adminTurnstileFallbackFrame'),api:document.getElementById('morleyAdminTurnstileApi')?.src||null,turnstile:typeof window.turnstile,supabase:typeof window.supabase,sb:typeof window.sb,adminV2Menu:!!document.getElementById('adminMoreMenu'),scripts:[...document.scripts].map(s=>s.src).filter(Boolean).map(s=>s.split('/').pop()).slice(-50)}))()`});return r.result.value}
async function scenario(chrome,label,blocked){const started=startChrome(chrome);let cdp;const out={label,blocked};try{const port=await waitPort(started);const target=await jsonRetry(`http://127.0.0.1:${port}/json/new?about:blank`,{method:'PUT'});cdp=new Cdp(target.webSocketDebuggerUrl);await cdp.open();await cdp.call('Page.enable');await cdp.call('Runtime.enable');await cdp.call('Network.enable');if(blocked.length)await cdp.call('Network.setBlockedURLs',{urls:blocked});const url=`https://buyshub.me/admin/?morley_runtime_diag=${Date.now()}-${encodeURIComponent(label)}`;await cdp.call('Page.navigate',{url});await sleep(1400);try{out.state=await state(cdp);out.evaluate='ok'}catch(e){out.evaluate=e.message}out.failures=cdp.events.filter(e=>e.method==='Network.loadingFailed').map(e=>({error:e.params?.errorText,blocked:e.params?.blockedReason,requestId:e.params?.requestId})).slice(-20);out.exceptions=cdp.events.filter(e=>e.method==='Runtime.exceptionThrown').map(e=>({text:e.params?.exceptionDetails?.text,description:e.params?.exceptionDetails?.exception?.description,url:e.params?.exceptionDetails?.url,line:e.params?.exceptionDetails?.lineNumber})).slice(-12)}catch(e){out.error=e.stack||e.message}finally{await cdp?.close();await stop(started)}return out}
async function run(){assert.equal(typeof WebSocket,'function','Node WebSocket support required');const chrome=chromeBinary();assert.ok(chrome,'Chrome/Chromium required');
  const scenarios=[
    ['baseline',[]],
    ['block-audit-triage',['*audit-triage.js*']],
    ['block-control-governance',['*control-governance.js*']],
    ['block-pricing-management',['*pricing-management.js*']],
    ['block-catalogue-live-sync',['*catalogue-live-sync.js*']],
    ['block-control-plus-pricing',['*control-governance.js*','*pricing-management.js*']],
    ['block-control-plus-audit',['*control-governance.js*','*audit-triage.js*']],
    ['block-pricing-plus-audit',['*pricing-management.js*','*audit-triage.js*']]
  ];
  const report=[];for(const [label,blocked] of scenarios)report.push(await scenario(chrome,label,blocked));console.log(JSON.stringify(report,null,2));
  const baseline=report.find(x=>x.label==='baseline');assert.equal(baseline?.evaluate,'ok','baseline must reproduce the live browser stall before this regression test can pass');
}
run().catch(e=>{console.error(e.stack||e);process.exitCode=1});