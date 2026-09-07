create table if not exists public.user_drive_backup_master_keys (
  key_id integer primary key check (key_id = 1),
  key_b64 text not null,
  created_at timestamptz not null default now()
);

alter table public.user_drive_backup_master_keys enable row level security;

revoke all on table public.user_drive_backup_master_keys from anon, authenticated;
grant select on table public.user_drive_backup_master_keys to service_role;

insert into public.user_drive_backup_master_keys (key_id, key_b64)
values (1, encode(gen_random_bytes(32), 'base64'))
on conflict (key_id) do nothing;

comment on table public.user_drive_backup_master_keys is
  'Server-only master key material used to derive per-user Drive backup wrapping keys. Never expose to clients.';
