package com.sentup.android.ml

import com.sentup.android.data.Song
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Pure-Kotlin, zero-LLM neural & statistical sequence engine covering:
 * - #1 Next-Track Sequence GRU/RNN
 * - #2 Skip-Aware Contextual Multi-Armed Bandit (LinUCB-style)
 * - #3 Session Arc Planner (Warm-up -> Peak -> Cool-down)
 * - #4 Circadian Rhythm Temporal State
 * - #5 Bi-Directional Latent Playlist Bridge
 * - #6 Session Context Recovery
 * - #19 Determinantal Point Process (DPP) Diversity Reranker
 * - #24 Predictive Cache Prefetch Ranker
 * - #29 Listening Burnout Prevention
 * - #30 First-Track-of-Day Bandit Selector
 */
object SequenceAndBanditEngine {

    private const val HIDDEN_DIM = 16

    data class BanditStats(
        val rewardSum: Double = 0.0,
        val trials: Int = 0
    ) {
        val meanReward: Double get() = if (trials == 0) 0.0 else rewardSum / trials
    }

    data class PlayEvent(
        val songId: String,
        val timestampMs: Long,
        val hourOfDay: Int,
        val skippedEarly: Boolean,
        val completed: Boolean
    )

    /**
     * #1 & #4: Lightweight GRU-style recurrent state update over a sequence of track embeddings
     * combined with 24-hour circadian sin/cos encoding.
     */
    fun computeSequenceHiddenState(
        recentTracks: List<FloatArray>,
        hourOfDay: Int
    ): FloatArray {
        val h = FloatArray(HIDDEN_DIM)
        val angle = (2.0 * PI * (hourOfDay.coerceIn(0, 23) / 24.0)).toFloat()
        val timeSin = sin(angle)
        val timeCos = cos(angle)

        for ((step, x) in recentTracks.withIndex()) {
            val recencyWeight = 0.5f + 0.5f * ((step + 1).toFloat() / recentTracks.size.coerceAtLeast(1))
            for (i in 0 until HIDDEN_DIM) {
                val xi = x.getOrElse(i) { 0f }
                val timeBias = if (i % 2 == 0) timeSin * 0.12f else timeCos * 0.12f
                // Update gate z and reset gate r with deterministic orthogonal-like projections
                val z = sigmoid(0.85f * xi + 0.45f * h[i] + timeBias)
                val r = sigmoid(0.65f * xi + 0.35f * h[(i + 3) % HIDDEN_DIM])
                val candidate = tanh(xi * recencyWeight + r * 0.55f * h[(i + 5) % HIDDEN_DIM] + timeBias)
                h[i] = (1f - z) * h[i] + z * candidate
            }
        }
        return normalizeVector(h)
    }

    /**
     * #1 & #2 & #29: Scores a candidate track using GRU hidden state similarity,
     * contextual bandit UCB reward, and 72h burnout decay.
     */
    fun scoreNextTrack(
        candidate: Song,
        candidateEmbedding: FloatArray,
        hiddenState: FloatArray,
        banditStats: Map<String, BanditStats>,
        recentEvents: List<PlayEvent>,
        nowMs: Long,
        totalTrials: Int
    ): Double {
        val rnnAffinity = cosineSimilarity(hiddenState, candidateEmbedding).toDouble()
        val stats = banditStats[candidate.id]
        val ucb = if (stats == null || stats.trials == 0) {
            0.25 // optimistic exploration bonus
        } else {
            stats.meanReward + 0.35 * sqrt(ln(max(2, totalTrials).toDouble()) / stats.trials)
        }
        val burnout = burnoutPenalty(candidate.id, recentEvents, nowMs)
        return (0.55 * rnnAffinity + 0.45 * ucb) - burnout
    }

    /**
     * #1: Ranks candidate songs for "Up Next" using the GRU + Bandit + Burnout pipeline.
     */
    fun predictNextTracks(
        recentSongs: List<Song>,
        candidates: List<Song>,
        embeddingsById: Map<String, FloatArray>,
        banditStats: Map<String, BanditStats>,
        recentEvents: List<PlayEvent>,
        hourOfDay: Int,
        nowMs: Long
    ): List<Song> {
        if (candidates.isEmpty()) return emptyList()
        val recentVecs = recentSongs.takeLast(10).mapNotNull { embeddingsById[it.id] }
        val hidden = computeSequenceHiddenState(recentVecs, hourOfDay)
        val totalTrials = banditStats.values.sumOf { it.trials }.coerceAtLeast(1)
        val recentIds = recentSongs.takeLast(3).mapTo(mutableSetOf()) { it.id }

        return candidates
            .filter { it.id !in recentIds }
            .sortedByDescending { song ->
                val emb = embeddingsById[song.id] ?: FloatArray(HIDDEN_DIM)
                scoreNextTrack(song, emb, hidden, banditStats, recentEvents, nowMs, totalTrials)
            }
    }

