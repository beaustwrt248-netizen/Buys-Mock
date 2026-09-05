import fs from 'node:fs';
import assert from 'node:assert/strict';

const source = fs.readFileSync('nova/recommendations.js', 'utf8');

assert.match(source, /Fix current failure: \$\{r\.name\}/, 'current workflow failures must be recommendation blockers');
assert.match(source, /,1,'BLOCKER'/, 'workflow failures must have priority 1');
assert.match(source, /,2,'HUMAN'/, 'explicit high-risk Nova PRs must have priority 2');
assert.match(source, /,3,'SUPPORT'/, 'support pressure must rank after blockers and human gates');
assert.match(source, /,4,'DATA'/, 'catalogue data quality must rank after operational pressure');
assert.match(source, /,5,'LEARNING'/, 'learning coverage must remain lower priority than operational/data integrity work');
assert.match(source, /function declaredHigh\(pr\)[\s\S]*?high\[- \]risk/, 'classifier must recognise explicit high-risk wording');
assert.match(source, /risk\\s\*\[:=-\][\s\S]*?high/, 'classifier must recognise explicit Risk: High declarations');
assert.match(source, /#\{1,6\}\\s\*risk[\s\S]*?high/, 'classifier must recognise Markdown Risk sections declaring High');
assert.match(source, /if\(!ref\.startsWith\('nova\/'\)\)return false/, 'only Nova branches may be treated as Nova high-risk recommendations');
assert.match(source, /recommendations\.sort\(\(a,b\)=>a\.priority-b\.priority\)/, 'recommendations must be rendered in priority order');
assert.match(source, /advisory only and cannot bypass approval, Guardian, auth\/RLS, pricing or release controls/, 'recommendations must preserve protected boundaries');
assert.doesNotMatch(source, /fetch\([^\n]*(POST|PUT|PATCH|DELETE)/i, 'recommendations module must not introduce write requests');

console.log('nova recommendations priority contract: PASS');
