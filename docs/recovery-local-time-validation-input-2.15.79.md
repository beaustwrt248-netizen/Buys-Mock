# Validation input

Captured production-style value: `2026-09-07 13:47:28.023974+00:00`.

The regression test uses this exact offset-aware PostgreSQL timestamp to ensure the Android parser covers the format seen in the Recovery Centre rather than only idealized ISO strings.
