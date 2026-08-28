package com.buysloans.admin

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SupportMessagePresentationTest {
    @Test
    fun `projects only display-safe protected message fields`() {
        val messages = JSONArray(
            """[
              {
                "id":"message-1",
                "ticket_id":"ticket-secret",
                "author_user_id":"user-secret",
                "author_role":"Manager",
                "body":"  Private reply  ",
                "created_at":"2026-08-28T07:00:00Z"
              }
            ]"""
        )

        val items = supportMessageViewItems(messages)

        assertEquals(1, items.size)
        assertEquals("manager", items.single().authorRole)
        assertEquals("Private reply", items.single().body)
        assertEquals("2026-08-28T07:00:00Z", items.single().createdAt)
        assertFalse(items.single().toString().contains("ticket-secret"))
        assertFalse(items.single().toString().contains("user-secret"))
    }

    @Test
    fun `skips blank bodies and caps projection at one hundred messages`() {
        val messages = JSONArray()
        messages.put(org.json.JSONObject().put("body", "   "))
        repeat(105) { index ->
            messages.put(
                org.json.JSONObject()
                    .put("author_role", "staff")
                    .put("body", "message-$index")
                    .put("created_at", "2026-08-28T07:00:00Z")
            )
        }

        val items = supportMessageViewItems(messages)

        assertEquals(99, items.size)
        assertEquals("message-0", items.first().body)
        assertEquals("message-98", items.last().body)
    }
}
