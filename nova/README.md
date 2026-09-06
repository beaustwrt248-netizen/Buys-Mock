# Nova AI standalone control centre

This directory is the isolated Nova application surface. It does not inherit Morley Admin UI state or Admin write authority, but it now uses the same Morley Supabase identity store and authorised account credentials for access.

## Authentication boundary

- Nova web and Nova Android use the same Morley Admin email/password identities.
- Sign-in is verified through Supabase Auth with Cloudflare Turnstile.
- Only enabled `admin` or `manager` profiles may enter Nova.
- Passwords and privileged service credentials are never hard-coded or stored by the Nova web page.
- Web access tokens are kept in session storage and revalidated against the authenticated user and profile before the control centre unlocks.
- Nova's own same-origin and GitHub data reads are held until authentication succeeds; Turnstile and authentication-provider infrastructure are not blocked by that gate.
- Sign-out clears the Nova browser session.
- The Android app retains its existing equivalent Supabase + Turnstile + role check.

## Security boundaries

- No production writes are granted by logging into Nova.
- No pricing approval authority is granted.
- No Guardian repair execution is granted.
- No release/deployment authority is granted.
- Protected Supabase data and Edge Functions must continue to require a valid bearer token and appropriate RLS/function authorization.
- Static files shipped by the web host cannot be made confidential by a client-side login gate. Sensitive Nova information must not be published as static assets; it belongs behind authenticated Supabase/Edge endpoints.

Morley Admin remains the operational business administration application. Guardian remains an independent validation layer. Protected actions continue through explicit human approval paths.
