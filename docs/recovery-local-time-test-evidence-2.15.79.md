# Recovery local-time test evidence

Automated source test `DriveRecoveryTimestampSourceContractTest` verifies:
- PostgreSQL/Supabase UTC input no longer displays `+00:00`.
- Australia/Perth conversion produces 21:47 / 9:47 pm for the captured production backup timestamp.
- The year remains 2026.
- Malformed timestamp text is preserved safely without crashing.

The repository's protected PR checks remain the authoritative build, lint, security and parity evidence before merge.
