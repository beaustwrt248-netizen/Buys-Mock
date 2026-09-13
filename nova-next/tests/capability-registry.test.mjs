import assert from 'node:assert/strict';
import { CAPABILITIES, capabilitiesForSection } from '../src/capabilities.mjs';

const required = [
  'conversation', 'web-research', 'camera-vision', 'catalogue-intelligence',
  'pricing-intelligence', 'support-intelligence', 'guardian-analysis',
  'release-readiness', 'bug-triage', 'business-intelligence', 'memory',
  'voice', 'scenario-planning', 'multi-step-jobs', 'proactive-alerts',
  'evidence-review', 'explain-mode', 'command-discovery', 'learning-controls'
];

for (const id of required) {
  assert.equal(CAPABILITIES.some(item => item.id === id), true, `missing ${id}`);
}
for (const item of CAPABILITIES) {
  assert.equal(typeof item.label, 'string');
  assert.equal(typeof item.section, 'string');
  assert.equal(typeof item.risk, 'string');
  assert.equal(item.requiresAuth, true);
  assert.equal(typeof item.adapter, 'string');
}
assert.equal(capabilitiesForSection('tools').every(item => item.section === 'tools'), true);
console.log('capability-registry: ok');
