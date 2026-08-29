package com.buysloans.admin

internal fun isAdminEmailValid(email: String): Boolean = email.trim().isNotEmpty()

internal fun isAdminLoginReady(
    email: String,
    password: String,
    captchaToken: String,
    busy: Boolean
): Boolean = !busy &&
    isAdminEmailValid(email) &&
    password.isNotEmpty() &&
    captchaToken.isNotBlank()
