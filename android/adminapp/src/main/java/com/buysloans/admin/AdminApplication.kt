package com.buysloans.admin

import android.app.Application

class AdminApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AdminTelemetry.installCrashHandler(this)
        AdminCopyrightFooter.install(this)
    }
}
