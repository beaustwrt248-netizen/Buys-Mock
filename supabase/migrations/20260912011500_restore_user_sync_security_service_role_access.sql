-- Restore the minimum service-role privileges required by the authenticated
-- user sync and security Edge Functions. These tables remain inaccessible
-- to anonymous clients; RLS/authentication boundaries are unchanged.

grant select on table public.user_sync_state to service_role;
grant insert on table public.user_sync_events to service_role;
grant select, insert, update on table public.user_session_devices to service_role;
grant select, insert on table public.user_security_events to service_role;

grant usage on sequence public.user_sync_events_id_seq to service_role;
grant usage on sequence public.user_security_events_id_seq to service_role;
