-- The protected Guardian Edge Functions use the Supabase service role as their
-- backend database client. Browser roles remain governed by the existing RLS.
-- Grant only the table operations required by the repair pipeline.

grant select, update on table public.guardian_repairs to service_role;
grant insert on table public.guardian_activity to service_role;
