import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const autopilot=require('../admin/catalogue-autopilot-model.js');

test('autopilot fails closed without confirmed catalogue evidence',()=>{const m=autopilot.build({catalogue:{state:'unavailable',devices:[{id:'x'}],findings:[{deviceId:'x',type:'missing_image'}]}});assert.equal(m.state,'unavailable');assert.deepEqual(m.items,[])});

test('autopilot prioritises high severity findings before medium and low',()=>{const m=autopilot.build({catalogue:{state:'confirmed',devices:[{id:'1',brand:'A',name:'One'}],findings:[{deviceId:'1',type:'missing_image',severity:'low'},{deviceId:'1',type:'missing_release_year',severity:'medium'},{deviceId:'1',type:'missing_model_number',severity:'high'}]}});assert.deepEqual(m.items.map(x=>x.severity),['high','medium','low'])});

test('autopilot filters by query severity and finding type',()=>{const m=autopilot.build({catalogue:{state:'confirmed',devices:[{id:'1',brand:'Samsung',name:'Galaxy Alpha',modelNumber:'SM-A1'},{id:'2',brand:'Apple',name:'iPhone Beta',modelNumber:'A2'}],findings:[{deviceId:'1',type:'missing_image',severity:'medium'},{deviceId:'2',type:'missing_model_number',severity:'high'}]}});assert.equal(autopilot.filter(m,{query:'Samsung'}).length,1);assert.equal(autopilot.filter(m,{severity:'high'}).length,1);assert.equal(autopilot.filter(m,{type:'missing_image'}).length,1)});

test('autopilot proposals remain non-destructive and approval required',()=>{const p=autopilot.proposal({deviceId:'1',type:'duplicate',field:null});assert.equal(p.status,'proposed');assert.equal(p.classification,'approval_required');assert.equal(p.destructive,false);assert.equal(p.evidenceRequired,true)});

test('autopilot exposes stable unique finding types',()=>{const m=autopilot.build({catalogue:{state:'confirmed',devices:[{id:'1'}],findings:[{deviceId:'1',type:'missing_image'},{deviceId:'1',type:'missing_image'},{deviceId:'1',type:'missing_storage'}]}});assert.deepEqual(autopilot.types(m),['missing_image','missing_storage'])});
