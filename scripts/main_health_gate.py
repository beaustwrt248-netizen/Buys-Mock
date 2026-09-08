#!/usr/bin/env python3
"""Read-only post-merge evidence aggregator for the exact main SHA."""

from __future__ import annotations
import argparse, json, os, sys, urllib.request
from dataclasses import dataclass
from pathlib import Path

DEFAULT_REQUIRED = (
    "Repository Security Audit",
    "B&L Morley Quality Gate",
    "Morley Ultimate Parity Gate",
    "Web Release Smoke Checks",
)

PROTECTED_NAMES = ("Nova PR Guard", "Guardian", "protected")

@dataclass
class Verdict:
    name: str
    status: str
    reason: str
    run_id: int | None = None
    run_url: str | None = None
    updated_at: str | None = None

def is_expected_skip(name: str, changed_paths: list[str]) -> bool:
    lower = [p.lower() for p in changed_paths]
    if "ota" in name.lower() or "apk" in name.lower():
        relevant = any(
            p.startswith(("android/", "nova/", ".github/workflows/")) or
            p.endswith((".gradle", ".kt"))
            for p in lower
        )
        return not relevant
    return False

def classify(main_sha: str, evidence: list[dict], required: list[str], changed_paths: list[str] | None = None):
    changed_paths = changed_paths or []
    verdicts: list[Verdict] = []
    by_name: dict[str, list[dict]] = {}
    for item in evidence:
        by_name.setdefault(str(item.get("name", "")), []).append(item)

    for name in required:
        candidates = by_name.get(name, [])
        exact = [x for x in candidates if x.get("head_sha") == main_sha]
        if not exact:
            stale = bool(candidates)
            verdicts.append(Verdict(name, "blocked", "stale-sha evidence" if stale else "missing evidence"))
            continue
        item = sorted(exact, key=lambda x: x.get("updated_at") or x.get("created_at") or "", reverse=True)[0]
        conclusion = str(item.get("conclusion") or item.get("status") or "").lower()
        kwargs = dict(run_id=item.get("id"), run_url=item.get("html_url"), updated_at=item.get("updated_at"))
        if conclusion == "success":
            verdicts.append(Verdict(name, "pass", "exact-sha success", **kwargs))
        elif conclusion == "skipped" and is_expected_skip(name, changed_paths):
            verdicts.append(Verdict(name, "not_applicable", "expected skip for unchanged component", **kwargs))
        elif conclusion in {"cancelled", "timed_out", "action_required"}:
            verdicts.append(Verdict(name, "blocked", f"workflow {conclusion}", **kwargs))
        elif conclusion == "skipped":
            verdicts.append(Verdict(name, "blocked", "unexpected skip", **kwargs))
        else:
            protected = any(token.lower() in name.lower() for token in PROTECTED_NAMES)
            verdicts.append(Verdict(name, "protected_block" if protected else "fail",
                                    "protected approval boundary" if protected else f"workflow {conclusion or 'incomplete'}",
                                    **kwargs))
    ready = all(v.status in {"pass", "not_applicable"} for v in verdicts)
    return ready, verdicts

def github_runs(repo: str, sha: str, token: str) -> list[dict]:
    url = f"https://api.github.com/repos/{repo}/actions/runs?branch=main&per_page=100"
    req = urllib.request.Request(url, headers={
        "Accept": "application/vnd.github+json",
        "Authorization": f"Bearer {token}",
        "X-GitHub-Api-Version": "2022-11-28",
        "User-Agent": "morley-main-health-gate",
    })
    with urllib.request.urlopen(req, timeout=20) as response:
        payload = json.load(response)
    return [r for r in payload.get("workflow_runs", []) if r.get("head_sha") == sha or r.get("head_branch") == "main"]

def parse_args():
    p = argparse.ArgumentParser()
    p.add_argument("--sha", required=True, help="Exact current main commit SHA")
    p.add_argument("--input", type=Path, help="JSON fixture/export of workflow runs")
    p.add_argument("--repo", default=os.getenv("GITHUB_REPOSITORY", "beaustwrt248-netizen/Buys-Mock"))
    p.add_argument("--required", action="append", dest="required")
    p.add_argument("--changed-path", action="append", default=[])
    p.add_argument("--json-out", type=Path)
    return p.parse_args()

def main() -> int:
    args = parse_args()
    required = args.required or list(DEFAULT_REQUIRED)
    if args.input:
        payload = json.loads(args.input.read_text(encoding="utf-8"))
        evidence = payload.get("workflow_runs", payload) if isinstance(payload, dict) else payload
    else:
        token = os.getenv("GITHUB_TOKEN")
        if not token:
            print("GITHUB_TOKEN is required when --input is not supplied", file=sys.stderr)
            return 2
        evidence = github_runs(args.repo, args.sha, token)

    ready, verdicts = classify(args.sha, evidence, required, args.changed_path)
    report = {
        "main_sha": args.sha,
        "ready": ready,
        "checks": [v.__dict__ for v in verdicts],
    }
    print(f"Main health: {'READY' if ready else 'BLOCKED'} @ {args.sha}")
    for v in verdicts:
        print(f"- {v.name}: {v.status} ({v.reason})")
    if args.json_out:
        args.json_out.write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
    return 0 if ready else 1

if __name__ == "__main__":
    raise SystemExit(main())
