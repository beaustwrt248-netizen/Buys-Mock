import assert from 'node:assert/strict';
import { RUNTIME_CONFIG, assertPublicRuntimeConfig } from '../src/runtime-config.mjs';

assert.equal(assertPublicRuntimeConfig(), true);
assert.equal(RUNTIME_CONFIG.chatFunction, 'nova-orchestrator');
assert.match(RUNTIME_CONFIG.supabasePublishableKey, /^sb_publishable_/);
assert.doesNotMatch(JSON.stringify(RUNTIME_CONFIG), /service[_-]?role|openrouter/i);
assert.throws(() => assertPublicRuntimeConfig({ ...RUNTIME_CONFIG, dangerous: 'SUPABASE_SERVICE_ROLE_KEY=secret' }), /PRIVILEGED_SECRET/);
console.log('runtime-config: ok');
