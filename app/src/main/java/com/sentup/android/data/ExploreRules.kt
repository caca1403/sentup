package com.sentup.android.data

import org.json.JSONArray
import org.json.JSONObject

/**
 * Where an Explore list draws its videos from.
 *
 * Each entry is something the device already holds, or one thing it can name
 * with a single request - never a search. [judgement] A source that cost a
 * fan-out would make the whole surface slow the first time it is opened, and a
 * discovery page that has to ask YouTube a question before it can draw a shelf
 * is the thing this feature exists to be unlike.
 *
 * The [id]s are stored and frozen.
 */
enum class ExploreSource(val id: String) {
    /**
     * The subscriptions feed: one account browse when signed in, the merged
     * per-channel RSS feed otherwise.
     */
    SUBSCRIPTIONS("subscriptions"),

    /** Sentüp's watch history for the active profile. Local, so it costs nothing. */
    HISTORY("history"),

    /** Playlists assembled on this device. Local, so they cost nothing. */
    DEVICE_PLAYLISTS("device_playlists"),

    /** The account's liked videos ("LL"). One browse, and only when signed in. */
    LIKED("liked");

    companion object {
        fun fromId(id: String?): ExploreSource? = entries.firstOrNull { it.id == id }
    }
}

/**
 * How the surviving videos are arranged once they have been filtered.
 *
 * [CHANNEL] is the one that has no equivalent anywhere on YouTube: a
 * recommendation feed is one flat column, so a prolific channel appears as an
 * unbroken run of near-identical rows, and the only way to watch a creator's
 * output as a unit is to open their page. Grouping turns each creator into one
 * shelf of their own.
 *
 * The [id]s are stored and frozen.
 */
enum class ExploreGrouping(val id: String) {
    /** One list, in the order the rules sort it. */
    NONE("none"),

    /** One shelf per channel, in that channel's own name order. */
    CHANNEL("channel"),

    /** One shelf per length band, from shortest to longest. */
    DURATION("duration"),

    /** One shelf per age band, from newest to oldest. */
    FRESHNESS("freshness");

    companion object {
        fun fromId(id: String?): ExploreGrouping = entries.firstOrNull { it.id == id } ?: NONE
    }
}

/**
 * The order a flat list, or each shelf inside a grouped one, is read in.
 *
 * [SHUFFLED] is deliberately here rather than behind a button: the app already
 * draws a channel-interleaving shuffle for Home when recommendations are off
 * (`SubscriptionMix`), and a discovery list that never repeats in the same
 * order is the point of a discovery surface.
 *
 * The [id]s are stored and frozen.
 */
enum class ExploreSort(val id: String) {
    NEWEST("newest"),
    OLDEST("oldest"),
    MOST_VIEWED("views"),
    QUICKEST("quickest"),
    LONGEST("longest"),
    SHUFFLED("shuffled");

    companion object {
        fun fromId(id: String?): ExploreSort = entries.firstOrNull { it.id == id } ?: NEWEST
    }
}

/**
 * A duration band, the "type" a video is filed under when a shelf is asked to
 * group by length.
 *
 * The cut points are YouTube's own size classes rather than round numbers: it
 * calls anything under four minutes a Short, and the audio/video distinction
 * most people make is at about twenty. [matches] is the single definition of
 * membership, so the shelves and the filter can never disagree.
 */
enum class ExploreLength(val id: String, val minSeconds: Long, val maxSeconds: Long) {
    ANY("any", 0L, Long.MAX_VALUE),
    SHORT("short", 0L, 4L * 60L),
    MEDIUM("medium", 4L * 60L, 20L * 60L),
    LONG("long", 20L * 60L, Long.MAX_VALUE);

    /**
     * Whether a video of [durationSeconds] belongs to this band.
     *
     * A live stream and a video whose length the source never gave are *not*
     * measured against the cut points: both fall outside every band but [ANY],
     * so asking for "short videos" leaves them out rather than filing a stream
     * with no runtime under four minutes. Dropping them is the honest reading -
     * the user asked for a length, and neither of these has one.
     */
    fun matches(durationSeconds: Long, isLive: Boolean): Boolean {
        if (this == ANY) return true
        if (isLive || durationSeconds <= 0L) return false
        return durationSeconds >= minSeconds && durationSeconds < maxSeconds
    }

    companion object {
        fun fromId(id: String?): ExploreLength = entries.firstOrNull { it.id == id } ?: ANY
    }
}

/**
 * How recent a video has to be to appear.
 *
 * The bands are windows rather than points, and [ANY] is 0 days rather than a
 * null so a shelf label can be derived from the same number the filter uses.
 */
enum class ExploreFreshness(val id: String, val maxAgeDays: Int) {
    ANY("any", 0),
    TODAY("today", 1),
    WEEK("week", 7),
    MONTH("month", 31),
    QUARTER("quarter", 92);

    /** True when this window places no limit on age at all. */
    val isUnbounded: Boolean get() = maxAgeDays <= 0

