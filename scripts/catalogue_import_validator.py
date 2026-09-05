#!/usr/bin/env python3
"""Validate candidate device-catalogue rows before production ingestion.

This validator is intentionally side-effect free. It rejects listing-specific text from
canonical device names and reports incomplete identity metadata without writing to Supabase.
"""

from __future__ import annotations

import argparse
import json
import re
import sys
from dataclasses import dataclass
from pathlib import Path
from typing import Any, Iterable

CANONICAL_CATEGORIES = {
    "mobile_phone",
    "tablet",
    "laptop",
    "desktop",
    "console",
    "wearable",
}

# Condition, stock-listing and transaction language must never become a canonical model name.
LISTING_TEXT_PATTERNS = (
    re.compile(r"\b(?:battery|batt)\s*(?:health)?\s*[-:]?\s*\d{1,3}\s*%", re.I),
    re.compile(r"(?:^|\s[-–—]\s)\d{1,3}\s*%\s*$", re.I),
    re.compile(r"\bcracked\b|\bbroken\b|\bdamaged\b|\bscratched\b", re.I),
    re.compile(r"\bsold\s+as\s+is\b|\bas\s+is\b|\bno\s+warranty\b", re.I),
    re.compile(r"\bdoesn['’]?t\s+(?:turn|power)\s+on\b|\bnot\s+turning\s+on\b", re.I),
    re.compile(r"\bcan(?:not|'t|’t)\s+update\b|\bunable\s+to\s+update\b", re.I),
    re.compile(r"\b(?:unlocked|locked)\b", re.I),
    re.compile(r"\b(?:excellent|good|fair|poor)\s+condition\b", re.I),
)

STORAGE_TOKEN = re.compile(r"\b\d+(?:\.\d+)?\s*(?:TB|GB)\b", re.I)


@dataclass(frozen=True)
class ValidationIssue:
    code: str
    field: str
    message: str

    def as_dict(self) -> dict[str, str]:
        return {"code": self.code, "field": self.field, "message": self.message}


def _text(value: Any) -> str:
    return str(value or "").strip()


def contains_listing_text(model_name: Any) -> bool:
    text = _text(model_name)
    return bool(text and any(pattern.search(text) for pattern in LISTING_TEXT_PATTERNS))


def validate_row(row: dict[str, Any]) -> list[ValidationIssue]:
    issues: list[ValidationIssue] = []
    category = _text(row.get("category"))
    brand = _text(row.get("brand"))
    model_name = _text(row.get("model_name"))
    model_number = _text(row.get("model_number"))
    source_url = _text(row.get("source_url"))
    storage = row.get("storage_options")

    if category not in CANONICAL_CATEGORIES:
        issues.append(ValidationIssue("invalid_category", "category", f"Unsupported canonical category: {category or '(blank)'}"))
    if not brand:
        issues.append(ValidationIssue("missing_brand", "brand", "Canonical rows require a manufacturer brand."))
    if not model_name:
        issues.append(ValidationIssue("missing_model_name", "model_name", "Canonical rows require a model name."))
    elif contains_listing_text(model_name):
        issues.append(ValidationIssue("listing_text_in_model_name", "model_name", "Condition, stock-listing or transaction text must not be stored in a canonical model name."))

    # A storage token may be part of a legitimate retail title, so do not reject it merely
    # for appearing in model_name. Instead require storage to be represented structurally
    # for non-wearables when the title contains one.
    if category != "wearable" and model_name and STORAGE_TOKEN.search(model_name):
        if not isinstance(storage, list) or not storage:
            issues.append(ValidationIssue("storage_not_structured", "storage_options", "Storage found in the name must also be represented in storage_options."))

    if not model_number:
        issues.append(ValidationIssue("missing_model_number", "model_number", "Model number requires verification before the row is considered canonical-complete."))
    if not source_url:
        issues.append(ValidationIssue("missing_source_url", "source_url", "Canonical identity metadata requires a source URL."))

    return issues


def validate_rows(rows: Iterable[dict[str, Any]]) -> list[dict[str, Any]]:
    results: list[dict[str, Any]] = []
    for index, row in enumerate(rows):
        issues = validate_row(row)
        results.append({
            "index": index,
            "id": row.get("id"),
            "valid": not issues,
            "issues": [issue.as_dict() for issue in issues],
        })
    return results


def _load_rows(path: Path) -> list[dict[str, Any]]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if isinstance(payload, dict):
        payload = payload.get("rows", [payload])
    if not isinstance(payload, list) or any(not isinstance(row, dict) for row in payload):
        raise ValueError("Input must be a JSON object, a JSON array of objects, or {'rows': [...]}.")
    return payload


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description="Validate device_catalog candidates without writing data.")
    parser.add_argument("path", type=Path, help="JSON file containing one row or a list of rows")
    args = parser.parse_args(argv)

    rows = _load_rows(args.path)
    results = validate_rows(rows)
    invalid = sum(not result["valid"] for result in results)
    print(json.dumps({"rows": len(results), "invalid": invalid, "results": results}, indent=2))
    return 1 if invalid else 0


if __name__ == "__main__":
    sys.exit(main())
