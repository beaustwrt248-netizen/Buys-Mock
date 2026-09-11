package com.buysloans.admin

import java.net.URI

object AdminWebParityPolicy {
    const val HOME_URL = "https://buyshub.me/admin/"
    private const val ADMIN_HOST = "buyshub.me"

    /**
     * The Admin app is a privileged WebView shell around the live Admin workspace.
     * Always identify the native build in the first navigation so Android WebView cannot
     * resurrect a stale cached /admin/ document after a web deployment.
     */
    fun freshHomeUrl(versionCode: Int): String = "$HOME_URL?adminApp=$versionCode"

    fun isTrustedAdminUrl(rawUrl: String): Boolean = runCatching {
        val uri = URI(rawUrl)
        val path = uri.path ?: return false
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(ADMIN_HOST, ignoreCase = true) &&
            (path == "/admin" || path.startsWith("/admin/"))
    }.getOrDefault(false)

    fun isExternallyRoutableScheme(scheme: String?): Boolean =
        scheme?.lowercase() in setOf("http", "https", "mailto", "tel", "sms", "smsto")
}
