package com.buysloans.hub

import android.content.Context
import java.time.Instant

data class TestBuyCompletionHistory(
    val source: DeviceTestHistorySource,
    val reference: String,
    val itemName: String,
    val category: String,
    val result: String,
    val summary: String,
    val recordedAt: Long
)

private fun testedChecksSummary(checks: List<HardwareCheck>): String {
    val tested = checks
        .filter { it.result != TestResult.NOT_TESTED }
        .joinToString(", ") { check -> "${check.label}=${check.result.name}" }
    return if (tested.isBlank()) "none" else tested
}

fun completionHistoryFor(session: TestBuySessionRecord): TestBuyCompletionHistory {
    val source = when (session.evidenceSource) {
        TestEvidenceSource.MANUAL_ENTRY -> DeviceTestHistorySource.TEST_BUY
        TestEvidenceSource.BARCODE -> DeviceTestHistorySource.BARCODE
        TestEvidenceSource.ANDROID_NFC_READ_ONLY -> DeviceTestHistorySource.NFC
    }
    val completedAtMillis = Instant.parse(session.completedAt).toEpochMilli()
    val faultsSummary = session.faults.ifBlank { "none recorded" }
    val testedChecks = testedChecksSummary(session.checks)
    return TestBuyCompletionHistory(
        source = source,
        reference = session.scanReference,
        itemName = session.itemName,
        category = session.category.name,
        result = session.outcome.name,
        summary = "${session.completedChecks}/${session.totalChecks} checks completed; ${session.failedChecks} failed; tested: $testedChecks; faults: $faultsSummary; explicit ${session.outcome.name} outcome.",
        recordedAt = completedAtMillis
    )
}

fun inventoryHandoffHistoryFor(session: TestBuySessionRecord): TestBuyCompletionHistory {
    require(session.outcome == BuyOutcome.SEND_TO_INVENTORY) { "Inventory handoff history requires Send to Inventory outcome." }
    return completionHistoryFor(session)
}

object TestBuyCompletionHistoryRecorder {
    private fun recordHistory(context: Context, history: TestBuyCompletionHistory): DeviceTestHistoryEntry =
        DeviceTestHistoryStore.record(
            context = context,
            source = history.source,
            reference = history.reference,
            itemName = history.itemName,
            category = history.category,
            result = history.result,
            summary = history.summary,
            recordedAt = history.recordedAt
        )

    fun record(context: Context, session: TestBuySessionRecord): DeviceTestHistoryEntry =
        recordHistory(context, completionHistoryFor(session))

    fun recordInventoryHandoff(context: Context, session: TestBuySessionRecord): DeviceTestHistoryEntry =
        recordHistory(context, inventoryHandoffHistoryFor(session))
}
