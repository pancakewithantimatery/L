package com.example.data

import android.content.Context
import android.net.Uri
import com.example.data.local.AgentMessageEntity
import com.example.data.local.EvolutionCycleEntity
import com.example.data.local.ResearchSessionEntity
import com.example.data.local.ScientificCitationEntity
import com.example.data.local.ScientificHypothesisEntity
import com.example.data.local.VerificationScenarioEntity
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object MarkdownExporter {

    fun generateMarkdown(
        session: ResearchSessionEntity,
        cycles: List<EvolutionCycleEntity>,
        messages: List<AgentMessageEntity>,
        hypotheses: List<ScientificHypothesisEntity>,
        scenarios: List<VerificationScenarioEntity>,
        citations: List<ScientificCitationEntity> = emptyList()
    ): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        val sb = StringBuilder()

        // Document Title
        sb.appendLine("# Научный отчет Прогрессора: ${session.title}")
        sb.appendLine()
        sb.appendLine("> Автоматически сгенерированный мультиагентный отчет эволюции научных гипотез и протоколов верификации.")
        sb.appendLine()

        // Metadata Table
        sb.appendLine("## 📋 Метаданные исследования")
        sb.appendLine()
        sb.appendLine("| Параметр | Значение |")
        sb.appendLine("| :--- | :--- |")
        sb.appendLine("| **Тематика** | `${session.topic}` |")
        sb.appendLine("| **Научная область** | ${session.domain} |")
        sb.appendLine("| **Всего циклов эволюции** | ${session.totalCycles} |")
        sb.appendLine("| **Дата создания** | ${dateFormat.format(Date(session.createdAt))} |")
        sb.appendLine("| **Последнее обновление** | ${dateFormat.format(Date(session.updatedAt))} |")
        sb.appendLine("| **Теоретик-Генератор** | `${session.activeTheoristModel}` |")
        sb.appendLine("| **Скептик-Верификатор** | `${session.activeSkepticModel}` |")
        sb.appendLine("| **Прогрессор-Синтезатор** | `${session.activeSynthesizerModel}` |")
        sb.appendLine()

        // Table of Contents
        sb.appendLine("## 📑 Оглавление")
        sb.appendLine()
        cycles.forEach { cycle ->
            sb.appendLine("- [Эволюционный цикл #${cycle.cycleNumber}: ${cycle.hypothesisTitle.ifEmpty { "Исследование" }}](#эволюционный-цикл-${cycle.cycleNumber})")
        }
        sb.appendLine()
        sb.appendLine("---")
        sb.appendLine()

        // Cycles Details
        cycles.forEach { cycle ->
            sb.appendLine("## 🔬 Эволюционный цикл #${cycle.cycleNumber}")
            sb.appendLine()
            sb.appendLine("- **Состав моделей:** Теоретик = `${cycle.theoristModel}` • Скептик = `${cycle.skepticModel}` • Синтезатор = `${cycle.synthesizerModel}`")
            sb.appendLine("- **Статус:** `${cycle.status}`")
            sb.appendLine("- **Интегральный индекс жизнеспособности:** **${cycle.viabilityScore}%**")
            if (!cycle.userGuidance.isNullOrBlank()) {
                sb.appendLine("- **Вектор оператора-человека:** *\"${cycle.userGuidance}\"*")
            }
            sb.appendLine()

            // Hypothesis
            val hyp = hypotheses.find { it.cycleId == cycle.id }
            if (hyp != null) {
                sb.appendLine("### 💡 Сформулированная научная гипотеза")
                sb.appendLine()
                sb.appendLine("#### «${hyp.title}»")
                sb.appendLine()
                sb.appendLine("**Формулировка тезиса:**")
                sb.appendLine("> ${hyp.thesis}")
                sb.appendLine()
                sb.appendLine("**Теоретический механизм:**")
                sb.appendLine(hyp.mechanism)
                sb.appendLine()
                sb.appendLine("**Парадигмальный сдвиг и теоретический базис:**")
                sb.appendLine(hyp.theoreticalBasis)
                sb.appendLine()
                sb.appendLine("- **Оценка новизны:** `${hyp.noveltyScore}%`")
                sb.appendLine("- **Индекс уверенности:** `${hyp.confidenceScore}%`")
                sb.appendLine()
            }

            // Scenario & Falsification
            val scen = scenarios.find { it.cycleId == cycle.id }
            if (scen != null) {
                sb.appendLine("### 🧪 Протокол экспериментальной верификации")
                sb.appendLine()
                sb.appendLine("#### «${scen.title}»")
                sb.appendLine()
                sb.appendLine("> [!WARNING]")
                sb.appendLine("> **Критерий фальсификации по Карлу Попперу:**")
                sb.appendLine("> ${scen.falsificationCriteria}")
                sb.appendLine()
                sb.appendLine("**Экспериментальный сетап и методология:**")
                sb.appendLine(scen.experimentalSetup)
                sb.appendLine()
                sb.appendLine("**Контрольные группы:**")
                sb.appendLine(scen.controlGroups)
                sb.appendLine()
                sb.appendLine("**Измеряемые метрики и пороги (p-value / границы):**")
                sb.appendLine(scen.metricsAndThresholds)
                sb.appendLine()
                sb.appendLine("- **Оценка экспериментальной реализуемости:** `${scen.feasibilityScore}%`")
                sb.appendLine()
            }

            // Dialogue Transcript for this cycle
            val cycleMessages = messages.filter { it.cycleId == cycle.id }
            if (cycleMessages.isNotEmpty()) {
                sb.appendLine("### 💬 Стенограмма диалога агентов")
                sb.appendLine()
                cycleMessages.forEach { msg ->
                    val timeStr = dateFormat.format(Date(msg.timestamp))
                    sb.appendLine("#### ${msg.agentTitle} (`${msg.modelName}`) — $timeStr")
                    sb.appendLine()
                    sb.appendLine(msg.content)
                    sb.appendLine()
                }
            }

            sb.appendLine("---")
            sb.appendLine()
        }

        if (citations.isNotEmpty()) {
            sb.appendLine("## 📚 Библиография и источники (arXiv / DOI)")
            sb.appendLine()
            citations.forEachIndexed { idx, cit ->
                val doiPart = if (!cit.doi.isNullOrBlank()) " • DOI: `${cit.doi}`" else ""
                sb.appendLine("${idx + 1}. **${cit.title}** — *${cit.authors}* (${cit.publishedYear}). arXiv:`${cit.arxivId}`$doiPart • [PDF](${cit.pdfUrl})")
            }
            sb.appendLine()
        }

        sb.appendLine("*Сгенерировано в AI Studio Android: Progressor Multi-Agent Research Platform*")
        return sb.toString()
    }

    fun writeMarkdownToUri(context: Context, uri: Uri, content: String): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                    writer.write(content)
                    writer.flush()
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
