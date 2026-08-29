package com.buysloans.admin

internal data class AdminUpdateRelease(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val sha256: String,
    val required: Boolean = false
)

private const val TRUSTED_ADMIN_RELEASE_PREFIX =
    "https://github.com/beaustwrt248-netizen/Buys-Mock/releases/download/admin-v"

internal fun shouldOfferAdminUpdate(installedVersionCode: Int, release: AdminUpdateRelease): Boolean =
    release.versionCode > installedVersionCode &&
        release.versionName.isNotBlank() &&
        release.apkUrl.startsWith(TRUSTED_ADMIN_RELEASE_PREFIX) &&
        release.apkUrl.endsWith(".apk") &&
        release.sha256.matches(Regex("^[a-fA-F0-9]{64}$"))
