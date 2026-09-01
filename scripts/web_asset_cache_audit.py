import re
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
html = (ROOT / "index.html").read_text(encoding="utf-8")
refs = re.findall(r'(?:href|src)=\\?"([^"?]+)(?:\?([^"&]+))?', html)

failures = []
seen = set()
for asset, query in refs:
    if asset.startswith(("http://", "https://", "data:")):
        continue
    if not asset.endswith((".js", ".css")):
        continue
    if not (ROOT / asset).is_file():
        failures.append(f"missing referenced asset: {asset}")
    if not re.fullmatch(r"v=\d+", query or ""):
        failures.append(f"missing numeric cache version: {asset}")
    if asset in seen:
        failures.append(f"duplicate asset reference: {asset}")
    seen.add(asset)

if not seen:
    failures.append("no local JavaScript or CSS assets found in index.html")
if failures:
    raise SystemExit("Web asset cache audit failed:\n- " + "\n- ".join(failures))

print(f"Web asset cache audit passed for {len(seen)} versioned assets")
