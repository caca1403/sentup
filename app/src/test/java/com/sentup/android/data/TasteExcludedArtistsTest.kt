package com.sentup.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The narrowing list lives inside the taste profile's own JSON, so its whole
 * contract is what [TasteProfileStore.encode] writes and what
 * [TasteProfileStore.decode] reads back.
 *
 * That pair is worth testing on its own because everything that moves the
 * profile - the per-profile preference key, a backup, the copy made to a profile
 * signed in from a device-only one - carries the encoded string unchanged and
 * therefore cannot be tested from here at all. What can be tested is the two
 * directions a schema change breaks: a field written and not read back, and old
 * text that no longer parses because a newer field is missing from it.
 */
class TasteExcludedArtistsTest {

    @Test
    fun `excluded artists survive the store's own encoding`() {
        val data = TasteProfileData(
            artists = listOf(TasteArtist("UC1", "Boarders")),
            genres = listOf(TasteGenre("post-punk")),
            excludedArtists = listOf("Radiohead", "Linkin Park")
        )

        val restored = TasteProfileStore.decode(TasteProfileStore.encode(data))

        assertEquals(data, restored)
        assertEquals(listOf("Radiohead", "Linkin Park"), restored.excludedArtists)
    }

    /** The backwards-compatibility claim: a profile from before this existed. */
    @Test
    fun `a profile written before the narrowing still reads back`() {
        val legacy = """{"artists":[{"id":"UC1","name":"Boarders","thumb":null}],""" +
            """"genres":[{"title":"post-punk"}],""" +
            """"songs":[{"id":"v1","title":"Tears","artist":"Boarders","thumb":null}]}"""

        val restored = TasteProfileStore.decode(legacy)

        assertEquals(listOf("Boarders"), restored.artists.map { it.name })
        assertEquals(listOf("post-punk"), restored.genres.map { it.title })
        assertEquals(listOf("Tears"), restored.songs.map { it.title })
        assertTrue(restored.excludedArtists.isEmpty())
    }

    /** An empty list is written as an empty array and reads back empty, not null-ish. */
    @Test
    fun `no exclusions round trips as no exclusions`() {
        val data = TasteProfileData(artists = listOf(TasteArtist("UC1", "Boarders")))

        val restored = TasteProfileStore.decode(TasteProfileStore.encode(data))

        assertEquals(data, restored)
        assertTrue(restored.excludedArtists.isEmpty())
    }

    @Test
    fun `blank and null entries in the array are dropped`() {
        val restored = TasteProfileStore.decode("""{"excluded":["Radiohead","","   ",null]}""")

        assertEquals(listOf("Radiohead"), restored.excludedArtists)
    }

    /**
     * An exclusion is a decision about what to leave out, not a taste to seed
     * recommendations from: a page whose only content is "not Radiohead" is
     * still empty for the cold-start path, which is what `isEmpty` is for.
     */
    @Test
    fun `an exclusion alone does not make a taste profile`() {
        assertTrue(TasteProfileData(excludedArtists = listOf("Radiohead")).isEmpty)

        val restored = TasteProfileStore.decode("""{"excluded":["Radiohead"]}""")

        assertEquals(listOf("Radiohead"), restored.excludedArtists)
        assertTrue(restored.isEmpty)
    }

    /** The rule that kept working: a taste store is never worth a crash. */
    @Test
    fun `unreadable text is an empty profile rather than a failure`() {
        val restored = TasteProfileStore.decode("{ not json at all")

        assertTrue(restored.excludedArtists.isEmpty())
        assertTrue(restored.isEmpty)
    }
}
