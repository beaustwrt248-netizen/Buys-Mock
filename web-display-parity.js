(()=>{
  const PREF='morley_menu_prefs';
  const STYLE_ID='morleyDisplayPreferenceStyles';
  let wakeLock=null;

  function readPrefs(){
    try{return JSON.parse(localStorage.getItem(PREF)||'{}')}catch{return{}}
  }

  function writePrefs(next){
    localStorage.setItem(PREF,JSON.stringify(next));
  }

  function ensurePreferenceStyles(){
    if(document.getElementById(STYLE_ID))return;
    const style=document.createElement('style');
    style.id=STYLE_ID;
    style.textContent=`
      html.morley-reduced-motion *,
      html.morley-reduced-motion *::before,
      html.morley-reduced-motion *::after {
        animation-duration: .01ms !important;
        animation-iteration-count: 1 !important;
        transition-duration: .01ms !important;
        scroll-behavior: auto !important;
      }
      html.morley-compact-interface #morleyMenuDialog .mm-card,
      html.morley-compact-interface #morleyMenuDialog .menu-card,
      html.morley-compact-interface #morleyMenuDialog .card {
        padding-top: 10px !important;
        padding-bottom: 10px !important;
      }
      html.morley-compact-interface #morleyMenuDialog button,
      html.morley-compact-interface #morleyMenuDialog input,
      html.morley-compact-interface #morleyMenuDialog select,
      html.morley-compact-interface #morleyMenuDialog textarea {
        min-height: 40px;
      }
    `;
    document.head.appendChild(style);
  }

  function syncVisualPrefs(){
    ensurePreferenceStyles();
    const prefs=readPrefs();
    document.documentElement.classList.toggle('morley-reduced-motion',!!prefs.reducedMotion);
    document.documentElement.classList.toggle('morley-compact-interface',!!prefs.compactInterface);
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

  function preferenceRow(id,title,detail,checked){
    const wrap=document.createElement('label');
    wrap.style.cssText='display:flex;gap:10px;align-items:flex-start;margin:14px 0';
    wrap.innerHTML=`<input id="${id}" type="checkbox" ${checked?'checked':''}><span><b style="display:block;color:#fff">${title}</b><small style="display:block;margin-top:3px;color:#9db0c9;line-height:1.4">${detail}</small></span>`;
    return wrap;
  }

  function ensureControls(){
    const dialog=findDisplayDialog();
    if(!dialog||dialog.querySelector('#mmKeepAwake'))return;
    const save=dialog.querySelector('#mmSaveD');
    if(!save)return;

    const prefs=readPrefs();
    const keepAwake=preferenceRow('mmKeepAwake','Keep screen awake','Useful during valuations and stock entry.',!!prefs.keepAwake);
    keepAwake.id='morleyWebKeepAwakeRow';
    keepAwake.querySelector('small').id='mmKeepAwakeStatus';
    const reducedMotion=preferenceRow('mmReducedMotion','Reduced motion','Minimise interface animation and animated transitions.',!!prefs.reducedMotion);
    const compact=preferenceRow('mmCompactInterface','Compact interface','Use denser controls inside B&L Morley menu panels.',!!prefs.compactInterface);
    save.insertAdjacentElement('beforebegin',keepAwake);
    save.insertAdjacentElement('beforebegin',reducedMotion);
    save.insertAdjacentElement('beforebegin',compact);

    save.addEventListener('click',async()=>{
      const next={
        ...readPrefs(),
        keepAwake:!!dialog.querySelector('#mmKeepAwake')?.checked,
        reducedMotion:!!dialog.querySelector('#mmReducedMotion')?.checked,
        compactInterface:!!dialog.querySelector('#mmCompactInterface')?.checked
      };
      writePrefs(next);
      syncVisualPrefs();
      const state=await syncWakeLock();
      const status=dialog.querySelector('#mmKeepAwakeStatus');
      if(status){
        status.textContent=!state.supported
          ? 'Saved. This browser does not support the Screen Wake Lock API.'
          : state.active
            ? 'Screen wake lock is active while this tab is visible.'
            : next.keepAwake
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
  window.addEventListener('storage',event=>{
    if(event.key===PREF){syncVisualPrefs();syncWakeLock().catch(()=>{})}
  });
  window.addEventListener('pagehide',()=>{releaseWakeLock().catch(()=>{})});
  new MutationObserver(()=>ensureControls()).observe(document.documentElement,{childList:true,subtree:true,attributes:true,attributeFilter:['class']});

  syncVisualPrefs();
  if(document.readyState==='loading'){
    document.addEventListener('DOMContentLoaded',()=>{syncVisualPrefs();syncWakeLock().catch(()=>{});ensureControls()},{once:true});
  }else{
    syncWakeLock().catch(()=>{});
    ensureControls();
  }
})();