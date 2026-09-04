-- Optimise request-stable support RLS checks with Postgres initPlans.
-- Policy permissions and row-access semantics are preserved.

alter policy support_tickets_select on public.support_tickets
using (
  user_id = (select auth.uid())
  or (select private.is_admin_or_manager())
  or ((select private.is_support_staff()) and assigned_to = (select auth.uid()))
);

alter policy support_tickets_insert on public.support_tickets
with check (
  user_id = (select auth.uid())
  and status = 'open'::text
  and priority = 'normal'::text
  and assigned_to is null
);

alter policy support_tickets_admin_update on public.support_tickets
using (
  (select private.is_admin_or_manager())
  or ((select private.is_support_staff()) and assigned_to = (select auth.uid()))
)
with check (
  (select private.is_admin_or_manager())
  or ((select private.is_support_staff()) and assigned_to = (select auth.uid()))
);

alter policy support_ticket_messages_select on public.support_ticket_messages
using (exists (
  select 1 from public.support_tickets t
  where t.id = support_ticket_messages.ticket_id
    and (
      t.user_id = (select auth.uid())
      or (select private.is_admin_or_manager())
      or ((select private.is_support_staff()) and t.assigned_to = (select auth.uid()))
    )
));

alter policy support_ticket_messages_insert on public.support_ticket_messages
with check (
  author_user_id = (select auth.uid())
  and (
    (author_role = 'user'::text and exists (
      select 1 from public.support_tickets t
      where t.id = support_ticket_messages.ticket_id
        and t.user_id = (select auth.uid())
    ))
    or (
      author_role = 'admin'::text
      and ((select private.is_admin_or_manager()) or private.can_work_support_ticket(ticket_id))
    )
  )
);

alter policy support_ticket_events_select on public.support_ticket_events
using (exists (
  select 1 from public.support_tickets t
  where t.id = support_ticket_events.ticket_id
    and (
      t.user_id = (select auth.uid())
      or (select private.is_admin_or_manager())
      or ((select private.is_support_staff()) and t.assigned_to = (select auth.uid()))
    )
));

alter policy support_ticket_attachments_select on public.support_ticket_attachments
using (exists (
  select 1 from public.support_tickets t
  where t.id = support_ticket_attachments.ticket_id
    and (
      t.user_id = (select auth.uid())
      or (select private.is_admin_or_manager())
      or ((select private.is_support_staff()) and t.assigned_to = (select auth.uid()))
    )
));

alter policy support_ticket_attachments_insert on public.support_ticket_attachments
with check (
  uploader_user_id = (select auth.uid())
  and exists (
    select 1 from public.support_tickets t
    where t.id = support_ticket_attachments.ticket_id
      and (
        t.user_id = (select auth.uid())
        or (select private.is_admin_or_manager())
        or ((select private.is_support_staff()) and t.assigned_to = (select auth.uid()))
      )
  )
);

alter policy support_ticket_internal_notes_select on public.support_ticket_internal_notes
using (
  (select private.is_admin_or_manager())
  or (
    (select private.is_support_staff())
    and exists (
      select 1 from public.support_tickets t
      where t.id = support_ticket_internal_notes.ticket_id
        and t.assigned_to = (select auth.uid())
    )
  )
);

alter policy support_ticket_internal_notes_insert on public.support_ticket_internal_notes
with check (
  author_user_id = (select auth.uid())
  and (
    (select private.is_admin_or_manager())
    or (
      (select private.is_support_staff())
      and exists (
        select 1 from public.support_tickets t
        where t.id = support_ticket_internal_notes.ticket_id
          and t.assigned_to = (select auth.uid())
      )
    )
  )
);
