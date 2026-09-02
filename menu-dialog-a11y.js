(()=>{
  const DIALOG_SELECTOR='.morley-menu-dialog';
  let lastTrigger=null;

  function focusable(root){
    return [...root.querySelectorAll('button,[href],input,select,textarea,[tabindex]:not([tabindex="-1"])')]
      .filter(el=>!el.disabled&&el.getAttribute('aria-hidden')!=='true'&&el.offsetParent!==null);
  }

  function labelDialog(dialog){
    if(!dialog||dialog.dataset.morleyA11y==='1')return;
    dialog.dataset.morleyA11y='1';
    dialog.setAttribute('role','dialog');
    dialog.setAttribute('aria-modal','true');
    const card=dialog.querySelector('.morley-menu-dialog-card');
    const heading=dialog.querySelector('h2');
    if(heading){
      if(!heading.id)heading.id=`morley-dialog-title-${Math.random().toString(36).slice(2,8)}`;
      dialog.setAttribute('aria-labelledby',heading.id);
    }
    if(card)card.setAttribute('tabindex','-1');
  }

  function activate(dialog){
    labelDialog(dialog);
    if(!dialog.classList.contains('open'))return;
    if(!dialog.dataset.morleyFocused){
      dialog.dataset.morleyFocused='1';
      const close=dialog.querySelector('.morley-menu-dialog-close');
      requestAnimationFrame(()=>{(close||dialog.querySelector('.morley-menu-dialog-card'))?.focus?.()});
    }
  }

  function restore(dialog){
    delete dialog.dataset.morleyFocused;
    const trigger=lastTrigger;
    if(trigger&&document.contains(trigger))requestAnimationFrame(()=>trigger.focus?.());
    lastTrigger=null;
  }

  document.addEventListener('click',event=>{
    const trigger=event.target.closest('button,a');
    if(trigger&&!trigger.closest(DIALOG_SELECTOR))lastTrigger=trigger;
  },true);

  document.addEventListener('keydown',event=>{
    const dialog=document.querySelector(`${DIALOG_SELECTOR}.open`);
    if(!dialog)return;
    if(event.key==='Escape'){
      event.preventDefault();
      const close=dialog.querySelector('.morley-menu-dialog-close');
      if(close)close.click();else{dialog.classList.remove('open');restore(dialog)}
      return;
    }
    if(event.key!=='Tab')return;
    const items=focusable(dialog);
    if(!items.length)return;
    const first=items[0],last=items[items.length-1];
    if(event.shiftKey&&document.activeElement===first){event.preventDefault();last.focus()}
    else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus()}
  });

  function scan(){
    document.querySelectorAll(DIALOG_SELECTOR).forEach(dialog=>{
      labelDialog(dialog);
      if(dialog.classList.contains('open'))activate(dialog);
      else if(dialog.dataset.morleyFocused)restore(dialog);
    });
  }

  function init(){
    scan();
    const observer=new MutationObserver(()=>requestAnimationFrame(scan));
    observer.observe(document.body,{childList:true,subtree:true,attributes:true,attributeFilter:['class']});
  }

  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();