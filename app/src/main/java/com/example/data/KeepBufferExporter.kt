package com.example.data

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import com.example.data.local.AgentMessageEntity
import com.example.data.local.EvolutionCycleEntity
import com.example.data.local.ResearchSessionEntity
import com.example.data.local.ScientificCitationEntity
import com.example.data.local.ScientificHypothesisEntity
import com.example.data.local.VerificationScenarioEntity

data class KeepBufferPayload(
    val title: String,
    val body: String
) {
    fun toClipboardText(): String = "$title\n\n$body"
}

data class KeepShareResult(
    val sentDirectlyToKeep: Boolean,
    val copiedToClipboard: Boolean,
    val fallbackChooserUsed: Boolean,
    val payload: KeepBufferPayload
)

/**
 * Generates ultra-condensed, high-density Markdown summaries designed strictly for
 * Google Keep as an operational working buffer (max 3 active anchor notes: structure,
 * current project, active tasks), ready for quick scanning and Obsidian handoff.
 */
object KeepBufferExporter {

    const val GOOGLE_KEEP_PACKAGE = "com.google.android.keep"

    private val ACTION_CUE_REGEX = Regex(
        pattern = "^(?:-\\s*\\[\\s*]\\s*|\\d+[.)]\\s*|[-•*]\\s*)?(?:следующ\\w*|шаг\\w*|провер\\w*|протестир\\w*|уточн\\w*|запуст\\w*|сравн\\w*|оцен\\w*|рассчита\\w*|внедр\\w*|перенес\\w*|вериф\\w*|todo|next|action|verify|test|check)\\b.*",
        option = RegexOption.IGNORE_CASE
    )

    fun generateSessionBuffer(
        session: ResearchSessionEntity,
        cycles: List<EvolutionCycleEntity>,
        messages: List<AgentMessageEntity>,
        hypotheses: List<ScientificHypothesisEntity>,
        scenarios: List<VerificationScenarioEntity>,
        citations: List<ScientificCitationEntity> = emptyList()
    ): KeepBufferPayload {
        val latestCycle = cycles.maxByOrNull { it.cycleNumber }
        val latestHyp = hypotheses.firstOrNull { it.cycleId == latestCycle?.id } ?: hypotheses.firstOrNull()
        val latestScen = scenarios.firstOrNull { it.cycleId == latestCycle?.id } ?: scenarios.firstOrNull()
        val latestSynth = messages.lastOrNull { it.role == "SYNTHESIZER" }
        val latestHuman = messages.lastOrNull { it.role == "HUMAN" }
        val fallbackMsg = latestSynth ?: messages.lastOrNull()

        val cycleTag = "Ц#${session.totalCycles.coerceAtLeast(latestCycle?.cycleNumber ?: 0)}"
        val title = "[Буфер] ${trimToLength(session.topic, 46)} • $cycleTag"

        val body = buildString {
            // 1. Context / Core Thesis (strictly 1-2 dense lines)
            appendLine("**Контекст / Тезис**")
            val domainClean = trimToLength(session.domain, 40)
            val topicClean = trimToLength(session.topic, 90)
            if (latestHyp != null) {
                val hypTitle = trimToLength(cleanInlineText(latestHyp.title), 80)
                val hypThesis = extractLeadSentence(latestHyp.thesis, maxChars = 165)
                appendLine("$domainClean • $topicClean")
                appendLine("→ **$hypTitle:** $hypThesis")
            } else {
                val lead = fallbackMsg?.let { extractLeadSentence(it.content, maxChars = 165) }
                    ?: "Активная сессия прогрессии ($cycleTag)."
                appendLine("$domainClean • $topicClean")
                appendLine("→ $lead")
            }
            appendLine()

            // 2. Confirmed Facts / Key Findings + arXiv links
            appendLine("**Факты и выводы**")
            val facts = mutableListOf<String>()

            val scoreParts = mutableListOf<String>()
            val viability = latestCycle?.viabilityScore?.takeIf { it > 0 }
            if (viability != null) scoreParts += "жизнеспособность $viability%"
            if (latestHyp != null) {
                scoreParts += "новизна ${latestHyp.noveltyScore}%"
                scoreParts += "уверенность ${latestHyp.confidenceScore}%"
            }
            if (scoreParts.isNotEmpty()) {
                facts += "Метрики ($cycleTag): ${scoreParts.joinToString(" • ")}"
            }

            if (latestHyp != null && latestHyp.mechanism.isNotBlank()) {
                facts += "Механизм: ${extractLeadSentence(latestHyp.mechanism, maxChars = 140)}"
            }

            if (latestScen != null && latestScen.falsificationCriteria.isNotBlank()) {
                facts += "Критерий Поппера: ${extractLeadSentence(latestScen.falsificationCriteria, maxChars = 140)}"
            }

            if (latestSynth != null) {
                val synthPoints = extractHighDensityBullets(latestSynth.content, maxBullets = 2, maxCharsPerBullet = 140)
                synthPoints.forEach { pt ->
                    if (facts.none { it.contains(pt.take(28), ignoreCase = true) }) {
                        facts += pt
                    }
                }
            } else if (fallbackMsg != null && latestHyp == null) {
                extractHighDensityBullets(fallbackMsg.content, maxBullets = 2, maxCharsPerBullet = 140)
                    .forEach { facts += it }
            }

            if (facts.isEmpty()) {
                facts += "Сессия инициализирована; ожидается первый завершённый цикл синтеза."
            }
            facts.take(5).forEach { appendLine("- $it") }

            if (citations.isNotEmpty()) {
                citations.take(3).forEach { cit ->
                    val link = cit.pdfUrl.ifBlank { "https://arxiv.org/abs/${cit.arxivId}" }
                    val doiPart = if (!cit.doi.isNullOrBlank()) " • DOI:${cit.doi}" else ""
                    val shortTitle = trimToLength(cleanInlineText(cit.title), 72)
                    appendLine("- arXiv:${cit.arxivId} (${cit.publishedYear}) — $shortTitle$doiPart ($link)")
                }
                if (citations.size > 3) {
                    appendLine("- + ещё ${citations.size - 3} ист. в полном .md отчёте")
                }
            }
            appendLine()

            // 3. Active Decisions & Next Steps (- [ ] checklist)
            appendLine("**Решения и шаги**")
            val tasks = linkedSetOf<String>()

            if (latestScen != null) {
                val metricCue = latestScen.metricsAndThresholds.takeIf { it.isNotBlank() }
                    ?.let { extractLeadSentence(it, maxChars = 110) }
                    ?: extractLeadSentence(latestScen.experimentalSetup, maxChars = 110)
                if (metricCue.isNotBlank()) {
                    tasks += "Верифицировать (${trimToLength(cleanInlineText(latestScen.title), 48)}): $metricCue"
                }
            }

            if (latestSynth != null) {
                extractActionItems(latestSynth.content, maxItems = 2).forEach { tasks += it }
            }

            if (latestHuman != null && tasks.size < 3) {
                val guidance = extractLeadSentence(latestHuman.content, maxChars = 120)
                if (guidance.isNotBlank()) {
                    tasks += "Отработать вектор оператора: $guidance"
                }
            }

            if (tasks.isEmpty()) {
                tasks += "Запустить следующий цикл Совета и проверить устойчивость гипотезы"
            }
            if (tasks.size < 3) {
                tasks += "Перенести подтверждённый синтез в Obsidian (.md архив)"
            }

            tasks.take(4).forEach { task ->
                appendLine("- [ ] $task")
            }
        }.trim()

        return KeepBufferPayload(title = title, body = body)
    }

