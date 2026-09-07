# Backup contract unchanged

The native app continues to send only the existing whitelisted workspace client state to `user-google-drive-backup`. This patch adds no new backup fields and does not serialize credentials, Supabase sessions or Google tokens.
