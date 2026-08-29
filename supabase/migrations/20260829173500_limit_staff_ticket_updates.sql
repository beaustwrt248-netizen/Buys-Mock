-- Admin Control 2.0: enforce least-privilege Staff support updates.
--
-- Existing RLS already limits Staff to tickets assigned to them, but row-level
-- policies cannot compare OLD and NEW column values. This trigger closes that
-- gap by allowing assigned Staff to move only the ticket workflow status while
-- reserving assignment, priority, ownership, diagnostics and other ticket
-- metadata for Admin/Manager workflows.
--
-- Non-destructive: no rows or columns are removed, user-owned ticket visibility
-- is unchanged, Admin/Manager authority is unchanged, and existing SLA/audit
-- triggers continue to run.

create schema if not exists private;
revoke all on schema private from public;

create or replace function private.support_enforce_staff_update_scope()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  -- Admin/Manager retain the existing update authority.
  if private.is_admin_or_manager() then
    return new;
  end if;

  -- This guard is specifically for enabled Staff sessions. Other callers remain
  -- subject to the existing RLS policies and do not gain any privilege here.
  if not private.is_support_staff() then
    return new;
  end if;

  if old.assigned_to is distinct from auth.uid()
     or new.assigned_to is distinct from old.assigned_to then
    raise exception 'staff may only update tickets currently assigned to them';
  end if;

  if new.user_id is distinct from old.user_id
     or new.category is distinct from old.category
     or new.subject is distinct from old.subject
     or new.description is distinct from old.description
     or new.priority is distinct from old.priority
     or new.app_version is distinct from old.app_version
     or new.app_version_code is distinct from old.app_version_code
     or new.device_model is distinct from old.device_model
     or new.android_version is distinct from old.android_version
     or new.diagnostics is distinct from old.diagnostics
     or new.diagnostics_opt_in is distinct from old.diagnostics_opt_in
     or new.created_at is distinct from old.created_at
     or new.sla_due_at is distinct from old.sla_due_at
     or new.first_response_at is distinct from old.first_response_at
     or new.resolved_at is distinct from old.resolved_at
     or new.closed_at is distinct from old.closed_at then
    raise exception 'staff may only change support ticket status';
  end if;

  if new.status is distinct from old.status
     and new.status not in ('in_progress','waiting_on_user','resolved') then
    raise exception 'staff cannot set this support ticket status';
  end if;

  return new;
end
$$;

revoke execute on function private.support_enforce_staff_update_scope()
  from public, anon, authenticated;

drop trigger if exists trg_support_enforce_staff_update_scope
  on public.support_tickets;
create trigger trg_support_enforce_staff_update_scope
before update on public.support_tickets
for each row execute function private.support_enforce_staff_update_scope();

comment on function private.support_enforce_staff_update_scope() is
  'Least-privilege guard: assigned Staff may change workflow status only; assignment, priority, ownership and ticket metadata remain Admin/Manager-controlled.';
