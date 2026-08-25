package com.buysloans.hub

import android.content.Context

object AppRuntime {
    /**
     * Always resolve the application context from MorleyApplication instead of
     * relying on an Activity to initialise a lateinit property. This keeps
     * pricing/auth requests safe even after Android recreates an Activity or
     * restores the process directly into a screen.
     */
    val context: Context
        get() = MorleyApplication.instance.applicationContext
}
