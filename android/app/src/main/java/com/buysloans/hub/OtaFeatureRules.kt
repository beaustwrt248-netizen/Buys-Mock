package com.buysloans.hub

/** Pure OTA feature-flag rule kept Android-free so it can be unit tested on the JVM. */
internal fun otaEnabledValue(configured: Boolean?): Boolean = configured ?: true
