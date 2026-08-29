-- Admin Control 2.0: durable, content-free audit events for privileged support communications.
-- Non-destructive: existing ticket/message/internal-note RLS and ownership rules remain authoritative.
-- Audit rows capture only actor/target metadata; reply and internal-note bodies are never copied.

create schema if not exists private;
revoke all on schema private from public;

create or replace function private.audit_privileged_support_reply()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  actor_role text;
begin
  if auth.uid() is null or new.author_user_id is distinct from auth.uid() then
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

  insert into public.admin_audit_log(
    actor_user_id,
    action,
    target_type,
    target_id,
    details
  ) values (
    auth.uid(),
    'support_reply_added',
    'support_ticket',
    new.ticket_id::text,
    jsonb_build_object(
      'actor_role', actor_role,
      'message_id', new.id
    )
  );

  return new;
end
$$;

create or replace function private.audit_support_internal_note()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  actor_role text;
begin
  if auth.uid() is null or new.author_user_id is distinct from auth.uid() then
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

  insert into public.admin_audit_log(
    actor_user_id,
    action,
    target_type,
    target_id,
    details
  ) values (
    auth.uid(),
    'support_internal_note_added',
    'support_ticket',
    new.ticket_id::text,
    jsonb_build_object(
      'actor_role', actor_role,
      'note_id', new.id
    )
  );

  return new;
end
$$;

revoke execute on function private.audit_privileged_support_reply() from public, anon, authenticated;
revoke execute on function private.audit_support_internal_note() from public, anon, authenticated;

drop trigger if exists trg_audit_privileged_support_reply on public.support_ticket_messages;
create trigger trg_audit_privileged_support_reply
after insert on public.support_ticket_messages
for each row execute function private.audit_privileged_support_reply();

drop trigger if exists trg_audit_support_internal_note on public.support_ticket_internal_notes;
create trigger trg_audit_support_internal_note
after insert on public.support_ticket_internal_notes
for each row execute function private.audit_support_internal_note();

-- Keep the application-facing audit history append-only.
revoke update, delete on table public.admin_audit_log from anon, authenticated;
