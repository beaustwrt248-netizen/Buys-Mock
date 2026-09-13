import assert from 'node:assert/strict';
import { createRouter } from '../src/router.mjs';

const seen = [];
const router = createRouter({ onRoute: route => seen.push(route), initialRoute: 'home' });
assert.equal(router.current(), 'home');
router.go('chat');
assert.equal(router.current(), 'chat');
router.go('not-real');
assert.equal(router.current(), 'home');
assert.deepEqual(seen, ['chat', 'home']);
console.log('router: ok');
