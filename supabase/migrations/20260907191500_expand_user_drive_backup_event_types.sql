alter table public.user_drive_backup_events
  drop constraint if exists user_drive_backup_events_event_type_check;

alter table public.user_drive_backup_events
  add constraint user_drive_backup_events_event_type_check
  check (event_type in (
    'backup_created','backup_failed','backup_verified','backup_deleted',
    'restore_started','restore_completed','restore_failed'
  ));
