package com.sentup.android.data

import java.util.Locale

/**
 * A single album folder nested under a [ChannelFolder] in the Music Library.
 */
data class AlbumFolder(
    val name: String,
    val channelName: String,
    val isSinglesBucket: Boolean,
    val releaseYear: Int?,
    val songs: List<Song>,
    val totalDurationMs: Long,
    val totalPlays: Int,
    val artworkUrl: String?
)

/**
 * A top-level channel / artist folder containing its [AlbumFolder]s and flattened [allSongs].
 */
data class ChannelFolder(
    val name: String,
    val albums: List<AlbumFolder>,
    val allSongs: List<Song>,
    val totalDurationMs: Long,
    val totalPlays: Int,
    val artworkUrl: String?,
    val isPinned: Boolean = false
)

/**
 * Sort orders for the Channel -> Album -> Song folder browser.
 */
enum class FolderSortOrder {
    Name,
    MostSongs,
    MostPlayed,
    TotalDuration
}

/**
 * Quick filter chips for the Music Library's instant search & filter bar.
 */
enum class LibraryFilterChip {
    All,
    Downloaded,
    Liked,
    Local,
    YouTube,
    Unplayed
}

/**
 * Auto-generated smart mix categories derived from the user's own library and play counts.
 */
enum class SmartMixKind {
    ArtistSpotlight,
    SmartDiscovery,
    TopRotation,
    ForgottenFavorites,
    FreshAdditions,
    OfflineReady,
    QuickBites,
    DeepDives
}

/**
 * A dynamic smart mix ready for one-tap playback or shuffling, with an optional dynamic [focusLabel]
 * (such as the spotlighted artist name).
 */
data class LibrarySmartMix(
    val kind: SmartMixKind,
    val songs: List<Song>,
    val totalDurationMs: Long,
    val focusLabel: String? = null
)

/**
 * Compact overview metrics ("Library DNA") for the user's Music Library.
 */
data class LibraryDnaStats(
    val totalSongs: Int,
    val totalChannels: Int,
    val totalAlbums: Int,
    val totalDurationMs: Long,
    val offlinePercent: Int,
    val unplayedCount: Int
)

private fun Song.preferredArtworkUrl(): String? =
    albumArtUri?.toString() ?: thumbnailUrl

/**
 * Builds the 3-level Channel -> Album -> Song hierarchy for the Music Library.
 *
 * Pinned channels stay at the top while preserving the selected [sortOrder]
 * within both the pinned and unpinned groups.
 */
fun buildChannelFolderHierarchy(
    songs: List<Song>,
    playCounts: Map<String, Int>,
    sortOrder: FolderSortOrder = FolderSortOrder.Name,
    pinnedChannels: Set<String> = emptySet(),
    singlesLabel: String = "Singles & Loose Tracks",
    unknownChannelLabel: String = "Unknown Channel"
): List<ChannelFolder> {
    if (songs.isEmpty()) return emptyList()

    val normalizedPins = pinnedChannels.mapTo(mutableSetOf()) { it.trim().lowercase(Locale.ROOT) }

    val channelFolders = songs
        .groupBy { song ->
            if (isUnknownArtist(song.artist)) unknownChannelLabel else song.artist.trim()
        }
        .map { (channelName, channelSongs) ->
            val albumFolders = channelSongs
                .groupBy { song ->
                    if (isUnknownAlbum(song.album)) null else song.album.trim()
                }
                .map { (albumNameOrNull, rawAlbumSongs) ->
                    val isSingles = albumNameOrNull == null
                    val orderedSongs = rawAlbumSongs.sortedInAlbumOrder()
                    val releaseYear = orderedSongs.mapNotNull { it.releaseYear }.maxOrNull()
                    AlbumFolder(
                        name = albumNameOrNull ?: singlesLabel,
                        channelName = channelName,
                        isSinglesBucket = isSingles,
                        releaseYear = releaseYear,
                        songs = orderedSongs,
                        totalDurationMs = orderedSongs.sumOf { it.duration.coerceAtLeast(0L) },
                        totalPlays = orderedSongs.sumOf { playCounts[it.id] ?: 0 },
                        artworkUrl = orderedSongs.firstNotNullOfOrNull { it.preferredArtworkUrl() }
                    )
                }
                .sortedWith(
                    compareBy<AlbumFolder> { if (it.isSinglesBucket) 1 else 0 }
                        .thenByDescending { it.releaseYear ?: Int.MIN_VALUE }
                        .thenBy { it.name.lowercase(Locale.ROOT) }
                )

            val flattenedSongs = albumFolders.flatMap { it.songs }
            ChannelFolder(
                name = channelName,
                albums = albumFolders,
                allSongs = flattenedSongs,
                totalDurationMs = flattenedSongs.sumOf { it.duration.coerceAtLeast(0L) },
                totalPlays = flattenedSongs.sumOf { playCounts[it.id] ?: 0 },
                artworkUrl = flattenedSongs.firstNotNullOfOrNull { it.preferredArtworkUrl() },
                isPinned = channelName.trim().lowercase(Locale.ROOT) in normalizedPins
            )
        }

    val byName = compareBy<ChannelFolder> { it.name.lowercase(Locale.ROOT) }
    val orderComparator = when (sortOrder) {
        FolderSortOrder.Name -> byName
        FolderSortOrder.MostSongs -> compareByDescending<ChannelFolder> { it.allSongs.size }.then(byName)
        FolderSortOrder.MostPlayed -> compareByDescending<ChannelFolder> { it.totalPlays }.then(byName)
        FolderSortOrder.TotalDuration -> compareByDescending<ChannelFolder> { it.totalDurationMs }.then(byName)
    }

    return channelFolders.sortedWith(
        compareByDescending<ChannelFolder> { it.isPinned }.then(orderComparator)
    )
}

