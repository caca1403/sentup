package com.sentup.android.ml

import android.content.Context
import com.sentup.android.data.VideoChapter
import com.sentup.android.data.VttCue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
import java.util.concurrent.TimeUnit
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

    private val aiClient by lazy {
        com.sentup.android.data.VpnResilientNetwork.configureForVpn(OkHttpClient.Builder())
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private const val PREFS_AI = "sentup_ai_prefs"
    private const val KEY_GEMINI_API = "gemini_api_key"

    fun getGeminiApiKey(context: Context): String =
        context.getSharedPreferences(PREFS_AI, Context.MODE_PRIVATE)
            .getString(KEY_GEMINI_API, "")
            .orEmpty()
            .trim()

    fun setGeminiApiKey(context: Context, key: String) {
        context.getSharedPreferences(PREFS_AI, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_GEMINI_API, key.trim())
            .apply()
    }

    sealed interface AiAnswerResult {
        data class Success(val text: String, val citedTimestampMs: Long?) : AiAnswerResult
        data class Error(val message: String) : AiAnswerResult
    }

    /**
     * Queries Gemini Flash with the video's title, chapters, description, and timestamped transcript
     * to produce a grounded natural-language answer.
     */
    suspend fun askGeminiAi(
        apiKey: String,
        question: String,
        title: String,
        description: String?,
        chapters: List<VideoChapter>,
        cues: List<VttCue>
    ): AiAnswerResult = withContext(Dispatchers.IO) {
        val cleanKey = apiKey.trim()
        if (cleanKey.isEmpty()) return@withContext AiAnswerResult.Error("API anahtarı boş.")

        val contextText = buildString {
            appendLine("Video Başlığı: $title")
            if (chapters.isNotEmpty()) {
                appendLine("\nBölümler:")
                chapters.take(20).forEach { ch ->
                    appendLine("- [${formatTimestamp(ch.startMs)}] ${ch.title}")
                }
            }
            if (!description.isNullOrBlank()) {
                appendLine("\nAçıklama:")
                appendLine(description.take(1500))
            }
            if (cues.isNotEmpty()) {
                appendLine("\nTranskript (zaman damgalı):")
                val step = (cues.size / 160).coerceAtLeast(1)
                cues.filterIndexed { index, _ -> index % step == 0 }
                    .take(160)
                    .forEach { cue ->
                        appendLine("[${formatTimestamp(cue.startMs)}] ${cue.text.trim()}")
                    }
            }
        }

        val prompt = """
            Sen bir video asistanısın. Aşağıdaki video bilgilerine dayanarak kullanıcının sorusunu net, doğru ve Türkçe (kullanıcı başka dilde sorduysa o dilde) yanıtla.
            Yanıtında ilgili bir an varsa mutlaka [MM:SS] biçiminde zaman damgası belirt. Video içeriğinde olmayan bilgiyi uydurma.

            $contextText

            Kullanıcı Sorusu: $question
        """.trimIndent()

        try {
            val payload = JSONObject().apply {
                put(
                    "contents",
                    JSONArray().put(
                        JSONObject().put(
                            "parts",
                            JSONArray().put(JSONObject().put("text", prompt))
                        )
                    )
                )
            }
            val url =
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=$cleanKey"
            val req = Request.Builder()
                .url(url)
                .post(payload.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            aiClient.newCall(req).execute().use { resp ->
                val bodyStr = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) {
                    val errMsg = runCatching {
                        JSONObject(bodyStr).optJSONObject("error")?.optString("message")
                    }.getOrNull()?.takeIf { it.isNotBlank() } ?: "HTTP ${resp.code}"
                    return@withContext AiAnswerResult.Error(errMsg)
                }
                val root = JSONObject(bodyStr)
                val text = root.optJSONArray("candidates")
                    ?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")
                    ?.trim()
                    .orEmpty()
                if (text.isEmpty()) {
                    return@withContext AiAnswerResult.Error("Boş yanıt döndü.")
                }
                val firstTs = parseFirstTimestampMs(text)
                AiAnswerResult.Success(text = text, citedTimestampMs = firstTs)
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (e: Exception) {
            AiAnswerResult.Error(e.message ?: "Bağlantı hatası")
        }
    }

    private fun formatTimestamp(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val min = totalSec / 60L
        val sec = totalSec % 60L
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }

    private fun parseFirstTimestampMs(text: String): Long? {
        val match = Regex("\\b(\\d{1,2}):(\\d{2})(?::(\\d{2}))?\\b").find(text) ?: return null
        val g1 = match.groupValues[1].toLongOrNull() ?: return null
        val g2 = match.groupValues[2].toLongOrNull() ?: return null
        val g3 = match.groupValues[3].takeIf { it.isNotEmpty() }?.toLongOrNull()
        return if (g3 != null) {
            (g1 * 3600L + g2 * 60L + g3) * 1000L
        } else {
            (g1 * 60L + g2) * 1000L
        }
    }
}
