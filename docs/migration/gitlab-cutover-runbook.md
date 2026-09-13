# GitLab Primary Cutover Runbook

## Cutover gate

Do not make GitLab primary until every item below is PASS:

- GitLab import verification = PASS
- GitLab CI parity = PASS
- Guardian/security parity = PASS
- Android/Admin artifacts = PASS
- GitHub rollback source verified = PASS

Current status: **NO-GO**.

## Development cutover sequence

1. Confirm the final migration SHA exists on both GitHub and GitLab.
2. Protect GitLab `main` and release tags.
3. Require GitLab CI for merge requests.
4. Configure protected/masked CI/CD variables without copying secret values into git.
5. Make GitLab merge requests the primary development path.
6. Keep GitHub available as the rollback/mirror source.
7. Stop independent feature development on GitHub after cutover to prevent divergent history.

## GitHub mirror strategy

Preferred stable state: one-way GitLab -> GitHub mirroring. GitHub is retained as a safety copy and emergency fallback, not as a second independent development source.

Never force-push GitHub `main` during the migration. Never archive or delete GitHub until the GitLab path has been stable and rollback has been tested.

## Rollback

If any required GitLab pipeline, artifact, Guardian/security gate, or deployment integration is weaker or fails unexpectedly:

1. Stop GitLab-primary merges.
2. Keep production pointed at the existing verified source/deployment configuration.
3. Continue development from the last verified GitHub `main` if necessary.
4. Fix the GitLab migration branch without rewriting GitHub history.
5. Re-run same-revision parity before attempting cutover again.

## Deployment separation

Repository/CI cutover does not automatically authorize production deployment changes. Vercel and Supabase integration changes are a separate gate and must be validated after GitLab-primary development is stable.