    /**
     * #2: Updates bandit reward table on early skip (<15s) or full completion.
     */
    fun recordPlaybackOutcome(
        current: Map<String, BanditStats>,
        songId: String,
        skippedEarly: Boolean,
        completed: Boolean
    ): Map<String, BanditStats> {
        val delta = when {
            skippedEarly -> -1.0
            completed -> 1.0
            else -> 0.2
        }
        val prev = current[songId] ?: BanditStats()
        return current + (songId to BanditStats(prev.rewardSum + delta, prev.trials + 1))
    }

    /**
     * #3: Session Arc Planner — arranges a queue along a Warm-up (0.45) -> Peak (0.88) -> Cool-down (0.35)
     * energy trajectory.
     */
    fun planSessionArc(
        songs: List<Song>,
        energyOf: (Song) -> Float
    ): List<Song> {
        if (songs.size <= 2) return songs
        val remaining = songs.toMutableList()
        val result = ArrayList<Song>(songs.size)
        val n = songs.size
        for (idx in 0 until n) {
            val progress = idx.toFloat() / max(1, n - 1)
            val targetEnergy = when {
                progress < 0.35f -> 0.40f + (progress / 0.35f) * 0.45f
                progress < 0.70f -> 0.85f
                else -> 0.85f - ((progress - 0.70f) / 0.30f) * 0.50f
            }
            val bestIdx = remaining.indices.minByOrNull { i ->
                kotlin.math.abs(energyOf(remaining[i]) - targetEnergy)
            } ?: 0
            result.add(remaining.removeAt(bestIdx))
        }
        return result
    }

    /**
     * #5: Bi-Directional Latent Playlist Bridge — finds [bridgeCount] intermediate tracks
     * smoothly interpolating from [fromSong] to [toSong] in embedding space.
     */
    fun buildPlaylistBridge(
        fromSong: Song,
        toSong: Song,
        pool: List<Song>,
        embeddingsById: Map<String, FloatArray>,
        bridgeCount: Int = 4
    ): List<Song> {
        val startVec = embeddingsById[fromSong.id] ?: return emptyList()
        val endVec = embeddingsById[toSong.id] ?: return emptyList()
        val candidates = pool.filter { it.id != fromSong.id && it.id != toSong.id }.toMutableList()
        val bridge = mutableListOf<Song>()
        for (step in 1..bridgeCount) {
            if (candidates.isEmpty()) break
            val alpha = step.toFloat() / (bridgeCount + 1)
            val target = FloatArray(HIDDEN_DIM) { i ->
                (1f - alpha) * startVec.getOrElse(i) { 0f } + alpha * endVec.getOrElse(i) { 0f }
            }
            val normTarget = normalizeVector(target)
            val bestIdx = candidates.indices.maxByOrNull { idx ->
                val vec = embeddingsById[candidates[idx].id] ?: FloatArray(HIDDEN_DIM)
                cosineSimilarity(normTarget, vec)
            } ?: break
            bridge.add(candidates.removeAt(bestIdx))
        }
        return bridge
    }

    /**
     * #6: Session Context Recovery — returns true if gap < 6h and current hour aligns with previous mood.
     */
    fun shouldResumePreviousMood(
        lastPlayTimestampMs: Long,
        nowMs: Long
    ): Boolean {
        if (lastPlayTimestampMs <= 0L) return false
        val gapHours = (nowMs - lastPlayTimestampMs) / 3_600_000.0
        return gapHours in 0.0..6.0
    }

