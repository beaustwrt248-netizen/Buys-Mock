package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
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
