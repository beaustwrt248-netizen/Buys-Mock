import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const model=require('../admin/pricing-intelligence-model.js');

test('pricing intelligence fails closed without observations',()=>{const m=model.build({pricing:{state:'unavailable'},pricingObservations:[],systemEvidence:[]});assert.equal(m.state,'unavailable');assert.equal(m.confirmedCount,0);assert.equal(model.confidenceLabel(m),'Unavailable')});

test('pricing intelligence counts only confirmed AUD observations',()=>{const m=model.build({pricing:{state:'confirmed',confidence:.6},pricingObservations:[{source:'A',price:500,currency:'AUD',confirmed:true,directSeller:true},{source:'B',price:550,currency:'USD',confirmed:true},{source:'C',price:530,currency:'AUD',confirmed:false}],systemEvidence:[]});assert.equal(m.confirmedCount,1);assert.equal(m.sourceCounts.direct,1);assert.equal(model.confidenceLabel(m),'Medium')});

test('pricing intelligence marks stale evidence',()=>{const now=Date.parse('2026-09-08T00:00:00Z');const m=model.build({pricing:{state:'confirmed'},pricingObservations:[{source:'Old',price:400,currency:'AUD',confirmed:true,observedAt:'2026-07-01T00:00:00Z'}],systemEvidence:[]},{now});assert.equal(m.staleCount,1);assert.equal(model.freshness('2026-09-05T00:00:00Z',now),'fresh')});

test('pricing intelligence normalizes source quality safely',()=>{assert.equal(model.normalizeObservation({source:'Retailer',price:999,currency:'AUD',confirmed:true,directSeller:true}).quality,'direct');assert.equal(model.normalizeObservation({price:999,currency:'AUD',confirmed:false}).quality,'unconfirmed')});
