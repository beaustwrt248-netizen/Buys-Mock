package com.buysloans.hub

import android.app.Application

/**
 * Copyright information belongs in the About/Legal surfaces instead of as a
 * global overlay. Keeping this installer as a no-op preserves the application
 * bootstrap contract while preventing the footer from sitting beneath Android
 * system navigation or covering app bottom navigation/content.
 */
object AppCopyrightFooter {
    fun install(application: Application) {
        // Intentionally no global overlay. Legal/About screens remain the
        // canonical location for copyright information.
        application.applicationContext
    }
}
