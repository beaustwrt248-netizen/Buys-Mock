#!/usr/bin/env python3
from pathlib import Path
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
contract = json.loads((ROOT / 'web-ui-contract.json').read_text(encoding='utf-8'))
foundation = (ROOT / 'morley-web-foundation.css').read_text(encoding='utf-8').lower()
index = (ROOT / 'index.html').read_text(encoding='utf-8').lower()
errors = []


def require(condition, message):
    if not condition:
        errors.append(message)


# Canonical palette must exist in the foundation and remain the only source of theme values.
for name, value in contract['palette'].items():
    require(value.lower() in foundation, f'foundation missing canonical palette token {name}={value}')

# Core responsive/layout contract.
layout = contract['layout']
require(f'@media (max-width:{layout["phoneMaxPx"]}px)' in foundation,
        f'foundation missing canonical phone breakpoint {layout["phoneMaxPx"]}px')
require(f'--morley-content-max:{layout["contentMaxPx"]}px' in foundation,
        'foundation content max-width drifted')
require(f'--morley-touch:{layout["touchTargetMinPx"]}px' in foundation,
        'foundation minimum touch target drifted')
require(f'grid-template-columns:repeat({layout["bottomNavItems"]},minmax(0,1fr))' in foundation,
        'primary navigation item contract drifted')

for component, class_name in contract['components'].items():
    require(f'.{class_name.lower()}' in foundation,
            f'foundation missing canonical component {component}: .{class_name}')

# Selected states must use strong emerald + white, preventing the dark-on-green regressions seen in production.
require('morley-chip[aria-pressed="true"]' in foundation and 'background:var(--morley-accent-strong)' in foundation,
        'selected chip must use canonical strong emerald')
require('morley-chip[aria-pressed="true"]' in foundation and 'color:#fff' in foundation,
        'selected chip must use white label text')
require('button[aria-current="page"]' in foundation and 'background:var(--morley-accent-soft)' in foundation,
        'active primary navigation must use accent-soft background')

# New foundation must not contain any retired electric-blue/cyan tokens.
for token in contract['forbidden']['colors']:
    require(token.lower() not in foundation, f'foundation contains retired color {token}')

# The rebuild is allowed to coexist with legacy files while staged, but they must not be loaded after the foundation
# once cutover mode is enabled. Cutover is detected by index loading morley-web-foundation.css.
if 'morley-web-foundation.css' in index:
    foundation_pos = index.rfind('morley-web-foundation.css')
    for legacy in contract['forbidden']['activeLegacyThemeFiles']:
        pos = index.rfind(legacy.lower())
        require(pos < 0 or pos < foundation_pos,
                f'legacy theme file loads after canonical foundation: {legacy}')

    # Require one canonical header and one canonical primary nav marker in the active shell.
    require(index.count('morley-header') >= 1, 'active shell missing canonical Morley header')
    require(index.count('morley-primary-nav') >= 1, 'active shell missing canonical Morley primary nav')

# Guard against new high-risk theme hacks in newly rebuilt assets.
for path in ROOT.glob('morley-web-*.css'):
    text = path.read_text(encoding='utf-8', errors='ignore').lower()
    if path.name == 'morley-web-foundation.css':
        continue
    for token in contract['forbidden']['colors']:
        require(token.lower() not in text, f'{path.name} reintroduces retired color {token}')

# No CSS transforms/scaling are allowed in the foundation responsive system.
require('transform:scale(' not in foundation, 'foundation must not scale the app to fake responsiveness')
require('zoom:' not in foundation, 'foundation must not use CSS zoom')

if errors:
    print('Morley web UI contract audit FAILED:')
    for error in errors:
        print(f' - {error}')
    sys.exit(1)

print('Morley web UI contract audit passed')
print(f"Contract version: {contract['version']}")
