-- Private support notes for Admin Control.
-- Ticket owners must never be able to read or create these records.
-- Admin/Manager may access any ticket; Staff remain assigned-ticket only.

create table if not exists public.support_ticket_internal_notes (
  id uuid primary key default gen_random_uuid(),
  ticket_id uuid not null references public.support_tickets(id) on delete cascade,
  author_user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  body text not null check (char_length(body) between 1 and 5000),
  created_at timestamptz not null default now()
);

create index if not exists support_ticket_internal_notes_ticket_created_idx
  on public.support_ticket_internal_notes(ticket_id, created_at);

alter table public.support_ticket_internal_notes enable row level security;

revoke all on public.support_ticket_internal_notes from anon;
revoke update, delete on public.support_ticket_internal_notes from authenticated;
grant select, insert on public.support_ticket_internal_notes to authenticated;

drop policy if exists support_ticket_internal_notes_select on public.support_ticket_internal_notes;
create policy support_ticket_internal_notes_select
on public.support_ticket_internal_notes
for select
to authenticated
using (
  private.is_admin_or_manager()
  or (
    private.is_support_staff()
    and exists (
      select 1
      from public.support_tickets t
      where t.id = support_ticket_internal_notes.ticket_id
        and t.assigned_to = auth.uid()
    )
  )
);

drop policy if exists support_ticket_internal_notes_insert on public.support_ticket_internal_notes;
create policy support_ticket_internal_notes_insert
on public.support_ticket_internal_notes
for insert
to authenticated
with check (
  author_user_id = auth.uid()
  and (
    private.is_admin_or_manager()
    or (
      private.is_support_staff()
      and exists (
        select 1
        from public.support_tickets t
        where t.id = support_ticket_internal_notes.ticket_id
          and t.assigned_to = auth.uid()
      )
    )
  )
);

create or replace function private.audit_support_internal_note_insert()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  insert into public.admin_audit_log(actor_user_id, action, target_type, target_id, details)
  values (
    auth.uid(),
    'support_internal_note_added',
    'support_ticket',
    new.ticket_id::text,
    jsonb_build_object('note_id', new.id)
  );
  return new;
end
$$;

revoke execute on function private.audit_support_internal_note_insert() from public, anon, authenticated;

drop trigger if exists trg_audit_support_internal_note_insert on public.support_ticket_internal_notes;
create trigger trg_audit_support_internal_note_insert
after insert on public.support_ticket_internal_notes
for each row execute function private.audit_support_internal_note_insert();
