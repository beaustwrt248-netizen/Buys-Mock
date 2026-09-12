import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const registry = fs.readFileSync(new URL('../nova/command-registry.js', import.meta.url), 'utf8');

function commandBlock(id) {
  const escaped = id.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  const match = registry.match(new RegExp(`command\\('${escaped}'[\\s\\S]*?\\)\\s*[,;]`));
  return match?.[0] || '';
}

test('registers read-only assessment status and explanation commands', () => {
  assert.match(registry, /assessment\.status\.read/);
  assert.match(registry, /assessment\.explain\.read/);
  assert.match(commandBlock('assessment.status.read'), /risk:RISK\.SAFE/);
  assert.match(commandBlock('assessment.explain.read'), /risk:RISK\.SAFE/);
});

test('registers repair opportunity and exception reads as safe', () => {
  assert.match(commandBlock('assessment.exceptions.read'), /risk:RISK\.SAFE/);
  assert.match(commandBlock('assessment.repair_opportunities.read'), /risk:RISK\.SAFE/);
});

test('commercial confirmation and stock publication remain protected', () => {
  const confirm = commandBlock('assessment.commercial.confirm');
  const stock = commandBlock('assessment.stock.publish');
  assert.match(confirm, /risk:RISK\.SENSITIVE/);
  assert.match(confirm, /guardian:true/);
  assert.match(stock, /risk:RISK\.(?:SENSITIVE|WRITE|DESTRUCTIVE)/);
  assert.match(stock, /guardian:true/);
});

test('protected assessment commands have no direct executable handler', () => {
  assert.match(commandBlock('assessment.commercial.confirm'), /,null,\{risk:/);
  assert.match(commandBlock('assessment.stock.publish'), /,null,\{risk:/);
});

test('registry guard continues to block every non-safe or guardian-gated action', () => {
  assert.match(registry, /cmd\.requiresGuardian\|\|cmd\.risk!==RISK\.SAFE\|\|typeof cmd\.handler!=='function'/);
});
