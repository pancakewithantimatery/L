package com.example.domain

/**
 * Strongly-typed voice commands recognized by on-device Android SpeechRecognizer
 * for hands-free navigation and debate control in XREAL AR mode.
 */
sealed class VoiceCommand(val confirmationPhrase: String) {
    data class NavigateTab(val tabIndex: Int, val spokenLabel: String) : VoiceCommand(spokenLabel)
    data object OpenSettings : VoiceCommand("Настройки")
    data object CloseOverlay : VoiceCommand("Возврат")

    data object LaunchDebateCycle : VoiceCommand("Эволюционный цикл запущен")
    data class HumanIntervention(val guidance: String) : VoiceCommand("Указание принято, цикл запущен")
    data class CreateNewSession(val topic: String?) : VoiceCommand(
        if (topic.isNullOrBlank()) "Новая тема" else "Создана тема $topic"
    )
    data object OpenCouncilConfig : VoiceCommand("Настройка состава Совета")
    data class SearchArxiv(val query: String?) : VoiceCommand(
        if (query.isNullOrBlank()) "Поиск арксив" else "Ищу $query"
    )

    data object TtsPlay : VoiceCommand("Озвучивание")
    data object TtsPause : VoiceCommand("Пауза")
    data object TtsNext : VoiceCommand("Следующая реплика")
    data object TtsPrevious : VoiceCommand("Предыдущая реплика")

    data object ExportToKeep : VoiceCommand("Отправлено в Keep")
    data object CopySynthesis : VoiceCommand("Синтез скопирован")
    data class SetXrealMode(val enabled: Boolean) : VoiceCommand(
        if (enabled) "Режим XREAL включён" else "Режим XREAL выключен"
    )

    data object StopHandsFree : VoiceCommand("Голосовое управление отключено")
    data object ShowHelp : VoiceCommand("Список голосовых команд")
    data class Unrecognized(val rawText: String) : VoiceCommand("Команда не распознана")
}

/**
 * Deterministic Russian + English voice command parser.
 * Evaluates multiple SpeechRecognizer hypotheses (top N matches) and selects the highest-priority valid command.
 */
object VoiceCommandParser {

    fun parseCandidates(candidates: List<String>): VoiceCommand {
        val cleaned = candidates.map { it.trim() }.filter { it.isNotBlank() }
        if (cleaned.isEmpty()) return VoiceCommand.Unrecognized("")

        for (candidate in cleaned) {
            val parsed = parse(candidate)
            if (parsed !is VoiceCommand.Unrecognized) {
                return parsed
            }
        }
        return VoiceCommand.Unrecognized(cleaned.first())
    }