    /**
     * Generates a tight Keep buffer note centered on a single high-signal Arena message
     * (e.g., Synthesizer conclusion or Skeptic falsification critique).
     */
    fun generateMessageBuffer(
        session: ResearchSessionEntity?,
        message: AgentMessageEntity,
        citations: List<ScientificCitationEntity> = emptyList()
    ): KeepBufferPayload {
        val roleShort = when (message.role) {
            "SYNTHESIZER" -> "Синтез"
            "SKEPTIC" -> "Скептик"
            "THEORIST" -> "Теоретик"
            else -> "Оператор"
        }
        val topicLabel = session?.topic?.let { trimToLength(it, 38) } ?: "Сессия"
        val title = "[Буфер • $roleShort] $topicLabel"

        val lead = extractLeadSentence(message.content, maxChars = 165)
        val bullets = extractHighDensityBullets(message.content, maxBullets = 3, maxCharsPerBullet = 140)
            .filterNot { lead.contains(it.take(28), ignoreCase = true) }
        val actions = extractActionItems(message.content, maxItems = 2)

        val body = buildString {
            appendLine("**Контекст / Тезис**")
            appendLine("${message.agentTitle} (${message.modelName}) • ${session?.domain ?: "Прогрессор"}")
            appendLine("→ $lead")
            appendLine()

            appendLine("**Факты и выводы**")
            if (bullets.isNotEmpty()) {
                bullets.forEach { appendLine("- $it") }
            } else {
                appendLine("- Ключевой вывод зафиксирован из реплики агента ($roleShort).")
            }
            citations.take(2).forEach { cit ->
                val link = cit.pdfUrl.ifBlank { "https://arxiv.org/abs/${cit.arxivId}" }
                appendLine("- arXiv:${cit.arxivId} — ${trimToLength(cleanInlineText(cit.title), 68)} ($link)")
            }
            appendLine()

            appendLine("**Решения и шаги**")
            if (actions.isNotEmpty()) {
                actions.forEach { appendLine("- [ ] $it") }
            } else {
                appendLine("- [ ] Проверить и развить тезис в следующем цикле Совета")
            }
            appendLine("- [ ] При подтверждении выгрузить в Obsidian (.md)")
        }.trim()

        return KeepBufferPayload(title = title, body = body)
    }

