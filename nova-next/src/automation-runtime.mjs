const PROTECTED=/\b(deploy|release|ota|sign(?:ing)?|guardian\s+repair|pricing\s+(?:approve|approval|change|write)|user\s+role|role\s+change|delete\s+production|truncate|drop\s+table)\b/i;

export function createAutomationRuntime({ store, workspaceRuntime }={}){
  if(!store) throw new TypeError('AUTOMATION_STORE_REQUIRED');
  const snapshot=()=>workspaceRuntime?.snapshot?.() || { tasks:[],projects:[] };
  const validateLinks=input=>{
    const state=snapshot();
    if(input.taskId && !(state.tasks||[]).some(x=>x.id===input.taskId)) throw new Error('AUTOMATION_TASK_NOT_FOUND');
    if(input.projectId && !(state.projects||[]).some(x=>x.id===input.projectId)) throw new Error('AUTOMATION_PROJECT_NOT_FOUND');
  };
  const validateIntent=input=>{
    const joined=[input.title,input.prompt,input.scheduleText].filter(Boolean).join(' ');
    if(PROTECTED.test(joined)) throw new Error('AUTOMATION_PROTECTED_INTENT');
  };
  function save(input={}){ validateIntent(input); validateLinks(input); return input.id ? store.update(input.id,input) : store.create(input); }
  function setEnabled(id,enabled){ const job=store.update(id,{state:enabled?'enabled':'disabled'}); return Object.freeze({ job, message:enabled?'Enabled locally; no background runner is connected.':'Disabled locally.' }); }
  function status(job){ return Object.freeze({ badge:'local-only', state:job?.state || 'draft', detail:'Stored on this device. No background execution service is connected.' }); }
  return Object.freeze({ list:()=>store.list(),save,setEnabled,status,remove:id=>store.remove(id) });
}
