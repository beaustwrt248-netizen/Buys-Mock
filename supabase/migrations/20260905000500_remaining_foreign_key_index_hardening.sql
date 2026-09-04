-- Add covering indexes for remaining foreign keys reported by the Supabase Performance Advisor.
-- Index-only change: no RLS, grants, data or authority changes.

create index if not exists device_buy_price_history_device_catalog_id_idx on public.device_buy_price_history(device_catalog_id);
create index if not exists guardian_incident_events_actor_user_id_idx on public.guardian_incident_events(actor_user_id);
create index if not exists guardian_incidents_approved_by_idx on public.guardian_incidents(approved_by);
create index if not exists guardian_repairs_requested_by_idx on public.guardian_repairs(requested_by);
create index if not exists guardian_settings_updated_by_idx on public.guardian_settings(updated_by);
create index if not exists support_ticket_attachments_ticket_id_idx on public.support_ticket_attachments(ticket_id);
create index if not exists support_ticket_attachments_uploader_user_id_idx on public.support_ticket_attachments(uploader_user_id);
create index if not exists support_ticket_events_actor_user_id_idx on public.support_ticket_events(actor_user_id);
create index if not exists support_ticket_internal_notes_author_user_id_idx on public.support_ticket_internal_notes(author_user_id);
create index if not exists support_ticket_messages_author_user_id_idx on public.support_ticket_messages(author_user_id);
