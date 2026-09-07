package com.buysloans.hub

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DriveRecoveryTimestampSourceContractTest {
    @Test
    fun backupTimestampsUseDeviceLocalTimeFormatting() {
        val source = sequenceOf(
            File("app/src/main/java/com/buysloans/hub/DriveBackupClient.kt"),
            File("android/app/src/main/java/com/buysloans/hub/DriveBackupClient.kt")
        ).firstOrNull { it.exists() } ?: error("DriveBackupClient.kt not found")

        val text = source.readText()
        assertTrue(text.contains("OffsetDateTime.parse(normalized)"))
        assertTrue(text.contains("ZoneId.systemDefault()"))
        assertTrue(text.contains("FormatStyle.MEDIUM"))
        assertTrue(text.contains("FormatStyle.SHORT"))
        assertTrue(text.contains("localTimestamp(row.optString(\"created_at\"))"))
        assertTrue(text.contains("localTimestamp(health.optJSONObject(\"last_backup\")?.optString(\"created_at\").orEmpty())"))
    }
}
