package com.example

import com.example.data.SettingsManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testVenicePresetModels() {
        val presets = SettingsManager.PRESET_MODELS
        assertTrue("Presets should have at least 5 models", presets.size >= 5)

        val theoristModel = presets.find { it.recommendedRole == "THEORIST" }
        assertNotNull("Should have a recommended theorist model", theoristModel)

        val skepticModel = presets.find { it.recommendedRole == "SKEPTIC" }
        assertNotNull("Should have a recommended skeptic model", skepticModel)

        val synthModel = presets.find { it.recommendedRole == "SYNTHESIZER" }
        assertNotNull("Should have a recommended synthesizer model", synthModel)
    }

    @Test
    fun testViabilityScoreRange() {
        val score = 82
        assertTrue("Viability score must be between 0 and 100", score in 0..100)
    }
}
