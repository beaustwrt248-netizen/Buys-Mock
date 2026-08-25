(()=>{
 const $=(s,r=document)=>r.querySelector(s), $$=(s,r=document)=>[...r.querySelectorAll(s)];
 function go(page){ if(typeof window.morleyDesktopGo==='function') window.morleyDesktopGo(page); else if(typeof window.show==='function') window.show(page); }
 function row(icon,title,sub,action){return `<button class="morley-menu-row" data-action="${action}"><span class="morley-menu-icon">${icon}</span><span class="morley-menu-copy"><b>${title}</b><small>${sub}</small></span><span class="morley-menu-chevron">›</span></button>`}
 function group(title,rows){return `<section class="morley-menu-group"><h3>${title}</h3><div class="morley-menu-card">${rows}</div></section>`}
 function build(){
  const sec=$('#settings'); if(!sec||$('#morleyRelevantMore'))return;
  sec.innerHTML=`<div id="morleyRelevantMore" class="morley-relevant-more">
   <div class="morley-menu-head"><div><small>B&L MORLEY</small><h1>Menu</h1></div></div>
   ${group('Workspace',row('◷','Valuations & Deals','Saved valuations and deal history.','deals')+row('▣','Inventory','Stock, costs and resale values.','inventory')+row('↗','Sales History','Revenue and realised profit.','sales')+row('⌗','Barcode Scanner','Find or add stock quickly.','scanner'))}
   ${group('Your account',row('●','Account & Profile','Signed-in account and profile details.','account')+row('⌁','Privacy & Security','Account privacy and session security.','privacy'))}
   ${group('Data & preferences',row('☁','Backup & Data','Export, import and local app data.','backup')+row('♢','Notifications','Update and app notification preferences.','notifications')+row('◐','Display','Interface and display preferences.','display'))}
   ${group('App',row('↻','Updates','Check app version and update status.','updates')+row('⚑','Report an Issue','Record an app problem for follow-up.','report')+row('§','Legal & Privacy','Privacy and application information.','legal')+row('ⓘ','About B&L Morley','Version and product information.','about'))}
   <button class="morley-menu-signout" data-action="signout">↪ <span>Sign out</span></button>
  </div>`;
  $$('[data-action]',sec).forEach(b=>b.onclick=()=>act(b.dataset.action));
 }
 function notice(title,text){let d=$('#morleyMenuDialog');if(!d){d=document.createElement('div');d.id='morleyMenuDialog';d.className='morley-menu-dialog';document.body.appendChild(d)}d.innerHTML=`<div class="morley-menu-dialog-card"><button class="morley-menu-dialog-close">×</button><h2>${title}</h2><p>${text}</p></div>`;d.classList.add('open');$('.morley-menu-dialog-close',d).onclick=()=>d.classList.remove('open');d.onclick=e=>{if(e.target===d)d.classList.remove('open')};}
 function act(a){
  if(['deals','inventory','sales','scanner'].includes(a))return go(a);
  if(a==='signout'){const b=$('.morley-web-signout');if(b)return b.click();localStorage.removeItem('morley_web_auth');location.reload();return}
  const info={account:['Account & Profile','Your signed-in B&L Morley account is used for authorised access.'],privacy:['Privacy & Security','B&L Morley keeps authenticated access protected and stores local workspace data in this browser where applicable.'],backup:['Backup & Data','Use Inventory export/import controls for stock backup and restore.'],notifications:['Notifications','Web notifications remain browser-controlled. Native app update notifications are managed in the Android app.'],display:['Display','B&L Morley currently uses the shared cyber dark interface across web and Android.'],updates:['Updates','The website updates automatically when a new production build deploys. Android includes its own update checker.'],report:['Report an Issue','Capture what happened, the page you were on, and any visible error so it can be fixed accurately.'],legal:['Legal & Privacy','Private B&L Morley business system. Access is limited to authorised accounts.'],about:['About B&L Morley','Buys & Loans Hub — shared valuation, inventory, deal and sales workspace.']}[a]; if(info)notice(info[0],info[1]);
 }
 function init(){build();new MutationObserver(()=>build()).observe(document.body,{childList:true,subtree:true});}
 if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',init);else init();
})();