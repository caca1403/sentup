package com.sentup.android.data

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Explore rules and the shelf builder.
 *
 * Pure on purpose - [ExploreEngine] takes its clock, its watch history and its
 * [Random] as arguments - so every rule is checked here rather than on a
 * screen. `isReturnDefaultValues = true` stubs the Android framework, which is
 * why nothing in this file touches `VideoItem.highResThumbnailUrl` or any
 * other platform-backed property.
 */
class ExploreEngineTest {

    private val now = 1_760_000_000_000L
    private val day = 86_400_000L

    private fun video(
        id: String,
        channelId: String? = "UCa",
        channelName: String = "Channel A",
        title: String = id,
        duration: Long = 600L,
        views: String = "1K views",
        publishedDaysAgo: Long? = 1L,
        live: Boolean = false
    ) = VideoItem(
        videoId = id,
        title = title,
        channelName = channelName,
        channelId = channelId,
        thumbnailUrl = null,
        duration = duration,
        viewCount = views,
        uploadedDate = null,
        isLive = live,
        publishedAtMs = publishedDaysAgo?.let { now - it * day }
    )

    private fun build(
        videos: List<VideoItem>,
        rules: ExploreRules,
        watched: Set<String> = emptySet()
    ) = ExploreEngine.build(videos, rules, now, watched, Random(7))

    // ---------------- Filtering ----------------

    @Test
    fun `a length filter keeps only that band`() {
        val videos = listOf(
            video("short", duration = 60L),
            video("medium", duration = 600L),
            video("long", duration = 3_600L)
        )
        val short = build(videos, ExploreRules(length = ExploreLength.SHORT))
        assertEquals(listOf("short"), short.flatMap { it.videos }.map { it.videoId })
    }

    @Test
    fun `a live stream is in no length band`() {
        // A broadcast has no runtime, so filing it under "under 4 minutes"
        // would be inventing one.
        val live = video("live", live = true, duration = 0L)
        assertTrue(ExploreLength.SHORT.matches(live.duration, live.isLive).not())
        assertTrue(ExploreLength.ANY.matches(live.duration, live.isLive))
        val shelves = build(listOf(live), ExploreRules(grouping = ExploreGrouping.DURATION))
        assertEquals(1, shelves.size)
        assertEquals(1, shelves.single().videos.size)
    }

    @Test
    fun `an undated video cannot satisfy a bounded age`() {
        val undated = video("undated", publishedDaysAgo = null)
        assertTrue(ExploreFreshness.TODAY.matches(undated.publishedAtMs, now).not())
        assertTrue(ExploreFreshness.ANY.matches(undated.publishedAtMs, now))
    }

    @Test
    fun `prose upload dates are read as timestamps`() {
        val rules = ExploreRules(freshness = ExploreFreshness.WEEK)
        val prose = video("prose").copy(publishedAtMs = null, uploadedDate = "2 days ago")
        assertEquals(listOf("prose"), build(listOf(prose), rules).flatMap { it.videos }.map { it.videoId })
    }

    @Test
    fun `hide watched drops what this profile has played`() {
        val videos = listOf(video("a"), video("b"))
        val rules = ExploreRules(hideWatched = true)
        val shelves = build(videos, rules, watched = setOf("a"))
        assertEquals(listOf("b"), shelves.flatMap { it.videos }.map { it.videoId })
    }

    @Test
    fun `include words keeps a match and exclude words drops one`() {
        val videos = listOf(
            video("tutorial", title = "Rust tutorial for beginners"),
            video("drama", title = "Kitchen drama")
        )
        val kept = build(videos, ExploreRules(includeWords = listOf("rust")))
        assertEquals(listOf("tutorial"), kept.flatMap { it.videos }.map { it.videoId })

        val dropped = build(videos, ExploreRules(excludeWords = listOf("drama")))
        assertEquals(listOf("tutorial"), dropped.flatMap { it.videos }.map { it.videoId })
    }

    @Test
    fun `an exclude word matches the channel name too`() {
        val videos = listOf(
            video("one", channelName = "News Room", title = "A report"),
            video("two", channelName = "Cooking", title = "A recipe")
        )
        val shelves = build(videos, ExploreRules(excludeWords = listOf("news")))
        assertEquals(listOf("two"), shelves.flatMap { it.videos }.map { it.videoId })
    }

    @Test
    fun `a channel filter matches ids and falls back to names`() {
        val videos = listOf(
            video("a", channelId = "UCa", channelName = "Alpha"),
            video("b", channelId = "UCb", channelName = "Beta"),
            video("c", channelId = null, channelName = "Gamma")
        )
        val byId = build(videos, ExploreRules(channelIds = listOf("UCb")))
        assertEquals(listOf("b"), byId.flatMap { it.videos }.map { it.videoId })

        // The lockup named the channel but not its id; a rule that silently
        // stopped matching those would look broken rather than lenient.
        val byName = build(videos, ExploreRules(channelIds = listOf("Gamma")))
        assertEquals(listOf("c"), byName.flatMap { it.videos }.map { it.videoId })
    }