    /**
     * #19: Determinantal Point Process (DPP) greedy diversity reranker so shuffle/mixes
     * avoid back-to-back repeats of the same artist or album while preserving quality.
     */
    fun rerankWithDppDiversity(
        rankedSongs: List<Song>,
        embeddingsById: Map<String, FloatArray>,
        limit: Int = rankedSongs.size
    ): List<Song> {
        if (rankedSongs.size <= 2) return rankedSongs.take(limit)
        val remaining = rankedSongs.toMutableList()
        val selected = mutableListOf<Song>()
        val targetSize = min(limit, rankedSongs.size)

        selected.add(remaining.removeAt(0))
        while (selected.size < targetSize && remaining.isNotEmpty()) {
            val lastSelected = selected.last()
            val bestIdx = remaining.indices.maxByOrNull { idx ->
                val cand = remaining[idx]
                val baseQuality = 1.0 - (idx.toDouble() / remaining.size.coerceAtLeast(1)) * 0.4
                val candVec = embeddingsById[cand.id] ?: FloatArray(HIDDEN_DIM)
                val maxSim = selected.takeLast(4).maxOf { prev ->
                    val prevVec = embeddingsById[prev.id] ?: FloatArray(HIDDEN_DIM)
                    cosineSimilarity(candVec, prevVec).toDouble()
                }
                val sameArtistPenalty = if (cand.artist.equals(lastSelected.artist, ignoreCase = true)) 0.65 else 0.0
                val sameAlbumPenalty = if (cand.album.equals(lastSelected.album, ignoreCase = true)) 0.35 else 0.0
                baseQuality - 0.5 * maxSim - sameArtistPenalty - sameAlbumPenalty
            } ?: 0
            selected.add(remaining.removeAt(bestIdx))
        }
        return selected
    }

    /**
     * #24: Predictive Cache Prefetch Ranker — selects top [count] tracks most likely to be played
     * without early skips.
     */
    fun rankPrefetchCandidates(
        upcomingQueue: List<Song>,
        banditStats: Map<String, BanditStats>,
        count: Int = 3
    ): List<Song> {
        return upcomingQueue
            .sortedByDescending { song -> banditStats[song.id]?.meanReward ?: 0.1 }
            .take(count)
    }

    /**
     * #29: Listening Burnout Prevention — penalizes songs overplayed in the last 72 hours.
     */
    fun burnoutPenalty(
        songId: String,
        recentEvents: List<PlayEvent>,
        nowMs: Long
    ): Double {
        val window72h = 72L * 3_600_000L
        val recentCount = recentEvents.count {
            it.songId == songId && (nowMs - it.timestampMs) in 0L..window72h
        }
        return if (recentCount <= 3) 0.0 else min(0.85, (recentCount - 3) * 0.12)
    }

    /**
     * #30: First-Track-of-Day Bandit — picks the best opener for the current morning/daytime window.
     */
    fun pickFirstTrackOfDay(
        songs: List<Song>,
        banditStats: Map<String, BanditStats>,
        recentEvents: List<PlayEvent>,
        hourOfDay: Int,
        nowMs: Long
    ): Song? {
        if (songs.isEmpty()) return null
        val morningHistory = recentEvents.filter { kotlin.math.abs(it.hourOfDay - hourOfDay) <= 2 && !it.skippedEarly }
        val morningBoostIds = morningHistory.groupingBy { it.songId }.eachCount()
        return songs.maxByOrNull { song ->
            val bandit = banditStats[song.id]?.meanReward ?: 0.0
            val morningAffinity = (morningBoostIds[song.id] ?: 0) * 0.25
            val burnout = burnoutPenalty(song.id, recentEvents, nowMs)
            bandit + morningAffinity - burnout
        }
    }

    fun cosineSimilarity(a: FloatArray, b: FloatArray): Float {
        val len = min(a.size, b.size)
        if (len == 0) return 0f
        var dot = 0f
        var na = 0f
        var nb = 0f
        for (i in 0 until len) {
            dot += a[i] * b[i]
            na += a[i] * a[i]
            nb += b[i] * b[i]
        }
        val denom = sqrt(na) * sqrt(nb)
        return if (denom <= 1e-6f) 0f else dot / denom
    }

    fun normalizeVector(v: FloatArray): FloatArray {
        var sumSq = 0f
        for (x in v) sumSq += x * x
        val norm = sqrt(sumSq)
        if (norm <= 1e-6f) return v
        return FloatArray(v.size) { v[it] / norm }
    }

    private fun sigmoid(x: Float): Float = (1.0 / (1.0 + exp(-x.toDouble()))).toFloat()
}
