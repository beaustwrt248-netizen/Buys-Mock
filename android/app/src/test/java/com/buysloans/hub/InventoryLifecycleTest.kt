package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLifecycleTest {
    @Test
    fun `normal purchase to sale path is allowed`() {
        assertTrue(InventoryLifecyclePolicy.canTransition(InventoryLifecycleState.PURCHASED, InventoryLifecycleState.TESTING))
        assertTrue(InventoryLifecyclePolicy.canTransition(InventoryLifecycleState.TESTING, InventoryLifecycleState.READY_FOR_SALE))
        assertTrue(InventoryLifecyclePolicy.canTransition(InventoryLifecycleState.READY_FOR_SALE, InventoryLifecycleState.LISTED))
        assertTrue(InventoryLifecyclePolicy.canTransition(InventoryLifecycleState.LISTED, InventoryLifecycleState.SOLD))
    }

    @Test
    fun `return and repair must route back through testing`() {
        assertTrue(InventoryLifecyclePolicy.canTransition(InventoryLifecycleState.SOLD, InventoryLifecycleState.RETURNED_REPAIR))
        assertTrue(InventoryLifecyclePolicy.canTransition(InventoryLifecycleState.RETURNED_REPAIR, InventoryLifecycleState.TESTING))
        assertFalse(InventoryLifecyclePolicy.canTransition(InventoryLifecycleState.RETURNED_REPAIR, InventoryLifecycleState.READY_FOR_SALE))
        assertFalse(InventoryLifecyclePolicy.canTransition(InventoryLifecycleState.RETURNED_REPAIR, InventoryLifecycleState.LISTED))
        assertFalse(InventoryLifecyclePolicy.canTransition(InventoryLifecycleState.RETURNED_REPAIR, InventoryLifecycleState.SOLD))
    }

    @Test
    fun `unsafe lifecycle jumps are rejected`() {
        assertFalse(InventoryLifecyclePolicy.canTransition(InventoryLifecycleState.PURCHASED, InventoryLifecycleState.SOLD))
        assertFalse(InventoryLifecyclePolicy.canTransition(InventoryLifecycleState.TESTING, InventoryLifecycleState.SOLD))
        assertFalse(InventoryLifecyclePolicy.canTransition(InventoryLifecycleState.READY_FOR_SALE, InventoryLifecycleState.SOLD))
        assertFalse(InventoryLifecyclePolicy.canTransition(InventoryLifecycleState.SOLD, InventoryLifecycleState.LISTED))
        assertFalse(InventoryLifecyclePolicy.canTransition(InventoryLifecycleState.LISTED, InventoryLifecycleState.TESTING))
    }

    @Test
    fun `same-state transitions are not treated as lifecycle changes`() {
        InventoryLifecycleState.entries.forEach { state ->
            assertFalse(InventoryLifecyclePolicy.canTransition(state, state))
        }
    }

    @Test
    fun `next states expose only policy-approved destinations`() {
        assertEquals(
            setOf(InventoryLifecycleState.TESTING, InventoryLifecycleState.RETURNED_REPAIR),
            InventoryLifecyclePolicy.nextStates(InventoryLifecycleState.PURCHASED)
        )
        assertEquals(
            setOf(InventoryLifecycleState.TESTING),
            InventoryLifecyclePolicy.nextStates(InventoryLifecycleState.RETURNED_REPAIR)
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `require transition rejects an unsafe jump`() {
        InventoryLifecyclePolicy.requireTransition(
            InventoryLifecycleState.PURCHASED,
            InventoryLifecycleState.LISTED
        )
    }
}
