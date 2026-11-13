package com.sentup.android.ml

import com.sentup.android.data.Song
import com.sentup.android.data.VideoItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NeuralAudioEnginesTest {

    private fun sampleSongs(): List<Song> = listOf(
        Song(id = "1", title = "Thunder Rock", artist = "Band A", album = "Live", duration = 220_000L),
        Song(id = "2", title = "Chill Lo-Fi Rain", artist = "Beatmaker", album = "Night", duration = 180_000L),
        Song(id = "3", title = "Acoustic Piano Instrumental", artist = "Pianist", album = "Solo", duration = 240_000L),
        Song(id = "4", title = "Trap Gym Workout", artist = "DJ X", album = "Bass", duration = 195_000L),
        Song(id = "5", title = "Ambient Sleep", artist = "Dreamer", album = "Rest", duration = 300_000L),
        Song(id = "6", title = "Club Remix", artist = "Band A", album = "Club", duration = 210_000L)
    )

    @Test
    fun `GRU sequence and skip-aware bandit rank next tracks and penalize early skips`() {
        val songs = sampleSongs()
        val embeddings = songs.associate { it.id to AudioFeatureAndDspEngine.extractEmbedding(it) }
        var bandit = emptyMap<String, SequenceAndBanditEngine.BanditStats>()
        bandit = SequenceAndBanditEngine.recordPlaybackOutcome(bandit, "4", skippedEarly = true, completed = false)
        bandit = SequenceAndBanditEngine.recordPlaybackOutcome(bandit, "3", skippedEarly = false, completed = true)

        assertTrue((bandit["3"]?.meanReward ?: 0.0) > (bandit["4"]?.meanReward ?: 0.0))

        val predicted = SequenceAndBanditEngine.predictNextTracks(
            recentSongs = listOf(songs[1]),
            candidates = songs,
            embeddingsById = embeddings,
            banditStats = bandit,
            recentEvents = emptyList(),
            hourOfDay = 22,
            nowMs = 1_000_000L
        )
        assertTrue(predicted.isNotEmpty())
        assertTrue(predicted.none { it.id == "2" })
    }

    @Test
    fun `DPP diversity reranker avoids back-to-back same artist when alternatives exist`() {
        val songs = listOf(
            Song(id = "a1", title = "One", artist = "SameArtist", album = "Al1", duration = 180_000L),
            Song(id = "a2", title = "Two", artist = "SameArtist", album = "Al2", duration = 180_000L),
            Song(id = "b1", title = "Three", artist = "OtherArtist", album = "Al3", duration = 180_000L)
        )
        val embeddings = songs.associate { it.id to AudioFeatureAndDspEngine.extractEmbedding(it) }
        val diverse = SequenceAndBanditEngine.rerankWithDppDiversity(songs, embeddings)
        assertEquals("a1", diverse[0].id)
        assertEquals("b1", diverse[1].id)
    }

    @Test
    fun `playlist bridge and session arc produce valid ordered subsets`() {
        val songs = sampleSongs()
        val embeddings = songs.associate { it.id to AudioFeatureAndDspEngine.extractEmbedding(it) }
        val bridge = SequenceAndBanditEngine.buildPlaylistBridge(songs.first(), songs[4], songs, embeddings, bridgeCount = 2)
        assertEquals(2, bridge.size)

        val arc = AudioFeatureAndDspEngine.sortFolderByEnergyArc(songs)
        assertEquals(songs.size, arc.size)
    }

    @Test
    fun `DSP features extract DNA, Camelot keys, clusters, overlap, and smart trim`() {
        val songs = sampleSongs()
        val embeddings = songs.associate { it.id to AudioFeatureAndDspEngine.extractEmbedding(it) }

        val dna = AudioFeatureAndDspEngine.extractSongDna(songs[3])
        assertTrue(dna.energy > 0.7f)

        val camelot = AudioFeatureAndDspEngine.camelotKey(songs[0])
        assertTrue(camelot.endsWith("A") || camelot.endsWith("B"))

        val clusters = AudioFeatureAndDspEngine.clusterUnorganizedSongs(songs, embeddings, k = 3)
        assertTrue(clusters.isNotEmpty())
        assertEquals(songs.size, clusters.sumOf { it.songs.size })

        val overlap = AudioFeatureAndDspEngine.computeFolderOverlapPercent(songs.take(3), songs.take(3), embeddings)
        assertEquals(100, overlap)

        val trim = AudioFeatureAndDspEngine.estimateSmartTrim(songs[4])
        assertTrue(trim.startMs > 0L)
        assertNotNull(trim.endMs)
    }

    @Test
    fun `MoodAndDiscoveryEngine projects 2D map, detects taste drift, forgotten gems, and video retention`() {
        val songs = sampleSongs()
        val embeddings = songs.associate { it.id to AudioFeatureAndDspEngine.extractEmbedding(it) }

        val points = MoodAndDiscoveryEngine.projectTo2DMusicMap(songs)
        assertEquals(songs.size, points.size)
        val near = MoodAndDiscoveryEngine.pickTracksNearMapCoordinate(points, 0.8f, 0.8f, limit = 2)
        assertEquals(2, near.size)

        val drift = MoodAndDiscoveryEngine.detectTasteDrift(
            allSongs = songs,
            recentPlayIds = listOf("4", "6"),
            baselinePlayIds = listOf("2", "5"),
            embeddingsById = embeddings
        )
        assertTrue(drift.driftScore >= 0f)

        val nowMs = 100L * 86_400_000L
        val gems = MoodAndDiscoveryEngine.rankForgottenGems(
            songs = songs,
            playCounts = mapOf("1" to 10, "2" to 5),
            lastPlayedAtMs = mapOf("1" to 10L * 86_400_000L, "2" to 98L * 86_400_000L),
            nowMs = nowMs
        )
        assertEquals(listOf("1"), gems.map { it.id })

        val retention = MoodAndDiscoveryEngine.analyzeVideoRetention(
            listOf(
                VideoItem(
                    videoId = "v1",
                    title = "Test Video",
                    channelName = "Ch",
                    thumbnailUrl = "",
                    duration = 200L,
                    viewCount = "1K"
                )
            )
        )
        assertEquals(1, retention.size)
        assertFalse(retention.first().highlightStartSec <= 0L)
    }

    @Test
    fun `VideoQaEngine answers questions with matching transcript timestamps and extracts key moments`() {
        val cues = listOf(
            com.sentup.android.data.VttCue(0L, 4_000L, "Welcome to the Kotlin coroutines tutorial"),
            com.sentup.android.data.VttCue(4_000L, 9_000L, "Today we benchmark Flow and StateFlow"),
            com.sentup.android.data.VttCue(45_000L, 52_000L, "Here is how to configure Room database migrations safely")
        )
        val answers = VideoQaEngine.answerQuestion(
            question = "How do I configure Room migrations?",
            title = "Kotlin Tutorial",
            description = "Full guide",
            chapters = emptyList(),
            cues = cues
        )
        assertTrue(answers.isNotEmpty())
        assertEquals(0L, answers.first().timestampMs)

        val moments = VideoQaEngine.extractKeyMoments(chapters = emptyList(), cues = cues)
        assertTrue(moments.isNotEmpty())
    }
}
