'use strict';
const assert=require('node:assert/strict');
const cp=require('node:child_process');
const fs=require('node:fs');
const net=require('node:net');
const os=require('node:os');
const path=require('node:path');
const {pathToFileURL}=require('node:url');

const ROOT=path.resolve(__dirname,'..');
const WIDTHS=[360,390,430,768,1440];
const HEIGHT=900;
const CDP_TIMEOUT=7000;

function chromeBinary(){
  for(const name of ['google-chrome','google-chrome-stable','chromium','chromium-browser']){
    const r=cp.spawnSync('which',[name],{encoding:'utf8'});
    if(r.status===0&&r.stdout.trim())return r.stdout.trim();
  }
  return null;
}
function freePort(){return new Promise((resolve,reject)=>{const s=net.createServer();s.unref();s.on('error',reject);s.listen(0,'127.0.0.1',()=>{const p=s.address().port;s.close(()=>resolve(p))})})}
function sleep(ms){return new Promise(r=>setTimeout(r,ms))}
function withTimeout(promise,ms,label){let timer;return Promise.race([promise,new Promise((_,reject)=>{timer=setTimeout(()=>reject(new Error(`${label} timed out after ${ms}ms`)),ms);timer.unref?.()})]).finally(()=>clearTimeout(timer))}
async function json(url,options){return withTimeout((async()=>{const r=await fetch(url,options);if(!r.ok)throw new Error(`${r.status} ${url}`);return r.json()})(),5000,`HTTP ${url}`)}
async function waitForVersion(port){let last;for(let i=0;i<50;i++){try{return await json(`http://127.0.0.1:${port}/json/version`)}catch(e){last=e;await sleep(100)}}throw last||new Error('Chrome DevTools endpoint unavailable')}

class Cdp{
  constructor(url){this.url=url;this.ws=null;this.seq=0;this.pending=new Map();this.events=[]}
  async open(){this.ws=new WebSocket(this.url);await withTimeout(new Promise((resolve,reject)=>{this.ws.addEventListener('open',resolve,{once:true});this.ws.addEventListener('error',reject,{once:true})}),CDP_TIMEOUT,'CDP WebSocket open');this.ws.addEventListener('message',e=>{const msg=JSON.parse(String(e.data));if(msg.id){const p=this.pending.get(msg.id);if(!p)return;this.pending.delete(msg.id);clearTimeout(p.timer);msg.error?p.reject(new Error(msg.error.message||JSON.stringify(msg.error))):p.resolve(msg.result||{})}else if(msg.method){this.events.push(msg)}});this.ws.addEventListener('close',()=>{for(const [id,p] of this.pending){clearTimeout(p.timer);p.reject(new Error(`CDP socket closed before response ${id}`))}this.pending.clear()})}
  call(method,params={}){const id=++this.seq;return new Promise((resolve,reject)=>{const timer=setTimeout(()=>{this.pending.delete(id);reject(new Error(`${method} timed out after ${CDP_TIMEOUT}ms`))},CDP_TIMEOUT);timer.unref?.();this.pending.set(id,{resolve,reject,timer});try{this.ws.send(JSON.stringify({id,method,params}))}catch(error){clearTimeout(timer);this.pending.delete(id);reject(error)}})}
  async waitEvent(method,timeout=5000){const start=Date.now();while(Date.now()-start<timeout){const i=this.events.findIndex(e=>e.method===method);if(i>=0)return this.events.splice(i,1)[0];await sleep(20)}throw new Error(`Timed out waiting for ${method}`)}
  async close(){if(!this.ws)return;for(const [id,p] of this.pending){clearTimeout(p.timer);p.reject(new Error(`CDP session closed before response ${id}`))}this.pending.clear();if(this.ws.readyState===WebSocket.CLOSED)return;await Promise.race([new Promise(resolve=>{this.ws.addEventListener('close',resolve,{once:true});try{this.ws.close()}catch{resolve()}}),sleep(500)])}
}

