package com.example.domain

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.example.data.local.AgentMessageEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class TtsPlaybackState(
    val isReady: Boolean = false,
    val isPlaying: Boolean = false,
    val currentMessageId: Long? = null,
    val currentIndex: Int = 0,
    val totalMessages: Int = 0,
    val currentSpeakerTitle: String = "",
    val currentRole: String = "",
    val speechSpeed: Float = 1.0f,
    val errorMessage: String? = null
)

class DebateTtsManager(context: Context) {

    private val _state = MutableStateFlow(TtsPlaybackState())
    val state: StateFlow<TtsPlaybackState> = _state.asStateFlow()

    private var tts: TextToSpeech? = null
    private var currentQueue: List<AgentMessageEntity> = emptyList()
    private var currentIndex: Int = 0
    private var isStoppedManually: Boolean = false

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val ruResult = tts?.setLanguage(Locale("ru", "RU"))
                if (ruResult == TextToSpeech.LANG_MISSING_DATA || ruResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.getDefault())
                }
                setupProgressListener()
                _state.value = _state.value.copy(isReady = true, errorMessage = null)
            } else {
                _state.value = _state.value.copy(
                    isReady = false,
                    errorMessage = "Движок синтеза речи недоступен на устройстве"
                )
            }
        }
    }

    private fun setupProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                // Already reflected in speakIndex
            }

            override fun onDone(utteranceId: String?) {
                if (isStoppedManually) return
                if (utteranceId?.startsWith("confirm_") == true) return
                val next = currentIndex + 1
                if (next < currentQueue.size) {
                    currentIndex = next
                    speakCurrent()
                } else {
                    _state.value = _state.value.copy(
                        isPlaying = false,
                        currentMessageId = null
                    )
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (utteranceId?.startsWith("confirm_") == true) return
                _state.value = _state.value.copy(
                    isPlaying = false,
                    errorMessage = "Ошибка озвучивания реплики"
                )
            }
        })
    }

    /**
     * Eyes-Free voice confirmation for critical spatial actions (launch cycle, council change, pause, copy).
     * Never overrides an actively playing scientific debate unless forceInterrupt is true (e.g., pause).
     */
    fun announceConfirmation(phrase: String, forceInterrupt: Boolean = false) {
        val engine = tts ?: return
        if (!_state.value.isReady) return
        if (_state.value.isPlaying && !forceInterrupt) return
        engine.setPitch(1.05f)
        engine.setSpeechRate(1.18f)
        val params = Bundle()
        engine.speak(phrase, TextToSpeech.QUEUE_FLUSH, params, "confirm_${System.currentTimeMillis()}")
    }

    fun playOrPause(messages: List<AgentMessageEntity>, startMessageId: Long? = null) {
        if (messages.isEmpty()) return

        if (_state.value.isPlaying && startMessageId == null) {
            pause()
            return
        }

        currentQueue = messages
        val targetIndex = if (startMessageId != null) {
            messages.indexOfFirst { it.id == startMessageId }.takeIf { it >= 0 } ?: 0
        } else if (_state.value.currentMessageId != null) {
            messages.indexOfFirst { it.id == _state.value.currentMessageId }.takeIf { it >= 0 } ?: 0
        } else {
            0
        }

        currentIndex = targetIndex
        isStoppedManually = false
        speakCurrent()
    }

    fun pause() {
        isStoppedManually = true
        tts?.stop()
        _state.value = _state.value.copy(isPlaying = false)
    }

    fun stop() {
        isStoppedManually = true
        tts?.stop()
        _state.value = _state.value.copy(
            isPlaying = false,
            currentMessageId = null,
            currentSpeakerTitle = "",
            currentRole = ""
        )
    }

    fun next(messages: List<AgentMessageEntity>) {
        if (messages.isEmpty()) return
        currentQueue = messages
        val nextIdx = (currentIndex + 1).coerceAtMost(messages.lastIndex)
        currentIndex = nextIdx
        isStoppedManually = false
        speakCurrent()
    }

    fun previous(messages: List<AgentMessageEntity>) {
        if (messages.isEmpty()) return
        currentQueue = messages
        val prevIdx = (currentIndex - 1).coerceAtLeast(0)
        currentIndex = prevIdx
        isStoppedManually = false
        speakCurrent()
    }

    fun cycleSpeed() {
        val speeds = listOf(0.9f, 1.0f, 1.25f, 1.5f)
        val currentIdx = speeds.indexOf(_state.value.speechSpeed)
        val nextSpeed = speeds[(currentIdx + 1) % speeds.size]
        _state.value = _state.value.copy(speechSpeed = nextSpeed)
        if (_state.value.isPlaying) {
            speakCurrent()
        }
    }

    private fun speakCurrent() {
        val engine = tts ?: return
        val msg = currentQueue.getOrNull(currentIndex) ?: return

        // Distinct pitch & speed profile per scientific council role
        val (pitch, roleRateMultiplier) = when (msg.role) {
            "THEORIST" -> Pair(1.06f, 1.0f)
            "SKEPTIC" -> Pair(0.88f, 0.96f)
            "SYNTHESIZER" -> Pair(0.98f, 1.04f)
            else -> Pair(1.12f, 1.0f) // HUMAN
        }

        engine.setPitch(pitch)
        engine.setSpeechRate((_state.value.speechSpeed * roleRateMultiplier).coerceIn(0.6f, 2.0f))

        val cleanText = cleanMarkdownForSpeech("${msg.agentTitle}. ${msg.content}")
        val truncated = cleanText.take(3800) // Safe Android TTS utterance buffer limit

        val params = Bundle()
        val utteranceId = "msg_${msg.id}_${System.currentTimeMillis()}"

        _state.value = _state.value.copy(
            isPlaying = true,
            currentMessageId = msg.id,
            currentIndex = currentIndex,
            totalMessages = currentQueue.size,
            currentSpeakerTitle = msg.agentTitle,
            currentRole = msg.role,
            errorMessage = null
        )

        engine.speak(truncated, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    fun shutdown() {
        isStoppedManually = true
        tts?.stop()
        tts?.shutdown()
        tts = null
    }

    companion object {
        fun cleanMarkdownForSpeech(raw: String): String {
            return raw
                .replace(Regex("#{1,6}\\s*"), "")
                .replace(Regex("\\*\\*|__|\\*|_|`"), "")
                .replace(Regex("\\[([^\\]]+)\\]\\([^)]+\\)"), "$1")
                .replace(Regex("(?m)^>\\s*"), "")
                .replace(Regex("(?m)^[-•*]\\s+"), "")
                .replace(Regex("\\s+"), " ")
                .trim()
        }
    }
}
