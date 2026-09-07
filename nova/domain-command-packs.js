(()=>{'use strict';
const base=window.NovaCommands;if(!base)return;
const RISK=base.risk||{SAFE:'safe',WRITE:'write',SENSITIVE:'sensitive',DESTRUCTIVE:'destructive'};
const PREFIXES=['','nova ','nova please ','please ','can you ','could you ','would you ','i want you to ','i need you to ','hey nova '];
const SUFFIXES=['',' please',' now',' for me'];
const READ_VERBS=['show','check','inspect','review','read','explain','summarise','summarize','report','list'];
const normalize=v=>String(v??'').toLowerCase().replace(/[’']/g,'').replace(/[^a-z0-9#._ -]+/g,' ').replace(/\s+/g,' ').trim();
const text=id=>document.getElementById(id)?.textContent?.trim()||'—';
const escSection=s=>window.CSS?.escape?CSS.escape(s):String(s).replace(/[^a-z0-9_-]/gi,'');
function openSection(section){const button=document.querySelector(`[data-section="${escSection(section)}"]`);if(button)button.click()}
function summary(section){
 const maps={
  overview:[['Core','coreState'],['Open Nova PRs','metricPrs'],['Needs attention','metricAttention'],['Main','metricMain']],
  catalogue:[['Records','catalogueTotal'],['Active','catalogueActive'],['AU region','catalogueAu'],['Model gaps','catalogueModelGaps']],
  support:[['Tickets','supportTotal'],['Active','supportActive'],['High priority','supportHigh'],['SLA overdue','supportSla']],
  guardian:[['State','guardianState'],['Latest repair','guardianLatest'],['Open repairs','guardianOpen'],['Blocked gates','guardianBlocked']],
  monitoring:[['State','monitorState'],['Tracked','monitorTracked'],['Healthy','monitorHealthy'],['Running','monitorRunning'],['Failed','monitorFailed']],
  releases:[['Readiness','releaseOverall'],['Production','releaseDeploy'],['Quality','releaseQuality'],['Security','releaseSecurity'],['Parity','releaseParity']],
  recommendations:[['State','recommendState'],['Urgent','recommendUrgent'],['High-risk PRs','recommendHighRisk'],['Model gaps','recommendDataGaps']],
  knowledge:[['Reference items','memoryKnowledge'],['Learning','memoryLearning'],['Verified','memoryVerified'],['Confidence','memoryConfidence']]
 };
 const rows=maps[section]||maps.overview;return rows.map(([label,id])=>`${label}: ${text(id)}`).join(' · ')
}
function phraseAliases(topic,verbs=READ_VERBS){return [...new Set(verbs.flatMap(v=>[`${v} ${topic}`,`${v} ${topic} status`,`${v} the ${topic}`,`${v} current ${topic}`]))]}
const PACKS={
 catalogue:{section:'catalogue',topics:['catalogue health','device count','active devices','australian mapping','model number gaps','storage gaps','release year gaps','missing images','duplicate devices','catalogue categories','catalogue audit','device coverage']},
 inventory:{section:'catalogue',topics:['inventory health','stock reconciliation','missing stock numbers','stale listings','cash converters stock','gumtree stock','inventory coverage','stock mismatches','active stock','inventory audit']},
 pricing:{section:'recommendations',topics:['pricing intelligence','market comparison','price evidence','price recommendations','underpriced stock','overpriced stock','ebay evidence','gumtree evidence','facebook marketplace evidence','retailer evidence','pricing approval status']},
 images:{section:'catalogue',topics:['image coverage','missing device images','official images','manufacturer images','duplicate images','low quality images','image source quality','image audit']},
 research:{section:'knowledge',topics:['manufacturer research','australian sources','retailer research','carrier research','device specifications','model number research','release year research','storage research','official source coverage','research policy']},
 support:{section:'support',topics:['support health','support queue','active tickets','high priority tickets','sla status','overdue tickets','first response status','ticket categories','duplicate tickets','support workload','support audit']},
 guardian:{section:'guardian',topics:['guardian health','guardian incidents','guardian approvals','guardian repeated failures','guardian evidence','guardian repairs','guardian blocked gates','guardian audit trail']},
 development:{section:'development',topics:['development status','open pull requests','nova pull requests','recent commits','engineering queue','branch status','development checks','recent development activity']},
 github:{section:'monitoring',topics:['github status','workflow status','failed workflows','running workflows','repository checks','ci status','main branch health','github activity']},
 release:{section:'releases',topics:['release readiness','production deployment','apk status','ota status','quality gate','security gate','parity gate','release evidence','deployment status','release version']},
 backup:{section:'overview',topics:['backup status','google drive backup','backup health','backup verification','restore readiness','backup evidence']},
 security:{section:'guardian',topics:['security status','authentication status','permission boundaries','session security','security audit','rls status','protected actions','approval boundaries']},
 users:{section:'overview',topics:['user account status','role status','invite status','session status','permission status','account access']},
 analytics:{section:'overview',topics:['nova analytics','command usage','command registry','catalogue analytics','support analytics','release analytics','error analytics','activity analytics']},
 nova:{section:'overview',topics:['nova health','nova capabilities','nova status','nova command coverage','nova command namespaces','nova audit history','nova diagnostics','nova learning status']}
};
const extras=[];
for(const [namespace,pack] of Object.entries(PACKS))for(const topic of pack.topics){
 const slug=normalize(topic).replace(/ /g,'_');
 extras.push(Object.freeze({id:`${namespace}.${slug}.read`,namespace,description:`Read ${topic}`,aliases:Object.freeze(phraseAliases(topic)),risk:RISK.SAFE,permission:'admin',requiresGuardian:false,tags:Object.freeze(['domain-pack','read',namespace]),section:pack.section,handler:()=>{openSection(pack.section);return{message:`${topic.replace(/^./,c=>c.toUpperCase())} · ${summary(pack.section)}`}}}));
}
const protectedDefs=[
 ['catalogue.device.write','catalogue','Change catalogue device data',['edit catalogue device','update catalogue device','write catalogue changes'],RISK.WRITE],
 ['catalogue.device.delete','catalogue','Delete catalogue device',['delete catalogue device','remove catalogue device'],RISK.DESTRUCTIVE],
 ['pricing.change.apply','pricing','Apply a pricing change',['apply price change','change stock price','set stock price'],RISK.SENSITIVE],
 ['pricing.approval.grant','pricing','Grant pricing approval',['approve price change','approve pricing recommendation'],RISK.SENSITIVE],
 ['support.ticket.close','support','Close a support ticket',['close support ticket','resolve support ticket'],RISK.WRITE],
 ['guardian.repair.approve','guardian','Approve a Guardian repair',['approve repair','approve guardian action'],RISK.SENSITIVE],
 ['guardian.repair.apply','guardian','Apply a Guardian repair',['apply repair','execute repair'],RISK.DESTRUCTIVE],
 ['release.production.deploy','release','Deploy production',['deploy production now','publish production','ship production'],RISK.DESTRUCTIVE],
 ['release.ota.publish','release','Publish OTA update',['publish ota','push ota update'],RISK.DESTRUCTIVE],
 ['users.role.promote','users','Promote a user role',['make user admin','promote user to admin'],RISK.SENSITIVE],
 ['users.role.demote','users','Demote a user role',['remove admin role','demote admin'],RISK.SENSITIVE],
 ['users.account.disable','users','Disable a user account',['disable user account','lock user account'],RISK.SENSITIVE],
 ['backup.restore.apply','backup','Restore a backup',['restore backup','restore google drive backup'],RISK.DESTRUCTIVE]
];
for(const [id,namespace,description,aliases,risk] of protectedDefs)extras.push(Object.freeze({id,namespace,description,aliases:Object.freeze(aliases),risk,permission:'admin',requiresGuardian:true,tags:Object.freeze(['domain-pack','protected',namespace]),handler:null}));
function expand(alias){const a=normalize(alias),out=[];for(const p of PREFIXES)for(const s of SUFFIXES)out.push(normalize(p+a+s));return [...new Set(out)]}
const index=new Map(),collisions=[];for(const cmd of extras)for(const alias of cmd.aliases)for(const phrase of expand(alias)){const prior=index.get(phrase);if(prior&&prior.id!==cmd.id){collisions.push({phrase,commands:[prior.id,cmd.id]});continue}index.set(phrase,cmd)}
function resolveExtra(input){const raw=normalize(input);if(!raw||/\b(?:not|dont|cant|cannot|wont|never|stop|avoid)\b/.test(raw))return null;const hit=index.get(raw);if(hit)return{command:hit,args:{},confidence:1,matched:raw};const stripped=raw.replace(/^(?:hey )?nova(?: please)?\s+/,'').replace(/\s+(?:please|for me|now)$/,'').trim();const match=index.get(stripped);return match?{command:match,args:{},confidence:.98,matched:stripped}:null}
function guardExtra(resolved){const c=resolved?.command;if(!c)return{allowed:false,reason:'unknown'};if(c.requiresGuardian||c.risk!==RISK.SAFE||typeof c.handler!=='function')return{allowed:false,reason:'protected',message:`${c.description} is recognised, but Nova will not execute it here. It remains behind the existing human/Guardian approval boundary.`};return{allowed:true}}
async function executeExtra(input){const r=resolveExtra(input);if(!r)return null;const gate=guardExtra(r);window.dispatchEvent(new CustomEvent('nova:command-audit',{detail:{input,command:r.command.id,risk:r.command.risk,allowed:gate.allowed,confidence:r.confidence,at:new Date().toISOString()}}));if(!gate.allowed)return{ok:false,type:gate.reason,command:r.command.id,message:gate.message};const result=await r.command.handler(r.args,{input,resolved:r});return{ok:true,type:'executed',command:r.command.id,message:result?.message||`${r.command.description} completed.`}}
const oldResolve=base.resolve.bind(base),oldExecute=base.execute.bind(base),oldStats=base.stats.bind(base),oldRegistry=[...base.registry];
const combined=Object.freeze({
 ...base,
 resolve(input){return resolveExtra(input)||oldResolve(input)},
 async execute(input){const extra=await executeExtra(input);return extra||oldExecute(input)},
 stats(){const s=oldStats(),namespaces={...(s.namespaces||{})};for(const c of extras)namespaces[c.namespace]=(namespaces[c.namespace]||0)+1;return{...s,canonicalCommands:(s.canonicalCommands||oldRegistry.length)+extras.length,understoodPhrases:(s.understoodPhrases||0)+index.size,namespaces,collisions:[...(s.collisions||[]),...collisions],domainPacks:Object.keys(PACKS).length,domainCommands:extras.length}},
 registry:Object.freeze([...oldRegistry,...extras]),
 domainPacks:Object.freeze(Object.keys(PACKS))
});
window.NovaCommands=combined;window.dispatchEvent(new CustomEvent('nova:command-packs-ready',{detail:combined.stats()}));
})();