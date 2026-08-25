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
    "index.html", "secure-pricing.js", "app.js", "desktop-oem.js", "desktop-parity.js",
    "web-auth.js", "signed-in-user.js", "web-a11y.js",
    "reference-theme.css", "premium-motion.css", "mobile-more.css",
    "cyber-ui.css", "cyber-spectrum.css", "no-gold.css",
    "web-assets/morley_buys_login_bg_app.mp4",
    "admin/index.html", "admin/app.js", "admin/invites.js", "admin/styles.css", "admin/turnstile.html",
    "android/app/build.gradle", "android/apply_cyber_palette.py",
    "android/app/src/main/AndroidManifest.xml",
    "android/app/src/main/java/com/buysloans/hub/AuthActivity.kt",
    "android/app/src/main/java/com/buysloans/hub/AuthManager.kt",
    "android/app/src/main/java/com/buysloans/hub/MorleyApplication.kt",
    "android/app/src/main/java/com/buysloans/hub/UpdateManager.kt",
    "android/app/src/main/java/com/buysloans/hub/UpdateActivity.kt",
    ".github/workflows/build-apk.yml", ".github/workflows/deploy-admin-pages.yml",
    ".github/workflows/quality-gate.yml", ".github/workflows/web-smoke.yml",
    "morley_buys_login_bg_app.zip",
]
for f in required: require(f)

video = ROOT / "web-assets/morley_buys_login_bg_app.mp4"
if video.exists() and video.stat().st_size < 1_000_000:
    errors.append("Web login background video is unexpectedly small")

retired_gold = ("#ffd400", "#ffe65b", "#ffd000", "#c99a27", "#cda51d", "#ddb347")
for rel in [
    "web-auth.js", "signed-in-user.js", "desktop-parity.js",
    "reference-theme.css", "premium-motion.css", "mobile-more.css",
    "cyber-ui.css", "cyber-spectrum.css", "no-gold.css",
    "admin/styles.css", "admin/index.html", "admin/app.js",
]:
    p = ROOT / rel
    if not p.exists(): continue
    text = p.read_text(encoding="utf-8", errors="replace").lower()
    for token in retired_gold:
        if token in text:
            errors.append(f"Retired gold token {token} still present in active UI layer: {rel}")

secure_pricing = (ROOT / "secure-pricing.js").read_text(encoding="utf-8")
for token in ("morley_web_auth", "Authorization", "Bearer", "window.market=secureMarket"):
    if token not in secure_pricing:
        errors.append(f"Production pricing transport is missing authenticated API control: {token}")

for rel in ("app.js", "desktop-oem.js"):
    text = (ROOT / rel).read_text(encoding="utf-8")
    for token in ("morley_web_auth", "Authorization", "Bearer"):
        if token not in text:
            errors.append(f"Web pricing client {rel} is missing authenticated API control: {token}")

palette = (ROOT / "android/apply_cyber_palette.py").read_text(encoding="utf-8")
for token in ("AuthManager.accessToken", "MorleyApplication.instance", 'setRequestProperty(\"Authorization\",\"Bearer $token\")', "AuthManager.accountLabel(context)"):
    if token not in palette:
        errors.append(f"Android production transform is missing authenticated/account control: {token}")
auth_manager = (ROOT / "android/app/src/main/java/com/buysloans/hub/AuthManager.kt").read_text(encoding="utf-8")
for token in ("DISPLAY_NAME", "fun displayName", "fun accountLabel", "verifyAndCacheProfile", "display_name,email"):
    if token not in auth_manager:
        errors.append(f"Android session manager is missing approved profile-name support: {token}")

auth_activity = (ROOT / "android/app/src/main/java/com/buysloans/hub/AuthActivity.kt").read_text(encoding="utf-8")
for token in ("AuthManager.validAccessToken(this@AuthActivity)", "SessionCheckScreen", "AuthManager.signOut(this@AuthActivity)", "AuthAccent", "AuthPrimary"):
    if token not in auth_activity:
        errors.append(f"Android auth entry point is missing secure startup control: {token}")
