package com.sentup.android.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlin.math.pow

/**
 * Local recommendation engine.
 *
 * Builds a "taste profile" from what the user actually does in the app —
 * play history ([StatsRepository]), liked songs ([LikedSongsRepository]) and
 * search history ([SearchHistoryRepository]) — and uses it to personalize
 * both the home feed and the auto-queue, without requiring a YouTube login.
 *
 * Scoring: each play contributes a recency-decayed weight (half-life
 * [HALF_LIFE_DAYS]), so heavy rotation from months ago fades out while this
 * week's obsession dominates. Liked songs get a flat bonus on top.
 */
class RecommendationEngine(
    context: Context,
    private val youTubeRepository: YouTubeRepository
) {
    private val statsRepository = StatsRepository(context)
    private val searchHistoryRepository = SearchHistoryRepository(context)
    private val likedSongsRepository = LikedSongsRepository(context)
    private val tasteProfileStore = TasteProfileStore(context)

    data class TasteProfile(
        /** Artist names ranked by recency-weighted listening, best first. */
        val topArtists: List<String>,
        /** The user's highest-scoring YouTube songs (radio seed candidates). */
        val topSongs: List<PlayHistoryEntry>,
        /** Most recent search queries, newest first. */
        val recentSearches: List<String>,
        /** Genres picked in taste setup. Never inferred: nothing here knows a song's genre. */
        val genres: List<String> = emptyList()
    ) {
        fun isEmpty() =
            topArtists.isEmpty() && topSongs.isEmpty() && recentSearches.isEmpty() && genres.isEmpty()
    }

    /**
     * The three discovery shelves, carried together because they are fetched
     * together and a partial result is normal: any one of them can come back
     * empty without the other two being wrong.
     */
    data class DiscoveryCollections(
        val albums: List<PlaylistDisplayItem> = emptyList(),
        val playlists: List<PlaylistDisplayItem> = emptyList(),
        val artists: List<ArtistItem> = emptyList()
    ) {
        fun isEmpty() = albums.isEmpty() && playlists.isEmpty() && artists.isEmpty()
    }

    companion object {
        /** A play from two weeks ago counts half as much as one from today. */
        private const val HALF_LIFE_DAYS = 14.0

        /** Flat score bonus for songs the user explicitly liked. */
        private const val LIKED_BONUS = 3.0

        private const val MAX_ARTISTS = 8
        private const val MAX_TOP_SONGS = 10
        private const val MAX_RECENT_SEARCHES = 5
        private const val MS_PER_DAY = 86_400_000.0

        // Cold start, for someone who has played nothing yet. Deliberately
        // undated: "albums 2026" is wrong every January and reads as a stale
        // app long before anyone thinks to fix it.
        private const val COLD_START_ALBUMS = "new album releases"
        private const val COLD_START_PLAYLISTS = "top hits playlist"
        private const val COLD_START_ARTISTS = "popular artists"
    }

    suspend fun buildTasteProfile(): TasteProfile = withContext(Dispatchers.Default) {
        val history = statsRepository.loadHistory()
        val likedIds = likedSongsRepository.getAllLikedSongIds()
        val now = System.currentTimeMillis()

        // Recency-weighted score per song. History is newest-first, so the
        // first entry seen for an id carries the freshest metadata.
        val songScores = HashMap<String, Double>()
        val entryById = HashMap<String, PlayHistoryEntry>()
        for (entry in history) {
            val ageDays = (now - entry.timestamp).coerceAtLeast(0) / MS_PER_DAY
            val weight = 0.5.pow(ageDays / HALF_LIFE_DAYS)
            songScores.merge(entry.songId, weight, Double::plus)
            entryById.putIfAbsent(entry.songId, entry)
        }
        for (id in likedIds) {
            if (entryById.containsKey(id)) {
                songScores.merge(id, LIKED_BONUS, Double::plus)
            }
        }

        val rankedIds = songScores.entries.sortedByDescending { it.value }.map { it.key }

        // The narrowing, read once and applied to every seed this profile can
        // produce. This function is the choke point on purpose: the home feed,
        // the auto-queue, discovery and the shelves all seed from what it
        // returns, so dropping an artist here is the one edit that actually
        // stops recommending them - everywhere, rather than in whichever call
        // site remembered to filter. A seed is dropped rather than scored
        // lower: a narrowed artist that survives as the tenth-best still seeds
        // a radio, and a radio returns that artist's whole orbit.
        val stated = tasteProfileStore.current()
        val narrowed = stated.excludedArtists.map { it.trim().lowercase() }.filterTo(HashSet()) {
            it.isNotBlank()
        }
        fun isNarrowed(artist: String): Boolean =
            narrowed.isNotEmpty() && artist.trim().lowercase() in narrowed

        val topSongs = rankedIds.asSequence()
            .mapNotNull { entryById[it] }
            .filter { it.source == SongSource.YOUTUBE && !isNarrowed(it.artist) }
            .take(MAX_TOP_SONGS)
            .toList()

        val artistScores = HashMap<String, Double>()
        for ((id, score) in songScores) {
            val artist = entryById[id]?.artist ?: continue
            if (isUnknownArtist(artist) || isNarrowed(artist)) continue
            artistScores.merge(artist, score, Double::plus)
        }
        val playedArtists = artistScores.entries
            .sortedByDescending { it.value }
            .take(MAX_ARTISTS)
            .map { it.key }

        // What the user said they like, beside what the history shows. On a
        // new install it is the whole profile; later the two alternate, so a
        // followed artist keeps a seat without crowding out this week's
        // listening. Shuffled, because a follow list has no ranking and the
        // same first four would otherwise seed every refresh.
        //
        // A followed artist who was also narrowed is treated as narrowed: the
        // follow keeps their place in the Library, and the later, more specific
        // decision is the one that steers the recommendations.
        val statedArtists = stated.artists.map { it.name }
            .filterNot { isUnknownArtist(it) || isNarrowed(it) }
            .shuffled()
        val topArtists = alternate(playedArtists, statedArtists)
            .distinctBy { it.trim().lowercase() }
            .take(MAX_ARTISTS)

        // Songs said yes to in the taste deck seed radios once the history
        // has run out of its own, never ahead of it.
        val statedSongs = stated.songs.asSequence()
            .filterNot { isNarrowed(it.artist) }
            .map { song ->
                PlayHistoryEntry(
                    songId = song.id,
                    title = song.title,
                    artist = song.artist,
                    album = "",
                    timestamp = now,
                    duration = 0L,
                    thumbnailUrl = song.thumbnailUrl
                )
            }
            .toList()

        TasteProfile(
            topArtists = topArtists,
            topSongs = (topSongs + statedSongs).distinctBy { it.songId }.take(MAX_TOP_SONGS),
            recentSearches = searchHistoryRepository.getHistory().take(MAX_RECENT_SEARCHES),
            genres = stated.genres.map { it.title }
        )
    }

    /**
     * Songs to append to the playing queue.
     *
     * Prefers YouTube's actual related-songs radio for the current track;
     * falls back to searches seeded by the user's top artists, then to a
     * generic similarity search.
     */
    suspend fun getQueueContinuation(
        currentSong: Song?,
        excludeIds: Set<String>,
        limit: Int = 10
    ): List<Song> {
        val results = mutableListOf<Song>()

        // 1. Real related-songs radio for the current track
        if (currentSong != null && currentSong.source == SongSource.YOUTUBE) {
            results += youTubeRepository.getRelatedSongs(currentSong.id)
        }

        // 2. Taste-profile fallback: songs by the user's top artists.
        // Shuffled so consecutive loads don't always seed from the same artist.
        if (results.count { it.id !in excludeIds } < limit) {
            val profile = buildTasteProfile()
            for (artist in profile.topArtists.shuffled().take(2)) {
                results += youTubeRepository.search("$artist songs", YouTubeRepository.FILTER_SONGS)
                if (results.count { it.id !in excludeIds } >= limit) break
            }
        }

        // 3. Last resort: similarity search on the current song
        if (results.isEmpty() && currentSong != null) {
            results += youTubeRepository.search(
                "songs like ${currentSong.title} ${currentSong.artist}",
                YouTubeRepository.FILTER_SONGS
            )
        }

        return results.asSequence()
            .filter { it.id !in excludeIds }
            .distinctBy { it.id }
            .filterNot { isNarrowedSong(it) }
            .take(limit)
            .toList()
    }

    /**
     * Personalized home feed for logged-out users: interleaved results from
     * the user's top artists, their recent searches, and a radio seeded by a
     * favourite song (for discovery beyond exact-artist matches).
     */
    suspend fun getHomeRecommendations(limit: Int = 30): List<Song> = coroutineScope {
        val profile = buildTasteProfile()
        if (profile.isEmpty()) {
            // Nothing local to personalize with yet — fall back to trending.
            return@coroutineScope youTubeRepository.search("trending music", YouTubeRepository.FILTER_SONGS)
                .withoutNarrowedArtists()
        }

        val seedQueries = buildList {
            profile.topArtists.take(3).forEach { add("$it songs") }
            profile.recentSearches.take(2).forEach { add(it) }
            profile.genres.shuffled().take(2).forEach { add("$it music") }
        }

        val searchBuckets = seedQueries.map { query ->
            async {
                runCatching { youTubeRepository.search(query, YouTubeRepository.FILTER_SONGS) }
                    .getOrDefault(emptyList())
            }
        }
        val radioBucket = async {
            profile.topSongs.firstOrNull()?.let { seed ->
                runCatching { youTubeRepository.getRelatedSongs(seed.songId) }
                    .getOrDefault(emptyList())
            } ?: emptyList()
        }

        val buckets = (searchBuckets.map { it.await() } + listOf(radioBucket.await()))
            .filter { it.isNotEmpty() }

        val mixed = interleave(buckets).distinctBy { it.id }
            .filterNot { isNarrowedSong(it) }
            .take(limit)
        if (mixed.isNotEmpty()) mixed
        else youTubeRepository.search("trending music", YouTubeRepository.FILTER_SONGS)
            .withoutNarrowedArtists()
    }

    /**
     * Songs the user has not heard, chosen from what they actually listen to.
     *
     * This is a different question from [getHomeRecommendations], which is
     * "what should this feed look like": that one happily returns the user's
     * own favourites back to them, and for a home feed that is correct.
     * Discovery is only worth the name if what comes back is *new*, so
     * [excludeIds] carries everything the caller already knows about - the
     * device library, likes, downloads, play history - and anything matching
     * is dropped rather than ranked lower.
     *
     * **YouTube's own recommendations skew to what you have already played**,
     * which is why this is not simply a browse call. Three seeds pull in
     * different directions and are interleaved so none of them dominates:
     *
     * - **"latest songs" searches on the top artists** are the half that
     *   actually surfaces *recent* releases. Relevance ranking does the work;
     *   there is no probed InnerTube browse id for new music in this codebase
     *   and inventing one would be writing a request from memory.
     * - **Related-song radios seeded from favourites** reach sideways into
     *   adjacent artists, which is where anything genuinely unfamiliar comes
     *   from - an artist search can only ever return that artist.
     * - **Recent searches** because something typed last week is a taste
     *   signal the play history has not caught up with yet.
     *
     * With no profile at all there is nothing to be personal about, so it asks
     * for new music generally rather than returning nothing.
     */
    suspend fun getDiscoveryRecommendations(
        excludeIds: Set<String> = emptySet(),
        limit: Int = 40
    ): List<Song> = coroutineScope {
        val profile = buildTasteProfile()
        if (profile.isEmpty()) {
            return@coroutineScope youTubeRepository
                .search("new music this week", YouTubeRepository.FILTER_SONGS)
                .filter { it.id !in excludeIds }
                .distinctBy { it.id }
                .filterNot { isNarrowedSong(it) }
                .take(limit)
        }

        // Shuffled rather than always the same three: a refresh that returns
        // the identical list is a refresh that appears not to have run.
        val artistSeeds = profile.topArtists.shuffled().take(3)
        val searchBuckets = artistSeeds.map { artist ->
            async {
                runCatching {
                    youTubeRepository.search("$artist latest songs", YouTubeRepository.FILTER_SONGS)
                }.getOrDefault(emptyList())
            }
        }
        val searchHistoryBucket = profile.recentSearches.take(2).map { query ->
            async {
                runCatching { youTubeRepository.search(query, YouTubeRepository.FILTER_SONGS) }
                    .getOrDefault(emptyList())
            }
        }
        val radioBuckets = profile.topSongs.take(2).map { seed ->
            async {
                runCatching { youTubeRepository.getRelatedSongs(seed.songId) }
                    .getOrDefault(emptyList())
            }
        }

        val genreBuckets = profile.genres.shuffled().take(1).map { genre ->
            async {
                runCatching {
                    youTubeRepository.search("new $genre songs", YouTubeRepository.FILTER_SONGS)
                }.getOrDefault(emptyList())
            }
        }

        val buckets = (searchBuckets + searchHistoryBucket + radioBuckets + genreBuckets)
            .map { it.await() }
            // The narrowing is applied per bucket, before the empties are
            // dropped, so a shelf made entirely of narrowed artists costs its
            // own bucket and does not come back as a blank column.
            .map { bucket -> bucket.filter { it.id !in excludeIds && !isNarrowedSong(it) } }
            .filter { it.isNotEmpty() }

        interleave(buckets).distinctBy { it.id }.take(limit)
    }

    /**
     * Albums, playlists and artists to discover, for the shelves under the
     * songs on Spotlight's "For you" tab.
     *
     * A song grid answers "what should I play next" and nothing else: it gives
     * no way to fall into a record, a curated hour, or somebody's whole
     * catalogue, which is most of how people actually find music. These three
     * shelves are that, built from the same taste profile the song half uses.
     *
     * Every query is an ordinary search, deliberately. There is no probed
     * browse id for "new music" in this codebase, and inventing one is writing
     * a request from memory - so this reaches for the endpoint that is already
     * verified rather than the one that would be tidier.
     *
     * Each shelf is independent and each is wrapped in its own runCatching: one
     * refused search costs its own shelf and not the other two, which is what
     * keeps a rate-limited moment from emptying the whole tab.
     */
    suspend fun getDiscoveryCollections(
        excludeCollectionIds: Set<String> = emptySet(),
        excludeArtistNames: Set<String> = emptySet(),
        limitPerShelf: Int = 12
    ): DiscoveryCollections = coroutineScope {
        val profile = buildTasteProfile()
        // Shuffled for the reason the song half shuffles its seeds: a refresh
        // returning the identical three shelves is a refresh that looks like it
        // did not run.
        val seeds = profile.topArtists.shuffled().take(2)
        val querySeeds = if (seeds.isNotEmpty()) seeds else profile.recentSearches.take(1)

        val albumJobs = if (querySeeds.isEmpty()) {
            listOf(async { searchOrEmpty { youTubeRepository.searchAlbums(COLD_START_ALBUMS) } })
        } else {
            querySeeds.map { seed ->
                async { searchOrEmpty { youTubeRepository.searchAlbums("$seed album") } }
            }
        }
        val playlistJobs = if (querySeeds.isEmpty()) {
            listOf(async { searchOrEmpty { youTubeRepository.searchPlaylists(COLD_START_PLAYLISTS) } })
        } else {
            querySeeds.map { seed ->
                async { searchOrEmpty { youTubeRepository.searchPlaylists("$seed playlist") } }
            }
        }
        val artistJobs = if (querySeeds.isEmpty()) {
            listOf(async { searchArtistsOrEmpty(COLD_START_ARTISTS) })
        } else {
            querySeeds.map { seed -> async { searchArtistsOrEmpty(seed) } }
        }

        val excludedNames = (excludeArtistNames + tasteProfileStore.current().excludedArtists)
            .mapNotNull { name -> name.trim().lowercase().takeIf { it.isNotBlank() } }
            .toSet()

        DiscoveryCollections(
            albums = mergeCollections(albumJobs.map { it.await() }, excludeCollectionIds, limitPerShelf),
            playlists = mergeCollections(
                playlistJobs.map { it.await() },
                excludeCollectionIds,
                limitPerShelf
            ),
            artists = interleaveBy(artistJobs.map { it.await() }) { it.id }
                .filter { it.name.isNotBlank() && it.name.lowercase() !in excludedNames }
                .take(limitPerShelf)
        )
    }

    /**
     * Whether [song] belongs to an artist the user has narrowed out.
     *
     * A song with no artist name cannot be matched, so it is kept: the
     * narrowing means "not this artist", not "only artists I can name".
     */
    private fun isNarrowedSong(song: Song): Boolean {
        val artist = song.artist.trim().lowercase()
        if (artist.isBlank()) return false
        return tasteProfileStore.current().excludedArtists.any { it.trim().lowercase() == artist }
    }

    /**
     * [buildTasteProfile] refuses to seed from a narrowed artist, which covers
     * everything built from the profile. This covers the other half: what comes
     * back *from* YouTube, where a related-song radio seeded by a favourite
     * returns the neighbouring artists and one of those may be a narrowed one.
     * The narrowing is a request about content, so it holds wherever the
     * content came from, not only where this app picked the seed.
     */
    private fun List<Song>.withoutNarrowedArtists(): List<Song> =
        if (tasteProfileStore.current().excludedArtists.isEmpty()) this
        else filterNot { isNarrowedSong(it) }

    private suspend fun searchOrEmpty(
        block: suspend () -> List<PlaylistDisplayItem>
    ): List<PlaylistDisplayItem> = runCatching { block() }.getOrDefault(emptyList())

    private suspend fun searchArtistsOrEmpty(query: String): List<ArtistItem> =
        runCatching { youTubeRepository.searchArtists(query) }.getOrDefault(emptyList())

    private fun mergeCollections(
        buckets: List<List<PlaylistDisplayItem>>,
        excludeIds: Set<String>,
        limit: Int
    ): List<PlaylistDisplayItem> =
        interleaveBy(buckets) { it.id }
            .filter { it.name.isNotBlank() && it.id.isNotBlank() && it.id !in excludeIds }
            .take(limit)

    /** [interleave] for anything with an identity, so one seed cannot own a shelf. */
    private fun <T> interleaveBy(buckets: List<List<T>>, id: (T) -> String): List<T> {
        val out = mutableListOf<T>()
        val seen = HashSet<String>()
        var i = 0
        var added = true
        while (added) {
            added = false
            for (bucket in buckets) {
                if (i < bucket.size) {
                    added = true
                    val item = bucket[i]
                    if (seen.add(id(item))) out.add(item)
                }
            }
            i++
        }
        return out
    }

    /** Two ranked lists taken in turn, first from [first]; whichever runs out stops contributing. */
    private fun alternate(first: List<String>, second: List<String>): List<String> = buildList {
        for (index in 0 until maxOf(first.size, second.size)) {
            first.getOrNull(index)?.let(::add)
            second.getOrNull(index)?.let(::add)
        }
    }

    /** Round-robin merge so one seed doesn't dominate the top of the feed. */
    private fun interleave(buckets: List<List<Song>>): List<Song> {
        val out = mutableListOf<Song>()
        var i = 0
        var added = true
        while (added) {
            added = false
            for (bucket in buckets) {
                if (i < bucket.size) {
                    out.add(bucket[i])
                    added = true
                }
            }
            i++
        }
        return out
    }
}
