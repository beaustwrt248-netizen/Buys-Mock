import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const model=require('../admin/marketplace-manager-model.js');

test('marketplace manager fails closed without reconciliation rows',()=>{const m=model.build([]);assert.equal(m.state,'unavailable');assert.equal(m.rows.length,0);assert.equal(m.counts.total,0)});

test('marketplace manager prioritises missing and mismatch rows',()=>{const m=model.build([{status:'matched',stockNumber:'1'},{status:'price_mismatch',stockNumber:'2'},{status:'missing_listing',stockNumber:'3'}]);assert.deepEqual(m.rows.map(x=>x.status),['missing_listing','price_mismatch','matched'])});

test('marketplace proposals never imply automatic deletion',()=>{for(const status of ['missing_listing','price_mismatch','uncertain_match','unmatched_listing','duplicate','matched']){const p=model.proposal(status);assert.equal(p.destructive,false);assert.ok(!/delete/i.test(p.label))}});

test('marketplace manager filters by status and evidence text',()=>{const rows=[{status:'missing_listing',stockNumber:'123',stock:{name:'Pixel'}},{status:'matched',stockNumber:'456',listing:{title:'Galaxy S24',url:'https://example.test/456'}}];assert.equal(model.build(rows,{status:'matched'}).rows.length,1);assert.equal(model.build(rows,{query:'pixel'}).rows[0].stockNumber,'123');assert.equal(model.build(rows,{query:'example.test'}).rows[0].stockNumber,'456')});