for token in ("0xFFFFD400", "0xFFC99A27", "0xFFDDB347", "android.graphics.Color.BLACK"):
    if token in auth_activity:
        errors.append(f"Android auth entry point still contains retired visual token: {token}")

application = (ROOT / "android/app/src/main/java/com/buysloans/hub/MorleyApplication.kt").read_text(encoding="utf-8")
if "lateinit var instance: MorleyApplication" not in application or "instance = this" not in application:
    errors.append("MorleyApplication does not expose the application context required by authenticated pricing")

gradle = (ROOT / "android/app/build.gradle").read_text(encoding="utf-8")
workflow = (ROOT / ".github/workflows/build-apk.yml").read_text(encoding="utf-8")
quality_gate = (ROOT / ".github/workflows/quality-gate.yml").read_text(encoding="utf-8")
vc = re.search(r"versionCode\s+(\d+)", gradle)
vn = re.search(r"versionName\s+'([^']+)'", gradle)
if not vc or not vn:
    errors.append("Could not parse Android versionCode/versionName")
else:
    version_code = int(vc.group(1)); version_name = vn.group(1)
    notes.append(f"Android version: {version_name} ({version_code})")
    if "Resolve Android release version" not in workflow:
        errors.append("Release workflow is missing dynamic Android version resolution")
    if "APP_VERSION_CODE" not in workflow or "APP_VERSION_NAME" not in workflow:
        errors.append("Release workflow is not using resolved Android version metadata")

for token in ("0xFFFFD400", "0xFFC99A27", "0xFFDDB347"):
    if token not in palette:
        errors.append(f"Android palette transformer does not account for retired token {token}")
if "Verify Android cyber palette transform" not in quality_gate:
    errors.append("Quality gate does not verify the Android cyber palette transform")
if "palette-before.sha" not in quality_gate or "palette-after.sha" not in quality_gate:
    errors.append("Quality gate does not verify Android palette transform idempotence")
if "Verify protected backend endpoints" not in quality_gate:
    errors.append("Quality gate does not prove paid backend endpoints reject anonymous requests")

if "sha256" not in workflow.lower() or "hashlib.sha256" not in workflow:
    errors.append("Release workflow is not publishing an APK SHA-256 checksum into OTA metadata")
if "issues: write" in workflow:
    errors.append("Android release workflow requests unnecessary issues:write permission")
if "publish_release:" not in workflow or "inputs.publish_release" not in workflow:
    errors.append("Official Android publishing must require an explicit workflow_dispatch approval")
if re.search(r"Publish GitHub release[\s\S]{0,300}github\.event_name == 'push'", workflow):
    errors.append("Official Android release publishing must not run automatically on a normal push")

pages_workflow = (ROOT / ".github/workflows/deploy-admin-pages.yml").read_text(encoding="utf-8")
for token in ("Build static web bundle", "cp index.html site/", "cp -R admin site/admin", "path: site", "Post-deploy smoke tests"):
    if token not in pages_workflow:
        errors.append(f"GitHub Pages workflow is missing full-site deployment step: {token}")
for token in ("secure-pricing.js", "morley_web_auth", "Authorization", "desktop-oem.js", "secureUserAction('set_role'", "secureUserAction('set_display_name'"):
    if token not in pages_workflow:
        errors.append(f"GitHub Pages post-deploy verification is missing production control: {token}")

turnstile = (ROOT / "admin/turnstile.html").read_text(encoding="utf-8")
if "postMessage(payload,parentOrigin)" not in turnstile:
    errors.append("Turnstile bridge must restrict browser postMessage delivery to the same origin")
if "postMessage(payload,'*')" in turnstile or 'postMessage(payload,"*")' in turnstile:
    errors.append("Turnstile bridge must not broadcast tokens with a wildcard target origin")

admin_app = (ROOT / "admin/app.js").read_text(encoding="utf-8")
for token in ("secureUserAction('set_role'", "secureUserAction('set_display_name'", "data-name-save", "admin-user-control"):
    if token not in admin_app:
        errors.append(f"Admin account management is missing hardened control: {token}")
