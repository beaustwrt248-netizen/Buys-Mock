# Google Drive backups

Morley backups are generated server-side by the `google-drive-backup` Supabase Edge Function. Google credentials must never be placed in the website, Android APK, Admin APK, repository, or browser storage.

## Recommended Google setup for a personal My Drive folder

Google service accounts do not have personal Drive storage quota. For a normal `My Drive` backup folder, use a one-time user OAuth grant so uploaded files consume the owner's Google Drive quota.

1. Create/select the Google Cloud project and enable the Google Drive API.
2. Configure an OAuth consent screen for the account that owns the backup folder.
3. Create an OAuth 2.0 Client ID for a Desktop app.
4. Authorize that client once with the narrow `https://www.googleapis.com/auth/drive.file` scope and obtain a refresh token.
5. Store the OAuth client ID, client secret and refresh token only as Supabase Edge Function secrets.
6. Keep `GOOGLE_DRIVE_BACKUP_FOLDER_ID` pointed at the dedicated `Morley Backups` folder.

The existing service-account configuration may remain present as a fallback for Google Workspace Shared Drive deployments, but it is not suitable for writing backups into a normal personal My Drive because service accounts have no storage quota there.

## Supabase secrets

Configure these only as Supabase Edge Function secrets:

- `GOOGLE_DRIVE_OAUTH_CLIENT_ID` — OAuth 2.0 client ID.
- `GOOGLE_DRIVE_OAUTH_CLIENT_SECRET` — OAuth 2.0 client secret.
- `GOOGLE_DRIVE_OAUTH_REFRESH_TOKEN` — long-lived refresh token produced by the owner's one-time Drive authorization.
- `GOOGLE_DRIVE_BACKUP_FOLDER_ID` — dedicated backup folder ID.
- `MORLEY_BACKUP_SECRET` — random high-entropy secret used only by a trusted scheduler.

Optional Shared Drive fallback secrets:

- `GOOGLE_DRIVE_CLIENT_EMAIL` — service-account email.
- `GOOGLE_DRIVE_PRIVATE_KEY` — service-account PKCS#8 private key; escaped `\n` is accepted.

The normal `SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY` runtime secrets are supplied by Supabase.

## Security model

- Manual execution requires an authenticated, enabled Morley `admin` account.
- Scheduled execution requires `x-morley-backup-secret` to exactly match `MORLEY_BACKUP_SECRET`.
- User OAuth is preferred when all three user-OAuth secrets are present; service-account auth is used only as a fallback.
- The Google OAuth scope is `drive.file`, and uploads are constrained to the configured folder ID.
- The function is backup-only. Restore requests are rejected. Restoration must remain a separate, human-approved Admin operation with dry-run validation and audit logging.
- Each backup contains a SHA-256 integrity digest and a format/version marker.
- The audit record notes which Google authentication mode was used, without storing tokens or secrets.

## Scheduling

After the function is deployed and secrets are configured, invoke it once per day from a trusted scheduler using POST, `Content-Type: application/json`, the scheduler secret header, and `{ "action": "backup" }`.

Do not put the scheduler secret, OAuth client secret, refresh token, service-account private key, or access token in GitHub Actions workflow YAML, client JavaScript, Android resources, screenshots, chat messages, or committed `.env` files. Use encrypted backend secret stores only.

## Restore policy

There is intentionally no automatic restore endpoint. A future restore implementation must:

1. Require an enabled Admin session plus explicit human confirmation.
2. Verify the backup format and SHA-256 digest before parsing data.
3. Produce a dry-run table/row change report before any write.
4. Never restore auth/service credentials from a backup document.
5. Respect current RLS/governance and Guardian boundaries.
6. Write a privileged audit record for approval, start, completion, and failure.
