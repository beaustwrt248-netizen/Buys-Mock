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

fun completionHistoryFor(session: TestBuySessionRecord): TestBuyCompletionHistory {
    val source = when (session.evidenceSource) {
        TestEvidenceSource.MANUAL_ENTRY -> DeviceTestHistorySource.TEST_BUY
        TestEvidenceSource.BARCODE -> DeviceTestHistorySource.BARCODE
        TestEvidenceSource.ANDROID_NFC_READ_ONLY -> DeviceTestHistorySource.NFC
    }
    val completedAtMillis = Instant.parse(session.completedAt).toEpochMilli()
    val faultsSummary = session.faults.ifBlank { "none recorded" }
    return TestBuyCompletionHistory(
        source = source,
        reference = session.scanReference,
        itemName = session.itemName,
        category = session.category.name,
        result = session.outcome.name,
        summary = "${session.completedChecks}/${session.totalChecks} checks completed; ${session.failedChecks} failed; faults: $faultsSummary; explicit ${session.outcome.name} outcome.",
        recordedAt = completedAtMillis
    )
}

object TestBuyCompletionHistoryRecorder {
    fun record(context: Context, session: TestBuySessionRecord): DeviceTestHistoryEntry {
        val history = completionHistoryFor(session)
        return DeviceTestHistoryStore.record(
            context = context,
            source = history.source,
            reference = history.reference,
            itemName = history.itemName,
            category = history.category,
            result = history.result,
            summary = history.summary,
            recordedAt = history.recordedAt
        )
    }
}
