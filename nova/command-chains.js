(()=>{'use strict';
const base=window.NovaCommands;if(!base)return;
const HISTORY_KEY='nova.command.history.v1';
const MAX_HISTORY=50;
const CHAIN_RE=/\s*(?:;|\band then\b|\bthen\b|\bfollowed by\b)\s*/i;
const NEGATION=/\b(?:not|dont|cannot|cant|wont|never|stop|avoid)\b/i;
function loadHistory(){try{const raw=JSON.parse(sessionStorage.getItem(HISTORY_KEY)||'[]');return Array.isArray(raw)?raw.slice(0,MAX_HISTORY):[]}catch(_e){return[]}}
function saveHistory(rows){try{sessionStorage.setItem(HISTORY_KEY,JSON.stringify(rows.slice(0,MAX_HISTORY)))}catch(_e){}}
let history=loadHistory();
function record(entry){history.unshift(Object.freeze({...entry,at:new Date().toISOString()}));history=history.slice(0,MAX_HISTORY);saveHistory(history);window.dispatchEvent(new CustomEvent('nova:command-history',{detail:history[0]}))}
function split(input){const raw=String(input??'').trim();if(!raw||NEGATION.test(raw))return[raw];const parts=raw.split(CHAIN_RE).map(x=>x.trim()).filter(Boolean);return parts.length>1?parts:[raw]}
function safeResolved(resolved){const c=resolved?.command;return !!c&&c.risk==='safe'&&!c.requiresGuardian&&typeof c.handler==='function'}
function plan(input){const parts=split(input);if(parts.length<2)return null;const steps=parts.map(text=>({text,resolved:base.resolve(text)}));const invalid=steps.find(step=>!step.resolved);if(invalid)return{ok:false,type:'unknown',steps,message:`Nova could not safely resolve “${invalid.text}”, so no chained command was run.`};const protectedStep=steps.find(step=>!safeResolved(step.resolved));if(protectedStep)return{ok:false,type:'protected',steps,message:`Nova will not chain “${protectedStep.text}” because it is not a safe read-only command. No step in this chain was run.`};return{ok:true,type:'plan',steps,message:`Ready to run ${steps.length} safe commands.`}}
const oldExecute=base.execute.bind(base);
async function execute(input){const planned=plan(input);if(!planned){const result=await oldExecute(input);record({input:String(input),ok:!!result?.ok,type:result?.type||'single',commands:result?.command?[result.command]:[]});return result}if(!planned.ok){record({input:String(input),ok:false,type:planned.type,commands:planned.steps.filter(s=>s.resolved).map(s=>s.resolved.command.id)});return planned}const results=[];for(const step of planned.steps){const result=await oldExecute(step.text);results.push(result);if(!result?.ok){record({input:String(input),ok:false,type:'chain-partial',commands:results.map(r=>r.command).filter(Boolean)});return{ok:false,type:'chain-partial',message:`Stopped after ${results.length} of ${planned.steps.length} safe commands: ${result?.message||'a command failed.'}`,results}}const commands=results.map(r=>r.command).filter(Boolean);record({input:String(input),ok:true,type:'chain',commands});return{ok:true,type:'chain',commands,results,message:results.map((r,i)=>`${i+1}. ${r.message}`).join('\n')}}
function historyText(limit=10){const rows=history.slice(0,Math.max(1,Math.min(Number(limit)||10,25)));if(!rows.length)return'No Nova command history exists in this browser session.';return rows.map((row,i)=>`${i+1}. ${row.ok?'OK':'FAILED'} · ${row.type} · ${row.input}`).join('\n')}
const enhanced=Object.freeze({...base,execute,planChain:plan,splitChain:split,getHistory:()=>[...history],clearHistory:()=>{history=[];saveHistory(history);return true},historyText});
window.NovaCommands=enhanced;
window.addEventListener('nova:commands-ready',()=>{});
window.dispatchEvent(new Event('nova:command-chains-ready'));
})();