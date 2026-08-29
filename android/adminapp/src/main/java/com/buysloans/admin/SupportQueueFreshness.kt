package com.buysloans.admin

internal const val SUPPORT_QUEUE_STALE_AFTER_MS = 5L * 60L * 1000L

internal data class SupportQueueFreshness(
    val ageMinutes: Long,
    val stale: Boolean
)

internal fun supportQueueFreshness(
    refreshedAtMillis: Long,
    nowMillis: Long
): SupportQueueFreshness {
    val ageMillis = (nowMillis - refreshedAtMillis).coerceAtLeast(0L)
    return SupportQueueFreshness(
        ageMinutes = ageMillis / 60_000L,
        stale = ageMillis >= SUPPORT_QUEUE_STALE_AFTER_MS
    )
}
