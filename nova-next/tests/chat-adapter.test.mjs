import assert from 'node:assert/strict';
import { createChatAdapter } from '../src/adapters/chat-adapter.mjs';

const seen=[];
const chat=createChatAdapter({ edgeClient:{ invoke: async (name,body)=>{seen.push({name,body});return {ok:true,mode:'single',provider:'gpt',answer:'Hello',models_used:['openai/test'],guarded:true};} } });
await assert.rejects(()=>chat.ask('   '),/PROMPT_REQUIRED/);
const answer=await chat.ask('Hi',{provider:'gpt',mode:'single'});
assert.equal(answer.ok,true);
assert.equal(answer.answer,'Hello');
assert.equal(answer.guarded,true);
assert.deepEqual(seen[0],{name:'nova-orchestrator',body:{prompt:'Hi',provider:'gpt',mode:'single'}});
await assert.rejects(()=>chat.ask('Hi',{provider:'bogus'}),/INVALID_PROVIDER/);
await assert.rejects(()=>chat.ask('Hi',{mode:'bogus'}),/INVALID_MODE/);

const degraded=createChatAdapter({ edgeClient:{ invoke: async()=>({ok:true,mode:'ensemble-degraded',answer:'Partial',failures:[{model:'x'}],guarded:true}) } });
const d=await degraded.ask('Question');
assert.equal(d.degraded,true);
assert.equal(d.answer,'Partial');

const unavailable=createChatAdapter({ edgeClient:{ invoke: async()=>({ok:false,mode:'single-unavailable',answer:'Provider down',code:'OPENROUTER_TIMEOUT',guarded:true}) } });
const u=await unavailable.ask('Question');
assert.equal(u.ok,false);
assert.equal(u.code,'OPENROUTER_TIMEOUT');
console.log('chat-adapter: ok');
