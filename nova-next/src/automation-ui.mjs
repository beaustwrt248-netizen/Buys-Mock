function el(d,t,c='',x=''){const n=d.createElement(t);if(c)n.className=c;if(x)n.textContent=x;return n;}
export function createAutomationUi({documentObj=globalThis.document,automationRuntime,workspaceRuntime,onToast=()=>{}}={}){
  if(!documentObj)throw new TypeError('AUTOMATION_DOCUMENT_REQUIRED');
  if(!automationRuntime)throw new TypeError('AUTOMATION_RUNTIME_REQUIRED');
  function option(value,label){const n=documentObj.createElement('option');n.value=value;n.textContent=label;return n;}
  function openForm(existing=null){
    documentObj.querySelector('.automation-sheet-backdrop')?.remove();
    const back=el(documentObj,'div','feature-sheet-backdrop automation-sheet-backdrop'),sheet=el(documentObj,'section','feature-sheet');sheet.setAttribute('role','dialog');sheet.setAttribute('aria-modal','true');sheet.setAttribute('aria-label','New Local Job');
    const head=el(documentObj,'div','feature-sheet-head');head.append(el(documentObj,'h2','',existing?'Edit Local Job':'New Local Job'));const close=el(documentObj,'button','icon-button','×');close.type='button';close.addEventListener('click',()=>back.remove());head.append(close);
    const form=el(documentObj,'form','workspace-form');
    const title=documentObj.createElement('input');title.required=true;title.maxLength=120;title.placeholder='Job title';title.value=existing?.title||'';
    const prompt=documentObj.createElement('textarea');prompt.required=true;prompt.rows=5;prompt.placeholder='What should this local job describe?';prompt.value=existing?.prompt||'';
    const schedule=documentObj.createElement('input');schedule.placeholder='Schedule description, e.g. Every morning';schedule.value=existing?.scheduleText||'';
    const snap=workspaceRuntime?.snapshot?.()||{tasks:[],projects:[]};
    const task=documentObj.createElement('select');task.append(option('','No linked task'),...(snap.tasks||[]).map(x=>option(x.id,x.title)));task.value=existing?.taskId||'';
    const project=documentObj.createElement('select');project.append(option('','No linked project'),...(snap.projects||[]).map(x=>option(x.id,x.title)));project.value=existing?.projectId||'';
    const submit=el(documentObj,'button','primary-button',existing?'Save changes':'Save local job');submit.type='submit';
    form.append(el(documentObj,'p','feature-boundary','Local-only metadata. Enabling a job does not start a timer, scheduler, background worker or protected action.'),title,prompt,schedule,task,project,submit);
    form.addEventListener('submit',e=>{e.preventDefault();try{automationRuntime.save({id:existing?.id,title:title.value,prompt:prompt.value,scheduleText:schedule.value,taskId:task.value,projectId:project.value,state:existing?.state||'draft'});back.remove();render();onToast('Local automation job saved.');}catch(err){onToast(err?.message==='AUTOMATION_PROTECTED_INTENT'?'Protected execution intent cannot be stored as an automation job.':'Automation job could not be saved.','error');}});
    sheet.append(head,form);back.append(sheet);documentObj.body.append(back);title.focus();
  }
  function render(){
    const root=documentObj.getElementById('novaNextAutomationList');if(!root)return;root.replaceChildren();
    const intro=el(documentObj,'article','feature-status-card');intro.append(el(documentObj,'small','','Execution mode'),el(documentObj,'strong','','Local-only'),el(documentObj,'p','','Jobs are stored on this device; no background runner is connected.'));root.append(intro);
    const add=el(documentObj,'button','small-primary','＋ New Local Job');add.type='button';add.addEventListener('click',()=>openForm());root.append(add);
    for(const job of automationRuntime.list()){
      const card=el(documentObj,'article','feature-status-card');const status=automationRuntime.status(job);card.append(el(documentObj,'small','',`${status.badge} · ${job.state}`),el(documentObj,'strong','',job.title||'Untitled job'),el(documentObj,'p','',job.scheduleText||'No schedule description'));
      const actions=el(documentObj,'div','workspace-actions');const toggle=el(documentObj,'button','secondary-button',job.state==='enabled'?'Disable':'Enable');toggle.type='button';toggle.addEventListener('click',()=>{const r=automationRuntime.setEnabled(job.id,job.state!=='enabled');onToast(r.message);render();});const edit=el(documentObj,'button','secondary-button','Edit');edit.type='button';edit.addEventListener('click',()=>openForm(job));actions.append(toggle,edit);card.append(actions);root.append(card);
    }
  }
  return Object.freeze({render,openForm,routeChanged:r=>{if(r==='automation')render();}});
}
