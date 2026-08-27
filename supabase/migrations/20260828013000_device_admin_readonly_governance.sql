-- Admin Control device governance: preserve user ownership while making
-- fleet/adoption visibility read-only for Admin/Manager roles.
-- Device registration continues through register_device_public().

begin;

alter table public.devices enable row level security;

drop policy if exists devices_owner_all on public.devices;
drop policy if exists devices_owner_manage_own on public.devices;
drop policy if exists devices_admin_manager_read on public.devices;

create policy devices_owner_manage_own
on public.devices
for all
to authenticated
using ((select auth.uid()) = user_id)
with check ((select auth.uid()) = user_id);

create policy devices_admin_manager_read
on public.devices
for select
to authenticated
using (private.is_admin_or_manager());

commit;
