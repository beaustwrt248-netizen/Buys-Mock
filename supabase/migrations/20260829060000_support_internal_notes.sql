-- Admin Control: private internal notes for support operations.
-- Notes are never visible to ticket owners; only Admin/Manager or assigned Staff may read/write them.

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

drop policy if exists support_ticket_internal_notes_select on public.support_ticket_internal_notes;
create policy support_ticket_internal_notes_select on public.support_ticket_internal_notes
for select to authenticated
using (private.can_work_support_ticket(ticket_id));

drop policy if exists support_ticket_internal_notes_insert on public.support_ticket_internal_notes;
create policy support_ticket_internal_notes_insert on public.support_ticket_internal_notes
for insert to authenticated
with check (
  author_user_id = auth.uid()
  and private.can_work_support_ticket(ticket_id)
);

grant select, insert on public.support_ticket_internal_notes to authenticated;
