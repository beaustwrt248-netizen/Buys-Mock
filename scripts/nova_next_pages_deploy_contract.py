from pathlib import Path

workflow = Path('.github/workflows/deploy-admin-pages.yml').read_text(encoding='utf-8')

required = {
    "trigger": "- 'nova-next/**'",
    "bundle": "cp -R nova-next site/nova-next",
    "index": "test -s site/nova-next/index.html",
    "runtime": "test -s site/nova-next/app.js",
    "styles": "test -s site/nova-next/styles.css",
    "live_styles": "test -s site/nova-next/live.css",
    "post_deploy": 'fetch_until_contains "$BASE/nova-next/" "$RUNNER_TEMP/nova-next-index.html" "<title>Nova Next</title>"',
}

missing = [name for name, token in required.items() if token not in workflow]
if missing:
    raise SystemExit('Nova Next Pages deployment contract missing: ' + ', '.join(missing))

print('nova-next-pages-deploy-contract: ok')
