-- Allow authorised Morley Admin/Manager sessions to submit privacy-minimal crash metadata.
-- Preserve the existing protected role boundary for both reads and inserts.

drop policy if exists admin_error_events_authenticated_insert on public.admin_error_events;
create policy admin_error_events_authenticated_insert
on public.admin_error_events
for insert
to authenticated
with check (
  private.is_admin_or_manager()
  and length(app_version) between 1 and 160
  and length(device_model) between 1 and 160
  and length(failing_screen) between 1 and 160
  and length(error_class) between 1 and 160
  and occurred_at >= now() - interval '30 days'
  and occurred_at <= now() + interval '5 minutes'
  and received_at >= now() - interval '5 minutes'
  and received_at <= now() + interval '5 minutes'
);
