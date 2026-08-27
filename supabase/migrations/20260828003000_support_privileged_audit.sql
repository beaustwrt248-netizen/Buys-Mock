-- Admin Control 2.0: durable audit trail for privileged support-ticket changes.
-- Non-destructive: preserves existing support RLS, ownership, SLA, message and attachment policies.
-- Records only operational change metadata; never copies ticket descriptions or message content.

create schema if not exists private;
revoke all on schema private from public;

create or replace function private.support_log_privileged_admin_audit()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  actor_role text;
  changes jsonb := '{}'::jsonb;
begin
  if auth.uid() is null then
    return new;
  end if;

  select p.role
    into actor_role
  from public.profiles p
  where p.id = auth.uid()
    and p.is_enabled
    and p.role in ('admin','manager','staff')
  limit 1;

  if actor_role is null then
    return new;
  end if;

  if new.status is distinct from old.status then
    changes := changes || jsonb_build_object(
      'status', jsonb_build_object('from', old.status, 'to', new.status)
    );
  end if;

  if new.priority is distinct from old.priority then
    changes := changes || jsonb_build_object(
      'priority', jsonb_build_object('from', old.priority, 'to', new.priority)
    );
  end if;

  if new.assigned_to is distinct from old.assigned_to then
    changes := changes || jsonb_build_object(
      'assigned_to', jsonb_build_object('from', old.assigned_to, 'to', new.assigned_to)
    );
  end if;

  if changes = '{}'::jsonb then
    return new;
  end if;

  insert into public.admin_audit_log(
    actor_user_id,
    action,
    target_type,
    target_id,
    details
  ) values (
    auth.uid(),
    'support_ticket_changed',
    'support_ticket',
    new.id::text,
    jsonb_build_object('actor_role', actor_role, 'changes', changes)
  );

  return new;
end
$$;

revoke execute on function private.support_log_privileged_admin_audit() from public, anon, authenticated;

drop trigger if exists trg_support_privileged_admin_audit on public.support_tickets;
create trigger trg_support_privileged_admin_audit
after update on public.support_tickets
for each row execute function private.support_log_privileged_admin_audit();

-- Admin audit entries are append-only for application roles.
-- Service-role/database-owner maintenance remains available outside normal clients.
revoke update, delete on table public.admin_audit_log from anon, authenticated;
