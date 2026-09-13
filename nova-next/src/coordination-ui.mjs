import { normaliseIntegration } from './integration-status.mjs';
function el(d,t,c='',x=''){const n=d.createElement(t);if(c)n.className=c;if(x)n.textContent=x;return n;}
function day(){return new Date().toISOString().slice(0,10);}
export function createCoordinationUi({documentObj=globalThis.document,workspaceRuntime,featureRuntime,onError=()=>{}}={}){
  if(!documentObj)throw new TypeError('COORDINATION_DOCUMENT_REQUIRED');
  function renderCalendar(){
    const root=documentObj.getElementById('novaNextCalendarList');if(!root)return;root.replaceChildren();
    const today=day(),tasks=workspaceRuntime?.listTasks?.('all')||[],projects=workspaceRuntime?.listProjects?.()||[];
    const pmap=new Map(projects.map(p=>[p.id,p.name||p.title||'Project']));
    const entries=[...tasks.filter(x=>x.dueDate).map(x=>({kind:'Task',label:x.title,date:x.dueDate,completed:x.completed,project:pmap.get(x.projectId)||''})),...projects.filter(x=>x.targetDate).map(x=>({kind:'Project',label:x.name||x.title,date:x.targetDate,completed:x.status==='done',project:''}))];
    const groups={Overdue:[],Today:[],Upcoming:[],Completed:[]};
    for(const item of entries.sort((a,b)=>a.date.localeCompare(b.date)||a.label.localeCompare(b.label))){
      const group=item.completed?'Completed':item.date<today?'Overdue':item.date===today?'Today':'Upcoming';groups[group].push(item);
    }
    for(const [label,items] of Object.entries(groups)){
      if(!items.length)continue;const group=el(documentObj,'section','calendar-date-group');group.append(el(documentObj,'h2','',label));
      for(const item of items){const card=el(documentObj,'article',`calendar-item${item.completed?' is-complete':''}`);card.append(el(documentObj,'span','calendar-kind',item.kind==='Task'?'✓':'▣'));const copy=el(documentObj,'div');copy.append(el(documentObj,'strong','',item.label),el(documentObj,'small','workspace-meta',[item.date,item.project].filter(Boolean).join(' · ')));card.append(copy);group.append(card);}root.append(group);
    }
    if(!root.children.length){const empty=el(documentObj,'div','workspace-empty');empty.append(el(documentObj,'strong','','No dated local work yet'),el(documentObj,'p','','Add due dates to tasks or target dates to projects to see them here.'));root.append(empty);}
  }
  async function renderIntegrations(){
    const root=documentObj.getElementById('novaNextIntegrationsList');if(!root)return;root.replaceChildren(el(documentObj,'p','muted','Loading verified status…'));
    try{const items=(await featureRuntime.integrationStatus()).map(normaliseIntegration);root.replaceChildren();for(const item of items){const card=el(documentObj,'article',`feature-status-card${item.state==='unavailable'?' error':''}`);card.append(el(documentObj,'small','',`${item.capability} · ${item.state}`),el(documentObj,'strong','',item.label),el(documentObj,'p','',item.detail));root.append(card);}root.append(el(documentObj,'p','feature-boundary','Verified status only. New provider connections require an approved isolated connection flow; no third-party credentials are collected here.'));}catch(error){root.replaceChildren(el(documentObj,'p','live-status error','Integration status is temporarily unavailable.'));onError(error);}
  }
  function routeChanged(route){if(route==='calendar')renderCalendar();if(route==='integrations')renderIntegrations();}
  return Object.freeze({renderCalendar,renderIntegrations,routeChanged});
}
