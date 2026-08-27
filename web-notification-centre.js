(()=>{
  const STORE='morley_web_notifications_v1';
  const MAX_ITEMS=50;
  const $=(s,r=document)=>r.querySelector(s);
  const $$=(s,r=document)=>[...r.querySelectorAll(s)];

  function load(){
    try{
      const value=JSON.parse(localStorage.getItem(STORE)||'[]');
      return Array.isArray(value)?value:[];
    }catch{return[]}
  }

  function save(items){
    localStorage.setItem(STORE,JSON.stringify(items.slice(0,MAX_ITEMS)));
    syncBadge();
  }

  function normalize(item={}){
    return {
      id:String(item.id||`${Date.now()}-${Math.random().toString(36).slice(2,8)}`),
      title:String(item.title||'B&L Morley'),
      body:String(item.body||item.message||''),
      type:String(item.type||'info'),
      createdAt:Number(item.createdAt||Date.now()),
      read:Boolean(item.read),
      action:item.action&&typeof item.action==='object'?item.action:null
    };
  }

  function push(item){
    const next=normalize(item);
    const items=load().filter(x=>x.id!==next.id);
    items.unshift(next);
    save(items);
    return next;
  }

  function markRead(id){
    save(load().map(item=>item.id===id?{...item,read:true}:item));
  }

  function markAllRead(){
    save(load().map(item=>({...item,read:true})));
  }

  function unread(){return load().filter(item=>!item.read).length}

  function formatTime(value){
    try{return new Intl.DateTimeFormat('en-AU',{dateStyle:'medium',timeStyle:'short'}).format(new Date(value))}
    catch{return''}
  }

  function esc(value){
    return String(value??'').replace(/[&<>"']/g,ch=>({'&':'&amp;','<':'&lt;','>':'&gt;','"':'&quot;',"'":'&#39;'}[ch]));
  }

  function ensureStyles(){
    if($('#morleyWebNotificationCentreStyle'))return;
    const style=document.createElement('style');
    style.id='morleyWebNotificationCentreStyle';
    style.textContent=`
      .morley-notification-badge{display:none;min-width:18px;height:18px;padding:0 5px;border-radius:999px;align-items:center;justify-content:center;background:#ff4e78;color:#fff;font-size:10px;font-weight:900;margin-left:auto;box-sizing:border-box}
      .morley-notification-badge.show{display:inline-flex}
      .morley-notification-list{display:grid;gap:10px;margin-top:14px}
      .morley-notification-item{border:1px solid rgba(47,124,255,.24);background:#07162b;border-radius:14px;padding:12px;text-align:left;color:#fff;cursor:pointer}
      .morley-notification-item.unread{border-color:rgba(18,201,255,.58);box-shadow:inset 3px 0 0 #12c9ff}
      .morley-notification-item h4{margin:0 0 5px;font-size:14px}
      .morley-notification-item p{margin:0;color:#a8bbd2;line-height:1.45;font-size:12px}
      .morley-notification-item small{display:block;color:#7188a4;margin-top:8px;font-size:10px}
      .morley-notification-empty{padding:24px 12px;text-align:center;color:#8da5c2;border:1px dashed rgba(47,124,255,.24);border-radius:14px}
      .morley-menu-row[data-web-notification-centre="1"]{position:relative}
      .morley-menu-row[data-web-notification-centre="1"] .morley-notification-badge{margin-right:8px}
      #morleyMenuTrigger .morley-notification-badge{position:absolute;top:-6px;right:-6px;margin:0}
    `;
    document.head.appendChild(style);
  }

  function openCentre(){
    let dialog=$('#morleyWebNotificationCentre');
    if(!dialog){
      dialog=document.createElement('div');
      dialog.id='morleyWebNotificationCentre';
      dialog.className='morley-menu-dialog';
      document.body.appendChild(dialog);
    }
    const items=load();
    const list=items.length?items.map(item=>`<button class="morley-notification-item ${item.read?'':'unread'}" data-notification-id="${esc(item.id)}"><h4>${esc(item.title)}</h4><p>${esc(item.body)}</p><small>${esc(formatTime(item.createdAt))}</small></button>`).join(''):'<div class="morley-notification-empty">No notifications yet.</div>';
    dialog.innerHTML=`<div class="morley-menu-dialog-card" style="max-height:82vh;overflow:auto"><button class="morley-menu-dialog-close" aria-label="Close">×</button><h2>Notification Centre</h2><p style="color:#9db0c9;line-height:1.5">Updates and important B&L Morley messages retained on this browser.</p>${items.some(x=>!x.read)?'<button id="morleyMarkAllRead" style="width:100%;margin-top:4px;padding:10px;border-radius:12px;border:1px solid rgba(47,124,255,.35);background:#0a1b33;color:#fff;font-weight:900;cursor:pointer">Mark all as read</button>':''}<div class="morley-notification-list">${list}</div></div>`;
    dialog.classList.add('open');
    const close=()=>dialog.classList.remove('open');
    $('.morley-menu-dialog-close',dialog).onclick=close;
    dialog.onclick=e=>{if(e.target===dialog)close()};
    $('#morleyMarkAllRead',dialog)?.addEventListener('click',()=>{markAllRead();openCentre()});
    $$('[data-notification-id]',dialog).forEach(button=>button.onclick=()=>{
      markRead(button.dataset.notificationId);
      const item=load().find(x=>x.id===button.dataset.notificationId);
      if(item?.action?.page){
        if(typeof window.morleyDesktopGo==='function')window.morleyDesktopGo(item.action.page);
        else if(typeof window.show==='function')window.show(item.action.page);
        close();
      }else openCentre();
    });
  }

  function injectMenuRow(){
    const menu=$('#morleyRelevantMore');
    if(!menu||$('[data-web-notification-centre="1"]',menu))return;
    const notifications=[...menu.querySelectorAll('.morley-menu-row')].find(row=>(row.textContent||'').includes('Notifications'));
    if(!notifications)return;
    const row=document.createElement('button');
    row.className='morley-menu-row';
    row.dataset.webNotificationCentre='1';
    row.innerHTML='<span class="morley-menu-icon">✦</span><span class="morley-menu-copy"><b>Notification Centre</b><small>Recent updates and important messages.</small></span><span class="morley-notification-badge" aria-label="Unread notifications"></span><span class="morley-menu-chevron">›</span>';
    row.onclick=openCentre;
    notifications.insertAdjacentElement('afterend',row);
    syncBadge();
  }

  function syncBadge(){
    const count=unread();
    const text=count>99?'99+':String(count);
    const rowBadge=$('[data-web-notification-centre="1"] .morley-notification-badge');
    if(rowBadge){rowBadge.textContent=text;rowBadge.classList.toggle('show',count>0)}
    const trigger=$('#morleyMenuTrigger');
    if(trigger){
      let badge=$('.morley-notification-badge',trigger);
      if(!badge){badge=document.createElement('span');badge.className='morley-notification-badge';trigger.appendChild(badge)}
      badge.textContent=text;badge.classList.toggle('show',count>0);
    }
  }

  function init(){
    ensureStyles();
    injectMenuRow();
    syncBadge();
    let queued=false;
    new MutationObserver(()=>{
      if(queued)return;
      queued=true;
      requestAnimationFrame(()=>{queued=false;injectMenuRow();syncBadge()});
    }).observe(document.body,{childList:true,subtree:true});
  }

  window.MorleyNotifications={push,load,markRead,markAllRead,unread,open:openCentre};
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();
