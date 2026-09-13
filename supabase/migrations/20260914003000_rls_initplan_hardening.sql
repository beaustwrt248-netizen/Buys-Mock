-- Avoid per-row auth.uid() re-evaluation in advisor-confirmed INSERT policies.
-- Authorization semantics are unchanged; only auth.uid() is evaluated once per statement.

alter policy "Morley staff append overrides"
  on public.staff_overrides
  with check (
    (private.is_admin_or_manager() or private.is_support_staff())
    and actor_user_id = (select auth.uid())
  );

alter policy "Morley staff create passports"
  on public.device_passports
  with check (
    (private.is_admin_or_manager() or private.is_support_staff())
    and created_by = (select auth.uid())
  );

alter policy "restore_points_admin_insert"
  on public.restore_points
  with check (
    private.is_admin_or_manager()
    and created_by = (select auth.uid())
  );

alter policy "restore_events_admin_insert"
  on public.restore_events
  with check (
    private.is_admin_or_manager()
    and created_by = (select auth.uid())
    and exists (
      select 1
      from public.restore_points rp
      where rp.id = restore_events.restore_point_id
    )
  );

alter policy "Morley staff create device assessments"
  on public.device_assessments
  with check (
    (private.is_admin_or_manager() or private.is_support_staff())
    and created_by = (select auth.uid())
  );

alter policy "Morley staff create assessment evidence"
  on public.assessment_evidence
  with check (
    (private.is_admin_or_manager() or private.is_support_staff())
    and created_by = (select auth.uid())
  );

alter policy "Morley staff create diagnostics"
  on public.diagnostic_results
  with check (
    (private.is_admin_or_manager() or private.is_support_staff())
    and created_by = (select auth.uid())
  );

alter policy "Morley staff create valuation quotes"
  on public.valuation_quotes
  with check (
    (private.is_admin_or_manager() or private.is_support_staff())
    and created_by = (select auth.uid())
  );
