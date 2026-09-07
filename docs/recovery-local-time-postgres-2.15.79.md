# PostgreSQL timestamp compatibility

Supabase may return PostgreSQL timestamps with a space between date and time, e.g. `2026-09-07 13:47:28.023974+00:00`. The Android formatter normalizes only that separator to ISO `T`, then parses the original offset-aware instant with `OffsetDateTime`.
