-- Reconcile server-side privileges required by the encrypted per-user Drive backup service.
-- These grants are intentionally idempotent and mirror the production permissions repaired on 2026-09-07.

-- The backup Edge Function must be able to read the server-only master key,
-- but it must never be able to mutate that key material.
revoke insert, update, delete on table public.user_drive_backup_master_keys from service_role;
grant select on table public.user_drive_backup_master_keys to service_role;

-- Backup metadata, wrapped DEKs, and audit events are maintained exclusively by the
-- authenticated server-side backup workflow.
grant select, insert, update, delete on table public.user_drive_backups to service_role;
grant select, insert, update, delete on table public.user_drive_backup_keys to service_role;
grant select, insert, update, delete on table public.user_drive_backup_events to service_role;

-- Conservative restore can replace the current user's valuation history after the
-- restore authorization, owner, Google-account, integrity, and confirmation checks pass.
grant select, insert, update, delete on table public.valuation_history to service_role;
