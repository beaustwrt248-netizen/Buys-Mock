#!/usr/bin/env python3
import json
import os
import re
import subprocess
import sys
from pathlib import Path

START = '<!-- UI-CHECKLIST-START -->'
END = '<!-- UI-CHECKLIST-END -->'
COMPLETE = 'UI-CHECKLIST: COMPLETE'
MOBILE_START = '<!-- MOBILE-WEB-THEME-CHECKLIST-START -->'
MOBILE_END = '<!-- MOBILE-WEB-THEME-CHECKLIST-END -->'
MOBILE_COMPLETE = 'MOBILE-WEB-THEME-CHECKLIST: COMPLETE'

MOBILE_WEB_EXACT = {
    'index.html',
    'app.css',
    'mobile-more.css',
    'mobile-parity-v3.css',
    'mobile-layout-fix.js',
    'mobile-readability-fix.js',
    'morley-app-parity-v2.css',
    'morley-graphite-web.css',
    'morley-light-web.css',
    'morley-ui-baseline.css',
    'morley-ui-baseline.js',
    'web-apk-home-parity.css',
    'web-apk-home-parity.js',
}
MOBILE_WEB_TOKENS = (
    'mobile', 'theme', 'parity', 'layout', 'readability', 'display', 'dashboard',
    'workspace', 'menu', 'motion', 'a11y', 'ui-', '-ui', 'style', 'home'
)


def fail(message):
    print(f'::error::{message}')
    sys.exit(1)


def section_sample(body, heading):
    pos = body.find(heading)
    if pos < 0:
        return None
    following = body[pos + len(heading):].split('\n', 3)
    return '\n'.join(following[:3]).strip()


def changed_files(pr):
    base = ((pr.get('base') or {}).get('sha') or '').strip()
    head = ((pr.get('head') or {}).get('sha') or '').strip()
    commands = []
    if base and head:
        commands.append(['git', 'diff', '--name-only', base, head])
    commands.append(['git', 'diff', '--name-only', 'HEAD^1', 'HEAD^2'])
    for command in commands:
        try:
            result = subprocess.run(command, check=True, capture_output=True, text=True)
            files = [line.strip() for line in result.stdout.splitlines() if line.strip()]
            if files:
                return files
        except Exception:
            continue
    return None


def is_mobile_web_theme_path(path):
    normal = path.replace('\\', '/').strip()
    if normal in MOBILE_WEB_EXACT:
        return True
    if normal.startswith(('android/', 'admin/', 'supabase/', '.github/', 'scripts/')):
        return False
    name = Path(normal).name.lower()
    if '/' not in normal and name.endswith('.css'):
        return True
    if name.endswith(('.css', '.js', '.html')) and any(token in name for token in MOBILE_WEB_TOKENS):
        return True
    return False


def validate_checkbox_block(body, start, end, label):
    if start not in body or end not in body:
        fail(f'{label} must include its required checklist block from the PR template')
    block = body.split(start, 1)[1].split(end, 1)[0]
    unresolved = re.findall(r'^\s*- \[ \] .+$', block, flags=re.MULTILINE)
    if unresolved:
        print(f'Unresolved {label} checklist categories:')
        for item in unresolved:
            print(f' - {item.strip()}')
        fail(f'Resolve every {label} checklist category before merge; verify it or document it as N/A')


event_path = os.environ.get('GITHUB_EVENT_PATH')
if not event_path:
    fail('GITHUB_EVENT_PATH is unavailable')

payload = json.loads(Path(event_path).read_text(encoding='utf-8'))
pr = payload.get('pull_request') or {}
body = pr.get('body') or ''

validate_checkbox_block(body, START, END, 'UI/theme/layout PR')

if COMPLETE not in body:
    fail(f'UI/theme/layout PR must contain `{COMPLETE}` after the detailed master checklist is reviewed')

for heading in ['Affected surfaces:', 'Explicit N/A areas and why:', 'Widths/devices checked:', 'Automated gates:']:
    sample = section_sample(body, heading)
    if sample is None:
        fail(f'Missing required UI checklist evidence section: {heading}')
    if not sample or sample == '-':
        fail(f'UI checklist evidence is empty for: {heading}')

files = changed_files(pr)
if files is None:
    print('::warning::Could not resolve changed files; applying the stricter mobile-web theme rule as a fail-safe.')
    mobile_theme_change = True
    matched_mobile_files = ['<diff unavailable>']
else:
    matched_mobile_files = [path for path in files if is_mobile_web_theme_path(path)]
    mobile_theme_change = bool(matched_mobile_files)

if mobile_theme_change:
    print('Mobile-web theme-sensitive files changed:')
    for path in matched_mobile_files:
        print(f' - {path}')

    validate_checkbox_block(body, MOBILE_START, MOBILE_END, 'mobile-web theme change')

    if MOBILE_COMPLETE not in body:
        fail(f'Mobile-web theme change must contain `{MOBILE_COMPLETE}` after the full mobile web sweep is complete')

    mobile_headings = [
        'Mobile web affected surfaces:',
        'Mobile web N/A surfaces and why:',
        'Mobile widths checked:',
        'Legacy/duplicate renderer result:',
        'Parity/visual result:',
        'Automated mobile-web gates:',
    ]
    for heading in mobile_headings:
        sample = section_sample(body, heading)
        if sample is None:
            fail(f'Missing required mobile-web theme evidence section: {heading}')
        if not sample or sample == '-':
            fail(f'Mobile-web theme evidence is empty for: {heading}')

print('UI PR checklist gate passed')
