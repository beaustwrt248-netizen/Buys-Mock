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
    fun validateTransition(
        from: InventoryLifecycle,
        to: InventoryLifecycle,
        reason: String = ""
    ): String {
        requireLifecycleTransition(from, to)
        val cleanReason = reason.trim()
        if (InventoryLifecycleActionPolicy.requiresReason(to)) {
            require(cleanReason.isNotBlank()) { "Returned/Repair transitions require a reason." }
        }
        return cleanReason
    }

    fun transition(
        inventoryId: String,
        from: InventoryLifecycle,
        to: InventoryLifecycle,
        reason: String = "",
        occurredAt: String = Instant.now().toString()
    ): InventoryLifecycleEvent {
        require(inventoryId.isNotBlank()) { "Inventory id is required." }
        val cleanReason = validateTransition(from, to, reason)
        require(runCatching { Instant.parse(occurredAt) }.isSuccess) { "A valid lifecycle timestamp is required." }
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
            validateTransition(event.from, event.to, event.reason)
            state = event.to
            previousTimestamp = timestamp
        }
        return state
    }
}
