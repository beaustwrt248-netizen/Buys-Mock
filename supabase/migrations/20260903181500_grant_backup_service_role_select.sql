-- Allow the server-side backup function's service-role client to read the
-- production tables included in the Google Drive backup export.
--
-- This grant is intentionally limited to SELECT and service_role only.
-- It does not change authenticated/anon privileges or restore behavior.

grant select on table public.valuation_history to service_role;
grant select on table public.laptop_models to service_role;
grant select on table public.support_ticket_messages to service_role;
grant select on table public.support_ticket_events to service_role;
grant select on table public.support_ticket_internal_notes to service_role;
grant select on table public.support_ticket_attachments to service_role;
