package com.buysloans.admin

internal data class AdminUpdateRelease(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val required: Boolean = false
)

internal fun shouldOfferAdminUpdate(installedVersionCode: Int, release: AdminUpdateRelease): Boolean =
    release.versionCode > installedVersionCode &&
        release.apkUrl.startsWith("https://") &&
        release.sha256.matches(Regex("^[a-fA-F0-9]{64}$"))
