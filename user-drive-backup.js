(()=>{
  const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co';
  const EDGE_URL=`${SUPABASE_URL}/functions/v1/user-google-drive-backup`;
  const DRIVE_SCOPE='https://www.googleapis.com/auth/drive.appdata';
  const CLIENT_KEYS=['bm_inv','bm_sales','bm_recent'];
  const AUTO_DEBOUNCE_MS=10000;
  const DAILY_MS=24*60*60*1000;
  let googleToken='';
  let tokenClient=null;
  let autoTimer=null;
  let lastAuto=0;

  function authSession(){try{return JSON.parse(localStorage.getItem('morley_web_auth')||'null')}catch{return null}}
  function accessToken(){const s=authSession();return s?.access_token||''}
  function clientId(){return String(window.MORLEY_GOOGLE_CLIENT_ID||document.querySelector('meta[name="morley-google-client-id"]')?.content||'').trim()}
  function readJson(key,fallback){try{return JSON.parse(localStorage.getItem(key)||'null')??fallback}catch{return fallback}}
  function clientState(){return {bm_inv:readJson('bm_inv',[]),bm_sales:readJson('bm_sales',[]),bm_recent:readJson('bm_recent',[])}}
  function applyClientState(state){if(!state||typeof state!=='object')return;for(const key of CLIENT_KEYS){if(Object.prototype.hasOwnProperty.call(state,key))localStorage.setItem(key,JSON.stringify(state[key]??[]))}}
  function esc(s){return String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
  function fmtDate(v){if(!v)return 'Never';try{return new Intl.DateTimeFormat('en-AU',{dateStyle:'medium',timeStyle:'short'}).format(new Date(v))}catch{return v}}
  function fmtBytes(n){n=Number(n)||0;if(n<1024)return `${n} B`;if(n<1048576)return `${(n/1024).toFixed(1)} KB`;return `${(n/1048576).toFixed(1)} MB`}
  function setStatus(text,kind='muted'){const el=document.getElementById('driveBackupStatus');if(el){el.className=kind;el.textContent=text}}

  async function api(action,extra={}){
    const morley=accessToken();
    if(!morley)throw new Error('Your Morley session has expired. Sign in again.');
    if(!googleToken)throw new Error('Connect Google Drive first.');
    const r=await fetch(EDGE_URL,{method:'POST',headers:{'Content-Type':'application/json','Authorization':`Bearer ${morley}`,'X-Google-Access-Token':googleToken},body:JSON.stringify({action,...extra})});
    let d={};try{d=await r.json()}catch{}
    if(r.status===401&&String(d?.error||'').toLowerCase().includes('google'))googleToken='';
    if(!r.ok)throw new Error(d?.error||`Backup request failed (${r.status})`);
    return d;
  }

  function loadGIS(){return new Promise((resolve,reject)=>{if(window.google?.accounts?.oauth2)return resolve();const existing=document.querySelector('script[data-morley-gis]');if(existing){existing.addEventListener('load',resolve,{once:true});existing.addEventListener('error',reject,{once:true});return}const s=document.createElement('script');s.src='https://accounts.google.com/gsi/client';s.async=true;s.defer=true;s.dataset.morleyGis='1';s.onload=resolve;s.onerror=()=>reject(new Error('Could not load Google sign-in.'));document.head.appendChild(s)})}

  async function connectDrive(){
    const id=clientId();
    if(!id)throw new Error('Google Drive connection is ready in Morley Buys, but the Google OAuth client ID still needs to be configured.');
    await loadGIS();
    if(!tokenClient){
      tokenClient=google.accounts.oauth2.initTokenClient({client_id:id,scope:DRIVE_SCOPE,callback:()=>{}});
    }
    return new Promise((resolve,reject)=>{
      tokenClient.callback=(resp)=>{if(resp?.error)return reject(new Error(resp.error_description||resp.error));googleToken=resp.access_token||'';if(!googleToken)return reject(new Error('Google did not return an access token.'));resolve(resp)};
      tokenClient.requestAccessToken({prompt:googleToken?'':'consent'});
    });
  }

  async function ensureConnected(){if(!googleToken)await connectDrive();return googleToken}

  async function backup(reason='manual'){
    await ensureConnected();
    setStatus('Creating encrypted Google Drive backup…','warn');
    const d=await api('backup',{reason,client_state:clientState()});
    localStorage.setItem('morley_last_drive_backup',d.backup?.created_at||new Date().toISOString());
    lastAuto=Date.now();
    setStatus(`Backup complete • ${fmtDate(d.backup?.created_at)}`,'good');
    await refresh();
    return d;
  }

  async function refresh(){
    if(!googleToken){renderHistory([]);return}
    const [list,health]=await Promise.all([api('list'),api('health')]);
    renderHistory(list.backups||[]);
    const who=list.google?.email||health.google?.email||'Google Drive';
    const last=health.last_backup?.created_at;
    const acct=document.getElementById('driveBackupAccount');if(acct)acct.textContent=who;
    const lastEl=document.getElementById('driveBackupLast');if(lastEl)lastEl.textContent=last?fmtDate(last):'No backup yet';
    setStatus(last?`Protected • AES-256-GCM • last backup ${fmtDate(last)}`:'Connected • AES-256-GCM • no backup yet','good');
  }

  async function previewAndRestore(id){
    await ensureConnected();
    const p=(await api('preview_restore',{backup_id:id})).preview;
    const c=p?.changes||{};
    const message=`Restore backup from ${fmtDate(p?.created_at)}?\n\nThis will restore:\n• ${c.valuation_history_rows||0} valuation-history rows\n• ${c.local_inventory||0} local inventory items\n• ${c.local_sales||0} local sales records\n• ${c.local_recent||0} recent entries\n\nA fresh safety backup will be created first.`;
    if(!window.confirm(message))return;
    setStatus('Creating safety backup and restoring…','warn');
    const d=await api('restore',{backup_id:id,confirm:'RESTORE',current_client_state:clientState()});
    applyClientState(d.restore?.client_state);
    setStatus('Restore complete. Reloading protected data…','good');
    setTimeout(()=>location.reload(),300);
  }

  async function verify(id){await ensureConnected();setStatus('Verifying encrypted backup integrity…','warn');await api('verify',{backup_id:id});setStatus('Backup integrity verified.','good')}

  async function remove(id){if(!window.confirm('Delete this encrypted backup from your Google Drive?'))return;await ensureConnected();await api('delete',{backup_id:id});await refresh()}

  function renderHistory(rows){const host=document.getElementById('driveBackupHistory');if(!host)return;if(!rows.length){host.innerHTML='<div class="item"><small>No encrypted Drive backups yet.</small></div>';return}host.innerHTML=rows.slice(0,12).map(r=>`<div class="item"><div><b>${esc(fmtDate(r.created_at))}</b><small>${esc(fmtBytes(r.byte_size))} • ${esc(r.status||'ready')}</small></div><div class="bar"><button class="secondary" data-drive-verify="${esc(r.id)}">Verify</button><button data-drive-restore="${esc(r.id)}">Restore</button><button class="danger" data-drive-delete="${esc(r.id)}">Delete</button></div></div>`).join('');host.querySelectorAll('[data-drive-restore]').forEach(b=>b.onclick=()=>previewAndRestore(b.dataset.driveRestore).catch(e=>setStatus(e.message,'bad')));host.querySelectorAll('[data-drive-verify]').forEach(b=>b.onclick=()=>verify(b.dataset.driveVerify).catch(e=>setStatus(e.message,'bad')));host.querySelectorAll('[data-drive-delete]').forEach(b=>b.onclick=()=>remove(b.dataset.driveDelete).catch(e=>setStatus(e.message,'bad')))}

  function injectUI(){
    const settings=document.getElementById('settings');if(!settings||document.getElementById('driveBackupCard'))return;
    const card=document.createElement('div');card.id='driveBackupCard';card.className='card';card.innerHTML=`<h2>☁️ Encrypted Google Drive Backup</h2><p class="muted">Each signed-in Morley user gets an isolated encrypted backup in their own Google Drive app data. Morley auth tokens and sessions are never included.</p><div class="stats"><div><small>GOOGLE ACCOUNT</small><b id="driveBackupAccount" style="font-size:14px">Not connected</b></div><div><small>LAST BACKUP</small><b id="driveBackupLast" style="font-size:14px">Never</b></div><div><small>ENCRYPTION</small><b style="font-size:14px">AES-256-GCM</b></div><div><small>RETENTION</small><b style="font-size:14px">30 versions</b></div></div><div class="bar"><button id="driveConnectBtn">Connect Google Drive</button><button id="driveBackupNowBtn" class="secondary">Back Up Now</button><button id="driveRefreshBtn" class="secondary">Refresh</button></div><p id="driveBackupStatus" class="muted">Google Drive is not connected yet.</p><h3>Backup history</h3><div id="driveBackupHistory" class="list"></div>`;
    settings.appendChild(card);
    document.getElementById('driveConnectBtn').onclick=async()=>{try{setStatus('Connecting Google Drive…','warn');await connectDrive();await refresh()}catch(e){setStatus(e.message,'bad')}};
    document.getElementById('driveBackupNowBtn').onclick=()=>backup('manual').catch(e=>setStatus(e.message,'bad'));
    document.getElementById('driveRefreshBtn').onclick=()=>refresh().catch(e=>setStatus(e.message,'bad'));

    const exportBtn=document.getElementById('exportBtn');if(exportBtn){exportBtn.textContent='Back Up to Drive';exportBtn.onclick=()=>backup('inventory-toolbar').catch(e=>setStatus(e.message,'bad'))}
    const importBtn=document.getElementById('importBtn');if(importBtn){importBtn.textContent='Restore from Drive';importBtn.onclick=async()=>{try{await ensureConnected();show('settings');await refresh()}catch(e){setStatus(e.message,'bad')}}}
    const importFile=document.getElementById('importFile');if(importFile)importFile.disabled=true;
  }

  function scheduleAuto(){
    const original=Storage.prototype.setItem;
    if(!Storage.prototype.__morleyDrivePatched){Storage.prototype.setItem=function(key,value){original.call(this,key,value);if(this===localStorage&&CLIENT_KEYS.includes(String(key))&&googleToken){clearTimeout(autoTimer);autoTimer=setTimeout(()=>{if(Date.now()-lastAuto>30000)backup('important-change').catch(()=>{})},AUTO_DEBOUNCE_MS)}};Storage.prototype.__morleyDrivePatched=true}
    setInterval(()=>{if(!googleToken)return;const last=Date.parse(localStorage.getItem('morley_last_drive_backup')||'')||0;if(Date.now()-last>=DAILY_MS)backup('daily-safety').catch(()=>{})},15*60*1000);
  }

  function init(){injectUI();scheduleAuto();const id=clientId();if(!id)setStatus('Backup system installed. Google OAuth client ID configuration is the remaining connection step.','warn')}
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(init,0),{once:true});else setTimeout(init,0);
  window.MorleyDriveBackup={connect:connectDrive,backup,refresh,restore:previewAndRestore,verify};
})();
