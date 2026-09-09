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

    @Test fun crashPayloadIsPrivacyMinimal() {
        val telemetry = source("app/src/main/java/com/buysloans/hub/MainAppCrashTelemetry.kt")
        assertTrue(telemetry.contains("admin_error_events"))
        assertTrue(telemetry.contains("error_class"))
        assertTrue(telemetry.contains("failing_screen"))
        assertTrue(telemetry.contains("app_version"))
        assertTrue(telemetry.contains("device_model"))
        assertFalse(telemetry.contains("stackTraceToString"))
        assertFalse(telemetry.contains("throwable.message"))
        assertFalse(telemetry.contains("user_id"))
    }

    @Test fun databasePolicyPreservesProtectedRoleBoundaryAndBoundsSubmissions() {
        val migration = source("../supabase/migrations/20260909121000_main_app_crash_telemetry.sql")
        assertTrue(migration.contains("to authenticated"))
        assertTrue(migration.contains("private.is_admin_or_manager()"))
        assertFalse(migration.contains("auth.uid() is not null"))
        assertTrue(migration.contains("length(error_class) between 1 and 160"))
        assertTrue(migration.contains("occurred_at >= now() - interval '30 days'"))
        assertTrue(migration.contains("occurred_at <= now() + interval '5 minutes'"))
        assertTrue(migration.contains("received_at >= now() - interval '5 minutes'"))
        assertTrue(migration.contains("received_at <= now() + interval '5 minutes'"))
    }
}
