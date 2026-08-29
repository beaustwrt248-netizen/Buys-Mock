-- Remove Supabase default table privileges that bypass row-level policy intent.
-- Internal notes must expose only SELECT/INSERT to authenticated sessions;
-- RLS then limits those operations to Admin/Manager or assigned Staff.

revoke all on table public.support_ticket_internal_notes from anon;
revoke all on table public.support_ticket_internal_notes from authenticated;
grant select, insert on table public.support_ticket_internal_notes to authenticated;
