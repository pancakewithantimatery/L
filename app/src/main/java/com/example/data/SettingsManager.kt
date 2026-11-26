package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

data class VeniceModelInfo(
    val id: String,
    val displayName: String,
    val recommendedRole: String, // "THEORIST", "SKEPTIC", "SYNTHESIZER", "ALL"
    val description: String,
    val tag: String = "70B+",
    val provider: String = "VENICE" // "VENICE", "GEMINI", "OPENROUTER", "OLLAMA"
)

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("progressor_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_API_KEY = "custom_venice_api_key"
        private const val KEY_CUSTOM_GEMINI_KEY = "custom_gemini_api_key"
        private const val KEY_CUSTOM_OPENROUTER_KEY = "custom_openrouter_api_key"
        private const val KEY_OLLAMA_HOST = "ollama_host_url"
        private const val KEY_SIMULATION_MODE = "simulation_mode_enabled"
        private const val KEY_THEORIST_MODEL = "pref_theorist_model"
        private const val KEY_SKEPTIC_MODEL = "pref_skeptic_model"
        private const val KEY_SYNTHESIZER_MODEL = "pref_synthesizer_model"

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

        // Combined default models
        val PRESET_MODELS = PRESET_VENICE_MODELS + PRESET_GEMINI_MODELS + PRESET_OPENROUTER_MODELS + PRESET_OLLAMA_MODELS
    }

    fun getApiKey(): String {
        val customKey = prefs.getString(KEY_CUSTOM_API_KEY, null)?.trim()
        if (!customKey.isNullOrEmpty()) {
            return customKey
        }
        val buildConfigKey = runCatching { BuildConfig.VENICE_API_KEY }.getOrNull()?.trim()
        return if (!buildConfigKey.isNullOrEmpty() && buildConfigKey != "PLACEHOLDER_VENICE_KEY") buildConfigKey else ""
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_API_KEY, key.trim()).apply()
    }

    fun isApiKeyConfigured(): Boolean {
        return getApiKey().isNotEmpty()
    }

    fun getGeminiApiKey(): String {
        val customKey = prefs.getString(KEY_CUSTOM_GEMINI_KEY, null)?.trim()
        if (!customKey.isNullOrEmpty()) {
            return customKey
        }
        val buildConfigKey = runCatching { BuildConfig.GEMINI_API_KEY }.getOrNull()?.trim()
        return if (!buildConfigKey.isNullOrEmpty() && buildConfigKey != "DEFAULT_GEMINI_KEY") buildConfigKey else ""
    }

    fun setCustomGeminiApiKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_GEMINI_KEY, key.trim()).apply()
    }

    fun isGeminiKeyConfigured(): Boolean {
        return getGeminiApiKey().isNotEmpty()
    }

    fun getOpenRouterApiKey(): String {
        val customKey = prefs.getString(KEY_CUSTOM_OPENROUTER_KEY, null)?.trim()
        if (!customKey.isNullOrEmpty()) {
            return customKey
        }
        val buildConfigKey = runCatching { BuildConfig.OPENROUTER_API_KEY }.getOrNull()?.trim()
        return if (!buildConfigKey.isNullOrEmpty() && buildConfigKey != "PLACEHOLDER_OPENROUTER_KEY") buildConfigKey else ""
    }

    fun setCustomOpenRouterApiKey(key: String) {
        prefs.edit().putString(KEY_CUSTOM_OPENROUTER_KEY, key.trim()).apply()
    }

    fun isOpenRouterKeyConfigured(): Boolean {
        return getOpenRouterApiKey().isNotEmpty()
    }

    fun getOllamaHost(): String {
        val saved = prefs.getString(KEY_OLLAMA_HOST, null)?.trim()
        if (!saved.isNullOrEmpty()) {
            return saved.removeSuffix("/")
        }
        val buildConfigHost = runCatching { BuildConfig.OLLAMA_HOST_URL }.getOrNull()?.trim()
        return if (!buildConfigHost.isNullOrEmpty()) buildConfigHost.removeSuffix("/") else "http://10.0.2.2:11434"
    }

    fun setOllamaHost(url: String) {
        val clean = url.trim().removeSuffix("/")
        prefs.edit().putString(KEY_OLLAMA_HOST, clean).apply()
    }

    fun isSimulationModeEnabled(): Boolean {
        return prefs.getBoolean(KEY_SIMULATION_MODE, false)
    }

    fun setSimulationMode(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SIMULATION_MODE, enabled).apply()
    }

    fun getTheoristModel(): String =
        prefs.getString(KEY_THEORIST_MODEL, "llama-3.3-70b") ?: "llama-3.3-70b"

    fun setTheoristModel(model: String) {
        prefs.edit().putString(KEY_THEORIST_MODEL, model).apply()
    }

    fun getSkepticModel(): String =
        prefs.getString(KEY_SKEPTIC_MODEL, "deepseek-r1-671b") ?: "deepseek-r1-671b"

    fun setSkepticModel(model: String) {
        prefs.edit().putString(KEY_SKEPTIC_MODEL, model).apply()
    }

    fun getSynthesizerModel(): String =
        prefs.getString(KEY_SYNTHESIZER_MODEL, "qwen-2.5-72b") ?: "qwen-2.5-72b"

    fun setSynthesizerModel(model: String) {
        prefs.edit().putString(KEY_SYNTHESIZER_MODEL, model).apply()
    }
}
