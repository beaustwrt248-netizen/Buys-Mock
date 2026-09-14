-- Approved narrow performance cleanup.
-- Preserve app_invites_active_email_idx: it is the UNIQUE integrity constraint
-- enforcing one unused invite per normalized email.
-- Remove only the later non-unique duplicate lookup index.

do $$
begin
  if to_regclass('public.app_invites_active_email_idx') is null then
    raise exception 'required UNIQUE invite integrity index app_invites_active_email_idx is missing';
  end if;
end
$$;

drop index if exists public.idx_app_invites_email_unused;
