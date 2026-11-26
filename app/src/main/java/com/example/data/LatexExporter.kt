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

object LatexExporter {

    fun generateLatex(
        session: ResearchSessionEntity,
        cycles: List<EvolutionCycleEntity>,
        messages: List<AgentMessageEntity>,
        hypotheses: List<ScientificHypothesisEntity>,
        scenarios: List<VerificationScenarioEntity>,
        citations: List<ScientificCitationEntity> = emptyList()
    ): String {
        val dateFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val dateStr = dateFormat.format(Date(session.updatedAt))
        val sb = StringBuilder()

        // Overleaf-ready Russian/English UTF-8 scientific article preamble
        sb.appendLine("% =============================================================================")
        sb.appendLine("% Progressor Multi-Agent Research Platform — Overleaf / LaTeX Preprint Export")
        sb.appendLine("% =============================================================================")
        sb.appendLine("\\documentclass[11pt,a4paper]{article}")
        sb.appendLine("\\usepackage[utf8]{inputenc}")
        sb.appendLine("\\usepackage[T2A]{fontenc}")
        sb.appendLine("\\usepackage[english,russian]{babel}")
        sb.appendLine("\\usepackage{amsmath,amssymb,amsfonts}")
        sb.appendLine("\\usepackage{booktabs}")
        sb.appendLine("\\usepackage{geometry}")
        sb.appendLine("\\usepackage{xcolor}")
        sb.appendLine("\\usepackage{tcolorbox}")
        sb.appendLine("\\usepackage{hyperref}")
        sb.appendLine("\\geometry{margin=2.2cm}")
        sb.appendLine("\\hypersetup{colorlinks=true, linkcolor=blue, urlcolor=teal, citecolor=purple}")
        sb.appendLine()
        sb.appendLine("\\title{\\textbf{${escapeLatex(session.topic)}}}")
        sb.appendLine("\\author{Мультиагентный консорциум \\textsc{Прогрессор} \\\\ \\small Теоретик: \\texttt{${escapeLatex(session.activeTheoristModel)}} \\and Скептик: \\texttt{${escapeLatex(session.activeSkepticModel)}} \\and Синтезатор: \\texttt{${escapeLatex(session.activeSynthesizerModel)}}}")
        sb.appendLine("\\date{$dateStr}")
        sb.appendLine()
        sb.appendLine("\\begin{document}")
        sb.appendLine("\\maketitle")
        sb.appendLine()

        // Abstract
        sb.appendLine("\\begin{abstract}")
        sb.appendLine("Настоящий препринт представляет результаты ${session.totalCycles} эволюционных циклов мультиагентного научного исследования в области \\emph{${escapeLatex(session.domain)}}. В ходе диалектического взаимодействия моделей были сформулированы ${hypotheses.size} научные гипотезы, разработаны ${scenarios.size} экспериментальных протокола с критериями фальсификации по Карлу Попперу и учтены ${citations.size} библиографических источников.")
        sb.appendLine("\\end{abstract}")
        sb.appendLine()
        sb.appendLine("\\tableofcontents")
        sb.appendLine("\\vspace{1em}")
        sb.appendLine()

        // Section 1: Council Metadata Table
        sb.appendLine("\\section{Метаданные и конфигурация Совета моделей}")
        sb.appendLine("\\begin{table}[h!]")
        sb.appendLine("\\centering")
        sb.appendLine("\\begin{tabular}{@{}ll@{}}")
        sb.appendLine("\\toprule")
        sb.appendLine("\\textbf{Параметр} & \\textbf{Значение} \\\\")
        sb.appendLine("\\midrule")
        sb.appendLine("Научная область & ${escapeLatex(session.domain)} \\\\")
        sb.appendLine("Количество эволюционных циклов & ${session.totalCycles} \\\\")
        sb.appendLine("Теоретик-Генератор & \\texttt{${escapeLatex(session.activeTheoristModel)}} \\\\")
        sb.appendLine("Скептик-Верификатор & \\texttt{${escapeLatex(session.activeSkepticModel)}} \\\\")
        sb.appendLine("Прогрессор-Синтезатор & \\texttt{${escapeLatex(session.activeSynthesizerModel)}} \\\\")
        sb.appendLine("Прикрепленные источники (arXiv/DOI) & ${citations.size} \\\\")
        sb.appendLine("\\bottomrule")
        sb.appendLine("\\end{tabular}")
        sb.appendLine("\\caption{Сводные параметры исследовательской сессии.}")
        sb.appendLine("\\end{table}")
        sb.appendLine()

        // Cycles
        cycles.forEach { cycle ->
            val cycleTitle = cycle.hypothesisTitle.ifEmpty { "Итерация синтеза" }
            sb.appendLine("\\section{Эволюционный цикл \\#${cycle.cycleNumber}: ${escapeLatex(cycleTitle)}}")
            sb.appendLine("\\noindent\\textbf{Индекс жизнеспособности:} ${cycle.viabilityScore}\\% \\quad | \\quad \\textbf{Статус:} \\texttt{${escapeLatex(cycle.status)}}\\\\")
            sb.appendLine("\\noindent\\textbf{Модели цикла:} \\texttt{${escapeLatex(cycle.theoristModel)}}, \\texttt{${escapeLatex(cycle.skepticModel)}}, \\texttt{${escapeLatex(cycle.synthesizerModel)}}.")
            if (!cycle.userGuidance.isNullOrBlank()) {
                sb.appendLine()
                sb.appendLine("\\begin{quote}")
                sb.appendLine("\\textbf{Указание оператора-человека:} \\emph{${escapeLatex(cycle.userGuidance)}}")
                sb.appendLine("\\end{quote}")
            }
            sb.appendLine()

            val hyp = hypotheses.find { it.cycleId == cycle.id }
            if (hyp != null) {
                sb.appendLine("\\subsection{Сформулированная гипотеза}")
                sb.appendLine("\\textbf{Тезис:} ${escapeLatex(hyp.thesis)}\\\\")
                sb.appendLine("\\textbf{Теоретический механизм:} ${escapeLatex(hyp.mechanism)}\\\\")
                sb.appendLine("\\textbf{Теоретический базис:} ${escapeLatex(hyp.theoreticalBasis)}\\\\")
                sb.appendLine("\\textbf{Оценка новизны:} ${hyp.noveltyScore}\\%, \\textbf{Уверенность:} ${hyp.confidenceScore}\\%.")
                sb.appendLine()
            }

            val scen = scenarios.find { it.cycleId == cycle.id }
            if (scen != null) {
                sb.appendLine("\\subsection{Протокол верификации и фальсификация}")
                sb.appendLine("\\begin{tcolorbox}[colback=orange!5!white,colframe=orange!75!black,title=Критерий фальсификации по Карлу Попперу]")
                sb.appendLine(escapeLatex(scen.falsificationCriteria))
                sb.appendLine("\\end{tcolorbox}")
                sb.appendLine()
                sb.appendLine("\\begin{table}[h!]")
                sb.appendLine("\\centering")
                sb.appendLine("\\begin{tabular}{@{}p{0.28\\textwidth}p{0.66\\textwidth}@{}}")
                sb.appendLine("\\toprule")
                sb.appendLine("\\textbf{Компонент протокола} & \\textbf{Спецификация} \\\\")
                sb.appendLine("\\midrule")
                sb.appendLine("Сценарий & ${escapeLatex(scen.title)} \\\\")
                sb.appendLine("Экспериментальный сетап & ${escapeLatex(scen.experimentalSetup)} \\\\")
                sb.appendLine("Контрольные группы & ${escapeLatex(scen.controlGroups)} \\\\")
                sb.appendLine("Метрики и пороги & ${escapeLatex(scen.metricsAndThresholds)} \\\\")
                sb.appendLine("Реализуемость & ${scen.feasibilityScore}\\% \\\\")
                sb.appendLine("\\bottomrule")
                sb.appendLine("\\end{tabular}")
                sb.appendLine("\\caption{Протокол экспериментальной проверки для цикла \\#${cycle.cycleNumber}.}")
                sb.appendLine("\\end{table}")
                sb.appendLine()
            }

            val cycleMessages = messages.filter { it.cycleId == cycle.id }
            if (cycleMessages.isNotEmpty()) {
                sb.appendLine("\\subsection{Стенограмма научного диалога}")
                cycleMessages.forEach { msg ->
                    sb.appendLine("\\paragraph{${escapeLatex(msg.agentTitle)} (\\texttt{${escapeLatex(msg.modelName)}}).}")
                    sb.appendLine(escapeLatex(msg.content))
                    sb.appendLine()
                }
            }
        }

        // Bibliography section from arXiv / DOI citations
        if (citations.isNotEmpty()) {
            sb.appendLine("\\begin{thebibliography}{${citations.size}}")
            citations.forEachIndexed { idx, cit ->
                val key = "ref_${idx + 1}_${cit.arxivId.replace(Regex("[^a-zA-Z0-9]"), "")}"
                val doiSuffix = if (!cit.doi.isNullOrBlank()) " DOI: \\href{https://doi.org/${escapeLatex(cit.doi)}}{\\texttt{${escapeLatex(cit.doi)}}}." else ""
                sb.appendLine("\\bibitem{$key} ${escapeLatex(cit.authors)}. \\emph{${escapeLatex(cit.title)}}. arXiv:\\texttt{${escapeLatex(cit.arxivId)}} (${escapeLatex(cit.publishedYear)}).$doiSuffix URL: \\url{${cit.pdfUrl}}")
            }
            sb.appendLine("\\end{thebibliography}")
            sb.appendLine()
        }

        sb.appendLine("\\end{document}")
        return sb.toString()
    }

    fun escapeLatex(text: String): String {
        return text
            .replace("\\", "\\textbackslash{}")
            .replace("{", "\\{")
            .replace("}", "\\}")
            .replace("&", "\\&")
            .replace("%", "\\%")
            .replace("$", "\\$")
            .replace("#", "\\#")
            .replace("_", "\\_")
            .replace("^", "\\textasciicircum{}")
            .replace("~", "\\textasciitilde{}")
    }

    fun writeLatexToUri(context: Context, uri: Uri, content: String): Boolean {
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
