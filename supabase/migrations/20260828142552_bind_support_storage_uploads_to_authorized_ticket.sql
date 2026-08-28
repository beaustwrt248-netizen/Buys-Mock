drop policy if exists support_ticket_storage_insert on storage.objects;
create policy support_ticket_storage_insert on storage.objects
for insert to authenticated
with check (
  bucket_id = 'support-ticket-attachments'
  and (storage.foldername(name))[1] = auth.uid()::text
  and exists (
    select 1
    from public.support_tickets t
    where t.id::text = (storage.foldername(name))[2]
      and (
        t.user_id = auth.uid()
        or private.is_admin_or_manager()
        or (private.is_support_staff() and t.assigned_to = auth.uid())
      )
  )
);
