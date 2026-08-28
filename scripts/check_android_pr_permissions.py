#!/usr/bin/env python3
from pathlib import Path

workflow = Path('.github/workflows/android-pr-readonly.yml').read_text(encoding='utf-8')
required = [
    'permissions:\n  contents: read',
    'pull_request:',
    'persist-credentials: false',
    'gradle -p android testDebugUnitTest',
    'gradle -p android lintDebug',
    'gradle -p android assembleDebug',
]
missing = [item for item in required if item not in workflow]
for forbidden in ('contents: write', 'BL_KEYSTORE_BASE64', 'softprops/action-gh-release', 'ota/latest.json'):
    if forbidden in workflow:
        raise SystemExit(f'Forbidden privileged release capability in Android PR workflow: {forbidden}')
if missing:
    raise SystemExit('Android PR least-privilege workflow missing required controls: ' + ', '.join(missing))
print('Android PR workflow least-privilege guard passed.')
