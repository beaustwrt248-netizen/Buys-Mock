from pathlib import Path
import re

WORKFLOW = Path('.github/workflows/auto-review-merge.yml')
text = WORKFLOW.read_text(encoding='utf-8')

errors = []

# Untrusted PR-controlled filenames flow through this step output. It must never be
# expanded directly into shell source in a privileged pull_request_target job.
direct = '${{ steps.risk.outputs.critical }}'
if direct in text:
    errors.append('critical step output is interpolated directly into workflow shell source')

# The hardened form must transport the value via env and quote the shell variable.
if not re.search(r'(?m)^\s*CRITICAL:\s*\$\{\{\s*steps\.risk\.outputs\.critical\s*\}\}\s*$', text):
    errors.append('critical step output is not transported through an environment variable')

if not re.search(r'(?m)^\s*pull-requests:\s*write\s*$', text):
    errors.append('pull-requests permission changed from write')
if not re.search(r'(?m)^\s*contents:\s*read\s*$', text):
    errors.append('contents permission changed from read')
if re.search(r'uses:\s*actions/checkout', text):
    errors.append('privileged pull_request_target workflow must not check out PR code')

if errors:
    raise SystemExit('auto-review workflow security regression:\n- ' + '\n- '.join(errors))

print('auto-review workflow shell-injection regression checks passed')
