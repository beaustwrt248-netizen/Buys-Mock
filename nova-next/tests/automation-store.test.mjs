import test from 'node:test';
import assert from 'node:assert/strict';
import { createAutomationStore } from '../src/automation-store.mjs';

function storage(seed={}){ const map=new Map(Object.entries(seed)); return { getItem:k=>map.get(k)??null, setItem:(k,v)=>map.set(k,String(v)), removeItem:k=>map.delete(k), dump:()=>Object.fromEntries(map) }; }

test('local jobs support immutable CRUD and approved states', () => {
  const store=createAutomationStore({ storage:storage(), now:()=>new Date('2026-09-13T00:00:00Z'), idFactory:()=> 'job-1' });
  const created=store.create({ title:'Morning summary', prompt:'Summarise tasks', scheduleText:'Every morning', state:'draft' });
  assert.equal(created.id,'job-1'); assert.equal(Object.isFrozen(created),true);
  assert.equal(store.list().length,1);
  assert.equal(store.update('job-1',{ state:'enabled' }).state,'enabled');
  assert.equal(store.remove('job-1'),true); assert.equal(store.list().length,0);
});

test('corrupt payload resets and invalid states are rejected', () => {
  const store=createAutomationStore({ storage:storage({'nova-next.automation.v1':'{bad'}), idFactory:()=> 'a' });
  assert.deepEqual(store.list(),[]);
  assert.throws(()=>store.create({ title:'x', prompt:'y', state:'running' }),/AUTOMATION_STATE_INVALID/);
});

test('write failure rolls back', () => {
  const bad={ getItem:()=>null, setItem:()=>{throw new Error('quota')}, removeItem:()=>{} };
  const store=createAutomationStore({ storage:bad, idFactory:()=> 'a' });
  assert.throws(()=>store.create({ title:'x', prompt:'y' }),/AUTOMATION_WRITE_FAILED/);
  assert.deepEqual(store.list(),[]);
});
