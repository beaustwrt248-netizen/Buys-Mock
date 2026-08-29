package com.buysloans.admin

import android.app.Application

class AdminApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AdminCopyrightFooter.install(this)
    }
}
