#!/usr/bin/env python3
"""Regression contract for Nova's exact-main workflow health presentation."""

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SOURCE = (ROOT / "nova" / "app-core.js").read_text(encoding="utf-8")


def check(condition: bool, message: str) -> None:
    if not condition:
        raise AssertionError(message)


# Nova must inspect the exact current-main commit so applicability is based on
# the same changed-path concept used by scripts/main_health_gate.py.
check("changedPaths:[]" in SOURCE, "Nova must retain exact-main changed paths")
check("api(`/commits/${main.commit.sha}`)" in SOURCE, "Nova must fetch exact-main commit detail")
check("state.changedPaths=(mainCommit.files||[]).map(file=>file.filename).filter(Boolean)" in SOURCE,
      "Nova must derive changed paths from exact-main files")

# Keep the client classifier aligned with main_health_gate.is_expected_skip().
for token in (
    "name.includes('ota')",
    "name.includes('apk')",
    "path.startsWith('android/')",
    "path.startsWith('nova/')",
    "path.startsWith('.github/workflows/')",
    "path.endsWith('.gradle')",
    "path.endsWith('.kt')",
):
    check(token in SOURCE, f"Nova expected-skip classifier is missing {token}")

check("isExpectedSkip(run)?'NOT APPLICABLE':'SKIPPED'" in SOURCE,
      "Expected skips must be labelled NOT APPLICABLE")
check("r.conclusion!=='success'&&!isExpectedSkip(r)" in SOURCE,
      "Unexpected skips/failures must remain in the blocking set")
check("r.conclusion==='success'||isExpectedSkip(r)" in SOURCE,
      "Expected skips must count as healthy/non-applicable monitoring evidence")
check("skippedRuns" not in SOURCE,
      "Nova must not have a generic skipped-runs branch that forces INCOMPLETE")
check("Nova is waiting on complete evidence" not in SOURCE,
      "Expected/non-applicable skips must not force the old INCOMPLETE summary")

print("Nova workflow health contract tests passed")
