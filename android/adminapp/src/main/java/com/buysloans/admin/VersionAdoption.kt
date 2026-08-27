package com.buysloans.admin

data class VersionAdoptionSummary(
    val current: Int,
    val outdated: Int,
    val aheadOrTest: Int,
    val unknown: Int,
)

fun summarizeVersionAdoption(appVersions: List<String?>, currentVersion: String?): VersionAdoptionSummary {
    val currentParts = parseVersion(currentVersion)
    var current = 0
    var outdated = 0
    var aheadOrTest = 0
    var unknown = 0

    appVersions.forEach { raw ->
        val parts = parseVersion(raw)
        when {
            currentParts == null || parts == null -> unknown++
            parts == currentParts -> current++
            compareVersion(parts, currentParts) < 0 -> outdated++
            else -> aheadOrTest++
        }
    }

    return VersionAdoptionSummary(current, outdated, aheadOrTest, unknown)
}

private fun parseVersion(raw: String?): List<Int>? {
    val text = raw?.trim().orEmpty()
    if (text.isBlank()) return null
    val match = Regex("^(?:v)?(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?").find(text) ?: return null
    return listOf(
        match.groupValues[1].toIntOrNull() ?: return null,
        match.groupValues[2].ifBlank { "0" }.toIntOrNull() ?: return null,
        match.groupValues[3].ifBlank { "0" }.toIntOrNull() ?: return null,
    )
}

private fun compareVersion(a: List<Int>, b: List<Int>): Int {
    for (i in 0..2) {
        val cmp = a[i].compareTo(b[i])
        if (cmp != 0) return cmp
    }
    return 0
}
