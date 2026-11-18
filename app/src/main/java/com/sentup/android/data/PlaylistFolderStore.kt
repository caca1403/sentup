package com.sentup.android.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Locale
import java.util.UUID

/**
 * A Musify-style playlist folder in the Music Library.
 * Holds references to playlists by their [PlaylistDisplayItem.id] in [playlistIds].
 * Deleting a folder never deletes its playlists; they simply return to the root list.
 */
@Serializable
data class PlaylistFolder(
    val id: String,
    val name: String,
    val playlistIds: List<String> = emptyList(),
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

enum class PlaylistFolderSortOrder {
    Default,
    NameAsc,
    NameDesc,
    MostSongs,
    Newest
}

enum class PlaylistSourceFilter {
    All,
    FoldersOnly,
    Local,
    YouTube,
    Saved,
    Offline
}

sealed interface CreatePlaylistFolderResult {
    data class Created(val folder: PlaylistFolder) : CreatePlaylistFolderResult
    data object BlankName : CreatePlaylistFolderResult
    data object DuplicateName : CreatePlaylistFolderResult
}

/**
 * Pure operations over [PlaylistFolder]s and [PlaylistDisplayItem]s, separated from
 * Android [Context] for deterministic filtering, sorting, and unit testing.
 */
object PlaylistFolderLogic {

    fun folderForPlaylist(folders: List<PlaylistFolder>, playlistId: String): PlaylistFolder? =
        folders.firstOrNull { playlistId in it.playlistIds }

    fun allFolderedPlaylistIds(folders: List<PlaylistFolder>): Set<String> =
        folders.flatMapTo(mutableSetOf()) { it.playlistIds }

    fun playlistsInFolder(
        folder: PlaylistFolder,
        allPlaylists: List<PlaylistDisplayItem>
    ): List<PlaylistDisplayItem> {
        val byId = allPlaylists.associateBy { it.id }
        return folder.playlistIds.mapNotNull { byId[it] }
    }

    fun playlistsNotInFolders(
        folders: List<PlaylistFolder>,
        allPlaylists: List<PlaylistDisplayItem>
    ): List<PlaylistDisplayItem> {
        val inFolders = allFolderedPlaylistIds(folders)
        return allPlaylists.filter { it.id !in inFolders }
    }

    fun totalSongsInFolder(
        folder: PlaylistFolder,
        allPlaylists: List<PlaylistDisplayItem>
    ): Int = playlistsInFolder(folder, allPlaylists).sumOf { it.itemCount.coerceAtLeast(0) }

    fun folderPreviewThumbnails(
        folder: PlaylistFolder,
        allPlaylists: List<PlaylistDisplayItem>,
        limit: Int = 4
    ): List<String> = playlistsInFolder(folder, allPlaylists)
        .mapNotNull { it.thumbnailUrl?.takeIf { url -> url.isNotBlank() && url != "null" } }
        .distinct()
        .take(limit)

    fun filterAndSortFolders(
        folders: List<PlaylistFolder>,
        allPlaylists: List<PlaylistDisplayItem>,
        query: String,
        sourceFilter: PlaylistSourceFilter,
        sortOrder: PlaylistFolderSortOrder,
        localPlaylistIds: Set<String>,
        savedPlaylistIds: Set<String>,
        offlinePlaylistIds: Set<String>
    ): List<PlaylistFolder> {
        val q = query.trim().lowercase(Locale.ROOT)
        val filtered = folders.filter { folder ->
            val inside = playlistsInFolder(folder, allPlaylists)
            val matchesQuery = q.isEmpty() ||
                folder.name.lowercase(Locale.ROOT).contains(q) ||
                inside.any { it.name.lowercase(Locale.ROOT).contains(q) }
            val matchesSource = when (sourceFilter) {
                PlaylistSourceFilter.All,
                PlaylistSourceFilter.FoldersOnly -> true
                PlaylistSourceFilter.Local -> inside.any { it.id in localPlaylistIds }
                PlaylistSourceFilter.Saved -> inside.any { it.id in savedPlaylistIds }
                PlaylistSourceFilter.YouTube -> inside.any {
                    it.id !in localPlaylistIds && it.id !in savedPlaylistIds && it.id != "LM" && it.id != "RTM"
                }
                PlaylistSourceFilter.Offline -> inside.any {
                    it.id in localPlaylistIds || it.id in offlinePlaylistIds
                }
            }
            matchesQuery && matchesSource
        }

        val byPinThenDefault = compareByDescending<PlaylistFolder> { it.isPinned }
        return when (sortOrder) {
            PlaylistFolderSortOrder.Default -> filtered.sortedWith(
                byPinThenDefault.thenBy { it.createdAt }
            )
            PlaylistFolderSortOrder.NameAsc -> filtered.sortedWith(
                byPinThenDefault.thenBy { it.name.lowercase(Locale.ROOT) }
            )
            PlaylistFolderSortOrder.NameDesc -> filtered.sortedWith(
                byPinThenDefault.thenByDescending { it.name.lowercase(Locale.ROOT) }
            )
            PlaylistFolderSortOrder.MostSongs -> filtered.sortedWith(
                byPinThenDefault
                    .thenByDescending { totalSongsInFolder(it, allPlaylists) }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
            PlaylistFolderSortOrder.Newest -> filtered.sortedWith(
                byPinThenDefault.thenByDescending { it.createdAt }
            )
        }
    }

    fun filterAndSortPlaylists(
        playlists: List<PlaylistDisplayItem>,
        query: String,
        sourceFilter: PlaylistSourceFilter,
        sortOrder: PlaylistFolderSortOrder,
        localPlaylistIds: Set<String>,
        savedPlaylistIds: Set<String>,
        offlinePlaylistIds: Set<String>
    ): List<PlaylistDisplayItem> {
        if (sourceFilter == PlaylistSourceFilter.FoldersOnly) return emptyList()
        val q = query.trim().lowercase(Locale.ROOT)
        val filtered = playlists.filter { playlist ->
            val matchesQuery = q.isEmpty() ||
                playlist.name.lowercase(Locale.ROOT).contains(q) ||
                playlist.uploaderName.lowercase(Locale.ROOT).contains(q)
            val matchesSource = when (sourceFilter) {
                PlaylistSourceFilter.All -> true
                PlaylistSourceFilter.FoldersOnly -> false
                PlaylistSourceFilter.Local -> playlist.id in localPlaylistIds
                PlaylistSourceFilter.Saved -> playlist.id in savedPlaylistIds
                PlaylistSourceFilter.YouTube ->
                    playlist.id !in localPlaylistIds &&
                        playlist.id !in savedPlaylistIds &&
                        playlist.id != "LM" &&
                        playlist.id != "RTM"
                PlaylistSourceFilter.Offline ->
                    playlist.id in localPlaylistIds || playlist.id in offlinePlaylistIds
            }
            matchesQuery && matchesSource
        }

        return when (sortOrder) {
            PlaylistFolderSortOrder.Default -> filtered
            PlaylistFolderSortOrder.NameAsc -> filtered.sortedBy { it.name.lowercase(Locale.ROOT) }
            PlaylistFolderSortOrder.NameDesc -> filtered.sortedByDescending { it.name.lowercase(Locale.ROOT) }
            PlaylistFolderSortOrder.MostSongs -> filtered.sortedWith(
                compareByDescending<PlaylistDisplayItem> { it.itemCount.coerceAtLeast(0) }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )
            PlaylistFolderSortOrder.Newest -> filtered.reversed()
        }
    }
}

/**
 * Persists user-created [PlaylistFolder]s in SharedPreferences (`playlist_folders`).
 * Device-wide, matching [PlaylistRepository] and [SavedPlaylistsRepository].
 */
class PlaylistFolderStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val _folders = MutableStateFlow(loadFromPrefs())
    val folders: StateFlow<List<PlaylistFolder>> = _folders.asStateFlow()

    private fun loadFromPrefs(): List<PlaylistFolder> {
        val raw = prefs.getString(KEY_FOLDERS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString<List<PlaylistFolder>>(raw)
        }.getOrDefault(emptyList())
    }

    private fun save(updated: List<PlaylistFolder>) {
        _folders.value = updated
        prefs.edit().putString(KEY_FOLDERS, json.encodeToString(updated)).apply()
    }

    fun createFolder(name: String): CreatePlaylistFolderResult {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return CreatePlaylistFolderResult.BlankName
        val exists = _folders.value.any {
            it.name.equals(trimmed, ignoreCase = true)
        }
        if (exists) return CreatePlaylistFolderResult.DuplicateName

        val folder = PlaylistFolder(
            id = UUID.randomUUID().toString(),
            name = trimmed
        )
        save(_folders.value + folder)
        return CreatePlaylistFolderResult.Created(folder)
    }

    fun renameFolder(folderId: String, newName: String): Boolean {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return false
        val conflict = _folders.value.any {
            it.id != folderId && it.name.equals(trimmed, ignoreCase = true)
        }
        if (conflict) return false

        var changed = false
        val updated = _folders.value.map { folder ->
            if (folder.id == folderId) {
                changed = true
                folder.copy(name = trimmed)
            } else {
                folder
            }
        }
        if (changed) save(updated)
        return changed
    }

    fun togglePinFolder(folderId: String) {
        save(_folders.value.map { folder ->
            if (folder.id == folderId) folder.copy(isPinned = !folder.isPinned) else folder
        })
    }

    fun deleteFolder(folderId: String) {
        save(_folders.value.filterNot { it.id == folderId })
    }

    /**
     * Moves [playlistId] into [targetFolderId], or out to the root library if [targetFolderId] is null.
     * Enforces Musify's invariant that a playlist belongs to at most one folder.
     */
    fun movePlaylistToFolder(playlistId: String, targetFolderId: String?) {
        val updated = _folders.value.map { folder ->
            val without = folder.playlistIds.filterNot { it == playlistId }
            if (folder.id == targetFolderId) {
                folder.copy(playlistIds = without + playlistId)
            } else {
                folder.copy(playlistIds = without)
            }
        }
        save(updated)
    }

    /**
     * Batch-moves [playlistIds] into [targetFolderId] (removing them from any previous folders).
     */
    fun addPlaylistsToFolder(playlistIds: List<String>, targetFolderId: String) {
        if (playlistIds.isEmpty()) return
        val idSet = playlistIds.toSet()
        val updated = _folders.value.map { folder ->
            val without = folder.playlistIds.filterNot { it in idSet }
            if (folder.id == targetFolderId) {
                val toAppend = playlistIds.filter { it !in without }
                folder.copy(playlistIds = without + toAppend)
            } else {
                folder.copy(playlistIds = without)
            }
        }
        save(updated)
    }

    fun removePlaylistFromFolder(playlistId: String) {
        movePlaylistToFolder(playlistId, null)
    }

    companion object {
        const val PREFS_NAME = "playlist_folders"
        private const val KEY_FOLDERS = "folders_json"
    }
}
