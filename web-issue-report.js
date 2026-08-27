(()=>{
  function buildReport(detail=''){
    return [
      'B&L Morley web issue report','',
      `Captured: ${new Date().toISOString()}`,
      `Page: ${location.href}`,
      `Browser: ${navigator.userAgent}`,
      `Online: ${navigator.onLine?'yes':'no'}`,
      `Viewport: ${window.innerWidth}x${window.innerHeight}`,
      `Screen: ${screen.width}x${screen.height}`,
      `Web deployment: ${document.lastModified||'current'}`,
      `Local storage keys: ${localStorage.length}`,'',
      'What happened:',detail.trim()||'(add a short description before sending)'
    ].join('\n');
  }
  async function copyText(text){
    if(navigator.clipboard?.writeText){await navigator.clipboard.writeText(text);return}
    const area=document.createElement('textarea');area.value=text;area.setAttribute('readonly','');area.style.position='fixed';area.style.opacity='0';document.body.appendChild(area);area.select();const ok=document.execCommand('copy');area.remove();if(!ok)throw new Error('Copy is not available in this browser.');
  }
  function downloadText(text){const blob=new Blob([text],{type:'text/plain;charset=utf-8'}),a=document.createElement('a');a.href=URL.createObjectURL(blob);a.download=`BL-Morley-issue-${new Date().toISOString().replace(/[:.]/g,'-')}.txt`;a.click();setTimeout(()=>URL.revokeObjectURL(a.href),500)}
  function enhance(){
    const dialog=document.querySelector('#morleyMenuDialog.open');
    if(!dialog||dialog.querySelector('h2')?.textContent?.trim()!=='Report an Issue'||dialog.dataset.issueTools==='1')return;
    const input=dialog.querySelector('#mmIssue'),send=dialog.querySelector('#mmIssueSend');if(!input||!send)return;dialog.dataset.issueTools='1';
    const intro=dialog.querySelector('.morley-settings-body p');if(intro)intro.textContent='Describe what happened, then copy, download or open a pre-filled email containing non-sensitive diagnostic details.';
    if(input.tagName==='INPUT'){const textarea=document.createElement('textarea');textarea.id='mmIssue';textarea.placeholder='What happened?';textarea.rows=6;textarea.style.cssText='box-sizing:border-box;width:100%;padding:12px 13px;border-radius:12px;border:1px solid rgba(47,124,255,.35);background:#041024;color:#fff;resize:vertical';input.replaceWith(textarea)}
    const copy=document.createElement('button');copy.id='mmIssueCopy';copy.textContent='Copy Diagnostic Report';copy.style.cssText='width:100%;margin-top:12px;padding:12px;border-radius:12px;border:1px solid #2f7cff;background:linear-gradient(120deg,#1f5fd8,#2f7cff,#12c9ff);color:#fff;font-weight:900;cursor:pointer';send.insertAdjacentElement('beforebegin',copy);
    const download=document.createElement('button');download.id='mmIssueDownload';download.textContent='Download Report';download.style.cssText='width:100%;margin-top:12px;padding:12px;border-radius:12px;border:1px solid rgba(47,124,255,.35);background:#0a1b33;color:#fff;font-weight:900;cursor:pointer';send.insertAdjacentElement('beforebegin',download);send.textContent='Create Issue Email';
    const status=document.createElement('div');status.id='mmIssueToolStatus';status.setAttribute('role','status');status.setAttribute('aria-live','polite');status.style.cssText='margin-top:10px;color:#9db0c9;font-size:12px;line-height:1.45';send.insertAdjacentElement('afterend',status);
    const detail=()=>dialog.querySelector('#mmIssue')?.value||'';
    copy.onclick=async()=>{try{await copyText(buildReport(detail()));status.textContent='Diagnostic report copied.'}catch(error){status.textContent=error.message||'Copy failed.'}};
    download.onclick=()=>{downloadText(buildReport(detail()));status.textContent='Diagnostic report downloaded.'};
    send.onclick=()=>{location.href=`mailto:?subject=${encodeURIComponent('B&L Morley web issue')}&body=${encodeURIComponent(buildReport(detail()))}`};
  }
  new MutationObserver(enhance).observe(document.documentElement,{childList:true,subtree:true,attributes:true,attributeFilter:['class']});
  if(document.readyState==='loading')document.addEventListener('DOMContentLoaded',enhance,{once:true});else enhance();
})();