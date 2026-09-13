# GitLab CI Parity Results

## Status

**NO-GO — GitLab read-only CI is green, but normalized GitHub-vs-GitLab same-revision parity is still pending.**

GitHub remains the production baseline and rollback source. This document must not be changed to final PASS unless GitHub Actions and GitLab CI are compared against the exact same commit SHA and all required manifest entries are equivalent or stricter on GitLab.

## Authenticated GitLab evidence

The first fully executing hosted-runner pipeline is now available:

- Exact GitLab-tested commit SHA: `f02a746440467dc406cf28cfc6c5e76e05f39f5c`
- GitLab pipeline: `2844956840`
- GitHub migration bridge run: `34775843567`
- GitLab normalized evidence artifact: `gitlab-pipeline-evidence-f02a746440467dc406cf28cfc6c5e76e05f39f5c`
- Pipeline status: `success`
- `admin:android-check`: success on `saas-linux-medium-amd64`
- `test:nova-next-ci`: success on `saas-linux-medium-amd64`
- `test:ultimate-parity`: success on `saas-linux-medium-amd64`
- Required validate/security/governance/quality jobs represented in that pipeline completed successfully.
- Provider-specific publishing/deployment jobs remained manual and were not authorized or executed.

This proves the GitLab read-only CI path and hosted Android toolchain can execute. It does **not** substitute for the final normalized GitHub-vs-GitLab comparison.

## Remaining required evidence

- Exact comparison SHA for the final side-by-side run: pending
- GitHub normalized result file for that SHA: pending
- GitLab normalized result file for that same SHA: pending
- `node scripts/ci/compare-ci-parity.mjs <github.json> <gitlab.json>` exit 0: pending

The migration branch push currently triggers only the dedicated GitLab Migration Bridge on GitHub, so a deliberate same-revision GitHub baseline run still has to be staged without enabling release/deployment writes.

## Required categories

| Category | Current evidence |
| --- | --- |
| Android / Morley Buys APK | GitLab execution proven; normalized cross-provider parity pending |
| Admin APK / recovery | GitLab Admin validation proven; protected signed/manual paths remain gated; cross-provider parity pending |
| OTA / release validation | Read-only GitLab contract passed; write jobs remained manual; cross-provider parity pending |
| Guardian / security | GitLab required security jobs passed; cross-provider normalized comparison pending |
| Governance / feature contracts | GitLab required governance jobs passed; cross-provider normalized comparison pending |
| General quality gates | GitLab quality/web/parity jobs passed; cross-provider normalized comparison pending |

## Rules

- Different commit SHAs are not valid parity evidence.
- Missing required jobs are failures, not skips.
- Missing expected build artifacts are failures.
- GitLab may be stricter than GitHub, but it may not weaken an existing required protection.
- Manual provider write jobs are not treated as read-only parity failures while the staged migration intentionally keeps them disabled.
- No secrets or privileged logs are copied into this document.

## Conclusion

GitLab hosted-runner execution is no longer a blocker. Cutover remains blocked until the final exact-SHA GitHub/GitLab normalized comparison passes, protected GitLab variables/settings are verified, rollback readiness is confirmed, and the final migration gate is approved.
