-- Explicit, audited Admin/Manager lifecycle operations.
-- These RPCs are user-triggered business actions; Nova learning never calls them.

create or replace function public.admin_inventory_create(
  p_item_type text,
  p_item_summary text,
  p_acquired_price numeric,
  p_expected_sale_price numeric default null,
  p_device_catalog_id bigint default null,
  p_valuation_id uuid default null,
  p_model_number text default null,
  p_storage text default null,
  p_item_grade text default null,
  p_notes text default null
) returns uuid
language plpgsql
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_id uuid;
  v_actor uuid := auth.uid();
begin
  if v_actor is null or not private.is_admin_or_manager() then
    raise exception 'Admin or Manager access required' using errcode='42501';
  end if;
  if p_item_type not in ('mobile_phone','tablet','laptop','desktop','console','wearable','other') then
    raise exception 'Invalid item type' using errcode='22023';
  end if;
  if char_length(trim(coalesce(p_item_summary,''))) < 2 then
    raise exception 'Item summary is required' using errcode='22023';
  end if;
  if p_acquired_price is null or p_acquired_price < 0 then
    raise exception 'Acquired price must be zero or greater' using errcode='22023';
  end if;
  if p_expected_sale_price is not null and p_expected_sale_price < 0 then
    raise exception 'Expected sale price must be zero or greater' using errcode='22023';
  end if;
  if p_item_grade is not null and p_item_grade not in ('A','B','C') then
    raise exception 'Invalid item grade' using errcode='22023';
  end if;
  if p_device_catalog_id is not null and not exists(select 1 from public.device_catalog where id=p_device_catalog_id and active) then
    raise exception 'Device catalogue item not found' using errcode='22023';
  end if;
  if p_valuation_id is not null and not exists(select 1 from public.valuation_history where id=p_valuation_id) then
    raise exception 'Valuation not found' using errcode='22023';
  end if;

  insert into public.inventory_items(
    valuation_id,device_catalog_id,item_type,item_summary,model_number,storage,item_grade,
    acquired_price,expected_sale_price,status,source,notes,created_by,updated_by
  ) values (
    p_valuation_id,p_device_catalog_id,p_item_type,trim(p_item_summary),nullif(trim(coalesce(p_model_number,'')),''),
    nullif(trim(coalesce(p_storage,'')),''),p_item_grade,p_acquired_price,p_expected_sale_price,'in_stock',
    case when p_valuation_id is not null then 'valuation' else 'manual' end,
    nullif(trim(coalesce(p_notes,'')),''),v_actor,v_actor
  ) returning id into v_id;

  if p_valuation_id is not null then
    update public.valuation_history
      set status='bought', bought_price=coalesce(bought_price,p_acquired_price), updated_at=now()
      where id=p_valuation_id and status in ('quoted','bought');
  end if;

  insert into public.admin_audit_log(actor_user_id,action,target_type,target_id,details)
  values(v_actor,'inventory.create','inventory_item',v_id::text,
    jsonb_build_object('item_type',p_item_type,'device_catalog_id',p_device_catalog_id,'valuation_id',p_valuation_id,'acquired_price',p_acquired_price));
  return v_id;
end;
$$;

create or replace function public.admin_inventory_set_status(
  p_inventory_item_id uuid,
  p_status text
) returns boolean
language plpgsql
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_actor uuid := auth.uid();
  v_old_status text;
begin
  if v_actor is null or not private.is_admin_or_manager() then
    raise exception 'Admin or Manager access required' using errcode='42501';
  end if;
  if p_status not in ('in_stock','listed','reserved','repair','retired') then
    raise exception 'Use the sale workflow to mark an item sold' using errcode='22023';
  end if;
  select status into v_old_status from public.inventory_items where id=p_inventory_item_id for update;
  if v_old_status is null then raise exception 'Inventory item not found' using errcode='P0002'; end if;
  if v_old_status='sold' then raise exception 'Sold inventory cannot be reopened through status control' using errcode='22023'; end if;

  update public.inventory_items set
    status=p_status,
    listed_at=case when p_status='listed' then coalesce(listed_at,now()) else listed_at end,
    retired_at=case when p_status='retired' then now() else null end,
    updated_by=v_actor,
    updated_at=now()
  where id=p_inventory_item_id;

  insert into public.admin_audit_log(actor_user_id,action,target_type,target_id,details)
  values(v_actor,'inventory.status','inventory_item',p_inventory_item_id::text,
    jsonb_build_object('old_status',v_old_status,'new_status',p_status));
  return true;
