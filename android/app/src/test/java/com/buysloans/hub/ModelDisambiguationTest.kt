package com.buysloans.hub

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelDisambiguationTest {
    private val candidates = listOf(
        ModelCandidate("m1", "ThinkPad X1 Carbon", 2021, "Gen 9", setOf("X1 Carbon Gen 9")),
        ModelCandidate("m2", "ThinkPad X1 Carbon", 2022, "Gen 10", setOf("X1 Carbon Gen 10"))
    )

    @Test
    fun explicitModelYearAndGenerationResolveUniquely() {
        val result = ModelDisambiguation.resolve(
            ModelQuery("ThinkPad X1 Carbon Gen 9 2021", model = "ThinkPad X1 Carbon", year = 2021, generation = "Gen 9"),
            candidates
        )
        assertEquals(ModelResolutionStatus.RESOLVED, result.status)
        assertEquals("m1", result.candidateId)
        assertTrue(result.confidence >= 0.95)
    }

    @Test
    fun missingYearAndGenerationRemainAmbiguous() {
        val result = ModelDisambiguation.resolve(ModelQuery("ThinkPad X1 Carbon"), candidates)
        assertEquals(ModelResolutionStatus.AMBIGUOUS, result.status)
        assertTrue(result.requiresConfirmation)
    }

    @Test
    fun weakTextDoesNotGuessADevice() {
        val result = ModelDisambiguation.resolve(ModelQuery("black laptop 16gb"), candidates)
        assertEquals(ModelResolutionStatus.UNRESOLVED, result.status)
        assertEquals(null, result.candidateId)
    }
}
