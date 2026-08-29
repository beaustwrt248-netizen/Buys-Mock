package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportTicketLogicTest {
    @Test
    fun validDraftIsTrimmedAndNormalized() {
        val draft = SupportTicketLogic.validateDraft(" Pricing ", "  Wrong price  ", "  The valuation is too low.  ")
        assertEquals("pricing", draft.category)
        assertEquals("Wrong price", draft.subject)
        assertEquals("The valuation is too low.", draft.description)
    }

    @Test
    fun invalidCategoryIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            SupportTicketLogic.validateDraft("admin", "Bad result", "Something went wrong")
        }
    }

    @Test
    fun shortSubjectAndDescriptionAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            SupportTicketLogic.validateDraft("other", "No", "Valid description")
        }
        assertThrows(IllegalArgumentException::class.java) {
            SupportTicketLogic.validateDraft("other", "Valid subject", "bad")
        }
    }

    @Test
    fun replyIsTrimmedAndValidated() {
        assertEquals("Thanks for the update", SupportTicketLogic.validateReply("  Thanks for the update  "))
        assertThrows(IllegalArgumentException::class.java) { SupportTicketLogic.validateReply("   ") }
        assertThrows(IllegalArgumentException::class.java) { SupportTicketLogic.validateReply("x".repeat(5001)) }
    }

    @Test
    fun supportStatusesHaveUserFriendlyLabels() {
        assertEquals("Open", SupportTicketLogic.statusLabel("open"))
        assertEquals("In progress", SupportTicketLogic.statusLabel("in_progress"))
        assertEquals("Waiting on you", SupportTicketLogic.statusLabel("waiting_on_user"))
        assertEquals("Resolved", SupportTicketLogic.statusLabel("resolved"))
        assertEquals("Closed", SupportTicketLogic.statusLabel("closed"))
    }

    @Test
    fun unreadReplyOnlyClearsAfterLatestAdminMessageIsSeen() {
        assertFalse(SupportTicketLogic.hasUnreadSupportReply(null, null))
        assertTrue(SupportTicketLogic.hasUnreadSupportReply("admin-message-1", null))
        assertFalse(SupportTicketLogic.hasUnreadSupportReply("admin-message-1", "admin-message-1"))
        assertTrue(SupportTicketLogic.hasUnreadSupportReply("admin-message-2", "admin-message-1"))
    }

    @Test
    fun supportedAttachmentTypesAndLimitPass() {
        SupportTicketLogic.validateAttachment("image/jpeg", SupportTicketLogic.MAX_ATTACHMENT_BYTES)
        SupportTicketLogic.validateAttachment("application/pdf", 1024)
    }

    @Test
    fun unsupportedOrOversizedAttachmentIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            SupportTicketLogic.validateAttachment("text/plain", 1024)
        }
        assertThrows(IllegalArgumentException::class.java) {
            SupportTicketLogic.validateAttachment("image/png", SupportTicketLogic.MAX_ATTACHMENT_BYTES + 1)
        }
    }

    @Test
    fun unsafeAttachmentNameIsSanitized() {
        assertEquals("my-screen-shot.png", SupportTicketLogic.safeFileName(" my screen shot.png "))
        assertEquals("attachment", SupportTicketLogic.safeFileName("***"))
    }
}
