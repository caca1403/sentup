package com.sentup.android.ui.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The suggestion engine is the testable half of as-you-type search: matching
 * tiers, source priority, dedupe, and the Turkish-I fold.
 *
 * The engine takes its four sources as separate lists rather than one
 * container, so that is how these call it.
 */
class SearchSuggestionTest {

    @Test
    fun `prefix match outranks word match outranks substring match`() {
        val suggestions = buildSearchSuggestions(
            query = "pin",
            artists = listOf("Pink Floyd", "The Pink Show", "Unpinned")
        )
        // Pink Floyd (prefix, 3) first, The Pink Show (word, 2) second,
        // Unpinned (the query only inside a word, 1) last.
        assertEquals(
            listOf("Pink Floyd", "The Pink Show", "Unpinned"),
            suggestions.map { it.text }
        )
    }

    @Test
    fun `an empty or whitespace-only query suggests nothing`() {
        assertTrue(buildSearchSuggestions(query = "", artists = listOf("Pink")).isEmpty())
        assertTrue(buildSearchSuggestions(query = "   ", artists = listOf("Pink")).isEmpty())
    }

    @Test
    fun `a query no source contains suggests nothing`() {
        assertTrue(
            buildSearchSuggestions(query = "xyz", artists = listOf("Pink")).isEmpty()
        )
    }

    @Test
    fun `the query itself is never suggested back`() {
        val suggestions = buildSearchSuggestions(
            query = "Pink",
            recentSearches = listOf("pink"),
            artists = listOf("Pink")
        )
        assertTrue(suggestions.isEmpty())
    }

    @Test
    fun `suggestions are capped at the limit`() {
        val suggestions = buildSearchSuggestions(
            query = "z",
            artists = (1..20).map { "Zartist $it" }
        )
        assertEquals(MAX_SEARCH_SUGGESTIONS, suggestions.size)
    }

    @Test
    fun `a recent query outranks an artist with the same name`() {
        val suggestions = buildSearchSuggestions(
            query = "pink",
            recentSearches = listOf("Pink Floyd"),
            artists = listOf("Pink Floyd")
        )
        assertEquals(1, suggestions.size)
        assertEquals(SearchSuggestionKind.RECENT_SEARCH, suggestions[0].kind)
    }

    @Test
    fun `case and whitespace differences collapse into one suggestion`() {
        val suggestions = buildSearchSuggestions(
            query = "pink",
            recentSearches = listOf("Pink Floyd"),
            artists = listOf("PINK FLOYD"),
            playlists = listOf(" pink floyd ")
        )
        assertEquals(1, suggestions.size)
        assertEquals("Pink Floyd", suggestions[0].text)
    }

    @Test
    fun `a song title carries its artist as the detail line`() {
        val suggestions = buildSearchSuggestions(
            query = "big",
            songs = listOf("Big Ideas" to "Hot Club")
        )
        assertEquals("Big Ideas", suggestions[0].text)
        assertEquals("Hot Club", suggestions[0].detail)
        assertEquals(SearchSuggestionKind.SONG, suggestions[0].kind)
    }

    @Test
    fun `every source kind is represented when it matches`() {
        val suggestions = buildSearchSuggestions(
            query = "paris",
            recentSearches = listOf("Paris Soon"),
            artists = listOf("Paris Hilton"),
            playlists = listOf("Paris 2024"),
            songs = listOf("Paris Nights" to "Lauv")
        )
        val kinds = suggestions.map { it.kind }.toSet()
        assertTrue(kinds.contains(SearchSuggestionKind.RECENT_SEARCH))
        assertTrue(kinds.contains(SearchSuggestionKind.ARTIST))
        assertTrue(kinds.contains(SearchSuggestionKind.PLAYLIST))
        assertTrue(kinds.contains(SearchSuggestionKind.SONG))
    }

    @Test
    fun `an unmatched artist never shadows a matched one`() {
        val suggestions = buildSearchSuggestions(
            query = "pink",
            artists = listOf("Unrelated", "Pink Floyd")
        )
        assertEquals(listOf("Pink Floyd"), suggestions.map { it.text })
    }

    @Test
    fun `a shorter prefix candidate beats a longer one at the same tier`() {
        val suggestions = buildSearchSuggestions(
            query = "pin",
            artists = listOf("Pinkie", "Pink")
        )
        assertEquals("Pink", suggestions[0].text)
    }

    @Test
    fun `the Turkish dotless i folds onto the dotted one in both directions`() {
        assertEquals("islak", foldForSearch("ISLAK"))
        assertEquals("islak", foldForSearch("ıslak"))
        assertEquals("islak", foldForSearch("İslak"))

        val fromDotless = buildSearchSuggestions(query = "ıslak", artists = listOf("Islak Plakları"))
        assertEquals(listOf("Islak Plakları"), fromDotless.map { it.text })

        val fromDotted = buildSearchSuggestions(query = "islak", artists = listOf("ISLAK PLAKLARI"))
        assertEquals(listOf("ISLAK PLAKLARI"), fromDotted.map { it.text })
    }

    @Test
    fun `match tiers are scored three two one`() {
        assertEquals(3, suggestionMatchScore("pin", "Pink Floyd"))
        assertEquals(2, suggestionMatchScore("pin", "The Pink Floyd"))
        assertEquals(1, suggestionMatchScore("pin", "Deep Purple Unpinned"))
        assertEquals(0, suggestionMatchScore("pin", "Radiohead"))
        assertEquals(0, suggestionMatchScore("", "Anything"))
    }
}
