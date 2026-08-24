(()=>{
  function enhanceAuth(){
    const root=document.getElementById('morleyWebAuth');
    if(!root||root.dataset.a11yReady)return;
    root.dataset.a11yReady='1';
    const msg=root.querySelector('#waMsg');
    if(msg){msg.setAttribute('role','status');msg.setAttribute('aria-live','polite');msg.setAttribute('aria-atomic','true')}
    const primary=root.querySelector('#waPrimary');
    root.querySelectorAll('button').forEach(b=>b.type='button');
    root.addEventListener('keydown',e=>{
      if(e.key==='Enter'&&e.target instanceof HTMLInputElement&&primary&&!primary.disabled){e.preventDefault();primary.click()}
    });
    setTimeout(()=>root.querySelector('#waEmail')?.focus(),120);
  }
  function enhanceApp(){
    document.querySelectorAll('button').forEach(b=>{
      if(!b.getAttribute('aria-label')){
        const text=(b.innerText||b.textContent||'').replace(/\s+/g,' ').trim();
        if(text)b.setAttribute('aria-label',text);
      }
    });
    const api=document.getElementById('apiStatus');
    if(api){api.setAttribute('role','status');api.setAttribute('aria-live','polite')}
    document.querySelectorAll('input,textarea,select').forEach(el=>{
      if(!el.getAttribute('aria-label')){
        const label=el.closest('div')?.querySelector('label')?.textContent?.trim();
        if(label)el.setAttribute('aria-label',label);
      }
    });
  }
  const style=document.createElement('style');
  style.textContent=':focus-visible{outline:2px solid #12c9ff!important;outline-offset:3px!important}button:focus:not(:focus-visible),input:focus:not(:focus-visible),select:focus:not(:focus-visible),textarea:focus:not(:focus-visible){outline:none}';
  document.head.appendChild(style);
  enhanceAuth();enhanceApp();
  new MutationObserver(()=>{enhanceAuth();enhanceApp()}).observe(document.documentElement,{childList:true,subtree:true});
})();