import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const contracts=require('../admin/intelligence-contracts.js');
const engine=require('../admin/intelligence-engine.js');

test('unavailable evidence never carries a fabricated value',()=>{const e=contracts.evidence({state:'unavailable',value:999,source:'missing'});assert.equal(e.value,null);assert.equal(e.state,'unavailable')});

test('protected Nova actions require both human and Guardian approval',()=>{const a=contracts.action({classification:'protected',label:'Delete marketplace listing'});assert.equal(contracts.canExecute(a,{authorised:true,humanApproved:true}),false);assert.equal(contracts.canExecute(a,{guardianApproved:true}),false);assert.equal(contracts.canExecute(a,{humanApproved:true,guardianApproved:true}),true)});

test('catalogue autopilot flags evidence gaps and exact duplicates',()=>{const findings=engine.catalogueFindings([{id:'1',category:'phone',brand:'Acme',name:'One'},{id:'2',category:'phone',brand:'Acme',name:'One'}]);assert.ok(findings.some(x=>x.type==='duplicate'));assert.ok(findings.some(x=>x.type==='missing_model_number'));assert.ok(findings.some(x=>x.type==='missing_storage'));assert.ok(findings.some(x=>x.type==='missing_image'))});

test('pricing engine fails closed without confirmed Australian price evidence',()=>{const r=engine.pricingRecommendation([{price:500,currency:'AUD',confirmed:false}]);assert.equal(r.state,'unavailable');assert.equal(r.recommendedSell,null);assert.equal(r.recommendedBuy,null)});

test('pricing engine returns range and margin from confirmed observations',()=>{const r=engine.pricingRecommendation([{price:400,currency:'AUD',confirmed:true},{price:500,currency:'AUD',confirmed:true},{price:600,currency:'AUD',confirmed:true}],{targetGrossMarginRate:.4});assert.deepEqual([r.marketLow,r.marketHigh,r.median],[400,600,500]);assert.equal(r.recommendedSell,500);assert.equal(r.recommendedBuy,300);assert.equal(r.expectedGrossMargin,200)});

test('marketplace reconciliation never turns missing listings into deletion',()=>{const rows=engine.marketplaceMatch([{stockNumber:'12345',price:200}],[]);assert.equal(rows.length,1);assert.equal(rows[0].status,'missing_listing');assert.ok(!('delete' in rows[0]))});

test('image identification remains a proposal requiring staff confirmation',()=>{const p=engine.identificationProposal({brand:'Samsung',modelNumber:'SM-X000',evidence:['visible rear label']});assert.equal(p.state,'proposed');assert.equal(p.requiresStaffConfirmation,true)});

test('device testing distinguishes unsupported from failure and only trusts platform-verified automation',()=>{const rows=engine.testSession([{name:'NFC',state:'unsupported',automated:true,platformVerified:false},{name:'Speaker',state:'pass',automated:true,platformVerified:true}]);assert.equal(rows[0].state,'unsupported');assert.equal(rows[0].automated,false);assert.equal(rows[1].automated,true)});

test('smart alerts deduplicate stable alert keys',()=>{const finding={type:'missing_image',deviceId:'d1',severity:'medium'};const alerts=engine.smartAlerts({catalogueFindings:[finding,finding]});assert.equal(alerts.length,1)});
