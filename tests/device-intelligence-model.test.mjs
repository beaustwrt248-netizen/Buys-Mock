import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const modelApi=require('../admin/device-intelligence-model.js');

test('device intelligence fails closed when catalogue evidence is unavailable',()=>{const m=modelApi.normalise({catalogue:{state:'unavailable',devices:[{id:'invented',name:'Should not appear'}]}});assert.equal(m.state,'unavailable');assert.deepEqual(m.devices,[])});

test('confirmed catalogue records retain only supplied identity fields',()=>{const m=modelApi.normalise({updatedAt:'2026-09-08T00:00:00Z',catalogue:{state:'confirmed',devices:[{id:'d1',category:'phone',brand:'Samsung',name:'Galaxy Test',modelNumber:'SM-TEST',storage:'256GB'}],findings:[],source:{source:'authoritative catalogue'}}});assert.equal(m.state,'confirmed');assert.equal(m.devices[0].modelNumber,'SM-TEST');assert.equal(m.devices[0].releaseYear,null);assert.equal(m.devices[0].colour,null)});

test('search matches brand model number storage colour and category without inventing rows',()=>{const m=modelApi.normalise({catalogue:{state:'confirmed',devices:[{id:'1',category:'phone',brand:'Apple',name:'iPhone Alpha',modelNumber:'A100',storage:'128GB',colour:'Black'},{id:'2',category:'tablet',brand:'Samsung',name:'Tab Beta',modelNumber:'SM-T1',storage:'256GB',colour:'Blue'}]}});assert.equal(modelApi.search(m,'A100').length,1);assert.equal(modelApi.search(m,'256GB','tablet').length,1);assert.equal(modelApi.search(m,'Pixel').length,0)});

test('device detail carries only findings for the selected device',()=>{const m=modelApi.normalise({catalogue:{state:'confirmed',devices:[{id:'1',name:'One'},{id:'2',name:'Two'}],findings:[{deviceId:'1',type:'missing_image'},{deviceId:'2',type:'missing_storage'}]}});const d=modelApi.detail(m,'1');assert.equal(d.findings.length,1);assert.equal(d.findings[0].type,'missing_image')});

test('categories are unique and alphabetically sorted',()=>{const m=modelApi.normalise({catalogue:{state:'confirmed',devices:[{id:'1',category:'tablet'},{id:'2',category:'phone'},{id:'3',category:'tablet'}]}});assert.deepEqual(modelApi.categories(m),['phone','tablet'])});
