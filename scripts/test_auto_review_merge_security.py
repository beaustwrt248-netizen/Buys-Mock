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

run_text = '\n'.join(run_lines)
if direct in run_text:
    errors.append('critical step output is interpolated directly into workflow shell source')

# The hardened form must transport the value via env and use it as a quoted shell
# variable written to a body file, so shell metacharacters remain inert data.
if not re.search(r'(?m)^\s*CRITICAL:\s*\$\{\{\s*steps\.risk\.outputs\.critical\s*\}\}\s*$', text):
    errors.append('critical step output is not transported through an environment variable')
if '"$CRITICAL"' not in run_text:
    errors.append('critical value is not consumed as a quoted shell variable')
if '--body-file "$RUNNER_TEMP/critical-stop.md"' not in run_text:
    errors.append('critical PR comment is not sent through a body file')

# Documentation may accurately describe existing service-role-only server behavior.
# That wording alone must not promote an otherwise guarded change to critical. Runtime
# source still needs broad service-role detection, so the workflow must make this
# exception by file type rather than by weakening/removing the service-role matcher.
if "service_role_pattern=re.compile(r'\\bservice[_-]?role\\b'" not in run_text:
    errors.append('service-role critical detection is not preserved as a dedicated runtime-source matcher')
if "document_path=re.compile(r'(^|/)(docs?/|[^/]+\\.md$)'" not in run_text:
    errors.append('documentation paths are not explicitly separated from runtime service-role detection')
if "if service_role_pattern.search(file_added) and not document_path.search(name):" not in run_text:
    errors.append('documentation-only service-role wording can still trigger a false critical classification')
if "r'\\bservice[_-]?role\\b'," in run_text:
    errors.append('service-role matcher is still applied globally across documentation and runtime patches')

# Keep the workflow least-privileged. The repository's Actions token cannot invoke
# mergePullRequest with these permissions, so auto-merge arming must be best-effort
# rather than turning a successful routine review into a failed workflow incident.
if not re.search(r'(?m)^\s*pull-requests:\s*write\s*$', text):
    errors.append('pull-requests permission changed from write')
if not re.search(r'(?m)^\s*contents:\s*read\s*$', text):
    errors.append('contents permission changed from read')
if re.search(r'(?m)^\s*contents:\s*write\s*$', text):
    errors.append('workflow must not expand contents permission to write just to arm auto-merge')
if re.search(r'uses:\s*actions/checkout', text):
    errors.append('privileged pull_request_target workflow must not check out PR code')

auto_merge_start = None
for index, line in enumerate(lines):
    if line.strip() == '- name: Enable auto-merge for routine changes only':
        auto_merge_start = index
        break
if auto_merge_start is None:
    errors.append('auto-merge arming step is missing')
else:
    auto_merge_step = []
    for line in lines[auto_merge_start + 1:]:
        if line.startswith('      - name:'):
            break
        auto_merge_step.append(line)
    if not any(line.strip() == 'continue-on-error: true' for line in auto_merge_step):
        errors.append('auto-merge arming must be best-effort when the least-privilege token cannot merge')

if errors:
    raise SystemExit('auto-review workflow security regression:\n- ' + '\n- '.join(errors))

print('auto-review workflow shell-injection, risk-classification and least-privilege merge checks passed')
