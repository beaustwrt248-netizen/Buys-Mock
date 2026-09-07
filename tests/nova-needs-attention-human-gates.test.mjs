import assert from 'node:assert/strict';
import fs from 'node:fs';

const source = fs.readFileSync('nova/app-core.js', 'utf8');

assert.match(source, /__NovaRecommendationPolicy\?\.requiresHumanDecision/, 'Needs Attention must reuse the shared protected human-decision policy');
assert.match(source, /const attention=state\.prs\.filter\(requiresHumanDecision\)/, 'Needs Attention must scan all open PRs for explicit protected human decisions');
assert.match(source, /const novaPrs=state\.prs\.filter/, 'Nova work/development queues must remain scoped to Nova branches');
assert.match(source, /protected pull request/, 'Overview copy must describe protected human decisions rather than only High-risk Nova PRs');
assert.doesNotMatch(source, /const attention=novaPrs\.filter\(p=>riskFor\(p\)===['"]high['"]\)/, 'Legacy Nova-only attention filtering must be removed');

console.log('Nova Needs Attention protected-human-gate integration: PASS');
