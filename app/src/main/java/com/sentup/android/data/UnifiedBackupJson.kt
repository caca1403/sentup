package com.sentup.android.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Single unified JSON export/import bundle combining:
 * - YouTube local subscriptions
 * - Custom music folders (artists, albums, songs, rules, audio profiles)
 * - Custom Explore feeds (encoded via ExploreRules JSON)
 * - Track intro/outro trims & timestamped bookmarks
 */
@Serializable
data class UnifiedBackupBundle(
    val version: Int = 1,
    val subscriptions: List<SerializedSubscription> = emptyList(),
    val musicFoldersJson: String = "[]",
    val exploreListsJson: List<String> = emptyList(),
    val trackTrims: List<TrackTrim> = emptyList(),
    val songBookmarks: List<SongBookmark> = emptyList()
)

@Serializable
data class SerializedSubscription(
    val channelId: String,
    val name: String,
    val avatarUrl: String? = null,
    val handle: String? = null
)

data class UnifiedImportResult(
    val importedSubscriptions: Int,
    val importedFolders: Int,
    val importedExploreLists: Int
)

object UnifiedBackupJson {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = true
    }

    fun exportAll(context: Context): String {
        val subsRepo = LocalSubscriptionsRepository(context)
        val folderStore = CustomFolderStore(context)
        val exploreRepo = ExploreListsRepository(context)
        val trackStore = TrackCustomizationStore(context)

        val subs = subsRepo.subscriptions.value.map {
            SerializedSubscription(
                channelId = it.channelId,
                name = it.name,
                avatarUrl = it.avatarUrl,
                handle = it.handle
            )
        }
        val exploreJsons = exploreRepo.lists.value.map { list ->
            org.json.JSONObject().apply {
                put("name", list.name)
                put("pinned", list.pinned)
                put("rules", list.rules.toJson())
            }.toString()
        }

        val bundle = UnifiedBackupBundle(
            subscriptions = subs,
            musicFoldersJson = folderStore.exportFoldersJson(),
            exploreListsJson = exploreJsons,
            trackTrims = trackStore.trims.value.values.toList(),
            songBookmarks = trackStore.bookmarks.value.values.flatten()
        )
        return json.encodeToString(bundle)
    }

    suspend fun importAll(context: Context, rawJson: String): UnifiedImportResult? {
        val bundle = runCatching { json.decodeFromString<UnifiedBackupBundle>(rawJson.trim()) }
            .getOrNull() ?: return null

        val subsRepo = LocalSubscriptionsRepository(context)
        val folderStore = CustomFolderStore(context)
        val exploreRepo = ExploreListsRepository(context)
        val trackStore = TrackCustomizationStore(context)

        val importedSubs = if (bundle.subscriptions.isNotEmpty()) {
            subsRepo.importAll(
                bundle.subscriptions.map {
                    LocalSubscription(
                        channelId = it.channelId,
                        name = it.name,
                        avatarUrl = it.avatarUrl,
                        handle = it.handle
                    )
                }
            )
        } else 0

        val importedFolders = folderStore.importFoldersJson(bundle.musicFoldersJson).coerceAtLeast(0)

        var importedExplore = 0
        val existingExploreNames = exploreRepo.lists.value.mapTo(mutableSetOf()) { it.name.lowercase() }
        for (itemJson in bundle.exploreListsJson) {
            val obj = runCatching { org.json.JSONObject(itemJson) }.getOrNull() ?: continue
            val name = obj.optString("name").trim()
            if (name.isEmpty() || name.lowercase() in existingExploreNames) continue
            val rules = ExploreRules.fromJson(obj.optJSONObject("rules"))
            val pinned = obj.optBoolean("pinned", false)
            if (exploreRepo.create(name = name, rules = rules, pinned = pinned) != null) {
                existingExploreNames.add(name.lowercase())
                importedExplore++
            }
        }

        bundle.trackTrims.forEach { trim ->
            trackStore.setTrim(trim.songId, trim.startMs, trim.endMs)
        }
        bundle.songBookmarks.forEach { bm ->
            trackStore.addBookmark(bm.songId, bm.positionMs, bm.note)
        }

        return UnifiedImportResult(
            importedSubscriptions = importedSubs,
            importedFolders = importedFolders,
            importedExploreLists = importedExplore
        )
    }
}
