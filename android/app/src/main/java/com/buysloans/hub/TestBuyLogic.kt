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

enum class TestBuyPricingGrade(val label:String, val targetGpPct:Double) {
    A("A", 30.0),
    B("B", 50.0),
    C("C", 70.0),
    LUXURY("Luxury", 30.0)
}

enum class TestBuyGuidanceState {
    COMPLETE_TEST_AND_PRICING,
    REJECT_ASK_ABOVE_MAX,
    REJECT_FAILED_CHECKS,
    READY_WITH_FAULTS,
    READY_CLEAN
}

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

/**
 * Test & Buy uses the same simple GP relationship already exposed by the Morley GP calculator:
 * max cost = expected sale value × (1 - target GP%). This is deliberately local to Test & Buy
 * and does not alter Valuation 3.0 or its evidence/decision algorithms.
 */
fun calculatedTestBuyMaxBuy(currentValuation:Double, grade:TestBuyPricingGrade):Double {
    if (!currentValuation.isFinite() || currentValuation <= 0.0) return 0.0
    return currentValuation * (1.0 - grade.targetGpPct / 100.0)
}

fun testBuyGuidanceState(draft:TestBuyDraft):TestBuyGuidanceState {
    if (draft.itemName.isBlank() || draft.hasUntestedChecks || draft.currentValuation <= 0.0 || draft.maxBuyPrice <= 0.0) {
        return TestBuyGuidanceState.COMPLETE_TEST_AND_PRICING
    }
    if (draft.askingPrice > draft.maxBuyPrice) return TestBuyGuidanceState.REJECT_ASK_ABOVE_MAX
    if (draft.failedChecks > 0) return TestBuyGuidanceState.REJECT_FAILED_CHECKS
    if (draft.faults.isNotBlank()) return TestBuyGuidanceState.READY_WITH_FAULTS
    return TestBuyGuidanceState.READY_CLEAN
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
