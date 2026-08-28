package com.buysloans.hub

/**
 * Sale-boundary policy for inventory items.
 *
 * A sale is only permitted from Listed so callers cannot bypass the lifecycle path
 * Purchased -> Testing -> Ready for Sale -> Listed -> Sold.
 */
object InventorySalePolicy {
    fun canSell(lifecycle: InventoryLifecycle): Boolean =
        lifecycle == InventoryLifecycle.LISTED &&
            canTransitionLifecycle(lifecycle, InventoryLifecycle.SOLD)

    fun requireSellable(lifecycle: InventoryLifecycle) {
        require(canSell(lifecycle)) {
            "Inventory must be Listed before it can be sold. Current state: ${lifecycle.label}."
        }
    }
}
