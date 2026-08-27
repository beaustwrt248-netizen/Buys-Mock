-- Admin Control 2.0: least-privilege support staff access.
-- Staff may work only tickets explicitly assigned to their own account.
-- Admin/manager access and user ownership rules remain unchanged.

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
        or (
          private.is_support_staff()
          and t.assigned_to = auth.uid()
        )
      )
  );
$$;

revoke all on function private.can_work_support_ticket(uuid) from public;
grant execute on function private.can_work_support_ticket(uuid) to authenticated;

-- Users still see only their own tickets. Admin/manager see all. Staff see only assigned tickets.
drop policy if exists support_tickets_select on public.support_tickets;
create policy support_tickets_select on public.support_tickets for select to authenticated
using (
  user_id = auth.uid()
  or private.is_admin_or_manager()
  or (private.is_support_staff() and assigned_to = auth.uid())
);

-- Admin/manager retain unrestricted support updates. Staff may update only tickets that remain assigned to themselves.
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

-- Conversation privacy follows the ticket visibility contract.
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

-- Ticket event and attachment reads follow the same owner/admin/assigned-staff boundary.
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

-- Staff may attach supporting files only to tickets assigned to themselves; users retain owner upload behavior.
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

-- Storage object reads mirror attachment-row visibility. Bucket remains private and constrained by existing MIME/size policy.
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
