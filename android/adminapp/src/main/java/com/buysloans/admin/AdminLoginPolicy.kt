package com.buysloans.admin

internal fun isAdminEmailValid(email: String): Boolean {
    val trimmed = email.trim()
    if (trimmed.isEmpty() || trimmed.any(Char::isWhitespace)) return false

    val at = trimmed.lastIndexOf('@')
    if (at <= 0 || at != trimmed.indexOf('@') || at == trimmed.lastIndex) return false

    val local = trimmed.substring(0, at)
    val domain = trimmed.substring(at + 1)
    if (local.isBlank() || domain.isBlank() || !domain.contains('.')) return false

    val labels = domain.split('.')
    if (labels.any { label ->
            label.isBlank() ||
                label.startsWith('-') ||
                label.endsWith('-') ||
                label.any { ch -> !ch.isLetterOrDigit() && ch != '-' }
        }
    ) return false

    return true
}

internal fun isAdminLoginReady(
    email: String,
    password: String,
    captchaToken: String,
    busy: Boolean
): Boolean = !busy &&
    isAdminEmailValid(email) &&
    password.isNotEmpty() &&
    captchaToken.isNotBlank()
