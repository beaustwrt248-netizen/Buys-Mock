# Nova Next Live Adapter Validation

This document records evidence for the merged Nova Next Admin-auth and safe-intelligence implementation. It does not authorize production promotion, package replacement, signing, deployment or OTA publication.

## Security boundary

- Current production Nova remains authoritative until an explicitly approved promotion changes that boundary.
- Password authentication requires Cloudflare Turnstile and the existing Supabase Admin identity contract.
- A session is accepted only after user revalidation plus enabled `admin` profile validation.
- Browser storage is session scoped and excludes passwords.
- Generic feature transport uses an explicit function allowlist.
- Browser access excludes internal Guardian intelligence/workers, Guardian repair status mutation, pricing control and user control.
- Mixed-action endpoints are wrapped with narrower client APIs so write/review/harvest/draft-PR actions are absent from Nova Next.
- Code proposals are restricted to non-protected `nova-next/` paths and are proposal-only.

## Contract evidence

The safe feature modules have been exercised with the repository's Nova Next contract suite, including:

```text
code-proposal-adapter: ok
feature-runtime: ok
read-adapters: ok
safe-client-functions: ok
safe-services: ok
vision-adapter: ok
```

The authentication/chat contracts include:

```text
auth-controller: ok
chat-adapter: ok
edge-function-client: ok
runtime-config: ok
session-store: ok
supabase-auth-client: ok
turnstile: ok
```

## Current protected boundary

The merged implementation connects Nova Next to the reviewed Admin authentication and safe live-adapter contracts. The remaining high-risk boundary is production promotion: replacing the current Nova route or package, using production signing identity, publishing a Nova Next OTA release, or changing production deployment authority. Those actions remain subject to the explicit, release-specific approval and rollback requirements in `docs/nova-next/PROMOTION_RUNBOOK.md`.
