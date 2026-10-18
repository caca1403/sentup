package com.sentup.android.data

import kotlin.random.Random

/** How one shelf of an Explore list came to be grouped. */
enum class ExploreShelfKind { WHOLE, CHANNEL, DURATION, FRESHNESS }

/**
 * One shelf of an Explore result: a channel's videos, a length band, an age
 * band, or the whole filtered list as one flat run.
 *
 * Only the fields that kind uses are filled. [band] and [age] are null when the
 * shelf is not of that kind; [age] being [ExploreFreshness.ANY] inside a
 * freshness grouping means the *rest* - older than a quarter - rather than "no
 * limit", because by then the window has already been applied as a filter.
 */
data class ExploreShelf(
    val id: String,
    val kind: ExploreShelfKind,
    val videos: List<VideoItem>,
    val channelId: String? = null,
    val channelName: String? = null,
    val avatarUrl: String? = null,
    val band: ExploreLength? = null,
    val age: ExploreFreshness? = null
) {
    /** The channel this shelf is grouped under, when it is a channel shelf. */
    val channelNavigationReference: String?
        get() = channelId?.takeIf { it.isNotBlank() }
            ?: channelName?.takeIf { it.isNotBlank() }
}

/**
 * Builds the shelves an Explore list shows, out of videos some other layer
 * already fetched.
 *
 * **Pure and deterministic for a given [Random]**, so every rule is tested on
 * the JVM rather than judged on a screen (`SubscriptionMix` is the precedent
 * for shaping this as an object rather than a repository). It never touches
 * the network, a preference or a store: filtering is a derived step over what
 * it is handed, exactly as `NotInterestedRepository.filter` is, so turning a
 * rule off brings every video straight back instead of waiting for a refetch.
 */
object ExploreEngine {

    /**
     * Filter, group, sort and cap [videos] under [rules].
     *
     * [watchedIds] is the profile's watch history as a set, [nowMs] the clock
     * the age bands are measured against, and [random] the source of the
     * shuffle. Both are arguments so a test can pin them.
     *
     * Empty input, or input where every video is filtered out, gives an empty
     * list rather than one empty shelf: an empty shelf has to be drawn, and a
     * list of nothing is what the interface already knows how to word.
     */
    fun build(
        videos: List<VideoItem>,
        rules: ExploreRules,
        nowMs: Long,
        watchedIds: Set<String>,
        random: Random = Random.Default
    ): List<ExploreShelf> {
        if (videos.isEmpty()) return emptyList()

        // First occurrence wins, so a video that arrived from two sources keeps
        // the copy that came first rather than being replaced by a later one.
        val kept = videos.distinctBy { it.videoId }
            .filter { rules.accepts(it, nowMs, watchedIds) }
        if (kept.isEmpty()) return emptyList()

        val limit = rules.perShelfLimit.coerceIn(
            ExploreRules.MIN_PER_SHELF_LIMIT,
            ExploreRules.MAX_PER_SHELF_LIMIT
        )

        val shelves = when (rules.grouping) {
            ExploreGrouping.NONE -> listOf(
                shelf(
                    id = "whole",
                    kind = ExploreShelfKind.WHOLE,
                    videos = sort(kept, rules.sort, random).take(limit)
                )
            )

            ExploreGrouping.CHANNEL -> groupByChannel(kept, rules.sort, limit, random)

            ExploreGrouping.DURATION -> ExploreLength.entries
                .filter { it != ExploreLength.ANY }
                .map { band ->
                    shelf(
                        id = "len_${band.id}",
                        kind = ExploreShelfKind.DURATION,
                        videos = sort(kept.filter { it.matchesBand(band) }, rules.sort, random).take(limit),
                        band = band
                    )
                } + shelf(
                    id = "len_other",
                    kind = ExploreShelfKind.DURATION,
                    // Live streams and videos whose runtime the source never
                    // gave are in no band, and a filter that asked for a length
                    // has already dropped them - so this only appears when the
                    // length filter is [ExploreLength.ANY], and then it is a
                    // real shelf rather than a hole.
                    videos = sort(kept.filter { !it.matchesAnyBand() }, rules.sort, random).take(limit),
                    band = null
                )

            ExploreGrouping.FRESHNESS -> ExploreFreshness.entries
                .filter { it != ExploreFreshness.ANY }
                .map { band ->
                    shelf(
                        id = "age_${band.id}",
                        kind = ExploreShelfKind.FRESHNESS,
                        videos = sort(kept.filter { ageBandOf(it, nowMs) == band }, rules.sort, random).take(limit),
                        age = band
                    )
                } + shelf(
                    id = "age_other",
                    kind = ExploreShelfKind.FRESHNESS,
                    videos = sort(kept.filter { ageBandOf(it, nowMs) == ExploreFreshness.ANY }, rules.sort, random).take(limit),
                    age = ExploreFreshness.ANY
                )
        }
        return shelves.filter { it.videos.isNotEmpty() }.take(MAX_SHELVES)
    }

