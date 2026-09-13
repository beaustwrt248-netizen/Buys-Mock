# Nova Next Promotion Runbook

Nova Next is developed as a separate successor candidate. Promotion to the primary Nova is never automatic.

## Before promotion

1. Feature parity must be complete for every capability marked required for the release; visual placeholders do not count.
2. Admin authentication, session handling, Turnstile and server-side role checks must pass security review.
3. Guardian must remain independently authoritative and all protected action tests must pass fail-closed.
4. Production data contracts must be compatible; no destructive migration is permitted as part of the UI swap.
5. Web/PWA scope and cache namespaces must be reviewed so the development app cannot poison the current Nova cache.
6. Android production package/application ID must exactly match the current Nova production identity only at the explicit promotion step.
7. Android signing must use the existing Nova production signing key for an in-place upgrade; Nova Next development builds use a separate application ID and may use development signing.
8. Release/version ordering must permit the replacement build to upgrade the installed Nova rather than conflict with it.
9. A rollback point for the current Nova web build and Android release must be recorded.
10. Final high-risk production routing, package/signing, deployment or OTA changes require explicit human approval for the specific ready release/PR.

## Promotion sequence

- Freeze Nova Next candidate SHA/version.
- Run the full parity, security, accessibility and regression suite.
- Verify production backend contract compatibility using read-only checks first.
- Build production web/PWA artifacts with the production Nova scope only after approval.
- Build Android production flavour with the existing Nova package identity and signing configuration only after approval.
- Perform staged smoke testing.
- Switch the Nova web route/deployment to the candidate.
- Release the Android update through the existing approved release path.
- Monitor authentication, Guardian, errors, job execution and cache behavior.
- Keep the previous Nova artifacts available for rollback until the candidate is proven stable.

## Rollback

Web rollback restores the previous Nova deployment artifact/routing. Android rollback follows the existing signed-version recovery path; because Android generally does not permit a lower versionCode to replace a higher one, the rollback build must preserve the same package/signing identity and use an allowed higher recovery versionCode.
