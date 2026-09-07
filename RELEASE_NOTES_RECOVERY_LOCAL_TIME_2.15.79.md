# Morley Android 2.15.79

- Backup & Data now displays encrypted Google Drive backup timestamps using the device's local time zone and locale instead of raw database UTC values.
- Includes regression coverage for PostgreSQL UTC timestamps and malformed timestamp fallback.
- Backup encryption, authorization, retention and restore behaviour are unchanged.
