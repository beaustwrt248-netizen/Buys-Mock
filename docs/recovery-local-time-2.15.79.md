# Recovery local-time verification

The native Android Recovery Centre parses offset-aware Supabase/Postgres backup timestamps, converts the instant to `ZoneId.systemDefault()`, and renders with the Android device locale using medium date / short time formatting. Invalid values fall back to their original text rather than crashing the recovery screen.

Regression input: `2026-09-07 13:47:28.023974+00:00`.
Expected Australia/Perth time: 21:47 on 7 Sep 2026, with exact punctuation determined by the Android locale formatter.
