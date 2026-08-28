package com.buysloans.hub

enum class DeviceCategory { LAPTOP, DESKTOP_PC, CONSOLE, PHONE, OTHER }

enum class TestResult { NOT_TESTED, PASS, FAIL, NOT_APPLICABLE }

enum class InventoryLifecycle(val label:String) {
    PURCHASED("Purchased"),
    TESTING("Testing"),
    READY_FOR_SALE("Ready for Sale"),
    LISTED("Listed"),
    SOLD("Sold"),
    RETURNED_REPAIR("Returned/Repair")
}

enum class BuyOutcome { REJECT, BUY, SEND_TO_INVENTORY }

data class HardwareCheck(
    val id:String,
    val label:String,
    val result:TestResult = TestResult.NOT_TESTED,
    val notes:String = ""
)

data class TestBuyDraft(
    val itemName:String,
    val scanValue:String = "",
    val category:DeviceCategory = DeviceCategory.OTHER,
    val askingPrice:Double = 0.0,
    val currentValuation:Double = 0.0,
    val maxBuyPrice:Double = 0.0,
    val faults:String = "",
    val checks:List<HardwareCheck> = checklistFor(DeviceCategory.OTHER)
) {
    val failedChecks:Int get() = checks.count { it.result == TestResult.FAIL }
    val completedChecks:Int get() = checks.count { it.result != TestResult.NOT_TESTED }
    val hasUntestedChecks:Boolean get() = checks.any { it.result == TestResult.NOT_TESTED }
}

fun checklistFor(category:DeviceCategory):List<HardwareCheck> =
    DeviceChecklistProfiles.forCategory(category).map { spec ->
        HardwareCheck(id = spec.id, label = spec.label)
    }

fun recommendedOutcome(draft:TestBuyDraft):BuyOutcome {
    if (draft.itemName.isBlank()) return BuyOutcome.REJECT
    if (draft.failedChecks > 0) return BuyOutcome.REJECT
    if (draft.hasUntestedChecks) return BuyOutcome.REJECT
    if (draft.maxBuyPrice <= 0.0) return BuyOutcome.REJECT
    if (draft.askingPrice > draft.maxBuyPrice) return BuyOutcome.REJECT
    return if (draft.faults.isBlank()) BuyOutcome.SEND_TO_INVENTORY else BuyOutcome.BUY
}

private val allowedLifecycleTransitions = mapOf(
    InventoryLifecycle.PURCHASED to setOf(InventoryLifecycle.TESTING, InventoryLifecycle.RETURNED_REPAIR),
    InventoryLifecycle.TESTING to setOf(InventoryLifecycle.READY_FOR_SALE, InventoryLifecycle.RETURNED_REPAIR),
    InventoryLifecycle.READY_FOR_SALE to setOf(InventoryLifecycle.LISTED, InventoryLifecycle.RETURNED_REPAIR),
    InventoryLifecycle.LISTED to setOf(InventoryLifecycle.SOLD, InventoryLifecycle.RETURNED_REPAIR),
    InventoryLifecycle.RETURNED_REPAIR to setOf(InventoryLifecycle.TESTING),
    InventoryLifecycle.SOLD to emptySet()
)

fun canTransitionLifecycle(from:InventoryLifecycle,to:InventoryLifecycle):Boolean =
    from == to || allowedLifecycleTransitions[from]?.contains(to) == true

fun requireLifecycleTransition(from:InventoryLifecycle,to:InventoryLifecycle) {
    require(canTransitionLifecycle(from,to)) { "Cannot move inventory from ${from.label} to ${to.label}." }
}
