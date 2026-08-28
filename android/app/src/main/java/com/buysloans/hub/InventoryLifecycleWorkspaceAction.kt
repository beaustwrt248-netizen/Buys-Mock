package com.buysloans.hub

/**
 * Lightweight presentation model for device-testing/inventory workspace lifecycle controls.
 *
 * Transition authority remains in [canTransitionLifecycle] via [InventoryLifecycleActionPolicy].
 * This model only exposes safe actionable destinations and whether the UI must collect a reason.
 */
data class InventoryLifecycleWorkspaceAction(
    val destination: InventoryLifecycle,
    val label: String,
    val requiresReason: Boolean
)

object InventoryLifecycleWorkspaceActions {
    fun forState(from: InventoryLifecycle): List<InventoryLifecycleWorkspaceAction> =
        InventoryLifecycleActionPolicy.nextStates(from).map { destination ->
            InventoryLifecycleWorkspaceAction(
                destination = destination,
                label = destination.displayLabel(),
                requiresReason = InventoryLifecycleActionPolicy.requiresReason(destination)
            )
        }

    fun isTerminal(state: InventoryLifecycle): Boolean = forState(state).isEmpty()

    private fun InventoryLifecycle.displayLabel(): String = when (this) {
        InventoryLifecycle.PURCHASED -> "Purchased"
        InventoryLifecycle.TESTING -> "Testing"
        InventoryLifecycle.READY_FOR_SALE -> "Ready for Sale"
        InventoryLifecycle.LISTED -> "Listed"
        InventoryLifecycle.SOLD -> "Sold"
        InventoryLifecycle.RETURNED_REPAIR -> "Returned/Repair"
    }
}
