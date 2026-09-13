import assert from 'node:assert/strict';
import { createEdgeFunctionClient } from '../src/adapters/edge-function-client.mjs';

function response(status, body) { return { ok: status >= 200 && status < 300, status, async json(){ return body; } }; }
const calls=[];
const client=createEdgeFunctionClient({
  baseUrl:'https://example.supabase.co',
  publishableKey:'pk_test',
  getAccessToken:()=> 'access',
  fetchImpl: async (url,init)=>{calls.push({url,init});return response(200,{ok:true,answer:'hello'});}
});
const result=await client.invoke('nova-orchestrator',{prompt:'Hi'});
assert.equal(result.ok,true);
assert.equal(calls[0].url,'https://example.supabase.co/functions/v1/nova-orchestrator');
assert.equal(calls[0].init.method,'POST');
assert.equal(calls[0].init.headers.apikey,'pk_test');
assert.equal(calls[0].init.headers.Authorization,'Bearer access');
assert.deepEqual(JSON.parse(calls[0].init.body),{prompt:'Hi'});
await assert.rejects(()=>client.invoke('guardian-repair-executor',{}),/EDGE_FUNCTION_BLOCKED/);
assert.equal(calls.length,1);

const unauth=createEdgeFunctionClient({baseUrl:'https://example.supabase.co',publishableKey:'pk',getAccessToken:()=>'',fetchImpl:async()=>response(200,{})});
await assert.rejects(()=>unauth.invoke('nova-orchestrator',{}),/AUTH_REQUIRED/);

const failed=createEdgeFunctionClient({baseUrl:'https://example.supabase.co',publishableKey:'pk',getAccessToken:()=> 'a',fetchImpl:async()=>response(403,{error:'Admin access required'})});
await assert.rejects(()=>failed.invoke('nova-orchestrator',{}),/Admin access required/);
console.log('edge-function-client: ok');
