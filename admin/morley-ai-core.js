(()=>{'use strict';
const RISK={READ:'read',LOW:'low',PROTECTED:'protected',CRITICAL:'critical'};
const registry=new Map();
const memory={session:[],feedback:[]};
function register(cap){if(!cap?.id||typeof cap.execute!=='function')throw new Error('Invalid Morley AI capability');registry.set(cap.id,Object.freeze({...cap}));}
function list(){return [...registry.values()].map(({execute,...x})=>x);}
function record(type,data){memory.session.push({at:new Date().toISOString(),type,data});if(memory.session.length>250)memory.session.shift();}
function feedback(entry){memory.feedback.push({at:new Date().toISOString(),...entry});if(memory.feedback.length>100)memory.feedback.shift();record('feedback',entry);}
function needsApproval(cap,input){if(cap.risk===RISK.PROTECTED||cap.risk===RISK.CRITICAL)return true;return typeof cap.approval==='function'?!!cap.approval(input):false;}
async function run(id,input={},ctx={}){const cap=registry.get(id);if(!cap)throw new Error(`Unknown Morley AI capability: ${id}`);const approval=needsApproval(cap,input);if(approval&&!ctx.approved)return {status:'needs_approval',capability:id,risk:cap.risk,summary:cap.describe?.(input)||cap.label,input};record('run',{capability:id,risk:cap.risk});const result=await cap.execute(input,ctx);record('result',{capability:id,ok:true});return {status:'completed',capability:id,result};}
function context(){return {capabilities:list(),recent:memory.session.slice(-30),feedback:memory.feedback.slice(-30)};}
window.MorleyAI=Object.freeze({RISK,register,list,run,context,feedback});
})();
