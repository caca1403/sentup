package com.sentup.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryFolderHierarchyTest {

    @Test
    fun `buildChannelFolderHierarchy groups by channel then album and places singles last`() {
        val songs = listOf(
            song(id = "s3", title = "Loose Single", artist = "Channel B", album = "<unknown>", track = null),
            song(id = "s2", title = "Track Two", artist = "Channel B", album = "Album X", track = 2, year = 2024),
            song(id = "s1", title = "Track One", artist = "Channel B", album = "Album X", track = 1, year = 2024),
            song(id = "a1", title = "Alpha Song", artist = "Channel A", album = "Debut", track = 1, year = 2020)
        )

        val hierarchy = buildChannelFolderHierarchy(
            songs = songs,
            playCounts = mapOf("s1" to 4, "s2" to 2),
            sortOrder = FolderSortOrder.Name,
            singlesLabel = "Singles",
            unknownChannelLabel = "Unknown"
        )

        assertEquals(listOf("Channel A", "Channel B"), hierarchy.map { it.name })
        val channelB = hierarchy[1]
        assertEquals(listOf("Album X", "Singles"), channelB.albums.map { it.name })
        assertEquals(listOf("s1", "s2"), channelB.albums[0].songs.map { it.id })
        assertTrue(channelB.albums[1].isSinglesBucket)
        assertEquals(6, channelB.totalPlays)
    }

    @Test
    fun `pinned channels always sort before unpinned channels`() {
        val songs = listOf(
            song(id = "1", title = "One", artist = "Alpha", album = "A"),
            song(id = "2", title = "Two", artist = "Zeta", album = "Z")
        )

        val hierarchy = buildChannelFolderHierarchy(
            songs = songs,
            playCounts = emptyMap(),
            sortOrder = FolderSortOrder.Name,
            pinnedChannels = setOf("Zeta")
        )

        assertEquals(listOf("Zeta", "Alpha"), hierarchy.map { it.name })
        assertTrue(hierarchy.first().isPinned)
    }

    @Test
    fun `filterLibrarySongs filters by query and chip`() {
        val songs = listOf(
            song(id = "1", title = "Midnight Drive", artist = "SynthWave", album = "Neon", source = SongSource.YOUTUBE),
            song(id = "2", title = "Morning Coffee", artist = "JazzClub", album = "Bistro", source = SongSource.LOCAL)
        )

        val result = filterLibrarySongs(
            songs = songs,
            query = "neon",
            filter = LibraryFilterChip.YouTube,
            downloadedIds = emptySet(),
            likedIds = emptySet(),
            playCounts = emptyMap()
        )

        assertEquals(listOf("1"), result.map { it.id })
    }

    @Test
    fun `buildLibrarySmartMixes generates expected non-empty mixes`() {
        val songs = listOf(
            song(id = "1", title = "Fast", artist = "A", album = "Al", duration = 120_000L, dateAdded = 100L),
            song(id = "2", title = "Long", artist = "B", album = "Bl", duration = 300_000L, dateAdded = 200L)
        )

        val mixes = buildLibrarySmartMixes(
            songs = songs,
            downloadedIds = setOf("2"),
            playCounts = mapOf("1" to 5)
        )

        val kinds = mixes.map { it.kind }.toSet()
        assertTrue(SmartMixKind.ArtistSpotlight in kinds)
        assertTrue(SmartMixKind.TopRotation in kinds)
        assertTrue(SmartMixKind.SmartDiscovery in kinds)
        assertTrue(SmartMixKind.FreshAdditions in kinds)
        assertTrue(SmartMixKind.OfflineReady in kinds)
        assertTrue(SmartMixKind.QuickBites in kinds)
        assertTrue(SmartMixKind.DeepDives in kinds)
    }

    @Test
    fun `computeLibraryDna summarizes counts and offline percentage`() {
        val songs = listOf(
            song(id = "1", title = "A", artist = "Ch1", album = "Alb1", duration = 100_000L),
            song(id = "2", title = "B", artist = "Ch1", album = "Alb2", duration = 200_000L)
        )

        val dna = computeLibraryDna(
            songs = songs,
            downloadedIds = setOf("1"),
            playCounts = mapOf("1" to 3)
        )

        assertEquals(2, dna.totalSongs)
        assertEquals(1, dna.totalChannels)
        assertEquals(2, dna.totalAlbums)
        assertEquals(300_000L, dna.totalDurationMs)
        assertEquals(50, dna.offlinePercent)
        assertEquals(1, dna.unplayedCount)
    }

    @Test
    fun `analyzeLibraryHealth detects duplicate songs and missing artist or album`() {
        val songs = listOf(
            song(id = "1", title = "Numb", artist = "Linkin Park", album = "Meteora"),
            song(id = "2", title = "numb ", artist = "linkin park", album = "Live"),
            song(id = "3", title = "Untitled Demo", artist = "<unknown>", album = "Bootlegs"),
            song(id = "4", title = "Loose Single", artist = "Indie Band", album = "<unknown>")
        )

        val report = analyzeLibraryHealth(songs)
        assertEquals(1, report.duplicateGroups.size)
        assertEquals(listOf("1", "2"), report.duplicateGroups.first().songs.map { it.id })
        assertEquals(listOf("3"), report.missingArtistSongs.map { it.id })
        assertEquals(listOf("4"), report.missingAlbumSongs.map { it.id })
        assertEquals(3, report.totalIssues)
    }

    private fun song(
        id: String,
        title: String,
        artist: String,
        album: String,
        duration: Long = 180_000L,
        track: Int? = null,
        year: Int? = null,
        source: SongSource = SongSource.YOUTUBE,
        dateAdded: Long? = null
    ) = Song(
        id = id,
        title = title,
        artist = artist,
        album = album,
        duration = duration,
        source = source,
        dateAdded = dateAdded,
        trackNumber = track,
        releaseYear = year
    )
}
