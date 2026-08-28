package com.buysloans.admin

internal data class ProtectedConversationState(
    val ticketSubject: String,
    val messages: List<SupportMessageViewItem>
)

internal object ProtectedMessageCoordinator {
    suspend fun load(
        session: AdminSession,
        ticketId: String,
        ticketSubject: String,
        limit: Int = 100
    ): ProtectedConversationState {
        require(ticketId.isNotBlank()) { "A ticket id is required for protected-message access." }
        require(SupportMessageAccessPolicy.canReadProtectedMessages(session)) {
            "Protected support messages require an authenticated Admin or Manager session."
        }

        val rows = AdminApi.loadSupportMessages(
            session = session,
            ticketId = ticketId,
            limit = limit.coerceIn(1, 100)
        )

        return ProtectedConversationState(
            ticketSubject = ticketSubject.trim().take(160),
            messages = supportMessageViewItems(rows)
        )
    }
}
