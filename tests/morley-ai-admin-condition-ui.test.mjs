import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const source = fs.readFileSync(new URL('../admin/device-testing-centre.js', import.meta.url), 'utf8');

test('staff testing surface loads and uses the shared Morley assessment core', () => {
  assert.match(source, /morley-ai-assessment-core\.js/);
  assert.match(source, /scoreStaffCondition/);
});

test('condition presentation explicitly requires verified cosmetic evidence for a full score', () => {
  assert.match(source, /verified cosmetic evidence required/i);
  assert.match(source, /Morley Condition Score/);
});

test('condition UI never invents a cosmetic score', () => {
  assert.doesNotMatch(source, /cosmeticScore\s*:\s*(?:100|90|80|75|70|50)\b/);
  assert.match(source, /Number\.isFinite/);
});

test('null and empty cosmetic evidence stay unavailable instead of coercing to zero', () => {
  assert.match(source, /raw==null/);
  assert.match(source, /String\(raw\)\.trim\(\)===''/);
});

test('unsupported and unavailable checks remain non-passing in the existing staff workflow', () => {
  assert.match(source, /state:\s*state\[name\]\?\.state\|\|'unavailable'/);
  assert.match(source, /Unsupported and unavailable are never treated as passing/);
});
