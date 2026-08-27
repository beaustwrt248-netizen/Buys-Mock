create table if not exists public.support_tickets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  category text not null check (category in ('valuation','pricing','inventory','scanner','account','update','other')),
  subject text not null check (char_length(subject) between 3 and 160),
  description text not null check (char_length(description) between 5 and 5000),
  status text not null default 'open' check (status in ('open','in_progress','waiting_on_user','resolved','closed')),
  priority text not null default 'normal' check (priority in ('low','normal','high','urgent')),
  assigned_to uuid references auth.users(id) on delete set null,
  app_version text,
  app_version_code integer,
  device_model text,
  android_version text,
  diagnostics jsonb not null default '{}'::jsonb,
  diagnostics_opt_in boolean not null default false,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  resolved_at timestamptz,
  closed_at timestamptz
);

create table if not exists public.support_ticket_messages (
  id uuid primary key default gen_random_uuid(),
  ticket_id uuid not null references public.support_tickets(id) on delete cascade,
  author_user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  author_role text not null check (author_role in ('user','admin')),
  body text not null check (char_length(body) between 1 and 5000),
  created_at timestamptz not null default now()
);

create table if not exists public.support_ticket_events (
  id bigint generated always as identity primary key,
  ticket_id uuid not null references public.support_tickets(id) on delete cascade,
  actor_user_id uuid references auth.users(id) on delete set null,
  event_type text not null,
  details jsonb not null default '{}'::jsonb,
  created_at timestamptz not null default now()
);

create table if not exists public.support_ticket_attachments (
  id uuid primary key default gen_random_uuid(),
  ticket_id uuid not null references public.support_tickets(id) on delete cascade,
  uploader_user_id uuid not null default auth.uid() references auth.users(id) on delete cascade,
  storage_path text not null unique,
  file_name text not null,
  content_type text,
  byte_size bigint check (byte_size is null or byte_size >= 0),
  created_at timestamptz not null default now()
);

create index if not exists support_tickets_user_created_idx on public.support_tickets(user_id, created_at desc);
create index if not exists support_tickets_status_created_idx on public.support_tickets(status, created_at desc);
create index if not exists support_ticket_messages_ticket_created_idx on public.support_ticket_messages(ticket_id, created_at);
create index if not exists support_ticket_events_ticket_created_idx on public.support_ticket_events(ticket_id, created_at);

alter table public.support_tickets enable row level security;
alter table public.support_ticket_messages enable row level security;
alter table public.support_ticket_events enable row level security;
alter table public.support_ticket_attachments enable row level security;

drop policy if exists support_tickets_select on public.support_tickets;
create policy support_tickets_select on public.support_tickets for select to authenticated
using (user_id = auth.uid() or private.is_admin_or_manager());

drop policy if exists support_tickets_insert on public.support_tickets;
create policy support_tickets_insert on public.support_tickets for insert to authenticated
with check (user_id = auth.uid() and status = 'open' and priority = 'normal' and assigned_to is null);

drop policy if exists support_tickets_admin_update on public.support_tickets;
create policy support_tickets_admin_update on public.support_tickets for update to authenticated
using (private.is_admin_or_manager()) with check (private.is_admin_or_manager());

drop policy if exists support_ticket_messages_select on public.support_ticket_messages;
create policy support_ticket_messages_select on public.support_ticket_messages for select to authenticated
using (exists (select 1 from public.support_tickets t where t.id = ticket_id and (t.user_id = auth.uid() or private.is_admin_or_manager())));

drop policy if exists support_ticket_messages_insert on public.support_ticket_messages;
create policy support_ticket_messages_insert on public.support_ticket_messages for insert to authenticated
with check (
  author_user_id = auth.uid()
  and (
    (author_role = 'user' and exists (select 1 from public.support_tickets t where t.id = ticket_id and t.user_id = auth.uid()))
    or (author_role = 'admin' and private.is_admin_or_manager())
  )
);

drop policy if exists support_ticket_events_select on public.support_ticket_events;
create policy support_ticket_events_select on public.support_ticket_events for select to authenticated
using (exists (select 1 from public.support_tickets t where t.id = ticket_id and (t.user_id = auth.uid() or private.is_admin_or_manager())));

