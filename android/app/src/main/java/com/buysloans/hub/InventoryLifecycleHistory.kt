package com.buysloans.hub

import java.time.Instant

data class InventoryLifecycleEvent(
    val inventoryId: String,
    val from: InventoryLifecycle,
    val to: InventoryLifecycle,
    val reason: String,
    val occurredAt: String
)

object InventoryLifecycleHistory {
    fun transition(
        inventoryId: String,
        from: InventoryLifecycle,
        to: InventoryLifecycle,
        reason: String = "",
        occurredAt: String = Instant.now().toString()
    ): InventoryLifecycleEvent {
        require(inventoryId.isNotBlank()) { "Inventory id is required." }
        requireLifecycleTransition(from, to)
        require(runCatching { Instant.parse(occurredAt) }.isSuccess) { "A valid lifecycle timestamp is required." }
        val cleanReason = reason.trim()
        if (to == InventoryLifecycle.RETURNED_REPAIR) require(cleanReason.isNotBlank()) { "Returned/Repair transitions require a reason." }
        return InventoryLifecycleEvent(inventoryId.trim(), from, to, cleanReason, occurredAt)
    }

    fun currentState(initial: InventoryLifecycle, events: List<InventoryLifecycleEvent>): InventoryLifecycle {
        var state = initial
        var inventoryId: String? = null
        var previousTimestamp: Instant? = null

        events.forEach { event ->
            val cleanInventoryId = event.inventoryId.trim()
            require(cleanInventoryId.isNotBlank()) { "Lifecycle history inventory id is required." }
            if (inventoryId == null) inventoryId = cleanInventoryId
            require(cleanInventoryId == inventoryId) { "Lifecycle history cannot mix inventory items." }

            val timestamp = runCatching { Instant.parse(event.occurredAt) }.getOrNull()
            require(timestamp != null) { "Lifecycle history contains an invalid timestamp." }
            require(previousTimestamp == null || !timestamp.isBefore(previousTimestamp)) {
                "Lifecycle history timestamps must be chronological."
            }

            require(event.from == state) { "Lifecycle history is not contiguous." }
            requireLifecycleTransition(event.from, event.to)
            state = event.to
            previousTimestamp = timestamp
        }
        return state
    }
}
