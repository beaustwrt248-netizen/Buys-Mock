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

function chromeBinary(){
  for(const name of ['google-chrome','google-chrome-stable','chromium','chromium-browser']){
    const r=cp.spawnSync('which',[name],{encoding:'utf8'});
    if(r.status===0&&r.stdout.trim())return r.stdout.trim();
  }
  return null;
}

function freePort(){return new Promise((resolve,reject)=>{const s=net.createServer();s.unref();s.on('error',reject);s.listen(0,'127.0.0.1',()=>{const p=s.address().port;s.close(()=>resolve(p))})})}
function sleep(ms){return new Promise(r=>setTimeout(r,ms))}
async function json(url,options){const r=await fetch(url,options);if(!r.ok)throw new Error(`${r.status} ${url}`);return r.json()}
async function waitForVersion(port){let last;for(let i=0;i<80;i++){try{return await json(`http://127.0.0.1:${port}/json/version`)}catch(e){last=e;await sleep(100)}}throw last||new Error('Chrome DevTools endpoint unavailable')}

class Cdp{
  constructor(url){this.url=url;this.ws=null;this.seq=0;this.pending=new Map();this.events=[]}
  async open(){this.ws=new WebSocket(this.url);await new Promise((resolve,reject)=>{this.ws.addEventListener('open',resolve,{once:true});this.ws.addEventListener('error',reject,{once:true})});this.ws.addEventListener('message',e=>{const msg=JSON.parse(String(e.data));if(msg.id){const p=this.pending.get(msg.id);if(!p)return;this.pending.delete(msg.id);msg.error?p.reject(new Error(msg.error.message||JSON.stringify(msg.error))):p.resolve(msg.result||{})}else if(msg.method){this.events.push(msg)}})}
  call(method,params={}){const id=++this.seq;return new Promise((resolve,reject)=>{this.pending.set(id,{resolve,reject});this.ws.send(JSON.stringify({id,method,params}))})}
  async waitEvent(method,timeout=5000){const start=Date.now();while(Date.now()-start<timeout){const i=this.events.findIndex(e=>e.method===method);if(i>=0)return this.events.splice(i,1)[0];await sleep(20)}throw new Error(`Timed out waiting for ${method}`)}
  close(){try{this.ws?.close()}catch{}}
}

async function newTarget(port){
  const target=await json(`http://127.0.0.1:${port}/json/new?about:blank`,{method:'PUT'});
  const cdp=new Cdp(target.webSocketDebuggerUrl);await cdp.open();await cdp.call('Page.enable');await cdp.call('Runtime.enable');return cdp;
}

async function inspect(cdp,file,width,label,expectedText){
  await cdp.call('Emulation.setDeviceMetricsOverride',{width,height:HEIGHT,deviceScaleFactor:1,mobile:width<=430,screenWidth:width,screenHeight:HEIGHT});
  const url=pathToFileURL(path.join(ROOT,file)).href;
  await cdp.call('Page.navigate',{url});
  await cdp.waitEvent('Page.loadEventFired',5000).catch(()=>{});
  await sleep(700);
  const result=await cdp.call('Runtime.evaluate',{returnByValue:true,expression:`(()=>{const de=document.documentElement,b=document.body;return {title:document.title,clientWidth:de.clientWidth,scrollWidth:Math.max(de.scrollWidth,b?.scrollWidth||0),bodyWidth:b?.getBoundingClientRect().width||0,bodyText:(b?.innerText||'').trim().slice(0,1600),bodyVisibility:getComputedStyle(b).visibility,bodyDisplay:getComputedStyle(b).display}})()`});
  const v=result.result.value;
  const semantic=`${v.title||''} ${v.bodyText||''}`.toLowerCase();
  assert.equal(v.clientWidth,width,`${label} viewport should apply at ${width}px`);
  assert.notEqual(v.bodyDisplay,'none',`${label} body must be displayed at ${width}px`);
  assert.notEqual(v.bodyVisibility,'hidden',`${label} body must be visible at ${width}px`);
  assert.ok(v.bodyText.length>20,`${label} should not render blank at ${width}px`);
  assert.ok(semantic.includes(String(expectedText).toLowerCase()),`${label} should expose expected product text at ${width}px`);
  assert.ok(v.bodyWidth<=width+2,`${label} body exceeds viewport at ${width}px: ${v.bodyWidth}px`);
  assert.ok(v.scrollWidth<=width+2,`${label} root horizontally overflows at ${width}px: ${v.scrollWidth}px`);
  const shot=await cdp.call('Page.captureScreenshot',{format:'png',fromSurface:true,captureBeyondViewport:false});
  assert.ok((shot.data||'').length>2500,`${label} screenshot should be non-empty at ${width}px`);
}

async function run(){
  if(typeof WebSocket!=='function')throw new Error('Node WebSocket support is required for real browser smoke tests');
  const chrome=chromeBinary();
  if(!chrome){if(process.env.GITHUB_ACTIONS==='true')throw new Error('Google Chrome is required on the GitHub Actions runner');console.log('SKIP browser responsive smoke: Chrome is not installed locally');return}
  const port=await freePort();
  const profile=fs.mkdtempSync(path.join(os.tmpdir(),'morley-browser-smoke-'));
  const proc=cp.spawn(chrome,[`--remote-debugging-port=${port}`,'--remote-debugging-address=127.0.0.1','--headless=new','--no-sandbox','--disable-gpu','--disable-dev-shm-usage','--allow-file-access-from-files','--no-first-run','--no-default-browser-check',`--user-data-dir=${profile}`,'about:blank'],{stdio:['ignore','ignore','pipe']});
  let stderr='';proc.stderr.on('data',d=>{stderr+=String(d)});
  try{
    await waitForVersion(port);
    for(const [file,label,expectedText] of [['nova/index.html','Nova static shell','Nova'],['admin/index.html','Admin sign-in shell','Admin']]){
      const cdp=await newTarget(port);
      try{for(const width of WIDTHS)await inspect(cdp,file,width,label,expectedText)}finally{cdp.close()}
    }
    console.log(`Browser responsive smoke passed at ${WIDTHS.join(', ')}px for Nova and Admin static shells`);
  }catch(e){throw new Error(`${e.message}\nChrome stderr: ${stderr.slice(-2000)}`)}finally{
    proc.kill('SIGTERM');
    await sleep(100);
    try{fs.rmSync(profile,{recursive:true,force:true})}catch{}
  }
}

run().catch(e=>{console.error(e.stack||e);process.exitCode=1});
