from pathlib import Path
import json
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
errors = []
notes = []

def require(path: str):
    p = ROOT / path
    if not p.exists() or p.stat().st_size == 0:
        errors.append(f"Missing or empty required file: {path}")
    return p

required = [
    "index.html",
    "web-auth.js",
    "signed-in-user.js",
    "reference-theme.css",
    "premium-motion.css",
    "mobile-more.css",
    "cyber-ui.css",
    "cyber-spectrum.css",
    "no-gold.css",
    "android/app/build.gradle",
    "android/apply_cyber_palette.py",
    ".github/workflows/build-apk.yml",
    "morley_buys_login_bg_app.zip",
]
for f in required:
    require(f)

# Keep the active web palette free of the retired yellow/gold branding values.
for rel in [
    "web-auth.js",
    "signed-in-user.js",
    "reference-theme.css",
    "premium-motion.css",
    "mobile-more.css",
    "cyber-ui.css",
    "cyber-spectrum.css",
    "no-gold.css",
]:
    p = ROOT / rel
    if not p.exists():
        continue
    text = p.read_text(encoding="utf-8", errors="replace").lower()
    for token in ("#ffd400", "#ffe65b", "#ffd000", "#c99a27", "#cda51d"):
        if token in text:
            errors.append(f"Retired gold token {token} still present in active web layer: {rel}")

# Version consistency between Android source and release workflow.
gradle = (ROOT / "android/app/build.gradle").read_text(encoding="utf-8")
workflow = (ROOT / ".github/workflows/build-apk.yml").read_text(encoding="utf-8")
vc = re.search(r"versionCode\s+(\d+)", gradle)
vn = re.search(r"versionName\s+'([^']+)'", gradle)
if not vc or not vn:
    errors.append("Could not parse Android versionCode/versionName")
else:
    version_code = int(vc.group(1))
    version_name = vn.group(1)
    notes.append(f"Android version: {version_name} ({version_code})")
    if "Resolve Android release version" not in workflow:
        errors.append("Release workflow is missing dynamic Android version resolution")
    if "APP_VERSION_CODE" not in workflow or "APP_VERSION_NAME" not in workflow:
        errors.append("Release workflow is not using resolved Android version metadata")

# OTA may intentionally lag until a successful release, but it must never point ahead.
ota_path = ROOT / "ota/latest.json"
if ota_path.exists():
    try:
        ota = json.loads(ota_path.read_text(encoding="utf-8"))
        if vc and int(ota.get("versionCode", 0)) > int(vc.group(1)):
            errors.append("OTA versionCode is ahead of the Android build")
        if not str(ota.get("apkUrl", "")).startswith("https://github.com/beaustwrt248-netizen/Buys-Mock/releases/download/"):
            errors.append("OTA apkUrl is not a trusted B&L Morley GitHub release URL")
    except Exception as exc:
        errors.append(f"Invalid ota/latest.json: {exc}")

# Ensure the shell actually loads the current auth and palette layers.
index = (ROOT / "index.html").read_text(encoding="utf-8")
for token in ("reference-theme.css", "premium-motion.css", "no-gold.css", "web-auth.js", "signed-in-user.js"):
    if token not in index:
        errors.append(f"index.html does not load required layer: {token}")
if "cache:'no-store'" not in index and 'cache:"no-store"' not in index:
    errors.append("Web shell candidate fetch is not configured with no-store caching")

# Auth must keep the production controls that prevent a public/open signup path.
auth = (ROOT / "web-auth.js").read_text(encoding="utf-8")
for token in ("verifyAuthorised", "redeem-app-invite", "captchaToken", "refresh_token"):
    if token not in auth:
        errors.append(f"Web auth is missing required control: {token}")

# Avoid accidentally shipping private signing material/config files.
for forbidden in (".jks", ".keystore", "google-services.json"):
    matches = [p for p in ROOT.rglob(f"*{forbidden}") if ".git" not in p.parts]
    if matches:
        errors.append(f"Sensitive/generated file present in repository: {matches[0].relative_to(ROOT)}")

if errors:
    print("RELEASE AUDIT FAILED")
    for e in errors:
        print(f"- {e}")
    sys.exit(1)

print("RELEASE AUDIT PASSED")
for n in notes:
    print(f"- {n}")
