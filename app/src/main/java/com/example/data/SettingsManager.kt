package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.BuildConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import java.io.IOException

// Extension property for DataStore instance per application context
val Context.progressorDataStore: DataStore<Preferences> by preferencesDataStore(name = "progressor_datastore_settings")

data class VeniceModelInfo(
    val id: String,
    val displayName: String,
    val recommendedRole: String, // "THEORIST", "SKEPTIC", "SYNTHESIZER", "ALL"
    val description: String,
    val tag: String = "70B+",
    val provider: String = "VENICE" // "VENICE", "GEMINI", "OPENROUTER", "OLLAMA"
)

class SettingsManager(private val context: Context) {

    companion object {
        val KEY_VENICE_API_KEY = stringPreferencesKey("venice_api_key")
        val KEY_GEMINI_API_KEY = stringPreferencesKey("gemini_api_key")
        val KEY_OPENROUTER_API_KEY = stringPreferencesKey("openrouter_api_key")
        val KEY_OLLAMA_HOST_URL = stringPreferencesKey("ollama_host_url")
        val KEY_SIMULATION_MODE = booleanPreferencesKey("simulation_mode")
        val KEY_THEORIST_MODEL = stringPreferencesKey("theorist_model")
        val KEY_SKEPTIC_MODEL = stringPreferencesKey("skeptic_model")
        val KEY_SYNTHESIZER_MODEL = stringPreferencesKey("synthesizer_model")

        // XREAL Air 2 Pro Spatial / Immersion Mode Preferences
        val KEY_XREAL_SPATIAL_MODE = booleanPreferencesKey("xreal_spatial_mode")
        val KEY_XREAL_AUTO_DETECT = booleanPreferencesKey("xreal_auto_detect")
        val KEY_XREAL_ZERO_GRAVITY_LOCK = booleanPreferencesKey("xreal_zero_gravity_lock")
        val KEY_XREAL_OVERSCAN_DP = intPreferencesKey("xreal_overscan_dp")
        val KEY_XREAL_EYES_FREE_FEEDBACK = booleanPreferencesKey("xreal_eyes_free_feedback")

        val PRESET_VENICE_MODELS = listOf(
            VeniceModelInfo(
                id = "llama-3.3-70b",
                displayName = "Llama 3.3 70B",
                recommendedRole = "THEORIST",
                description = "Глубокая междисциплинарная эрудиция, выдвижение смелых фундаментальных теорий",
                tag = "Meta / 70B",
                provider = "VENICE"
            ),
            VeniceModelInfo(
                id = "deepseek-r1-671b",
                displayName = "DeepSeek R1 (Reasoning)",
                recommendedRole = "SKEPTIC",
                description = "Пошаговое строгое рассуждение, выявление парадоксов и попперовская фальсификация",
                tag = "DeepSeek / 671B",
                provider = "VENICE"
            ),
            VeniceModelInfo(
                id = "qwen-2.5-72b",
                displayName = "Qwen 2.5 72B",
                recommendedRole = "SYNTHESIZER",
                description = "Исключительные математические выкладки, прикладной инженерный синтез",
                tag = "Alibaba / 72B",
                provider = "VENICE"
            ),
            VeniceModelInfo(
                id = "dolphin-2.9.2-qwen2-72b",
                displayName = "Dolphin 2.9.2 Qwen2",
                recommendedRole = "THEORIST",
                description = "Неограниченное научное творчество, нестандартные эвристики и догадки",
                tag = "Cognitive / 72B",
                provider = "VENICE"
            ),
            VeniceModelInfo(
                id = "mistral-nemo",
                displayName = "Mistral NeMo",
                recommendedRole = "SYNTHESIZER",
                description = "Высокая плотность смыслов, строгая протоколизация и формализация сценариев",
                tag = "Mistral / 12B",
                provider = "VENICE"
            ),
            VeniceModelInfo(
                id = "llama-3.1-405b",
                displayName = "Llama 3.1 405B",
                recommendedRole = "ALL",
                description = "Флагманский масштаб познания для сложнейших многофакторных проблем",
                tag = "Meta / 405B",
                provider = "VENICE"
            ),
            VeniceModelInfo(
                id = "hermes-3-llama-3.1-405b",
                displayName = "Hermes 3 405B",
                recommendedRole = "SKEPTIC",
                description = "Продвинутое агентное рассуждение и дизайн экспериментальных сетапов",
                tag = "Nous / 405B",
                provider = "VENICE"
            ),
            VeniceModelInfo(
                id = "deepseek-v3",
                displayName = "DeepSeek V3",
                recommendedRole = "ALL",
                description = "Универсальная архитектура MoE высокой точности",
                tag = "DeepSeek / MoE",
                provider = "VENICE"
            )
        )

        val PRESET_GEMINI_MODELS = listOf(
            VeniceModelInfo(
                id = "gemini-2.5-flash",
                displayName = "Gemini 2.5 Flash",
                recommendedRole = "THEORIST",
                description = "Сверхбыстрая генерация с адаптивным мышлением и широким контекстом",
                tag = "Google / Default Tier",
                provider = "GEMINI"
            ),
            VeniceModelInfo(
                id = "gemini-2.5-pro",
                displayName = "Gemini 2.5 Pro",
                recommendedRole = "SKEPTIC",
                description = "Глубокий пошаговый анализ сложных научных парадоксов и критика",
                tag = "Google / Pro Tier",
                provider = "GEMINI"
            ),
            VeniceModelInfo(
                id = "gemini-2.0-flash",
                displayName = "Gemini 2.0 Flash",
                recommendedRole = "SYNTHESIZER",
                description = "Оптимизированная модель для мгновенного многофакторного синтеза",
                tag = "Google / Flash",
                provider = "GEMINI"
            ),
            VeniceModelInfo(
                id = "gemini-1.5-pro",
                displayName = "Gemini 1.5 Pro",
                recommendedRole = "ALL",
                description = "Миллионный контекст для обработки обширных научных баз знаний",
                tag = "Google / 1.5 Pro",
                provider = "GEMINI"
            ),
            VeniceModelInfo(
                id = "gemini-1.5-flash",
                displayName = "Gemini 1.5 Flash",
                recommendedRole = "SYNTHESIZER",
                description = "Экономичная и легковесная модель для рутинных исследовательских фаз",
                tag = "Google / 1.5 Flash",
                provider = "GEMINI"
            )
        )

        val PRESET_OPENROUTER_MODELS = listOf(
            VeniceModelInfo(
                id = "anthropic/claude-3.5-sonnet",
                displayName = "Claude 3.5 Sonnet",
                recommendedRole = "SKEPTIC",
                description = "Исключительная научная логика, академическая критика и код",
                tag = "Anthropic / Router",
                provider = "OPENROUTER"
            ),
            VeniceModelInfo(
                id = "openai/gpt-4o",
                displayName = "GPT-4o",
                recommendedRole = "THEORIST",
                description = "Флагманское междисциплинарное рассуждение и концептуализация",
                tag = "OpenAI / Router",
                provider = "OPENROUTER"
            ),
            VeniceModelInfo(
                id = "deepseek/deepseek-r1",
                displayName = "DeepSeek R1",
                recommendedRole = "SKEPTIC",
                description = "Глубокое логическое рассуждение (Open Weights) через OpenRouter",
                tag = "DeepSeek / Router",
                provider = "OPENROUTER"
            ),
            VeniceModelInfo(
                id = "meta-llama/llama-3.3-70b-instruct",
                displayName = "Llama 3.3 70B Instruct",
                recommendedRole = "THEORIST",
                description = "Открытая 70B модель с мощным рассуждением",
                tag = "Meta / Router",
                provider = "OPENROUTER"
            ),
            VeniceModelInfo(
                id = "qwen/qwen-2.5-72b-instruct",
                displayName = "Qwen 2.5 72B Instruct",
                recommendedRole = "SYNTHESIZER",
                description = "Высокие STEM показатели для расчетов и синтеза",
                tag = "Qwen / Router",
                provider = "OPENROUTER"
            )
        )

        val PRESET_OLLAMA_MODELS = listOf(
            VeniceModelInfo(
                id = "ollama/llama3.2",
                displayName = "Llama 3.2 (Local)",
                recommendedRole = "THEORIST",
                description = "Локальная модель на вашем сервере Ollama",
                tag = "Ollama / Local",
                provider = "OLLAMA"
            ),
            VeniceModelInfo(
                id = "ollama/deepseek-r1:8b",
                displayName = "DeepSeek R1 8B (Local)",
                recommendedRole = "SKEPTIC",
                description = "Локальное пошаговое рассуждение без интернета",
                tag = "Ollama / Local",
                provider = "OLLAMA"
            ),
            VeniceModelInfo(
                id = "ollama/qwen2.5:7b",
                displayName = "Qwen 2.5 7B (Local)",
                recommendedRole = "SYNTHESIZER",
                description = "Математика и прикладной синтез на локальном железе",
                tag = "Ollama / Local",
                provider = "OLLAMA"
            ),
            VeniceModelInfo(
                id = "ollama/mistral",
                displayName = "Mistral 7B (Local)",
                recommendedRole = "ALL",
                description = "Компактная быстрая локальная модель",
                tag = "Ollama / Local",
                provider = "OLLAMA"
            )
        )

        // Combined default models across all providers
        val PRESET_MODELS = PRESET_VENICE_MODELS + PRESET_GEMINI_MODELS + PRESET_OPENROUTER_MODELS + PRESET_OLLAMA_MODELS
    }

