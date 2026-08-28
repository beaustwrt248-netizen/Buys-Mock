package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLifecycleRegressionMatrixTest {
    private val actionable = mapOf(
        InventoryLifecycle.PURCHASED to setOf(InventoryLifecycle.TESTING, InventoryLifecycle.RETURNED_REPAIR),
        InventoryLifecycle.TESTING to setOf(InventoryLifecycle.READY_FOR_SALE, InventoryLifecycle.RETURNED_REPAIR),
        InventoryLifecycle.READY_FOR_SALE to setOf(InventoryLifecycle.LISTED, InventoryLifecycle.RETURNED_REPAIR),
        InventoryLifecycle.LISTED to setOf(InventoryLifecycle.SOLD, InventoryLifecycle.RETURNED_REPAIR),
        InventoryLifecycle.RETURNED_REPAIR to setOf(InventoryLifecycle.TESTING),
        InventoryLifecycle.SOLD to emptySet()
    )

    @Test
    fun `complete lifecycle matrix allows only documented transitions and same-state persistence`() {
        InventoryLifecycle.entries.forEach { from ->
            InventoryLifecycle.entries.forEach { to ->
                val expected = from == to || actionable.getValue(from).contains(to)
                assertEquals("Unexpected lifecycle rule for ${from.name} -> ${to.name}", expected, canTransitionLifecycle(from, to))
            }
        }
    }

    @Test
    fun `normal sale path is ordered and cannot skip testing listing or sale prerequisites`() {
        val path = listOf(
            InventoryLifecycle.PURCHASED,
            InventoryLifecycle.TESTING,
            InventoryLifecycle.READY_FOR_SALE,
            InventoryLifecycle.LISTED,
            InventoryLifecycle.SOLD
        )
        path.zipWithNext().forEach { (from, to) ->
            assertTrue(canTransitionLifecycle(from, to))
            assertTrue(InventoryLifecycleActionPolicy.canOffer(from, to))
        }
        assertFalse(canTransitionLifecycle(InventoryLifecycle.PURCHASED, InventoryLifecycle.READY_FOR_SALE))
        assertFalse(canTransitionLifecycle(InventoryLifecycle.PURCHASED, InventoryLifecycle.LISTED))
        assertFalse(canTransitionLifecycle(InventoryLifecycle.TESTING, InventoryLifecycle.LISTED))
        assertFalse(canTransitionLifecycle(InventoryLifecycle.READY_FOR_SALE, InventoryLifecycle.SOLD))
    }

    @Test
    fun `repair route returns through testing and sold is terminal`() {
        listOf(
            InventoryLifecycle.PURCHASED,
            InventoryLifecycle.TESTING,
            InventoryLifecycle.READY_FOR_SALE,
            InventoryLifecycle.LISTED
        ).forEach { from ->
            assertTrue(canTransitionLifecycle(from, InventoryLifecycle.RETURNED_REPAIR))
        }
        assertTrue(canTransitionLifecycle(InventoryLifecycle.RETURNED_REPAIR, InventoryLifecycle.TESTING))
        InventoryLifecycle.entries.filter { it != InventoryLifecycle.SOLD }.forEach { to ->
            assertFalse(canTransitionLifecycle(InventoryLifecycle.SOLD, to))
        }
        assertTrue(canTransitionLifecycle(InventoryLifecycle.SOLD, InventoryLifecycle.SOLD))
        assertTrue(InventoryLifecycleActionPolicy.nextStates(InventoryLifecycle.SOLD).isEmpty())
    }

    @Test
    fun `action policy exactly matches actionable transition rules`() {
        InventoryLifecycle.entries.forEach { from ->
            assertEquals(actionable.getValue(from).toList(), InventoryLifecycleActionPolicy.nextStates(from))
        }
    }
}
