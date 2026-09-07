# Nova AI standalone control centre

This directory is the isolated Nova application surface. It uses the Morley Supabase identity store, but Nova itself is an owner/Admin-only control centre rather than a staff application.

## Product architecture

Nova is the single assistant and operator surface. Guardian is not a competing assistant: it is Nova's independent security, governance and enforcement layer.

The operating path is `Human Admin -> Nova AI -> Guardian enforcement -> Morley systems`.

Nova may inspect evidence, research, diagnose, recommend, queue allowed work and explain why Guardian blocked or gated something. Guardian remains independently authoritative for protected operations. Nova must not be able to disable, bypass, weaken, self-approve, inherit or silently expand Guardian authority.

Internal `guardian_*` database objects, repair records and compatibility APIs remain intentionally named Guardian so the enforcement boundary is explicit and existing audit history is preserved.

## Authentication boundary

- Nova web and Nova Android use the Morley Supabase Auth identity store.
- Sign-in is verified through Supabase Auth with Cloudflare Turnstile.
- Only an enabled `admin` profile may enter Nova. `staff` and `manager` profiles are not Nova roles and must be rejected even if they are valid Morley accounts.
- The production ownership model is one human Admin; Nova does not receive its own staff or manager identity.
- Passwords and privileged service credentials are never hard-coded or stored by the Nova web page.
- Web access tokens are kept in session storage and revalidated against the authenticated user and profile before the control centre unlocks.
- Nova's own same-origin and GitHub data reads are held until authentication succeeds; Turnstile and authentication-provider infrastructure are not blocked by that gate.
- Sign-out clears the Nova browser session.

## Security and execution boundaries

- Logging into Nova does not grant arbitrary database or repository authority.
- Nova-specific Edge Functions must verify the bearer token and require an enabled `admin` profile server-side.
- Direct client access to Nova's protected RLS tables remains deny-by-default; service-role access stays inside authenticated Edge Functions.
- `nova-actions` is the controlled-write boundary. Its allow-list may create auditable catalogue audit queue items and evidence-backed findings that still require approval.
- Pricing approval/write authority is not granted to Nova.
- Guardian incident/repair approval or execution authority is not granted to Nova.
- Guardian is fail-closed for protected actions: unavailable or incomplete enforcement evidence must never be interpreted as permission.
- Release, deployment, OTA, signing, role/user changes, destructive deletes and automatic catalogue patch application remain prohibited or human-gated.
- `nova-vision` is Admin-only, uses a server-side AI credential, does not expose the provider key, does not persist submitted photos at the application layer, masks label identifiers and cannot change the catalogue.
- Sensitive Nova information must stay behind authenticated Supabase/Edge endpoints rather than static assets.

Morley Admin remains the operational business administration application for staff. Nova is the unified AI/operator experience; Guardian remains the independent enforcement layer inside that experience. Protected actions continue through explicit human approval paths.
