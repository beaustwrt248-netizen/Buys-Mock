-- Guardian approval/rejection decisions are first-class audit activity.
-- The decision RPC emits actor='approval', so keep the activity constraint aligned.

alter table public.guardian_activity
  drop constraint if exists guardian_activity_actor_check;

alter table public.guardian_activity
  add constraint guardian_activity_actor_check
  check (actor = any (array[
    'guardian'::text,
    'worker'::text,
    'system'::text,
    'admin'::text,
    'approval'::text
  ]));