    fun parse(rawTranscript: String): VoiceCommand {
        val original = rawTranscript.trim()
        if (original.isBlank()) return VoiceCommand.Unrecognized("")
        val text = original.lowercase()
            .replace('ё', 'е')
            .replace(Regex("[^a-zа-я0-9\\s-]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

        // 1. Stop voice listening / hands-free
        if (containsAny(
                text,
                "выключи микрофон", "выключить микрофон", "стоп голос", "хватит слушать",
                "отключи голос", "отключить голос", "выключи голос", "stop listening", "mute mic"
            )
        ) {
            return VoiceCommand.StopHandsFree
        }

        // 2. Help / Voice commands list
        if (containsAny(text, "голосовые команды", "список команд", "помощь", "справка", "что ты умеешь", "voice help")) {
            return VoiceCommand.ShowHelp
        }

        // 3. Human-in-the-loop voice intervention (dictated guidance -> triggers debate cycle)
        val interventionPrefixes = listOf(
            "вмешательство ", "указание ", "скажи совету ", "совету ", "добавь указание ",
            "передай совету ", "инъекция ", "задай вопрос ", "комментарий ", "guide ", "intervene "
        )
        for (prefix in interventionPrefixes) {
            val idx = text.indexOf(prefix)
            if (idx >= 0) {
                val payload = extractOriginalTail(original, prefix.trim())
                if (payload.length >= 3) {
                    return VoiceCommand.HumanIntervention(payload)
                }
            }
        }

        // 4. Create new research topic / session (with optional dictated title)
        val newSessionPrefixes = listOf(
            "новая тема", "создай тему", "создать тему", "новая сессия",
            "создай сессию", "создать сессию", "новое исследование", "new topic", "new session"
        )
        for (prefix in newSessionPrefixes) {
            if (text == prefix || text.startsWith("$prefix ") || text.contains(" $prefix")) {
                val topic = extractOriginalTail(original, prefix).takeIf { it.length >= 3 }
                return VoiceCommand.CreateNewSession(topic)
            }
        }

        // 5. arXiv search (with optional dictated query)
        val arxivPrefixes = listOf(
            "поиск arxiv", "поиск арксив", "поиск архив", "найди статьи", "найти статьи",
            "открой arxiv", "открой арксив", "открыть арксив", "search arxiv", "arxiv"
        )
        for (prefix in arxivPrefixes) {
            if (text == prefix || text.startsWith("$prefix ") || text.contains(" $prefix")) {
                val query = extractOriginalTail(original, prefix)
                    .removePrefix("по ")
                    .removePrefix("про ")
                    .trim()
                    .takeIf { it.length >= 2 }
                return VoiceCommand.SearchArxiv(query)
            }
        }

        // 6. Export to Google Keep operational buffer
        if (containsAny(
                text,
                "в keep", "в кип", "в гугл кип", "гугл кип", "google keep",
                "сохрани в заметки", "экспорт в заметки", "оперативный буфер", "в буфер keep", "export keep"
            )
        ) {
            return VoiceCommand.ExportToKeep
        }

        // 7. Copy synthesis for cross-posting
        if (containsAny(
                text,
                "скопируй синтез", "копировать синтез", "скопируй тезис", "копировать тезис",
                "копируй итог", "copy synthesis", "copy thesis"
            )
        ) {
            return VoiceCommand.CopySynthesis
        }

        // 8. XREAL Spatial AR mode toggles
        if (containsAny(text, "выключи xreal", "выключить xreal", "выключи ar", "обычный режим", "отключи xreal", "disable xreal")) {
            return VoiceCommand.SetXrealMode(false)
        }
        if (containsAny(text, "включи xreal", "включить xreal", "режим xreal", "режим очков", "включи ar", "enable xreal")) {
            return VoiceCommand.SetXrealMode(true)
        }

        // 9. Launch debate / evolution cycle
        if (containsAny(
                text,
                "запусти цикл", "запустить цикл", "новый цикл", "следующий цикл",
                "запусти дебаты", "запустить дебаты", "начни дебаты", "начать дебаты",
                "старт дебаты", "старт цикл", "запуск цикла", "запуск дебатов",
                "launch cycle", "start debate", "next cycle"
            )
        ) {
            return VoiceCommand.LaunchDebateCycle
        }

        // 10. Council configuration modal
        if (containsAny(
                text,
                "настрой совет", "настроить совет", "смени модели", "сменить модели",
                "состав совета", "выбор моделей", "configure council"
            )
        ) {
            return VoiceCommand.OpenCouncilConfig
        }

        // 11. TTS Podcast playback controls
        if (containsAny(text, "пауза", "стоп озвучка", "останови подкаст", "хватит читать", "тише", "pause", "stop audio")) {
            return VoiceCommand.TtsPause
        }
        if (containsAny(text, "следующая реплика", "дальше", "далее", "следующее сообщение", "next message")) {
            return VoiceCommand.TtsNext
        }
        if (containsAny(text, "предыдущая реплика", "предыдущее сообщение", "повтори прошлую", "previous message")) {
            return VoiceCommand.TtsPrevious
        }
        if (containsAny(text, "озвучь", "озвучить", "включи подкаст", "подкаст", "читай дебаты", "воспроизведи", "play podcast")) {
            return VoiceCommand.TtsPlay
        }

        // 12. Settings & Back / Close
        if (containsAny(text, "настройки", "открой настройки", "открыть настройки", "параметры", "settings")) {
            return VoiceCommand.OpenSettings
        }
        if (containsAny(text, "назад", "закрой", "закрыть", "отмена", "выйти", "back", "close")) {
            return VoiceCommand.CloseOverlay
        }

        // 13. Primary Tab Navigation (0..4)
        if (containsAny(text, "арена", "арену", "дебаты", "чат", "arena")) {
            return VoiceCommand.NavigateTab(0, "Арена")
        }
        if (containsAny(text, "гипотезы", "гипотез", "граф", "даги", "dag", "hypotheses", "graph")) {
            return VoiceCommand.NavigateTab(1, "Гипотезы")
        }
        if (containsAny(text, "лаборатория", "лабораторию", "лаб", "лаба", "ai lab", "lab")) {
            return VoiceCommand.NavigateTab(2, "Лаборатория")
        }
        if (containsAny(text, "совет", "модели", "роли", "персоны", "council")) {
            return VoiceCommand.NavigateTab(3, "Совет")
        }
        if (containsAny(text, "темы", "тем", "сессии", "проекты", "topics", "sessions")) {
            return VoiceCommand.NavigateTab(4, "Темы")
        }

        return VoiceCommand.Unrecognized(original)
    }

    private fun containsAny(text: String, vararg phrases: String): Boolean {
        return phrases.any { phrase ->
            text == phrase || text.contains(phrase)
        }
    }

    private fun extractOriginalTail(original: String, keyword: String): String {
        val lowerNorm = original.lowercase().replace('ё', 'е')
        val idx = lowerNorm.indexOf(keyword)
        if (idx < 0) return ""
        val afterIdx = (idx + keyword.length).coerceAtMost(original.length)
        return original.substring(afterIdx)
            .trimStart(' ', ':', '-', '—', ',', '.')
            .trim()
    }
}