end;
$$;

create or replace function public.admin_inventory_record_sale(
  p_inventory_item_id uuid,
  p_sold_price numeric,
  p_fees numeric default 0,
  p_other_costs numeric default 0,
  p_sales_channel text default null,
  p_notes text default null,
  p_sold_at timestamptz default now()
) returns uuid
language plpgsql
security definer
set search_path = public, private, pg_temp
as $$
declare
  v_actor uuid := auth.uid();
  v_item public.inventory_items%rowtype;
  v_sale_id uuid;
begin
  if v_actor is null or not private.is_admin_or_manager() then
    raise exception 'Admin or Manager access required' using errcode='42501';
  end if;
  if p_sold_price is null or p_sold_price < 0 or coalesce(p_fees,0) < 0 or coalesce(p_other_costs,0) < 0 then
    raise exception 'Sale amounts must be zero or greater' using errcode='22023';
  end if;

  select * into v_item from public.inventory_items where id=p_inventory_item_id for update;
  if not found then raise exception 'Inventory item not found' using errcode='P0002'; end if;
  if v_item.status in ('sold','retired') then raise exception 'Inventory item is not available for sale' using errcode='22023'; end if;

  insert into public.sales_records(
    inventory_item_id,valuation_id,acquired_cost,sold_price,fees,other_costs,sales_channel,sold_at,source,notes,created_by
  ) values (
    v_item.id,v_item.valuation_id,v_item.acquired_price,p_sold_price,coalesce(p_fees,0),coalesce(p_other_costs,0),
    nullif(trim(coalesce(p_sales_channel,'')),''),coalesce(p_sold_at,now()),'inventory',nullif(trim(coalesce(p_notes,'')),''),v_actor
  ) returning id into v_sale_id;

  update public.inventory_items set status='sold',updated_by=v_actor,updated_at=now() where id=v_item.id;

  if v_item.valuation_id is not null then
    update public.valuation_history set
      status='sold',
      bought_price=coalesce(bought_price,v_item.acquired_price),
      sold_price=p_sold_price,
      updated_at=now()
    where id=v_item.valuation_id;
  end if;

  insert into public.admin_audit_log(actor_user_id,action,target_type,target_id,details)
  values(v_actor,'inventory.sale','sales_record',v_sale_id::text,
    jsonb_build_object('inventory_item_id',v_item.id,'sold_price',p_sold_price,'fees',coalesce(p_fees,0),'other_costs',coalesce(p_other_costs,0),'sales_channel',p_sales_channel));
  return v_sale_id;
end;
$$;

revoke all on function public.admin_inventory_create(text,text,numeric,numeric,bigint,uuid,text,text,text,text) from public, anon;
revoke all on function public.admin_inventory_set_status(uuid,text) from public, anon;
revoke all on function public.admin_inventory_record_sale(uuid,numeric,numeric,numeric,text,text,timestamptz) from public, anon;
grant execute on function public.admin_inventory_create(text,text,numeric,numeric,bigint,uuid,text,text,text,text) to authenticated;
grant execute on function public.admin_inventory_set_status(uuid,text) to authenticated;
grant execute on function public.admin_inventory_record_sale(uuid,numeric,numeric,numeric,text,text,timestamptz) to authenticated;

comment on function public.admin_inventory_create is 'Explicit Admin/Manager inventory creation. Audited and not callable by Nova learning.';
comment on function public.admin_inventory_set_status is 'Explicit Admin/Manager inventory status transition except sold. Audited.';
comment on function public.admin_inventory_record_sale is 'Atomic Admin/Manager sale recording with inventory + valuation reconciliation and audit.';
