import assert from 'node:assert/strict';
import fs from 'node:fs';
import vm from 'node:vm';

const source = fs.readFileSync('nova/recommendations.js', 'utf8');
const context = {
  console,
  URL,
  window: {
    addEventListener() {},
    NovaCatalogueLive: { load: async () => null },
  },
  document: {
    getElementById() { return null; },
    addEventListener() {},
    createElement() { return {}; },
    head: { appendChild() {} },
  },
  fetch: async () => { throw new Error('network should not run in policy test'); },
};
vm.createContext(context);
vm.runInContext(source, context);

const policy = context.window.__NovaRecommendationPolicy;
assert.ok(policy, 'recommendation policy should be exposed for regression testing');

assert.equal(policy.requiresHumanDecision({
  head: { ref: 'nova/example' },
  title: 'Protected change',
  body: 'Risk: High',
}), true, 'explicit high-risk Nova work must remain a human decision');

assert.equal(policy.requiresHumanDecision({
  head: { ref: 'guardian/repair-123' },
  title: 'Guardian repair',
  body: 'This repair remains draft and human-merge-only.',
}), true, 'human-only Guardian repairs must surface even outside nova/* branches');

assert.equal(policy.requiresHumanDecision({
  head: { ref: 'release/example' },
  title: 'Release promotion',
  body: 'This protected step requires explicit human approval before merge.',
}), true, 'explicit release approvals must surface as human decisions');

assert.equal(policy.requiresHumanDecision({
  head: { ref: 'nova/safe-read-only' },
  title: 'Read-only health improvement',
  body: 'Risk: Low. No protected action is performed.',
}), false, 'ordinary low-risk Nova work must not be escalated');

assert.equal(policy.requiresHumanDecision({
  head: { ref: 'feature/example' },
  title: 'Unrelated feature',
  body: 'Authentication remains human-controlled elsewhere in the product.',
}), false, 'generic boundary documentation must not falsely gate an unrelated PR');

console.log('Nova recommendation human-gate policy: PASS');
