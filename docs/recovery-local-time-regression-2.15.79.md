# Recovery local-time regression boundary

The formatter is intentionally isolated in `DriveBackupClient.localTimestamp`. Snapshot parsing applies it only to `last_backup.created_at` and each backup history `created_at`. No timestamp is modified before being sent to the backend and no backup identity or ordering field is changed.
