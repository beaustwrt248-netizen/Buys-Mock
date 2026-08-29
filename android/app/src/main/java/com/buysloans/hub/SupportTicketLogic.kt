package com.buysloans.hub

internal data class SupportTicketDraft(
    val category: String,
    val subject: String,
    val description: String
)

internal object SupportTicketLogic {
    const val MAX_ATTACHMENT_BYTES = 10L * 1024L * 1024L
    val allowedTypes = setOf("image/jpeg", "image/png", "image/webp", "application/pdf")
    val allowedCategories = setOf("valuation", "pricing", "inventory", "scanner", "account", "update", "other")

    fun validateDraft(category: String, subject: String, description: String): SupportTicketDraft {
        val cleanCategory = category.trim().lowercase()
        val cleanSubject = subject.trim()
        val cleanDescription = description.trim()
        require(cleanCategory in allowedCategories) { "Choose a valid support category." }
        require(cleanSubject.length in 3..160) { "Subject must be between 3 and 160 characters." }
        require(cleanDescription.length in 5..5000) { "Description must be between 5 and 5000 characters." }
        return SupportTicketDraft(cleanCategory, cleanSubject, cleanDescription)
    }

    fun validateReply(body: String): String {
        val cleanBody = body.trim()
        require(cleanBody.length in 1..5000) { "Reply must be between 1 and 5000 characters." }
        return cleanBody
    }

    fun statusLabel(status: String): String = when (status.trim().lowercase()) {
        "open" -> "Open"
        "in_progress" -> "In progress"
        "waiting_on_user" -> "Waiting on you"
        "resolved" -> "Resolved"
        "closed" -> "Closed"
        else -> "Support"
    }

    fun hasUnreadSupportReply(latestAdminMessageId: String?, lastSeenAdminMessageId: String?): Boolean {
        val latest = latestAdminMessageId.orEmpty().trim()
        if (latest.isBlank()) return false
        return latest != lastSeenAdminMessageId.orEmpty().trim()
    }

    fun validateAttachment(contentType: String?, byteSize: Long?) {
        val mime = contentType.orEmpty().trim().lowercase()
        require(mime in allowedTypes) { "Attachment must be a JPG, PNG, WebP or PDF." }
        if (byteSize != null && byteSize >= 0) {
            require(byteSize <= MAX_ATTACHMENT_BYTES) { "Attachment must be 10 MB or smaller." }
        }
    }

    fun safeFileName(name: String): String = name
        .replace(Regex("[^A-Za-z0-9._-]+"), "-")
        .trim('-')
        .take(120)
        .ifBlank { "attachment" }
}
