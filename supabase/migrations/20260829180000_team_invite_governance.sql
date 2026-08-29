-- Admin Control team/staff invitation governance.
-- Admins may invite Staff or Managers. Managers may invite Staff only.
-- Invite secrets remain client-generated and only SHA-256 hashes are stored.

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

create policy admin_insert_team_invites
on public.app_invites
for insert
to authenticated
with check (
  created_by = (select auth.uid())
  and role in ('staff','manager')
  and exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid())
      and p.role = 'admin'
      and p.is_enabled = true
  )
);

create policy manager_insert_own_staff_invites
on public.app_invites
for insert
to authenticated
with check (
  created_by = (select auth.uid())
  and role = 'staff'
  and exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid())
      and p.role = 'manager'
      and p.is_enabled = true
  )
);

create policy admin_update_team_invites
on public.app_invites
for update
to authenticated
using (
  exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid())
      and p.role = 'admin'
      and p.is_enabled = true
  )
)
with check (
  role in ('staff','manager')
  and exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid())
      and p.role = 'admin'
      and p.is_enabled = true
  )
);

create policy manager_update_own_staff_invites
on public.app_invites
for update
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
)
with check (
  role = 'staff'
  and created_by = (select auth.uid())
  and exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid())
      and p.role = 'manager'
      and p.is_enabled = true
  )
);

create policy admin_delete_team_invites
on public.app_invites
for delete
to authenticated
using (
  exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid())
      and p.role = 'admin'
      and p.is_enabled = true
  )
);

create policy manager_delete_own_staff_invites
on public.app_invites
for delete
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

create or replace function private.audit_team_invite_change()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  invite_id uuid;
  invite_role text;
  action_name text;
begin
  if tg_op = 'INSERT' then
    invite_id := new.id;
    invite_role := new.role;
    action_name := 'team_invite_created';
  elsif tg_op = 'DELETE' then
    invite_id := old.id;
    invite_role := old.role;
    action_name := 'team_invite_revoked';
  else
    invite_id := new.id;
    invite_role := new.role;
    if old.used_at is null and new.used_at is not null then
      action_name := 'team_invite_redeemed';
    elsif old.code_hash is distinct from new.code_hash or old.expires_at is distinct from new.expires_at then
      action_name := 'team_invite_reissued';
    else
      return new;
    end if;
  end if;

  insert into public.admin_audit_log(actor_user_id, action, target_type, target_id, details)
  values (
    auth.uid(),
    action_name,
    'team_invite',
    invite_id::text,
    jsonb_build_object('role', invite_role)
  );

  if tg_op = 'DELETE' then return old; end if;
  return new;
end
$$;

revoke execute on function private.audit_team_invite_change() from public, anon, authenticated;

drop trigger if exists trg_audit_team_invite_change on public.app_invites;
create trigger trg_audit_team_invite_change
after insert or update or delete on public.app_invites
for each row execute function private.audit_team_invite_change();
