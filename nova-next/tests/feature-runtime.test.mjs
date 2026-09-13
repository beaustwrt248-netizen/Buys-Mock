import assert from 'node:assert/strict';
import { createFeatureRuntime } from '../src/feature-runtime.mjs';

const calls=[];
const edgeClient={invoke:async(name,body)=>{calls.push({name,body});if(name==='nova-ai-metrics')return{ok:true,total_runs:10};if(name==='nova-attention-control')return{ok:true,counts:{needs_attention:2}};if(name==='nova-github')return{ok:true,configured:true};if(name==='nova-learning')return{ok:true,count:5};return{ok:true};}};
const runtime=createFeatureRuntime({getAccessToken:()=> 'a',edgeClient});
const centre=await runtime.controlCentre();
assert.equal(centre.metrics.ok,true);
assert.equal(centre.metrics.data.total_runs,10);
assert.equal(centre.attention.data.counts.needs_attention,2);
assert.equal(centre.github.data.configured,true);
assert.equal(centre.learning.data.count,5);
await runtime.knowledgeSummary();
assert.deepEqual(calls.at(-1),{name:'nova-knowledge',body:{action:'summary'}});
console.log('feature-runtime: ok');
