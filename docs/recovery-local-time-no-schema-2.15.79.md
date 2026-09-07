# Schema impact

None. No migration is required. Existing `created_at` values remain offset-aware backend timestamps; the Android app converts them only when constructing user-facing snapshot text.
