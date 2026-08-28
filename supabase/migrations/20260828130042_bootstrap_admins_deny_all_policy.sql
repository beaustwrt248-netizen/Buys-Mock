drop policy if exists bootstrap_admins_deny_all on public.bootstrap_admins;
create policy bootstrap_admins_deny_all
on public.bootstrap_admins
for all
to public
using (false)
with check (false);
