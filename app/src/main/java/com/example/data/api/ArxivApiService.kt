package com.example.data.api

import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

data class ArxivPaper(
    val arxivId: String,
    val doi: String?,
    val title: String,
    val authors: List<String>,
    val summary: String,
    val publishedYear: String,
    val pdfUrl: String
)

class ArxivApiService {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    suspend fun searchPapers(
        rawQuery: String,
        maxResults: Int = 10
    ): Result<List<ArxivPaper>> = withContext(Dispatchers.IO) {
        try {
            val cleanedQuery = rawQuery.trim()
            if (cleanedQuery.isEmpty()) {
                return@withContext Result.success(emptyList())
            }

            // Build an arXiv query supporting both general terms and direct arXiv IDs
            val searchParam = if (cleanedQuery.matches(Regex("^\\d{4}\\.\\d{4,5}(v\\d+)?$"))) {
                "id_list=${URLEncoder.encode(cleanedQuery, "UTF-8")}"
            } else {
                val encoded = URLEncoder.encode("all:$cleanedQuery", "UTF-8")
                "search_query=$encoded&start=0&max_results=$maxResults&sortBy=relevance&sortOrder=descending"
            }

            val url = "https://export.arxiv.org/api/query?$searchParam"
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "ProgressorResearchApp/2.0 (Android; Scientific Hypothesis Evolution)")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Ошибка arXiv API: HTTP ${response.code}"))
                }
                val xmlBody = response.body?.string()
                    ?: return@withContext Result.failure(Exception("Пустой ответ от arXiv"))
                val papers = parseAtomFeed(xmlBody)
                Result.success(papers)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    internal fun parseAtomFeed(xml: String): List<ArxivPaper> {
        val papers = mutableListOf<ArxivPaper>()
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var inEntry = false
        var inAuthor = false

        var currentId = ""
        var currentTitle = ""
        var currentSummary = ""
        var currentPublished = ""
        var currentDoi: String? = null
        var currentPdfUrl = ""
        val currentAuthors = mutableListOf<String>()

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name ?: ""
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when {
                        tagName == "entry" -> {
                            inEntry = true
                            currentId = ""
                            currentTitle = ""
                            currentSummary = ""
                            currentPublished = ""
                            currentDoi = null
                            currentPdfUrl = ""
                            currentAuthors.clear()
                        }
                        inEntry && tagName == "author" -> inAuthor = true
                        inEntry && inAuthor && tagName == "name" -> {
                            val name = parser.nextText().trim()
                            if (name.isNotEmpty()) currentAuthors.add(name)
                        }
                        inEntry && tagName == "id" -> {
                            currentId = parser.nextText().trim()
                        }
                        inEntry && tagName == "title" -> {
                            currentTitle = parser.nextText().replace(Regex("\\s+"), " ").trim()
                        }
                        inEntry && tagName == "summary" -> {
                            currentSummary = parser.nextText().replace(Regex("\\s+"), " ").trim()
                        }
                        inEntry && tagName == "published" -> {
                            currentPublished = parser.nextText().trim()
                        }
                        inEntry && (tagName == "arxiv:doi" || tagName == "doi") -> {
                            currentDoi = parser.nextText().trim().takeIf { it.isNotEmpty() }
                        }
                        inEntry && tagName == "link" -> {
                            val linkTitle = parser.getAttributeValue(null, "title")
                            val linkType = parser.getAttributeValue(null, "type")
                            val href = parser.getAttributeValue(null, "href")
                            if (!href.isNullOrEmpty() && (linkTitle == "pdf" || linkType == "application/pdf")) {
                                currentPdfUrl = href
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName == "author") {
                        inAuthor = false
                    } else if (tagName == "entry" && inEntry) {
                        inEntry = false
                        val cleanArxivId = currentId
                            .substringAfter("/abs/", currentId)
                            .removePrefix("http://arxiv.org/abs/")
                            .removePrefix("https://arxiv.org/abs/")
                            .trim()
                        val year = currentPublished.take(4).ifEmpty { "N/A" }
                        val finalPdf = currentPdfUrl.ifEmpty {
                            if (cleanArxivId.isNotEmpty()) "https://arxiv.org/pdf/$cleanArxivId.pdf" else currentId
                        }
                        if (currentTitle.isNotEmpty()) {
                            papers.add(
                                ArxivPaper(
                                    arxivId = cleanArxivId.ifEmpty { "arXiv" },
                                    doi = currentDoi,
                                    title = currentTitle,
                                    authors = currentAuthors.toList(),
                                    summary = currentSummary,
                                    publishedYear = year,
                                    pdfUrl = finalPdf
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return papers
    }
}
