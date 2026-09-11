#!/usr/bin/env python3
"""Regression contract for Morley guarded auto-review/merge boundaries."""
from pathlib import Path
import re
import sys

workflow = Path(sys.argv[1] if len(sys.argv) > 1 else ".github/workflows/auto-review-merge.yml")
text = workflow.read_text(encoding="utf-8")

assert "if: steps.risk.outputs.level == 'routine'" in text, (
    "automatic approval/merge must be restricted to routine changes"
)
assert "if: steps.risk.outputs.level != 'critical'" not in text, (
    "guarded changes must never share automatic approval/merge conditions"
)
assert re.search(r"\^\\\.github/workflows/", text), "workflow changes must remain guarded"
assert "^ota/" in text, "OTA/release metadata must remain guarded"
assert "pricing[-_]?approval" in text, "pricing approval controls must remain guarded"
assert "privileged[-_]?roles?" in text, "privileged-role controls must remain guarded"
assert "Manual approval required" in text, "guarded changes must state the approval boundary"

print("auto-review protected-boundary contract: ok")
