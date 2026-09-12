import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';

const workflow = fs.readFileSync(new URL('../.github/workflows/auto-review-merge.yml', import.meta.url), 'utf8');

test('service-role documentation is exempted narrowly without weakening other critical patterns', () => {
  assert.match(workflow, /service_role_pattern=re\.compile\(r'\\bservice\[_-\]\?role\\b'/);
  assert.match(workflow, /document_path=re\.compile\(r'\(\^\|\/\)\(docs\?\/\|\[\^\/\]\+\\\.md\$\)'/);
  assert.match(workflow, /if service_role_pattern\.search\(file_added\) and not document_path\.search\(name\):/);
  assert.doesNotMatch(workflow, /def safe_security_reference/);
});

test('non-service-role critical patterns remain global across documentation and tests', () => {
  assert.match(workflow, /for pattern in critical_added:/);
  assert.match(workflow, /re\.search\(pattern, added, re\.I\|re\.M\)/);
  assert.match(workflow, /disable\\s\+row\\s\+level\\s\+security/);
  assert.match(workflow, /persist-credentials/);
});

test('classifier keeps added text associated with each file for the service-role exception', () => {
  assert.match(workflow, /added_by_file=\{\}/);
  assert.match(workflow, /for f in files:/);
  assert.match(workflow, /added_by_file\[name\]=/);
  assert.match(workflow, /for name, file_added in added_by_file\.items\(\):/);
});
