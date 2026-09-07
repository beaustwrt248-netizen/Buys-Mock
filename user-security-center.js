(()=>{
  const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co';
  const API_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
  const EDGE=`${SUPABASE_URL}/functions/v1/user-security-center`;
  const STORE='morley_web_auth';
  const INSTALL_KEY='morley_installation_id';
  let heartbeatTimer=null;

  function session(){try{return JSON.parse(localStorage.getItem(STORE)||'null')}catch{return null}}
  function token(){return session()?.access_token||''}
  function installationId(){let id=localStorage.getItem(INSTALL_KEY);if(!id){id=crypto.randomUUID();localStorage.setItem(INSTALL_KEY,id)}return id}
  function deviceName(){const ua=navigator.userAgent||'';if(/Android/i.test(ua))return 'Android device';if(/iPhone|iPad/i.test(ua))return 'Apple mobile device';if(/Windows/i.test(ua))return 'Windows device';if(/Macintosh|Mac OS/i.test(ua))return 'Mac';if(/Linux/i.test(ua))return 'Linux device';return 'Web browser'}
  function platform(){const ua=navigator.userAgent||'';if(/Android/i.test(ua))return 'android';if(/iPhone|iPad/i.test(ua))return 'ios';return 'web'}
  function esc(s){return String(s??'').replace(/[&<>"']/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[c]))}
  function when(v){if(!v)return 'Never';try{return new Intl.DateTimeFormat('en-AU',{dateStyle:'medium',timeStyle:'short'}).format(new Date(v))}catch{return String(v)}}
  function age(v){if(!v)return 'unknown';const ms=Date.now()-Date.parse(v);if(ms<60000)return 'just now';if(ms<3600000)return `${Math.floor(ms/60000)}m ago`;if(ms<86400000)return `${Math.floor(ms/3600000)}h ago`;return `${Math.floor(ms/86400000)}d ago`}
  function setStatus(text,kind='muted'){const el=document.getElementById('securityCenterStatus');if(el){el.className=kind;el.textContent=text}}

  async function api(action,extra={}){
    const jwt=token();if(!jwt)throw new Error('Your Morley session has expired. Sign in again.');
    const r=await fetch(EDGE,{method:'POST',headers:{'Content-Type':'application/json','Authorization':`Bearer ${jwt}`},body:JSON.stringify({action,...extra})});
    let d={};try{d=await r.json()}catch{}
    if(!r.ok)throw new Error(d?.error||`Security request failed (${r.status})`);
    return d;
  }

  async function heartbeat(showNew=true){
    if(!token())return null;
    const d=await api('heartbeat',{installation_id:installationId(),platform:platform(),device_name:deviceName(),app_version:String(window.MORLEY_APP_VERSION||'web-ota')});
    if(d.is_new&&showNew){
      const el=document.getElementById('securityCenterStatus');
      if(el){el.className='warn';el.textContent='New sign-in detected on this device. Review your active devices below.'}
    }
    return d;
  }

  async function refresh(){
    await heartbeat(false);
    const d=await api('list');
    renderDevices(d.devices||[]);
    renderEvents(d.events||[]);
    const active=(d.devices||[]).filter(x=>!x.revoked_at).length;
    const current=(d.devices||[]).find(x=>x.current);
    const activeEl=document.getElementById('securityActiveDevices');if(activeEl)activeEl.textContent=String(active);
    const currentEl=document.getElementById('securityCurrentDevice');if(currentEl)currentEl.textContent=current?.device_name||deviceName();
    setStatus('Security activity is current. Tokens are never stored in the device registry.','good');
    return d;
  }

  async function trustCurrent(){setStatus('Marking this device as trusted…','warn');await api('trust_current');await refresh()}

  async function signOutOthers(){
    if(!confirm('Sign out every other Morley session? This device will remain signed in.'))return;
    setStatus('Revoking other sessions…','warn');
    await api('signout_others');
    setStatus('Other refresh sessions revoked. Existing short-lived access tokens will expire normally.','good');
    await refresh();
  }

  async function localSignOut(){
    const jwt=token();
    try{
      if(jwt){
        await api('heartbeat',{installation_id:installationId(),platform:platform(),device_name:deviceName(),app_version:String(window.MORLEY_APP_VERSION||'web-ota')}).catch(()=>{});
        await fetch(`${SUPABASE_URL}/auth/v1/logout?scope=local`,{method:'POST',headers:{apikey:API_KEY,Authorization:`Bearer ${jwt}`}}).catch(()=>{});
      }
    }finally{
      localStorage.removeItem(STORE);
      location.reload();
    }
  }

  function renderDevices(rows){
    const host=document.getElementById('securityDeviceList');if(!host)return;
    if(!rows.length){host.innerHTML='<div class="item"><small>No session devices recorded yet.</small></div>';return}
    host.innerHTML=rows.map(d=>{const state=d.revoked_at?'Signed out':d.current?'Current device':d.trusted_at?'Trusted':'Active';return `<div class="item"><div><b>${esc(d.device_name||d.platform||'Device')}</b><small>${esc(state)} • last seen ${esc(age(d.last_seen_at))}<br>${esc(d.platform||'web')} • ${esc(d.app_version||'version unknown')}</small></div>${d.current&&!d.trusted_at?'<button class="secondary" data-trust-current>Trust</button>':''}</div>`}).join('');
    host.querySelectorAll('[data-trust-current]').forEach(b=>b.onclick=()=>trustCurrent().catch(e=>setStatus(e.message,'bad')));
  }

  function eventLabel(t){return ({new_session:'New sign-in',session_seen:'Session activity',trusted_device:'Device trusted',signout_others:'Other sessions signed out',local_signout:'Signed out',backup_warning:'Backup warning',backup_failure:'Backup failed',restore_attempt:'Restore started',restore_success:'Restore completed',security_notice:'Security notice'})[t]||String(t||'Activity').replaceAll('_',' ')}
  function renderEvents(rows){
    const host=document.getElementById('securityEventList');if(!host)return;
    if(!rows.length){host.innerHTML='<div class="item"><small>No security activity recorded yet.</small></div>';return}
    host.innerHTML=rows.slice(0,20).map(e=>`<div class="item"><div><b>${esc(eventLabel(e.event_type))}</b><small>${esc(when(e.created_at))}${e.detail?.device_name?` • ${esc(e.detail.device_name)}`:''}</small></div></div>`).join('');
  }

  function inject(){
    const settings=document.getElementById('settings');if(!settings||document.getElementById('securityCenterCard'))return;
    const card=document.createElement('div');card.id='securityCenterCard';card.className='card';card.innerHTML=`<h2>🛡️ Recovery & Security Centre</h2><p class="muted">Review signed-in devices, security activity and recovery protection for your Morley account.</p><div class="stats"><div><small>ACTIVE DEVICES</small><b id="securityActiveDevices">—</b></div><div><small>CURRENT DEVICE</small><b id="securityCurrentDevice" style="font-size:14px">—</b></div><div><small>SESSION PROTECTION</small><b style="font-size:14px">Enabled</b></div><div><small>TOKEN STORAGE</small><b style="font-size:14px">Registry: none</b></div></div><div class="bar"><button id="securityRefreshBtn" class="secondary">Refresh Security</button><button id="securityTrustBtn" class="secondary">Trust This Device</button><button id="securitySignOutOthersBtn" class="danger">Sign Out Other Devices</button></div><p id="securityCenterStatus" class="muted">Checking account security…</p><h3>Devices & sessions</h3><div id="securityDeviceList" class="list"></div><h3>Security activity</h3><div id="securityEventList" class="list"></div>`;
    settings.appendChild(card);
    document.getElementById('securityRefreshBtn').onclick=()=>refresh().catch(e=>setStatus(e.message,'bad'));
    document.getElementById('securityTrustBtn').onclick=()=>trustCurrent().catch(e=>setStatus(e.message,'bad'));
    document.getElementById('securitySignOutOthersBtn').onclick=()=>signOutOthers().catch(e=>setStatus(e.message,'bad'));
  }

  function secureExistingSignout(){
    const bind=()=>{const b=document.querySelector('.morley-web-signout');if(b&&!b.dataset.secureLogout){b.dataset.secureLogout='1';b.onclick=()=>localSignOut()}};
    bind();
    const observer=new MutationObserver(bind);observer.observe(document.documentElement,{childList:true,subtree:true});
    setTimeout(()=>observer.disconnect(),30000);
  }

  function init(){inject();secureExistingSignout();heartbeat(true).then(()=>refresh()).catch(e=>setStatus(e.message,'bad'));heartbeatTimer=setInterval(()=>heartbeat(false).catch(()=>{}),5*60*1000)}
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',()=>setTimeout(init,0),{once:true});else setTimeout(init,0);
  window.MorleySecurityCenter={refresh,heartbeat,trustCurrent,signOutOthers,localSignOut};
})();
