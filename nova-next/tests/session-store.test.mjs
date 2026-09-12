import assert from 'node:assert/strict';
import { createSessionStore } from '../src/session-store.mjs';

function memoryStorage() {
  const map = new Map();
  return { getItem:k=>map.has(k)?map.get(k):null, setItem:(k,v)=>map.set(k,String(v)), removeItem:k=>map.delete(k), dump:()=>Object.fromEntries(map) };
}
const storage = memoryStorage();
const store = createSessionStore({ storage, key: 'test.session' });
store.save({ access_token:'a', refresh_token:'r', user:{ id:'u1', email:'a@b.com' }, password:'must-not-save' });
const raw = storage.getItem('test.session');
assert.equal(raw.includes('must-not-save'), false);
assert.equal(store.load().access_token, 'a');
store.clear();
assert.equal(store.load(), null);
storage.setItem('test.session', '{not-json');
assert.equal(store.load(), null);
assert.equal(storage.getItem('test.session'), null);
console.log('session-store: ok');
