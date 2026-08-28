package com.buysloans.hub

/**
 * Presentation/action policy for inventory lifecycle controls.
 *
 * The authoritative transition rules remain in [canTransitionLifecycle]. This policy only
 * exposes actionable next states, intentionally excluding same-state no-op transitions.
 */
object InventoryLifecycleActionPolicy {
    fun nextStates(from: InventoryLifecycle): List<InventoryLifecycle> =
        InventoryLifecycle.entries.filter { to ->
            to != from && canTransitionLifecycle(from, to)
        }

    fun canOffer(from: InventoryLifecycle, to: InventoryLifecycle): Boolean =
        to != from && canTransitionLifecycle(from, to)

    fun requiresReason(to: InventoryLifecycle): Boolean =
        to == InventoryLifecycle.RETURNED_REPAIR
}
