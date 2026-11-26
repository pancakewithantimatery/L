package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.KeepBufferExporter
import com.example.data.KeepBufferPayload
import com.example.data.LatexExporter
import com.example.data.MarkdownExporter
import com.example.data.ProgressorRepository
import com.example.data.SettingsManager
import com.example.data.VeniceModelInfo
import com.example.data.api.ArxivApiService
import com.example.data.api.ArxivPaper
import com.example.data.api.GeminiApiService
import com.example.data.api.OllamaApiService
import com.example.data.api.OpenRouterApiService
import com.example.data.api.VeniceApiService
import com.example.data.local.AgentMessageEntity
import com.example.data.local.AppDatabase
import com.example.data.local.CustomAgentRoleEntity
import com.example.data.local.EvolutionCycleEntity
import com.example.data.local.ResearchSessionEntity
import com.example.data.local.ScientificCitationEntity
import com.example.data.local.ScientificHypothesisEntity
import com.example.data.local.VerificationScenarioEntity
import com.example.domain.DebateTtsManager
import com.example.domain.ProgressorEngine
import com.example.domain.TtsPlaybackState
import com.example.domain.VoiceCommand
import com.example.domain.VoiceCommandManager
import com.example.domain.VoiceCommandParser
import com.example.domain.VoiceControlState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val arxivApiService = ArxivApiService()
    private val ttsManager = DebateTtsManager(application)

    private val _voiceCommands = MutableSharedFlow<VoiceCommand>(extraBufferCapacity = 8)
    val voiceCommands: SharedFlow<VoiceCommand> = _voiceCommands.asSharedFlow()

    private val voiceCommandManager = VoiceCommandManager(
        context = application,
        onBeforeStartListening = {
            if (ttsManager.state.value.isPlaying) {
                ttsManager.pause()
            }
        },
        onCommandRecognized = { command, _ ->
            _voiceCommands.tryEmit(command)
        }
    )

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

    val customRoles: StateFlow<List<CustomAgentRoleEntity>> = repository.allCustomRoles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val ttsState: StateFlow<TtsPlaybackState> = ttsManager.state
    val voiceState: StateFlow<VoiceControlState> = voiceCommandManager.state

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

    val currentCitations: StateFlow<List<ScientificCitationEntity>> = _activeSessionId.flatMapLatest { id ->
        if (id != null) repository.getCitationsForSession(id) else flowOf(emptyList())
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // arXiv Search State
    private val _arxivSearchResults = MutableStateFlow<List<ArxivPaper>>(emptyList())
    val arxivSearchResults: StateFlow<List<ArxivPaper>> = _arxivSearchResults.asStateFlow()

    private val _isSearchingArxiv = MutableStateFlow(false)
    val isSearchingArxiv: StateFlow<Boolean> = _isSearchingArxiv.asStateFlow()

    private val _arxivError = MutableStateFlow<String?>(null)
    val arxivError: StateFlow<String?> = _arxivError.asStateFlow()

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

    // --- XREAL Air 2 Pro Spatial / Multi-Window States ---
    val xrealSpatialMode: StateFlow<Boolean> = settingsManager.xrealSpatialModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val xrealAutoDetect: StateFlow<Boolean> = settingsManager.xrealAutoDetectFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val xrealZeroGravityLock: StateFlow<Boolean> = settingsManager.xrealZeroGravityLockFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val xrealOverscanDp: StateFlow<Int> = settingsManager.xrealOverscanDpFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 28)

    val xrealEyesFreeFeedback: StateFlow<Boolean> = settingsManager.xrealEyesFreeFeedbackFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    init {
        // Collect reactive DataStore flows
        viewModelScope.launch {
            settingsManager.veniceApiKeyFlow.collect { _apiKey.value = it }
        }
        viewModelScope.launch {
            settingsManager.geminiApiKeyFlow.collect { _geminiApiKey.value = it }
        }
        viewModelScope.launch {
            settingsManager.openRouterApiKeyFlow.collect { _openRouterApiKey.value = it }
        }
        viewModelScope.launch {
            settingsManager.ollamaHostFlow.collect { _ollamaHost.value = it }
        }
        viewModelScope.launch {
            settingsManager.simulationModeFlow.collect { _isSimulationMode.value = it }
        }
        viewModelScope.launch(Dispatchers.IO) {
            repository.ensureDefaultCustomRolesSeeded()
        }

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
        viewModelScope.launch {
            settingsManager.setVeniceApiKey(key)
            refreshModelsFromVenice()
        }
    }

    fun saveGeminiApiKey(key: String) {
        viewModelScope.launch {
            settingsManager.setGeminiApiKey(key)
            refreshGeminiModels()
        }
    }

    fun saveOpenRouterApiKey(key: String) {
        viewModelScope.launch {
            settingsManager.setOpenRouterApiKey(key)
            refreshOpenRouterModels()
        }
    }

    fun saveOllamaHost(host: String) {
        viewModelScope.launch {
            settingsManager.setOllamaHost(host)
            refreshOllamaModels()
        }
    }

    fun toggleSimulationMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setSimulationMode(enabled)
        }
    }

    fun testVeniceConnection(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = veniceApiService.listModels("Bearer $key")
                val count = res.body()?.data?.size ?: 0
                withContext(Dispatchers.Main) {
                    if (res.isSuccessful && count > 0) {
                        onResult(true, "Успешно: доступно $count моделей Venice.ai")
                    } else {
                        onResult(false, "Ошибка ответа Venice: HTTP ${res.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Ошибка подключения: ${e.localizedMessage ?: "Network error"}")
                }
            }
        }
    }

    fun testOpenRouterConnection(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = openRouterApiService.listModels("Bearer $key")
                val count = res.body()?.data?.size ?: 0
                withContext(Dispatchers.Main) {
                    if (res.isSuccessful && count > 0) {
                        onResult(true, "Успешно: доступно $count моделей OpenRouter")
                    } else {
                        onResult(false, "Ошибка OpenRouter: HTTP ${res.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Ошибка подключения: ${e.localizedMessage ?: "Network error"}")
                }
            }
        }
    }

    fun testOllamaConnection(hostUrl: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val clean = hostUrl.trim().removeSuffix("/")
                val versionRes = ollamaApiService.checkVersion("$clean/api/version")
                val tagsRes = ollamaApiService.listModels("$clean/api/tags")
                withContext(Dispatchers.Main) {
                    if (tagsRes.isSuccessful) {
                        val count = tagsRes.body()?.models?.size ?: 0
                        val version = versionRes.body()?.version ?: "OK"
                        onResult(true, "Ollama онлайн (v$version): $count локальных моделей")
                    } else {
                        onResult(false, "Сервер Ollama вернул ошибку HTTP ${tagsRes.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Недоступен: ${e.localizedMessage ?: "Connection refused"}")
                }
            }
        }
    }

    fun testGeminiConnection(key: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val res = geminiApiService.listModels(key)
                val count = res.body()?.models?.size ?: 0
                withContext(Dispatchers.Main) {
                    if (res.isSuccessful && count > 0) {
                        onResult(true, "Успешно: доступно $count моделей Gemini")
                    } else {
                        onResult(false, "Ошибка Gemini API: HTTP ${res.code()}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Ошибка подключения: ${e.localizedMessage ?: "Network error"}")
                }
            }
        }
    }

    fun setModelsForNextCycle(theorist: String, skeptic: String, synthesizer: String) {
        _selectedTheoristModel.value = theorist
        _selectedSkepticModel.value = skeptic
        _selectedSynthesizerModel.value = synthesizer
        viewModelScope.launch {
            settingsManager.setTheoristModel(theorist)
            settingsManager.setSkepticModel(skeptic)
            settingsManager.setSynthesizerModel(synthesizer)
        }

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

    // --- arXiv & DOI Search & Citations ---
    fun searchArxiv(query: String) {
        if (query.isBlank()) return
        viewModelScope.launch {
            _isSearchingArxiv.value = true
            _arxivError.value = null
            val res = arxivApiService.searchPapers(query, maxResults = 10)
            res.onSuccess { papers ->
                _arxivSearchResults.value = papers
                if (papers.isEmpty()) {
                    _arxivError.value = "По запросу ничего не найдено. Попробуйте англоязычные термины."
                }
            }.onFailure { err ->
                _arxivError.value = err.localizedMessage ?: "Ошибка подключения к arXiv"
            }
            _isSearchingArxiv.value = false
        }
    }

    fun pinArxivPaper(paper: ArxivPaper) {
        val sessionId = _activeSessionId.value ?: return
        if (currentCitations.value.any { it.arxivId == paper.arxivId }) return
        viewModelScope.launch(Dispatchers.IO) {
            repository.insertCitation(
                ScientificCitationEntity(
                    sessionId = sessionId,
                    arxivId = paper.arxivId,
                    doi = paper.doi,
                    title = paper.title,
                    authors = paper.authors.joinToString(", "),
                    summary = paper.summary,
                    publishedYear = paper.publishedYear,
                    pdfUrl = paper.pdfUrl
                )
            )
        }
    }

    fun deleteCitation(citationId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCitation(citationId)
        }
    }

    fun injectPaperIntoCycle(paper: ArxivPaper) {
        val doiSuffix = if (!paper.doi.isNullOrBlank()) ", DOI: ${paper.doi}" else ""
        val guidance = "Учти реальную публикацию arXiv:${paper.arxivId}$doiSuffix — «${paper.title}» (${paper.publishedYear}). Сопоставь нашу гипотезу с её выводами."
        sendHumanIntervention(guidance)
    }

    // --- Custom Council Roles ---
    fun saveCustomRole(role: CustomAgentRoleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveCustomRole(role)
        }
    }

    fun toggleCustomRole(role: CustomAgentRoleEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.toggleCustomRoleActivation(role)
        }
    }

    fun deleteCustomRole(roleId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomRole(roleId)
        }
    }

    // --- Debate TTS Podcast Controls ---
    fun toggleTtsPlayback(startMessageId: Long? = null) {
        ttsManager.playOrPause(currentMessages.value, startMessageId)
    }

    fun ttsNext() {
        ttsManager.next(currentMessages.value)
    }

    fun ttsPrevious() {
        ttsManager.previous(currentMessages.value)
    }

    fun ttsCycleSpeed() {
        ttsManager.cycleSpeed()
    }

    fun ttsStop() {
        ttsManager.stop()
    }

    // --- XREAL Air 2 Pro Spatial / Multi-Window Controls ---
    fun setXrealSpatialMode(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setXrealSpatialMode(enabled)
        }
    }

    fun setXrealAutoDetect(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setXrealAutoDetect(enabled)
        }
    }

    fun setXrealZeroGravityLock(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setXrealZeroGravityLock(enabled)
        }
    }

    fun setXrealOverscanDp(dpValue: Int) {
        viewModelScope.launch {
            settingsManager.setXrealOverscanDp(dpValue)
        }
    }

    fun setXrealEyesFreeFeedback(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setXrealEyesFreeFeedback(enabled)
        }
    }

    fun announceEyesFree(phrase: String, forceInterrupt: Boolean = false) {
        if (xrealEyesFreeFeedback.value) {
            ttsManager.announceConfirmation(phrase, forceInterrupt)
        }
    }

    // --- Hands-Free Voice Control (Android SpeechRecognizer) ---
    fun toggleVoiceListening() {
        voiceCommandManager.toggleSingleListening()
    }

    fun setHandsFreeVoiceMode(enabled: Boolean) {
        voiceCommandManager.setHandsFreeMode(enabled)
    }

    fun reportVoicePermissionDenied() {
        voiceCommandManager.reportPermissionDenied()
    }

    fun stopVoiceListening() {
        voiceCommandManager.stopAll()
    }

    fun clearVoiceStatusBanner() {
        voiceCommandManager.clearStatusBanner()
    }

    fun dispatchVoiceSamplePhrase(phrase: String) {
        val parsed = VoiceCommandParser.parse(phrase)
        _voiceCommands.tryEmit(parsed)
    }

    /**
     * Builds a clean cross-posting summary (latest thesis, final synthesis, Popper criterion, arXiv refs)
     * ready to paste directly into neighboring spatial windows (OpenRouter, Venice, Gemini).
     */
    fun buildCrossPostSynthesisText(): String {
        val session = _activeSession.value ?: return ""
        val latestHyp = currentHypotheses.value.firstOrNull()
        val latestScen = currentScenarios.value.firstOrNull()
        val latestSynthMsg = currentMessages.value.lastOrNull { it.role == "SYNTHESIZER" }
            ?: currentMessages.value.lastOrNull()
        val citations = currentCitations.value

        return buildString {
            appendLine("=== ПРОГРЕССОР | КРОСС-ПОСТИНГ СИНТЕЗА ===")
            appendLine("Тема: ${session.topic} (${session.domain}) • Цикл #${session.totalCycles}")
            if (latestHyp != null) {
                appendLine()
                appendLine("ГИПОТЕЗА: ${latestHyp.title}")
                appendLine("ТЕЗИС: ${latestHyp.thesis}")
                appendLine("МЕХАНИЗМ: ${latestHyp.mechanism}")
            }
            if (latestScen != null) {
                appendLine()
                appendLine("КРИТЕРИЙ ФАЛЬСИФИКАЦИИ (ПОППЕР): ${latestScen.falsificationCriteria}")
            }
            if (latestSynthMsg != null) {
                appendLine()
                appendLine("ИТОГОВЫЙ СИНТЕЗ (${latestSynthMsg.modelName}):")
                appendLine(latestSynthMsg.content)
            }
            if (citations.isNotEmpty()) {
                appendLine()
                appendLine("ИСТОЧНИКИ arXiv/DOI:")
                citations.forEach { cit ->
                    val doiPart = if (!cit.doi.isNullOrBlank()) " | DOI:${cit.doi}" else ""
                    appendLine("- ${cit.title} (${cit.publishedYear}) arXiv:${cit.arxivId}$doiPart")
                }
            }
        }.trim()
    }

    /**
     * Generates a high-density operational buffer summary of the current session
     * tailored for Google Keep (Context/Thesis, Verified Facts + arXiv, Checklist).
     */
    fun buildKeepBufferPayload(): KeepBufferPayload? {
        val session = _activeSession.value ?: return null
        return KeepBufferExporter.generateSessionBuffer(
            session = session,
            cycles = currentCycles.value,
            messages = currentMessages.value,
            hypotheses = currentHypotheses.value,
            scenarios = currentScenarios.value,
            citations = currentCitations.value
        )
    }

    /**
     * Generates a compact Google Keep operational buffer note from a specific Arena message.
     */
    fun buildMessageKeepBufferPayload(message: AgentMessageEntity): KeepBufferPayload {
        return KeepBufferExporter.generateMessageBuffer(
            session = _activeSession.value,
            message = message,
            citations = currentCitations.value
        )
    }

    fun generateMarkdownReport(): String {
        val session = _activeSession.value ?: return "# Нет активной сессии"
        return MarkdownExporter.generateMarkdown(
            session = session,
            cycles = currentCycles.value,
            messages = currentMessages.value,
            hypotheses = currentHypotheses.value,
            scenarios = currentScenarios.value,
            citations = currentCitations.value
        )
    }

    fun exportMarkdownToFile(context: Context, uri: Uri): Boolean {
        val content = generateMarkdownReport()
        return MarkdownExporter.writeMarkdownToUri(context, uri, content)
    }

    fun generateLatexReport(): String {
        val session = _activeSession.value ?: return "% Нет активной сессии"
        return LatexExporter.generateLatex(
            session = session,
            cycles = currentCycles.value,
            messages = currentMessages.value,
            hypotheses = currentHypotheses.value,
            scenarios = currentScenarios.value,
            citations = currentCitations.value
        )
    }

    fun exportLatexToFile(context: Context, uri: Uri): Boolean {
        val content = generateLatexReport()
        return LatexExporter.writeLatexToUri(context, uri, content)
    }

    override fun onCleared() {
        super.onCleared()
        voiceCommandManager.destroy()
        ttsManager.shutdown()
    }
}
