package com.sentup.android.ui.search

import java.util.Locale

/**
 * Search completions offered while the field is focused, before the user
 * commits.
 *
 * There is no autocomplete endpoint in the project's YouTube layer (see the
 * InnerTube wrappers in `data/`), so the suggestions are built locally from
 * what this app already knows: the search history, the library's artists and
 * song titles, and the playlists the user has kept. Nothing here invents a
 * source, so a suggestion is always something the user has touched before.
 *
 * The whole engine is pure on purpose - it is the half of the feature worth
 * proving, and it compiles against the stub classpath in .probe/kt/verify.sh.
 */
internal const val SEARCH_SUGGESTION_DEBOUNCE_MS = 300L
internal const val MAX_SEARCH_SUGGESTIONS = 7

/** Where a suggestion came from, which decides its icon and its kind label. */
enum class SearchSuggestionKind {
    RECENT_SEARCH,
    ARTIST,
    PLAYLIST,
    SONG
}

internal data class SearchSuggestion(
    val text: String,
    val kind: SearchSuggestionKind,
    /** Second line: the artist a song came from, or a kind label for the rest. */
    val detail: String? = null
)

/**
 * Folds to one letter family so a Turkish user typing "ıslak" finds "Islak"
 * and typing "ISLAK" finds it too. Both spellings must match, which is not
 * what a plain `lowercase()` gives.
 */
internal fun foldForSearch(input: String): String = input
    .replace('ı', 'i')
    .replace('I', 'i')
    .replace('İ', 'i')
    .lowercase(Locale.ROOT)

/**
 * How well [candidate] answers [query]. Higher is better; 0 is no match.
 *
 * Three tiers, because a prefix answer is what the user was typing and a
 * buried match is something else: candidate starts with the query, one of its
 * words starts with the query, or the query merely appears inside it.
 */
internal fun suggestionMatchScore(query: String, candidate: String): Int {
    if (query.isEmpty()) return 0
    val folded = foldForSearch(candidate)
    if (!folded.contains(query)) return 0
    if (folded.startsWith(query)) return 3
    if (folded.split(' ', '\t', '\n', '(', '-', '–', '—').any { it.startsWith(query) }) return 2
    return 1
}

internal fun buildSearchSuggestions(
    query: String,
    recentSearches: List<String> = emptyList(),
    artists: List<String> = emptyList(),
    playlists: List<String> = emptyList(),
    songs: List<Pair<String, String>> = emptyList(),
    limit: Int = MAX_SEARCH_SUGGESTIONS
): List<SearchSuggestion> {
    val needle = foldForSearch(query.trim())
    if (needle.isEmpty()) return emptyList()

    data class Candidate(
        val suggestion: SearchSuggestion,
        val score: Int,
        val priority: Int,
        val order: Int
    )

    val candidates = mutableListOf<Candidate>()
    var order = 0

    fun add(text: String, kind: SearchSuggestionKind, detail: String?, priority: Int) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || foldForSearch(trimmed) == needle) return
        val score = suggestionMatchScore(needle, trimmed)
        if (score == 0) return
        candidates += Candidate(SearchSuggestion(trimmed, kind, detail), score, priority, order++)
    }

    recentSearches.forEach { add(it, SearchSuggestionKind.RECENT_SEARCH, null, 0) }
    artists.forEach { add(it, SearchSuggestionKind.ARTIST, null, 1) }
    playlists.forEach { add(it, SearchSuggestionKind.PLAYLIST, null, 2) }
    songs.forEach { (title, artist) ->
        add(
            text = title,
            kind = SearchSuggestionKind.SONG,
            detail = artist.ifBlank { null },
            priority = 3
        )
    }

    // The folded text is the dedupe key: a song title the user also searched
    // for is one row, and the higher-priority source wins it (recent search,
    // then artist, then playlist, then song).
    val byText = LinkedHashMap<String, Candidate>()
    candidates.sortedWith(
        compareByDescending<Candidate> { it.score }
            .thenBy { it.priority }
            .thenBy { it.suggestion.text.length }
            .thenBy { it.order }
    ).forEach { candidate ->
        byText.putIfAbsent(foldForSearch(candidate.suggestion.text), candidate)
    }

    return byText.values
        .map { it.suggestion }
        .take(limit)
}
