# Google Drive backups

Morley backups are generated server-side by the `google-drive-backup` Supabase Edge Function. Google credentials must never be placed in the website, Android APK, Admin APK, repository, or browser storage.

## Google setup

1. Create/select a Google Cloud project and enable the Google Drive API.
2. Create a service account and a JSON key for it.
3. In the owner's Google Drive, create a dedicated private folder such as `Morley Backups`.
4. Share only that folder with the service account email as Editor. Do not share the whole Drive.
5. Record the folder ID from the folder URL.

## Supabase secrets

Configure these only as Supabase Edge Function secrets:

- `GOOGLE_DRIVE_CLIENT_EMAIL` — service-account email.
- `GOOGLE_DRIVE_PRIVATE_KEY` — service-account PKCS#8 private key; escaped `\n` is accepted.
- `GOOGLE_DRIVE_BACKUP_FOLDER_ID` — dedicated shared backup folder ID.
- `MORLEY_BACKUP_SECRET` — random high-entropy secret used only by a trusted scheduler.

The normal `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` runtime secrets are supplied by Supabase.

## Security model

- Manual execution requires an authenticated, enabled Morley `admin` account.
- Scheduled execution requires `x-morley-backup-secret` to exactly match `MORLEY_BACKUP_SECRET`.
- The Google OAuth scope is `drive.file`, and uploads are constrained to the configured folder.
- The function is backup-only. Restore requests are rejected. Restoration must remain a separate, human-approved Admin operation with dry-run validation and audit logging.
- Each backup contains a SHA-256 integrity digest and a format/version marker.
- Missing optional tables are reported rather than silently represented as successful exports.

## Scheduling

After the function is deployed and secrets are configured, invoke it once per day from a trusted scheduler using POST, `Content-Type: application/json`, the scheduler secret header, and `{ "action": "backup" }`.

Do not put the scheduler secret in GitHub Actions workflow YAML, client JavaScript, Android resources, or committed `.env` files. Use the scheduler's encrypted secret store.

## Restore policy

There is intentionally no automatic restore endpoint. A future restore implementation must:

1. Require an enabled Admin session plus explicit human confirmation.
2. Verify the backup format and SHA-256 digest before parsing data.
3. Produce a dry-run table/row change report before any write.
4. Never restore auth/service credentials from a backup document.
5. Respect current RLS/governance and Guardian boundaries.
6. Write a privileged audit record for approval, start, completion, and failure.
