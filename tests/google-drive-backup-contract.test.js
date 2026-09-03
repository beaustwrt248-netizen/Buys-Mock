const fs = require('fs');
const assert = require('assert');

const src = fs.readFileSync('supabase/functions/google-drive-backup/index.ts','utf8');
const docs = fs.readFileSync('docs/google-drive-backups.md','utf8');

assert.match(src, /GOOGLE_DRIVE_OAUTH_CLIENT_ID/);
assert.match(src, /GOOGLE_DRIVE_OAUTH_CLIENT_SECRET/);
assert.match(src, /GOOGLE_DRIVE_OAUTH_REFRESH_TOKEN/);
assert.match(src, /grant_type: 'refresh_token'/);
assert.match(src, /authMode: 'user-oauth'/);
assert.match(src, /GOOGLE_DRIVE_CLIENT_EMAIL/);
assert.match(src, /GOOGLE_DRIVE_PRIVATE_KEY/);
assert.match(src, /GOOGLE_DRIVE_BACKUP_FOLDER_ID/);
assert.match(src, /MORLEY_BACKUP_SECRET/);
assert.match(src, /https:\/\/www\.googleapis\.com\/auth\/drive\.file/);
assert.match(src, /supportsAllDrives=true/);
assert.match(src, /profile\.role!=='admin'/);
assert.match(src, /Restore is deliberately not available/);
assert.match(src, /sha256/);
assert.match(src, /admin_audit_log/);
assert.doesNotMatch(src, /BEGIN PRIVATE KEY-----[A-Za-z0-9+/]/);
assert.doesNotMatch(src, /AIza[0-9A-Za-z_-]{20,}/);
assert.doesNotMatch(src, /1\/\/[0-9A-Za-z_-]{20,}/);
assert.match(docs, /service accounts do not have personal Drive storage quota/i);
assert.match(docs, /must never be placed in the website, Android APK, Admin APK, repository/);
assert.match(docs, /human-approved Admin operation/);

console.log('Google Drive backup security contract passed');