    @Test
    fun `live only keeps broadcasts`() {
        val videos = listOf(video("a"), video("live", live = true))
        val shelves = build(videos, ExploreRules(liveOnly = true))
        assertEquals(listOf("live"), shelves.flatMap { it.videos }.map { it.videoId })
    }

    @Test
    fun `everything filtered out gives no shelves at all`() {
        // An empty shelf would still have to be drawn; an empty list is what
        // the interface already knows how to word.
        val shelves = build(listOf(video("a")), ExploreRules(includeWords = listOf("zzz")))
        assertTrue(shelves.isEmpty())
    }

    // ---------------- Grouping ----------------

    @Test
    fun `grouping by channel gives one shelf each, named by its channel`() {
        val videos = listOf(
            video("a1", channelId = "UCa", channelName = "Alpha"),
            video("b1", channelId = "UCb", channelName = "Beta"),
            video("a2", channelId = "UCa", channelName = "Alpha")
        )
        val shelves = build(videos, ExploreRules(grouping = ExploreGrouping.CHANNEL))
        assertEquals(2, shelves.size)
        assertEquals(setOf("Alpha", "Beta"), shelves.map { it.channelName }.toSet())
        val alpha = shelves.first { it.channelName == "Alpha" }
        assertEquals(2, alpha.videos.size)
        assertEquals("UCa", alpha.channelId)
    }

    @Test
    fun `a channel shelf shows the name, not the id it groups by`() {
        // The grouping key is the UC id whenever a video carries one, which is
        // nearly always - so a header reading shelf.channelName was showing
        // "UCabc" where it meant to show the channel's name.
        val shelves = build(
            listOf(video("a", channelId = "UCabcdefghij", channelName = "Alpha")),
            ExploreRules(grouping = ExploreGrouping.CHANNEL)
        )
        assertEquals("Alpha", shelves.single().channelName)
    }

    @Test
    fun `every grouping honours the shelf cap, not only the channel one`() {
        val videos = (1..30).map { video("v$it", channelId = "UC$it") }
        val rules = ExploreRules(perShelfLimit = 4)
        for (grouping in ExploreGrouping.entries) {
            val capped = build(videos, rules.copy(grouping = grouping))
            assertTrue(
                "grouping $grouping drew a shelf over the cap",
                capped.all { it.videos.size <= 4 }
            )
        }
    }

    @Test
    fun `shelf ids are distinct, because they are lazy list keys`() {
        val videos = listOf(
            video("a", channelId = "UCa", channelName = "Same"),
            video("b", channelId = "UCb", channelName = "Same")
        )
        val shelves = build(videos, ExploreRules(grouping = ExploreGrouping.CHANNEL))
        assertEquals(shelves.size, shelves.map { it.id }.toSet().size)
    }

    @Test
    fun `grouping by duration keeps every video exactly once`() {
        val videos = listOf(
            video("short", duration = 60L),
            video("medium", duration = 600L),
            video("long", duration = 7_200L),
            video("live", live = true, duration = 0L),
            video("unknown", duration = 0L)
        )
        val shelves = build(videos, ExploreRules(grouping = ExploreGrouping.DURATION))
        val all = shelves.flatMap { it.videos }.map { it.videoId }
        assertEquals(5, all.size)
        assertEquals(5, all.toSet().size)
    }

    @Test
    fun `grouping by age bands newest to oldest`() {
        val videos = listOf(
            video("today", publishedDaysAgo = 0L),
            video("week", publishedDaysAgo = 3L),
            video("month", publishedDaysAgo = 20L),
            video("old", publishedDaysAgo = 400L)
        )
        val shelves = build(videos, ExploreRules(grouping = ExploreGrouping.FRESHNESS))
        val order = shelves.mapNotNull { it.age?.id }
        assertEquals(listOf("today", "week", "month", "any"), order)
    }

    @Test
    fun `no grouping is one flat shelf`() {
        val videos = listOf(video("a", channelId = "UCa"), video("b", channelId = "UCb"))
        val shelves = build(videos, ExploreRules(grouping = ExploreGrouping.NONE))
        assertEquals(1, shelves.size)
        assertEquals(ExploreShelfKind.WHOLE, shelves.single().kind)
        assertEquals(2, shelves.single().videos.size)
    }

    // ---------------- Ordering ----------------

    @Test
    fun `newest sorts by timestamp and undated goes last`() {
        val videos = listOf(
            video("old", publishedDaysAgo = 10L),
            video("new", publishedDaysAgo = 1L),
            video("undated", publishedDaysAgo = null)
        )
        val order = build(videos, ExploreRules(grouping = ExploreGrouping.NONE))
            .single().videos.map { it.videoId }
        assertEquals(listOf("new", "old", "undated"), order)
    }

