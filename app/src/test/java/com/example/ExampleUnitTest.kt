package com.example

import com.example.data.MarkdownExporter
import com.example.data.SettingsManager
import com.example.data.local.AgentMessageEntity
import com.example.data.local.EvolutionCycleEntity
import com.example.data.local.ResearchSessionEntity
import com.example.data.local.ScientificHypothesisEntity
import com.example.data.local.VerificationScenarioEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAllPresetProviderCatalogs() {
        val veniceModels = SettingsManager.PRESET_VENICE_MODELS
        val geminiModels = SettingsManager.PRESET_GEMINI_MODELS
        val routerModels = SettingsManager.PRESET_OPENROUTER_MODELS
        val ollamaModels = SettingsManager.PRESET_OLLAMA_MODELS

        assertTrue("Venice presets should not be empty", veniceModels.isNotEmpty())
        assertTrue("Gemini presets should not be empty", geminiModels.isNotEmpty())
        assertTrue("OpenRouter presets should not be empty", routerModels.isNotEmpty())
        assertTrue("Ollama presets should not be empty", ollamaModels.isNotEmpty())

        // Check providers
        assertTrue("Venice models have VENICE provider", veniceModels.all { it.provider == "VENICE" })
        assertTrue("Gemini models have GEMINI provider", geminiModels.all { it.provider == "GEMINI" })
        assertTrue("OpenRouter models have OPENROUTER provider", routerModels.all { it.provider == "OPENROUTER" })
        assertTrue("Ollama models have OLLAMA provider", ollamaModels.all { it.provider == "OLLAMA" })

        // Check combined catalog
        val combined = SettingsManager.PRESET_MODELS
        assertEquals(veniceModels.size + geminiModels.size + routerModels.size + ollamaModels.size, combined.size)
    }

    @Test
    fun testMarkdownExporterGeneration() {
        val session = ResearchSessionEntity(
            id = 1L,
            title = "Квантовая биофизика переноса энергии",
            topic = "Квантовая биофизика переноса энергии",
            domain = "Биофизика",
            activeTheoristModel = "gemini-2.5-pro",
            activeSkepticModel = "openrouter/deepseek/deepseek-r1",
            activeSynthesizerModel = "ollama/llama3.3",
            totalCycles = 2
        )

        val cycles = listOf(
            EvolutionCycleEntity(
                id = 101L,
                sessionId = 1L,
                cycleNumber = 1,
                theoristModel = "gemini-2.5-pro",
                skepticModel = "openrouter/deepseek/deepseek-r1",
                synthesizerModel = "ollama/llama3.3",
                userGuidance = "Проверить при криогенных температурах",
                status = "COMPLETED",
                viabilityScore = 88,
                hypothesisTitle = "Топологическая защита экситонного переноса"
            )
        )

        val messages = listOf(
            AgentMessageEntity(
                id = 201L,
                sessionId = 1L,
                cycleId = 101L,
                role = "THEORIST",
                modelName = "gemini-2.5-pro",
                agentTitle = "Теоретик-Прогрессор",
                content = "Предлагаю модель делокализованного экситона",
                timestamp = System.currentTimeMillis(),
                turnNumber = 1
            )
        )

        val hypotheses = listOf(
            ScientificHypothesisEntity(
                id = 301L,
                sessionId = 1L,
                cycleId = 101L,
                title = "Топологическая защита экситонного переноса",
                thesis = "Экситонные состояния в FMO-комплексе топологически защищены от декогеренции",
                mechanism = "Резонансные фононные моды подавляют тепловую флуктуацию",
                theoreticalBasis = "Уравнение Линдблада с немарковской релаксацией",
                noveltyScore = 92,
                confidenceScore = 85
            )
        )

        val scenarios = listOf(
            VerificationScenarioEntity(
                id = 401L,
                sessionId = 1L,
                cycleId = 101L,
                hypothesisId = 301L,
                title = "Двумерная фемтосекундная электронная спектроскопия (2DES)",
                methodology = "Фемтосекундная лазерная спектроскопия биений",
                experimentalSetup = "Титаново-сапфировый лазер 15 фс, криостат 77-300 K",
                falsificationCriteria = "Отсутствие квантовых осцилляций кросс-пиков при T > 77 K",
                controlGroups = "Денатурированный белковый комплекс без хромофоров",
                metricsAndThresholds = "Период биений tau > 250 фс, дефазировка gamma < 0.1 пс^-1",
                feasibilityScore = 80
            )
        )

        val md = MarkdownExporter.generateMarkdown(
            session = session,
            cycles = cycles,
            messages = messages,
            hypotheses = hypotheses,
            scenarios = scenarios
        )

        assertTrue("Markdown must contain header", md.contains("# Научный отчет Прогрессора"))
        assertTrue("Markdown must contain session topic", md.contains("Квантовая биофизика переноса энергии"))
        assertTrue("Markdown must contain table of contents", md.contains("## 📑 Оглавление"))
        assertTrue("Markdown must contain Popper falsification", md.contains("Критерий фальсификации по Карлу Попперу"))
        assertTrue("Markdown must contain hypothesis thesis", md.contains("Экситонные состояния"))
        assertTrue("Markdown must contain cycle transcript", md.contains("Теоретик-Прогрессор"))
        assertTrue("Markdown must contain footer", md.contains("AI Studio Android: Progressor"))
    }
}
