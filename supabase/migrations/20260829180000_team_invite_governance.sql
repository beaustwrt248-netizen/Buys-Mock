-- Admin Control team/staff invitation governance.
-- Direct authenticated mutations are removed: invitation writes pass through
-- narrow SECURITY DEFINER RPCs that enforce role boundaries and audit changes.

alter table public.app_invites
  drop constraint if exists app_invites_role_check;

alter table public.app_invites
  add constraint app_invites_role_check
  check (role in ('staff','manager'));

drop policy if exists admins_manage_invites on public.app_invites;
drop policy if exists admin_select_team_invites on public.app_invites;
drop policy if exists manager_select_own_staff_invites on public.app_invites;
drop policy if exists admin_insert_team_invites on public.app_invites;
drop policy if exists manager_insert_own_staff_invites on public.app_invites;
drop policy if exists admin_update_team_invites on public.app_invites;
drop policy if exists manager_update_own_staff_invites on public.app_invites;
drop policy if exists admin_delete_team_invites on public.app_invites;
drop policy if exists manager_delete_own_staff_invites on public.app_invites;

create policy admin_select_team_invites
on public.app_invites
for select
to authenticated
using (
  exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid())
      and p.role = 'admin'
      and p.is_enabled = true
  )
);

create policy manager_select_own_staff_invites
on public.app_invites
for select
to authenticated
using (
  role = 'staff'
  and created_by = (select auth.uid())
  and exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid())
      and p.role = 'manager'
      and p.is_enabled = true
  )
);

revoke insert, update, delete on public.app_invites from authenticated;
grant select on public.app_invites to authenticated;

create or replace function public.admin_create_team_invite(
  invite_email text,
  invite_display_name text,
  invite_role text,
  invite_code_hash text,
  invite_expires_at timestamptz
)
returns table(id uuid, email text, display_name text, role text, expires_at timestamptz, used_at timestamptz, created_at timestamptz)
language plpgsql
security definer
set search_path = ''
as $$
declare
  caller_role text;
  clean_email text := lower(btrim(coalesce(invite_email,'')));
  clean_name text := regexp_replace(btrim(coalesce(invite_display_name,'')), '\s+', ' ', 'g');
begin
  select p.role into caller_role
  from public.profiles p
  where p.id = auth.uid() and p.is_enabled = true;

  if caller_role not in ('admin','manager') then raise exception 'admin or manager required'; end if;
  if caller_role = 'manager' and invite_role <> 'staff' then raise exception 'managers may invite staff only'; end if;
  if caller_role = 'admin' and invite_role not in ('staff','manager') then raise exception 'unsupported invite role'; end if;
  if length(clean_name) not between 3 and 100 or position(' ' in clean_name) = 0 then raise exception 'first and last name required'; end if;
  if length(clean_email) not between 3 and 254 or clean_email !~ '^[^@[:space:]]+@[^@[:space:]]+\.[^@[:space:]]+$' then raise exception 'valid email required'; end if;
  if invite_code_hash !~ '^[0-9a-f]{64}$' then raise exception 'invalid invite code hash'; end if;
  if invite_expires_at <= now() or invite_expires_at > now() + interval '8 days' then raise exception 'invalid invite expiry'; end if;
  if exists (
    select 1 from public.app_invites i
    where lower(i.email) = clean_email and i.used_at is null and i.expires_at > now()
  ) then raise exception 'an active invitation already exists for this email'; end if;

  return query
  with inserted as (
    insert into public.app_invites(email,display_name,role,code_hash,expires_at,created_by)
    values(clean_email,clean_name,invite_role,invite_code_hash,invite_expires_at,auth.uid())
    returning app_invites.id,app_invites.email,app_invites.display_name,app_invites.role,app_invites.expires_at,app_invites.used_at,app_invites.created_at
  ), audited as (
    insert into public.admin_audit_log(actor_user_id,action,target_type,target_id,details)
    select auth.uid(),'team_invite_created','team_invite',inserted.id::text,jsonb_build_object('role',inserted.role)
    from inserted
    returning 1
  )
  select inserted.id,inserted.email,inserted.display_name,inserted.role,inserted.expires_at,inserted.used_at,inserted.created_at
  from inserted;
