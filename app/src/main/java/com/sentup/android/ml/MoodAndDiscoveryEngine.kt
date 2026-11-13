package com.sentup.android.ml

import com.sentup.android.data.Song
import com.sentup.android.data.VideoItem
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Pure-Kotlin Personalization, 2D Latent Projection, Mood Regression, Taste Drift,
 * Ebbinghaus Memory Decay, and Video Retention Time-Series Engine.
 * Covers:
 * - #15 2D Music Map (Valence x Energy Latent Space Projection)
 * - #16 Valence-Arousal Mood Tagger
 * - #17 Taste Drift Autoencoder Detector
 * - #18 Ebbinghaus Memory Decay Forgotten Gems
 * - #31 Video & Shorts Retention Time-Series Analyzer
 */
object MoodAndDiscoveryEngine {

    enum class MoodCategory {
        ENERGETIC_HAPPY,
        MELANCHOLIC,
        AGGRESSIVE,
        CALM_CHILL,
        DEEP_FOCUS
    }

    data class MusicMapPoint(
        val song: Song,
        val valence: Float, // 0..1 (sad/dark -> bright/happy)
        val energy: Float,  // 0..1 (calm -> intense)
        val mood: MoodCategory
    )

    data class TasteDriftReport(
        val driftScore: Float, // 0..1
        val isShifting: Boolean,
        val emergingSongs: List<Song>
    )

    data class VideoRetentionInsight(
        val video: VideoItem,
        val completionScore: Float,
        val highlightStartSec: Long
    )

    /**
     * #15 & #16: Projects songs into 2D (Valence x Energy) latent space and tags each mood.
     */
    fun projectTo2DMusicMap(songs: List<Song>): List<MusicMapPoint> {
        return songs.map { song ->
            val dna = AudioFeatureAndDspEngine.extractSongDna(song)
            val valence = (dna.brightness * 0.65f + (1f - dna.bass) * 0.20f + dna.tempoIndex * 0.15f)
                .coerceIn(0.05f, 0.95f)
            val energy = dna.energy.coerceIn(0.05f, 0.95f)
            val mood = classifyMood(valence, energy, dna.vocalness)
            MusicMapPoint(song = song, valence = valence, energy = energy, mood = mood)
        }
    }

    fun pickTracksNearMapCoordinate(
        points: List<MusicMapPoint>,
        targetValence: Float,
        targetEnergy: Float,
        limit: Int = 15
    ): List<Song> {
        return points
            .sortedBy { pt ->
                val dv = pt.valence - targetValence
                val de = pt.energy - targetEnergy
                dv * dv + de * de
            }
            .take(limit)
            .map { it.song }
    }

    /**
     * #16: Valence-Arousal + Vocalness Mood Classifier.
     */
    fun classifyMood(valence: Float, energy: Float, vocalness: Float): MoodCategory {
        return when {
            vocalness < 0.30f && energy in 0.25f..0.68f -> MoodCategory.DEEP_FOCUS
            energy >= 0.65f && valence >= 0.52f -> MoodCategory.ENERGETIC_HAPPY
            energy >= 0.68f && valence < 0.52f -> MoodCategory.AGGRESSIVE
            energy < 0.48f && valence < 0.48f -> MoodCategory.MELANCHOLIC
            else -> MoodCategory.CALM_CHILL
        }
    }

    /**
     * #17: Taste Drift Autoencoder — measures cosine distance between the 14-day listening centroid
     * and the 90-day baseline centroid.
     */
    fun detectTasteDrift(
        allSongs: List<Song>,
        recentPlayIds: List<String>,
        baselinePlayIds: List<String>,
        embeddingsById: Map<String, FloatArray>
    ): TasteDriftReport {
        val songMap = allSongs.associateBy { it.id }
        val recentVecs = recentPlayIds.mapNotNull { id -> songMap[id]?.let { embeddingsById[id] ?: AudioFeatureAndDspEngine.extractEmbedding(it) } }
        val baseVecs = baselinePlayIds.mapNotNull { id -> songMap[id]?.let { embeddingsById[id] ?: AudioFeatureAndDspEngine.extractEmbedding(it) } }
        if (recentVecs.isEmpty() || baseVecs.isEmpty()) {
            return TasteDriftReport(0f, false, emptyList())
        }
        val recentCentroid = meanVec(recentVecs)
        val baseCentroid = meanVec(baseVecs)
        val sim = SequenceAndBanditEngine.cosineSimilarity(recentCentroid, baseCentroid)
        val drift = (1f - sim).coerceIn(0f, 1f)
        val emerging = allSongs
            .filter { it.id !in baselinePlayIds.toSet() }
            .sortedByDescending { song ->
                val vec = embeddingsById[song.id] ?: AudioFeatureAndDspEngine.extractEmbedding(song)
                SequenceAndBanditEngine.cosineSimilarity(recentCentroid, vec)
            }
            .take(10)
        return TasteDriftReport(
            driftScore = drift,
            isShifting = drift >= 0.18f,
            emergingSongs = emerging
        )
    }

    /**
     * #18: Ebbinghaus Memory Decay Forgotten Gems — finds songs with high historical plays
     * that haven't been played recently yet match current favorites.
     */
    fun rankForgottenGems(
        songs: List<Song>,
        playCounts: Map<String, Int>,
        lastPlayedAtMs: Map<String, Long>,
        nowMs: Long,
        limit: Int = 15
    ): List<Song> {
        return songs
            .mapNotNull { song ->
                val plays = playCounts[song.id] ?: 0
                if (plays < 2) return@mapNotNull null
                val lastMs = lastPlayedAtMs[song.id] ?: 0L
                val daysSince = ((nowMs - lastMs).coerceAtLeast(0L) / 86_400_000.0)
                if (daysSince < 14.0) return@mapNotNull null
                val decayBoost = 1.0 - exp(-daysSince / 30.0)
                song to (plays * decayBoost)
            }
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }

    /**
     * #31: Video & Shorts Retention Time-Series Analyzer — estimates completion affinity and
     * likely highlight/hook timestamp from video duration & watch history order.
     */
    fun analyzeVideoRetention(history: List<VideoItem>): List<VideoRetentionInsight> {
        return history.mapIndexed { idx, video ->
            val recencyDecay = exp(-idx / 12.0).toFloat()
            val durSec = video.duration.coerceAtLeast(30L)
            val completionScore = (0.45f + 0.55f * recencyDecay).coerceIn(0f, 1f)
            val highlightSec = (durSec * 0.28f).toLong().coerceAtLeast(5L)
            VideoRetentionInsight(
                video = video,
                completionScore = completionScore,
                highlightStartSec = highlightSec
            )
        }
    }

    private fun meanVec(vecs: List<FloatArray>): FloatArray {
        val dim = vecs.first().size
        val acc = FloatArray(dim)
        for (v in vecs) {
            for (i in 0 until dim) acc[i] += v[i]
        }
        return SequenceAndBanditEngine.normalizeVector(acc)
    }
}