    /**
     * Whether a video published at [publishedAtMs] falls inside this window.
     *
     * A video with no usable timestamp cannot satisfy a bounded window and is
     * left out: "today" that quietly includes a four-year-old upload is worse
     * than a shelf that is honestly short. [VideoItem.parseRelativeTime] turns
     * InnerTube's prose into a timestamp so a feed card is judged on the same
     * footing as an RSS one.
     */
    fun matches(publishedAtMs: Long?, nowMs: Long): Boolean {
        if (isUnbounded) return true
        if (publishedAtMs == null || publishedAtMs <= 0L) return false
        val ageMs = nowMs - publishedAtMs
        if (ageMs < 0L) return true
        return ageMs <= maxAgeDays.toLong() * 86_400_000L
    }

    companion object {
        fun fromId(id: String?): ExploreFreshness = entries.firstOrNull { it.id == id } ?: ANY

        /**
         * The band [publishedAtMs] belongs to, for grouping by age.
         *
         * The first matching band wins, and the list runs newest to oldest, so
         * an unstated timestamp lands in [ANY] at the end instead of being
         * dropped - grouping shows everything that survived filtering, it does
         * not filter.
         *
         * In the companion because it is asked of the enum rather than of one
         * of its values: a grouping has no band in hand to call it on.
         */
        fun bandOf(publishedAtMs: Long?, nowMs: Long): ExploreFreshness {
            for (band in entries) {
                if (band == ANY) continue
                if (band.matches(publishedAtMs, nowMs)) return band
            }
            return ANY
        }
    }
}

/**
 * One saved Explore list's behaviour: where it reads from, how it filters, and
 * how the result is arranged.
 *
 * This is a *rule*, not a copy. [ExploreListsRepository] stores these rather
 * than videos, which is the whole difference between this and a playlist: a
 * list stays correct as new uploads arrive, and re-opening it costs nothing
 * beyond the one fetch its sources already need.
 *
 * Every field has a default that describes the plain subscriptions feed, so a
 * list that was stored by an older build - or hand-edited - still decodes.
 */
