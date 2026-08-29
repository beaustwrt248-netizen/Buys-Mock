package com.buysloans.admin

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class SupportQueueFilterTest {
    @Test
    fun searchesOnlyWithinAlreadyAuthorisedTicketSet() {
        val tickets = JSONArray()
            .put(ticket("1", "Battery failure", "open", "high", "staff-1"))
            .put(ticket("2", "Screen issue", "resolved", "normal", ""))

        val result = filterSupportQueue(tickets, SupportQueueFilter(query = "battery"))

        assertEquals(listOf("1"), result.map { it.optString("id") })
    }

    @Test
    fun combinesStatusPriorityAndAssigneeFilters() {
        val tickets = JSONArray()
            .put(ticket("1", "One", "open", "high", "staff-1"))
            .put(ticket("2", "Two", "open", "high", "staff-2"))
            .put(ticket("3", "Three", "open", "normal", "staff-1"))

        val result = filterSupportQueue(
            tickets,
            SupportQueueFilter(status = "open", priority = "high", assignee = "staff-1")
        )

        assertEquals(listOf("1"), result.map { it.optString("id") })
    }

    @Test
    fun supportsExplicitUnassignedFilter() {
        val tickets = JSONArray()
            .put(ticket("1", "Assigned", "open", "high", "staff-1"))
            .put(ticket("2", "Unassigned", "open", "high", ""))

        val result = filterSupportQueue(
            tickets,
            SupportQueueFilter(assignee = SUPPORT_ASSIGNEE_UNASSIGNED)
        )

        assertEquals(listOf("2"), result.map { it.optString("id") })
    }

    @Test
    fun neverExpandsBeyondExistingFiftyTicketPresentationLimit() {
        val tickets = JSONArray()
        repeat(55) { index -> tickets.put(ticket(index.toString(), "Ticket $index", "open", "normal", "")) }

        assertEquals(50, filterSupportQueue(tickets, SupportQueueFilter()).size)
    }

    private fun ticket(id: String, subject: String, status: String, priority: String, assignedTo: String) = JSONObject()
        .put("id", id)
        .put("subject", subject)
        .put("description", "Description for $subject")
        .put("category", "app")
        .put("status", status)
        .put("priority", priority)
        .put("assigned_to", assignedTo)
}
