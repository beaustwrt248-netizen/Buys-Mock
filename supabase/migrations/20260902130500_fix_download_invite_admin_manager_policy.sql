-- Keep download-invite reads on the hardened private role helper.
-- The public helper intentionally has no authenticated EXECUTE grant.
drop policy if exists download_invites_admin_manager_read on public.app_download_invites;

create policy download_invites_admin_manager_read
on public.app_download_invites
for select
to authenticated
using ((select private.is_admin_or_manager()));
