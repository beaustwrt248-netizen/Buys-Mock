package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLifecycleActionPolicyTest {
    @Test
    fun `exposes only safe actionable next states`() {
        assertEquals(
            listOf(InventoryLifecycle.TESTING, InventoryLifecycle.RETURNED_REPAIR),
            InventoryLifecycleActionPolicy.nextStates(InventoryLifecycle.PURCHASED)
        )
        assertEquals(
            listOf(InventoryLifecycle.READY_FOR_SALE, InventoryLifecycle.RETURNED_REPAIR),
            InventoryLifecycleActionPolicy.nextStates(InventoryLifecycle.TESTING)
        )
        assertEquals(
            listOf(InventoryLifecycle.LISTED, InventoryLifecycle.RETURNED_REPAIR),
            InventoryLifecycleActionPolicy.nextStates(InventoryLifecycle.READY_FOR_SALE)
        )
        assertEquals(
            listOf(InventoryLifecycle.SOLD, InventoryLifecycle.RETURNED_REPAIR),
            InventoryLifecycleActionPolicy.nextStates(InventoryLifecycle.LISTED)
        )
        assertEquals(
            listOf(InventoryLifecycle.TESTING),
            InventoryLifecycleActionPolicy.nextStates(InventoryLifecycle.RETURNED_REPAIR)
        )
        assertEquals(emptyList<InventoryLifecycle>(), InventoryLifecycleActionPolicy.nextStates(InventoryLifecycle.SOLD))
    }

    @Test
    fun `does not expose same-state no-op or illegal jumps`() {
        InventoryLifecycle.entries.forEach { state ->
            assertFalse(InventoryLifecycleActionPolicy.nextStates(state).contains(state))
            assertFalse(InventoryLifecycleActionPolicy.canOffer(state, state))
        }
        assertFalse(InventoryLifecycleActionPolicy.canOffer(InventoryLifecycle.PURCHASED, InventoryLifecycle.LISTED))
        assertFalse(InventoryLifecycleActionPolicy.canOffer(InventoryLifecycle.TESTING, InventoryLifecycle.SOLD))
        assertFalse(InventoryLifecycleActionPolicy.canOffer(InventoryLifecycle.SOLD, InventoryLifecycle.TESTING))
        assertTrue(InventoryLifecycleActionPolicy.canOffer(InventoryLifecycle.RETURNED_REPAIR, InventoryLifecycle.TESTING))
    }

    @Test
    fun `returned repair action is the only action requiring a reason`() {
        InventoryLifecycle.entries.forEach { state ->
            assertEquals(
                state == InventoryLifecycle.RETURNED_REPAIR,
                InventoryLifecycleActionPolicy.requiresReason(state)
            )
        }
    }
}
