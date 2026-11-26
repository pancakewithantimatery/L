package com.example.domain

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class VoiceControlState(
    val isAvailable: Boolean = true,
    val isHandsFreeEnabled: Boolean = false,
    val isListening: Boolean = false,
    val rmsDb: Float = 0f,
    val partialTranscript: String = "",
    val lastTranscript: String = "",
    val lastCommandLabel: String = "",
    val errorMessage: String? = null
)

/**
 * Manages Android's on-device SpeechRecognizer lifecycle on the Main Looper for
 * hands-free voice navigation and debate launching (including continuous XREAL AR listening mode).
 */
class VoiceCommandManager(
    context: Context,
    private val onBeforeStartListening: () -> Unit,
    private val onCommandRecognized: (VoiceCommand, String) -> Unit
) {
    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(
        VoiceControlState(
            isAvailable = runCatching { SpeechRecognizer.isRecognitionAvailable(appContext) }.getOrDefault(false)
        )
    )
    val state: StateFlow<VoiceControlState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var isDestroyed = false
    private var pendingRestartRunnable: Runnable? = null

    fun refreshAvailability(): Boolean {
        val available = runCatching { SpeechRecognizer.isRecognitionAvailable(appContext) }.getOrDefault(false)
        _state.value = _state.value.copy(isAvailable = available)
        return available
    }

    /**
     * Starts a single voice command capture session (or stops listening if already active).
     */
    fun toggleSingleListening() {
        mainHandler.post {
            if (_state.value.isListening) {
                stopListeningInternal(disableHandsFree = true)
            } else {
                startListeningInternal()
            }
        }
    }

    /**
     * Enables or disables continuous hands-free listening loop (ideal for XREAL Air 2 Pro AR mode).
     */
    fun setHandsFreeMode(enabled: Boolean) {
        mainHandler.post {
            if (isDestroyed) return@post
            cancelPendingRestart()
            _state.value = _state.value.copy(
                isHandsFreeEnabled = enabled,
                errorMessage = null
            )
            if (enabled) {
                if (!_state.value.isListening) {
                    startListeningInternal()
                }
            } else {
                stopListeningInternal(disableHandsFree = true)
            }
        }
    }

    /**
     * Temporarily defers hands-free auto-restart so a short TTS confirmation can finish cleanly
     * without the microphone picking up the app's own voice.
     */
    fun scheduleHandsFreeResumeAfterTts(delayMs: Long = 1600L) {
        mainHandler.post {
            if (isDestroyed || !_state.value.isHandsFreeEnabled) return@post
            cancelPendingRestart()
            val runnable = Runnable {
                if (!isDestroyed && _state.value.isHandsFreeEnabled && !_state.value.isListening) {
                    startListeningInternal()
                }
            }
            pendingRestartRunnable = runnable
            mainHandler.postDelayed(runnable, delayMs)
        }
    }

    fun reportPermissionDenied() {
        mainHandler.post {
            stopListeningInternal(disableHandsFree = true)
            _state.value = _state.value.copy(
                errorMessage = "Требуется разрешение на микрофон (RECORD_AUDIO) для голосового управления"
            )
        }
    }

    fun clearStatusBanner() {
        mainHandler.post {
            _state.value = _state.value.copy(
                lastCommandLabel = "",
                partialTranscript = "",
                errorMessage = null
            )
        }
    }

    fun stopAll() {
        mainHandler.post {
            stopListeningInternal(disableHandsFree = true)
        }
    }

    private fun startListeningInternal() {
        if (isDestroyed) return
        cancelPendingRestart()

        val available = refreshAvailability()
        if (!available) {
            _state.value = _state.value.copy(
                isListening = false,
                isHandsFreeEnabled = false,
                errorMessage = "Сервис распознавания речи SpeechRecognizer недоступен на устройстве"
            )
            return
        }

        // Pause any active debate TTS before opening microphone
        onBeforeStartListening()

        val recognizer = ensureRecognizer() ?: run {
            _state.value = _state.value.copy(
                isListening = false,
                isHandsFreeEnabled = false,
                errorMessage = "Не удалось инициализировать SpeechRecognizer"
            )
            return
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        }

        runCatching {
            _state.value = _state.value.copy(
                isListening = true,
                partialTranscript = "",
                errorMessage = null,
                rmsDb = 0f
            )
            recognizer.startListening(intent)
        }.onFailure { err ->
            _state.value = _state.value.copy(
                isListening = false,
                errorMessage = err.localizedMessage ?: "Ошибка запуска микрофона"
            )
            recreateRecognizer()
        }
    }

    private fun stopListeningInternal(disableHandsFree: Boolean) {
        cancelPendingRestart()
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        }
        _state.value = _state.value.copy(
            isListening = false,
            isHandsFreeEnabled = if (disableHandsFree) false else _state.value.isHandsFreeEnabled,
            rmsDb = 0f,
            partialTranscript = ""
        )
    }

    private fun ensureRecognizer(): SpeechRecognizer? {
        if (speechRecognizer == null) {
            speechRecognizer = runCatching {
                SpeechRecognizer.createSpeechRecognizer(appContext).apply {
                    setRecognitionListener(recognitionListener)
                }
            }.getOrNull()
        }
        return speechRecognizer
    }

    private fun recreateRecognizer() {
        runCatching { speechRecognizer?.destroy() }
        speechRecognizer = null
    }

    private fun cancelPendingRestart() {
        pendingRestartRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingRestartRunnable = null
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _state.value = _state.value.copy(isListening = true, errorMessage = null)
        }

        override fun onBeginningOfSpeech() {
            _state.value = _state.value.copy(isListening = true)
        }

        override fun onRmsChanged(rmsdB: Float) {
            _state.value = _state.value.copy(rmsDb = rmsdB.coerceIn(0f, 12f))
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            _state.value = _state.value.copy(isListening = false, rmsDb = 0f)
        }

        override fun onError(error: Int) {
            val isFatal = error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ||
                error == SpeechRecognizer.ERROR_AUDIO

            val isSilentTimeout = error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT

            val humanError = when (error) {
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Нет разрешения на микрофон"
                SpeechRecognizer.ERROR_AUDIO -> "Ошибка аудиопотока микрофона"
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Нет сети для облачного распознавания"
                SpeechRecognizer.ERROR_NO_MATCH -> if (_state.value.isHandsFreeEnabled) null else "Речь не распознана"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> if (_state.value.isHandsFreeEnabled) null else "Время ожидания команды истекло"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Распознаватель занят, перезапуск..."
                else -> "Ошибка распознавания (#$error)"
            }

            if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY || error == SpeechRecognizer.ERROR_CLIENT) {
                recreateRecognizer()
            }

            _state.value = _state.value.copy(
                isListening = false,
                rmsDb = 0f,
                partialTranscript = "",
                isHandsFreeEnabled = if (isFatal) false else _state.value.isHandsFreeEnabled,
                errorMessage = humanError
            )

            if (!isFatal && _state.value.isHandsFreeEnabled) {
                val backoff = if (isSilentTimeout) 700L else 1500L
                scheduleHandsFreeResumeAfterTts(backoff)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
            val primaryText = matches.firstOrNull().orEmpty()
            val parsed = VoiceCommandParser.parseCandidates(matches)

            _state.value = _state.value.copy(
                isListening = false,
                rmsDb = 0f,
                partialTranscript = "",
                lastTranscript = primaryText,
                lastCommandLabel = when (parsed) {
                    is VoiceCommand.Unrecognized -> "Не распознано: «$primaryText»"
                    else -> parsed.confirmationPhrase
                },
                errorMessage = null
            )

            if (parsed is VoiceCommand.StopHandsFree) {
                stopListeningInternal(disableHandsFree = true)
            }

            onCommandRecognized(parsed, primaryText)

            if (_state.value.isHandsFreeEnabled && parsed !is VoiceCommand.StopHandsFree && parsed !is VoiceCommand.TtsPlay) {
                scheduleHandsFreeResumeAfterTts(delayMs = 1750L)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (partial.isNotBlank()) {
                _state.value = _state.value.copy(partialTranscript = partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    fun destroy() {
        isDestroyed = true
        mainHandler.post {
            cancelPendingRestart()
            runCatching {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            }
            speechRecognizer = null
        }
    }
}
