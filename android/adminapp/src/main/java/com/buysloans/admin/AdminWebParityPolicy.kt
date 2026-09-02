package com.buysloans.admin

import java.net.URI

object AdminWebParityPolicy {
    const val HOME_URL = "https://buyshub.me/admin/"
    private const val ADMIN_HOST = "buyshub.me"

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