async function newTarget(port){
  const target=await json(`http://127.0.0.1:${port}/json/new?about:blank`,{method:'PUT'});
  const cdp=new Cdp(target.webSocketDebuggerUrl);
  await cdp.open();
  await cdp.call('Page.enable');
  await cdp.call('Runtime.enable');
  await cdp.call('Network.enable');
  await cdp.call('Network.setBlockedURLs',{urls:['*.js','*.mjs']});
  return cdp;
}
async function inspect(cdp,file,width,label,expectedText){
  await cdp.call('Emulation.setDeviceMetricsOverride',{width,height:HEIGHT,deviceScaleFactor:1,mobile:width<=430,screenWidth:width,screenHeight:HEIGHT});
  const url=pathToFileURL(path.join(ROOT,file)).href;
  await cdp.call('Page.navigate',{url});
  await cdp.waitEvent('Page.loadEventFired',3000).catch(()=>{});
  await sleep(200);
  const result=await cdp.call('Runtime.evaluate',{returnByValue:true,expression:`(()=>{document.documentElement.classList.add('nova-auth-unlocked');const de=document.documentElement,b=document.body;return {title:document.title,innerWidth:window.innerWidth,clientWidth:de.clientWidth,scrollWidth:Math.max(de.scrollWidth,b?.scrollWidth||0),bodyWidth:b?.getBoundingClientRect().width||0,bodyText:(b?.innerText||'').trim().slice(0,1600),bodyVisibility:getComputedStyle(b).visibility,bodyDisplay:getComputedStyle(b).display}})()`});
  const v=result.result.value,semantic=`${v.title||''} ${v.bodyText||''}`.toLowerCase();
  assert.equal(v.innerWidth,width,`${label} emulated viewport should apply at ${width}px`);
  assert.ok(v.clientWidth>0&&v.clientWidth<=v.innerWidth,`${label} layout viewport should fit the emulated viewport at ${width}px`);
  assert.notEqual(v.bodyDisplay,'none',`${label} body must be displayed at ${width}px`);
  assert.notEqual(v.bodyVisibility,'hidden',`${label} body must be visible at ${width}px`);
  assert.ok(v.bodyText.length>20,`${label} should not render blank at ${width}px`);
  assert.ok(semantic.includes(String(expectedText).toLowerCase()),`${label} should expose expected product text at ${width}px`);
  assert.ok(v.bodyWidth<=v.clientWidth+2,`${label} body exceeds layout viewport at ${width}px: ${v.bodyWidth}px > ${v.clientWidth}px`);
  assert.ok(v.scrollWidth<=v.clientWidth+2,`${label} root horizontally overflows at ${width}px: ${v.scrollWidth}px > ${v.clientWidth}px`);
  const shot=await cdp.call('Page.captureScreenshot',{format:'png',fromSurface:true,captureBeyondViewport:false});
  assert.ok((shot.data||'').length>2500,`${label} screenshot should be non-empty at ${width}px`);
}
async function stopChrome(proc){if(!proc||proc.exitCode!==null)return;proc.kill('SIGTERM');await Promise.race([new Promise(resolve=>proc.once('exit',resolve)),sleep(1200)]);if(proc.exitCode===null){proc.kill('SIGKILL');await Promise.race([new Promise(resolve=>proc.once('exit',resolve)),sleep(800)])}}
async function run(){
  if(typeof WebSocket!=='function')throw new Error('Node WebSocket support is required for real browser smoke tests');
  const chrome=chromeBinary();
  if(!chrome){if(process.env.GITHUB_ACTIONS==='true')throw new Error('Google Chrome is required on the GitHub Actions runner');console.log('SKIP browser responsive smoke: Chrome is not installed locally');return}
  const port=await freePort(),profile=fs.mkdtempSync(path.join(os.tmpdir(),'morley-browser-smoke-'));
  const proc=cp.spawn(chrome,[`--remote-debugging-port=${port}`,'--remote-debugging-address=127.0.0.1','--headless=new','--no-sandbox','--disable-gpu','--disable-dev-shm-usage','--allow-file-access-from-files','--no-first-run','--no-default-browser-check',`--user-data-dir=${profile}`,'about:blank'],{stdio:['ignore','ignore','pipe']});
  let stderr='';proc.stderr.on('data',d=>{stderr+=String(d)});
  try{
    await waitForVersion(port);
    for(const [file,label,expectedText] of [['nova/index.html','Nova static shell','Nova'],['admin/index.html','Admin sign-in shell','Admin']]){
      const cdp=await newTarget(port);
      try{for(const width of WIDTHS)await inspect(cdp,file,width,label,expectedText)}finally{await cdp.close()}
    }
    console.log(`Browser responsive smoke passed at ${WIDTHS.join(', ')}px for Nova and Admin static shells`);
  }catch(e){throw new Error(`${e.message}\nChrome stderr: ${stderr.slice(-2000)}`)}finally{
    await stopChrome(proc);
    try{fs.rmSync(profile,{recursive:true,force:true})}catch{}
  }
}
run().catch(e=>{console.error(e.stack||e);process.exitCode=1});
