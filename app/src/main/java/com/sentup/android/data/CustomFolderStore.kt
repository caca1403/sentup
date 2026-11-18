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
 * A user-created, customizable folder in the Music Library.
 * Supports arbitrary nesting via [parentId], custom [colorHex], pinning, and ordered [songIds].
 */
@Serializable
enum class FolderEqPreset {
    Default,
    BassBoost,
    Vocal,
    NightLoFi,
    Rock,
    Acoustic
}

@Serializable
data class FolderAutoRule(
    val keyword: String = "",
    val minPlays: Int = 0,
    val downloadedOnly: Boolean = false
) {
    val isActive: Boolean
        get() = keyword.isNotBlank() || minPlays > 0 || downloadedOnly
    val isEmpty: Boolean
        get() = !isActive
}

@Serializable
data class MusicFolder(
    val id: String,
    val name: String,
    val parentId: String? = null,
    val songIds: List<String> = emptyList(),
    val albumNames: List<String> = emptyList(),
    val artistNames: List<String> = emptyList(),
    val colorHex: Long? = null,
    val isPinned: Boolean = false,
    val playbackSpeed: Float? = null,
    val eqPreset: FolderEqPreset? = null,
    val autoRule: FolderAutoRule? = null,
    val coverUri: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Pure tree operations over a flat list of [MusicFolder]s, separated from Android
 * [Context] so folder hierarchy rules are deterministic and unit-testable.
 */
object CustomFolderTree {

    fun childFoldersOf(folders: List<MusicFolder>, parentId: String?): List<MusicFolder> =
        folders
            .filter { it.parentId == parentId }
            .sortedWith(
                compareByDescending<MusicFolder> { it.isPinned }
                    .thenBy { it.name.lowercase(Locale.ROOT) }
            )

    fun breadcrumbPath(folders: List<MusicFolder>, folderId: String?): List<MusicFolder> {
        if (folderId == null) return emptyList()
        val byId = folders.associateBy { it.id }
        val path = mutableListOf<MusicFolder>()
        val visited = mutableSetOf<String>()
        var currentId: String? = folderId
        while (currentId != null && visited.add(currentId)) {
            val folder = byId[currentId] ?: break
            path.add(folder)
            currentId = folder.parentId
        }
        return path.asReversed()
    }

    /**
     * Resolves all songs belonging directly to [folder] via its [MusicFolder.artistNames],
     * [MusicFolder.albumNames], and [MusicFolder.songIds], deduplicated by song ID.
     */
    fun resolveFolderSongs(
        folder: MusicFolder,
        allSongs: List<Song>,
        playCounts: Map<String, Int> = emptyMap(),
        downloadedIds: Set<String> = emptySet()
    ): List<Song> {
        val artistKeys = folder.artistNames.mapTo(mutableSetOf()) { it.trim().lowercase(Locale.ROOT) }
        val albumKeys = folder.albumNames.mapTo(mutableSetOf()) { it.trim().lowercase(Locale.ROOT) }
        val songIdSet = folder.songIds.toSet()

        val result = LinkedHashMap<String, Song>()
        if (artistKeys.isNotEmpty()) {
            allSongs.filter { it.artist.trim().lowercase(Locale.ROOT) in artistKeys }
                .forEach { result.putIfAbsent(it.id, it) }
        }
        if (albumKeys.isNotEmpty()) {
            allSongs.filter { it.album.trim().lowercase(Locale.ROOT) in albumKeys }
                .forEach { result.putIfAbsent(it.id, it) }
        }
        if (songIdSet.isNotEmpty()) {
            val byId = allSongs.associateBy { it.id }
            folder.songIds.mapNotNull { byId[it] }.forEach { result.putIfAbsent(it.id, it) }
        }
        val rule = folder.autoRule
        if (rule != null && rule.isActive) {
            val kw = rule.keyword.trim().lowercase(Locale.ROOT)
            allSongs.filter { song ->
                val matchesKw = kw.isEmpty() ||
                    song.title.lowercase(Locale.ROOT).contains(kw) ||
                    song.artist.lowercase(Locale.ROOT).contains(kw) ||
                    song.album.lowercase(Locale.ROOT).contains(kw)
                val matchesPlays = (playCounts[song.id] ?: 0) >= rule.minPlays
                val matchesDl = !rule.downloadedOnly || song.id in downloadedIds
                matchesKw && matchesPlays && matchesDl
            }.forEach { result.putIfAbsent(it.id, it) }
        }
        return result.values.toList()
    }

    /**
     * Collects all resolved songs in [folderId] and recursively in all descendant subfolders.
     */
    fun allDescendantSongs(
        folders: List<MusicFolder>,
        folderId: String,
        allSongs: List<Song>,
        playCounts: Map<String, Int> = emptyMap(),
        downloadedIds: Set<String> = emptySet()
    ): List<Song> {
        val byId = folders.associateBy { it.id }
        val childrenByParent = folders.groupBy { it.parentId }
        val result = LinkedHashMap<String, Song>()
        val visited = mutableSetOf<String>()

        fun visit(id: String) {
            if (!visited.add(id)) return
            byId[id]?.let { folder ->
                resolveFolderSongs(folder, allSongs, playCounts, downloadedIds)
                    .forEach { result.putIfAbsent(it.id, it) }
            }
            childrenByParent[id].orEmpty()
                .sortedWith(compareByDescending<MusicFolder> { it.isPinned }.thenBy { it.name.lowercase(Locale.ROOT) })
                .forEach { child -> visit(child.id) }
        }

        visit(folderId)
        return result.values.toList()
    }

    /**
     * Collects all song IDs directly in [folderId] and recursively in all descendant subfolders,
     * preserving folder-tree order without duplicates.
     */
    fun allDescendantSongIds(folders: List<MusicFolder>, folderId: String): List<String> {
        val byId = folders.associateBy { it.id }
        val childrenByParent = folders.groupBy { it.parentId }
        val result = LinkedHashSet<String>()
        val visited = mutableSetOf<String>()

        fun visit(id: String) {
            if (!visited.add(id)) return
            byId[id]?.songIds?.let { result.addAll(it) }
            childrenByParent[id].orEmpty()
                .sortedWith(compareByDescending<MusicFolder> { it.isPinned }.thenBy { it.name.lowercase(Locale.ROOT) })
                .forEach { child -> visit(child.id) }
        }

        visit(folderId)
        return result.toList()
    }

    /**
     * Collects all descendant folder IDs including [folderId] itself.
     */
    fun subtreeIds(folders: List<MusicFolder>, folderId: String): Set<String> {
        val childrenByParent = folders.groupBy { it.parentId }
        val collected = mutableSetOf<String>()
        fun visit(id: String) {
            if (!collected.add(id)) return
            childrenByParent[id].orEmpty().forEach { visit(it.id) }
        }
        visit(folderId)
        return collected
    }

    /**
     * Returns true if moving [folderId] under [newParentId] is valid (not self or descendant).
     */
    fun canMoveFolder(folders: List<MusicFolder>, folderId: String, newParentId: String?): Boolean {
        if (newParentId == null) return true
        return newParentId !in subtreeIds(folders, folderId)
    }

    /**
     * Returns songs from [allSongs] that are not assigned to any folder in [folders].
     */
    fun unfiledSongs(folders: List<MusicFolder>, allSongs: List<Song>): List<Song> {
        val filed = folders.flatMapTo(mutableSetOf()) { it.songIds }
        return allSongs.filter { it.id !in filed }
    }
}

/**
 * Persists user-created [MusicFolder] hierarchies in SharedPreferences as JSON.
 */
class CustomFolderStore(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    private val _folders = MutableStateFlow(loadFolders())
    val folders: StateFlow<List<MusicFolder>> = _folders.asStateFlow()

    private fun loadFolders(): List<MusicFolder> {
        val raw = prefs.getString(KEY_FOLDERS_JSON, null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<MusicFolder>>(raw) }.getOrDefault(emptyList())
    }

    private fun save(updated: List<MusicFolder>) {
        _folders.value = updated
        prefs.edit().putString(KEY_FOLDERS_JSON, json.encodeToString(updated)).apply()
    }

    fun createFolder(
        name: String,
        parentId: String? = null,
        colorHex: Long? = null
    ): MusicFolder {
        val trimmed = name.trim().ifEmpty { "New Folder" }
        val folder = MusicFolder(
            id = UUID.randomUUID().toString(),
            name = trimmed,
            parentId = parentId,
            colorHex = colorHex
        )
        save(_folders.value + folder)
        return folder
    }

    fun renameFolder(folderId: String, newName: String, newColorHex: Long? = null) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        save(_folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(name = trimmed, colorHex = newColorHex ?: folder.colorHex)
            } else {
                folder
            }
        })
    }

    fun deleteFolder(folderId: String) {
        val toRemove = CustomFolderTree.subtreeIds(_folders.value, folderId)
        save(_folders.value.filterNot { it.id in toRemove })
    }

    fun togglePinFolder(folderId: String) {
        save(_folders.value.map { folder ->
            if (folder.id == folderId) folder.copy(isPinned = !folder.isPinned) else folder
        })
    }

    fun addSongsToFolder(folderId: String, songIds: Collection<String>) {
        if (songIds.isEmpty()) return
        save(_folders.value.map { folder ->
            if (folder.id == folderId) {
                val merged = (folder.songIds + songIds).distinct()
                folder.copy(songIds = merged)
            } else {
                folder
            }
        })
    }

    fun removeSongFromFolder(folderId: String, songId: String) {
        save(_folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(songIds = folder.songIds.filterNot { it == songId })
            } else {
                folder
            }
        })
    }

    fun setFolderContent(
        folderId: String,
        artistNames: List<String>,
        albumNames: List<String>,
        songIds: List<String>
    ) {
        save(_folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(
                    artistNames = artistNames.distinct(),
                    albumNames = albumNames.distinct(),
                    songIds = songIds.distinct()
                )
            } else {
                folder
            }
        })
    }

    fun removeArtistFromFolder(folderId: String, artistName: String) {
        save(_folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(artistNames = folder.artistNames.filterNot { it.equals(artistName, ignoreCase = true) })
            } else folder
        })
    }

    fun removeAlbumFromFolder(folderId: String, albumName: String) {
        save(_folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(albumNames = folder.albumNames.filterNot { it.equals(albumName, ignoreCase = true) })
            } else folder
        })
    }

    fun setFolderAudioProfile(folderId: String, playbackSpeed: Float?, eqPreset: FolderEqPreset?) {
        save(_folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(playbackSpeed = playbackSpeed, eqPreset = eqPreset)
            } else folder
        })
    }

    fun setFolderAutoRule(folderId: String, autoRule: FolderAutoRule?) {
        save(_folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(autoRule = autoRule?.takeIf { it.isActive })
            } else folder
        })
    }

    fun setFolderCover(folderId: String, coverUri: String?) {
        val cleaned = coverUri?.trim()?.takeIf { it.isNotEmpty() }
        save(_folders.value.map { folder ->
            if (folder.id == folderId) {
                folder.copy(coverUri = cleaned)
            } else folder
        })
    }

    /** Copies a picked gallery image into internal app storage so permissions never expire. */
    fun savePickedCover(folderId: String, sourceUri: android.net.Uri): String? = runCatching {
        val coverDir = java.io.File(appContext.filesDir, "folder_covers").apply { mkdirs() }
        coverDir.listFiles()?.forEach { old ->
            if (old.name.startsWith("${folderId}_")) old.delete()
        }
        val dest = java.io.File(coverDir, "${folderId}_${System.currentTimeMillis()}.jpg")
        appContext.contentResolver.openInputStream(sourceUri)?.use { input ->
            dest.outputStream().use { output -> input.copyTo(output) }
        } ?: return null
        val savedUri = android.net.Uri.fromFile(dest).toString()
        setFolderCover(folderId, savedUri)
        savedUri
    }.getOrNull()

    fun moveFolder(folderId: String, newParentId: String?): Boolean {
        if (!CustomFolderTree.canMoveFolder(_folders.value, folderId, newParentId)) return false
        save(_folders.value.map { folder ->
            if (folder.id == folderId) folder.copy(parentId = newParentId) else folder
        })
        return true
    }

    /**
     * Materializes an automatic [ChannelFolder] (and its [AlbumFolder]s as subfolders)
     * into real customizable [MusicFolder]s under [parentId].
     */
    fun importChannelAsFolder(channel: ChannelFolder, parentId: String? = null): MusicFolder {
        val channelFolder = MusicFolder(
            id = UUID.randomUUID().toString(),
            name = channel.name,
            parentId = parentId
        )
        val albumSubfolders = channel.albums.map { album ->
            MusicFolder(
                id = UUID.randomUUID().toString(),
                name = album.name,
                parentId = channelFolder.id,
                songIds = album.songs.map { it.id }
            )
        }
        save(_folders.value + channelFolder + albumSubfolders)
        return channelFolder
    }

    /**
     * Serializes the entire custom folder hierarchy (including rules and audio profiles) to JSON.
     */
    fun exportFoldersJson(): String = json.encodeToString(_folders.value)

    /**
     * Imports folders from a JSON payload exported by [exportFoldersJson], skipping colliding IDs.
     * @return the number of imported folders, or -1 if the JSON is invalid.
     */
    fun importFoldersJson(rawJson: String): Int {
        val parsed = runCatching { json.decodeFromString<List<MusicFolder>>(rawJson.trim()) }
            .getOrNull() ?: return -1
        val existingIds = _folders.value.mapTo(mutableSetOf()) { it.id }
        val toAdd = parsed.filter { it.id.isNotBlank() && it.id !in existingIds }
        if (toAdd.isNotEmpty()) {
            save(_folders.value + toAdd)
        }
        return toAdd.size
    }

    companion object {
        private const val PREFS_NAME = "sentup_custom_music_folders"
        private const val KEY_FOLDERS_JSON = "folders_json"
    }
}
