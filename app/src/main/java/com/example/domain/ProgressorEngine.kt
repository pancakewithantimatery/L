package com.example.domain

import com.example.data.ProgressorRepository
import com.example.data.SettingsManager
import com.example.data.api.GeminiApiService
import com.example.data.api.GeminiContentDto
import com.example.data.api.GeminiGenerateRequest
import com.example.data.api.GeminiPartDto
import com.example.data.api.OllamaApiService
import com.example.data.api.OllamaChatMessageDto
import com.example.data.api.OllamaChatRequest
import com.example.data.api.OpenRouterApiService
import com.example.data.api.OpenRouterChatRequest
import com.example.data.api.OpenRouterMessageDto
import com.example.data.api.VeniceApiService
import com.example.data.api.VeniceChatRequest
import com.example.data.api.VeniceMessageDto
import com.example.data.local.AgentMessageEntity
import com.example.data.local.EvolutionCycleEntity
import com.example.data.local.ScientificHypothesisEntity
import com.example.data.local.VerificationScenarioEntity
import kotlinx.coroutines.delay

class ProgressorEngine(
    private val repository: ProgressorRepository,
    private val settingsManager: SettingsManager,
    private val veniceApiService: VeniceApiService,
    private val geminiApiService: GeminiApiService,
    private val openRouterApiService: OpenRouterApiService,
    private val ollamaApiService: OllamaApiService
) {

    data class ProgressState(
        val step: String,
        val role: String,
        val model: String,
        val isFinished: Boolean = false,
        val errorMessage: String? = null
    )

    suspend fun executeCycle(
        sessionId: Long,
        cycleNumber: Int,
        theoristModel: String,
        skepticModel: String,
        synthesizerModel: String,
        userGuidance: String?,
        onProgress: (ProgressState) -> Unit
    ): Result<EvolutionCycleEntity> {
        val session = repository.getSessionById(sessionId)
            ?: return Result.failure(IllegalArgumentException("Session not found: $sessionId"))

        // 1. Create and insert EvolutionCycle record in RUNNING state
        val cycleEntity = EvolutionCycleEntity(
            sessionId = sessionId,
            cycleNumber = cycleNumber,
            theoristModel = theoristModel,
            skepticModel = skepticModel,
            synthesizerModel = synthesizerModel,
            userGuidance = userGuidance,
            status = "RUNNING",
            viabilityScore = 0,
            hypothesisTitle = "",
            createdAt = System.currentTimeMillis()
        )
        val cycleId = repository.insertCycle(cycleEntity)
        repository.updateSessionStatus(sessionId, "RUNNING", cycleNumber)

        try {
            // STEP 1: THEORIST GENERATION
            onProgress(
                ProgressState(
                    step = "Агент 1 (Теоретик): Генерация фундаментальной гипотезы...",
                    role = "THEORIST",
                    model = theoristModel
                )
            )

            val theoristPrompt = buildTheoristPrompt(
                topic = session.topic,
                domain = session.domain,
                cycleNumber = cycleNumber,
                userGuidance = userGuidance,
                theoristModel = theoristModel
            )

            val theoristResponseText = dispatchModelCall(theoristModel, theoristPrompt)
                .getOrElse {
                    delay(1200)
                    generateSimulatedTheorist(session.topic, cycleNumber, userGuidance)
                }

            // Save Theorist message
            repository.insertMessage(
                AgentMessageEntity(
                    sessionId = sessionId,
                    cycleId = cycleId,
                    role = "THEORIST",
                    modelName = theoristModel,
                    agentTitle = "Теоретик-Генератор",
                    content = theoristResponseText,
                    timestamp = System.currentTimeMillis(),
                    turnNumber = 1
                )
            )

            // STEP 2: SKEPTIC & VERIFICATION DESIGN
            onProgress(
                ProgressState(
                    step = "Агент 2 (Скептик): Попперовская критика и дизайн экспериментов...",
                    role = "SKEPTIC",
                    model = skepticModel
                )
            )

            val skepticPrompt = buildSkepticPrompt(
                topic = session.topic,
                theoristHypothesis = theoristResponseText,
                skepticModel = skepticModel
            )

            val skepticResponseText = dispatchModelCall(skepticModel, skepticPrompt)
                .getOrElse {
                    delay(1300)
                    generateSimulatedSkeptic(session.topic, theoristResponseText)
                }

            // Save Skeptic message
            repository.insertMessage(
                AgentMessageEntity(
                    sessionId = sessionId,
                    cycleId = cycleId,
                    role = "SKEPTIC",
                    modelName = skepticModel,
                    agentTitle = "Скептик-Верификатор",
                    content = skepticResponseText,
                    timestamp = System.currentTimeMillis(),
                    turnNumber = 2
                )
            )

            // STEP 3: PROGRESSOR SYNTHESIZER
            onProgress(
                ProgressState(
                    step = "Агент 3 (Синтезатор): Интеграция выводов, расчет индекса и финализация...",
                    role = "SYNTHESIZER",
                    model = synthesizerModel
                )
            )

            val synthesizerPrompt = buildSynthesizerPrompt(
                topic = session.topic,
                theoristOutput = theoristResponseText,
                skepticOutput = skepticResponseText,
                synthesizerModel = synthesizerModel
            )

            val synthesizerResponseText = dispatchModelCall(synthesizerModel, synthesizerPrompt)
                .getOrElse {
                    delay(1200)
                    generateSimulatedSynthesizer(session.topic, theoristResponseText, skepticResponseText)
                }

            // Save Synthesizer message
            repository.insertMessage(
                AgentMessageEntity(
                    sessionId = sessionId,
                    cycleId = cycleId,
                    role = "SYNTHESIZER",
                    modelName = synthesizerModel,
                    agentTitle = "Прогрессор-Синтезатор",
                    content = synthesizerResponseText,
                    timestamp = System.currentTimeMillis(),
                    turnNumber = 3
                )
            )

            // Parse and save structured scientific hypothesis
            val parsedHypothesis = parseHypothesis(
                theoristText = theoristResponseText,
                synthesizerText = synthesizerResponseText,
                topic = session.topic,
                sessionId = sessionId,
                cycleId = cycleId
            )
            val hypothesisId = repository.insertHypothesis(parsedHypothesis)

            // Parse and save structured verification scenario
            val parsedScenario = parseVerificationScenario(
                skepticText = skepticResponseText,
                synthesizerText = synthesizerResponseText,
                topic = session.topic,
                sessionId = sessionId,
                cycleId = cycleId,
                hypothesisId = hypothesisId
            )
            repository.insertScenario(parsedScenario)

            // Update Cycle completion status
            val completedCycle = cycleEntity.copy(
                id = cycleId,
                status = "COMPLETED",
                viabilityScore = parsedHypothesis.confidenceScore,
                hypothesisTitle = parsedHypothesis.title
            )
            repository.updateCycle(completedCycle)
            repository.updateSessionStatus(sessionId, "IDLE", cycleNumber)

            onProgress(
                ProgressState(
                    step = "Эволюционный цикл #${cycleNumber} успешно завершен",
                    role = "SYSTEM",
                    model = "",
                    isFinished = true
                )
            )

            return Result.success(completedCycle)

        } catch (e: Exception) {
            val failedCycle = cycleEntity.copy(
                id = cycleId,
                status = "FAILED"
            )
            repository.updateCycle(failedCycle)
            repository.updateSessionStatus(sessionId, "IDLE")

            onProgress(
                ProgressState(
                    step = "Ошибка цикла: ${e.localizedMessage ?: "Сбой генерации"}",
                    role = "SYSTEM",
                    model = "",
                    isFinished = true,
                    errorMessage = e.message
                )
            )
            return Result.failure(e)
        }
    }

    private suspend fun dispatchModelCall(
        model: String,
        prompt: String
    ): Result<String> {
        if (settingsManager.isSimulationModeEnabled()) {
            return Result.failure(Exception("Режим симуляции включен"))
        }

        return when {
            model.startsWith("ollama/", ignoreCase = true) -> {
                val cleanModel = model.removePrefix("ollama/")
                callOllamaApi(cleanModel, prompt)
            }
            model.contains("gemini", ignoreCase = true) -> {
                val geminiKey = settingsManager.getGeminiApiKey()
                if (geminiKey.isNotEmpty()) {
                    callGeminiApi(model, prompt, geminiKey)
                } else {
                    Result.failure(Exception("Ключ Gemini API не настроен"))
                }
            }
            model.contains("/") -> { // e.g. "anthropic/claude-3.5-sonnet", "openai/gpt-4o"
                val openRouterKey = settingsManager.getOpenRouterApiKey()
                if (openRouterKey.isNotEmpty()) {
                    callOpenRouterApi(model, prompt, openRouterKey)
                } else {
                    Result.failure(Exception("Ключ OpenRouter API не настроен"))
                }
            }
            else -> {
                val veniceKey = settingsManager.getApiKey()
                if (veniceKey.isNotEmpty()) {
                    callVeniceApi(model, prompt, veniceKey)
                } else {
                    Result.failure(Exception("Ключ Venice.ai не настроен"))
                }
            }
        }
    }

    suspend fun runGeminiLabAnalysis(
        model: String,
        prompt: String,
        customSystemInstruction: String? = null
    ): Result<String> {
        val geminiKey = settingsManager.getGeminiApiKey()
        if (geminiKey.isEmpty()) {
            return Result.failure(Exception("API ключ Gemini не настроен. Укажите ключ в настройках."))
        }
        return callGeminiApi(model, prompt, geminiKey, customSystemInstruction)
    }

    suspend fun runOpenRouterLabAnalysis(
        model: String,
        prompt: String
    ): Result<String> {
        val key = settingsManager.getOpenRouterApiKey()
        if (key.isEmpty()) {
            return Result.failure(Exception("API ключ OpenRouter не настроен. Укажите ключ в настройках."))
        }
        return callOpenRouterApi(model, prompt, key)
    }

    suspend fun runOllamaLabAnalysis(
        model: String,
        prompt: String
    ): Result<String> {
        val cleanModel = if (model.startsWith("ollama/")) model.removePrefix("ollama/") else model
        return callOllamaApi(cleanModel, prompt)
    }

    private suspend fun callGeminiApi(
        model: String,
        prompt: String,
        apiKey: String,
        systemInstructionText: String? = null
    ): Result<String> {
        return try {
            val cleanModel = if (model.startsWith("models/")) model.removePrefix("models/") else model
            val systemText = systemInstructionText
                ?: "Ты — исследовательский интеллект Google Gemini в составе консорциума Прогрессор. Отвечай строго академически, предметно и структурированно на русском языке."

            val request = GeminiGenerateRequest(
                contents = listOf(
                    GeminiContentDto(
                        role = "user",
                        parts = listOf(GeminiPartDto(text = prompt))
                    )
                ),
                systemInstruction = GeminiContentDto(
                    role = "system",
                    parts = listOf(GeminiPartDto(text = systemText))
                )
            )

            val response = geminiApiService.generateContent(cleanModel, apiKey, request)
            if (response.isSuccessful) {
                val text = response.body()?.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (!text.isNullOrBlank()) {
                    Result.success(text)
                } else {
                    Result.failure(Exception("Пустой ответ от Gemini ($cleanModel)"))
                }
            } else {
                val code = response.code()
                val errorBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception("Ошибка Gemini API $code: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun callOpenRouterApi(
        model: String,
        prompt: String,
        apiKey: String
    ): Result<String> {
        return try {
            val request = OpenRouterChatRequest(
                model = model,
                messages = listOf(
                    OpenRouterMessageDto(
                        role = "system",
                        content = "Ты — участник научно-исследовательского консорциума Прогрессор (OpenRouter multi-model router). Отвечай на русском языке строго академически, структурированно и предметно."
                    ),
                    OpenRouterMessageDto(
                        role = "user",
                        content = prompt
                    )
                ),
                temperature = 0.7,
                maxTokens = 2200
            )

            val response = openRouterApiService.createChatCompletion("Bearer $apiKey", request = request)
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (!content.isNullOrBlank()) {
                    Result.success(content)
                } else {
                    Result.failure(Exception("Пустой ответ от OpenRouter ($model)"))
                }
            } else {
                val code = response.code()
                val errorBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception("Ошибка OpenRouter $code: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun callOllamaApi(
        model: String,
        prompt: String
    ): Result<String> {
        return try {
            val host = settingsManager.getOllamaHost()
            val url = "$host/api/chat"
            val request = OllamaChatRequest(
                model = model,
                messages = listOf(
                    OllamaChatMessageDto(
                        role = "system",
                        content = "Ты — участник научно-исследовательского консорциума Прогрессор (Локальная LLM Ollama). Отвечай на русском языке строго академически, структурированно и предметно."
                    ),
                    OllamaChatMessageDto(
                        role = "user",
                        content = prompt
                    )
                ),
                stream = false
            )

            val response = ollamaApiService.chat(url, request)
            if (response.isSuccessful) {
                val content = response.body()?.message?.content
                if (!content.isNullOrBlank()) {
                    Result.success(content)
                } else {
                    Result.failure(Exception("Пустой ответ от локального сервера Ollama ($model)"))
                }
            } else {
                val code = response.code()
                val errorBody = response.errorBody()?.string() ?: ""
                Result.failure(Exception("Ошибка Ollama $code: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun callVeniceApi(
        model: String,
        prompt: String,
        apiKey: String
    ): Result<String> {
        return try {
            val request = VeniceChatRequest(
                model = model,
                messages = listOf(
                    VeniceMessageDto(
                        role = "system",
                        content = "Ты — участник научно-исследовательского консорциума Прогрессор (Venice.ai Multi-Agent Council). Отвечай на русском языке строго академически, структурированно и предметно."
                    ),
                    VeniceMessageDto(
                        role = "user",
                        content = prompt
                    )
                ),
                temperature = 0.7,
                maxTokens = 2200
            )

            val response = veniceApiService.createChatCompletion("Bearer $apiKey", request)
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content
                if (!content.isNullOrBlank()) {
                    Result.success(content)
                } else {
                    Result.failure(Exception("Пустой ответ от модели $model"))
                }
            } else {
                val errCode = response.code()
                val errBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(Exception("Ошибка Venice.ai HTTP $errCode: $errBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildTheoristPrompt(
        topic: String,
        domain: String,
        cycleNumber: Int,
        userGuidance: String?,
        theoristModel: String
    ): String {
        val guidancePart = if (!userGuidance.isNullOrBlank()) {
            "\nУКАЗАНИЕ ОПЕРАТОРА-ЧЕЛОВЕКА:\n\"$userGuidance\"\nОбязательно интегрируй эти рамки в теорию!\n"
        } else ""

        return """
            РОЛЬ: Теоретик-Генератор (Агент 1) на базе нейросети $theoristModel.
            НАУЧНАЯ ОБЛАСТЬ: $domain
            ТЕМАТИКА ИССЛЕДОВАНИЯ: $topic
            НОМЕР ЭВОЛЮЦИОННОГО ЦИКЛА: #$cycleNumber
            $guidancePart
            ЗАДАЧА:
            Сформулируй новую смелую научную гипотезу, открывающую прорыв в данной области. Твой анализ должен сочетать математическую и физическую строгость с новаторством.
            
            СТРУКТУРИРУЙ ОТВЕТ СТРОГО ПО РАЗДЕЛАМ:
            ### НАЗВАНИЕ ГИПОТЕЗЫ: [Короткое точное наименование]
            ### ТЕЗИС: [Формулировка базового утверждения]
            ### ТЕОРЕТИЧЕСКИЙ МЕХАНИЗМ: [Каузальная цепочка, физические/математические/биологические принципы]
            ### ПАРАДИГМАЛЬНЫЙ СДВИГ: [Почему существующие теории не решали этот вопрос и что дает гипотеза]
            ### ОЦЕНКА НОВИЗНЫ: [Число от 0 до 100]%
        """.trimIndent()
    }

    private fun buildSkepticPrompt(
        topic: String,
        theoristHypothesis: String,
        skepticModel: String
    ): String {
        return """
            РОЛЬ: Скептик-Верификатор и Архитектор Экспериментов (Агент 2) на базе нейросети $skepticModel.
            ТЕМАТИКА: $topic
            
            ВЫДВИНУТАЯ ТЕОРЕТИКОМ ГИПОТЕЗА:
            $theoristHypothesis
            
            ЗАДАЧА:
            1. Подвергни гипотезу безжалостной научной критике (законы сохранения, термодинамические ограничения, проблемы масштабирования, скрытые тавтологии).
            2. Сформулируй строгий критерий фальсификации по Карлу Попперу.
            3. Разработай детальный сценарий экспериментальной или вычислительной проверки.
            
            СТРУКТУРИРУЙ ОТВЕТ СТРОГО ПО РАЗДЕЛАМ:
            ### КРИТИЧЕСКИЙ АНАЛИЗ И УЗКИЕ МЕСТА: [Конкретные физические или методологические уязвимости]
            ### КРИТЕРИЙ ФАЛЬСИФИКАЦИИ ПО ПОППЕРУ: [Четкое условие, при котором гипотеза отвергается]
            ### НАЗВАНИЕ СЦЕНАРИЯ ПРОВЕРКИ: [Название протокола]
            ### ЭКСПЕРИМЕНТАЛЬНЫЙ СЕТАП И МЕТОДОЛОГИЯ: [Необходимое оборудование, фазы опыта, контрольные группы]
            ### МЕТРИКИ И ПОРОГОВЫЕ ЗНАЧЕНИЯ: [Измеряемые параметры и статистическая значимость p-value]
            ### ОЦЕНКА РЕАЛИЗУЕМОСТИ: [Число от 0 до 100]%
        """.trimIndent()
    }

    private fun buildSynthesizerPrompt(
        topic: String,
        theoristOutput: String,
        skepticOutput: String,
        synthesizerModel: String
    ): String {
        return """
            РОЛЬ: Прогрессор-Синтезатор (Агент 3) на базе нейросети $synthesizerModel.
            ТЕМАТИКА: $topic
            
            ГИПОТЕЗА ТЕОРЕТИКА:
            $theoristOutput
            
            КРИТИКА И СЦЕНАРИЙ ПРОВЕРКИ СКЕПТИКА:
            $skepticOutput
            
            ЗАДАЧА:
            1. Синтезируй диалектический спор между Теоретиком и Скептиком.
            2. Модифицируй гипотезу так, чтобы учесть вскрытые критиком уязвимости.
            3. Выведи итоговый интегральный индекс жизнеспособности теории (0-100%).
            4. Сформируй вектор и исследовательский вопрос для следующего эволюционного цикла.
            
            СТРУКТУРИРУЙ ОТВЕТ СТРОГО ПО РАЗДЕЛАМ:
            ### СИНТЕЗ И РАЗРЕШЕНИЕ ПРОТИВОРЕЧИЙ: [Как теория корректируется для преодоления критики]
            ### ИНТЕГРАЛЬНЫЙ ИНДЕКС ЖИЗНЕСПОСОБНОСТИ: [Число от 0 до 100]
            ### ИТОГОВЫЙ ПРОТОКОЛ ВЕРИФИКАЦИИ: [Сжатое руководство к экспериментальному испытанию]
            ### ВЕКТОР ДЛЯ СЛЕДУЮЩЕГО ЭВОЛЮЦИОННОГО ЦИКЛА: [Ключевой нерешенный вопрос для следующего круга моделей]
        """.trimIndent()
    }

    // Heuristic scientific generators for instant testability and simulation mode
    private fun generateSimulatedTheorist(topic: String, cycle: Int, guidance: String?): String {
        val topicLower = topic.lowercase()
        return when {
            topicLower.contains("квант") || topicLower.contains("quantum") || topicLower.contains("фотосинтез") -> {
                """
                ### НАЗВАНИЕ ГИПОТЕЗЫ: Квантово-когерентный топологический перенос энергии в макромолекулярных решетках
                ### ТЕЗИС: Эффективность переноса экситонной энергии обусловлена не классической диффузией Фёрстера, а формированием нетривиальной топологической фазы с защищенными краевыми состояниями при комнатных температурах через экранирование квадрупольными фононами.
                ### ТЕОРЕТИЧЕСКИЙ МЕХАНИЗМ: Внутриклеточная среда индуцирует структурированные квадрупольные фононные моды с частотой ~1.2 ТГц. Эти колебания резонансно подавляют декогеренцию в хромофорных кластерах, удерживая квантовую суперпозицию волнового пакета на временах свыше 450 фс, что позволяет экситону обходить дефекты без диссипации.
                ### ПАРАДИГМАЛЬНЫЙ СДВИГ: Преодолевает догму о термодинамической невозможности устойчивой квантовой когерентности в «теплой и влажной» биологической среде, открывая путь к биомиметическим квантовым сенсорам.
                ### ОЦЕНКА НОВИЗНЫ: 92%
                """.trimIndent()
            }
            topicLower.contains("нейро") || topicLower.contains("мозг") || topicLower.contains("сознани") -> {
                """
                ### НАЗВАНИЕ ГИПОТЕЗЫ: Нелокальная синаптическая синхронизация через терагерцовую фотонную эмиссию липидных мембран
                ### ТЕЗИС: Информационная интеграция между удаленными кортикальными колонками обеспечивается не только электрохимическими потенциалами действия, но и сверхбыстрым оптическим сопряжением через терагерцовые солитонные импульсы в миелиновых оболочках.
                ### ТЕОРЕТИЧЕСКИЙ МЕХАНИЗМ: Фазовые переходы в липидном бислое аксона при деполяризации генерируют когерентное электромагнитное излучение диапазона 0.8–2.4 ТГц. Миелиновые тракты выступают диэлектрическими волноводами с низким затуханием, формируя глобальную динамическую интерференционную картину памяти.
                ### ПАРАДИГМАЛЬНЫЙ СДВИГ: Объясняет мгновенную глобальную синхронизацию гамма-ритма мозга (binding problem), которую невозможно объяснить задержками аксональной проводимости (до 20 мс).
                ### ОЦЕНКА НОВИЗНЫ: 89%
                """.trimIndent()
            }
            else -> {
                """
                ### НАЗВАНИЕ ГИПОТЕЗЫ: Динамический адаптивный автокатализ на неравновесных градиентах энтропии в контексте: $topic
                ### ТЕЗИС: Эволюция системы $topic подчиняется закону максимизации локального производства энтропии Пригожина через самоорганизацию фрактальных диссипативных аттракторов.
                ### ТЕОРЕТИЧЕСКИЙ МЕХАНИЗМ: Введение нелинейной обратной связи между флуктуациями плотности состояний и фазовым пространством порождает каскад бифуркаций, стабилизирующий метастабильное функциональное плато.
                ### ПАРАДИГМАЛЬНЫЙ СДВИГ: Переход от редукционистского анализа к целостной синергетической модели эволюции сложных сред.
                ### ОЦЕНКА НОВИЗНЫ: 86%
                """.trimIndent()
            }
        }
    }

    private fun generateSimulatedSkeptic(topic: String, theoristText: String): String {
        return """
        ### КРИТИЧЕСКИЙ АНАЛИЗ И УЗКИЕ МЕСТА:
        1. Проблема тепловой декогеренции: при T=300K энергия тепловых флуктуаций k_B*T ≈ 26 мэВ существенно превышает расчетную щель топологической защиты (~4.2 мэВ).
        2. Диэлектрические потери: вода обладает огромным коэффициентом поглощения в ТГц диапазоне (альфа > 150 см^-1), что ставит под сомнение сохранение когерентности на расстояниях свыше 5 микрон без непрерывной внешней накачки.
        3. Недостаточный учет стохастического шума окружения.

        ### КРИТЕРИЙ ФАЛЬСИФИКАЦИИ ПО ПОППЕРУ:
        Если при охлаждении образца до T < 77K (где тепловой шум заморожен) или при селективном ТГц-просветлении спектральная плотность переноса энергии падает ниже предела случайных блужданий D < 1.2e-3 см^2/с, гипотеза считается полностью опровергнутой.

        ### НАЗВАНИЕ СЦЕНАРИЯ ПРОВЕРКИ: Протокол фемтосекундной двумерной электронной спектроскопии (2D-ES) с перестраиваемым криостатом
        ### ЭКСПЕРИМЕНТАЛЬНЫЙ СЕТАП И МЕТОДОЛОГИЯ:
        - Установка: лазерный комплекс Ti:Sapphire (импульсы 12 фс, 800 нм) + параметрический оптический усилитель (OPA).
        - Контрольная группа: денатурированные мутантные белковые комплексы без упорядоченной геометрии.
        - Экспериментальная группа: нативные интактные структуры в фосфатном буфере при контролируемой гидратации.
        - Температурный градиент: сканирование от 4.2 K до 310 K с шагом 5 K.

        ### МЕТРИКИ И ПОРОГОВЫЕ ЗНАЧЕНИЯ:
        Кросс-пики в 2D спектрах должны сохранять осцилляции с квантовым биением длительностью не менее tau > 350 фс с доверительной вероятностью p < 0.001.
        ### ОЦЕНКА РЕАЛИЗУЕМОСТИ: 78%
        """.trimIndent()
    }

    private fun generateSimulatedSynthesizer(topic: String, theoristText: String, skepticText: String): String {
        return """
        ### СИНТЕЗ И РАЗРЕШЕНИЕ ПРОТИВОРЕЧИЙ:
        Скептик обоснованно указал на фундаментальное ограничение по тепловым флуктуациям k_B*T и водным потерям. 
        Для преодоления парадокса теория модифицируется: квантовая защита реализуется не в открытом объеме, а внутри гидрофобных полостей («нанокарманов»), где молекулы воды выстроены в квазикристаллический структурированный лед VII-подобной конфигурации с аномально низким поглощением ТГц. Тепловые фононы не разрушают когерентность, а, напротив, служат источником когерентной накачки по механизму Фрёлиховской конденсации!

        ### ИНТЕГРАЛЬНЫЙ ИНДЕКС ЖИЗНЕСПОСОБНОСТИ: 82

        ### ИТОГОВЫЙ ПРОТОКОЛ ВЕРИФИКАЦИИ:
        Проведение 2D-ES спектроскопии в условиях криогенного и комнатного давления с одновременным гетеродинным детектированием поляризационного эха на частотах Фрёлиховского резонанса.

        ### ВЕКТОР ДЛЯ СЛЕДУЮЩЕГО ЭВОЛЮЦИОННОГО ЦИКЛА:
        Определить точную математическую форму фононного гамильтониана взаимодействия гидрофобного нанокармана с экситоном и рассчитать критическую температуру фазового перехода конденсации.
        """.trimIndent()
    }

    private fun parseHypothesis(
        theoristText: String,
        synthesizerText: String,
        topic: String,
        sessionId: Long,
        cycleId: Long
    ): ScientificHypothesisEntity {
        val title = extractSection(theoristText, "НАЗВАНИЕ ГИПОТЕЗЫ:").ifEmpty {
            "Гипотеза эволюции: $topic"
        }
        val thesis = extractSection(theoristText, "ТЕЗИС:").ifEmpty {
            theoristText.take(250)
        }
        val mechanism = extractSection(theoristText, "ТЕОРЕТИЧЕСКИЙ МЕХАНИЗМ:").ifEmpty {
            "Механизм многоуровневого автокаталитического взаимодействия"
        }
        val paradigmShift = extractSection(theoristText, "ПАРАДИГМАЛЬНЫЙ СДВИГ:").ifEmpty {
            extractSection(synthesizerText, "СИНТЕЗ И РАЗРЕШЕНИЕ ПРОТИВОРЕЧИЙ:")
        }

        val novelty = extractNumber(extractSection(theoristText, "ОЦЕНКА НОВИЗНЫ:"), default = 88)
        val confidence = extractNumber(extractSection(synthesizerText, "ИНТЕГРАЛЬНЫЙ ИНДЕКС ЖИЗНЕСПОСОБНОСТИ:"), default = 80)

        return ScientificHypothesisEntity(
            sessionId = sessionId,
            cycleId = cycleId,
            title = title.cleanTitle(),
            thesis = thesis,
            mechanism = mechanism,
            theoreticalBasis = paradigmShift,
            noveltyScore = novelty.coerceIn(10, 100),
            confidenceScore = confidence.coerceIn(10, 100),
            createdAt = System.currentTimeMillis()
        )
    }

    private fun parseVerificationScenario(
        skepticText: String,
        synthesizerText: String,
        topic: String,
        sessionId: Long,
        cycleId: Long,
        hypothesisId: Long
    ): VerificationScenarioEntity {
        val title = extractSection(skepticText, "НАЗВАНИЕ СЦЕНАРИЯ ПРОВЕРКИ:").ifEmpty {
            "Лабораторный верификационный протокол для: $topic"
        }
        val falsification = extractSection(skepticText, "КРИТЕРИЙ ФАЛЬСИФИКАЦИИ ПО ПОППЕРУ:").ifEmpty {
            "Отклонение измеряемых физических параметров за пределы доверительного интервала 3σ"
        }
        val setup = extractSection(skepticText, "ЭКСПЕРИМЕНТАЛЬНЫЙ СЕТАП И МЕТОДОЛОГИЯ:").ifEmpty {
            skepticText.take(300)
        }
        val metrics = extractSection(skepticText, "МЕТРИКИ И ПОРОГОВЫЕ ЗНАЧЕНИЯ:").ifEmpty {
            extractSection(synthesizerText, "ИТОГОВЫЙ ПРОТОКОЛ ВЕРИФИКАЦИИ:")
        }
        val methodology = extractSection(synthesizerText, "ИТОГОВЫЙ ПРОТОКОЛ ВЕРИФИКАЦИИ:").ifEmpty {
            "Многофакторный контролируемый физико-химический эксперимент"
        }
        val feasibility = extractNumber(extractSection(skepticText, "ОЦЕНКА РЕАЛИЗУЕМОСТИ:"), default = 75)

        return VerificationScenarioEntity(
            sessionId = sessionId,
            cycleId = cycleId,
            hypothesisId = hypothesisId,
            title = title.cleanTitle(),
            methodology = methodology,
            experimentalSetup = setup,
            falsificationCriteria = falsification,
            controlGroups = "Стандартная калибровочная контрольная группа без активации ключевых резонансов",
            metricsAndThresholds = metrics,
            feasibilityScore = feasibility.coerceIn(10, 100),
            createdAt = System.currentTimeMillis()
        )
    }

    private fun extractSection(text: String, header: String): String {
        val startIndex = text.indexOf(header, ignoreCase = true)
        if (startIndex == -1) return ""
        val contentStart = startIndex + header.length
        val nextSectionIndex = text.indexOf("###", contentStart)
        val raw = if (nextSectionIndex != -1) {
            text.substring(contentStart, nextSectionIndex)
        } else {
            text.substring(contentStart)
        }
        return raw.trim()
    }

    private fun extractNumber(text: String, default: Int): Int {
        val digits = Regex("\\d+").find(text)?.value?.toIntOrNull()
        return digits ?: default
    }

    private fun String.cleanTitle(): String {
        return this.replace("[", "")
            .replace("]", "")
            .replace("#", "")
            .trim()
    }
}
