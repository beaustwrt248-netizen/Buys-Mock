create index if not exists user_drive_backup_events_backup_id_idx
  on public.user_drive_backup_events (backup_id);

drop policy if exists "Users can read only their own Drive backups" on public.user_drive_backups;
create policy "Users can read only their own Drive backups"
on public.user_drive_backups
for select
to authenticated
using (user_id = (select auth.uid()));

drop policy if exists "Users can read only their own Drive backup events" on public.user_drive_backup_events;
create policy "Users can read only their own Drive backup events"
on public.user_drive_backup_events
for select
to authenticated
using (user_id = (select auth.uid()));
