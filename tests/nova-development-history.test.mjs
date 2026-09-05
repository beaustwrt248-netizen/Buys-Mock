import assert from 'node:assert/strict';
import fs from 'node:fs';
import test from 'node:test';

const history = fs.readFileSync(new URL('../nova/development-history.js', import.meta.url), 'utf8');
const recommendations = fs.readFileSync(new URL('../nova/recommendations.js', import.meta.url), 'utf8');

test('development history only selects Nova branches', () => {
  assert.match(history, /startsWith\('nova\/'\)/);
  assert.match(history, /pulls\?state=closed/);
});

test('development history distinguishes merged and closed outcomes', () => {
  assert.match(history, /merged_at/);
  assert.match(history, /label:'MERGED'/);
  assert.match(history, /label:'CLOSED'/);
});

test('development history is read-only', () => {
  assert.doesNotMatch(history, /method\s*:\s*['"](?:POST|PUT|PATCH|DELETE)['"]/i);
  assert.doesNotMatch(history, /\/merge|\/reviews|\/comments|\/dispatches/);
  assert.match(history, /No protected action was attempted/);
});

test('Nova shell loads the development history module', () => {
  assert.match(recommendations, /development-history\.js\?v=1/);
});
