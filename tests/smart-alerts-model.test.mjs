import test from 'node:test';
import assert from 'node:assert/strict';
import {createRequire} from 'node:module';
const require=createRequire(import.meta.url);
const model=require('../admin/smart-alerts-model.js');

test('smart alerts report clear state when no evidence-backed alerts exist',()=>{const m=model.build([]);assert.equal(m.state,'clear');assert.equal(m.counts.total,0);assert.equal(m.rows.length,0)});

test('smart alerts deduplicate repeated keys and prioritise severity',()=>{const rows=[{key:'a',type:'catalogue',severity:'medium',detail:{type:'missing_image'}},{key:'a',type:'catalogue',severity:'medium',detail:{type:'missing_image'}},{key:'b',type:'system',severity:'high',detail:{type:'deployment'}}];const m=model.build(rows);assert.equal(m.rows.length,2);assert.equal(m.rows[0].severity,'high');assert.equal(m.counts.total,2)});

test('smart alerts can filter by severity and source type',()=>{const rows=[{key:'a',type:'catalogue',severity:'medium'},{key:'b',type:'marketplace',severity:'high'}];assert.equal(model.build(rows,{severity:'high'}).rows[0].type,'marketplace');assert.equal(model.build(rows,{type:'catalogue'}).rows.length,1)});

test('smart alert actions route to review surfaces without executing changes',()=>{assert.equal(model.normalize({type:'catalogue'}).action.target,'devices');assert.equal(model.normalize({type:'marketplace'}).action.target,'devices');assert.equal(model.normalize({type:'system'}).action.target,'controls')});
