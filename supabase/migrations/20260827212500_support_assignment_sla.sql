-- Admin Control 2.0 support triage increment.
-- Non-destructive: preserves existing support-ticket RLS and ownership policies.
-- Adds durable SLA timestamps and first-response tracking only.

alter table public.support_tickets
  add column if not exists sla_due_at timestamptz,
  add column if not exists first_response_at timestamptz;

create or replace function public.support_sla_due(created_at_value timestamptz, priority_value text)
returns timestamptz
language sql
immutable
set search_path = public
as $$
  select created_at_value + case priority_value
    when 'urgent' then interval '2 hours'
    when 'high' then interval '8 hours'
    when 'low' then interval '72 hours'
    else interval '24 hours'
  end
$$;

update public.support_tickets
set sla_due_at = public.support_sla_due(created_at, priority)
where sla_due_at is null;

create or replace function public.support_touch_ticket()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  new.updated_at := now();

  if new.sla_due_at is null or new.priority is distinct from old.priority then
    new.sla_due_at := public.support_sla_due(new.created_at, new.priority);
  end if;

  if new.status = 'resolved' and old.status is distinct from 'resolved' then
    new.resolved_at := now();
  end if;
  if new.status = 'closed' and old.status is distinct from 'closed' then
    new.closed_at := now();
  end if;

  return new;
end
$$;

create or replace function public.support_mark_first_response()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if new.author_role = 'admin' then
    update public.support_tickets
       set first_response_at = coalesce(first_response_at, new.created_at)
     where id = new.ticket_id
       and first_response_at is null;
  end if;
  return new;
end
$$;

drop trigger if exists trg_support_first_response on public.support_ticket_messages;
create trigger trg_support_first_response
after insert on public.support_ticket_messages
for each row execute function public.support_mark_first_response();

create index if not exists support_tickets_assignment_updated_idx
  on public.support_tickets(assigned_to, updated_at desc);
create index if not exists support_tickets_sla_due_idx
  on public.support_tickets(sla_due_at)
  where status not in ('resolved','closed');

comment on column public.support_tickets.sla_due_at is
  'Operational support target derived from created_at and priority; does not change ticket ownership or RLS.';
comment on column public.support_tickets.first_response_at is
  'Timestamp of the first authorised admin/manager support reply.';
