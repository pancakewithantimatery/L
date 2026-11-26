package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MarkdownExporter
import com.example.data.ProgressorRepository
import com.example.data.SettingsManager
import com.example.data.VeniceModelInfo
import com.example.data.api.GeminiApiService
import com.example.data.api.OllamaApiService
import com.example.data.api.OpenRouterApiService
import com.example.data.api.VeniceApiService
import com.example.data.local.AgentMessageEntity
import com.example.data.local.AppDatabase
import com.example.data.local.EvolutionCycleEntity
import com.example.data.local.ResearchSessionEntity
import com.example.data.local.ScientificHypothesisEntity
import com.example.data.local.VerificationScenarioEntity
import com.example.domain.ProgressorEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ProgressorViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = ProgressorRepository(database.progressorDao())
    val settingsManager = SettingsManager(application)
    private val veniceApiService = VeniceApiService.create()
    private val geminiApiService = GeminiApiService.create()
    private val openRouterApiService = OpenRouterApiService.create()
    private val ollamaApiService = OllamaApiService.create()

    private val engine = ProgressorEngine(
        repository = repository,
        settingsManager = settingsManager,
        veniceApiService = veniceApiService,
        geminiApiService = geminiApiService,
        openRouterApiService = openRouterApiService,
        ollamaApiService = ollamaApiService
    )

    val allSessions: StateFlow<List<ResearchSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _activeSessionId = MutableStateFlow<Long?>(null)
    val activeSessionId: StateFlow<Long?> = _activeSessionId.asStateFlow()

    private val _activeSession = MutableStateFlow<ResearchSessionEntity?>(null)
    val activeSession: StateFlow<ResearchSessionEntity?> = _activeSession.asStateFlow()

    val currentCycles: StateFlow<List<EvolutionCycleEntity>> = _activeSessionId.flatMapLatest { id ->
        if (id != null) repository.getCyclesForSession(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMessages: StateFlow<List<AgentMessageEntity>> = _activeSessionId.flatMapLatest { id ->
        if (id != null) repository.getMessagesForSession(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentHypotheses: StateFlow<List<ScientificHypothesisEntity>> = _activeSessionId.flatMapLatest { id ->
        if (id != null) repository.getHypothesesForSession(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentScenarios: StateFlow<List<VerificationScenarioEntity>> = _activeSessionId.flatMapLatest { id ->
        if (id != null) repository.getScenariosForSession(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isCycleRunning = MutableStateFlow(false)
    val isCycleRunning: StateFlow<Boolean> = _isCycleRunning.asStateFlow()

    private val _progressState = MutableStateFlow<ProgressorEngine.ProgressState?>(null)
    val progressState: StateFlow<ProgressorEngine.ProgressState?> = _progressState.asStateFlow()

    // 1. Venice Models
    private val _availableVeniceModels = MutableStateFlow<List<VeniceModelInfo>>(SettingsManager.PRESET_VENICE_MODELS)
    val availableVeniceModels: StateFlow<List<VeniceModelInfo>> = _availableVeniceModels.asStateFlow()

    // 2. Gemini Models
    private val _availableGeminiModels = MutableStateFlow<List<VeniceModelInfo>>(SettingsManager.PRESET_GEMINI_MODELS)
    val availableGeminiModels: StateFlow<List<VeniceModelInfo>> = _availableGeminiModels.asStateFlow()

    // 3. OpenRouter Models
    private val _availableOpenRouterModels = MutableStateFlow<List<VeniceModelInfo>>(SettingsManager.PRESET_OPENROUTER_MODELS)
    val availableOpenRouterModels: StateFlow<List<VeniceModelInfo>> = _availableOpenRouterModels.asStateFlow()

    // 4. Ollama Models
    private val _availableOllamaModels = MutableStateFlow<List<VeniceModelInfo>>(SettingsManager.PRESET_OLLAMA_MODELS)
    val availableOllamaModels: StateFlow<List<VeniceModelInfo>> = _availableOllamaModels.asStateFlow()

    private val _isFetchingModels = MutableStateFlow(false)
    val isFetchingModels: StateFlow<Boolean> = _isFetchingModels.asStateFlow()

    // Combined all models across all 4 providers for the Council
    val allAvailableModels: StateFlow<List<VeniceModelInfo>> = combine(
        _availableVeniceModels,
        _availableGeminiModels,
        _availableOpenRouterModels,
        _availableOllamaModels
    ) { venice, gemini, router, ollama ->
        venice + gemini + router + ollama
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsManager.PRESET_MODELS)

    private val _apiKey = MutableStateFlow(settingsManager.getApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

    private val _geminiApiKey = MutableStateFlow(settingsManager.getGeminiApiKey())
    val geminiApiKey: StateFlow<String> = _geminiApiKey.asStateFlow()

    private val _openRouterApiKey = MutableStateFlow(settingsManager.getOpenRouterApiKey())
    val openRouterApiKey: StateFlow<String> = _openRouterApiKey.asStateFlow()

    private val _ollamaHost = MutableStateFlow(settingsManager.getOllamaHost())
    val ollamaHost: StateFlow<String> = _ollamaHost.asStateFlow()

    private val _selectedTheoristModel = MutableStateFlow(settingsManager.getTheoristModel())
    val selectedTheoristModel: StateFlow<String> = _selectedTheoristModel.asStateFlow()

    private val _selectedSkepticModel = MutableStateFlow(settingsManager.getSkepticModel())
    val selectedSkepticModel: StateFlow<String> = _selectedSkepticModel.asStateFlow()

    private val _selectedSynthesizerModel = MutableStateFlow(settingsManager.getSynthesizerModel())
    val selectedSynthesizerModel: StateFlow<String> = _selectedSynthesizerModel.asStateFlow()

    private val _isSimulationMode = MutableStateFlow(settingsManager.isSimulationModeEnabled())
    val isSimulationMode: StateFlow<Boolean> = _isSimulationMode.asStateFlow()

    init {
        // Automatically select the most recent session or create an initial starter if none exists
        viewModelScope.launch {
            repository.allSessions.collect { list ->
                if (_activeSessionId.value == null && list.isNotEmpty()) {
                    selectSession(list.first().id)
                } else if (list.isEmpty() && _activeSessionId.value == null) {
                    // Seed initial welcome research session
                    createSeedSession()
                }
            }
        }
        refreshAllProviders()
    }

    private suspend fun createSeedSession() {
        val defaultTopic = "Квантовая когерентность и топологический перенос в фотосинтетических комплексах"
        val defaultDomain = "Квантовая биофизика и нанобиотехнологии"
        val id = repository.createSession(
            topic = defaultTopic,
            domain = defaultDomain,
            theoristModel = "llama-3.3-70b",
            skepticModel = "deepseek-r1-671b",
            synthesizerModel = "qwen-2.5-72b"
        )
        selectSession(id)
        launchNextCycle(null)
    }

    fun selectSession(sessionId: Long) {
        _activeSessionId.value = sessionId
        viewModelScope.launch {
            val session = repository.getSessionById(sessionId)
            _activeSession.value = session
            if (session != null) {
                _selectedTheoristModel.value = session.activeTheoristModel
                _selectedSkepticModel.value = session.activeSkepticModel
                _selectedSynthesizerModel.value = session.activeSynthesizerModel
            }
        }
    }

    fun createNewSession(topic: String, domain: String) {
        viewModelScope.launch {
            val id = repository.createSession(
                topic = topic,
                domain = domain,
                theoristModel = _selectedTheoristModel.value,
                skepticModel = _selectedSkepticModel.value,
                synthesizerModel = _selectedSynthesizerModel.value
            )
            selectSession(id)
            launchNextCycle(null)
        }
    }

    fun launchNextCycle(userGuidance: String? = null) {
        val sessionId = _activeSessionId.value ?: return
        if (_isCycleRunning.value) return

        viewModelScope.launch(Dispatchers.IO) {
            _isCycleRunning.value = true
            val session = repository.getSessionById(sessionId) ?: return@launch
            val nextCycleNumber = session.totalCycles + 1

            val theorist = _selectedTheoristModel.value
            val skeptic = _selectedSkepticModel.value
            val synth = _selectedSynthesizerModel.value

            repository.updateSessionModels(sessionId, theorist, skeptic, synth)

            engine.executeCycle(
                sessionId = sessionId,
                cycleNumber = nextCycleNumber,
                theoristModel = theorist,
                skepticModel = skeptic,
                synthesizerModel = synth,
                userGuidance = userGuidance,
                onProgress = { state ->
                    _progressState.value = state
                }
            )

            // Refresh session info
            val updated = repository.getSessionById(sessionId)
            _activeSession.value = updated
            _isCycleRunning.value = false
        }
    }

    fun sendHumanIntervention(content: String) {
        val sessionId = _activeSessionId.value ?: return
        if (content.isBlank()) return

        viewModelScope.launch {
            val latestCycle = repository.getLatestCycle(sessionId)
            val cycleId = latestCycle?.id ?: 0L

            repository.insertMessage(
                AgentMessageEntity(
                    sessionId = sessionId,
                    cycleId = cycleId,
                    role = "HUMAN",
                    modelName = "Human-in-the-Loop",
                    agentTitle = "Оператор (Человек)",
                    content = content,
                    timestamp = System.currentTimeMillis(),
                    turnNumber = 4
                )
            )

            // Automatically trigger the next evolutionary cycle taking this guidance into account!
            launchNextCycle(userGuidance = content)
        }
    }

    fun saveApiKey(key: String) {
        settingsManager.setCustomApiKey(key)
        _apiKey.value = settingsManager.getApiKey()
        refreshModelsFromVenice()
    }

    fun saveGeminiApiKey(key: String) {
        settingsManager.setCustomGeminiApiKey(key)
        _geminiApiKey.value = settingsManager.getGeminiApiKey()
        refreshGeminiModels()
    }

    fun saveOpenRouterApiKey(key: String) {
        settingsManager.setCustomOpenRouterApiKey(key)
        _openRouterApiKey.value = settingsManager.getOpenRouterApiKey()
        refreshOpenRouterModels()
    }

    fun saveOllamaHost(host: String) {
        settingsManager.setOllamaHost(host)
        _ollamaHost.value = settingsManager.getOllamaHost()
        refreshOllamaModels()
    }

    fun toggleSimulationMode(enabled: Boolean) {
        settingsManager.setSimulationMode(enabled)
        _isSimulationMode.value = enabled
    }

    fun setModelsForNextCycle(theorist: String, skeptic: String, synthesizer: String) {
        _selectedTheoristModel.value = theorist
        _selectedSkepticModel.value = skeptic
        _selectedSynthesizerModel.value = synthesizer
        settingsManager.setTheoristModel(theorist)
        settingsManager.setSkepticModel(skeptic)
        settingsManager.setSynthesizerModel(synthesizer)

        val sessionId = _activeSessionId.value
        if (sessionId != null) {
            viewModelScope.launch {
                repository.updateSessionModels(sessionId, theorist, skeptic, synthesizer)
                _activeSession.value = repository.getSessionById(sessionId)
            }
        }
    }

    fun refreshAllProviders() {
        refreshModelsFromVenice()
        refreshGeminiModels()
        refreshOpenRouterModels()
        refreshOllamaModels()
    }

    fun refreshModelsFromVenice() {
        val key = settingsManager.getApiKey()
        if (key.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isFetchingModels.value = true
            try {
                val response = veniceApiService.listModels("Bearer $key")
                if (response.isSuccessful) {
                    val rawList = response.body()?.data ?: emptyList()
                    val dynamicList = rawList.map { dto ->
                        val existing = SettingsManager.PRESET_VENICE_MODELS.find { it.id == dto.id }
                        existing ?: VeniceModelInfo(
                            id = dto.id,
                            displayName = dto.id,
                            recommendedRole = "ALL",
                            description = "Venice.ai модель (${dto.type ?: "LLM"})",
                            tag = dto.type ?: "Venice",
                            provider = "VENICE"
                        )
                    }
                    if (dynamicList.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _availableVeniceModels.value = dynamicList
                        }
                    }
                }
            } catch (e: Exception) {
                // Keep presets on network/auth error
            } finally {
                _isFetchingModels.value = false
            }
        }
    }

    fun refreshGeminiModels() {
        val key = settingsManager.getGeminiApiKey()
        if (key.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isFetchingModels.value = true
            try {
                val response = geminiApiService.listModels(key)
                if (response.isSuccessful) {
                    val rawList = response.body()?.models ?: emptyList()
                    val filtered = rawList.filter {
                        it.supportedGenerationMethods?.contains("generateContent") == true
                    }
                    val dynamicList = filtered.map { dto ->
                        val cleanId = dto.name.removePrefix("models/")
                        val existing = SettingsManager.PRESET_GEMINI_MODELS.find { it.id == cleanId }
                        existing ?: VeniceModelInfo(
                            id = cleanId,
                            displayName = dto.displayName ?: cleanId,
                            recommendedRole = when {
                                cleanId.contains("pro", ignoreCase = true) -> "SKEPTIC"
                                cleanId.contains("flash", ignoreCase = true) -> "THEORIST"
                                else -> "SYNTHESIZER"
                            },
                            description = dto.description ?: "Google Gemini доступная по текущему API-ключу",
                            tag = "Google / Subscription Tier",
                            provider = "GEMINI"
                        )
                    }
                    if (dynamicList.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _availableGeminiModels.value = dynamicList
                        }
                    }
                }
            } catch (e: Exception) {
                // Keep presets on network/auth error
            } finally {
                _isFetchingModels.value = false
            }
        }
    }

    fun refreshOpenRouterModels() {
        val key = settingsManager.getOpenRouterApiKey()
        if (key.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isFetchingModels.value = true
            try {
                val response = openRouterApiService.listModels("Bearer $key")
                if (response.isSuccessful) {
                    val rawList = response.body()?.data ?: emptyList()
                    val dynamicList = rawList.take(30).map { dto ->
                        val existing = SettingsManager.PRESET_OPENROUTER_MODELS.find { it.id == dto.id }
                        existing ?: VeniceModelInfo(
                            id = dto.id,
                            displayName = dto.name ?: dto.id,
                            recommendedRole = when {
                                dto.id.contains("r1") || dto.id.contains("sonnet") -> "SKEPTIC"
                                dto.id.contains("4o") || dto.id.contains("llama") -> "THEORIST"
                                else -> "SYNTHESIZER"
                            },
                            description = dto.description ?: "OpenRouter.ai модель",
                            tag = "OpenRouter",
                            provider = "OPENROUTER"
                        )
                    }
                    if (dynamicList.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _availableOpenRouterModels.value = dynamicList
                        }
                    }
                }
            } catch (e: Exception) {
                // Keep presets on error
            } finally {
                _isFetchingModels.value = false
            }
        }
    }

    fun refreshOllamaModels() {
        val host = settingsManager.getOllamaHost()
        val url = "$host/api/tags"

        viewModelScope.launch(Dispatchers.IO) {
            _isFetchingModels.value = true
            try {
                val response = ollamaApiService.listModels(url)
                if (response.isSuccessful) {
                    val rawList = response.body()?.models ?: emptyList()
                    val dynamicList = rawList.map { dto ->
                        val modelId = "ollama/${dto.name}"
                        VeniceModelInfo(
                            id = modelId,
                            displayName = dto.name,
                            recommendedRole = when {
                                dto.name.contains("r1") -> "SKEPTIC"
                                dto.name.contains("llama") -> "THEORIST"
                                else -> "SYNTHESIZER"
                            },
                            description = "Локальная модель (${dto.details?.parameterSize ?: ""}) на сервере $host",
                            tag = "Ollama / Local",
                            provider = "OLLAMA"
                        )
                    }
                    if (dynamicList.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _availableOllamaModels.value = dynamicList
                        }
                    }
                }
            } catch (e: Exception) {
                // Keep presets on local connection error
            } finally {
                _isFetchingModels.value = false
            }
        }
    }

    fun runAiLabAnalysis(
        provider: String,
        model: String,
        prompt: String,
        onResult: (String?) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val res = when (provider) {
                "GEMINI" -> engine.runGeminiLabAnalysis(model, prompt)
                "OPENROUTER" -> engine.runOpenRouterLabAnalysis(model, prompt)
                "OLLAMA" -> engine.runOllamaLabAnalysis(model, prompt)
                else -> engine.runGeminiLabAnalysis(model, prompt)
            }
            withContext(Dispatchers.Main) {
                onResult(res.getOrNull())
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_activeSessionId.value == sessionId) {
                val remaining = allSessions.value.filter { it.id != sessionId }
                if (remaining.isNotEmpty()) {
                    selectSession(remaining.first().id)
                } else {
                    _activeSessionId.value = null
                    _activeSession.value = null
                }
            }
        }
    }

    fun generateMarkdownReport(): String {
        val session = _activeSession.value ?: return "# Нет активной сессии"
        return MarkdownExporter.generateMarkdown(
            session = session,
            cycles = currentCycles.value,
            messages = currentMessages.value,
            hypotheses = currentHypotheses.value,
            scenarios = currentScenarios.value
        )
    }

    fun exportMarkdownToFile(context: Context, uri: Uri): Boolean {
        val content = generateMarkdownReport()
        return MarkdownExporter.writeMarkdownToUri(context, uri, content)
    }
}
