#!/usr/bin/env python3
"""Validate a GitHub release response for the exact guarded Nova APK asset."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


SHA256_RE = re.compile(r"[0-9a-f]{64}")


def validate_release_asset(
    release: object,
    asset_name: str,
    expected_sha256: str,
) -> int:
    if not isinstance(release, dict) or not isinstance(release.get("assets"), list):
        raise ValueError("GitHub release response must contain an assets array")
    if not SHA256_RE.fullmatch(expected_sha256):
        raise ValueError("Expected Nova APK SHA-256 is invalid")

    matches = [
        asset
        for asset in release["assets"]
        if isinstance(asset, dict) and asset.get("name") == asset_name
    ]
    if len(matches) != 1:
        raise ValueError(
            f"Expected exactly one published Nova APK asset named {asset_name}; "
            f"found {len(matches)}"
        )

    asset = matches[0]
    if asset.get("digest") != f"sha256:{expected_sha256}":
        raise ValueError("Published Nova APK asset digest does not match OTA metadata")

    size = asset.get("size")
    if isinstance(size, bool) or not isinstance(size, int) or size <= 0:
        raise ValueError("Published Nova APK asset must have a positive byte size")
    return size


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--release-json", required=True, type=Path)
    parser.add_argument("--asset-name", required=True)
    parser.add_argument("--expected-sha256", required=True)
    args = parser.parse_args()

    try:
        release = json.loads(args.release_json.read_text(encoding="utf-8"))
        size = validate_release_asset(
            release, args.asset_name, args.expected_sha256
        )
    except (OSError, json.JSONDecodeError, ValueError) as exc:
        raise SystemExit(str(exc)) from exc

    print(
        "Nova OTA metadata matches the published release asset, "
        f"SHA-256, and size ({size} bytes)."
    )
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