drop policy if exists support_ticket_attachments_select on public.support_ticket_attachments;
create policy support_ticket_attachments_select on public.support_ticket_attachments for select to authenticated
using (exists (select 1 from public.support_tickets t where t.id = ticket_id and (t.user_id = auth.uid() or private.is_admin_or_manager())));

drop policy if exists support_ticket_attachments_insert on public.support_ticket_attachments;
create policy support_ticket_attachments_insert on public.support_ticket_attachments for insert to authenticated
with check (
  uploader_user_id = auth.uid()
  and exists (select 1 from public.support_tickets t where t.id = ticket_id and (t.user_id = auth.uid() or private.is_admin_or_manager()))
);

create or replace function public.support_touch_ticket()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  new.updated_at := now();
  if new.status = 'resolved' and old.status is distinct from 'resolved' then new.resolved_at := now(); end if;
  if new.status = 'closed' and old.status is distinct from 'closed' then new.closed_at := now(); end if;
  return new;
end $$;

drop trigger if exists trg_support_touch_ticket on public.support_tickets;
create trigger trg_support_touch_ticket before update on public.support_tickets
for each row execute function public.support_touch_ticket();

create or replace function public.support_log_ticket_event()
returns trigger language plpgsql security definer set search_path = public as $$
begin
  if tg_table_name = 'support_tickets' then
    if tg_op = 'INSERT' then
      insert into public.support_ticket_events(ticket_id,actor_user_id,event_type,details)
      values(new.id,auth.uid(),'created',jsonb_build_object('status',new.status,'category',new.category));
    else
      if new.status is distinct from old.status then
        insert into public.support_ticket_events(ticket_id,actor_user_id,event_type,details)
        values(new.id,auth.uid(),'status_changed',jsonb_build_object('from',old.status,'to',new.status));
      end if;
      if new.priority is distinct from old.priority then
        insert into public.support_ticket_events(ticket_id,actor_user_id,event_type,details)
        values(new.id,auth.uid(),'priority_changed',jsonb_build_object('from',old.priority,'to',new.priority));
      end if;
      if new.assigned_to is distinct from old.assigned_to then
        insert into public.support_ticket_events(ticket_id,actor_user_id,event_type,details)
        values(new.id,auth.uid(),'assignment_changed',jsonb_build_object('assigned_to',new.assigned_to));
      end if;
    end if;
  elsif tg_table_name = 'support_ticket_messages' then
    insert into public.support_ticket_events(ticket_id,actor_user_id,event_type,details)
    values(new.ticket_id,new.author_user_id,'message_added',jsonb_build_object('author_role',new.author_role));
  end if;
  return new;
end $$;

drop trigger if exists trg_support_ticket_event on public.support_tickets;
create trigger trg_support_ticket_event after insert or update on public.support_tickets
for each row execute function public.support_log_ticket_event();

drop trigger if exists trg_support_message_event on public.support_ticket_messages;
create trigger trg_support_message_event after insert on public.support_ticket_messages
for each row execute function public.support_log_ticket_event();

grant select, insert on public.support_tickets to authenticated;
grant update on public.support_tickets to authenticated;
grant select, insert on public.support_ticket_messages to authenticated;
grant select on public.support_ticket_events to authenticated;
grant select, insert on public.support_ticket_attachments to authenticated;

insert into storage.buckets (id,name,public,file_size_limit,allowed_mime_types)
values ('support-ticket-attachments','support-ticket-attachments',false,10485760,array['image/jpeg','image/png','image/webp','application/pdf'])
on conflict (id) do update set public=false,file_size_limit=10485760,allowed_mime_types=excluded.allowed_mime_types;

drop policy if exists support_ticket_storage_read on storage.objects;
create policy support_ticket_storage_read on storage.objects for select to authenticated
using (
  bucket_id='support-ticket-attachments'
  and exists (
    select 1 from public.support_ticket_attachments a
    join public.support_tickets t on t.id=a.ticket_id
    where a.storage_path=name and (t.user_id=auth.uid() or private.is_admin_or_manager())
  )
);

drop policy if exists support_ticket_storage_insert on storage.objects;
create policy support_ticket_storage_insert on storage.objects for insert to authenticated
with check (
  bucket_id='support-ticket-attachments'
  and (storage.foldername(name))[1]=auth.uid()::text
);
