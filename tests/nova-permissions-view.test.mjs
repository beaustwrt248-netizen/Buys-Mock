import fs from 'node:fs';
import assert from 'node:assert/strict';

const app = fs.readFileSync('nova/app.js', 'utf8');
const permissions = fs.readFileSync('nova/permissions.js', 'utf8');

assert.match(app, /permissions\.js\?v=1/, 'Nova core must load the isolated permissions view');
assert.match(app, /let refreshVersion=0;/, 'permissions branch must preserve current Nova refresh freshness protection');
assert.match(app, /const requestVersion=\+\+refreshVersion/, 'permissions branch must preserve version-bound refreshes');
assert.match(permissions, /data-section=\"permissions\"/, 'permissions view must add a navigation entry');
assert.match(permissions, /data-page=\"permissions\"/, 'permissions view must add an isolated page');
assert.match(permissions, /Production writes[\s\S]*?DISABLED/, 'production write authority must be visibly disabled');
assert.match(permissions, /Auth, roles & RLS[\s\S]*?HUMAN/, 'auth and RLS must remain human controlled');
assert.match(permissions, /Pricing approvals[\s\S]*?HUMAN/, 'pricing approvals must remain human controlled');
assert.match(permissions, /Guardian repairs[\s\S]*?HUMAN/, 'Guardian repair authority must remain human controlled');
assert.match(permissions, /Releases & signing[\s\S]*?HUMAN/, 'release and signing authority must remain human controlled');
assert.match(permissions, /Workflow governance[\s\S]*?HUMAN/, 'workflow governance must remain human controlled');
assert.match(permissions, /NOVA_AUTONOMOUS_DEVELOPMENT\.md\?ref=main/, 'permissions evidence must read the current-main policy');
assert.match(permissions, /\.github\/workflows\/nova-pr-guard\.yml\?ref=main/, 'permissions evidence must read the current-main guard');
assert.match(permissions, /does not claim that a failed guard is technically impossible to merge/, 'UI must not overstate repository-level enforcement');
assert.doesNotMatch(permissions, /\b(?:POST|PUT|PATCH|DELETE)\b/, 'permissions view must not introduce HTTP write methods');
assert.doesNotMatch(permissions, /mergePull|approve|rerun|cancelWorkflow|updateFile|createFile/, 'permissions view must not introduce protected action functions');

console.log('nova permissions view regression: PASS');