    /** How many grouped shelves one list may draw. */
    const val MAX_SHELVES = 40

    /**
     * One shelf per channel, each capped, ordered by the channel's newest
     * surviving video so the creators with something to watch now are at the
     * top rather than whoever sorts first by name.
     *
     * A video whose channel is unknown - a lockup that named none - is filed
     * under a blank channel rather than dropped, so the total on screen adds
     * up to what the filters kept.
     */
    private fun groupByChannel(
        videos: List<VideoItem>,
        sort: ExploreSort,
        limit: Int,
        random: Random
    ): List<ExploreShelf> {
        val grouped = videos.groupBy { it.channelId?.takeIf { id -> id.isNotBlank() } ?: it.channelName }
        val shelves = grouped.map { (key, group) ->
            shelf(
                id = "ch_$key",
                kind = ExploreShelfKind.CHANNEL,
                videos = sort(group, sort, random).take(limit),
                // The grouping key is the id whenever the video carries one,
                // which is nearly always - so the display name is read off the
                // group itself, and the key is only the fallback for a shelf
                // that has no name to show.
                channelId = key.takeIf { it.startsWith("UC") },
                channelName = group.firstOrNull { it.channelName.isNotBlank() }?.channelName ?: key,
                avatarUrl = group.firstOrNull { !it.channelIconUrl.isNullOrBlank() }?.channelIconUrl
            )
        }
        return shelves.sortedWith(
            compareByDescending<ExploreShelf> { newestTimestamp(it) }
                .thenBy { it.channelName?.lowercase() ?: "" }
        )
    }

    private fun newestTimestamp(shelf: ExploreShelf): Long =
        shelf.videos.maxOfOrNull { it.publishedAtMs ?: 0L } ?: 0L

    /** Dropped when the list came back empty, so no caller has to check. */
    private fun shelf(
        id: String,
        kind: ExploreShelfKind,
        videos: List<VideoItem>,
        channelId: String? = null,
        channelName: String? = null,
        avatarUrl: String? = null,
        band: ExploreLength? = null,
        age: ExploreFreshness? = null
    ): ExploreShelf = ExploreShelf(
        id = id,
        kind = kind,
        videos = videos,
        channelId = channelId,
        channelName = channelName,
        avatarUrl = avatarUrl,
        band = band,
        age = age
    )

    /**
     * Order inside a shelf.
     *
     * Every comparator is total - a video that cannot be dated sorts last on
     * a date order rather than comparing 0 against a real timestamp - so the
     * order never depends on which item happened to arrive first. `sortedBy`
     * is stable, so equal keys keep the source order.
     */
    fun sort(videos: List<VideoItem>, sort: ExploreSort, random: Random): List<VideoItem> =
        when (sort) {
            ExploreSort.NEWEST -> videos.sortedWith(
                compareByDescending<VideoItem> { it.publishedAtMs ?: 0L }.thenByDescending { it.videoId }
            )

            ExploreSort.OLDEST -> videos.sortedWith(
                compareBy<VideoItem> { it.publishedAtMs ?: Long.MAX_VALUE }.thenBy { it.videoId }
            )

            ExploreSort.MOST_VIEWED -> videos.sortedWith(
                compareByDescending<VideoItem> { parseViewCount(it.viewCount) ?: -1L }
                    .thenByDescending { it.publishedAtMs ?: 0L }
            )

            ExploreSort.QUICKEST -> videos.sortedWith(
                compareBy<VideoItem> { if (it.isLive) Long.MAX_VALUE else it.duration }
                    .thenByDescending { it.publishedAtMs ?: 0L }
            )

            ExploreSort.LONGEST -> videos.sortedWith(
                compareByDescending<VideoItem> { if (it.isLive) 0L else it.duration }
                    .thenByDescending { it.publishedAtMs ?: 0L }
            )

            ExploreSort.SHUFFLED -> videos.shuffled(random)
        }

