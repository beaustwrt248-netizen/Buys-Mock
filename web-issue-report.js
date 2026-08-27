(()=>{
  const SUPABASE_URL='https://ghdhairijqjqivqriigi.supabase.co';
  const API_KEY='sb_publishable_ch49o8WRnDb8pPzowZH3Tg_XZcIbgvt';
  const SESSION_STORE='morley_web_auth';
  const ATTACHMENT_BUCKET='support-ticket-attachments';
  const MAX_ATTACHMENT_BYTES=10*1024*1024;
  const ALLOWED_TYPES=new Set(['image/jpeg','image/png','image/webp','application/pdf']);

  function loadSession(){try{return JSON.parse(localStorage.getItem(SESSION_STORE)||'null')}catch{return null}}
  function jwtSub(token){try{const p=token.split('.')[1].replace(/-/g,'+').replace(/_/g,'/');return JSON.parse(atob(p+'='.repeat((4-p.length%4)%4))).sub||''}catch{return''}}
  function buildDiagnostics(){return {platform:'web',captured:new Date().toISOString(),page:location.href,browser:navigator.userAgent,online:navigator.onLine,viewport:`${window.innerWidth}x${window.innerHeight}`,screen:`${screen.width}x${screen.height}`,webDeployment:document.lastModified||'current'}}
  function buildReport(detail='',subject='',category='other',includeDiagnostics=false){
    const lines=['B&L Morley web issue report','',`Subject: ${subject.trim()||'(not provided)'}`,`Category: ${category}`,'','What happened:',detail.trim()||'(add a short description before sending)'];
    if(includeDiagnostics){const d=buildDiagnostics();lines.push('','Diagnostics:',...Object.entries(d).map(([k,v])=>`${k}: ${v}`))}
    return lines.join('\n');
  }
  async function copyText(text){if(navigator.clipboard?.writeText){await navigator.clipboard.writeText(text);return}const area=document.createElement('textarea');area.value=text;area.setAttribute('readonly','');area.style.position='fixed';area.style.opacity='0';document.body.appendChild(area);area.select();const ok=document.execCommand('copy');area.remove();if(!ok)throw new Error('Copy is not available in this browser.')}
  function downloadText(text){const blob=new Blob([text],{type:'text/plain;charset=utf-8'}),a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download=`BL-Morley-issue-${new Date().toISOString().replace(/[:.]/g,'-')}.txt`;a.click();setTimeout(()=>URL.revokeObjectURL(a.href),500)}
  async function api(path,{method='GET',body,headers={}}={}){
    const s=loadSession(),token=s?.access_token,userId=jwtSub(token||'');
    if(!token||!userId)throw new Error('Your sign-in session is unavailable. Sign in again before submitting a ticket.');
    const response=await fetch(`${SUPABASE_URL}${path}`,{method,headers:{apikey:API_KEY,Authorization:`Bearer ${token}`,...headers},body});
    let payload=null;try{payload=await response.json()}catch{}
    if(!response.ok){const msg=payload?.message||payload?.error_description||payload?.error||`Request failed (${response.status})`;throw new Error(msg)}
    return {payload,userId,token};
  }
  function safeFileName(name){return String(name||'attachment').replace(/[^A-Za-z0-9._-]+/g,'-').replace(/^-+|-+$/g,'').slice(0,120)||'attachment'}
  function validateAttachment(file){if(!file)return;if(file.size>MAX_ATTACHMENT_BYTES)throw new Error('Attachment must be 10 MB or smaller.');if(!ALLOWED_TYPES.has(file.type))throw new Error('Attachment must be a JPG, PNG, WebP or PDF.')}
  async function createTicket({category,subject,description,includeDiagnostics}){
    const s=loadSession(),userId=jwtSub(s?.access_token||'');
    if(!s?.access_token||!userId)throw new Error('Your sign-in session is unavailable. Sign in again before submitting a ticket.');
    const body={user_id:userId,category,subject,description,status:'open',priority:'normal',assigned_to:null,app_version:'web',app_version_code:null,device_model:null,android_version:null,diagnostics:includeDiagnostics?buildDiagnostics():{},diagnostics_opt_in:includeDiagnostics};
    const result=await api('/rest/v1/support_tickets?select=id,created_at',{method:'POST',headers:{'Content-Type':'application/json','Prefer':'return=representation'},body:JSON.stringify(body)});
    const ticket=Array.isArray(result.payload)?result.payload[0]:result.payload;if(!ticket?.id)throw new Error('Ticket was created but its reference number was not returned.');return {ticket,userId,token:result.token};
  }
  async function uploadAttachment(ticketId,userId,token,file){
    validateAttachment(file);if(!file)return;
    const fileName=safeFileName(file.name),storagePath=`${userId}/${ticketId}/${Date.now()}-${fileName}`;
    const upload=await fetch(`${SUPABASE_URL}/storage/v1/object/${ATTACHMENT_BUCKET}/${storagePath}`,{method:'POST',headers:{apikey:API_KEY,Authorization:`Bearer ${token}`,'Content-Type':file.type,'x-upsert':'false'},body:file});
    if(!upload.ok){let data={};try{data=await upload.json()}catch{};throw new Error(data.message||data.error||`Attachment upload failed (${upload.status})`)}
    await api('/rest/v1/support_ticket_attachments',{method:'POST',headers:{'Content-Type':'application/json','Prefer':'return=minimal'},body:JSON.stringify({ticket_id:ticketId,uploader_user_id:userId,storage_path:storagePath,file_name:file.name,content_type:file.type,byte_size:file.size})});
  }
  function styleButton(button,primary=false){button.type='button';button.style.cssText=`width:100%;margin-top:12px;padding:12px;border-radius:12px;border:1px solid ${primary?'#2f7cff':'rgba(47,124,255,.35)'};background:${primary?'linear-gradient(120deg,#1f5fd8,#2f7cff,#12c9ff)':'#0a1b33'};color:#fff;font-weight:900;cursor:pointer`}
  function makeLabel(text){const label=document.createElement('label');label.textContent=text;label.style.cssText='display:block;margin:12px 0 6px;color:#b8c9df;font-size:12px;font-weight:800';return label}
  function enhance(){
    const dialog=document.querySelector('#morleyMenuDialog.open');
    if(!dialog||dialog.querySelector('h2')?.textContent?.trim()!=='Report an Issue'||dialog.dataset.issueTools==='2')return;
    const input=dialog.querySelector('#mmIssue'),send=dialog.querySelector('#mmIssueSend');if(!input||!send)return;dialog.dataset.issueTools='2';
    const body=dialog.querySelector('.morley-settings-body');const intro=body?.querySelector('p');if(intro)intro.textContent='Send a private support ticket directly to B&L Morley Admin Control. Diagnostics and attachments are optional.';
    if(input.tagName==='INPUT'){const textarea=document.createElement('textarea');textarea.id='mmIssue';textarea.placeholder='Describe what happened and what you expected instead.';textarea.rows=6;textarea.style.cssText='box-sizing:border-box;width:100%;padding:12px 13px;border-radius:12px;border:1px solid rgba(47,124,255,.35);background:#041024;color:#fff;resize:vertical';input.replaceWith(textarea)}
    const description=dialog.querySelector('#mmIssue');
    const subject=document.createElement('input');subject.id='mmIssueSubject';subject.placeholder='Short summary';subject.maxLength=160;subject.style.cssText='box-sizing:border-box;width:100%;padding:12px 13px;border-radius:12px;border:1px solid rgba(47,124,255,.35);background:#041024;color:#fff';description.insertAdjacentElement('beforebegin',subject);subject.insertAdjacentElement('beforebegin',makeLabel('Subject'));
    const category=document.createElement('select');category.id='mmIssueCategory';category.innerHTML='<option value="other">Other</option><option value="valuation">Valuation</option><option value="pricing">Pricing</option><option value="inventory">Inventory</option><option value="scanner">Scanner</option><option value="account">Account</option><option value="update">Update</option>';category.style.cssText='box-sizing:border-box;width:100%;padding:12px 13px;border-radius:12px;border:1px solid rgba(47,124,255,.35);background:#041024;color:#fff';subject.insertAdjacentElement('beforebegin',category);category.insertAdjacentElement('beforebegin',makeLabel('Category'));
    description.insertAdjacentElement('beforebegin',makeLabel('Details'));
    const diagnosticsWrap=document.createElement('label');diagnosticsWrap.style.cssText='display:flex;gap:9px;align-items:flex-start;margin-top:12px;color:#c6d6e9;font-size:13px;line-height:1.4';diagnosticsWrap.innerHTML='<input id="mmIssueDiagnostics" type="checkbox" style="margin-top:3px"> <span>Include non-sensitive web diagnostics (browser, page, screen size and online status).</span>';description.insertAdjacentElement('afterend',diagnosticsWrap);
    const attachment=document.createElement('input');attachment.id='mmIssueAttachment';attachment.type='file';attachment.accept='image/jpeg,image/png,image/webp,application/pdf';attachment.style.cssText='box-sizing:border-box;width:100%;margin-top:8px;color:#c6d6e9';diagnosticsWrap.insertAdjacentElement('afterend',makeLabel('Optional screenshot / PDF (max 10 MB)'));diagnosticsWrap.nextElementSibling.insertAdjacentElement('afterend',attachment);
    const copy=document.createElement('button');copy.id='mmIssueCopy';copy.textContent='Copy Report';styleButton(copy);send.insertAdjacentElement('beforebegin',copy);
    const download=document.createElement('button');download.id='mmIssueDownload';download.textContent='Download Report';styleButton(download);send.insertAdjacentElement('beforebegin',download);
    const email=document.createElement('button');email.id='mmIssueEmail';email.textContent='Email Instead';styleButton(email);send.insertAdjacentElement('beforebegin',email);
    send.textContent='Submit Support Ticket';styleButton(send,true);
    const status=document.createElement('div');status.id='mmIssueToolStatus';status.setAttribute('role','status');status.setAttribute('aria-live','polite');status.style.cssText='margin-top:10px;color:#9db0c9;font-size:12px;line-height:1.45';send.insertAdjacentElement('afterend',status);
    const report=()=>buildReport(description.value,subject.value,category.value,dialog.querySelector('#mmIssueDiagnostics').checked);
    copy.onclick=async()=>{try{await copyText(report());status.textContent='Report copied.'}catch(error){status.textContent=error.message||'Copy failed.'}};
    download.onclick=()=>{downloadText(report());status.textContent='Report downloaded.'};
    email.onclick=()=>{location.href=`mailto:?subject=${encodeURIComponent(subject.value.trim()||'B&L Morley web issue')}&body=${encodeURIComponent(report())}`};
    send.onclick=async()=>{
      const detail=description.value.trim(),subjectText=subject.value.trim(),file=attachment.files?.[0]||null;
      if(subjectText.length<3){status.textContent='Enter a subject of at least 3 characters.';subject.focus();return}
      if(detail.length<5){status.textContent='Describe the issue in at least 5 characters.';description.focus();return}
      try{validateAttachment(file)}catch(error){status.textContent=error.message;return}
      send.disabled=true;copy.disabled=true;download.disabled=true;email.disabled=true;status.textContent='Submitting private support ticket…';
      try{
        const created=await createTicket({category:category.value,subject:subjectText,description:detail,includeDiagnostics:dialog.querySelector('#mmIssueDiagnostics').checked});
        if(file){status.textContent='Ticket submitted. Uploading attachment…';try{await uploadAttachment(created.ticket.id,created.userId,created.token,file)}catch(error){status.textContent=`Ticket submitted (${created.ticket.id.slice(0,8)}), but the attachment failed: ${error.message}`;return}}
        status.textContent=`Support ticket submitted successfully. Reference ${created.ticket.id.slice(0,8)}.`;description.value='';subject.value='';attachment.value='';dialog.querySelector('#mmIssueDiagnostics').checked=false;
      }catch(error){status.textContent=error.message||'Ticket submission failed.'}
      finally{send.disabled=false;copy.disabled=false;download.disabled=false;email.disabled=false}
    };
  }
  new MutationObserver(enhance).observe(document.documentElement,{childList:true,subtree:true,attributes:true,attributeFilter:['class']});
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',enhance,{once:true});else enhance();
})();