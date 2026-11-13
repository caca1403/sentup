package com.sentup.android.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

/**
 * Per-track intro/outro trim points so songs with long intros or silent outros
 * start at [startMs] and auto-advance at [endMs].
 */
@Serializable
data class TrackTrim(
    val songId: String,
    val startMs: Long = 0L,
    val endMs: Long? = null
)

/**
 * A personal timestamped bookmark/note attached to a specific moment in a song.
 */
@Serializable
data class SongBookmark(
    val id: String,
    val songId: String,
    val positionMs: Long,
    val note: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
private data class TrackCustomizationsSnapshot(
    val trims: Map<String, TrackTrim> = emptyMap(),
    val bookmarks: Map<String, List<SongBookmark>> = emptyMap()
)

/**
 * Persists per-track [TrackTrim]s and [SongBookmark]s in SharedPreferences as JSON.
 */
class TrackCustomizationStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _trims = MutableStateFlow<Map<String, TrackTrim>>(emptyMap())
    val trims: StateFlow<Map<String, TrackTrim>> = _trims.asStateFlow()

    private val _bookmarks = MutableStateFlow<Map<String, List<SongBookmark>>>(emptyMap())
    val bookmarks: StateFlow<Map<String, List<SongBookmark>>> = _bookmarks.asStateFlow()

    init {
        val snapshot = loadSnapshot()
        _trims.value = snapshot.trims
        _bookmarks.value = snapshot.bookmarks
    }

    private fun loadSnapshot(): TrackCustomizationsSnapshot {
        val raw = prefs.getString(KEY_SNAPSHOT_JSON, null) ?: return TrackCustomizationsSnapshot()
        return runCatching { json.decodeFromString<TrackCustomizationsSnapshot>(raw) }
            .getOrDefault(TrackCustomizationsSnapshot())
    }

    private fun persist() {
        val snapshot = TrackCustomizationsSnapshot(
            trims = _trims.value,
            bookmarks = _bookmarks.value
        )
        prefs.edit().putString(KEY_SNAPSHOT_JSON, json.encodeToString(snapshot)).apply()
    }

    fun getTrim(songId: String): TrackTrim? = _trims.value[songId]

    fun setTrim(songId: String, startMs: Long, endMs: Long?) {
        val safeStart = startMs.coerceAtLeast(0L)
        val safeEnd = endMs?.takeIf { it > safeStart + 1_000L }
        val updated = _trims.value.toMutableMap()
        if (safeStart == 0L && safeEnd == null) {
            updated.remove(songId)
        } else {
            updated[songId] = TrackTrim(songId = songId, startMs = safeStart, endMs = safeEnd)
        }
        _trims.value = updated
        persist()
    }

    fun clearTrim(songId: String) {
        val updated = _trims.value.toMutableMap()
        if (updated.remove(songId) != null) {
            _trims.value = updated
            persist()
        }
    }

    fun bookmarksFor(songId: String): List<SongBookmark> =
        _bookmarks.value[songId].orEmpty().sortedBy { it.positionMs }

    fun addBookmark(songId: String, positionMs: Long, note: String): SongBookmark {
        val bookmark = SongBookmark(
            id = UUID.randomUUID().toString(),
            songId = songId,
            positionMs = positionMs.coerceAtLeast(0L),
            note = note.trim()
        )
        val updated = _bookmarks.value.toMutableMap()
        val list = updated[songId].orEmpty() + bookmark
        updated[songId] = list.sortedBy { it.positionMs }
        _bookmarks.value = updated
        persist()
        return bookmark
    }

    fun deleteBookmark(songId: String, bookmarkId: String) {
        val updated = _bookmarks.value.toMutableMap()
        val remaining = updated[songId].orEmpty().filterNot { it.id == bookmarkId }
        if (remaining.isEmpty()) {
            updated.remove(songId)
        } else {
            updated[songId] = remaining
        }
        _bookmarks.value = updated
        persist()
    }

    companion object {
        private const val PREFS_NAME = "sentup_track_customizations"
        private const val KEY_SNAPSHOT_JSON = "snapshot_json"
    }
}
