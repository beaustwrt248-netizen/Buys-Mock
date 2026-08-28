package com.buysloans.admin

import org.json.JSONArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportMessagePresentationTest {
    @Test
    fun `presentation exposes role body and timestamp without user identifiers`() {
        val source = JSONArray("""[
            {"id":"m1","ticket_id":"t1","author_user_id":"secret-user","author_role":"manager","body":"  Please send a screenshot.  ","created_at":"2026-08-28T05:00:00Z"}
        ]""")

        val presented = SupportMessagePresentation.present(source)

        assertEquals(1, presented.size)
        assertEquals("Manager", presented.single().roleLabel)
        assertEquals("Please send a screenshot.", presented.single().body)
        assertEquals("2026-08-28T05:00:00Z", presented.single().createdAt)
        assertFalse(presented.single().toString().contains("secret-user"))
        assertFalse(presented.single().toString().contains("t1"))
        assertFalse(presented.single().toString().contains("m1"))
    }

    @Test
    fun `blank messages are omitted and unknown roles are neutral`() {
        val source = JSONArray("""[
            {"author_role":"root","body":"Visible response","created_at":"now"},
            {"author_role":"staff","body":"   ","created_at":"later"}
        ]""")

        val presented = SupportMessagePresentation.present(source)

        assertEquals(1, presented.size)
        assertEquals("Participant", presented.single().roleLabel)
    }

    @Test
    fun `message body and row count are bounded for admin display`() {
        val source = JSONArray()
        repeat(105) { index ->
            source.put(org.json.JSONObject().put("author_role", "user").put("body", "x".repeat(5000)).put("created_at", index.toString()))
        }

        val presented = SupportMessagePresentation.present(source)

        assertEquals(100, presented.size)
        assertTrue(presented.all { it.body.length == 4000 })
        assertEquals("User", presented.first().roleLabel)
    }
}
