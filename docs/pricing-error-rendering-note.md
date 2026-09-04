# Pricing error hardening

This change hardens the protected Admin Pricing path after the live Edge Function began returning opaque errors in the Admin UI.

- Backend PostgREST/object errors are converted to actionable strings with stage context.
- Catalogue and pricing queries are constrained and bounded.
- Ignored write/history/audit errors are now surfaced.
- The Admin Pricing UI no longer renders JavaScript objects as `[object Object]`.
- Authoritative pricing approval and role checks remain unchanged.
