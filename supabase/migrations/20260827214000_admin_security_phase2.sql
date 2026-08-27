-- Morley Security/Admin hardening phase 2.
-- Keeps trigger-only SECURITY DEFINER helpers out of the exposed public schema
-- and narrows support attachment uploads to a ticket owned by the caller.

create schema if not exists private;
revoke all on schema private from public;

create or replace function private.support_touch_ticket()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  new.updated_at := now();
  if new.status = 'resolved' and old.status is distinct from 'resolved' then
    new.resolved_at := now();
  end if;
  if new.status = 'closed' and old.status is distinct from 'closed' then
    new.closed_at := now();
  end if;
  return new;
end
$$;

create or replace function private.support_log_ticket_event()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_table_name = 'support_tickets' then
    if tg_op = 'INSERT' then
      insert into public.support_ticket_events(ticket_id, actor_user_id, event_type, details)
      values(new.id, auth.uid(), 'created', jsonb_build_object('status', new.status, 'category', new.category));
    else
      if new.status is distinct from old.status then
        insert into public.support_ticket_events(ticket_id, actor_user_id, event_type, details)
        values(new.id, auth.uid(), 'status_changed', jsonb_build_object('from', old.status, 'to', new.status));
      end if;
      if new.priority is distinct from old.priority then
        insert into public.support_ticket_events(ticket_id, actor_user_id, event_type, details)
        values(new.id, auth.uid(), 'priority_changed', jsonb_build_object('from', old.priority, 'to', new.priority));
      end if;
      if new.assigned_to is distinct from old.assigned_to then
        insert into public.support_ticket_events(ticket_id, actor_user_id, event_type, details)
        values(new.id, auth.uid(), 'assignment_changed', jsonb_build_object('assigned_to', new.assigned_to));
      end if;
    end if;
  elsif tg_table_name = 'support_ticket_messages' then
    insert into public.support_ticket_events(ticket_id, actor_user_id, event_type, details)
    values(new.ticket_id, new.author_user_id, 'message_added', jsonb_build_object('author_role', new.author_role));
  end if;
  return new;
end
$$;

revoke execute on function private.support_touch_ticket() from public, anon, authenticated;
revoke execute on function private.support_log_ticket_event() from public, anon, authenticated;

drop trigger if exists trg_support_touch_ticket on public.support_tickets;
create trigger trg_support_touch_ticket
before update on public.support_tickets
for each row execute function private.support_touch_ticket();

drop trigger if exists trg_support_ticket_event on public.support_tickets;
create trigger trg_support_ticket_event
after insert or update on public.support_tickets
for each row execute function private.support_log_ticket_event();

drop trigger if exists trg_support_message_event on public.support_ticket_messages;
create trigger trg_support_message_event
after insert on public.support_ticket_messages
for each row execute function private.support_log_ticket_event();

-- Remove the now-unused exposed trigger helpers.
drop function if exists public.support_touch_ticket();
drop function if exists public.support_log_ticket_event();

-- Uploads must remain in the caller's folder AND reference a ticket they own.
-- This preserves the current Android upload order (object first, metadata second)
-- without allowing arbitrary authenticated uploads into the private ticket bucket.
drop policy if exists support_ticket_storage_insert on storage.objects;
create policy support_ticket_storage_insert
on storage.objects
for insert
to authenticated
with check (
  bucket_id = 'support-ticket-attachments'
  and (storage.foldername(name))[1] = (select auth.uid())::text
  and exists (
    select 1
    from public.support_tickets t
    where t.user_id = (select auth.uid())
      and t.id::text = (storage.foldername(name))[2]
  )
);
