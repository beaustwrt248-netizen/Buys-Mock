begin;

alter table public.device_assessments
  add column if not exists source text,
  add column if not exists checkpoint text,
  add column if not exists checkpoint_metadata jsonb not null default '{}'::jsonb,
  add column if not exists last_checkpoint_at timestamptz,
  add column if not exists last_error_code text,
  add column if not exists cancelled_at timestamptz,
  add column if not exists completed_at timestamptz;

alter table public.device_assessments
  drop constraint if exists device_assessments_checkpoint_check;

alter table public.device_assessments
  add constraint device_assessments_checkpoint_check check (
    checkpoint is null or checkpoint in (
      'capture_started',
      'front_captured',
      'rear_captured',
      'analysis_started',
      'analysis_failed',
      'review_ready',
      'review_completed',
      'pricing_started',
      'pricing_ready',
      'repair_decision_ready',
      'staff_confirmed',
      'stock_prepared',
      'cancelled',
      'completed'
    )
  );

create index if not exists device_assessments_source_checkpoint_idx
  on public.device_assessments (source, checkpoint, created_at desc);

create index if not exists device_assessments_created_by_history_idx
  on public.device_assessments (created_by, created_at desc);

comment on column public.device_assessments.source is 'Assessment entry source such as device_lens; never contains raw device identifiers.';
comment on column public.device_assessments.checkpoint is 'Durable AI scan workflow checkpoint. Failed and cancelled sessions are intentionally retained.';
comment on column public.device_assessments.checkpoint_metadata is 'Sanitized workflow metadata only; raw IMEI/serial and secrets are forbidden by application contracts.';
comment on column public.device_assessments.last_error_code is 'Sanitized recoverable error code for scan-history retry, not provider secrets or image data.';

commit;
