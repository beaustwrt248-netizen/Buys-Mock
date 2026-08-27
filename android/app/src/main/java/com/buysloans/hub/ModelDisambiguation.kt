package com.buysloans.hub

data class ModelCandidate(
    val id: String,
    val model: String,
    val year: Int? = null,
    val generation: String? = null,
    val aliases: Set<String> = emptySet()
)

data class ModelQuery(
    val text: String,
    val model: String? = null,
    val year: Int? = null,
    val generation: String? = null
)

enum class ModelResolutionStatus { RESOLVED, AMBIGUOUS, UNRESOLVED }

data class ModelResolution(
    val status: ModelResolutionStatus,
    val candidateId: String?,
    val confidence: Double,
    val reasons: List<String>
) {
    val requiresConfirmation: Boolean get() = status != ModelResolutionStatus.RESOLVED
}

object ModelDisambiguation {
    fun resolve(query: ModelQuery, candidates: List<ModelCandidate>): ModelResolution {
        if (candidates.isEmpty()) return ModelResolution(
            ModelResolutionStatus.UNRESOLVED, null, 0.0, listOf("No model candidates are available")
        )

        val scored = candidates.map { it to score(query, it) }.sortedByDescending { it.second }
        val best = scored.first()
        val next = scored.getOrNull(1)?.second ?: 0.0
        val status = when {
            best.second < 0.45 -> ModelResolutionStatus.UNRESOLVED
            best.second - next < 0.15 -> ModelResolutionStatus.AMBIGUOUS
            else -> ModelResolutionStatus.RESOLVED
        }
        val reasons = when (status) {
            ModelResolutionStatus.RESOLVED -> listOf("Model identity is uniquely supported by the supplied evidence")
            ModelResolutionStatus.AMBIGUOUS -> listOf("Multiple model, year, or generation candidates remain plausible")
            ModelResolutionStatus.UNRESOLVED -> listOf("Evidence is insufficient to resolve model identity")
        }
        return ModelResolution(status, best.first.id.takeIf { status != ModelResolutionStatus.UNRESOLVED }, best.second, reasons)
    }

    private fun score(query: ModelQuery, candidate: ModelCandidate): Double {
        val text = normalize(query.text)
        val requestedModel = query.model?.let(::normalize).orEmpty()
        val names = (candidate.aliases + candidate.model).map(::normalize)
        var score = 0.0
        if ((requestedModel.isNotBlank() && names.any { it == requestedModel }) || names.any { it.isNotBlank() && text.contains(it) }) score += 0.55
        if (query.year != null && candidate.year == query.year) score += 0.25
        else if (query.year == null && candidate.year != null && text.contains(candidate.year.toString())) score += 0.15
        if (!query.generation.isNullOrBlank() && !candidate.generation.isNullOrBlank() && normalize(query.generation) == normalize(candidate.generation)) score += 0.20
        else if (query.generation.isNullOrBlank() && !candidate.generation.isNullOrBlank() && text.contains(normalize(candidate.generation))) score += 0.15
        return score.coerceIn(0.0, 1.0)
    }

    private fun normalize(value: String): String = value.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
}
