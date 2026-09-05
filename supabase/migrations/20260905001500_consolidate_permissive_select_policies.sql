-- Consolidate overlapping permissive SELECT policies without changing access semantics.
-- App invites: Admin may read all; Manager may read own staff invites.
-- Devices: owner retains own CRUD; Admin/Manager retains read-only visibility.

drop policy if exists admin_select_team_invites on public.app_invites;
drop policy if exists manager_select_own_staff_invites on public.app_invites;
create policy app_invites_admin_or_manager_select on public.app_invites
for select to authenticated
using (
  exists (
    select 1 from public.profiles p
    where p.id = (select auth.uid())
      and p.role = 'admin'::text
      and p.is_enabled = true
  )
  or (
    role = 'staff'::text
    and created_by = (select auth.uid())
    and exists (
      select 1 from public.profiles p
      where p.id = (select auth.uid())
        and p.role = 'manager'::text
        and p.is_enabled = true
    )
  )
);

drop policy if exists devices_admin_manager_read on public.devices;
drop policy if exists devices_owner_manage_own on public.devices;
create policy devices_owner_or_admin_manager_select on public.devices
for select to authenticated
using (
  user_id = (select auth.uid())
  or (select private.is_admin_or_manager())
);
create policy devices_owner_insert on public.devices
for insert to authenticated
with check (user_id = (select auth.uid()));
create policy devices_owner_update on public.devices
for update to authenticated
using (user_id = (select auth.uid()))
with check (user_id = (select auth.uid()));
create policy devices_owner_delete on public.devices
for delete to authenticated
using (user_id = (select auth.uid()));
