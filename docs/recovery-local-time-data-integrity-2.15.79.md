# Data-integrity boundary

Timestamp conversion is performed only on strings copied into the Android `Snapshot` display model. Backend `created_at` values are not rewritten, backup IDs are unchanged, and backup ordering/retention remain controlled by the backend response.
