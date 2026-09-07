# UI Change Checklist — Recovery Local Time

UI-CHECKLIST: COMPLETE

Affected surfaces:
- Native Android Backup & Data / encrypted Google Drive Recovery Centre.
- Last Backup and Backup History timestamp text.

Explicit N/A areas and why:
- Web/mobile web, pricing, catalogue, inventory, scanner, sales, support, notifications, diagnostics, admin, NFC, Guardian and Nova are unchanged because this is an Android display-only timestamp formatter.
- Encryption, Google Drive authorization, retention, verification, restore semantics, backup payloads, Supabase schema and Edge Functions are unchanged.

Widths/devices checked:
- Native Compose structure is unchanged.
- Friendly localized timestamps are shorter than raw PostgreSQL UTC strings, reducing width pressure on phone and tablet layouts.

Automated gates:
- Deterministic Android unit coverage validates the captured PostgreSQL timestamp format in Australia/Perth with en-AU.
- Malformed timestamp fallback coverage prevents parser errors from crashing the Recovery Centre.
- Protected Android build, lint, security, UI, parity, feature-contract and OTA-policy workflows remain authoritative before merge.

<!-- UI-CHECKLIST-START -->
- [x] Global visual contract reviewed.
- [x] Wording, spelling and capitalisation reviewed.
- [x] Authentication/account surfaces reviewed; unchanged.
- [x] Dashboard/home/navigation/status surfaces reviewed; unchanged.
- [x] Pricing and catalogue surfaces reviewed; unchanged.
- [x] Inventory, scanner, sales and saved history reviewed; unchanged.
- [x] Menu / More / Help reviewed; Recovery Centre entry remains unchanged.
- [x] Notifications, diagnostics and support reviewed; unchanged.
- [x] Updates/release delivery reviewed; version advances to 2.15.79 / 123.
- [x] Android NFC presentation reviewed; unchanged.
- [x] Phone and tablet layout impact reviewed; timestamp strings become shorter.
- [x] Accessibility and interaction states reviewed; controls unchanged and timestamp readability improved.
- [x] Admin surfaces reviewed; unchanged.
- [x] Web-to-Android parity reviewed; backend remains locale-neutral and Android formats locally.
- [x] Valid, blank and malformed timestamp states reviewed.
- [x] Final release evidence will be verified after merge, signed APK publication and OTA promotion.
<!-- UI-CHECKLIST-END -->