    @Test
    fun `most viewed reads a formatted count`() {
        val videos = listOf(
            video("small", views = "900 views"),
            video("huge", views = "2.4M views"),
            video("mid", views = "345,678 views")
        )
        val order = build(videos, ExploreRules(grouping = ExploreGrouping.NONE, sort = ExploreSort.MOST_VIEWED))
            .single().videos.map { it.videoId }
        assertEquals(listOf("huge", "mid", "small"), order)
    }

    @Test
    fun `shortest sorts a live stream last rather than first`() {
        val videos = listOf(
            video("live", duration = 0L, live = true),
            video("long", duration = 3_600L),
            video("quick", duration = 60L)
        )
        val order = build(videos, ExploreRules(grouping = ExploreGrouping.NONE, sort = ExploreSort.QUICKEST))
            .single().videos.map { it.videoId }
        assertEquals(listOf("quick", "long", "live"), order)
    }

    @Test
    fun `the same seed gives the same order`() {
        val videos = (1..8).map { video("v$it") }
        val rules = ExploreRules(grouping = ExploreGrouping.NONE, sort = ExploreSort.SHUFFLED)
        val first = ExploreEngine.build(videos, rules, now, emptySet(), Random(3))
            .single().videos.map { it.videoId }
        val second = ExploreEngine.build(videos, rules, now, emptySet(), Random(3))
            .single().videos.map { it.videoId }
        assertEquals(first, second)
    }

    // ---------------- Capping and dedupe ----------------

    @Test
    fun `each shelf is capped and repeats collapse`() {
        val videos = (1..30).map { video("v$it") }
        val shelves = build(videos, ExploreRules(perShelfLimit = 5))
        assertEquals(5, shelves.single().videos.size)

        val repeated = build(videos + videos, ExploreRules(perShelfLimit = 30))
        assertEquals(30, repeated.single().videos.size)
    }

    @Test
    fun `a shelf limit outside the supported range is clamped`() {
        val rules = ExploreRules(perShelfLimit = 9_999).withShelfLimit(9_999)
        assertEquals(ExploreRules.MAX_PER_SHELF_LIMIT, rules.perShelfLimit)
        assertEquals(
            ExploreRules.MIN_PER_SHELF_LIMIT,
            ExploreRules(perShelfLimit = 0).withShelfLimit(0).perShelfLimit
        )
    }

    // ---------------- View counts ----------------

    @Test
    fun `view counts parse in either locale and with any suffix`() {
        assertEquals(1_200_000L, ExploreEngine.parseViewCount("1.2M views"))
        assertEquals(1_200_000L, ExploreEngine.parseViewCount("1,2M görüntülenme"))
        assertEquals(1_234L, ExploreEngine.parseViewCount("1.234 views"))
        assertEquals(1_234_567L, ExploreEngine.parseViewCount("1,234,567 views"))
        assertEquals(1_500_000_000L, ExploreEngine.parseViewCount("1.5B views"))
        assertEquals(42L, ExploreEngine.parseViewCount("42"))
        assertEquals(2_000L, ExploreEngine.parseViewCount("2K"))
    }

    @Test
    fun `a view count with no number is absent, not zero`() {
        assertNull(ExploreEngine.parseViewCount(null))
        assertNull(ExploreEngine.parseViewCount(""))
        assertNull(ExploreEngine.parseViewCount("No views"))
    }

    // ---------------- Persistence ----------------

    @Test
    fun `rules survive a round trip through json`() {
        val rules = ExploreRules(
            sources = setOf(ExploreSource.HISTORY, ExploreSource.LIKED),
            grouping = ExploreGrouping.DURATION,
            sort = ExploreSort.SHUFFLED,
            length = ExploreLength.SHORT,
            freshness = ExploreFreshness.MONTH,
            channelIds = listOf("UCa", "UCb"),
            includeWords = listOf("rust"),
            excludeWords = listOf("drama"),
            hideWatched = true,
            liveOnly = true,
            perShelfLimit = 7
        )
        assertEquals(rules, ExploreRules.fromJson(rules.toJson()))
    }

    @Test
    fun `an unreadable value falls back rather than dropping the list`() {
        val stored = ExploreRules().toJson()
            .put("grouping", "something_from_a_later_build")
            .put("sources", org.json.JSONArray().put("nonsense"))
            .put("perShelfLimit", 9_999)
        val decoded = ExploreRules.fromJson(stored)
        assertEquals(ExploreGrouping.NONE, decoded.grouping)
        // An empty source set would fetch nothing and show nothing.
        assertEquals(setOf(ExploreSource.SUBSCRIPTIONS), decoded.sources)
        assertEquals(ExploreRules.MAX_PER_SHELF_LIMIT, decoded.perShelfLimit)
    }

    @Test
    fun `a preset is a usable rule set`() {
        for (preset in ExplorePreset.entries) {
            // A preset that read from nothing would create a list that can
            // never be filled, which is worse than not offering it.
            assertFalse(preset.rules.sources.isEmpty())
        }
    }
}