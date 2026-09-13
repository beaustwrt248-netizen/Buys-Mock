const KEY='nova-next.automation.v1';
const STATES=new Set(['draft','enabled','disabled','completed','failed']);
const text=(v,max=500)=>String(v??'').trim().slice(0,max);
const freeze=job=>Object.freeze({...job});

export function createAutomationStore({ storage=globalThis.localStorage, now=()=>new Date(), idFactory=()=>globalThis.crypto?.randomUUID?.() || `job-${Date.now()}` }={}){
  if(!storage) throw new TypeError('AUTOMATION_STORAGE_REQUIRED');
  let jobs=[];
  try { const raw=storage.getItem(KEY); const parsed=raw?JSON.parse(raw):[]; jobs=Array.isArray(parsed)?parsed.filter(x=>x&&typeof x==='object').map(freeze):[]; } catch { jobs=[]; }
  const list=()=>Object.freeze(jobs.map(freeze));
  const persist=next=>{ try{ storage.setItem(KEY,JSON.stringify(next)); }catch(error){ const wrapped=new Error('AUTOMATION_WRITE_FAILED'); wrapped.cause=error; throw wrapped; } jobs=next; };
  const stamp=()=>now().toISOString();
  function normalise(input={},base={}){
    const state=input.state ?? base.state ?? 'draft';
    if(!STATES.has(state)) throw new TypeError('AUTOMATION_STATE_INVALID');
    return { ...base, title:text(input.title ?? base.title,120), prompt:text(input.prompt ?? base.prompt,2000), scheduleText:text(input.scheduleText ?? base.scheduleText,240), state, taskId:text(input.taskId ?? base.taskId,120), projectId:text(input.projectId ?? base.projectId,120), lastResult:text(input.lastResult ?? base.lastResult,1000) };
  }
  function create(input={}){
    const id=text(input.id || idFactory(),120); if(!id || jobs.some(j=>j.id===id)) throw new Error('AUTOMATION_ID_DUPLICATE');
    const createdAt=stamp(); const job={ id,...normalise(input),createdAt,updatedAt:createdAt };
    persist([...jobs,job]); return freeze(job);
  }
  function update(id,patch={}){
    const index=jobs.findIndex(j=>j.id===id); if(index<0) throw new Error('AUTOMATION_NOT_FOUND');
    const next={ ...normalise(patch,jobs[index]), id:jobs[index].id, createdAt:jobs[index].createdAt, updatedAt:stamp() };
    const copy=jobs.slice(); copy[index]=next; persist(copy); return freeze(next);
  }
  function remove(id){ const next=jobs.filter(j=>j.id!==id); if(next.length===jobs.length)return false; persist(next); return true; }
  return Object.freeze({ list,create,update,remove });
}
