#!/usr/bin/env python3
from __future__ import annotations

import hashlib
import json
import shutil
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "dist" / "private-distribution-readiness"
ADMIN = ROOT / "admin"
OTA = ROOT / "ota" / "latest.json"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as handle:
        for chunk in iter(lambda: handle.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def main() -> None:
    if not ADMIN.is_dir():
        raise SystemExit("admin directory missing")
    if not OTA.is_file():
        raise SystemExit("ota/latest.json missing")

    ota = json.loads(OTA.read_text(encoding="utf-8"))
    required = {"versionCode", "versionName", "apkUrl", "sha256"}
    missing = sorted(required.difference(ota))
    if missing:
        raise SystemExit(f"OTA metadata missing required fields: {', '.join(missing)}")
    if not isinstance(ota["versionCode"], int) or ota["versionCode"] < 1:
        raise SystemExit("OTA versionCode must be a positive integer")
    if len(str(ota["sha256"])) != 64:
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

    files = []
    for path in sorted(OUT.rglob("*")):
        if path.is_file():
            files.append({
                "path": path.relative_to(OUT).as_posix(),
                "sha256": sha256(path),
                "bytes": path.stat().st_size,
            })

    manifest = {
        "schema": 1,
        "purpose": "private-repository migration readiness only; does not alter live OTA or repository visibility",
        "ota": {
            "versionCode": ota["versionCode"],
            "versionName": ota["versionName"],
            "publishedApkUrl": ota["apkUrl"],
            "publishedApkSha256": str(ota["sha256"]).lower(),
        },
        "files": files,
    }
    (OUT / "distribution-manifest.json").write_text(
        json.dumps(manifest, indent=2, sort_keys=True) + "\n", encoding="utf-8"
    )

    checksum_lines = []
    for path in sorted(OUT.rglob("*")):
        if path.is_file() and path.name != "SHA256SUMS":
            checksum_lines.append(f"{sha256(path)}  {path.relative_to(OUT).as_posix()}")
    (OUT / "SHA256SUMS").write_text("\n".join(checksum_lines) + "\n", encoding="utf-8")

    print(f"Prepared portable Admin/OTA readiness bundle with {len(files)} source files")
    print(f"Live OTA remains {ota['versionName']} ({ota['versionCode']}) at its existing published URL")


if __name__ == "__main__":
    main()
