package com.sentup.android.ui.video

import android.content.Context
import com.sentup.android.data.CommentItem
import com.sentup.android.data.VttCue
import org.json.JSONArray
import org.json.JSONObject

enum class CommentFilterMode {
    ALL,
    TIMESTAMPS,
    QUESTIONS,
    PINNED_OR_HEARTED,
}

data class VideoNote(
    val id: String,
    val videoId: String,
    val positionMs: Long,
    val text: String,
    val createdAtMs: Long,
)

object VideoToolsEngine {
    private val TIMESTAMP_REGEX = Regex("""\b\d{1,2}:\d{2}(?::\d{2})?\b""")

    fun abLoopNextSeekMs(positionMs: Long, startMs: Long?, endMs: Long?): Long? {
        if (startMs == null || endMs == null) return null
        if (endMs <= startMs + 500L) return null
        return if (positionMs >= endMs) startMs else null
    }

    fun filterComments(
        comments: List<CommentItem>,
        query: String,
        mode: CommentFilterMode,
    ): List<CommentItem> {
        val q = query.trim().lowercase()
        return comments.filter { c ->
            val modeMatch = when (mode) {
                CommentFilterMode.ALL -> true
                CommentFilterMode.TIMESTAMPS -> TIMESTAMP_REGEX.containsMatchIn(c.text)
                CommentFilterMode.QUESTIONS -> c.text.contains('?')
                CommentFilterMode.PINNED_OR_HEARTED -> c.isPinned || c.isHearted
            }
            if (!modeMatch) return@filter false
            if (q.isEmpty()) return@filter true
            c.text.lowercase().contains(q) || c.author.lowercase().contains(q)
        }
    }

    fun searchTranscriptCues(cues: List<VttCue>, query: String): List<VttCue> {
        val q = query.trim().lowercase()
        if (q.isEmpty()) return cues
        return cues.filter { it.text.lowercase().contains(q) }
    }

    fun buildShareTimestampUrl(videoId: String, positionMs: Long): String {
        val cleanId = videoId.trim()
        val sec = (positionMs.coerceAtLeast(0L) / 1000L)
        return if (sec > 0L) "https://youtu.be/$cleanId?t=$sec" else "https://youtu.be/$cleanId"
    }

    fun formatTimestampMs(ms: Long): String {
        val totalSec = (ms.coerceAtLeast(0L) / 1000L)
        val hours = totalSec / 3600L
        val mins = (totalSec % 3600L) / 60L
        val secs = totalSec % 60L
        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, mins, secs)
        } else {
            "%d:%02d".format(mins, secs)
        }
    }
}

object VideoNotesStore {
    private const val PREFS = "sentup_video_notes"
    private const val KEY_PREFIX = "notes_"

    fun getNotes(context: Context, videoId: String): List<VideoNote> {
        if (videoId.isBlank()) return emptyList()
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_PREFIX + videoId, null) ?: return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        VideoNote(
                            id = o.optString("id"),
                            videoId = videoId,
                            positionMs = o.optLong("pos", 0L),
                            text = o.optString("text"),
                            createdAtMs = o.optLong("ts", 0L),
                        )
                    )
                }
            }.sortedBy { it.positionMs }
        }.getOrDefault(emptyList())
    }

    fun addNote(context: Context, videoId: String, positionMs: Long, text: String): List<VideoNote> {
        val trimmed = text.trim()
        if (videoId.isBlank() || trimmed.isEmpty()) return getNotes(context, videoId)
        val current = getNotes(context, videoId).toMutableList()
        val now = System.currentTimeMillis()
        current.add(
            VideoNote(
                id = "${now}_${current.size}",
                videoId = videoId,
                positionMs = positionMs.coerceAtLeast(0L),
                text = trimmed,
                createdAtMs = now,
            )
        )
        save(context, videoId, current)
        return current.sortedBy { it.positionMs }
    }

    fun deleteNote(context: Context, videoId: String, noteId: String): List<VideoNote> {
        val updated = getNotes(context, videoId).filterNot { it.id == noteId }
        save(context, videoId, updated)
        return updated
    }

    private fun save(context: Context, videoId: String, notes: List<VideoNote>) {
        val arr = JSONArray()
        notes.forEach { n ->
            arr.put(
                JSONObject().apply {
                    put("id", n.id)
                    put("pos", n.positionMs)
                    put("text", n.text)
                    put("ts", n.createdAtMs)
                }
            )
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PREFIX + videoId, arr.toString())
            .apply()
    }
}
