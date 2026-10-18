package com.sentup.android.ui.library

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListNameLabelsTest {

    @Test
    fun `your likes is recognized`() {
        assertEquals(
            KnownListNameMatch(KnownListName.YourLikes),
            matchKnownListName("Your Likes")
        )
    }

    @Test
    fun `your likes is case insensitive`() {
        assertEquals(
            KnownListNameMatch(KnownListName.YourLikes),
            matchKnownListName("your likes")
        )
    }

    @Test
    fun `audio library songs with order marker is recognized`() {
        assertEquals(
            KnownListNameMatch(KnownListName.AllAudioLibrarySongs),
            matchKnownListName("All Youtube Audio Library Songs [Order: Artist]")
        )
    }

    @Test
    fun `audio library songs without marker is recognized`() {
        assertEquals(
            KnownListNameMatch(KnownListName.AllAudioLibrarySongs),
            matchKnownListName("All Youtube Audio Library Songs")
        )
    }

    @Test
    fun `episodes for later is recognized`() {
        assertEquals(
            KnownListNameMatch(KnownListName.EpisodesForLater),
            matchKnownListName("Episodes for Later")
        )
    }

    @Test
    fun `watch later is recognized`() {
        assertEquals(
            KnownListNameMatch(KnownListName.EpisodesForLater),
            matchKnownListName("Watch Later")
        )
    }

    @Test
    fun `presenting keeps the artist argument`() {
        assertEquals(
            KnownListNameMatch(KnownListName.Presenting, "P!NK"),
            matchKnownListName("Presenting P!NK")
        )
    }

    @Test
    fun `user playlist names are never matched`() {
        assertNull(matchKnownListName("My Mix"))
        assertNull(matchKnownListName("Workout 2024"))
        assertNull(matchKnownListName(""))
        assertNull(matchKnownListName("   "))
    }

    @Test
    fun `presenting without an artist does not match`() {
        // The pattern needs an artist after the keyword; a bare "Presenting"
        // is a user's own title and must survive untouched.
        assertNull(matchKnownListName("Presenting"))
        assertNull(matchKnownListName("Presenting   "))
    }

    @Test
    fun `the presenting argument keeps its original capitals`() {
        assertEquals(
            KnownListNameMatch(KnownListName.Presenting, "Ärtsy and the Aints"),
            matchKnownListName("Presenting Ärtsy and the Aints")
        )
        assertEquals(
            KnownListNameMatch(KnownListName.UploadsFrom, "Sezen Aksu"),
            matchKnownListName("Uploads from Sezen Aksu")
        )
    }

    @Test
    fun `localizedListName falls back to the raw name for unknown titles`() {
        val raw = "Gym Playlist"
        assertEquals(raw, localizedListName(raw) { _, _ -> error("must not be called") })
    }

    @Test
    fun `localizedListName falls back when the resolver returns null`() {
        // A match with no translation still shows the user the original name.
        assertEquals("Your Likes", localizedListName("Your Likes") { _, _ -> null })
    }

    @Test
    fun `localizedListName resolves a known title`() {
        val resolved = localizedListName("Your Likes") { key, _ ->
            when (key) {
                KnownListName.YourLikes -> "Beğenilenler"
                else -> null
            }
        }
        assertEquals("Beğenilenler", resolved)
    }

    @Test
    fun `localizedListName passes the presenting argument through`() {
        val resolved = localizedListName("Presenting P!NK") { key, argument ->
            when (key) {
                KnownListName.Presenting -> "Sunan: $argument"
                else -> null
            }
        }
        assertEquals("Sunan: P!NK", resolved)
    }
}
