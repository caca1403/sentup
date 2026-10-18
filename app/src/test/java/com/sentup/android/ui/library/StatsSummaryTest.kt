package com.sentup.android.ui.library

import com.sentup.android.data.PlayHistoryEntry
import com.sentup.android.data.SongSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StatsSummaryTest {

    private val now = 1_700_000_000_000L
    private val day = 24L * 60 * 60 * 1000

    private fun entry(
        songId: String,
        title: String,
        artist: String,
        album: String,
        timestamp: Long,
        duration: Long = 180_000L
    ) = PlayHistoryEntry(
        songId = songId,
        title = title,
        artist = artist,
        album = album,
        timestamp = timestamp,
        duration = duration,
        source = SongSource.YOUTUBE
    )

    @Test
    fun `empty history yields an empty summary`() {
        val summary = summarizeHistory(emptyList(), now)
        assertEquals(0, summary.plays)
        assertEquals(0, summary.totalSeconds)
        assertTrue(summary.topSongs.isEmpty())
        assertTrue(summary.topArtists.isEmpty())
        assertTrue(summary.topAlbums.isEmpty())
    }

    @Test
    fun `totals count only the scoped window`() {
        val history = listOf(
            entry("1", "A", "Artist A", "Album A", now - 1 * day),
            entry("2", "B", "Artist B", "Album B", now - 10 * day)
        )
        val week = summarizeHistory(history, now, StatsScope.Last7Days)
        assertEquals(1, week.plays)
        val all = summarizeHistory(history, now, StatsScope.AllTime)
        assertEquals(2, all.plays)
    }

    @Test
    fun `duration is read as milliseconds`() {
        val history = listOf(
            entry("1", "A", "A", "A", now, duration = 180_000L),
            entry("2", "B", "B", "B", now, duration = 60_000L)
        )
        val summary = summarizeHistory(history, now, StatsScope.AllTime)
        assertEquals(240, summary.totalSeconds)
    }

    @Test
    fun `top songs are ranked by play count`() {
        val history = listOf(
            entry("1", "Hit", "Artist", "Album", now),
            entry("1", "Hit", "Artist", "Album", now - 1 * day),
            entry("2", "Once", "Artist", "Album", now)
        )
        val summary = summarizeHistory(history, now, StatsScope.AllTime)
        assertEquals("Hit", summary.topSongs.first().title)
        assertEquals(2, summary.topSongs.first().plays)
    }

    @Test
    fun `top artists and albums are computed`() {
        val history = listOf(
            entry("1", "A", "Artist A", "Album A", now),
            entry("2", "B", "Artist A", "Album A", now - 1 * day),
            entry("3", "C", "Artist B", "Album B", now)
        )
        val summary = summarizeHistory(history, now, StatsScope.AllTime)
        assertEquals("Artist A", summary.topArtists.first().title)
        assertEquals(2, summary.topArtists.first().plays)
        assertEquals("Album A", summary.topAlbums.first().title)
        assertEquals(2, summary.topAlbums.first().plays)
    }

    @Test
    fun `blank albums are excluded from top albums`() {
        val history = listOf(
            entry("1", "A", "Artist", "", now),
            entry("2", "B", "Artist", "Real Album", now - 1 * day)
        )
        val summary = summarizeHistory(history, now, StatsScope.AllTime)
        assertEquals(1, summary.topAlbums.size)
        assertEquals("Real Album", summary.topAlbums.first().title)
    }

    @Test
    fun `week comparison counts last week against the week before`() {
        val history = listOf(
            entry("1", "A", "A", "A", now - 1 * day),
            entry("2", "B", "B", "B", now - 2 * day),
            entry("3", "C", "C", "C", now - 10 * day)
        )
        val summary = summarizeHistory(history, now, StatsScope.AllTime)
        assertEquals(2, summary.lastWeekPlays)
        assertEquals(1, summary.previousWeekPlays)
    }

    @Test
    fun `top lists are capped`() {
        // Distinct artists and albums, or the grouping collapses them to one row
        // and the cap never comes into play.
        val history = (1..10).map {
            entry("$it", "Song $it", "Artist $it", "Album ${it % 7}", now)
        }
        val summary = summarizeHistory(history, now, StatsScope.AllTime)
        assertEquals(STATS_TOP_LIMIT, summary.topSongs.size)
        assertEquals(STATS_TOP_LIMIT, summary.topArtists.size)
        assertEquals(STATS_TOP_LIMIT, summary.topAlbums.size)
    }
}