/**
 * Filters [songs] by free-text [query] (title, channel/artist, or album) and [filter] chip.
 */
fun filterLibrarySongs(
    songs: List<Song>,
    query: String,
    filter: LibraryFilterChip,
    downloadedIds: Set<String>,
    likedIds: Set<String>,
    playCounts: Map<String, Int>
): List<Song> {
    val trimmedQuery = query.trim().lowercase(Locale.ROOT)
    return songs.filter { song ->
        val matchesFilter = when (filter) {
            LibraryFilterChip.All -> true
            LibraryFilterChip.Downloaded -> song.id in downloadedIds
            LibraryFilterChip.Liked -> song.id in likedIds
            LibraryFilterChip.Local -> song.source == SongSource.LOCAL
            LibraryFilterChip.YouTube -> song.source == SongSource.YOUTUBE
            LibraryFilterChip.Unplayed -> (playCounts[song.id] ?: 0) == 0
        }
        if (!matchesFilter) return@filter false
        if (trimmedQuery.isEmpty()) return@filter true

        song.title.lowercase(Locale.ROOT).contains(trimmedQuery) ||
            song.artist.lowercase(Locale.ROOT).contains(trimmedQuery) ||
            song.album.lowercase(Locale.ROOT).contains(trimmedQuery)
    }
}

/**
 * Builds non-empty smart auto-mixes from the user's current library state.
 */
fun buildLibrarySmartMixes(
    songs: List<Song>,
    downloadedIds: Set<String>,
    playCounts: Map<String, Int>
): List<LibrarySmartMix> {
    if (songs.isEmpty()) return emptyList()

    fun mixOf(kind: SmartMixKind, list: List<Song>, focusLabel: String? = null): LibrarySmartMix? =
        if (list.isEmpty()) null else LibrarySmartMix(
            kind = kind,
            songs = list,
            totalDurationMs = list.sumOf { it.duration.coerceAtLeast(0L) },
            focusLabel = focusLabel
        )

    // Compute artist affinity from total play counts per artist.
    val artistPlays = songs
        .filterNot { isUnknownArtist(it.artist) }
        .groupBy { it.artist.trim() }
        .mapValues { (_, list) -> list.sumOf { playCounts[it.id] ?: 0 } }
    val topArtistEntry = artistPlays.entries
        .filter { it.value > 0 }
        .maxByOrNull { it.value }

    val artistSpotlightSongs = topArtistEntry?.let { (artist, _) ->
        songs.filter { it.artist.trim().equals(artist, ignoreCase = true) }
            .sortedByDescending { playCounts[it.id] ?: 0 }
            .take(25)
    }.orEmpty()

    // High-affinity discovery: unplayed songs by artists the user already enjoys come first.
    val smartDiscovery = songs
        .filter { (playCounts[it.id] ?: 0) == 0 }
        .sortedByDescending { artistPlays[it.artist.trim()] ?: 0 }
        .take(30)

    val topRotation = songs
        .filter { (playCounts[it.id] ?: 0) > 0 }
        .sortedByDescending { playCounts[it.id] ?: 0 }
        .take(25)

    val topIds = topRotation.take(5).mapTo(mutableSetOf()) { it.id }
    val forgottenFavorites = songs
        .filter { (playCounts[it.id] ?: 0) >= 2 && it.id !in topIds }
        .sortedByDescending { playCounts[it.id] ?: 0 }
        .take(25)

    val freshAdditions = songs
        .filter { it.dateAdded != null }
        .sortedByDescending { it.dateAdded ?: Long.MIN_VALUE }
        .take(25)

    val offlineReady = songs
        .filter { it.id in downloadedIds }
        .take(50)

    val quickBites = songs
        .filter { it.duration in 1L..180_000L }
        .take(25)

    val deepDives = songs
        .filter { it.duration >= 240_000L }
        .take(25)

    return listOfNotNull(
        mixOf(SmartMixKind.ArtistSpotlight, artistSpotlightSongs, topArtistEntry?.key),
        mixOf(SmartMixKind.SmartDiscovery, smartDiscovery),
        mixOf(SmartMixKind.TopRotation, topRotation),
        mixOf(SmartMixKind.ForgottenFavorites, forgottenFavorites),
        mixOf(SmartMixKind.FreshAdditions, freshAdditions),
        mixOf(SmartMixKind.OfflineReady, offlineReady),
        mixOf(SmartMixKind.QuickBites, quickBites),
        mixOf(SmartMixKind.DeepDives, deepDives)
    )
}

