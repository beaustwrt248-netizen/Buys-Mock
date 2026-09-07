# Performance note

Timestamp conversion is local CPU-only work over the small backup-history response (retention is capped at 30 versions). It introduces no extra network request, Drive operation, database query or background worker.
