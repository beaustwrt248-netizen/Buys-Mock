# Display-model boundary

`DriveBackupClient.Snapshot.lastBackupAt` and `BackupRow.createdAt` now contain friendly display strings. The raw backend timestamps are consumed only during snapshot parsing and are not reused as identifiers or request parameters elsewhere in the native backup client.