/**
 * Computes compact summary stats ("Library DNA") for the Music Library.
 */
fun computeLibraryDna(
    songs: List<Song>,
    downloadedIds: Set<String>,
    playCounts: Map<String, Int>
): LibraryDnaStats {
    if (songs.isEmpty()) {
        return LibraryDnaStats(0, 0, 0, 0L, 0, 0)
    }
    val channels = songs.asSequence()
        .filterNot { isUnknownArtist(it.artist) }
        .map { it.artist.trim().lowercase(Locale.ROOT) }
        .distinct()
        .count()
    val albums = songs.asSequence()
        .filterNot { isUnknownAlbum(it.album) }
        .map { it.album.trim().lowercase(Locale.ROOT) }
        .distinct()
        .count()
    val totalDurationMs = songs.sumOf { it.duration.coerceAtLeast(0L) }
    val downloadedCount = songs.count { it.id in downloadedIds }
    val offlinePercent = ((downloadedCount * 100f) / songs.size).toInt().coerceIn(0, 100)
    val unplayedCount = songs.count { (playCounts[it.id] ?: 0) == 0 }
    return LibraryDnaStats(
        totalSongs = songs.size,
        totalChannels = channels,
        totalAlbums = albums,
        totalDurationMs = totalDurationMs,
        offlinePercent = offlinePercent,
        unplayedCount = unplayedCount
    )
}

/**
 * A group of songs that share the same normalized title and artist (e.g. a local file
 * and a YouTube download/like of the same track).
 */
data class DuplicateSongGroup(
    val normalizedKey: String,
    val title: String,
    val artist: String,
    val songs: List<Song>
)

/**
 * Health scan report for the Music Library: duplicate tracks and tracks missing artist/album tags.
 */
data class LibraryHealthReport(
    val duplicateGroups: List<DuplicateSongGroup>,
    val missingArtistSongs: List<Song>,
    val missingAlbumSongs: List<Song>
) {
    val totalIssues: Int
        get() = duplicateGroups.size + missingArtistSongs.size + missingAlbumSongs.size
}

/**
 * Scans [songs] for duplicate title+artist pairs and missing artist/album metadata.
 */
fun analyzeLibraryHealth(songs: List<Song>): LibraryHealthReport {
    if (songs.isEmpty()) {
        return LibraryHealthReport(emptyList(), emptyList(), emptyList())
    }
    val normalize: (String) -> String = { raw ->
        raw.trim().lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
    }

    val duplicateGroups = songs
        .groupBy { "${normalize(it.title)}::${normalize(it.artist)}" }
        .filter { (_, group) -> group.size > 1 }
        .map { (key, group) ->
            DuplicateSongGroup(
                normalizedKey = key,
                title = group.first().title,
                artist = group.first().artist,
                songs = group
            )
        }
        .sortedByDescending { it.songs.size }

    val missingArtist = songs.filter { isUnknownArtist(it.artist) }
    val missingAlbum = songs.filter { isUnknownAlbum(it.album) }

    return LibraryHealthReport(
        duplicateGroups = duplicateGroups,
        missingArtistSongs = missingArtist,
        missingAlbumSongs = missingAlbum
    )
}
