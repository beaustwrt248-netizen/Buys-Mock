#!/usr/bin/env python3
"""Classify device catalogue rows for safe cleanup review.

This tool is intentionally read-only. It consumes JSON exported from device_catalog,
uses the canonical import validator, and emits machine-readable review classifications.
It never connects to Supabase and never mutates production data.
"""

from __future__ import annotations

import argparse
import json
import re
from pathlib import Path
from typing import Any

import catalogue_import_validator as validator

RETAILER_ONLY_SOURCES = ("cash converters",)
SUSPICIOUS_MODEL_NUMBER_PATTERNS = (
    re.compile(r"(?:\s+-\s+|\s+Patt\b|\s+MIL\b|\s+CHROME\b|\s+Bx\d+\b)", re.I),
    re.compile(r"\b(?:INC\s+BATTERY|WITH\s+ATTACHED\s+COVER)\b", re.I),
    re.compile(r"\b\d{1,3}\s*%\s*$", re.I),
)


def _text(value: Any) -> str:
    return str(value or "").strip()


def classify_row(row: dict[str, Any]) -> dict[str, Any]:
    issues = validator.validate_row(row)
    codes = {issue.code for issue in issues}
    source_name = _text(row.get("source_name")).lower()
    model_number = _text(row.get("model_number"))

    retailer_only = any(token in source_name for token in RETAILER_ONLY_SOURCES)
    suspicious_model_number = bool(
        model_number and any(pattern.search(model_number) for pattern in SUSPICIOUS_MODEL_NUMBER_PATTERNS)
    )

    reasons: list[str] = []
    if "listing_text_in_model_name" in codes:
        reasons.append("listing_text_in_model_name")
    if suspicious_model_number:
        reasons.append("suspicious_model_number")
    if "missing_model_number" in codes:
        reasons.append("missing_model_number")
    if retailer_only:
        reasons.append("retailer_only_identity_evidence")
    for code in sorted(codes - {"listing_text_in_model_name", "missing_model_number"}):
        reasons.append(code)

    if "listing_text_in_model_name" in codes or suspicious_model_number:
        classification = "high_confidence_repair_candidate"
        action = "verify manufacturer/Australian-market identity, then repair or deactivate only with approved production change"
    elif "missing_model_number" in codes:
        classification = "metadata_verification_required"
        action = "verify model number from manufacturer/Australian carrier evidence; never guess"
    elif retailer_only:
        classification = "retailer_only_evidence"
        action = "retain for now; add independent canonical evidence before treating identity as verified"
    elif issues:
        classification = "validation_issue"
        action = "resolve validator issues before import or canonical promotion"
    else:
        classification = "canonical_clean"
        action = "no cleanup action indicated"

    return {
        "id": row.get("id"),
        "category": row.get("category"),
        "brand": row.get("brand"),
        "model_name": row.get("model_name"),
        "model_number": row.get("model_number"),
        "source_name": row.get("source_name"),
        "classification": classification,
        "reasons": reasons,
        "recommended_action": action,
        "validator_issues": [issue.as_dict() for issue in issues],
    }


def _load_rows(path: Path) -> list[dict[str, Any]]:
    payload = json.loads(path.read_text(encoding="utf-8"))
    if isinstance(payload, dict):
        payload = payload.get("rows", [payload])
    if not isinstance(payload, list) or any(not isinstance(row, dict) for row in payload):
        raise ValueError("Input must be a JSON object, JSON array of objects, or {'rows': [...]}.")
    return payload


def main() -> int:
    parser = argparse.ArgumentParser(description="Classify device_catalog rows without writing production data.")
    parser.add_argument("path", type=Path, help="JSON export of device_catalog rows")
    args = parser.parse_args()

    results = [classify_row(row) for row in _load_rows(args.path)]
    summary: dict[str, int] = {}
    for result in results:
        key = result["classification"]
        summary[key] = summary.get(key, 0) + 1

    print(json.dumps({"rows": len(results), "summary": summary, "results": results}, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
