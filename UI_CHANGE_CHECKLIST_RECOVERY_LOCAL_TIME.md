# UI Change Checklist — Recovery Local Time

UI-CHECKLIST: COMPLETE

## Summary
Native Android Backup & Data timestamp presentation only. Raw Supabase/Postgres UTC timestamps are converted to the Android device locale and time zone. Android version is bumped to 2.15.79 / 123 for OTA delivery.

## Affected surfaces
- Native Android Backup & Data / encrypted Google Drive Recovery Centre.
- Last Backup timestamp.
- Backup History timestamps.

## Explicit N/A areas and why
All pricing, catalogue, inventory, scanner, sales, support, notifications, diagnostics, admin, NFC and web/mobile-web surfaces are unchanged. Encryption, Google Drive authorization, retention, verification, restore semantics and backup payloads are unchanged.

## Widths/devices checked
Native Compose layout is unchanged. Friendly timestamps are shorter than the raw database strings, reducing width pressure on phones and tablets.

## Automated gates
Existing Android unit tests, build, lint, security, feature-contract, UI consistency and ultimate parity gates apply to the exact PR head.
