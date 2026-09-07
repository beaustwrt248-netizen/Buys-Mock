create table if not exists public.role_capabilities (
  role text not null check (role in ('admin','manager','staff')),
  capability text not null,
  allowed boolean not null default false,
  description text,
  primary key(role,capability)
);

create table if not exists public.user_permission_overrides (
  user_id uuid not null references auth.users(id) on delete cascade,
  capability text not null,
  allowed boolean not null,
  reason text,
  updated_by uuid references auth.users(id),
  updated_at timestamptz not null default now(),
  primary key(user_id,capability)
);

alter table public.role_capabilities enable row level security;
alter table public.user_permission_overrides enable row level security;
revoke all on public.role_capabilities from anon;
revoke insert,update,delete on public.role_capabilities from authenticated;
grant select on public.role_capabilities to authenticated;
revoke all on public.user_permission_overrides from anon;
revoke insert,update,delete on public.user_permission_overrides from authenticated;
grant select on public.user_permission_overrides to authenticated;

drop policy if exists "Authenticated users read role capabilities" on public.role_capabilities;
create policy "Authenticated users read role capabilities" on public.role_capabilities for select to authenticated using (true);
drop policy if exists "Users read their own permission overrides" on public.user_permission_overrides;
create policy "Users read their own permission overrides" on public.user_permission_overrides for select to authenticated using (user_id=(select auth.uid()));

insert into public.role_capabilities(role,capability,allowed,description) values
('admin','pricing.use',true,'Use pricing tools'),('manager','pricing.use',true,'Use pricing tools'),('staff','pricing.use',true,'Use pricing tools'),
('admin','pricing.approve',true,'Approve protected pricing changes'),('manager','pricing.approve',true,'Approve protected pricing changes'),('staff','pricing.approve',false,'Approve protected pricing changes'),
('admin','catalogue.edit',true,'Edit catalogue data'),('manager','catalogue.edit',true,'Edit catalogue data'),('staff','catalogue.edit',false,'Edit catalogue data'),
('admin','inventory.edit',true,'Edit operational inventory'),('manager','inventory.edit',true,'Edit operational inventory'),('staff','inventory.edit',true,'Edit operational inventory'),
('admin','support.manage',true,'Manage support tickets'),('manager','support.manage',true,'Manage support tickets'),('staff','support.manage',true,'Manage support tickets'),
('admin','users.manage',true,'Manage staff accounts and permissions'),('manager','users.manage',false,'Manage staff accounts and permissions'),('staff','users.manage',false,'Manage staff accounts and permissions'),
('admin','releases.manage',true,'Manage releases and deployment approvals'),('manager','releases.manage',false,'Manage releases and deployment approvals'),('staff','releases.manage',false,'Manage releases and deployment approvals'),
('admin','recovery.use',true,'Use own recovery centre'),('manager','recovery.use',true,'Use own recovery centre'),('staff','recovery.use',true,'Use own recovery centre'),
('admin','recovery.global',true,'Run global disaster-recovery backup'),('manager','recovery.global',false,'Run global disaster-recovery backup'),('staff','recovery.global',false,'Run global disaster-recovery backup'),
('admin','sync.use',true,'Use cross-device sync'),('manager','sync.use',true,'Use cross-device sync'),('staff','sync.use',true,'Use cross-device sync'),
('admin','nova.use',true,'Use Nova intelligence'),('manager','nova.use',true,'Use Nova intelligence'),('staff','nova.use',true,'Use Nova intelligence'),
('admin','guardian.review',true,'Review Guardian findings'),('manager','guardian.review',true,'Review Guardian findings'),('staff','guardian.review',false,'Review Guardian findings'),
('admin','guardian.protected_repair',false,'Protected repair still requires explicit human approval'),('manager','guardian.protected_repair',false,'Protected repair still requires explicit human approval'),('staff','guardian.protected_repair',false,'Protected repair still requires explicit human approval')
on conflict(role,capability) do update set allowed=excluded.allowed,description=excluded.description;

comment on table public.role_capabilities is 'Default capability matrix for admin/manager/staff. Protected Guardian repair remains separately human-gated.';
comment on table public.user_permission_overrides is 'Per-user capability overrides; clients can only read their own rows and cannot mutate permissions.';
