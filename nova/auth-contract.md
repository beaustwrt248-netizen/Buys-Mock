# Nova shared authentication contract

Risk: **High / protected authentication boundary**.

Nova web and Nova Android must use the same Morley Supabase Auth identity store. A successful password exchange alone is insufficient: the account must also have an existing `profiles` row with `is_enabled = true` and `role` equal to `admin` or `manager`. Cloudflare Turnstile is required on interactive password sign-in.

The public/publishable Supabase key may be present in client code; no service-role key, password, or privileged credential may be shipped to the browser or APK. Protected database rows and Edge Function operations must remain guarded by the authenticated bearer token and server-side RLS/authorization.

The web control centre must stay visually locked and defer its normal network reads until the shared Morley account has been validated. Sign-out must clear the Nova browser session. Static hosting cannot make shipped static files confidential, so sensitive Nova data must not be emitted as static JSON/assets.
