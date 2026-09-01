#!/usr/bin/env python3
import json
import os
import re
import sys
from pathlib import Path

START = '<!-- UI-CHECKLIST-START -->'
END = '<!-- UI-CHECKLIST-END -->'
COMPLETE = 'UI-CHECKLIST: COMPLETE'


def fail(message):
    print(f'::error::{message}')
    sys.exit(1)


event_path = os.environ.get('GITHUB_EVENT_PATH')
if not event_path:
    fail('GITHUB_EVENT_PATH is unavailable')

payload = json.loads(Path(event_path).read_text(encoding='utf-8'))
pr = payload.get('pull_request') or {}
body = pr.get('body') or ''

if START not in body or END not in body:
    fail('UI/theme/layout PR must include the master UI checklist block from the PR template')

block = body.split(START, 1)[1].split(END, 1)[0]
unresolved = re.findall(r'^\s*- \[ \] .+$', block, flags=re.MULTILINE)
if unresolved:
    print('Unresolved UI checklist categories:')
    for item in unresolved:
        print(f' - {item.strip()}')
    fail('Resolve every UI checklist category before merge; verify it or document it as N/A')

if COMPLETE not in body:
    fail(f'UI/theme/layout PR must contain `{COMPLETE}` after the detailed master checklist is reviewed')

# Evidence must not be left as an empty template.
for heading in ['Affected surfaces:', 'Explicit N/A areas and why:', 'Widths/devices checked:', 'Automated gates:']:
    pos = body.find(heading)
    if pos < 0:
        fail(f'Missing required UI checklist evidence section: {heading}')
    following = body[pos + len(heading):].split('\n', 3)
    sample = '\n'.join(following[:3]).strip()
    if not sample or sample == '-':
        fail(f'UI checklist evidence is empty for: {heading}')

print('UI PR checklist gate passed')
