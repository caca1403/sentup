package com.sentup.android.ui.library

import com.sentup.android.data.PlayHistoryEntry

/**
 * Stats figures read straight off the listening history, for one scope.
 *
 * The screen shows a week-vs-all-time switch, and every figure in this summary
 * is computed for the chosen window: the totals, the top three lists, and the
 * week-over-week comparison. Nothing here is stored; the history is the only
 * input, so the numbers cannot disagree with the page that produced them.
 *
 * Duration is in **milliseconds** in [PlayHistoryEntry], matching
 * `StatsRepository.getGlobalStats` (`duration / 1000` for seconds). The screen
 * divides again, in one place, to stop the unit from drifting a second time.
 */
enum class StatsScope {
    Last7Days,
    AllTime
}

internal data class StatsRankedEntry(
    val title: String,
    val subtitle: String,
    val plays: Int,
    val seconds: Long,
    val thumbnailUrl: String? = null
)

internal data class StatsSummary(
    val plays: Int,
    val totalSeconds: Long,
    val uniqueSongs: Int,
    val uniqueArtists: Int,
    val topSongs: List<StatsRankedEntry>,
    val topArtists: List<StatsRankedEntry>,
    val topAlbums: List<StatsRankedEntry>,
    val lastWeekPlays: Int,
    val previousWeekPlays: Int
)

internal const val STATS_TOP_LIMIT = 5

private const val WEEK_MILLIS = 7L * 24 * 60 * 60 * 1000
private const val DAY_MILLIS = 24L * 60 * 60 * 1000

/**
 * Empty history must produce empty summaries rather than zeros dressed as
 * numbers: the screen reads the empty branch off `plays == 0`, and a zero-filled
 * summary would put a card full of "0 şarkı" in front of a new user.
 */
internal fun summarizeHistory(
    history: List<PlayHistoryEntry>,
    nowMillis: Long = System.currentTimeMillis(),
    scope: StatsScope = StatsScope.Last7Days
): StatsSummary {
    if (history.isEmpty()) {
        return StatsSummary(
            plays = 0,
            totalSeconds = 0,
            uniqueSongs = 0,
            uniqueArtists = 0,
            topSongs = emptyList(),
            topArtists = emptyList(),
            topAlbums = emptyList(),
            lastWeekPlays = 0,
            previousWeekPlays = 0
        )
    }

    val windowStart = when (scope) {
        StatsScope.AllTime -> Long.MIN_VALUE
        StatsScope.Last7Days -> nowMillis - WEEK_MILLIS
    }

    // Last week and the week before it are counted against the whole history,
    // whatever the scope: the comparison row is the "what changed" answer and
    // stays meaningful when the totals above it are scoped to seven days.
    val lastWeekPlays = history.count { it.timestamp >= nowMillis - WEEK_MILLIS }
    val previousWeekPlays = history.count { it.timestamp >= nowMillis - 2 * WEEK_MILLIS } - lastWeekPlays

    val scoped = history.filter { it.timestamp >= windowStart }

    fun List<PlayHistoryEntry>.seconds(): Long = sumOf { it.duration / 1000 }

    val topSongs = scoped
        .groupBy { it.songId }
        .map { (id, entries) ->
            val first = entries.first()
            StatsRankedEntry(
                title = first.title,
                subtitle = first.artist,
                plays = entries.size,
                seconds = entries.seconds(),
                thumbnailUrl = first.thumbnailUrl
            )
        }
        .sortedWith(
            compareByDescending<StatsRankedEntry> { it.plays }.thenByDescending { it.seconds }
        )
        .take(STATS_TOP_LIMIT)

    val topArtists = scoped
        .groupBy { it.artist }
        .filterNot { it.key.isBlank() }
        .map { (name, entries) ->
            StatsRankedEntry(
                title = name,
                subtitle = "",
                plays = entries.size,
                seconds = entries.seconds()
            )
        }
        .sortedWith(
            compareByDescending<StatsRankedEntry> { it.plays }.thenByDescending { it.seconds }
        )
        .take(STATS_TOP_LIMIT)

    val topAlbums = scoped
        .groupBy { it.album }
        .filterNot { it.key.isBlank() }
        .map { (name, entries) ->
            StatsRankedEntry(
                title = name,
                subtitle = entries.firstOrNull { it.artist.isNotBlank() }?.artist.orEmpty(),
                plays = entries.size,
                seconds = entries.seconds(),
                thumbnailUrl = entries.firstNotNullOfOrNull { it.thumbnailUrl }
            )
        }
        .sortedWith(
            compareByDescending<StatsRankedEntry> { it.plays }.thenByDescending { it.seconds }
        )
        .take(STATS_TOP_LIMIT)

    return StatsSummary(
        plays = scoped.size,
        totalSeconds = scoped.seconds(),
        uniqueSongs = scoped.map { it.songId }.distinct().size,
        uniqueArtists = scoped.map { it.artist }.filter { it.isNotBlank() }.distinct().size,
        topSongs = topSongs,
        topArtists = topArtists,
        topAlbums = topAlbums,
        lastWeekPlays = lastWeekPlays,
        previousWeekPlays = previousWeekPlays
    )
}

/** Day boundaries follow the device clock: the week starts midnight local. */
internal fun statsDayKey(timestamp: Long): String {
    val millis = timestamp % DAY_MILLIS
    val hour = millis / (60 * 60 * 1000)
    val minute = (millis / (60 * 1000)) % 60
    return String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
}
