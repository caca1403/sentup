package com.sentup.android.ml

import com.sentup.android.data.VideoChapter
import com.sentup.android.data.VttCue
import java.util.Locale
import kotlin.math.ln

/**
 * Pure-Kotlin, zero-LLM Video Q&A and Key Moment Extraction Engine.
 * Uses BM25/TF-IDF lexical scoring over video transcripts ([VttCue]),
 * chapters ([VideoChapter]), title, and description to answer questions
 * with clickable timestamps.
 */
object VideoQaEngine {

    data class VideoQaAnswer(
        val text: String,
        val timestampMs: Long?,
        val sourceLabel: String,
        val score: Double
    )

    data class KeyMoment(
        val title: String,
        val timestampMs: Long
    )

    data class SummaryPoint(
        val text: String,
        val timestampMs: Long?
    )

    /**
     * Generates a concise extractive summary using TF-IDF centrality across
     * transcript windows, chapters, and description lines — zero LLM required.
     */
    fun summarizeVideo(
        title: String,
        description: String?,
        chapters: List<VideoChapter>,
        cues: List<VttCue>,
        limit: Int = 4
    ): List<SummaryPoint> {
        val candidates = buildList {
            cues.chunked(4).forEach { chunk ->
                val text = chunk.joinToString(" ") { it.text.trim() }.trim()
                if (text.length >= 24) add(SummaryPoint(text = text, timestampMs = chunk.first().startMs))
            }
            if (isEmpty() && chapters.isNotEmpty()) {
                chapters.forEach { add(SummaryPoint(text = it.title, timestampMs = it.startMs)) }
            }
            if (isEmpty() && !description.isNullOrBlank()) {
                description.lineSequence()
                    .map { it.trim() }
                    .filter { it.length >= 20 && !it.startsWith("http", ignoreCase = true) }
                    .take(limit)
                    .forEach { add(SummaryPoint(text = it, timestampMs = null)) }
            }
        }
        if (candidates.size <= limit) return candidates

        val allTokens = candidates.map { tokenize(it.text) }
        val df = mutableMapOf<String, Int>()
        allTokens.forEach { tokens ->
            tokens.distinct().forEach { t -> df[t] = (df[t] ?: 0) + 1 }
        }
        val titleTokens = tokenize(title).toSet()
        val n = candidates.size.toDouble()
        return candidates.mapIndexed { idx, point ->
            val tokens = allTokens[idx]
            val tfidfSum = tokens.sumOf { t ->
                val freq = df[t] ?: 1
                ln(1.0 + n / freq) + if (t in titleTokens) 0.5 else 0.0
            }
            val score = tfidfSum / tokens.size.coerceAtLeast(1)
            Triple(idx, point, score)
        }
            .sortedByDescending { it.third }
            .take(limit)
            .sortedBy { it.first }
            .map { it.second }
    }

    fun answerQuestion(
        question: String,
        title: String,
        description: String?,
        chapters: List<VideoChapter>,
        cues: List<VttCue>,
        limit: Int = 5
    ): List<VideoQaAnswer> {
        val queryTokens = tokenize(question)
        if (queryTokens.isEmpty()) return emptyList()

        val documents = buildList {
            chapters.forEach { ch ->
                add(Triple(ch.title, ch.startMs as Long?, "Chapter"))
            }
            // Group every 3 cues into a coherent window so context isn't fragmented
            cues.chunked(3).forEach { chunk ->
                val merged = chunk.joinToString(" ") { it.text.trim() }.trim()
                if (merged.isNotEmpty()) {
                    add(Triple(merged, chunk.first().startMs as Long?, "Transcript"))
                }
            }
            description
                ?.lineSequence()
                ?.map { it.trim() }
                ?.filter { it.length >= 12 }
                ?.forEach { line ->
                    add(Triple(line, null as Long?, "Description"))
                }
            if (title.isNotBlank()) {
                add(Triple(title, null as Long?, "Title"))
            }
        }
        if (documents.isEmpty()) return emptyList()

        val tokenizedDocs = documents.map { tokenize(it.first) }
        val avgLen = tokenizedDocs.map { it.size }.average().coerceAtLeast(1.0)
        val totalDocs = documents.size.toDouble()

        val idf = queryTokens.associateWith { term ->
            val df = tokenizedDocs.count { term in it }
            ln(1.0 + (totalDocs - df + 0.5) / (df + 0.5))
        }

        val k1 = 1.5
        val b = 0.75

        return documents.mapIndexedNotNull { idx, (text, ts, source) ->
            val docTokens = tokenizedDocs[idx]
            if (docTokens.isEmpty()) return@mapIndexedNotNull null
            var score = 0.0
            for (q in queryTokens) {
                val tf = docTokens.count { it == q || it.startsWith(q) || q.startsWith(it) }.toDouble()
                if (tf > 0.0) {
                    val termIdf = idf[q] ?: 0.5
                    val denom = tf + k1 * (1.0 - b + b * (docTokens.size / avgLen))
                    score += termIdf * ((tf * (k1 + 1.0)) / denom)
                }
            }
            if (score <= 0.0) null
            else VideoQaAnswer(text = text, timestampMs = ts, sourceLabel = source, score = score)
        }
            .sortedByDescending { it.score }
            .take(limit)
    }

    fun extractKeyMoments(
        chapters: List<VideoChapter>,
        cues: List<VttCue>,
        limit: Int = 6
    ): List<KeyMoment> {
        if (chapters.isNotEmpty()) {
            return chapters.take(limit).map { KeyMoment(title = it.title, timestampMs = it.startMs) }
        }
        if (cues.isEmpty()) return emptyList()
        val stride = (cues.size / limit).coerceAtLeast(1)
        return cues.chunked(stride)
            .take(limit)
            .mapNotNull { chunk ->
                val text = chunk.firstOrNull()?.text?.trim().orEmpty()
                if (text.isEmpty()) null
                else KeyMoment(title = text, timestampMs = chunk.first().startMs)
            }
    }

    private fun tokenize(input: String): List<String> =
        input.lowercase(Locale.ROOT)
            .split(Regex("[^\\p{L}\\p{Nd}]+"))
            .filter { it.length >= 2 }
}
