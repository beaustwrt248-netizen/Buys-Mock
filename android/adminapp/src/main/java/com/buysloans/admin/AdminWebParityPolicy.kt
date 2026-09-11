package com.buysloans.admin

import java.net.URI

object AdminWebParityPolicy {
    const val HOME_URL = "https://buyshub.me/admin/"
    private const val ADMIN_HOST = "buyshub.me"
    private const val NATIVE_SESSION_BOOTSTRAP_PATH = "/admin/native-session-bootstrap.html"

    /**
     * The Admin app is a privileged WebView shell around the live Admin workspace.
     * Always identify the native build in the first navigation so Android WebView cannot
     * resurrect a stale cached /admin/ document after a web deployment.
     */
    fun freshHomeUrl(versionCode: Int): String = "$HOME_URL?adminApp=$versionCode"

    /**
     * Native authentication is installed into same-origin Supabase storage before the
     * privileged Admin workspace loads. This removes the race where /admin/ could render
     * its logged-out web view before the Android session was available.
     */
    fun nativeSessionBootstrapUrl(versionCode: Int): String =
        "https://$ADMIN_HOST$NATIVE_SESSION_BOOTSTRAP_PATH?adminApp=$versionCode"

    fun isNativeSessionBootstrapUrl(rawUrl: String): Boolean = runCatching {
        val uri = URI(rawUrl)
        uri.scheme.equals("https", ignoreCase = true) &&
            uri.host.equals(ADMIN_HOST, ignoreCase = true) &&
            uri.path == NATIVE_SESSION_BOOTSTRAP_PATH
    }.getOrDefault(false)

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
