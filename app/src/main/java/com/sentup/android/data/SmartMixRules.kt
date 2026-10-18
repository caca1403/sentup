package com.sentup.android.data

import java.util.Random

/**
 * The smart mixes: small, deterministic list builders that YouTube Music does
 * not offer.
 *
 * Every rule here is a pure function of the pools and history it is handed, so
 * it can be run by a test with no Android and no clock beyond a parameter, and
 * it can never quietly depend on something the caller cannot see. The screen
 * that shows them builds the pools; this file only decides order and size.
 */
object SmartMixRules {

    /** A built list: what it is called, why it exists, and what is in it. */
    data class Built(
        val id: String,
        val title: String,
        val reason: String,
        val tracks: List<Song>,
    ) {
        val isEmpty: Boolean get() = tracks.isEmpty()
    }

    /**
     * A YouTube Music shelf entry carries no duration, so a timed mix would
     * fill to nothing. Three and a half minutes is the catalogue's own average.
     */
    const val ASSUMED_TRACK_MS = 3L * 60_000 + 30_000

    private fun ms(song: Song): Long = if (song.duration > 0) song.duration else ASSUMED_TRACK_MS

    /** Round-robin over lists, de-duplicated by song id. The shared spine. */
    private fun interleave(lists: List<List<Song>>): List<Song> {
        val out = mutableListOf<Song>()
        val seen = mutableSetOf<String>()
        var i = 0
        while (lists.any { i < it.size }) {
            for (list in lists) {
                val song = list.getOrNull(i) ?: continue
                if (seen.add(song.id)) out += song
            }
            i++
        }
        return out
    }

    /** One list, de-duplicated, length-capped, wrapped in a [Built]. */
    private fun built(id: String, title: String, reason: String, tracks: List<Song>, size: Int): Built =
        Built(id, title, reason, tracks.distinctBy { it.id }.take(size))

    /**
     * Exactly-long-enough mix: takes tracks until the target is met. Unknown
     * durations count as [ASSUMED_TRACK_MS] so a shelf of YT entries still
     * fills to a real number of minutes.
     */
    fun durationMix(pool: List<Song>, targetMs: Long, title: String): Built {
        val out = mutableListOf<Song>()
        var total = 0L
        for (song in pool.distinctBy { it.id }) {
            if (total >= targetMs) break
            out += song
            total += ms(song)
        }
        return Built("duration_$targetMs", title, "yaklaşık ${total / 60_000} dakika", out)
    }

    /** Round-robin across every shelf, so one shelf cannot dominate the mix. */
    fun shelfBlend(pools: List<List<Song>>, size: Int = 40): Built {
        val lists = pools.map { it.filter { s -> s.id.isNotBlank() } }.filter { it.isNotEmpty() }
        return built("shelf_blend", "Raf harmanı", "${lists.size} raf tek karışımda", interleave(lists), size)
    }

    /**
     * The same ten all day, a different ten tomorrow: the pool is rotated by
     * the day number, so a refresh never reshuffles what is already playing.
     */
    fun dailyTen(pools: List<List<Song>>, dayIndex: Long, size: Int = 10): Built {
        val merged = interleave(pools.map { it.distinctBy { s -> s.id } })
        if (merged.isEmpty()) return Built("daily", "Günün onlusu", "", emptyList())
        val offset = (dayIndex % merged.size).let { if (it < 0) it + merged.size else it }.toInt()
        val rotated = merged.drop(offset) + merged.take(offset)
        return built("daily", "Günün onlusu", "her gün yenilenir", rotated, size)
    }

    /** Familiar and new alternating: a bridge, not another feed. */
    fun discoveryBridge(familiar: List<Song>, fresh: List<Song>, size: Int = 30): Built {
        val f = familiar.distinctBy { it.id }
        val n = fresh.distinctBy { it.id }
        val out = mutableListOf<Song>()
        val seen = mutableSetOf<String>()
        var i = 0
        while (out.size < size && (i < f.size || i < n.size)) {
            f.getOrNull(i)?.let { if (seen.add(it.id)) out += it }
            n.getOrNull(i)?.let { if (out.size < size && seen.add(it.id)) out += it }
            i++
        }
        return Built("bridge", "Keşif köprüsü", "bildiğinle yeni, dönüşümlü", out)
    }

    /** In the charts, never played here. The gap the charts leave. */
    fun neverPlayed(candidates: List<Song>, playedIds: Set<String>, size: Int = 30): Built {
        val out = candidates.distinctBy { it.id }.filterNot { it.id in playedIds }
        return built("never_played", "Hiç dinlemediklerin", "listelerde olup çalmadığın", out, size)
    }

    /**
     * What was playing around this time a year (and two years) ago: the
     * anniversary window over the play history, not a random old song.
     */
    fun timeCapsule(history: List<PlayHistoryEntry>, now: Long, size: Int = 25): Built {
        val day = 24L * 60 * 60 * 1000
        val picks = history
            .filter { entry ->
                val days = (now - entry.timestamp) / day
                (days in 358..372) || (days in 723..737)
            }
            .sortedBy { it.timestamp }
            .distinctBy { it.songId }
            .take(size)
            .map { Song.fromYouTube(it.songId, it.title, it.artist, it.album, it.duration, it.thumbnailUrl) }
        return Built("capsule", "Zaman kapsülü", "geçen yıl bugünlerde çalanlar", picks)
    }

    /** The long tail: the artists you have played the least, heard again. */
    fun longTail(history: List<PlayHistoryEntry>, size: Int = 25): Built {
        val picks = history
            .filter { it.artist.isNotBlank() }
            .groupBy { it.artist }
            .entries.sortedBy { it.value.size }
            .take(12)
            .flatMap { it.value }
            .distinctBy { it.songId }
            .take(size)
            .map { Song.fromYouTube(it.songId, it.title, it.artist, it.album, it.duration, it.thumbnailUrl) }
        return Built("long_tail", "Uzun kuyruk", "en az çaldığın sanatçılar", picks)
    }

    /** Several moods, read one track at a time, so no single mood wins. */
    fun moodBlend(pages: List<List<Song>>, size: Int = 50): Built {
        val lists = pages.map { it.distinctBy { s -> s.id } }.filter { it.isNotEmpty() }
        return built("mood_blend", "Ruh hâli harmanı", "${lists.size} ruh hâli karışık", interleave(lists), size)
    }

    /**
     * A shuffle that never puts the same artist twice in a row - the thing a
     * plain shuffle gets wrong the moment an album is in the queue.
     */
    fun artistSpaced(songs: List<Song>, seed: Long): List<Song> {
        val rnd = Random(seed)
        val byArtist = songs.distinctBy { it.id }.groupBy { it.artist.lowercase().ifBlank { "?" } }
            .mapValues { it.value.toMutableList() }
            .toMutableMap()
        val out = mutableListOf<Song>()
        var last: String? = null
        while (true) {
            val available = byArtist.filterValues { it.isNotEmpty() }.keys
            if (available.isEmpty()) break
            val preferred = available.filter { it != last }.ifEmpty { available.toList() }
            val key = preferred[rnd.nextInt(preferred.size)]
            val list = byArtist.getValue(key)
            out += list.removeAt(rnd.nextInt(list.size))
            last = key
        }
        return out
    }
}
