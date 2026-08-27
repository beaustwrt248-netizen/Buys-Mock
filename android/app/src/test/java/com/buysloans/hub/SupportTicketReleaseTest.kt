package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportTicketReleaseTest {
    @Test
    fun productionSupportCategoriesRemainAccepted() {
        val expected = setOf("valuation", "pricing", "inventory", "scanner", "account", "update", "other")
        assertEquals(expected, SupportTicketLogic.allowedCategories)
    }

    @Test
    fun diagnosticsDoNotChangeTicketValidationContract() {
        val draft = SupportTicketLogic.validateDraft("valuation", "A1932 valuation issue", "Valuation result did not match the expected item.")
        assertEquals("valuation", draft.category)
        assertEquals("A1932 valuation issue", draft.subject)
    }

    @Test
    fun attachmentPolicyRemainsPrivateSupportSafe() {
        assertEquals(10L * 1024L * 1024L, SupportTicketLogic.MAX_ATTACHMENT_BYTES)
        assertTrue("application/pdf" in SupportTicketLogic.allowedTypes)
        assertTrue("image/jpeg" in SupportTicketLogic.allowedTypes)
    }
}
