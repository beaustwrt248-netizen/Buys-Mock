# Admin team invitation boundaries

- Team invitations are managed only from Admin Control.
- Admin may invite Staff or Manager accounts.
- Manager may invite Staff accounts only, and may only list/reissue/revoke Staff invitations they created.
- Staff cannot create, list, reissue, or revoke team invitations.
- Invitation plaintext codes are generated on-device, displayed once, and only a SHA-256 hash is stored in `app_invites`.
- Invite redemption continues through the existing server-side `redeem-app-invite` flow with Cloudflare Turnstile validation and Supabase Auth account creation.
- Existing account role/enable/disable controls remain Admin-only.
- Invitation lifecycle audit records exclude email addresses, plaintext codes, and code hashes.
- No Valuation 3.0, NFC, repository visibility, or live OTA/versioning behavior is changed by this increment.
