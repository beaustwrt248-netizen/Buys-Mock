# Test boundary

The regression test injects an explicit `ZoneId` and `Locale`, making the expected conversion deterministic in CI regardless of the GitHub runner's own time zone or locale. Production defaults still use the Android device settings.
