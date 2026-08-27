-- Admin Control 2.0 production reconciliation.
-- Safely reconciles the already-merged SLA/assignment and assigned-Staff support contracts
-- on databases where later Admin security migrations were applied first.
-- Non-destructive: preserves user ownership, existing ticket data and private attachment limits.

create schema if not exists private;
revoke all on schema private from public;

alter table public.support_tickets
  add column if not exists sla_due_at timestamptz,
  add column if not exists first_response_at timestamptz;

create or replace function private.support_sla_due(created_at_value timestamptz, priority_value text)
returns timestamptz
language sql
immutable
security invoker
set search_path = ''
as $$
  select created_at_value + case priority_value
    when 'urgent' then interval '2 hours'
    when 'high' then interval '8 hours'
    when 'low' then interval '72 hours'
    else interval '24 hours'
  end
$$;

revoke all on function private.support_sla_due(timestamptz, text) from public;

update public.support_tickets
set sla_due_at = private.support_sla_due(created_at, priority)
where sla_due_at is null;

create or replace function private.support_touch_ticket()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  new.updated_at := now();

  if new.sla_due_at is null or new.priority is distinct from old.priority then
    new.sla_due_at := private.support_sla_due(new.created_at, new.priority);
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

revoke execute on function private.support_touch_ticket() from public, anon, authenticated;

drop trigger if exists trg_support_touch_ticket on public.support_tickets;
create trigger trg_support_touch_ticket
before update on public.support_tickets
for each row execute function private.support_touch_ticket();

create or replace function private.support_mark_first_response()
returns trigger
language plpgsql
security definer
set search_path = ''
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

revoke execute on function private.support_mark_first_response() from public, anon, authenticated;

drop trigger if exists trg_support_first_response on public.support_ticket_messages;
create trigger trg_support_first_response
after insert on public.support_ticket_messages
for each row execute function private.support_mark_first_response();

-- Remove stale exposed trigger helpers if an older migration created them.
drop function if exists public.support_touch_ticket();
drop function if exists public.support_mark_first_response();

create index if not exists support_tickets_assignment_updated_idx
  on public.support_tickets(assigned_to, updated_at desc);
create index if not exists support_tickets_sla_due_idx
  on public.support_tickets(sla_due_at)
  where status not in ('resolved','closed');

create or replace function private.is_support_staff()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.profiles p
    where p.id = auth.uid()
      and p.is_enabled = true
      and p.role = 'staff'
  );
$$;

revoke all on function private.is_support_staff() from public;
grant execute on function private.is_support_staff() to authenticated;

create or replace function private.can_work_support_ticket(ticket_uuid uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.support_tickets t
    where t.id = ticket_uuid
      and (
        private.is_admin_or_manager()
        or (private.is_support_staff() and t.assigned_to = auth.uid())
      )
  );
$$;

revoke all on function private.can_work_support_ticket(uuid) from public;
grant execute on function private.can_work_support_ticket(uuid) to authenticated;

drop policy if exists support_tickets_select on public.support_tickets;
create policy support_tickets_select on public.support_tickets for select to authenticated
using (
  user_id = auth.uid()
  or private.is_admin_or_manager()
  or (private.is_support_staff() and assigned_to = auth.uid())
);

drop policy if exists support_tickets_admin_update on public.support_tickets;
create policy support_tickets_admin_update on public.support_tickets for update to authenticated
using (
  private.is_admin_or_manager()
  or (private.is_support_staff() and assigned_to = auth.uid())
)
with check (
  private.is_admin_or_manager()
  or (private.is_support_staff() and assigned_to = auth.uid())
);

drop policy if exists support_ticket_messages_select on public.support_ticket_messages;
create policy support_ticket_messages_select on public.support_ticket_messages for select to authenticated
using (
  exists (
    select 1 from public.support_tickets t
    where t.id = ticket_id
      and (
        t.user_id = auth.uid()
        or private.is_admin_or_manager()
        or (private.is_support_staff() and t.assigned_to = auth.uid())
      )
  )
);

drop policy if exists support_ticket_messages_insert on public.support_ticket_messages;
create policy support_ticket_messages_insert on public.support_ticket_messages for insert to authenticated
with check (
  author_user_id = auth.uid()
  and (
    (author_role = 'user' and exists (
      select 1 from public.support_tickets t where t.id = ticket_id and t.user_id = auth.uid()
    ))
    or (
      author_role = 'admin'
      and (
        private.is_admin_or_manager()
        or private.can_work_support_ticket(ticket_id)
      )
    )
  )
);

drop policy if exists support_ticket_events_select on public.support_ticket_events;
create policy support_ticket_events_select on public.support_ticket_events for select to authenticated
using (
  exists (
    select 1 from public.support_tickets t
    where t.id = ticket_id
      and (
        t.user_id = auth.uid()
        or private.is_admin_or_manager()
        or (private.is_support_staff() and t.assigned_to = auth.uid())
      )
  )
);

drop policy if exists support_ticket_attachments_select on public.support_ticket_attachments;
create policy support_ticket_attachments_select on public.support_ticket_attachments for select to authenticated
using (
  exists (
    select 1 from public.support_tickets t
    where t.id = ticket_id
      and (
        t.user_id = auth.uid()
        or private.is_admin_or_manager()
        or (private.is_support_staff() and t.assigned_to = auth.uid())
      )
  )
);

drop policy if exists support_ticket_attachments_insert on public.support_ticket_attachments;
create policy support_ticket_attachments_insert on public.support_ticket_attachments for insert to authenticated
with check (
  uploader_user_id = auth.uid()
  and exists (
    select 1 from public.support_tickets t
    where t.id = ticket_id
      and (
        t.user_id = auth.uid()
        or private.is_admin_or_manager()
        or (private.is_support_staff() and t.assigned_to = auth.uid())
      )
  )
);

drop policy if exists support_ticket_storage_read on storage.objects;
create policy support_ticket_storage_read on storage.objects for select to authenticated
using (
  bucket_id = 'support-ticket-attachments'
  and exists (
    select 1
    from public.support_ticket_attachments a
    join public.support_tickets t on t.id = a.ticket_id
    where a.storage_path = name
      and (
        t.user_id = auth.uid()
        or private.is_admin_or_manager()
        or (private.is_support_staff() and t.assigned_to = auth.uid())
      )
  )
);

comment on column public.support_tickets.sla_due_at is
  'Operational support target derived from created_at and priority; does not change ticket ownership or RLS.';
comment on column public.support_tickets.first_response_at is
  'Timestamp of the first authorised staff/admin/manager support reply.';
