package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MainAppCrashTelemetryContractTest {
    private fun source(path: String): String = sequenceOf(
        File(path),
        File("../$path"),
        File("../../$path")
    ).firstOrNull { it.isFile }?.readText() ?: error("Missing source contract file: $path")

    @Test fun applicationInstallsAndFlushesCrashTelemetry() {
        val app = source("app/src/main/java/com/buysloans/hub/MorleyApplication.kt")
        assertTrue(app.contains("MainAppCrashTelemetry.install(this)"))
        assertTrue(app.contains("MainAppCrashTelemetry.flushPending(this@MorleyApplication)"))
    }

    @Test fun crashPayloadIsPrivacyMinimalAndUsesNarrowRpc() {
        val telemetry = source("app/src/main/java/com/buysloans/hub/MainAppCrashTelemetry.kt")
        assertTrue(telemetry.contains("/rest/v1/rpc/report_client_crash"))
        assertTrue(telemetry.contains("p_error_class"))
        assertTrue(telemetry.contains("p_failing_screen"))
        assertTrue(telemetry.contains("p_app_version"))
        assertTrue(telemetry.contains("p_device_model"))
        assertFalse(telemetry.contains("/rest/v1/admin_error_events"))
        assertFalse(telemetry.contains("stackTraceToString"))
        assertFalse(telemetry.contains("throwable.message"))
        assertFalse(telemetry.contains("user_id"))
    }

    @Test fun reportingRpcPreservesTableBoundaryAndValidatesCallerAndPayload() {
        val migration = source("../supabase/migrations/20260909122000_main_app_crash_telemetry_rpc.sql")
        assertTrue(migration.contains("security definer"))
        assertTrue(migration.contains("set search_path = ''"))
        assertTrue(migration.contains("v_uid uuid := auth.uid()"))
        assertTrue(migration.contains("p.is_enabled is true"))
        assertTrue(migration.contains("length(btrim(p_error_class)) not between 1 and 160"))
        assertTrue(migration.contains("p_occurred_at < now() - interval '30 days'"))
        assertTrue(migration.contains("p_occurred_at > now() + interval '5 minutes'"))
        assertTrue(migration.contains("revoke all on function public.report_client_crash"))
        assertTrue(migration.contains("grant execute on function public.report_client_crash"))
        assertFalse(migration.contains("create policy admin_error_events_authenticated_insert"))
    }
}
