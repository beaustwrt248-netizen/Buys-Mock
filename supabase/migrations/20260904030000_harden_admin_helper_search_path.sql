-- Harden privileged role helpers against search-path hijacking without
-- changing their bodies, ownership, volatility, role semantics or execute ACLs.
-- Both function bodies already schema-qualify public.profiles and auth.uid().

alter function public.is_admin()
  set search_path = '';

alter function public.is_admin_or_manager()
  set search_path = '';
