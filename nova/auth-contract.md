# Nova shared authentication contract

Risk: **High / protected authentication boundary**.

Nova uses the Morley Supabase Auth identity store, but Nova access is **Admin-only**. A successful password exchange alone is insufficient: the account must have an existing `profiles` row with `is_enabled = true` and `role = 'admin'`. `staff` and `manager` profiles must be rejected by both the Nova client gate and every Nova-specific privileged Edge Function. Cloudflare Turnstile is required on interactive password sign-in.

The production ownership model is one human Admin. Nova does not receive a Staff or Manager role and does not create a second privileged identity for itself.

The public/publishable Supabase key may be present in client code; no service-role key, AI-provider key, password, or other privileged credential may be shipped to the browser or APK. Protected database rows and Edge Function operations must remain guarded by the authenticated bearer token plus server-side role authorization.

Nova-specific protected tables remain deny-by-default to direct authenticated clients. Controlled writes must go through an Admin-only, allow-listed server-side action boundary with an audit record. Pricing approval/write, Guardian approval/repair execution, release/deployment/OTA/signing, role/user changes, destructive deletes and silent catalogue patch application remain outside Nova authority.

The web control centre must stay visually locked and defer its own same-origin and GitHub data reads until the Admin account has been validated. The network gate must not intercept Cloudflare Turnstile or authentication-provider infrastructure needed to complete sign-in. Sign-out must clear the Nova browser session. Static hosting cannot make shipped static files confidential, so sensitive Nova data must not be emitted as static JSON/assets.
