package com.buysloans.hub

data class DeviceTestChecklistResult(
    val checklistItemId: String,
    val result: TestResult,
    val notes: String = ""
)

data class DeviceTestChecklistReview(
    val category: DeviceTestCategory,
    val totalChecks: Int,
    val passedChecks: Int,
    val failedChecks: Int,
    val notApplicableChecks: Int,
    val untestedChecks: Int,
    val faults: List<String>,
    val isComplete: Boolean
)

/**
 * Reviews recorded Test & Buy results against the structured checklist catalog.
 *
 * This only summarises tester-entered or already system-reported evidence. It performs no active
 * hardware probing and does not claim that a device is fault-free when a check was not performed.
 */
object DeviceTestChecklistReviewer {
    fun review(
        category: DeviceTestCategory,
        results: List<DeviceTestChecklistResult>
    ): DeviceTestChecklistReview {
        val catalog = DeviceTestChecklistCatalog.forCategory(category)
        val catalogIds = catalog.map { it.id }.toSet()
        val duplicatedIds = results.groupingBy { it.checklistItemId }.eachCount().filterValues { it > 1 }.keys
        require(duplicatedIds.isEmpty()) { "Duplicate checklist results: ${duplicatedIds.sorted().joinToString()}" }

        val unknownIds = results.map { it.checklistItemId }.filterNot(catalogIds::contains).distinct()
        require(unknownIds.isEmpty()) { "Unknown checklist results: ${unknownIds.sorted().joinToString()}" }

        val resultById = results.associateBy { it.checklistItemId }
        val normalized = catalog.map { item ->
            item to (resultById[item.id] ?: DeviceTestChecklistResult(item.id, TestResult.NOT_TESTED))
        }

        val faults = normalized.mapNotNull { (item, recorded) ->
            if (recorded.result != TestResult.FAIL) return@mapNotNull null
            val note = recorded.notes.trim()
            if (note.isBlank()) item.label else "${item.label}: $note"
        }

        val passed = normalized.count { it.second.result == TestResult.PASS }
        val failed = normalized.count { it.second.result == TestResult.FAIL }
        val notApplicable = normalized.count { it.second.result == TestResult.NOT_APPLICABLE }
        val untested = normalized.count { it.second.result == TestResult.NOT_TESTED }

        return DeviceTestChecklistReview(
            category = category,
            totalChecks = normalized.size,
            passedChecks = passed,
            failedChecks = failed,
            notApplicableChecks = notApplicable,
            untestedChecks = untested,
            faults = faults,
            isComplete = untested == 0
        )
    }
}
