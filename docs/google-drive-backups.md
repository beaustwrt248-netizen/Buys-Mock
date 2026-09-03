# Google Drive backups

Morley backups are generated server-side by the `google-drive-backup` Supabase Edge Function. Google credentials must never be placed in the website, Android APK, Admin APK, repository, browser storage, screenshots, or chat messages.

## Google setup for personal My Drive

Google service accounts do not have personal Drive storage quota, so Morley uses a user OAuth refresh token for the Google account that owns the dedicated `Morley Backups` folder.

The current setup uses an OAuth 2.0 **Web application** client with Google OAuth Playground as the one-time authorization flow. The authorized scope is limited to `https://www.googleapis.com/auth/drive.file`.

Supabase Edge Function secrets required by the live backup function:

- `GOOGLE_DRIVE_OAUTH_CLIENT_ID`
- `GOOGLE_DRIVE_OAUTH_CLIENT_SECRET`
- `GOOGLE_DRIVE_OAUTH_REFRESH_TOKEN`
- `GOOGLE_DRIVE_BACKUP_FOLDER_ID`
- `MORLEY_BACKUP_SECRET` for explicit trusted/manual scheduler calls

The normal `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` runtime secrets are supplied by Supabase.

The live function is user-OAuth only. The previous service-account fallback is deliberately removed from backup execution because the production destination is personal My Drive.

## Security model

- Manual execution requires either the trusted backup secret or an authenticated, enabled Morley `admin` account.
- Production cron execution uses a separate random scheduler secret generated inside Supabase Vault. The plaintext value is not committed or returned to clients.
- Only `service_role` can execute the scheduler-secret comparison RPC.
- OAuth client ID, client secret and refresh token remain Edge Function secrets only.
- The Google OAuth scope remains `drive.file`.
- Uploads are constrained to the configured backup folder ID.
- The function is backup-only. Restore requests are rejected. Restoration must remain a separate, human-approved Admin operation with dry-run validation and audit logging.
- Each backup contains a SHA-256 integrity digest and a format/version marker.
- Audit records store backup metadata, trigger type, retention results and `google_auth_mode`, never credentials or access tokens.

## Automatic schedule

Production uses Supabase `pg_cron` plus `pg_net` and runs the backup daily at **03:00 AWST** (`19:00 UTC`). The scheduler reads its credential from Supabase Vault at execution time and sends it only as the `x-morley-backup-secret` request header.

The HTTP timeout is 120 seconds so a normal full-table export and Drive upload are not cut off by the short pg_net default timeout.

## Retention and cleanup

Every successful backup performs a scoped retention pass in the configured Drive folder.

The policy is **30-day retention with a minimum of 7 backups kept**. Only files in the configured folder whose names match the Morley backup filename pattern are considered. Backups that are older than 30 days and outside the newest seven are moved to Google Drive Trash rather than permanently deleted. A retention failure does not invalidate an otherwise successful new backup; it is surfaced as a warning and the backup remains auditable.

## Legacy service-account cleanup

The service-account authentication path is no longer present in the Edge Function. Any old `GOOGLE_DRIVE_CLIENT_EMAIL` or `GOOGLE_DRIVE_PRIVATE_KEY` project secrets are therefore inert and should be deleted from the Supabase dashboard, and the old service-account writer permission should be removed from the `Morley Backups` folder. The corresponding Google Cloud service-account key should also be revoked/deleted once OAuth backups are confirmed working.

## Restore policy

There is intentionally no automatic restore endpoint. A future restore implementation must:

1. Require an enabled Admin session plus explicit human confirmation.
2. Verify the backup format and SHA-256 digest before parsing data.
3. Produce a dry-run table/row change report before any write.
4. Never restore auth/service credentials from a backup document.
5. Respect current RLS/governance and Guardian boundaries.
6. Write a privileged audit record for approval, start, completion and failure.
