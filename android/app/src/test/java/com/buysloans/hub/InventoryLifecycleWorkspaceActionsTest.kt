package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InventoryLifecycleWorkspaceActionsTest {
    @Test
    fun `workspace actions exactly mirror authoritative action policy`() {
        InventoryLifecycle.entries.forEach { from ->
            assertEquals(
                InventoryLifecycleActionPolicy.nextStates(from),
                InventoryLifecycleWorkspaceActions.forState(from).map { it.destination }
            )
        }
    }

    @Test
    fun `returned repair is the only action that requires a reason`() {
        InventoryLifecycle.entries.forEach { from ->
            InventoryLifecycleWorkspaceActions.forState(from).forEach { action ->
                assertEquals(
                    action.destination == InventoryLifecycle.RETURNED_REPAIR,
                    action.requiresReason
                )
            }
        }
    }

    @Test
    fun `sold remains terminal in workspace controls`() {
        assertTrue(InventoryLifecycleWorkspaceActions.forState(InventoryLifecycle.SOLD).isEmpty())
        assertTrue(InventoryLifecycleWorkspaceActions.isTerminal(InventoryLifecycle.SOLD))
    }

    @Test
    fun `non terminal lifecycle states expose safe actions`() {
        listOf(
            InventoryLifecycle.PURCHASED,
            InventoryLifecycle.TESTING,
            InventoryLifecycle.READY_FOR_SALE,
            InventoryLifecycle.LISTED,
            InventoryLifecycle.RETURNED_REPAIR
        ).forEach { state ->
            assertFalse(InventoryLifecycleWorkspaceActions.forState(state).isEmpty())
            assertFalse(InventoryLifecycleWorkspaceActions.isTerminal(state))
        }
    }

    @Test
    fun `workspace labels are explicit and stable`() {
        val labels = InventoryLifecycle.entries.associateWith { state ->
            when (state) {
                InventoryLifecycle.SOLD -> "Sold"
                else -> InventoryLifecycleWorkspaceActions.forState(state)
                    .firstOrNull { it.destination == InventoryLifecycle.SOLD }
                    ?.label
            }
        }

        assertEquals("Sold", labels[InventoryLifecycle.SOLD])
        assertEquals(
            "Returned/Repair",
            InventoryLifecycleWorkspaceActions.forState(InventoryLifecycle.LISTED)
                .single { it.destination == InventoryLifecycle.RETURNED_REPAIR }
                .label
        )
        assertEquals(
            "Ready for Sale",
            InventoryLifecycleWorkspaceActions.forState(InventoryLifecycle.TESTING)
                .single { it.destination == InventoryLifecycle.READY_FOR_SALE }
                .label
        )
    }
}
