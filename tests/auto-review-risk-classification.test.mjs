import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

const repoRoot = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const workflowPath = path.join(repoRoot, '.github', 'workflows', 'auto-review-merge.yml');

function extractClassifier() {
  const source = fs.readFileSync(workflowPath, 'utf8');
  const match = source.match(/python3 - <<'PY'\n([\s\S]*?)\n\s*PY\n/);
  assert.ok(match, 'auto-review workflow must contain the Python risk classifier');

  const lines = match[1].split('\n');
  const nonEmpty = lines.filter((line) => line.trim());
  const indent = Math.min(...nonEmpty.map((line) => line.match(/^\s*/)[0].length));
  return lines.map((line) => line.slice(Math.min(indent, line.length))).join('\n');
}

function classify(files) {
  const temp = fs.mkdtempSync(path.join(os.tmpdir(), 'morley-risk-'));
  const output = path.join(temp, 'github-output.txt');
  fs.writeFileSync(path.join(temp, 'pr-files.json'), JSON.stringify(files), 'utf8');
  fs.writeFileSync(output, '', 'utf8');

  const run = spawnSync('python3', ['-c', extractClassifier()], {
    cwd: repoRoot,
    env: { ...process.env, RUNNER_TEMP: temp, GITHUB_OUTPUT: output },
    encoding: 'utf8',
  });

  assert.equal(run.status, 0, `classifier failed:\n${run.stderr || run.stdout}`);
  const values = Object.fromEntries(
    fs.readFileSync(output, 'utf8')
      .split('\n')
      .filter((line) => line.includes('=') && !line.includes('<<'))
      .map((line) => {
        const index = line.indexOf('=');
        return [line.slice(0, index), line.slice(index + 1)];
      }),
  );
  return values.level;
}

function patchForAddedLine(line) {
  return `@@ -0,0 +1 @@\n+${line}`;
}

test('safety documentation mentioning force-push is not treated as a destructive action', () => {
  const level = classify([
    {
      filename: 'docs/repository-path-stability.md',
      patch: patchForAddedLine('Do not force-push or rewrite main during reconciliation.'),
    },
  ]);
  assert.equal(level, 'routine');
});

test('an executable force-push remains a critical autonomy stop', () => {
  const level = classify([
    {
      filename: 'scripts/reconcile.sh',
      patch: patchForAddedLine('git push --force origin main'),
    },
  ]);
  assert.equal(level, 'critical');
});
