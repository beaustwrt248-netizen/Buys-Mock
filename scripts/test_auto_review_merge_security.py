from pathlib import Path
import re

WORKFLOW = Path('.github/workflows/auto-review-merge.yml')
text = WORKFLOW.read_text(encoding='utf-8')
lines = text.splitlines()

errors = []

# Untrusted PR-controlled filenames may be transported through env, but must never
# be expanded directly into shell source in a privileged pull_request_target job.
direct = '${{ steps.risk.outputs.critical }}'
run_lines = []
in_run = False
run_indent = -1
for line in lines:
    stripped = line.lstrip()
    indent = len(line) - len(stripped)
    if re.fullmatch(r'run:\s*\|\s*', stripped):
        in_run = True
        run_indent = indent
        continue
    if in_run and stripped and indent <= run_indent:
        in_run = False
    if in_run:
        run_lines.append(line)

if direct in '\n'.join(run_lines):
    errors.append('critical step output is interpolated directly into workflow shell source')

# The hardened form must transport the value via env and use it as a quoted shell
# variable written to a body file, so shell metacharacters remain inert data.
if not re.search(r'(?m)^\s*CRITICAL:\s*\$\{\{\s*steps\.risk\.outputs\.critical\s*\}\}\s*$', text):
    errors.append('critical step output is not transported through an environment variable')
if '"$CRITICAL"' not in '\n'.join(run_lines):
    errors.append('critical value is not consumed as a quoted shell variable')
if '--body-file "$RUNNER_TEMP/critical-stop.md"' not in '\n'.join(run_lines):
    errors.append('critical PR comment is not sent through a body file')

if not re.search(r'(?m)^\s*pull-requests:\s*write\s*$', text):
    errors.append('pull-requests permission changed from write')
if not re.search(r'(?m)^\s*contents:\s*read\s*$', text):
    errors.append('contents permission changed from read')
if re.search(r'uses:\s*actions/checkout', text):
    errors.append('privileged pull_request_target workflow must not check out PR code')

if errors:
    raise SystemExit('auto-review workflow security regression:\n- ' + '\n- '.join(errors))

print('auto-review workflow shell-injection regression checks passed')
