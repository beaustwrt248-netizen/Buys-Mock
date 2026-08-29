package com.buysloans.hub

enum class InventoryLifecycleState {
    PURCHASED,
    TESTING,
    READY_FOR_SALE,
    LISTED,
    SOLD,
    RETURNED_REPAIR
}

/**
 * Explicit inventory lifecycle policy for Test & Buy handoff and resale progression.
 *
 * Transitions are intentionally conservative: state changes must follow the normal testing and
 * resale path, while returns/repairs route back through testing before an item can be sold again.
 */
object InventoryLifecyclePolicy {
    private val allowedTransitions: Map<InventoryLifecycleState, Set<InventoryLifecycleState>> = mapOf(
        InventoryLifecycleState.PURCHASED to setOf(
            InventoryLifecycleState.TESTING,
            InventoryLifecycleState.RETURNED_REPAIR
        ),
        InventoryLifecycleState.TESTING to setOf(
            InventoryLifecycleState.READY_FOR_SALE,
            InventoryLifecycleState.RETURNED_REPAIR
        ),
        InventoryLifecycleState.READY_FOR_SALE to setOf(
            InventoryLifecycleState.LISTED,
            InventoryLifecycleState.TESTING,
            InventoryLifecycleState.RETURNED_REPAIR
        ),
        InventoryLifecycleState.LISTED to setOf(
            InventoryLifecycleState.SOLD,
            InventoryLifecycleState.READY_FOR_SALE,
            InventoryLifecycleState.RETURNED_REPAIR
        ),
        InventoryLifecycleState.SOLD to setOf(
            InventoryLifecycleState.RETURNED_REPAIR
        ),
        InventoryLifecycleState.RETURNED_REPAIR to setOf(
            InventoryLifecycleState.TESTING
        )
    )

    fun canTransition(
        from: InventoryLifecycleState,
        to: InventoryLifecycleState
    ): Boolean = to in allowedTransitions.getValue(from)

    fun requireTransition(
        from: InventoryLifecycleState,
        to: InventoryLifecycleState
    ) {
        require(canTransition(from, to)) {
            "Unsafe inventory lifecycle transition: $from -> $to"
        }
    }

    fun nextStates(from: InventoryLifecycleState): Set<InventoryLifecycleState> =
        allowedTransitions.getValue(from)
}
