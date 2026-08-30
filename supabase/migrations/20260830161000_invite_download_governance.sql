-- Governed app-download invitations for Morley distribution.
-- Admin/Manager may create/revoke; raw invite secrets are never stored.

create table if not exists public.app_download_invites (
  id uuid primary key default gen_random_uuid(),
  email text not null,
  display_name text,
  app_channel text not null default 'morley' check (app_channel in ('morley','admin')),
  role text not null default 'staff' check (role in ('staff','manager','admin')),
  token_hash text not null unique,
  expires_at timestamptz not null,
  created_at timestamptz not null default now(),
  created_by uuid not null default auth.uid(),
  redeemed_at timestamptz,
  redeemed_by uuid,
  revoked_at timestamptz,
  revoked_by uuid
);

alter table public.app_download_invites enable row level security;
revoke all on table public.app_download_invites from anon;
revoke all on table public.app_download_invites from authenticated;
grant select on table public.app_download_invites to authenticated;

create or replace function public.admin_create_download_invite(
  invite_email text,
  invite_display_name text,
  invite_role text,
  invite_app_channel text,
  invite_token_hash text,
  invite_expires_at timestamptz
) returns uuid
language plpgsql
security definer
set search_path = public, pg_temp
as $$
declare new_id uuid;
begin
  if not (public.is_admin() or public.is_manager()) then raise exception 'admin_or_manager_required'; end if;
  if invite_app_channel = 'admin' and not public.is_admin() then raise exception 'admin_required_for_admin_app_invites'; end if;
  if invite_role not in ('staff','manager','admin') then raise exception 'invalid_role'; end if;
  if invite_role in ('manager','admin') and not public.is_admin() then raise exception 'admin_required_for_privileged_invite'; end if;
  if invite_expires_at <= now() or invite_expires_at > now() + interval '14 days' then raise exception 'invalid_expiry'; end if;
  insert into public.app_download_invites(email,display_name,role,app_channel,token_hash,expires_at)
  values(lower(trim(invite_email)),nullif(trim(invite_display_name),''),invite_role,invite_app_channel,invite_token_hash,invite_expires_at)
  returning id into new_id;
  return new_id;
end $$;

create or replace function public.admin_revoke_download_invite(invite_id uuid) returns void
language plpgsql
security definer
set search_path = public, pg_temp
as $$
begin
  if not (public.is_admin() or public.is_manager()) then raise exception 'admin_or_manager_required'; end if;
  update public.app_download_invites set revoked_at=now(), revoked_by=auth.uid()
  where id=invite_id and redeemed_at is null and revoked_at is null;
end $$;

create policy download_invites_admin_manager_read on public.app_download_invites
for select to authenticated using (public.is_admin() or public.is_manager());

revoke all on function public.admin_create_download_invite(text,text,text,text,text,timestamptz) from public, anon;
revoke all on function public.admin_revoke_download_invite(uuid) from public, anon;
grant execute on function public.admin_create_download_invite(text,text,text,text,text,timestamptz) to authenticated;
grant execute on function public.admin_revoke_download_invite(uuid) to authenticated;
