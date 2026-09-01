# Security and privacy baseline

Last reviewed: 2026-09-01

This document applies to B&L Morley Android, Admin Android, buyshub.me / GitHub Pages, Supabase services, Guardian automation and release delivery.

## Data minimisation

Public clients use only the Supabase project URL and publishable key. Service-role credentials, CAPTCHA secrets, signing keys and notification credentials remain server-side.

The platform stores only data required for account access, device visibility, valuation history, support, administration and release delivery:

- profiles: account email, approved display name, role and enabled state;
- devices: user-owned device and installed-version visibility;
- valuation history: user-owned valuation records;
- support: ticket content, messages, attachments, assignment and audit events;
- Admin telemetry: app version, device model, failing screen, error class and timestamps only;
- invitations: approved email, role, hashed one-time code/token and expiry;
- Guardian: support-linked incident classification, risk, decisions and audit events.

Passwords are handled by Supabase Auth and are never stored in application tables. Invite codes and download tokens are stored as hashes. The invite rate limiter stores only a salted SHA-256 fingerprint, never a raw IP address or email.

## Access boundaries

All public-schema application tables have RLS enabled. User data is owner-scoped. Staff support access is assigned-only. Assignment and privileged support actions are Admin/Manager-only. Internal notes, Admin controls, user controls, invites, remote configuration and Guardian decisions remain privileged and audited.

The Guardian worker cannot deploy code autonomously. Privileged, authentication, payment and security incidents require human approval. Its dispatch token rotates after a successful claim, and a kill switch is available.

## Retention

Automated daily retention removes:

- invite rate-limit fingerprints after 7 days;
- redeemed, expired or revoked invitations after 30 days;
- privacy-minimal Admin error telemetry after 90 days.

Support records, valuation records, device records and audit logs are retained while operationally required and remain subject to owner/role access policies. Account deletion or statutory deletion requests require an Admin-reviewed workflow so linked audit and support records are handled consistently.

## Client and release security

- Android production releases are signed.
- OTA manifests include an exact SHA-256 digest and clients verify downloads.
- Pull requests must pass repository security, Android read-only, Admin integration and parity gates before merge.
- Web authentication uses the same Supabase session and RLS boundaries as Android.
- Cloudflare Turnstile protects invite redemption and Admin authentication.
- Invite redemption is additionally limited to five attempts per email/network fingerprint per fifteen-minute window.

## Incident handling

Do not include passwords, invite codes, access tokens, payment details or government identifiers in support tickets or screenshots. Security incidents should be classified as privileged and escalated to an Admin/Manager. Audit evidence must not be silently rewritten or deleted outside the approved retention process.

## User controls

Users may sign out to end the local session and may request account-data access, correction or deletion through support. A deletion request must verify identity, revoke active sessions first, delete or anonymise user-owned operational data as appropriate, and preserve only records required for security, fraud prevention or legal obligations.

## Review cadence

Run Supabase security advisors after every schema or authorization change, repository security scans on every pull request and schedule, and a manual cross-platform privacy review before each public release.
