import assert from 'node:assert/strict';
import { createSafeServices } from '../src/safe-services.mjs';

const edgeClient={invoke:async(name,body)=>({ok:true,name,body})};
const services=createSafeServices({edgeClient});
assert.equal(typeof services.chat.ask,'function');
assert.equal(typeof services.knowledge.search,'function');
assert.equal(typeof services.learning.summary,'function');
assert.equal(typeof services.attention.list,'function');
assert.equal(typeof services.metrics.summary,'function');
assert.equal(typeof services.github.status,'function');
assert.equal(typeof services.vision.analyse,'function');
assert.equal(typeof services.codeProposal.propose,'function');
assert.equal('review' in services.attention,false);
assert.equal('harvest' in services.learning,false);
assert.equal('prepareDraft' in services.github,false);
console.log('safe-services: ok');
