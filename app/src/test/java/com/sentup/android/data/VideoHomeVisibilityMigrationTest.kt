package com.sentup.android.data

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The visible-set migration in `ThemePreferences.getVideoHomeConfiguration`.
 *
 * Copied here rather than imported, because `ThemePreferences` is a 3000-line
 * class with a Context, a SharedPreferences and thirty other keys in it. What
 * is under test is the decision, not the plumbing: the expression below is the
 * one in the class, with `defaults` and the two stored sets as arguments.
 */
class VideoHomeVisibilityMigrationTest {

    /** Verbatim from the enum's companion. */
    private val defaults = VideoHomeDestination.entries.toList()

    private val addedAfterKey = setOf(VideoHomeDestination.EXPLORE)

    private fun resolve(
        storedVisible: Set<String>?,
        storedKnown: Set<String>?
    ): Set<VideoHomeDestination> {
        val known = storedKnown
            ?.mapNotNull(VideoHomeDestination::fromStorageId)
            ?: defaults.filterNot { it in addedAfterKey }
        val arrived = defaults.filterNot { it in known }
        val visible = storedVisible
            ?.mapNotNull(VideoHomeDestination::fromStorageId)
            ?.toSet()
            ?.takeIf { it.isNotEmpty() }
            ?: defaults.toSet()
        return (visible + arrived).toSet()
    }

    private fun ids(set: Set<VideoHomeDestination>) = set.map { it.storageId }.toSet()

    @Test
    fun `a fresh install shows everything`() {
        assertEquals(ids(defaults.toSet()), ids(resolve(null, null)))
    }

    @Test
    fun `an existing install gains only the new tab`() {
        // Subscriptions hidden by the user before this build existed.
        val stored = setOf("home", "search", "library")
        assertEquals(setOf("home", "search", "library", "explore"), ids(resolve(stored, null)))
    }

    @Test
    fun `an install that already wrote the key gains nothing`() {
        // Second launch after the upgrade: the key is present, so nothing is
        // "arriving" and a deliberately hidden destination stays hidden.
        val stored = setOf("home", "search", "library")
        val known = defaults.mapTo(mutableSetOf()) { it.storageId }
        assertEquals(setOf("home", "search", "library"), ids(resolve(stored, known)))
    }

    @Test
    fun `hiding the new tab after the upgrade survives`() {
        // The user hides Explore; the written set no longer names it, and the
        // key now does, so it must not come back on the next read.
        val known = defaults.mapTo(mutableSetOf()) { it.storageId }
        assertEquals(
            setOf("home", "search", "library"),
            ids(resolve(setOf("home", "search", "library"), known))
        )
    }

    @Test
    fun `the known key is written after the upgrade`() {
        // The guard has to fire exactly once, on the run that fills the key in.
        val knownOnUpgrade = defaults.filterNot { it in addedAfterKey }.toSet()
        val arrivedOnUpgrade = defaults.filterNot { it in knownOnUpgrade }
        assertEquals(setOf(VideoHomeDestination.EXPLORE), arrivedOnUpgrade.toSet())
        assertEquals(true, knownOnUpgrade.toSet() != defaults.toSet())
        // And not again once the key holds the full set.
        val knownAfter = defaults.toSet()
        val arrivedAfter = defaults.filterNot { it in knownAfter }
        assertEquals(emptyList<VideoHomeDestination>(), arrivedAfter)
        assertEquals(false, knownAfter != defaults.toSet())
    }

    @Test
    fun `the order appends a new destination rather than renumbering`() {
        val storedOrder = listOf("home", "search", "subscriptions", "library")
            .mapNotNull(VideoHomeDestination::fromStorageId)
        val order = storedOrder + defaults.filterNot { it in storedOrder }
        assertEquals(
            listOf("home", "search", "subscriptions", "library", "explore"),
            order.map { it.storageId }
        )
        // The ids that were already stored still mean the same screen.
        assertEquals(0, VideoHomeDestination.fromStorageId("home")?.tabId)
        assertEquals(3, VideoHomeDestination.fromStorageId("library")?.tabId)
        assertEquals(4, VideoHomeDestination.fromStorageId("explore")?.tabId)
    }
}
