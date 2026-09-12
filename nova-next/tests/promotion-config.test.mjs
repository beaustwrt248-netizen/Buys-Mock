import assert from 'node:assert/strict';
import { CHANNELS, promotionChecklist } from '../src/promotion-config.mjs';

assert.notEqual(CHANNELS.development.appId, CHANNELS.production.appId);
assert.notEqual(CHANNELS.development.cacheNamespace, CHANNELS.production.cacheNamespace);
assert.equal(CHANNELS.production.requiresExplicitPromotion, true);
assert.equal(CHANNELS.development.requiresExplicitPromotion, false);
assert.equal(promotionChecklist().includes('explicit-human-approval'), true);
console.log('promotion-config: ok');