end
$$;

create or replace function public.admin_reissue_team_invite(
  invite_id uuid,
  invite_code_hash text,
  invite_expires_at timestamptz
)
returns table(id uuid, email text, display_name text, role text, expires_at timestamptz, used_at timestamptz, created_at timestamptz)
language plpgsql
security definer
set search_path = ''
as $$
declare
  caller_role text;
  target public.app_invites%rowtype;
begin
  select p.role into caller_role
  from public.profiles p
  where p.id = auth.uid() and p.is_enabled = true;
  if caller_role not in ('admin','manager') then raise exception 'admin or manager required'; end if;
  if invite_code_hash !~ '^[0-9a-f]{64}$' then raise exception 'invalid invite code hash'; end if;
  if invite_expires_at <= now() or invite_expires_at > now() + interval '8 days' then raise exception 'invalid invite expiry'; end if;

  select i.* into target from public.app_invites i where i.id = invite_id for update;
  if target.id is null or target.used_at is not null then raise exception 'active invitation not found'; end if;
  if caller_role = 'manager' and (target.role <> 'staff' or target.created_by is distinct from auth.uid()) then
    raise exception 'managers may manage only their own staff invitations';
  end if;

  update public.app_invites i
  set code_hash = invite_code_hash, expires_at = invite_expires_at
  where i.id = invite_id;

  insert into public.admin_audit_log(actor_user_id,action,target_type,target_id,details)
  values(auth.uid(),'team_invite_reissued','team_invite',invite_id::text,jsonb_build_object('role',target.role));

  return query
  select i.id,i.email,i.display_name,i.role,i.expires_at,i.used_at,i.created_at
  from public.app_invites i where i.id = invite_id;
end
$$;

create or replace function public.admin_revoke_team_invite(invite_id uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  caller_role text;
  target public.app_invites%rowtype;
begin
  select p.role into caller_role
  from public.profiles p
  where p.id = auth.uid() and p.is_enabled = true;
  if caller_role not in ('admin','manager') then raise exception 'admin or manager required'; end if;

  select i.* into target from public.app_invites i where i.id = invite_id for update;
  if target.id is null or target.used_at is not null then raise exception 'active invitation not found'; end if;
  if caller_role = 'manager' and (target.role <> 'staff' or target.created_by is distinct from auth.uid()) then
    raise exception 'managers may manage only their own staff invitations';
  end if;

  delete from public.app_invites i where i.id = invite_id;
  insert into public.admin_audit_log(actor_user_id,action,target_type,target_id,details)
  values(auth.uid(),'team_invite_revoked','team_invite',invite_id::text,jsonb_build_object('role',target.role));
end
$$;

revoke all on function public.admin_create_team_invite(text,text,text,text,timestamptz) from public, anon;
revoke all on function public.admin_reissue_team_invite(uuid,text,timestamptz) from public, anon;
revoke all on function public.admin_revoke_team_invite(uuid) from public, anon;
grant execute on function public.admin_create_team_invite(text,text,text,text,timestamptz) to authenticated;
grant execute on function public.admin_reissue_team_invite(uuid,text,timestamptz) to authenticated;
grant execute on function public.admin_revoke_team_invite(uuid) to authenticated;

create or replace function private.audit_team_invite_redemption()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if old.used_at is null and new.used_at is not null then
    insert into public.admin_audit_log(actor_user_id,action,target_type,target_id,details)
    values(auth.uid(),'team_invite_redeemed','team_invite',new.id::text,jsonb_build_object('role',new.role));
  end if;
  return new;
end
$$;

revoke execute on function private.audit_team_invite_redemption() from public, anon, authenticated;

drop trigger if exists trg_audit_team_invite_change on public.app_invites;
drop trigger if exists trg_audit_team_invite_redemption on public.app_invites;
create trigger trg_audit_team_invite_redemption
after update of used_at on public.app_invites
for each row execute function private.audit_team_invite_redemption();
