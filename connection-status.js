(()=>{
  const ID='morley-connection-status';
  let hideTimer=null;

  function ensure(){
    let el=document.getElementById(ID);
    if(el)return el;
    el=document.createElement('div');
    el.id=ID;
    el.setAttribute('role','status');
    el.setAttribute('aria-live','polite');
    el.style.cssText='position:fixed;right:14px;bottom:14px;z-index:99998;display:none;align-items:center;gap:8px;max-width:min(360px,calc(100vw - 28px));padding:10px 12px;border-radius:999px;border:1px solid rgba(18,201,255,.28);background:rgba(4,13,29,.96);color:#eef8ff;font:800 12px/1.25 Inter,system-ui,-apple-system,Segoe UI,sans-serif;box-shadow:0 10px 28px rgba(0,0,0,.32);backdrop-filter:blur(12px)';
    document.body.appendChild(el);
    return el;
  }

  function paint(online,initial=false){
    const el=ensure();
    document.documentElement.classList.toggle('morley-offline',!online);
    clearTimeout(hideTimer);
    if(!online){
      el.style.display='flex';
      el.style.borderColor='rgba(255,200,87,.48)';
      el.innerHTML='<span style="width:8px;height:8px;border-radius:50%;background:#ffc857;box-shadow:0 0 10px rgba(255,200,87,.75);flex:0 0 auto"></span><span>Offline — saved/local information remains available where supported.</span>';
      window.dispatchEvent(new CustomEvent('morley-connectivity-change',{detail:{online:false}}));
      return;
    }
    if(initial){ el.style.display='none'; return; }
    el.style.display='flex';
    el.style.borderColor='rgba(37,217,145,.38)';
    el.innerHTML='<span style="width:8px;height:8px;border-radius:50%;background:#25d991;box-shadow:0 0 10px rgba(37,217,145,.75);flex:0 0 auto"></span><span>Connection restored.</span>';
    window.dispatchEvent(new CustomEvent('morley-connectivity-change',{detail:{online:true}}));
    hideTimer=setTimeout(()=>{el.style.display='none'},3500);
  }

  function init(){
    paint(navigator.onLine,true);
    window.addEventListener('online',()=>paint(true));
    window.addEventListener('offline',()=>paint(false));
  }

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init,{once:true});else init();
})();
