import fs from 'node:fs';
import assert from 'node:assert/strict';

const source = fs.readFileSync('nova/app.js', 'utf8');

assert.match(source, /let refreshVersion=0;/, 'Nova refreshes must keep a monotonic freshness version');
assert.match(source, /async function refresh\(\)\{const requestVersion=\+\+refreshVersion;/, 'each refresh must capture a new request version before async work');
assert.match(source, /await Promise\.all\([\s\S]*?if\(requestVersion!==refreshVersion\)return;state\.main=main;/, 'stale successful refreshes must be rejected before mutating shared state');
assert.match(source, /catch\(e\)\{if\(requestVersion!==refreshVersion\)return;console\.error\(e\)/, 'stale refresh failures must not repaint a newer successful state as degraded');

console.log('nova refresh stale-result contract: PASS');
