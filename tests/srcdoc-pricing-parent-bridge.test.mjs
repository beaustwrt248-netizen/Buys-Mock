import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';

const source = readFileSync(new URL('../morley-central-pricing.js', import.meta.url), 'utf8');

assert.match(source, /BRIDGE_NAMESPACE='morley\.catalogue\.bridge\.v1'/, 'bridge namespace must be explicit and versioned');
assert.match(source, /BRIDGE_ACTION='catalogue\.read'/, 'bridge must expose only the catalogue read action');
assert.match(source, /location\.origin==='null'\|\|location\.protocol==='about:'/, 'opaque srcdoc runtimes must be detected');
assert.match(source, /document\.querySelectorAll\('iframe\[srcdoc\]'\)/, 'parent must limit requests to owned srcdoc frames');
assert.match(source, /frame\.contentWindow===source/, 'parent must authenticate the requesting window by identity');
assert.match(source, /event\.source!==window\.parent/, 'child must accept responses only from its parent window');
assert.match(source, /message\.kind!=='response'/, 'child must accept only bridge response messages');
assert.match(source, /message\.action!==BRIDGE_ACTION/, 'unexpected bridge actions must be rejected');
assert.match(source, /setTimeout\(\(\)=>finish\(new Error\('Catalogue bridge request timed out'\)\),15000\)/, 'bridge requests must have a bounded timeout');
assert.match(source, /fetchCatalogueViaParent\(\)/, 'sync must have a parent-bridge path');
assert.match(source, /if\(!isOpaqueRuntime\(\)\)return fetchCatalogueDirect\(\)/, 'normal top-level runtime must continue to fetch directly');
assert.doesNotMatch(source, /Access-Control-Allow-Origin/, 'client bridge must not weaken server CORS');
assert.doesNotMatch(source, /origin\s*===?\s*['"]null['"]\s*&&\s*allow/i, 'null origins must never be trusted as authorization');

console.log('srcdoc pricing parent bridge contract: ok');