    /**
     * A view count as a number, out of the text YouTube and Sentüp both write.
     *
     * `VideoItem.viewCount` is prose ("1.2M views", "345,678 views"), and it is
     * formatted with the device's locale - so the same value arrives as "1.2M"
     * on one phone and "1,2M" on another. Both separators are therefore read,
     * and which one is the decimal point is decided by the group after it: a
     * trailing group of exactly three digits is a thousands separator
     * ("1.234" is one thousand two hundred), anything else is a decimal
     * ("1,2" is one point two). Null when there is no number at all, which the
     * view-count order treats as the smallest rather than as zero-because-known.
     *
     * Only one decimal digit is read, which is all either source ever writes
     * (`VideoItem.formatViewCount` produces exactly one, and so does YouTube).
     */
    fun parseViewCount(text: String?): Long? {
        if (text.isNullOrBlank()) return null
        val match = VIEW_COUNT.find(text) ?: return null
        val parts = splitNumber(match.groupValues[1]) ?: return null
        val scale = when (match.groupValues[2].uppercase()) {
            "K" -> 1_000L
            "M" -> 1_000_000L
            "B" -> 1_000_000_000L
            else -> 1L
        }
        // The suffix is folded in here rather than applied to a rounded value:
        // 1.2M is 1_200_000, not the 12_000_000 that rounding 1.2 to 12 first
        // would produce.
        return parts.whole * scale + parts.tenths * scale / 10L
    }

    /**
     * One number as whole digits and a single leading decimal digit.
     *
     * Null when there are no digits at all, which is what "No views" and an
     * empty string both are: a count nobody published is not zero.
     */
    private fun splitNumber(raw: String): NumberParts? {
        val cleaned = raw.filter { it.isDigit() || it == '.' || it == ',' }
        if (cleaned.isBlank()) return null
        val decimalAt = maxOf(cleaned.lastIndexOf('.'), cleaned.lastIndexOf(','))
        if (decimalAt < 0) {
            return cleaned.toLongOrNull()?.let { NumberParts(it, 0L) }
        }
        val trailing = cleaned.length - decimalAt - 1
        if (trailing == 3) {
            // Three digits after the last separator: grouping rather than a
            // decimal, so every separator goes and the digits read as one run.
            val grouped = cleaned.filter { it.isDigit() }.toLongOrNull() ?: return null
            return NumberParts(grouped, 0L)
        }
        val whole = cleaned.substring(0, decimalAt).filter { it.isDigit() }.toLongOrNull() ?: return null
        val tenths = cleaned.substring(decimalAt + 1).filter { it.isDigit() }.take(1)
            .toLongOrNull() ?: 0L
        return NumberParts(whole, tenths)
    }

    /** A number as [whole] units and one [tenths] digit. */
    private data class NumberParts(val whole: Long, val tenths: Long)

    private val VIEW_COUNT = Regex("""([\d][\d.,\s]*)\s*([KMB])?""", RegexOption.IGNORE_CASE)

    private fun VideoItem.matchesBand(band: ExploreLength): Boolean =
        band.matches(duration, isLive)

    private fun VideoItem.matchesAnyBand(): Boolean =
        ExploreLength.entries.any { it != ExploreLength.ANY && it.matches(duration, isLive) }

    private fun ageBandOf(video: VideoItem, nowMs: Long): ExploreFreshness =
        ExploreFreshness.bandOf(video.publishedAtMs ?: VideoItem.parseRelativeTime(video.uploadedDate, nowMs), nowMs)
}