-- Enforce privacy-minimal Admin Android telemetry at the database boundary.
-- The client already sanitizes these values; these constraints make that
-- contract authoritative even for modified or nonstandard clients.

alter table public.admin_error_events
  add constraint admin_error_events_app_version_safe_chars
  check (app_version ~ '^[A-Za-z0-9 ._:/()-]+$') not valid;

alter table public.admin_error_events
  add constraint admin_error_events_device_model_safe_chars
  check (device_model ~ '^[A-Za-z0-9 ._:/()-]+$') not valid;

alter table public.admin_error_events
  add constraint admin_error_events_failing_screen_safe_chars
  check (failing_screen ~ '^[A-Za-z0-9 ._:/()-]+$') not valid;

alter table public.admin_error_events
  add constraint admin_error_events_error_class_safe_chars
  check (error_class ~ '^[A-Za-z0-9 ._:/()-]+$') not valid;

alter table public.admin_error_events validate constraint admin_error_events_app_version_safe_chars;
alter table public.admin_error_events validate constraint admin_error_events_device_model_safe_chars;
alter table public.admin_error_events validate constraint admin_error_events_failing_screen_safe_chars;
alter table public.admin_error_events validate constraint admin_error_events_error_class_safe_chars;

comment on constraint admin_error_events_app_version_safe_chars on public.admin_error_events is
  'Rejects free-form or identifier-like telemetry values outside the Admin client sanitization allowlist.';
comment on constraint admin_error_events_device_model_safe_chars on public.admin_error_events is
  'Rejects free-form or identifier-like telemetry values outside the Admin client sanitization allowlist.';
comment on constraint admin_error_events_failing_screen_safe_chars on public.admin_error_events is
  'Rejects free-form or identifier-like telemetry values outside the Admin client sanitization allowlist.';
comment on constraint admin_error_events_error_class_safe_chars on public.admin_error_events is
  'Rejects free-form or identifier-like telemetry values outside the Admin client sanitization allowlist.';
