import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const workflow = fs.readFileSync(new URL('../.github/workflows/auto-review-merge.yml', import.meta.url), 'utf8');

test('guarded review ignores negative or sanitizer-only service-role references but keeps the critical pattern', () => {
  assert.match(workflow, /service\[_-\]\?role/);
  assert.match(workflow, /safe_security_reference/);
  assert.match(workflow, /tests\//);
  assert.match(workflow, /docs\//);
  assert.match(workflow, /assertFalse|doesNotMatch/);
  assert.match(workflow, /blockedMetadataKeys|SECRET_KEY_RE/);
  assert.match(workflow, /if safe_security_reference\(name, line\):\s*continue/);
});

test('guarded review evaluates added lines per file so safe references cannot hide real critical changes', () => {
  assert.match(workflow, /for file in files:/);
  assert.match(workflow, /name=file\.get\('filename',''\)/);
  assert.match(workflow, /for line in \(file\.get\('patch'\) or ''\)\.splitlines\(\):/);
  assert.match(workflow, /critical\.append\(f'diff:\{pattern\}'\)/);
});
