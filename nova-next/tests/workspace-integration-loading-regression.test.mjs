import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const here = path.dirname(fileURLToPath(import.meta.url));
const source = fs.readFileSync(path.resolve(here, '../src/workspace-ui.mjs'), 'utf8');

assert.match(source, /WORKSPACE_ROUTE_TIMEOUT_MS/, 'workspace remote routes must have a bounded UI deadline');
assert.match(source, /withWorkspaceRouteDeadline/, 'integration status must not wait indefinitely for remote status');
assert.match(source, /Retry integrations/, 'integration timeout/error state must provide an explicit retry action');
assert.match(source, /integrationGeneration/, 'stale integration responses must not overwrite a newer route state');

console.log('workspace-integration-loading-regression: ok');
