import assert from 'node:assert/strict';
import { createKnowledgeReadAdapter, createLearningReadAdapter, createAttentionReadAdapter, createMetricsReadAdapter, createGithubStatusAdapter } from '../src/adapters/read-adapters.mjs';

const calls=[];
const edgeClient={invoke:async(name,body)=>{calls.push({name,body});return {ok:true,name,body};}};

const knowledge=createKnowledgeReadAdapter({edgeClient});
await knowledge.summary();
await knowledge.list({category:'catalogue',limit:25});
await knowledge.search('pixel 9',{category:'catalogue',limit:10});
await knowledge.get('item-1');
assert.deepEqual(calls.slice(0,4),[
  {name:'nova-knowledge',body:{action:'summary'}},
  {name:'nova-knowledge',body:{action:'list',category:'catalogue',limit:25}},
  {name:'nova-knowledge',body:{action:'search',q:'pixel 9',category:'catalogue',limit:10}},
  {name:'nova-knowledge',body:{action:'get',id:'item-1'}}
]);
assert.equal('create' in knowledge,false);
assert.equal('update' in knowledge,false);
await assert.rejects(()=>knowledge.search(''),/QUERY_REQUIRED/);

const learning=createLearningReadAdapter({edgeClient});
await learning.summary();
assert.equal('harvest' in learning,false);
assert.equal('feedback' in learning,false);

const attention=createAttentionReadAdapter({edgeClient});
await attention.list({limit:120});
assert.equal('review' in attention,false);

const metrics=createMetricsReadAdapter({edgeClient});
await metrics.summary();

const github=createGithubStatusAdapter({edgeClient});
await github.status();
assert.equal('prepareDraft' in github,false);

assert.deepEqual(calls.slice(4),[
  {name:'nova-learning',body:{action:'summary'}},
  {name:'nova-attention-control',body:{action:'list',limit:120}},
  {name:'nova-ai-metrics',body:{}},
  {name:'nova-github',body:{action:'status'}}
]);
console.log('read-adapters: ok');
