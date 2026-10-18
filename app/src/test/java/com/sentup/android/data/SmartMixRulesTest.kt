package com.sentup.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartMixRulesTest {

    private fun song(id: String, artist: String, durationMs: Long = 0L) =
        Song.fromYouTube(id, "t$id", artist, "", durationMs, null)

    private fun play(id: String, artist: String, daysAgo: Long, now: Long) =
        PlayHistoryEntry(
            songId = id,
            title = "t$id",
            artist = artist,
            album = "",
            timestamp = now - daysAgo * 24L * 60 * 60 * 1000,
            duration = 0L,
        )

    @Test
    fun durationMixFillsToTargetUsingAssumedLength() {
        val pool = (1..20).map { song("s$it", "a$it") }
        val mix = SmartMixRules.durationMix(pool, 10L * 60_000, "10 dk")
        // 3.5 min assumed each -> 3 tracks cross ten minutes.
        assertEquals(3, mix.tracks.size)
    }

    @Test
    fun shelfBlendIsRoundRobinAndDeduplicated() {
        val a = listOf(song("1", "a"), song("2", "a"))
        val b = listOf(song("1", "b"), song("3", "b"))
        val blend = SmartMixRules.shelfBlend(listOf(a, b))
        assertEquals(listOf("1", "2", "3"), blend.tracks.map { it.id })
    }

    @Test
    fun dailyTenIsStableWithinADayAndRotatesAcrossDays() {
        val pool = (1..30).map { listOf(song("s$it", "a$it")) }
        val day1 = SmartMixRules.dailyTen(pool, 1)
        val day1again = SmartMixRules.dailyTen(pool, 1)
        val day2 = SmartMixRules.dailyTen(pool, 2)
        assertEquals(day1.tracks.map { it.id }, day1again.tracks.map { it.id })
        assertTrue(day1.tracks.first().id != day2.tracks.first().id)
    }

    @Test
    fun discoveryBridgeAlternatesFamiliarAndFresh() {
        val familiar = (1..5).map { song("f$it", "x") }
        val fresh = (1..5).map { song("n$it", "y") }
        val bridge = SmartMixRules.discoveryBridge(familiar, fresh)
        assertEquals(listOf("f1", "n1", "f2", "n2", "f3", "n3", "f4", "n4", "f5", "n5"),
            bridge.tracks.map { it.id })
    }

    @Test
    fun neverPlayedDropsWhatWasPlayed() {
        val all = (1..6).map { song("s$it", "a") }
        val mix = SmartMixRules.neverPlayed(all, setOf("s1", "s3"))
        assertEquals(listOf("s2", "s4", "s5", "s6"), mix.tracks.map { it.id })
    }

    @Test
    fun timeCapsulePicksLastYearWindowOnly() {
        val now = 1_900_000_000_000L
        val history = listOf(
            play("old", "a", 365, now),
            play("recent", "b", 3, now),
        )
        val capsule = SmartMixRules.timeCapsule(history, now)
        assertEquals(listOf("old"), capsule.tracks.map { it.id })
    }

    @Test
    fun longTailPutsRareArtistsFirst() {
        val now = 1_900_000_000_000L
        val history = listOf(
            play("common1", "star", 1, now),
            play("common2", "star", 2, now),
            play("rare", "nobody", 3, now),
        )
        val tail = SmartMixRules.longTail(history)
        assertEquals("rare", tail.tracks.first().id)
    }

    @Test
    fun moodBlendReadsOneTrackPerMood() {
        val a = listOf(song("a1", "x"), song("a2", "x"))
        val b = listOf(song("b1", "y"), song("b2", "y"))
        val blend = SmartMixRules.moodBlend(listOf(a, b))
        assertEquals(listOf("a1", "b1", "a2", "b2"), blend.tracks.map { it.id })
    }

    @Test
    fun artistSpacedNeverRepeatsAnArtistBackToBack() {
        val songs = (1..3).flatMap { n -> (1..3).map { song("$n-$it", "artist$n") } }
        val out = SmartMixRules.artistSpaced(songs, seed = 7L).map { it.artist }
        assertEquals(songs.size, out.size)
        out.zipWithNext().forEach { (a, b) -> assertTrue("adjacent $a == $b", a != b) }
    }
}
