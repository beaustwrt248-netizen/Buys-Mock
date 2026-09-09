-- Allow authenticated Morley app sessions to submit privacy-minimal crash metadata.
-- Admin/manager remain the only readers via the existing select policy.

drop policy if exists admin_error_events_authenticated_insert on public.admin_error_events;
create policy admin_error_events_authenticated_insert
on public.admin_error_events
for insert
to authenticated
with check (
  auth.uid() is not null
  and length(app_version) between 1 and 160
  and length(device_model) between 1 and 160
  and length(failing_screen) between 1 and 160
  and length(error_class) between 1 and 160
  and occurred_at >= now() - interval '30 days'
  and occurred_at <= now() + interval '5 minutes'
  and received_at >= now() - interval '5 minutes'
  and received_at <= now() + interval '5 minutes'
);
