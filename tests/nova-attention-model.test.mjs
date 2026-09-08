import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const model=require('../admin/nova-attention-model.js');

test('attention model keeps research work separate from approval findings',()=>{const x=model.build({findings:[{id:'a',device_id:1,field_name:'model_number',status:'open',requires_approval:true,severity:'high',confidence:.9,device_catalog:{brand:'A',model_name:'One'}},{id:'b',device_id:2,field_name:'release_year',status:'confirmed',requires_approval:true,severity:'medium',confidence:.8}],queue:[{id:9,device_id:3,status:'pending',priority:80,reason:'missing_model_number'}]});assert.equal(x.needsAttention,1);assert.equal(x.confirmed.length,1);assert.equal(x.pendingWork,1);assert.equal(x.open[0].device,'A One')});

test('only open approval-required findings permit a human decision',()=>{assert.equal(model.decisionAllowed(model.finding({id:'a',status:'open',requires_approval:true})),true);assert.equal(model.decisionAllowed(model.finding({id:'b',status:'confirmed',requires_approval:true})),false);assert.equal(model.decisionAllowed(model.finding({id:'c',status:'open',requires_approval:false})),false)});

test('unsafe source URLs are not exposed as evidence links',()=>{const x=model.finding({id:'a',status:'open',requires_approval:true,source_url:'javascript:alert(1)'});assert.equal(x.sourceUrl,null)});