    /**
     * Copies the operational summary to the system clipboard and dispatches an Android ACTION_SEND
     * Intent directly targeting Google Keep (`com.google.android.keep`) if installed, or falls back
     * to the standard Android Share Sheet.
     */
    fun exportToGoogleKeep(
        context: Context,
        payload: KeepBufferPayload
    ): KeepShareResult {
        val copied = copyToSystemClipboard(context, payload)

        val keepIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            setPackage(GOOGLE_KEEP_PACKAGE)
            putExtra(Intent.EXTRA_SUBJECT, payload.title)
            putExtra(Intent.EXTRA_TEXT, payload.body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        val keepResolved = runCatching {
            context.packageManager.resolveActivity(keepIntent, 0) != null
        }.getOrDefault(false)

        if (keepResolved) {
            try {
                context.startActivity(keepIntent)
                return KeepShareResult(
                    sentDirectlyToKeep = true,
                    copiedToClipboard = copied,
                    fallbackChooserUsed = false,
                    payload = payload
                )
            } catch (_: ActivityNotFoundException) {
                // Fall through to generic Share Sheet
            }
        }

        val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, payload.title)
            putExtra(Intent.EXTRA_TEXT, payload.body)
        }
        val chooser = Intent.createChooser(fallbackIntent, "Отправить оперативный буфер (Google Keep)").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooserLaunched = runCatching {
            context.startActivity(chooser)
            true
        }.getOrDefault(false)

        return KeepShareResult(
            sentDirectlyToKeep = false,
            copiedToClipboard = copied,
            fallbackChooserUsed = chooserLaunched,
            payload = payload
        )
    }

    private fun copyToSystemClipboard(context: Context, payload: KeepBufferPayload): Boolean {
        return runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            val clip = ClipData.newPlainText("Прогрессор • Буфер Keep", payload.toClipboardText())
            clipboard?.setPrimaryClip(clip)
            clipboard != null
        }.getOrDefault(false)
    }

    internal fun cleanInlineText(raw: String): String {
        return raw
            .replace(Regex("```[\\s\\S]*?```"), " ")
            .replace(Regex("^#{1,6}\\s+", RegexOption.MULTILINE), "")
            .replace(Regex("[*_`~>]+"), "")
            .replace(Regex("\\[([^\\]]+)]\\(([^)]+)\\)"), "$1 ($2)")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    internal fun trimToLength(text: String, maxChars: Int): String {
        val normalized = text.trim()
        if (normalized.length <= maxChars) return normalized
        val slice = normalized.take(maxChars - 1)
        val lastSpace = slice.lastIndexOf(' ')
        val cut = if (lastSpace > maxChars / 2) slice.take(lastSpace) else slice
        return "${cut.trimEnd('.', ',', ';', ':', '-', ' ')}…"
    }

    internal fun extractLeadSentence(raw: String, maxChars: Int): String {
        val cleaned = cleanInlineText(raw)
        if (cleaned.isEmpty()) return ""
        val firstSentence = Regex("[^.!?\\n]+[.!?]?").find(cleaned)?.value?.trim() ?: cleaned
        return trimToLength(firstSentence, maxChars)
    }

    internal fun extractHighDensityBullets(
        raw: String,
        maxBullets: Int = 3,
        maxCharsPerBullet: Int = 140
    ): List<String> {
        val lines = raw.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("---") }

        val explicitBullets = lines
            .filter { it.startsWith("- ") || it.startsWith("* ") || it.startsWith("• ") || Regex("^\\d+[.)]\\s+").containsMatchIn(it) }
            .map { line ->
                cleanInlineText(
                    line.replaceFirst(Regex("^(?:[-*•]|\\d+[.)])\\s*(?:\\[[ xX]]\\s*)?"), "")
                )
            }
            .filter { it.length >= 16 }
            .distinct()

        if (explicitBullets.size >= maxBullets) {
            return explicitBullets.take(maxBullets).map { trimToLength(it, maxCharsPerBullet) }
        }

        val sentences = cleanInlineText(raw)
            .split(Regex("(?<=[.!?])\\s+"))
            .map { it.trim() }
            .filter { it.length >= 20 }

        val merged = linkedSetOf<String>()
        explicitBullets.forEach { merged += trimToLength(it, maxCharsPerBullet) }
        for (s in sentences) {
            if (merged.size >= maxBullets) break
            val candidate = trimToLength(s, maxCharsPerBullet)
            if (merged.none { it.contains(candidate.take(24), ignoreCase = true) }) {
                merged += candidate
            }
        }
        return merged.take(maxBullets)
    }

    internal fun extractActionItems(raw: String, maxItems: Int = 2): List<String> {
        return raw.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && ACTION_CUE_REGEX.matches(it) }
            .map { line ->
                cleanInlineText(
                    line.replaceFirst(Regex("^(?:[-*•]|\\d+[.)])\\s*(?:\\[[ xX]]\\s*)?"), "")
                )
            }
            .filter { it.length >= 12 }
            .distinct()
            .take(maxItems)
            .map { trimToLength(it, 130) }
    }
}
