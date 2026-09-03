const fs = require('fs');
const assert = require('assert');

const src = fs.readFileSync('supabase/functions/google-drive-backup/index.ts','utf8');
const docs = fs.readFileSync('docs/google-drive-backups.md','utf8');
const scheduler = fs.readFileSync('supabase/migrations/20260903192038_add_secure_google_drive_backup_scheduler.sql','utf8');
const timeout = fs.readFileSync('supabase/migrations/20260903192251_increase_google_drive_backup_scheduler_timeout.sql','utf8');

assert.match(src, /GOOGLE_DRIVE_OAUTH_CLIENT_ID/);
assert.match(src, /GOOGLE_DRIVE_OAUTH_CLIENT_SECRET/);
assert.match(src, /GOOGLE_DRIVE_OAUTH_REFRESH_TOKEN/);
assert.match(src, /grant_type: 'refresh_token'/);
assert.match(src, /google_auth_mode: 'user-oauth'/);
assert.doesNotMatch(src, /GOOGLE_DRIVE_CLIENT_EMAIL/);
assert.doesNotMatch(src, /GOOGLE_DRIVE_PRIVATE_KEY/);
assert.doesNotMatch(src, /serviceAccountToken/);
assert.match(src, /GOOGLE_DRIVE_BACKUP_FOLDER_ID/);
assert.match(src, /MORLEY_BACKUP_SECRET/);
assert.match(src, /morley_backup_scheduler_secret_matches/);
assert.match(src, /RETENTION_DAYS = 30/);
assert.match(src, /RETENTION_MIN_KEEP = 7/);
assert.match(src, /trashed: true/);
assert.match(src, /supportsAllDrives=true/);
assert.match(src, /profile\?\.is_enabled/);
assert.match(src, /Administrator access required/);
assert.match(src, /Restore is deliberately not available/);
assert.match(src, /sha256/);
assert.match(src, /admin_audit_log/);
assert.doesNotMatch(src, /BEGIN PRIVATE KEY-----[A-Za-z0-9+/]/);
assert.doesNotMatch(src, /AIza[0-9A-Za-z_-]{20,}/);
assert.doesNotMatch(src, /1\/\/[0-9A-Za-z_-]{20,}/);

assert.match(scheduler, /vault\.create_secret/);
assert.match(scheduler, /morley_backup_scheduler_secret/);
assert.match(scheduler, /grant execute on function public\.morley_backup_scheduler_secret_matches\(text\) to service_role/);
assert.match(scheduler, /morley-google-drive-backup-daily/);
assert.match(scheduler, /0 19 \* \* \*/);
assert.doesNotMatch(scheduler, /1\/\/[0-9A-Za-z_-]{20,}/);
assert.match(timeout, /timeout_milliseconds := 120000/);

assert.match(docs, /service accounts do not have personal Drive storage quota/i);
assert.match(docs, /must never be placed in the website, Android APK, Admin APK, repository/);
assert.match(docs, /30-day retention/i);
assert.match(docs, /daily at 03:00 AWST/i);
assert.match(docs, /human-approved Admin operation/);

console.log('Google Drive backup security contract passed');