if "sb.rpc('admin_set_user_role'" in admin_app:
    errors.append("Admin UI still calls the retired role-change SECURITY DEFININER RPC")

admin_index = (ROOT / "admin/index.html").read_text(encoding="utf-8")
admin_invites = (ROOT / "admin/invites.js").read_text(encoding="utf-8")
for token in ('id="inviteName"', 'placeholder="First and last name"', 'app.js?v=4', 'invites.js?v=3'):
    if token not in admin_index:
        errors.append(f"Admin account/invite UI is missing approved full-name control: {token}")
for token in ("inviteName", "display_name:name", "crypto.getRandomValues", "sha256Hex"):
    if token not in admin_invites:
        errors.append(f"Admin invite logic is missing secure full-name handling: {token}")

signed_user = (ROOT / "signed-in-user.js").read_text(encoding="utf-8")
if "display_name" not in signed_user or "morley-web-user" not in signed_user:
    errors.append("Signed-in web identity does not use the approved profile display name")

update_manager = (ROOT / "android/app/src/main/java/com/buysloans/hub/UpdateManager.kt").read_text(encoding="utf-8")
update_activity = (ROOT / "android/app/src/main/java/com/buysloans/hub/UpdateActivity.kt").read_text(encoding="utf-8")
for token in ("isTrustedApkUrl", "isValidSha256", 'putExtra("sha256", update.sha256)'):
    if token not in update_manager:
        errors.append(f"Android OTA manager is missing integrity control: {token}")
for token in ("MessageDigest", "SHA-256", "expectedSha256", "APK integrity check failed"):
    if token not in update_activity:
        errors.append(f"Android OTA installer is missing checksum verification control: {token}")

manifest = (ROOT / "android/app/src/main/AndroidManifest.xml").read_text(encoding="utf-8")
if 'android:usesCleartextTraffic="false"' not in manifest:
    errors.append("Android manifest must disable cleartext network traffic")
if 'android:allowBackup="false"' not in manifest:
    errors.append("Android manifest must disable app-data backup for private session data")

ota_path = ROOT / "ota/latest.json"
if ota_path.exists():
    try:
        ota = json.loads(ota_path.read_text(encoding="utf-8"))
        if vc and int(ota.get("versionCode", 0)) > int(vc.group(1)):
            errors.append("OTA versionCode is ahead of the Android build")
        if not str(ota.get("apkUrl", "")).startswith("https://github.com/beaustwrt248-netizen/Buys-Mock/releases/download/"):
            errors.append("OTA apkUrl is not a trusted B&L Morley GitHub release URL")
        checksum = str(ota.get("sha256", "")).strip().lower()
        if not re.fullmatch(r"[0-9a-f]{64}", checksum):
            errors.append("OTA metadata is missing a valid SHA-256 checksum")
    except Exception as exc:
        errors.append(f"Invalid ota/latest.json: {exc}")

index = (ROOT / "index.html").read_text(encoding="utf-8")
for token in ("reference-theme.css", "premium-motion.css", "no-gold.css", "secure-pricing.js", "web-auth.js", "signed-in-user.js", "web-a11y.js"):
    if token not in index:
        errors.append(f"index.html does not load required layer: {token}")
if "cache:'no-store'" not in index and 'cache:"no-store"' not in index:
    errors.append("Web shell candidate fetch is not configured with no-store caching")

auth = (ROOT / "web-auth.js").read_text(encoding="utf-8")
for token in ("verifyAuthorised", "redeem-app-invite", "captchaToken", "refresh_token"):
    if token not in auth:
        errors.append(f"Web auth is missing required control: {token}")

for forbidden in (".jks", ".keystore", "google-services.json"):
    matches = [p for p in ROOT.rglob(f"*{forbidden}") if ".git" not in p.parts]
    if matches:
        errors.append(f"Sensitive/generated file present in repository: {matches[0].relative_to(ROOT)}")

if errors:
    print("RELEASE AUDIT FAILED")
    for e in errors: print(f"- {e}")
    sys.exit(1)

print("RELEASE AUDIT PASSED")
for n in notes: print(f"- {n}")
