package com.buysloans.hub

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DriveRecoverySourceContractTest {
    private fun source(path: String): String = sequenceOf(
        File(path),
        File("../$path"),
        File("../../$path")
    ).firstOrNull { it.isFile }?.readText() ?: error("Missing source contract file: $path")

    @Test fun encryptedDriveRecoveryOwnsAndroidBackupRoute() {
        val app = source("app/src/main/java/com/buysloans/hub/MorleyApplication.kt")
        val recovery = source("app/src/main/java/com/buysloans/hub/DriveRecoveryActivity.kt")
        assertTrue(app.contains("EXTRA_FEATURE)==\"backup\""))
        assertTrue(app.contains("DriveRecoveryActivity::class.java"))
        assertTrue(recovery.contains("Encrypted Google Drive backup"))
        assertTrue(recovery.contains("Connect Google Drive"))
        assertTrue(recovery.contains("Back Up Now"))
        assertTrue(recovery.contains("Backup history"))
    }

    @Test fun backupClientNeverSerializesMorleyAuthenticationState() {
        val client = source("app/src/main/java/com/buysloans/hub/DriveBackupClient.kt")
        assertTrue(client.contains("WorkspaceStore.exportJson"))
        assertTrue(client.contains("X-Google-Access-Token"))
        assertTrue(client.contains("AuthManager.validAccessToken"))
        assertFalse(client.contains("refresh_token"))
        assertFalse(client.contains("morley_auth"))
        assertFalse(client.contains("SUPABASE_SERVICE_ROLE"))
    }

    @Test fun restoreRequiresServerConfirmationAndSafetyState() {
        val client = source("app/src/main/java/com/buysloans/hub/DriveBackupClient.kt")
        assertTrue(client.contains("preview_restore"))
        assertTrue(client.contains("put(\"confirm\", \"RESTORE\")"))
        assertTrue(client.contains("current_client_state"))
        assertTrue(client.contains("WorkspaceStore.importJson"))
    }
}
