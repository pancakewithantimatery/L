package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig

data class VeniceModelInfo(
    val id: String,
    val displayName: String,
    val recommendedRole: String, // "THEORIST", "SKEPTIC", "SYNTHESIZER", "ALL"
    val description: String,
    val tag: String = "70B+"
)

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("progressor_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_API_KEY = "custom_venice_api_key"
        private const val KEY_SIMULATION_MODE = "simulation_mode_enabled"
        private const val KEY_THEORIST_MODEL = "pref_theorist_model"
        private const val KEY_SKEPTIC_MODEL = "pref_skeptic_model"
        private const val KEY_SYNTHESIZER_MODEL = "pref_synthesizer_model"

        val PRESET_MODELS = listOf(
            VeniceModelInfo(
                id = "llama-3.3-70b",
                displayName = "Llama 3.3 70B",
                recommendedRole = "THEORIST",
                description = "Глубокая междисциплинарная эрудиция, выдвижение смелых фундаментальных теорий",
                tag = "Meta / 70B"
            ),
            VeniceModelInfo(
                id = "deepseek-r1-671b",
                displayName = "DeepSeek R1 (Reasoning)",
                recommendedRole = "SKEPTIC",
                description = "Пошаговое строгое рассуждение, выявление парадоксов и попперовская фальсификация",
                tag = "DeepSeek / 671B"
            ),
            VeniceModelInfo(
                id = "qwen-2.5-72b",
                displayName = "Qwen 2.5 72B",
                recommendedRole = "SYNTHESIZER",
                description = "Исключительные математические выкладки, прикладной инженерный синтез",
                tag = "Alibaba / 72B"
            ),
            VeniceModelInfo(
                id = "dolphin-2.9.2-qwen2-72b",
                displayName = "Dolphin 2.9.2 Qwen2",
                recommendedRole = "THEORIST",
                description = "Неограниченное научное творчество, нестандартные эвристики и догадки",
                tag = "Cognitive / 72B"
            ),
            VeniceModelInfo(
                id = "mistral-nemo",
                displayName = "Mistral NeMo",
                recommendedRole = "SYNTHESIZER",
                description = "Высокая плотность смыслов, строгая протоколизация и формализация сценариев",
                tag = "Mistral / 12B"
            ),
            VeniceModelInfo(
                id = "llama-3.1-405b",
                displayName = "Llama 3.1 405B",
                recommendedRole = "ALL",
                description = "Флагманский масштаб познания для сложнейших многофакторных проблем",
                tag = "Meta / 405B"
            ),
            VeniceModelInfo(
                id = "hermes-3-llama-3.1-405b",
                displayName = "Hermes 3 405B",
                recommendedRole = "SKEPTIC",
                description = "Продвинутое агентное рассуждение и дизайн экспериментальных сетапов",
                tag = "Nous / 405B"
            ),
            VeniceModelInfo(
                id = "deepseek-v3",
                displayName = "DeepSeek V3",
                recommendedRole = "ALL",
                description = "Универсальная архитектура MoE высокой точности",
                tag = "DeepSeek / MoE"
            )
        )
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
