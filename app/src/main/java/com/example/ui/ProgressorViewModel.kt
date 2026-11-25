package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ProgressorRepository
import com.example.data.SettingsManager
import com.example.data.VeniceModelInfo
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
    private val apiService = VeniceApiService.create()
    private val engine = ProgressorEngine(repository, settingsManager, apiService)

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

    private val _availableModels = MutableStateFlow<List<VeniceModelInfo>>(SettingsManager.PRESET_MODELS)
    val availableModels: StateFlow<List<VeniceModelInfo>> = _availableModels.asStateFlow()

    private val _isFetchingModels = MutableStateFlow(false)
    val isFetchingModels: StateFlow<Boolean> = _isFetchingModels.asStateFlow()

    private val _apiKey = MutableStateFlow(settingsManager.getApiKey())
    val apiKey: StateFlow<String> = _apiKey.asStateFlow()

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
        refreshModelsFromVenice()
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
        // Auto-run first introductory cycle for seamless first launch UX
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

    fun refreshModelsFromVenice() {
        val key = settingsManager.getApiKey()
        if (key.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _isFetchingModels.value = true
            try {
                val response = apiService.listModels("Bearer $key")
                if (response.isSuccessful) {
                    val rawList = response.body()?.data ?: emptyList()
                    val dynamicList = rawList.map { dto ->
                        val existing = SettingsManager.PRESET_MODELS.find { it.id == dto.id }
                        existing ?: VeniceModelInfo(
                            id = dto.id,
                            displayName = dto.id,
                            recommendedRole = "ALL",
                            description = "Venice.ai модель (${dto.type ?: "LLM"})",
                            tag = dto.type ?: "Venice"
                        )
                    }
                    if (dynamicList.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _availableModels.value = dynamicList
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

    fun generateFullSessionReport(): String {
        val session = _activeSession.value ?: return "Нет активной сессии"
        val cycles = currentCycles.value
        val hypotheses = currentHypotheses.value
        val scenarios = currentScenarios.value

        val sb = StringBuilder()
        sb.appendLine("==========================================")
        sb.appendLine("ПРОГРЕССОР: НАУЧНЫЙ ОТЧЕТ ИССЛЕДОВАНИЯ")
        sb.appendLine("==========================================")
        sb.appendLine("Тема: ${session.topic}")
        sb.appendLine("Область: ${session.domain}")
        sb.appendLine("Всего циклов эволюции: ${session.totalCycles}")
        sb.appendLine("Модели консорциума: Теоретик=${session.activeTheoristModel}, Скептик=${session.activeSkepticModel}, Синтезатор=${session.activeSynthesizerModel}")
        sb.appendLine()

        cycles.forEach { cycle ->
            sb.appendLine("------------------------------------------")
            sb.appendLine("ЭВОЛЮЦИОННЫЙ ЦИКЛ #${cycle.cycleNumber}")
            sb.appendLine("Модели: [${cycle.theoristModel} vs ${cycle.skepticModel} -> ${cycle.synthesizerModel}]")
            if (!cycle.userGuidance.isNullOrBlank()) {
                sb.appendLine("Вмешательство оператора: \"${cycle.userGuidance}\"")
            }
            sb.appendLine("Индекс жизнеспособности: ${cycle.viabilityScore}%")

            val hyp = hypotheses.find { it.cycleId == cycle.id }
            if (hyp != null) {
                sb.appendLine()
                sb.appendLine("[НАУЧНАЯ ГИПОТЕЗА]")
                sb.appendLine("Название: ${hyp.title}")
                sb.appendLine("Тезис: ${hyp.thesis}")
                sb.appendLine("Механизм: ${hyp.mechanism}")
                sb.appendLine("Теоретический базис: ${hyp.theoreticalBasis}")
                sb.appendLine("Оценка новизны: ${hyp.noveltyScore}%, Уверенность: ${hyp.confidenceScore}%")
            }

            val scen = scenarios.find { it.cycleId == cycle.id }
            if (scen != null) {
                sb.appendLine()
                sb.appendLine("[СЦЕНАРИЙ ВЕРИФИКАЦИИ И ЭКСПЕРИМЕНТА]")
                sb.appendLine("Протокол: ${scen.title}")
                sb.appendLine("Методология: ${scen.methodology}")
                sb.appendLine("Сетап: ${scen.experimentalSetup}")
                sb.appendLine("Критерий фальсификации (Поппер): ${scen.falsificationCriteria}")
                sb.appendLine("Метрики: ${scen.metricsAndThresholds}")
                sb.appendLine("Реализуемость: ${scen.feasibilityScore}%")
            }
            sb.appendLine()
        }

        return sb.toString()
    }
}
