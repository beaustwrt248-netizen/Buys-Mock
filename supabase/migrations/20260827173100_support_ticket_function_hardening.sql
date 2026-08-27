-- Harden privileged trigger helpers: SECURITY DEFINER is required so audit events can be
-- written regardless of caller RLS, but these functions are trigger-only and must not be
-- callable as public RPC endpoints.

create or replace function public.support_touch_ticket()
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

create or replace function public.support_log_ticket_event()
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

revoke execute on function public.support_touch_ticket() from public, anon, authenticated;
revoke execute on function public.support_log_ticket_event() from public, anon, authenticated;
