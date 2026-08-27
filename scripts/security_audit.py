#!/usr/bin/env python3
from __future__ import annotations

import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SKIP_DIRS = {'.git', '.gradle', 'build', 'node_modules'}
TEXT_SUFFIXES = {'.kt', '.java', '.xml', '.gradle', '.kts', '.yml', '.yaml', '.json', '.js', '.html', '.css', '.sql', '.py', '.md', '.properties', '.txt'}
FORBIDDEN_FILE_SUFFIXES = {'.jks', '.keystore', '.p12', '.pfx', '.pem', '.key'}

# Only high-confidence privileged-secret indicators belong here. Public Supabase anon/publishable
# keys are intentionally not treated as secrets because mobile/web clients necessarily contain them.
PATTERNS = [
    ('Supabase service-role key name', re.compile(r'(?i)SUPABASE_(?:SERVICE_ROLE|SECRET)_KEY\s*[:=]\s*["\']?[A-Za-z0-9._-]{20,}')),
    ('PEM private key', re.compile(r'-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----')),
    ('AWS access key', re.compile(r'\bAKIA[0-9A-Z]{16}\b')),
    ('GitHub token', re.compile(r'\bgh[pousr]_[A-Za-z0-9]{30,}\b')),
]

SAFE_SECRET_REFERENCE = re.compile(r'\$\{\{\s*secrets\.[A-Z0-9_]+\s*\}\}')


def iter_files():
    for path in ROOT.rglob('*'):
        if not path.is_file():
            continue
        if any(part in SKIP_DIRS for part in path.parts):
            continue
        yield path


def main() -> int:
    failures: list[str] = []
    warnings: list[str] = []

    for path in iter_files():
        rel = path.relative_to(ROOT)
        if path.suffix.lower() in FORBIDDEN_FILE_SUFFIXES:
            failures.append(f'Privileged key/container file committed: {rel}')
            continue
        if path.suffix.lower() not in TEXT_SUFFIXES and path.name not in {'.env', '.env.local'}:
            continue
        try:
            text = path.read_text(encoding='utf-8')
        except (UnicodeDecodeError, OSError):
            continue
        for label, pattern in PATTERNS:
            for match in pattern.finditer(text):
                line = text.count('\n', 0, match.start()) + 1
                # GitHub Actions secret references are names, not secret values.
                snippet_start = max(0, match.start() - 80)
                snippet_end = min(len(text), match.end() + 80)
                if SAFE_SECRET_REFERENCE.search(text[snippet_start:snippet_end]):
                    continue
                failures.append(f'{label} indicator: {rel}:{line}')

    workflows = ROOT / '.github' / 'workflows'
    if workflows.is_dir():
        for path in workflows.glob('*.y*ml'):
            text = path.read_text(encoding='utf-8')
            if re.search(r'(?m)^\s*permissions:\s*\n\s*contents:\s*write\s*$', text):
                warnings.append(f'Workflow-wide contents:write should be split/narrowed: {path.relative_to(ROOT)}')
            for m in re.finditer(r'(?m)^\s*-?\s*uses:\s*([^\s#]+)\s*$', text):
                ref = m.group(1)
                if ref.startswith('./'):
                    continue
                if '@' not in ref:
                    warnings.append(f'Action reference lacks immutable ref: {path.relative_to(ROOT)} -> {ref}')
                    continue
                version = ref.rsplit('@', 1)[1]
                if not re.fullmatch(r'[0-9a-fA-F]{40}', version):
                    warnings.append(f'Action is not pinned to full commit SHA: {path.relative_to(ROOT)} -> {ref}')

    if warnings:
        print('SECURITY AUDIT WARNINGS')
        for warning in sorted(set(warnings)):
            print(f'- {warning}')
    if failures:
        print('SECURITY AUDIT FAILED')
        for failure in sorted(set(failures)):
            print(f'- {failure}')
        return 1
    print('SECURITY AUDIT PASSED: no high-confidence privileged credentials or key files detected.')
    return 0


if __name__ == '__main__':
    sys.exit(main())
