(()=>{
  const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co';
  const EDGE=`${SUPABASE_URL}/functions/v1/user-sync-state`;
  const STORE='morley_web_auth';
  const INSTALL_KEY='morley_installation_id';
  const REV_KEY='morley_sync_revision';
  const PENDING_KEY='morley_sync_pending';
  const KEYS=['bm_inv','bm_sales','bm_recent'];
  let suppress=false;
  let timer=null;

  function auth(){try{return JSON.parse(localStorage.getItem(STORE)||'null')}catch{return null}}
  function token(){return auth()?.access_token||''}
  function installation(){let id=localStorage.getItem(INSTALL_KEY);if(!id){id=crypto.randomUUID();localStorage.setItem(INSTALL_KEY,id)}return id}
  function read(key){try{return JSON.parse(localStorage.getItem(key)||'null')??[]}catch{return[]}}
  function state(){return{bm_inv:read('bm_inv'),bm_sales:read('bm_sales'),bm_recent:read('bm_recent')}}
  function empty(s){return KEYS.every(k=>!Array.isArray(s?.[k])||s[k].length===0)}
  function revision(){return Math.max(0,Number(localStorage.getItem(REV_KEY))||0)}
  function setRevision(v){localStorage.setItem(REV_KEY,String(Math.max(0,Number(v)||0)))}
  function same(a,b){try{return JSON.stringify(a)===JSON.stringify(b)}catch{return false}}
  function apply(s){if(!s||typeof s!=='object')return;suppress=true;try{for(const k of KEYS)if(Array.isArray(s[k]))localStorage.setItem(k,JSON.stringify(s[k]))}finally{suppress=false}}
  function setStatus(text,kind='muted'){const el=document.getElementById('syncCenterStatus');if(el){el.className=kind;el.textContent=text}}
  function setPending(v){if(v)localStorage.setItem(PENDING_KEY,'1');else localStorage.removeItem(PENDING_KEY);const el=document.getElementById('syncPending');if(el)el.textContent=v?'Queued':'Clear'}

  async function api(action,extra={}){
    const jwt=token();if(!jwt)throw new Error('Sign in to sync.');
    const r=await fetch(EDGE,{method:'POST',headers:{'Content-Type':'application/json','Authorization':`Bearer ${jwt}`},body:JSON.stringify({action,installation_id:installation(),...extra})});
    let d={};try{d=await r.json()}catch{}
    if(r.status===409)return{conflict:true,...d};
    if(!r.ok)throw new Error(d?.error||`Sync failed (${r.status})`);
    return d;
  }

  async function pull(){
    if(!navigator.onLine)throw new Error('Offline — changes will sync when connection returns.');
    const server=await api('pull');
    const local=state(),remote=server.state||{bm_inv:[],bm_sales:[],bm_recent:[]};
    const serverRev=Number(server.revision)||0;
    if(serverRev===0&&empty(remote)&&!empty(local)){
      setRevision(0);
      return push('initial-upload');
    }
    if(empty(local)&&!empty(remote)){
      apply(remote);setRevision(serverRev);setPending(false);renderMeta(server);return server;
    }
    if(serverRev>revision()&&!same(local,remote)){
      const merged=await api('merge',{state:local});
      apply(merged.state);setRevision(merged.revision);setPending(false);renderMeta(merged);return merged;
    }
    if(serverRev>=revision()){
      apply(remote);setRevision(serverRev);setPending(false);
    }
    renderMeta(server);return server;
  }

  async function push(reason='change'){
    if(suppress)return;
    if(!navigator.onLine){setPending(true);setStatus('Offline • changes queued safely on this device','warn');return}
    setStatus('Syncing changes…','warn');
    const local=state();
    const result=await api(localStorage.getItem(PENDING_KEY)?'offline_replay':'push',{state:local,base_revision:revision(),reason});
    if(result.conflict){
      setStatus('Concurrent change detected • merging safely…','warn');
      const merged=await api('merge',{state:local});
      apply(merged.state);setRevision(merged.revision);setPending(false);renderMeta(merged);setStatus('Conflict merged • devices are in sync','good');return merged;
    }
    setRevision(result.revision);setPending(false);renderMeta(result);setStatus('Live sync current','good');return result;
  }

  function renderMeta(row){const r=document.getElementById('syncRevision');if(r)r.textContent=String(row?.revision??revision());const u=document.getElementById('syncUpdated');if(u)u.textContent=row?.updated_at?new Intl.DateTimeFormat('en-AU',{dateStyle:'medium',timeStyle:'short'}).format(new Date(row.updated_at)):'Not yet';const c=document.getElementById('syncConnection');if(c)c.textContent=navigator.onLine?'Online':'Offline'}

  function schedule(){if(suppress||!token())return;setPending(true);clearTimeout(timer);timer=setTimeout(()=>push('local-change').catch(e=>{setPending(true);setStatus(e.message,'bad')}),1500)}

  function patchStorage(){
    const proto=Storage.prototype;if(proto.__morleySyncPatched)return;const previous=proto.setItem;
    proto.setItem=function(k,v){previous.call(this,k,v);if(this===localStorage&&KEYS.includes(String(k))&&!suppress)schedule()};proto.__morleySyncPatched=true;
  }

  function inject(){
    const settings=document.getElementById('settings');if(!settings||document.getElementById('syncCenterCard'))return;
    const card=document.createElement('div');card.id='syncCenterCard';card.className='card';card.innerHTML=`<h2>🔄 Live Cross-Device Sync</h2><p class="muted">Keeps your Morley workspace aligned between the website and APK. Offline changes queue locally and revision conflicts are merged instead of overwritten.</p><div class="stats"><div><small>CONNECTION</small><b id="syncConnection">${navigator.onLine?'Online':'Offline'}</b></div><div><small>REVISION</small><b id="syncRevision">${revision()}</b></div><div><small>OFFLINE QUEUE</small><b id="syncPending">${localStorage.getItem(PENDING_KEY)?'Queued':'Clear'}</b></div><div><small>LAST SERVER UPDATE</small><b id="syncUpdated" style="font-size:13px">Not yet</b></div></div><div class="bar"><button id="syncNowBtn">Sync Now</button><button id="syncPullBtn" class="secondary">Check Other Devices</button></div><p id="syncCenterStatus" class="muted">Initialising live sync…</p>`;settings.appendChild(card);document.getElementById('syncNowBtn').onclick=()=>push('manual').catch(e=>setStatus(e.message,'bad'));document.getElementById('syncPullBtn').onclick=()=>pull().then(()=>setStatus('Latest cross-device state loaded','good')).catch(e=>setStatus(e.message,'bad'));
  }

  function init(){inject();patchStorage();window.addEventListener('online',()=>{renderMeta({revision:revision()});if(localStorage.getItem(PENDING_KEY))push('online-replay').catch(e=>setStatus(e.message,'bad'));else pull().catch(()=>{})});window.addEventListener('offline',()=>{renderMeta({revision:revision()});setStatus('Offline • changes will queue on this device','warn')});pull().then(()=>setStatus('Live sync current','good')).catch(e=>{if(!navigator.onLine)setPending(true);setStatus(e.message,navigator.onLine?'bad':'warn')})}
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(init,0),{once:true});else setTimeout(init,0);
  window.MorleyCrossDeviceSync={pull,push,state};
})();
