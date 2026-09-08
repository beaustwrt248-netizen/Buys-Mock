#!/usr/bin/env python3
"""Validate guarded Nova OTA metadata without mutating repository or release state."""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path


VERSION_NAME_RE = re.compile(r"[0-9A-Za-z][0-9A-Za-z._-]*")
SHA256_RE = re.compile(r"[0-9a-f]{64}")


def parse_source_identity(gradle_text: str) -> tuple[int, str]:
    code = re.search(r"\bversionCode\s+(\d+)", gradle_text)
    name = re.search(r"""\bversionName\s+['"]([^'"]+)['"]""", gradle_text)
    if not code or not name:
        raise ValueError("Invalid Nova source release identity")

    source_name = name.group(1).strip()
    if not VERSION_NAME_RE.fullmatch(source_name):
        raise ValueError("Nova source versionName contains unsafe release-path characters")
    return int(code.group(1)), source_name


def validate_metadata(
    source_code: int,
    source_name: str,
    metadata: object,
    repository: str,
) -> dict[str, str]:
    if not isinstance(metadata, dict):
        raise ValueError("Nova OTA metadata must be a JSON object")

    published_code = metadata.get("versionCode")
    published_name = metadata.get("versionName")
    if (
        isinstance(published_code, bool)
        or not isinstance(published_code, int)
        or not isinstance(published_name, str)
        or source_code != published_code
        or source_name != published_name.strip()
    ):
        raise ValueError(
            "OTA promotion must match Nova source exactly: "
            f"source {source_name}/{source_code}, "
            f"manifest {published_name}/{published_code}"
        )

    digest = metadata.get("sha256")
    if not isinstance(digest, str) or not SHA256_RE.fullmatch(digest.strip()):
        raise ValueError(
            "Nova OTA sha256 must be exactly 64 lowercase hexadecimal characters"
        )

    if not isinstance(metadata.get("mandatory"), bool):
        raise ValueError("Nova OTA mandatory must be a boolean")

    release_file = f"Nova-AI-{source_name}.apk"
    release_tag = f"nova-v{source_name}"
    expected_url = (
        f"https://github.com/{repository}/releases/download/"
        f"{release_tag}/{release_file}"
    )
    apk_url = metadata.get("apkUrl")
    if not isinstance(apk_url, str) or apk_url.strip() != expected_url:
        raise ValueError(
            f"Nova OTA apkUrl must exactly match verified release URL {expected_url}"
        )

    return {
        "NOVA_RELEASE_TAG": release_tag,
        "NOVA_RELEASE_FILE": release_file,
        "NOVA_EXPECTED_SHA256": digest.strip(),
    }


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--gradle", required=True, type=Path)
    parser.add_argument("--metadata", required=True, type=Path)
    parser.add_argument("--repository", required=True)
    parser.add_argument("--github-env", required=True, type=Path)
    args = parser.parse_args()

    try:
        source_code, source_name = parse_source_identity(
            args.gradle.read_text(encoding="utf-8")
        )
        metadata = json.loads(args.metadata.read_text(encoding="utf-8"))
        values = validate_metadata(
            source_code, source_name, metadata, args.repository
        )
    except (OSError, json.JSONDecodeError, ValueError) as exc:
        raise SystemExit(str(exc)) from exc

    with args.github_env.open("a", encoding="utf-8") as env_file:
        for key, value in values.items():
            env_file.write(f"{key}={value}\n")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
