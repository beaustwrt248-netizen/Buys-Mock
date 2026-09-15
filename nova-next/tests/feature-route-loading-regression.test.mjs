import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const source = fs.readFileSync(path.resolve(here, '../src/feature-ui.mjs'), 'utf8');

assert.match(source, /FEATURE_ROUTE_TIMEOUT_MS/, 'feature routes must have a bounded UI deadline');
assert.match(source, /withRouteDeadline/, 'feature routes must stop waiting indefinitely for remote diagnostics');
assert.match(source, /Retry Control Centre/, 'Control Centre timeout state must provide an explicit retry action');
assert.match(source, /Loading support diagnostics/, 'Help must render a visible diagnostics loading state before awaiting remote status');
assert.match(source, /Retry diagnostics/, 'Help timeout state must provide an explicit retry action');
assert.match(source, /routeGeneration/, 'async feature-route rendering must guard against stale route completions');

console.log('feature-route-loading-regression: ok');
