-- Performance hardening for Nova knowledge and authoritative inventory/sales lifecycle.
-- This migration does not broaden access or authority. It preserves existing Admin/Manager RLS semantics.

create index if not exists inventory_items_created_by_idx on public.inventory_items(created_by);
create index if not exists inventory_items_updated_by_idx on public.inventory_items(updated_by);
create index if not exists sales_records_created_by_idx on public.sales_records(created_by);
create index if not exists nova_knowledge_items_created_by_idx on public.nova_knowledge_items(created_by);
create index if not exists nova_knowledge_items_updated_by_idx on public.nova_knowledge_items(updated_by);
create index if not exists nova_knowledge_revisions_changed_by_idx on public.nova_knowledge_revisions(changed_by);

-- Cache request-stable role/auth checks through initPlans rather than evaluating them per row.
alter policy inventory_items_admin_manager_select on public.inventory_items
using ((select private.is_admin_or_manager()));

alter policy inventory_items_admin_manager_insert on public.inventory_items
with check (
  (select private.is_admin_or_manager())
  and created_by = (select auth.uid())
  and updated_by = (select auth.uid())
);

alter policy inventory_items_admin_manager_update on public.inventory_items
using ((select private.is_admin_or_manager()))
with check (
  (select private.is_admin_or_manager())
  and updated_by = (select auth.uid())
);

alter policy inventory_items_admin_manager_delete on public.inventory_items
using ((select private.is_admin_or_manager()));

alter policy sales_records_admin_manager_select on public.sales_records
using ((select private.is_admin_or_manager()));

alter policy sales_records_admin_manager_insert on public.sales_records
with check (
  (select private.is_admin_or_manager())
  and created_by = (select auth.uid())
);

alter policy sales_records_admin_manager_update on public.sales_records
using ((select private.is_admin_or_manager()))
with check ((select private.is_admin_or_manager()));

alter policy sales_records_admin_manager_delete on public.sales_records
using ((select private.is_admin_or_manager()));
