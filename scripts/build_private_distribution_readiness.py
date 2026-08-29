#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import re
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "dist" / "private-distribution-readiness"
ADMIN = ROOT / "admin"
OTA = ROOT / "ota" / "latest.json"
ADMIN_APK_WORKFLOW = ROOT / ".github" / "workflows" / "admin-apk-build.yml"

PUBLIC_RESOURCE_PATTERNS = (
    ("github_release", re.compile(r"https://github\.com/[^\s\"']+/releases/download/[^\s\"']+")),
    ("github_raw", re.compile(r"https://raw\.githubusercontent\.com/[^\s\"']+")),
    ("github_pages", re.compile(r"https://[^\s\"']+\.github\.io(?:/[^\s\"']*)?")),
)
PUBLIC_SCAN_FILES = (
    OTA,
    ADMIN / "release-control.js",
    ADMIN_APK_WORKFLOW,
)


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def public_resource_dependencies() -> list[dict[str, object]]:
    dependencies: list[dict[str, object]] = []
    seen: set[tuple[str, str, str]] = set()
    for path in PUBLIC_SCAN_FILES:
        if not path.is_file():
            continue
        text = path.read_text(encoding="utf-8")
        rel = path.relative_to(ROOT).as_posix()
        for resource_type, pattern in PUBLIC_RESOURCE_PATTERNS:
            for match in pattern.finditer(text):
                url = match.group(0).rstrip(",);]}")
                key = (rel, resource_type, url)
                if key in seen:
                    continue
                seen.add(key)
                dependencies.append({
                    "source": rel,
                    "type": resource_type,
                    "url": url,
                    "blockingPrivateRepositoryCutover": True,
                    "replacementRequirement": "authenticated HTTPS distribution origin with checksum verification",
                })
    return sorted(dependencies, key=lambda item: (str(item["source"]), str(item["url"])))


def write_json(path: Path, payload: object) -> None:
    path.write_text(json.dumps(payload, indent=2, sort_keys=True) + "\n", encoding="utf-8")


def main() -> None:
    if not ADMIN.is_dir():
        raise SystemExit("admin directory missing")
    if not OTA.is_file():
        raise SystemExit("ota/latest.json missing")
    if not ADMIN_APK_WORKFLOW.is_file():
        raise SystemExit("Admin APK workflow missing")

    ota = json.loads(OTA.read_text(encoding="utf-8"))
    required = {"versionCode", "versionName", "apkUrl", "sha256"}
    missing = sorted(required.difference(ota))
    if missing:
        raise SystemExit(f"OTA metadata missing required fields: {', '.join(missing)}")
    if not isinstance(ota["versionCode"], int) or ota["versionCode"] < 1:
        raise SystemExit("OTA versionCode must be a positive integer")
    if not re.fullmatch(r"[0-9a-fA-F]{64}", str(ota["sha256"])):
        raise SystemExit("OTA sha256 must be 64 hex characters")

    if OUT.exists():
        shutil.rmtree(OUT)
    (OUT / "admin").mkdir(parents=True)
    (OUT / "ota").mkdir(parents=True)

    for path in ADMIN.rglob("*"):
        if path.is_file():
            target = OUT / "admin" / path.relative_to(ADMIN)
            target.parent.mkdir(parents=True, exist_ok=True)
            shutil.copy2(path, target)
    shutil.copy2(OTA, OUT / "ota" / "latest.json")

    public_dependencies = public_resource_dependencies()
    write_json(
        OUT / "public-resource-dependencies.json",
        {
            "schema": 1,
            "purpose": "inventory only; live distribution remains unchanged",
            "repositoryVisibilityChanged": False,
            "liveOtaChanged": False,
            "migrationReady": len(public_dependencies) == 0,
            "blockingDependencyCount": len(public_dependencies),
            "dependencies": public_dependencies,
        },
    )
    write_json(
        OUT / "private-origin-contract.json",
        {
            "schema": 1,
            "status": "prepared-not-enabled",
            "requirements": {
                "transport": "https-only",
                "authentication": "required for Admin/OTA distribution origin",
                "metadataIntegrity": "sha256 required; signed metadata preferred before cutover",
                "apkIntegrity": "published APK sha256 must be verified before install/distribution",
                "secretsInClient": False,
                "serviceRoleInClient": False,
            },
            "cutoverOrder": [
                "admin_bundle",
                "ota_metadata",
                "apk_binary",
                "repository_visibility",
            ],
        },
    )

    files = []
    for path in sorted(OUT.rglob("*")):
        if path.is_file():
            files.append({
                "path": path.relative_to(OUT).as_posix(),
                "sha256": sha256(path),
                "bytes": path.stat().st_size,
            })

    manifest = {
        "schema": 2,
        "purpose": "private-repository migration readiness only; does not alter live OTA or repository visibility",
        "migrationReady": len(public_dependencies) == 0,
        "blockingPublicResourceCount": len(public_dependencies),
        "ota": {
            "versionCode": ota["versionCode"],
            "versionName": ota["versionName"],
            "publishedApkUrl": ota["apkUrl"],
            "publishedApkSha256": str(ota["sha256"]).lower(),
        },
        "files": files,
    }
    write_json(OUT / "distribution-manifest.json", manifest)

    checksum_lines = []
    for path in sorted(OUT.rglob("*")):
        if path.is_file() and path.name != "SHA256SUMS":
            checksum_lines.append(f"{sha256(path)}  {path.relative_to(OUT).as_posix()}")
    (OUT / "SHA256SUMS").write_text("\n".join(checksum_lines) + "\n", encoding="utf-8")

    print(f"Prepared portable Admin/OTA readiness bundle with {len(files)} source files")
    print(f"Recorded {len(public_dependencies)} public distribution dependencies that must be replaced before private-repository cutover")
    print(f"Live OTA remains {ota['versionName']} ({ota['versionCode']}) at its existing published URL")


if __name__ == "__main__":
    main()