    private fun getBuildConfigVeniceKey(): String {
        val key = runCatching { BuildConfig.VENICE_API_KEY }.getOrNull()?.trim()
        return if (!key.isNullOrEmpty() && key != "PLACEHOLDER_VENICE_KEY") key else ""
    }

    private fun getBuildConfigGeminiKey(): String {
        val key = runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()?.trim()
        return if (!key.isNullOrEmpty() && key != "DEFAULT_GEMINI_KEY") key else ""
    }

    private fun getBuildConfigOpenRouterKey(): String {
        val key = runCatching { BuildConfig.OPENROUTER_API_KEY }.getOrNull()?.trim()
        return if (!key.isNullOrEmpty() && key != "PLACEHOLDER_OPENROUTER_KEY") key else ""
    }

    private fun getBuildConfigOllamaHost(): String {
        val host = runCatching { BuildConfig.OLLAMA_HOST_URL }.getOrNull()?.trim()
        return if (!host.isNullOrEmpty()) host.removeSuffix("/") else "http://10.0.2.2:11434"
    }

    // --- Reactive DataStore Flows ---

    val veniceApiKeyFlow: Flow<String> = context.progressorDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val saved = prefs[KEY_VENICE_API_KEY]?.trim()
            if (!saved.isNullOrEmpty()) saved else getBuildConfigVeniceKey()
        }

    val geminiApiKeyFlow: Flow<String> = context.progressorDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val saved = prefs[KEY_GEMINI_API_KEY]?.trim()
            if (!saved.isNullOrEmpty()) saved else getBuildConfigGeminiKey()
        }

    val openRouterApiKeyFlow: Flow<String> = context.progressorDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val saved = prefs[KEY_OPENROUTER_API_KEY]?.trim()
            if (!saved.isNullOrEmpty()) saved else getBuildConfigOpenRouterKey()
        }

    val ollamaHostFlow: Flow<String> = context.progressorDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs ->
            val saved = prefs[KEY_OLLAMA_HOST_URL]?.trim()
            if (!saved.isNullOrEmpty()) saved.removeSuffix("/") else getBuildConfigOllamaHost()
        }

    val simulationModeFlow: Flow<Boolean> = context.progressorDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[KEY_SIMULATION_MODE] ?: false }

    val theoristModelFlow: Flow<String> = context.progressorDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[KEY_THEORIST_MODEL] ?: "llama-3.3-70b" }

    val skepticModelFlow: Flow<String> = context.progressorDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[KEY_SKEPTIC_MODEL] ?: "deepseek-r1-671b" }

    val synthesizerModelFlow: Flow<String> = context.progressorDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[KEY_SYNTHESIZER_MODEL] ?: "qwen-2.5-72b" }

    // --- XREAL Spatial / Immersion Mode Reactive Flows ---

    val xrealSpatialModeFlow: Flow<Boolean> = context.progressorDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[KEY_XREAL_SPATIAL_MODE] ?: false }

    val xrealAutoDetectFlow: Flow<Boolean> = context.progressorDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[KEY_XREAL_AUTO_DETECT] ?: true }

    val xrealZeroGravityLockFlow: Flow<Boolean> = context.progressorDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[KEY_XREAL_ZERO_GRAVITY_LOCK] ?: true }

    val xrealOverscanDpFlow: Flow<Int> = context.progressorDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> (prefs[KEY_XREAL_OVERSCAN_DP] ?: 28).coerceIn(24, 32) }

    val xrealEyesFreeFeedbackFlow: Flow<Boolean> = context.progressorDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[KEY_XREAL_EYES_FREE_FEEDBACK] ?: true }

    // --- Suspend DataStore Mutations ---

    suspend fun setVeniceApiKey(key: String) {
        context.progressorDataStore.edit { prefs ->
            prefs[KEY_VENICE_API_KEY] = key.trim()
        }
    }

    suspend fun setGeminiApiKey(key: String) {
        context.progressorDataStore.edit { prefs ->
            prefs[KEY_GEMINI_API_KEY] = key.trim()
        }
    }

    suspend fun setOpenRouterApiKey(key: String) {
        context.progressorDataStore.edit { prefs ->
            prefs[KEY_OPENROUTER_API_KEY] = key.trim()
        }
    }

    suspend fun setOllamaHost(url: String) {
        val clean = url.trim().removeSuffix("/")
        context.progressorDataStore.edit { prefs ->
            prefs[KEY_OLLAMA_HOST_URL] = clean
        }
    }

    suspend fun setSimulationMode(enabled: Boolean) {
        context.progressorDataStore.edit { prefs ->
            prefs[KEY_SIMULATION_MODE] = enabled
        }
    }

    suspend fun setTheoristModel(model: String) {
        context.progressorDataStore.edit { prefs ->
            prefs[KEY_THEORIST_MODEL] = model
        }
    }

    suspend fun setSkepticModel(model: String) {
        context.progressorDataStore.edit { prefs ->
            prefs[KEY_SKEPTIC_MODEL] = model
        }
    }

    suspend fun setSynthesizerModel(model: String) {
        context.progressorDataStore.edit { prefs ->
            prefs[KEY_SYNTHESIZER_MODEL] = model
        }
    }

    suspend fun setXrealSpatialMode(enabled: Boolean) {
        context.progressorDataStore.edit { prefs ->
            prefs[KEY_XREAL_SPATIAL_MODE] = enabled
        }
    }

    suspend fun setXrealAutoDetect(enabled: Boolean) {
        context.progressorDataStore.edit { prefs ->
            prefs[KEY_XREAL_AUTO_DETECT] = enabled
        }
    }

    suspend fun setXrealZeroGravityLock(enabled: Boolean) {
        context.progressorDataStore.edit { prefs ->
            prefs[KEY_XREAL_ZERO_GRAVITY_LOCK] = enabled
        }
    }

    suspend fun setXrealOverscanDp(dpValue: Int) {
        context.progressorDataStore.edit { prefs ->
            prefs[KEY_XREAL_OVERSCAN_DP] = dpValue.coerceIn(24, 32)
        }
    }

    suspend fun setXrealEyesFreeFeedback(enabled: Boolean) {
        context.progressorDataStore.edit { prefs ->
            prefs[KEY_XREAL_EYES_FREE_FEEDBACK] = enabled
        }
    }

    // --- Synchronous Getters (with runBlocking & DataStore first()) for engine / startup compatibility ---

    fun getApiKey(): String = runCatching {
        runBlocking { veniceApiKeyFlow.first() }
    }.getOrDefault(getBuildConfigVeniceKey())

    fun setCustomApiKey(key: String) {
        runBlocking { setVeniceApiKey(key) }
    }

    fun isApiKeyConfigured(): Boolean = getApiKey().isNotEmpty()

    fun getGeminiApiKey(): String = runCatching {
        runBlocking { geminiApiKeyFlow.first() }
    }.getOrDefault(getBuildConfigGeminiKey())

    fun setCustomGeminiApiKey(key: String) {
        runBlocking { setGeminiApiKey(key) }
    }

    fun isGeminiKeyConfigured(): Boolean = getGeminiApiKey().isNotEmpty()

    fun getOpenRouterApiKey(): String = runCatching {
        runBlocking { openRouterApiKeyFlow.first() }
    }.getOrDefault(getBuildConfigOpenRouterKey())

    fun setCustomOpenRouterApiKey(key: String) {
        runBlocking { setOpenRouterApiKey(key) }
    }

    fun isOpenRouterKeyConfigured(): Boolean = getOpenRouterApiKey().isNotEmpty()

    fun getOllamaHost(): String = runCatching {
        runBlocking { ollamaHostFlow.first() }
    }.getOrDefault(getBuildConfigOllamaHost())

    fun isSimulationModeEnabled(): Boolean = runCatching {
        runBlocking { simulationModeFlow.first() }
    }.getOrDefault(false)

    fun getTheoristModel(): String = runCatching {
        runBlocking { theoristModelFlow.first() }
    }.getOrDefault("llama-3.3-70b")

    fun getSkepticModel(): String = runCatching {
        runBlocking { skepticModelFlow.first() }
    }.getOrDefault("deepseek-r1-671b")

    fun getSynthesizerModel(): String = runCatching {
        runBlocking { synthesizerModelFlow.first() }
    }.getOrDefault("qwen-2.5-72b")
}
