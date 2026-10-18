package com.sentup.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import com.sentup.android.data.AutoMixEngine.VideoRun

/**
 * The video half of [AutoMixEngine], rule by rule.
 *
 * These rules are pure functions of a collapsed watch list and a clock, so all
 * of them can be answered here without a Context, a preference file or a
 * YouTube call - which is the point of them being pure. What is under test is
 * the *choosing*, since everything around it (titles, reasons, ordering of the
 * cards) is decoration over what these return.
 *
 * Every number below is spelled out rather than derived from the constants in
 * the rule, so a rule that quietly moves its own threshold fails here instead
 * of moving the test with it.
 */
class AutoMixRulesTest {

    private val now = 1_750_000_000_000L
    private val day = 86_400_000L

    /**
     * @param daysAgo when the watch happened; null stands for a row the store
     *   never timed, which is the case a rule has to refuse rather than guess.
     */
    private fun video(
        id: String,
        daysAgo: Double? = null,
        durationSec: Long = 600L,
        progress: Float? = null,
        channel: String = "Channel",
        index: Int = 0
    ) = VideoRun(
        videoId = id,
        channelName = channel,
        durationSec = durationSec,
        watchedProgress = progress,
        lastWatchedAt = daysAgo?.let { now - (it * day).toLong() } ?: 0L,
        index = index
    )

    // ----------------------------------------------------------- rediscover

    @Test
    fun `a watch thirty days old counts as forgotten and a fresh one does not`() {
        val videos = listOf(
            video("old", daysAgo = 45.0, index = 2),
            video("exactly-thirty", daysAgo = 30.0, index = 1),
            video("fresh", daysAgo = 3.0, index = 0),
            video("last-week", daysAgo = 8.0, index = 3)
        )

        assertEquals(listOf("old", "exactly-thirty"), AutoMixEngine.videoRediscover(videos, now))
    }

    /** The boundary case that matters: an untimed row is not "very old". */
    @Test
    fun `a watch with no time on it is never claimed as forgotten`() {
        val videos = listOf(
            video("undated-deep", daysAgo = null, index = 90),
            video("old", daysAgo = 60.0, index = 91)
        )

        val chosen = AutoMixEngine.videoRediscover(videos, now)

        assertEquals(listOf("old"), chosen)
        assertFalse("an untimed row was treated as forgotten", "undated-deep" in chosen)
    }

    @Test
    fun `rediscovery puts the most forgotten first and stops at the mix size`() {
        val videos = (1..40).map { video("v$it", daysAgo = 31.0 + it, index = it) }

        val chosen = AutoMixEngine.videoRediscover(videos, now)

        assertEquals(25, chosen.size)
        // Oldest watch first: 31+40 days is the furthest gone.
        assertEquals("v40", chosen.first())
        assertEquals("v16", chosen.last())
    }

    // ---------------------------------------------------------- this week

    /** The seven-day count, which is the whole reason the mix says a number. */
    @Test
    fun `only the watches inside seven days are counted`() {
        val videos = listOf(
            video("today", daysAgo = 0.0, index = 0),
            video("two-days", daysAgo = 2.0, index = 1),
            video("six-days", daysAgo = 6.5, index = 2),
            video("eight-days", daysAgo = 8.0, index = 3),
            video("untimed", daysAgo = null, index = 4)
        )

        val chosen = AutoMixEngine.videoThisWeek(videos, now)

        assertEquals(listOf("today", "two-days", "six-days"), chosen)
        assertEquals(3, chosen.size)
    }

    @Test
    fun `this week is ordered by the newest watch`() {
        // Store order (index) and watch time disagree on purpose.
        val videos = listOf(
            video("older-but-top", daysAgo = 5.0, index = 0),
            video("newest", daysAgo = 1.0, index = 5),
            video("untimed", daysAgo = null, index = 1)
        )

        assertEquals(
            listOf("newest", "older-but-top"),
            AutoMixEngine.videoThisWeek(videos, now)
        )
    }

    // -------------------------------------------------- one and done video

    /**
     * The "watched once" rule, which this store can only answer as "started
     * and left part-way" - a rewatch lifts the row instead of counting.
     */
    @Test
    fun `a video is one-and-done when it was started and left part-way`() {
        val videos = listOf(
            video("half-watched", progress = 0.4f, daysAgo = 1.0, index = 1),
            video("finished", progress = 0.95f, daysAgo = 1.0, index = 2),
            video("mis-tap", progress = 0.01f, daysAgo = 1.0, index = 3),
            video("no-progress", progress = null, daysAgo = 1.0, index = 4),
            video("just-started", progress = 0.05f, daysAgo = 1.0, index = 0)
        )

        // Both bounds are inclusive, and the store's newest-first order holds.
        assertEquals(listOf("just-started", "half-watched"), AutoMixEngine.videoOneAndDone(videos))
    }

    @Test
    fun `the abandoned rule ignores length and time entirely`() {
        val videos = listOf(
            video("short-abandoned", daysAgo = 400.0, durationSec = 40L, progress = 0.2f, index = 0),
            video("long-abandoned", daysAgo = 0.0, durationSec = 9_000L, progress = 0.3f, index = 1)
        )

        assertEquals(
            listOf("short-abandoned", "long-abandoned"),
            AutoMixEngine.videoOneAndDone(videos)
        )
    }

    // ------------------------------------------------------------ lengths

    @Test
    fun `a short route is under three minutes and a known length`() {
        val videos = listOf(
            video("clip", durationSec = 45L, index = 1),
            video("under", durationSec = 179L, index = 0),
            video("exactly-three", durationSec = 180L, index = 2),
            video("long", durationSec = 600L, index = 3),
            video("unknown-length", durationSec = 0L, index = 4)
        )

        assertEquals(listOf("under", "clip"), AutoMixEngine.videoShortRuns(videos))
    }

    @Test
    fun `a deep watch is past eight minutes and comes longest first`() {
        val videos = listOf(
            video("eight-min", durationSec = 480L),
            video("nine-min", durationSec = 540L, index = 1),
            video("two-hours", durationSec = 7_200L, index = 2),
            video("seven-min", durationSec = 479L, index = 3)
        )

        assertEquals(listOf("two-hours", "nine-min"), AutoMixEngine.videoDeepDives(videos))
    }

    @Test
    fun `a length rule caps at the mix size`() {
        val videos = (1..30).map { video("s$it", durationSec = 60L, index = it) }

        assertEquals(25, AutoMixEngine.videoShortRuns(videos).size)
        assertEquals(25, AutoMixEngine.videoDeepDives(videos.map { it.copy(durationSec = 900L) }).size)
    }

    // ------------------------------------------------------------- sanity

    @Test
    fun `an empty watch history builds no ids anywhere`() {
        assertTrue(AutoMixEngine.videoRediscover(emptyList(), now).isEmpty())
        assertTrue(AutoMixEngine.videoThisWeek(emptyList(), now).isEmpty())
        assertTrue(AutoMixEngine.videoOneAndDone(emptyList()).isEmpty())
        assertTrue(AutoMixEngine.videoShortRuns(emptyList()).isEmpty())
        assertTrue(AutoMixEngine.videoDeepDives(emptyList()).isEmpty())
    }

    /** The thresholds the rules are documented by, asserted where they live. */
    @Test
    fun `the video rules share the music thresholds and their own lengths`() {
        assertEquals(30, AutoMixEngine.FORGET_DAYS)
        assertEquals(25, AutoMixEngine.MIX_SIZE)
        assertEquals(180L, AutoMixEngine.SHORT_MAX_SEC)
        assertEquals(480L, AutoMixEngine.LONG_MIN_SEC)
    }
}