data class ExploreRules(
    val sources: Set<ExploreSource> = setOf(ExploreSource.SUBSCRIPTIONS),
    val grouping: ExploreGrouping = ExploreGrouping.CHANNEL,
    val sort: ExploreSort = ExploreSort.NEWEST,
    val length: ExploreLength = ExploreLength.ANY,
    val freshness: ExploreFreshness = ExploreFreshness.ANY,
    /**
     * Restrict to these channels, by UC id.
     *
     * Empty means every channel the sources return. An empty list is not the
     * same as "none": nothing was chosen, as opposed to a choice that selected
     * no channels, which is not a rule anybody would keep.
     */
    val channelIds: List<String> = emptyList(),
    /** Kept when the title contains one of these. Case-insensitive substring. */
    val includeWords: List<String> = emptyList(),
    /** Dropped when the title or the channel name contains one of these. */
    val excludeWords: List<String> = emptyList(),
    /** Leave out anything already in this profile's watch history. */
    val hideWatched: Boolean = false,
    /** Only broadcasts, which is the one "type" YouTube never files for you. */
    val liveOnly: Boolean = false,
    /** How many videos one shelf holds. A grouped list is many shelves, so this
     *  is per shelf rather than per list. */
    val perShelfLimit: Int = DEFAULT_PER_SHELF_LIMIT
) {
    /**
     * Whether [video] survives every filter in these rules.
     *
     * [watchedIds] is the profile's watch history as a set, passed in rather
     * than read: this has to stay pure so the rules can be tested on the JVM,
     * and so one call can filter against a snapshot that cannot change
     * halfway through a shelf.
     */
    fun accepts(video: VideoItem, nowMs: Long, watchedIds: Set<String>): Boolean {
        if (video.videoId.isBlank()) return false
        if (liveOnly && !video.isLive) return false
        if (hideWatched && video.videoId in watchedIds) return false
        if (!length.matches(video.duration, video.isLive)) return false
        if (!freshness.matches(video.publishedAtMs ?: VideoItem.parseRelativeTime(video.uploadedDate, nowMs), nowMs)) {
            return false
        }
        if (channelIds.isNotEmpty() && !matchesChannel(video)) return false
        if (includeWords.isNotEmpty() && includeWords.none { video.title.contains(it, ignoreCase = true) }) {
            return false
        }
        if (excludeWords.isNotEmpty()) {
            val blocked = excludeWords.any { word ->
                video.title.contains(word, ignoreCase = true) ||
                    video.channelName.contains(word, ignoreCase = true)
            }
            if (blocked) return false
        }
        return true
    }

    /**
     * Channel membership, by id, with a name fallback.
     *
     * Feed lockups sometimes omit the channel id while still naming the
     * channel, and a rule that silently stopped matching those would look
     * broken rather than lenient. A stored entry that is not a UC id is
     * therefore compared against the display name as well, which is also what
     * makes a list hand-edited with a channel's name do something sensible.
     */
    private fun matchesChannel(video: VideoItem): Boolean {
        val channelId = video.channelId?.takeIf { it.isNotBlank() }
        return channelIds.any { wanted ->
            if (channelId != null) wanted == channelId
            else video.channelName.equals(wanted, ignoreCase = true)
        }
    }

    /** The same rules with [limit] videos per shelf, clamped to something sane. */
    fun withShelfLimit(limit: Int): ExploreRules =
        copy(perShelfLimit = limit.coerceIn(MIN_PER_SHELF_LIMIT, MAX_PER_SHELF_LIMIT))

    // ---------------- Persistence ----------------

    /** These rules as JSON. Every field is written, so a later default change
     *  cannot retroactively alter a list somebody already saved. */
    fun toJson(): JSONObject = JSONObject().apply {
        put("sources", JSONArray().apply { sources.forEach { put(it.id) } })
        put("grouping", grouping.id)
        put("sort", sort.id)
        put("length", length.id)
        put("freshness", freshness.id)
        put("channelIds", JSONArray().apply { channelIds.forEach { put(it) } })
        put("includeWords", JSONArray().apply { includeWords.forEach { put(it) } })
        put("excludeWords", JSONArray().apply { excludeWords.forEach { put(it) } })
        put("hideWatched", hideWatched)
        put("liveOnly", liveOnly)
        put("perShelfLimit", perShelfLimit)
    }

    companion object {
        const val DEFAULT_PER_SHELF_LIMIT = 12
        const val MIN_PER_SHELF_LIMIT = 3
        const val MAX_PER_SHELF_LIMIT = 50

        /**
         * Rules out of stored JSON, falling back to the defaults field by field.
         *
         * Nothing here is allowed to throw on a value it does not recognise: a
         * list that cannot be read is worse than a list that lost one filter,
         * so an unknown id takes the default rather than dropping the list.
         */
        fun fromJson(obj: JSONObject?): ExploreRules {
            if (obj == null) return ExploreRules()
            val sources = obj.optJSONArray("sources").toStringList()
                .mapNotNull(ExploreSource::fromId)
                .toSet()
            val channels = obj.optJSONArray("channelIds").toStringList()
                .filter { it.isNotBlank() }
            val include = obj.optJSONArray("includeWords").toStringList()
                .filter { it.isNotBlank() }
            val exclude = obj.optJSONArray("excludeWords").toStringList()
                .filter { it.isNotBlank() }
            return ExploreRules(
                // An empty source set would fetch nothing and show nothing, so
                // it decodes as the default rather than as an empty screen.
                sources = sources.ifEmpty { ExploreRules().sources },
                grouping = ExploreGrouping.fromId(obj.optString("grouping")),
                sort = ExploreSort.fromId(obj.optString("sort")),
                length = ExploreLength.fromId(obj.optString("length")),
                freshness = ExploreFreshness.fromId(obj.optString("freshness")),
                channelIds = channels,
                includeWords = include,
                excludeWords = exclude,
                hideWatched = obj.optBoolean("hideWatched", false),
                liveOnly = obj.optBoolean("liveOnly", false),
                perShelfLimit = obj.optInt("perShelfLimit", DEFAULT_PER_SHELF_LIMIT)
                    .coerceIn(MIN_PER_SHELF_LIMIT, MAX_PER_SHELF_LIMIT)
            )
        }
    }
}

/** A JSON string array as a list of strings, or empty when it is absent. */
internal fun JSONArray?.toStringList(): List<String> {
    val array = this ?: return emptyList()
    return (0 until array.length()).mapNotNull { index ->
        array.optString(index).takeIf { it.isNotBlank() }
    }
}

/**
 * One saved Explore list: a name and the rules behind it.
 *
 * Deliberately not a [LocalVideoPlaylist]. That store holds videos, so its
 * contents are frozen the day they are saved; this one holds a question, and
 * answers it again every time it is opened.
 */
data class ExploreList(
    val id: String,
    val name: String,
    val rules: ExploreRules = ExploreRules(),
    val createdAt: Long = System.currentTimeMillis(),
    /** Pinned lists are listed first and cannot be deleted by accident. */
    val pinned: Boolean = false
) {
    /**
     * What a card under this list would say, in words the data layer already
     * owns: how many sources it draws from and how it is arranged.
     *
     * Deliberately unlocalised free text would be wrong here, so this is only
     * the machine-readable shape - [ExploreListSummary] - and the interface
     * turns it into a sentence in the user's own language.
     */
    fun summary(): ExploreListSummary = ExploreListSummary(
        sourceCount = rules.sources.size,
        grouping = rules.grouping,
        hideWatched = rules.hideWatched,
        liveOnly = rules.liveOnly,
        length = rules.length,
        freshness = rules.freshness
    )
}

/** The parts of a list's rules worth naming on a card, without any wording. */
data class ExploreListSummary(
    val sourceCount: Int,
    val grouping: ExploreGrouping,
    val hideWatched: Boolean,
    val liveOnly: Boolean,
    val length: ExploreLength,
    val freshness: ExploreFreshness
)