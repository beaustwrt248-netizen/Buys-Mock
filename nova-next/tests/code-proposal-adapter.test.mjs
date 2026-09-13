import assert from 'node:assert/strict';
import { createCodeProposalAdapter } from '../src/adapters/code-proposal-adapter.mjs';

const calls=[];
const adapter=createCodeProposalAdapter({edgeClient:{invoke:async(name,body)=>{calls.push({name,body});return {ok:true,proposal:{summary:'x',changes:[],tests_run:false,protected_paths_rejected:true}};}}});
const result=await adapter.propose({diagnostic:'The Home card layout overflows on small screens.',candidateFiles:['nova-next/styles.css','nova-next/app.js']});
assert.equal(result.ok,true);
assert.deepEqual(calls[0],{name:'nova-code-proposal',body:{diagnostic:'The Home card layout overflows on small screens.',candidate_files:['nova-next/styles.css','nova-next/app.js']}});
await assert.rejects(()=>adapter.propose({diagnostic:'short',candidateFiles:['nova-next/app.js']}),/DIAGNOSTIC_REQUIRED/);
await assert.rejects(()=>adapter.propose({diagnostic:'A sufficiently detailed issue description',candidateFiles:[]}),/CANDIDATE_FILES_REQUIRED/);
await assert.rejects(()=>adapter.propose({diagnostic:'A sufficiently detailed issue description',candidateFiles:['nova/app.js']}),/PROTECTED_PATH/);
await assert.rejects(()=>adapter.propose({diagnostic:'A sufficiently detailed issue description',candidateFiles:['supabase/functions/x/index.ts']}),/PROTECTED_PATH/);
await assert.rejects(()=>adapter.propose({diagnostic:'A sufficiently detailed issue description',candidateFiles:['.github/workflows/x.yml']}),/PROTECTED_PATH/);
console.log('code-proposal-adapter: ok');
