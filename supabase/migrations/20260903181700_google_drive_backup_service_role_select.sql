-- Google Drive backup export access.
-- Keep these grants service_role-only and read-only.
grant select on table public.valuation_history to service_role;
grant select on table public.laptop_models to service_role;
grant select on table public.support_ticket_messages to service_role;
grant select on table public.support_ticket_events to service_role;
grant select on table public.support_ticket_internal_notes to service_role;
grant select on table public.support_ticket_attachments to service_role;
