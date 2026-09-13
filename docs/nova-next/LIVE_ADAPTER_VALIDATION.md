# Nova Next Live Adapter Validation

This document records evidence for the isolated Nova Next Admin-auth and safe-intelligence phase. It does not authorize merge or deployment.

## Security boundary

- Current `nova/`, `android/novaapp/`, `supabase/` and production deployment files remain unchanged by this phase.
- Password authentication requires Cloudflare Turnstile and the existing Supabase Admin identity contract.
- A session is accepted only after user revalidation plus enabled `admin` profile validation.
- Browser storage is session scoped and excludes passwords.
- Generic feature transport uses an explicit function allowlist.
- Browser access excludes internal Guardian intelligence/workers, Guardian repair status mutation, pricing control and user control.
- Mixed-action endpoints are wrapped with narrower client APIs so write/review/harvest/draft-PR actions are absent from Nova Next.
- Code proposals are restricted to non-protected `nova-next/` paths and are proposal-only.

## Fresh local contract evidence

The safe feature modules were reconstructed from the branch source and executed with Node 22. The following tests exited successfully:

```text
code-proposal-adapter: ok
feature-runtime: ok
read-adapters: ok
safe-client-functions: ok
safe-services: ok
vision-adapter: ok
```

The earlier live authentication/chat suite also completed successfully:

```text
auth-controller: ok
chat-adapter: ok
edge-function-client: ok
runtime-config: ok
session-store: ok
supabase-auth-client: ok
turnstile: ok
```

## Merge boundary

PR #1784 remains high-risk because it connects the new UI to production authentication. Even if automated checks are green, it must stay unmerged until the exact ready PR/head is explicitly approved in conversation.
