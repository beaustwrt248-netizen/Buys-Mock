(()=>{
  const PREF='morley_menu_prefs';
  let wakeLock=null;

  function readPrefs(){
    try{return JSON.parse(localStorage.getItem(PREF)||'{}')}catch{return{}}
  }

  function writePrefs(next){
    localStorage.setItem(PREF,JSON.stringify(next));
  }

  async function releaseWakeLock(){
    const current=wakeLock;
    wakeLock=null;
    if(current){
      try{await current.release()}catch{}
    }
  }

  async function syncWakeLock(){
    const enabled=!!readPrefs().keepAwake;
    if(!enabled||document.visibilityState!=='visible'){
      await releaseWakeLock();
      return {enabled,supported:'wakeLock' in navigator,active:false};
    }
    if(!('wakeLock' in navigator))return {enabled:true,supported:false,active:false};
    if(wakeLock)return {enabled:true,supported:true,active:true};
    try{
      wakeLock=await navigator.wakeLock.request('screen');
      wakeLock.addEventListener('release',()=>{wakeLock=null},{once:true});
      return {enabled:true,supported:true,active:true};
    }catch{
      wakeLock=null;
      return {enabled:true,supported:true,active:false};
    }
  }

  function findDisplayDialog(){
    const dialog=document.querySelector('#morleyMenuDialog.open');
    if(!dialog)return null;
    return dialog.querySelector('h2')?.textContent?.trim()==='Display'?dialog:null;
  }

  function ensureControl(){
    const dialog=findDisplayDialog();
    if(!dialog||dialog.querySelector('#mmKeepAwake'))return;
    const save=dialog.querySelector('#mmSaveD');
    if(!save)return;

    const prefs=readPrefs();
    const wrap=document.createElement('label');
    wrap.id='morleyWebKeepAwakeRow';
    wrap.style.cssText='display:flex;gap:10px;align-items:flex-start;margin:14px 0';
    wrap.innerHTML=`<input id="mmKeepAwake" type="checkbox" ${prefs.keepAwake?'checked':''}><span><b style="display:block;color:#fff">Keep screen awake</b><small id="mmKeepAwakeStatus" style="display:block;margin-top:3px;color:#9db0c9;line-height:1.4">Useful during valuations and stock entry.</small></span>`;
    save.insertAdjacentElement('beforebegin',wrap);

    save.addEventListener('click',async()=>{
      const checked=!!dialog.querySelector('#mmKeepAwake')?.checked;
      writePrefs({...readPrefs(),keepAwake:checked});
      const state=await syncWakeLock();
      const status=dialog.querySelector('#mmKeepAwakeStatus');
      if(status){
        status.textContent=!state.supported
          ? 'Saved. This browser does not support the Screen Wake Lock API.'
          : state.active
            ? 'Screen wake lock is active while this tab is visible.'
            : checked
              ? 'Saved. Wake lock will retry when this tab becomes active.'
              : 'Keep-awake is off.';
      }
    });

    syncWakeLock().then(state=>{
      const status=dialog.querySelector('#mmKeepAwakeStatus');
      if(!status)return;
      if(!state.supported)status.textContent='This browser does not support keep-awake; the preference can still be saved.';
      else if(state.active)status.textContent='Screen wake lock is active while this tab is visible.';
    });
  }

  document.addEventListener('visibilitychange',()=>{syncWakeLock().catch(()=>{})});
  window.addEventListener('pagehide',()=>{releaseWakeLock().catch(()=>{})});
  new MutationObserver(()=>ensureControl()).observe(document.documentElement,{childList:true,subtree:true,attributes:true,attributeFilter:['class']});

  if(document.readyState==='loading'){
    document.addEventListener('DOMContentLoaded',()=>{syncWakeLock().catch(()=>{});ensureControl()},{once:true});
  }else{
    syncWakeLock().catch(()=>{});
    ensureControl();
  }
})();