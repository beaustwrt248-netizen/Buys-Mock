package com.buysloans.admin

private val ADMIN_EMAIL_PATTERN = Regex(
    "^[A-Za-z0-9.!#$%&'*+/=?^_`{|}~-]+@[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"
)

internal fun isAdminEmailValid(email: String): Boolean = ADMIN_EMAIL_PATTERN.matches(email.trim())

internal fun isAdminLoginReady(
    email: String,
    password: String,
    captchaToken: String,
    busy: Boolean
): Boolean = !busy &&
    isAdminEmailValid(email) &&
    password.isNotEmpty() &&
    captchaToken.isNotBlank()
