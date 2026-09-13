# GitLab CI Parity Results

## Status

**NO-GO — authenticated GitLab pipeline evidence is still required.**

GitHub remains the production baseline and rollback source. This document must not be changed to PASS unless GitHub Actions and GitLab CI are run against the exact same commit SHA and all required manifest entries are equivalent or stricter on GitLab.

## Required evidence

- Exact compared commit SHA: pending
- GitHub normalized result file: pending
- GitLab normalized result file: pending
- `node scripts/ci/compare-ci-parity.mjs <github.json> <gitlab.json>`: pending

## Required categories

| Category | Result |
| --- | --- |
| Android / Morley Buys APK | PENDING |
| Admin APK / recovery | PENDING |
| OTA / release validation | PENDING |
| Guardian / security | PENDING |
| Governance / feature contracts | PENDING |
| General quality gates | PENDING |

## Rules

- Different commit SHAs are not valid parity evidence.
- Missing required jobs are failures, not skips.
- Missing expected build artifacts are failures.
- GitLab may be stricter than GitHub, but it may not weaken an existing required protection.
- No secrets or privileged logs are copied into this document.

## Conclusion

Cutover remains blocked until all required categories are PASS and the comparison validator exits 0.