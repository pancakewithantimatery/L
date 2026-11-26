package com.example

import com.example.data.KeepBufferExporter
import com.example.data.LatexExporter
import com.example.data.MarkdownExporter
import com.example.data.SettingsManager
import com.example.data.local.AgentMessageEntity
import com.example.data.local.EvolutionCycleEntity
import com.example.data.local.ResearchSessionEntity
import com.example.data.local.ScientificCitationEntity
import com.example.data.local.ScientificHypothesisEntity
import com.example.data.local.VerificationScenarioEntity
import com.example.domain.DebateTtsManager
import com.example.domain.VoiceCommand
import com.example.domain.VoiceCommandParser
import com.example.ui.spatial.ProgressorFocusGraph
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
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

    @Test
    fun testLatexExporterAndBibliography() {
        val session = ResearchSessionEntity(
            id = 2L,
            title = "Топология & квантовые вычисления 100%",
            topic = "Топология & квантовые вычисления 100%",
            domain = "Квантовая физика",
            activeTheoristModel = "gemini-2.5-pro",
            activeSkepticModel = "deepseek_r1",
            activeSynthesizerModel = "llama_3.3",
            totalCycles = 1
        )
        val citations = listOf(
            ScientificCitationEntity(
                id = 10L,
                sessionId = 2L,
                arxivId = "2401.12345",
                title = "Topological Protection in Quantum Systems",
                authors = "A. Ivanov, B. Petrov",
                summary = "Abstract text",
                publishedYear = "2024",
                pdfUrl = "https://arxiv.org/pdf/2401.12345",
                doi = "10.1000/test.doi"
            )
        )

        val tex = LatexExporter.generateLatex(
            session = session,
            cycles = emptyList(),
            messages = emptyList(),
            hypotheses = emptyList(),
            scenarios = emptyList(),
            citations = citations
        )

        assertTrue("LaTeX must contain Russian babel preamble", tex.contains("\\usepackage[english,russian]{babel}"))
        assertTrue("LaTeX must escape & and % in title", tex.contains("Топология \\& квантовые вычисления 100\\%"))
        assertTrue("LaTeX must contain bibliography block", tex.contains("\\begin{thebibliography}{1}"))
        assertTrue("LaTeX must contain arXiv ID", tex.contains("2401.12345"))
        assertTrue("LaTeX must contain DOI", tex.contains("10.1000/test.doi"))
        assertFalse("LaTeX must not reference undefined amber color", tex.contains("colback=amber"))
    }

    @Test
    fun testTtsMarkdownSanitizer() {
        val raw = "### **Тезис:** Использование [модели FMO](https://arxiv.org) при `T = 77 K`\n> Важная цитата"
        val cleaned = DebateTtsManager.cleanMarkdownForSpeech(raw)
        assertEquals("Тезис: Использование модели FMO при T = 77 K Важная цитата", cleaned)
    }

    @Test
    fun testKeepBufferExporterSessionSummary() {
        val session = ResearchSessionEntity(
            id = 7L,
            title = "Топологическая защита квантового переноса",
            topic = "Топологическая защита квантового переноса в FMO-комплексах",
            domain = "Квантовая биофизика",
            activeTheoristModel = "gemini-2.5-pro",
            activeSkepticModel = "deepseek-r1",
            activeSynthesizerModel = "qwen-2.5-72b",
            totalCycles = 3
        )
        val cycles = listOf(
            EvolutionCycleEntity(
                id = 701L,
                sessionId = 7L,
                cycleNumber = 3,
                theoristModel = "gemini-2.5-pro",
                skepticModel = "deepseek-r1",
                synthesizerModel = "qwen-2.5-72b",
                userGuidance = "Проверить стабильность при 300 K",
                status = "COMPLETED",
                viabilityScore = 84,
                hypothesisTitle = "Вибронная защита когерентности"
            )
        )
        val hypotheses = listOf(
            ScientificHypothesisEntity(
                id = 711L,
                sessionId = 7L,
                cycleId = 701L,
                title = "Вибронная защита когерентности",
                thesis = "Резонансная связь экситонов и фононов удерживает фазу более 300 фс. Вторичный эффект снижает потери.",
                mechanism = "Когерентная перекачка энергии через локализованные колебательные моды",
                theoreticalBasis = "Немарковская модель Линдблада",
                noveltyScore = 89,
                confidenceScore = 81
            )
        )
        val scenarios = listOf(
            VerificationScenarioEntity(
                id = 721L,
                sessionId = 7L,
                cycleId = 701L,
                hypothesisId = 711L,
                title = "2DES спектроскопия",
                methodology = "Двумерная электронная спектроскопия",
                experimentalSetup = "Фемтосекундный импульс 15 фс при 295 K",
                falsificationCriteria = "Отсутствие осцилляций кросс-пиков при комнатной температуре",
                controlGroups = "Мутантный комплекс без резонансной моды",
                metricsAndThresholds = "Время жизни когерентности > 250 фс",
                feasibilityScore = 78
            )
        )
        val messages = listOf(
            AgentMessageEntity(
                id = 731L,
                sessionId = 7L,
                cycleId = 701L,
                role = "SYNTHESIZER",
                modelName = "qwen-2.5-72b",
                agentTitle = "Прогрессор-Синтезатор",
                content = "- Подтверждена устойчивость модели к тепловому шуму.\n- Проверить чувствительность к изотопному замещению дейтерием.",
                timestamp = System.currentTimeMillis(),
                turnNumber = 3
            )
        )
        val citations = listOf(
            ScientificCitationEntity(
                id = 741L,
                sessionId = 7L,
                arxivId = "2405.09876",
                title = "Vibronic Coherence in Photosynthetic Light Harvesting",
                authors = "E. Smith",
                summary = "Summary",
                publishedYear = "2024",
                pdfUrl = "https://arxiv.org/pdf/2405.09876",
                doi = "10.1038/s41567-024-0001"
            )
        )

        val payload = KeepBufferExporter.generateSessionBuffer(
            session = session,
            cycles = cycles,
            messages = messages,
            hypotheses = hypotheses,
            scenarios = scenarios,
            citations = citations
        )

        assertTrue("Keep title must carry buffer prefix and cycle", payload.title.startsWith("[Буфер]") && payload.title.contains("Ц#3"))
        assertTrue("Must include Context/Thesis section", payload.body.contains("**Контекст / Тезис**"))
        assertTrue("Must include Facts section", payload.body.contains("**Факты и выводы**"))
        assertTrue("Must include arXiv citation link", payload.body.contains("arXiv:2405.09876") && payload.body.contains("https://arxiv.org/pdf/2405.09876"))
        assertTrue("Must include Popper falsification fact", payload.body.contains("Критерий Поппера:"))
        assertTrue("Must include Markdown checklist items", payload.body.contains("**Решения и шаги**") && payload.body.contains("- [ ] "))
        assertTrue("Operational buffer should remain compact (< 1800 chars)", payload.body.length < 1800)
    }

    @Test
    fun testKeepBufferExporterMessageSummary() {
        val msg = AgentMessageEntity(
            id = 801L,
            sessionId = 7L,
            cycleId = 701L,
            role = "SKEPTIC",
            modelName = "deepseek-r1",
            agentTitle = "Скептик-Верификатор",
            content = "Гипотеза уязвима к дефазировке в растворителе. Проверить контрольный эксперимент с мутантным хромофором.",
            timestamp = System.currentTimeMillis(),
            turnNumber = 2
        )
        val payload = KeepBufferExporter.generateMessageBuffer(null, msg)
        assertTrue("Title must tag Skeptic buffer", payload.title.contains("[Буфер • Скептик]"))
        assertTrue("Body must contain checklist", payload.body.contains("- [ ] "))
        assertTrue("Body must contain distilled thesis", payload.body.contains("Гипотеза уязвима к дефазировке"))
    }

    @Test
    fun testProgressorFocusGraphRouting() {
        val graph = ProgressorFocusGraph()
        assertSame(graph.navArena, graph.navRequesterForIndex(0))
        assertSame(graph.navHypotheses, graph.navRequesterForIndex(1))
        assertSame(graph.navAiLab, graph.navRequesterForIndex(2))
        assertSame(graph.navCouncil, graph.navRequesterForIndex(3))
        assertSame(graph.navTopics, graph.navRequesterForIndex(4))

        assertSame(graph.arenaArxiv, graph.contentEntryForTab(0))
        assertSame(graph.hypTabDag, graph.contentEntryForTab(1))
        assertSame(graph.councilConfigBtn, graph.contentEntryForTab(3))
        assertSame(graph.sessionsNewBtn, graph.contentEntryForTab(4))

        assertSame(graph.arenaPrimaryAction, graph.contentBottomForTab(0))
        assertSame(graph.dagCopyThesis, graph.contentBottomForTab(1))
        assertSame(graph.councilFastLaunchBtn, graph.contentBottomForTab(3))
        assertNotNull(graph.arenaKeepExport)
        assertNotNull(graph.topVoiceMic)
    }

    @Test
    fun testVoiceCommandParserNavigationAndDebates() {
        assertEquals(VoiceCommand.LaunchDebateCycle, VoiceCommandParser.parse("Запусти цикл дебатов"))
        assertEquals(VoiceCommand.LaunchDebateCycle, VoiceCommandParser.parse("start debate"))

        val navHyp = VoiceCommandParser.parse("Открой гипотезы")
        assertTrue(navHyp is VoiceCommand.NavigateTab && navHyp.tabIndex == 1)

        val navCouncil = VoiceCommandParser.parse("Совет моделей")
        assertTrue(navCouncil is VoiceCommand.NavigateTab && navCouncil.tabIndex == 3)

        val intervention = VoiceCommandParser.parse("Указание: проверь стабильность при 300 кельвинах")
        assertTrue(intervention is VoiceCommand.HumanIntervention)
        assertEquals("проверь стабильность при 300 кельвинах", (intervention as VoiceCommand.HumanIntervention).guidance)

        val newSession = VoiceCommandParser.parse("Новая тема Квантовая гравитация")
        assertTrue(newSession is VoiceCommand.CreateNewSession)
        assertEquals("Квантовая гравитация", (newSession as VoiceCommand.CreateNewSession).topic)

        val arxiv = VoiceCommandParser.parse("Поиск arXiv topological qubits")
        assertTrue(arxiv is VoiceCommand.SearchArxiv)
        assertEquals("topological qubits", (arxiv as VoiceCommand.SearchArxiv).query)

        assertEquals(VoiceCommand.ExportToKeep, VoiceCommandParser.parse("Сохрани в Google Keep"))
        assertEquals(VoiceCommand.SetXrealMode(true), VoiceCommandParser.parse("Включи режим XREAL"))
        assertEquals(VoiceCommand.SetXrealMode(false), VoiceCommandParser.parse("Выключи XREAL"))
        assertEquals(VoiceCommand.TtsPause, VoiceCommandParser.parse("Пауза"))
        assertEquals(VoiceCommand.StopHandsFree, VoiceCommandParser.parse("Выключи микрофон"))

        // Multi-hypothesis fallback: ignores noisy #1 match and picks #2 valid command
        val candidateCmd = VoiceCommandParser.parseCandidates(listOf("в кипарис", "в кип"))
        assertEquals(VoiceCommand.ExportToKeep, candidateCmd)
    }
}
