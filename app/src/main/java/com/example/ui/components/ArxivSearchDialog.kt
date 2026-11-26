package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.api.ArxivPaper
import com.example.data.local.ScientificCitationEntity
import com.example.ui.spatial.performSpatialHaptic
import com.example.ui.spatial.spatialInteractive
import com.example.ui.theme.AmberSkeptic
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldHuman
import com.example.ui.theme.LocalSpatialConfig
import com.example.ui.theme.PurpleTheorist
import com.example.ui.theme.RoseWarning
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VoidDark
import com.example.ui.theme.spatialBorderColor
import com.example.ui.theme.spatialCardColor
import com.example.ui.theme.spatialSurfaceColor

@Composable
fun ArxivSearchDialog(
    initialQuery: String,
    searchResults: List<ArxivPaper>,
    pinnedCitations: List<ScientificCitationEntity>,
    isSearching: Boolean,
    errorMessage: String?,
    onSearch: (String) -> Unit,
    onPinPaper: (ArxivPaper) -> Unit,
    onDeleteCitation: (Long) -> Unit,
    onInjectPaperToCycle: (ArxivPaper) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var query by remember(initialQuery) { mutableStateOf(initialQuery) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Search, 1: Pinned

    LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && searchResults.isEmpty()) {
            onSearch(initialQuery)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.86f)
                .clip(RoundedCornerShape(22.dp))
                .border(1.dp, CyanAccent.copy(alpha = 0.5f), RoundedCornerShape(22.dp))
                .testTag("arxiv_search_dialog"),
            color = spatialSurfaceColor()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.16f))
                                .border(1.dp, CyanAccent, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MenuBook,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "База публикаций arXiv / DOI",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Реальные препринты для доказательной базы",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                            .testTag("close_arxiv_dialog_button")
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Tabs
                TabRow(
                    selectedTabIndex = activeTab,
                    containerColor = spatialCardColor(),
                    contentColor = CyanAccent,
                    indicator = { positions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(positions[activeTab]),
                            color = CyanAccent
                        )
                    },
                    modifier = Modifier.clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = activeTab == 0,
                        onClick = { activeTab = 0 },
                        text = { Text("Поиск arXiv (${searchResults.size})", fontSize = 12.sp) },
                        modifier = Modifier.testTag("arxiv_tab_search")
                    )
                    Tab(
                        selected = activeTab == 1,
                        onClick = { activeTab = 1 },
                        text = { Text("Прикреплено (${pinnedCitations.size})", fontSize = 12.sp) },
                        modifier = Modifier.testTag("arxiv_tab_pinned")
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (activeTab == 0) {
                    // Search Input Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = { query = it },
                            placeholder = { Text("Тема или arXiv ID (напр. 2305.10601)", color = TextMuted, fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("arxiv_query_input"),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = CyanAccent,
                                unfocusedBorderColor = BorderSubtle,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { onSearch(query) },
                            enabled = !isSearching && query.isNotBlank(),
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(CyanAccent)
                                .testTag("arxiv_execute_search_button")
                        ) {
                            if (isSearching) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = VoidDark
                                )
                            } else {
                                Icon(Icons.Default.Search, contentDescription = "Искать на arXiv", tint = VoidDark)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = RoseWarning.copy(alpha = 0.12f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, RoseWarning.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = errorMessage,
                                    color = RoseWarning,
                                    fontSize = 11.5.sp,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = { onSearch(query) },
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text("Повтор", fontSize = 11.sp, color = RoseWarning)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("arxiv_results_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (searchResults.isEmpty() && !isSearching) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Введите научный запрос на английском или arXiv ID (например, quantum coherence FMO), чтобы найти реальные статьи.",
                                        color = TextMuted,
                                        fontSize = 12.5.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(searchResults, key = { it.arxivId + it.title }) { paper ->
                                val isAlreadyPinned = pinnedCitations.any { it.arxivId == paper.arxivId }
                                ArxivPaperCard(
                                    paper = paper,
                                    isPinned = isAlreadyPinned,
                                    onPin = { onPinPaper(paper) },
                                    onInject = {
                                        onPinPaper(paper)
                                        onInjectPaperToCycle(paper)
                                    },
                                    onOpenPdf = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(paper.pdfUrl))
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                    }
                } else {
                    // Pinned Citations List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .testTag("arxiv_pinned_list"),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (pinnedCitations.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "К текущей сессии еще не прикреплены научные статьи. Прикрепленные источники автоматически передаются агентам и попадают в LaTeX/Markdown библиографию.",
                                        color = TextMuted,
                                        fontSize = 12.5.sp,
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        } else {
                            items(pinnedCitations, key = { it.id }) { cit ->
                                PinnedCitationCard(
                                    citation = cit,
                                    onDelete = { onDeleteCitation(cit.id) },
                                    onOpenPdf = {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(cit.pdfUrl))
                                        context.startActivity(intent)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ArxivPaperCard(
    paper: ArxivPaper,
    isPinned: Boolean,
    onPin: () -> Unit,
    onInject: () -> Unit,
    onOpenPdf: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val spatialConfig = LocalSpatialConfig.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isPinned) EmeraldHuman.copy(alpha = 0.6f) else spatialBorderColor(BorderSubtle)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyanAccent.copy(alpha = 0.16f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "arXiv:${paper.arxivId}",
                        color = CyanAccent,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = paper.publishedYear,
                    color = AmberSkeptic,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
                if (!paper.doi.isNullOrBlank()) {
                    Text(
                        text = "DOI:${paper.doi}",
                        color = PurpleTheorist,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = paper.title,
                color = TextPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = paper.authors.take(4).joinToString(", ") + if (paper.authors.size > 4) " et al." else "",
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = paper.summary,
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                        onPin()
                    },
                    enabled = !isPinned,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = if (isPinned) Icons.Default.Bookmark else Icons.Default.Add,
                        contentDescription = null,
                        tint = if (isPinned) EmeraldHuman else CyanAccent,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isPinned) "В сессии" else "Прикрепить",
                        color = if (isPinned) EmeraldHuman else CyanAccent,
                        fontSize = 11.sp
                    )
                }

                Button(
                    onClick = {
                        performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled, strong = true)
                        onInject()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PurpleTheorist)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = VoidDark,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("В цикл", color = VoidDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                // Quick Copy Citation for cross-posting into neighboring spatial windows
                IconButton(
                    onClick = {
                        performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                        val doiStr = if (!paper.doi.isNullOrBlank()) " | DOI:${paper.doi}" else ""
                        val cite = "${paper.title} — ${paper.authors.take(3).joinToString(", ")} (${paper.publishedYear}). arXiv:${paper.arxivId}$doiStr\n${paper.pdfUrl}"
                        clipboard.setText(AnnotatedString(cite))
                        Toast.makeText(context, "Цитата arXiv скопирована", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier
                        .size(40.dp)
                        .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                        .testTag("copy_arxiv_paper_${paper.arxivId}")
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Скопировать цитату arXiv",
                        tint = CyanAccent,
                        modifier = Modifier.size(17.dp)
                    )
                }

                IconButton(
                    onClick = onOpenPdf,
                    modifier = Modifier
                        .size(40.dp)
                        .spatialInteractive(accentColor = TextSecondary, shape = CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Открыть PDF",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PinnedCitationCard(
    citation: ScientificCitationEntity,
    onDelete: () -> Unit,
    onOpenPdf: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    val spatialConfig = LocalSpatialConfig.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = spatialCardColor()),
        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldHuman.copy(alpha = 0.45f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "arXiv:${citation.arxivId} (${citation.publishedYear})",
                    color = EmeraldHuman,
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                )
                Row {
                    IconButton(
                        onClick = {
                            performSpatialHaptic(context, haptic, spatialConfig.isEyesFreeFeedbackEnabled)
                            val doiStr = if (!citation.doi.isNullOrBlank()) " | DOI:${citation.doi}" else ""
                            val cite = "${citation.title} — ${citation.authors} (${citation.publishedYear}). arXiv:${citation.arxivId}$doiStr\n${citation.pdfUrl}"
                            clipboard.setText(AnnotatedString(cite))
                            Toast.makeText(context, "Цитата скопирована для кросс-постинга", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                            .testTag("copy_pinned_citation_${citation.id}")
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Копировать цитату", tint = CyanAccent, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onOpenPdf,
                        modifier = Modifier
                            .size(36.dp)
                            .spatialInteractive(accentColor = CyanAccent, shape = CircleShape)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "PDF", tint = CyanAccent, modifier = Modifier.size(16.dp))
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .spatialInteractive(accentColor = RoseWarning, shape = CircleShape)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = RoseWarning, modifier = Modifier.size(16.dp))
                    }
                }
            }
            Text(
                text = citation.title,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.5.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = citation.authors,
                color = TextSecondary,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (!citation.doi.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "DOI: ${citation.doi}",
                    color = PurpleTheorist,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
