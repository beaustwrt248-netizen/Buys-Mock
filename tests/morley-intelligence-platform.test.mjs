import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const contracts=require('../admin/intelligence-contracts.js');
const engine=require('../admin/intelligence-engine.js');
const deviceAdapter=require('../admin/intelligence-device-adapter.js');

test('unavailable evidence never carries a fabricated value',()=>{const e=contracts.evidence({state:'unavailable',value:999,source:'missing'});assert.equal(e.value,null);assert.equal(e.state,'unavailable')});

test('protected Nova actions require both human and Guardian approval',()=>{const a=contracts.action({classification:'protected',label:'Delete marketplace listing'});assert.equal(contracts.canExecute(a,{authorised:true,humanApproved:true}),false);assert.equal(contracts.canExecute(a,{guardianApproved:true}),false);assert.equal(contracts.canExecute(a,{humanApproved:true,guardianApproved:true}),true)});

test('catalogue autopilot flags evidence gaps and exact duplicates',()=>{const findings=engine.catalogueFindings([{id:'1',category:'phone',brand:'Acme',name:'One'},{id:'2',category:'phone',brand:'Acme',name:'One'}]);assert.ok(findings.some(x=>x.type==='duplicate'));assert.ok(findings.some(x=>x.type==='missing_model_number'));assert.ok(findings.some(x=>x.type==='missing_storage'));assert.ok(findings.some(x=>x.type==='missing_image'))});

test('pricing engine fails closed without confirmed Australian price evidence',()=>{const r=engine.pricingRecommendation([{price:500,currency:'AUD',confirmed:false}]);assert.equal(r.state,'unavailable');assert.equal(r.recommendedSell,null);assert.equal(r.recommendedBuy,null)});

test('pricing engine returns range and margin from confirmed observations',()=>{const r=engine.pricingRecommendation([{price:400,currency:'AUD',confirmed:true},{price:500,currency:'AUD',confirmed:true},{price:600,currency:'AUD',confirmed:true}],{targetGrossMarginRate:.4});assert.deepEqual([r.marketLow,r.marketHigh,r.median],[400,600,500]);assert.equal(r.recommendedSell,500);assert.equal(r.recommendedBuy,300);assert.equal(r.expectedGrossMargin,200)});

test('marketplace reconciliation never turns missing listings into deletion',()=>{const rows=engine.marketplaceMatch([{stockNumber:'12345',price:200}],[]);assert.equal(rows.length,1);assert.equal(rows[0].status,'missing_listing');assert.ok(!('delete' in rows[0]))});

test('image identification remains a proposal requiring staff confirmation',()=>{const p=engine.identificationProposal({brand:'Samsung',modelNumber:'SM-X000',evidence:['visible rear label']});assert.equal(p.state,'proposed');assert.equal(p.requiresStaffConfirmation,true)});

test('device testing distinguishes unsupported from failure and only trusts platform-verified automation',()=>{const rows=engine.testSession([{name:'NFC',state:'unsupported',automated:true,platformVerified:false},{name:'Speaker',state:'pass',automated:true,platformVerified:true}]);assert.equal(rows[0].state,'unsupported');assert.equal(rows[0].automated,false);assert.equal(rows[1].automated,true)});

test('smart alerts deduplicate stable alert keys',()=>{const finding={type:'missing_image',deviceId:'d1',severity:'medium'};const alerts=engine.smartAlerts({catalogueFindings:[finding,finding]});assert.equal(alerts.length,1)});

test('device catalogue adapter preserves Australian identity and provenance',()=>{const row=deviceAdapter.normalize({id:42,category:'mobile_phone',brand:'Samsung',family:'Galaxy S',model_name:'Galaxy S26',model_number:'SM-S000',release_year:2026,ram_options:['12GB'],storage_options:['256GB','512GB'],image_reference_url:'https://example.invalid/device.jpg',source_url:'https://manufacturer.invalid/device',source_name:'Manufacturer AU',source_checked_at:'2026-09-08T00:00:00Z',market_region:'Australia',sim_configuration:'nano-SIM + eSIM',physical_sim_slots:1,esim_supported:true,dual_sim_supported:true});assert.equal(row.id,'42');assert.equal(row.storage,'256GB / 512GB');assert.equal(row.marketRegion,'Australia');assert.equal(row.sourceName,'Manufacturer AU');assert.equal(row.evidence.length,1);assert.equal(row.esimSupported,true)});

test('device catalogue adapter pages until the final short batch',async()=>{const calls=[];const batches=[[{id:1,category:'tablet',brand:'A',model_name:'One',storage_options:['128GB'],ram_options:[]}],[{id:2,category:'tablet',brand:'A',model_name:'Two',storage_options:['256GB'],ram_options:[]}],[]];const sb={from(){return{select(){return this},eq(){return this},order(){return this},range(from,to){calls.push([from,to]);return Promise.resolve({data:batches[calls.length-1],error:null})}}}};const rows=await deviceAdapter.fetchAll(sb,{pageSize:1,maxRows:10});assert.equal(rows.length,2);assert.deepEqual(calls,[[0,0],[1,1],[2,2]])});

test('device catalogue refresh fails closed and clears stale catalogue on query error',async()=>{let cleared=null,ingested=false;const runtime={ingest(){ingested=true},clear(kind){cleared=kind}};const sb={from(){return{select(){return this},eq(){return this},order(){return this},range(){return Promise.resolve({data:null,error:{message:'offline'}})}}}};const result=await deviceAdapter.refresh({sb,runtime});assert.equal(result.state,'unavailable');assert.equal(result.count,null);assert.equal(cleared,'catalogue');assert.equal(ingested,false)});
