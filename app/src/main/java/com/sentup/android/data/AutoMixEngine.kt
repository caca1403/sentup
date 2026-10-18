package com.sentup.android.data

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import kotlin.math.max

/**
 * The automatic mixes: lists built from what has actually been played.
 *
 * **Why this is a list generator and not a recommendation feed.** Every
 * "for you" surface in a music app answers the same question - what should I
 * hear next - and answers it with an endless feed of songs the user has never
 * heard of, which cannot be re-read, paused, reordered or taken offline. A mix
 * is a *thing*: it has a name, a reason for existing, a fixed size and an end.
 * Once you have heard it you can hear it again, and it looks the same both
 * times. That is the difference this app cares about, and it is why these are
 * built here from local history instead of fetched.
 *
 * **Everything is derived, nothing is stored.** Each mix is a pure function of
 * the play history at the moment it is opened, so the same history yields the
 * same mix and a mix can never go stale or contradict the stats screen. The
 * cost is that a mix is only as good as the history behind it, which is why
 * every one of them says what it is made of instead of pretending to be a
 * mood.
 *
 * **Music and video are the same problem.** The rules below run over songs and
 * over videos with the same arithmetic - recency decay, rediscovery, novelty -
 * because a watch is a play. A person who watches three cooking videos on a
 * Sunday wants the same treatment as a person who plays three cooking playlists.
 */
class AutoMixEngine(context: Context) {

    private val appContext = context.applicationContext
    private val statsRepository = StatsRepository(appContext)

    /**
     * One generated list.
     *
     * @param reason why this list exists, in words, shown under the title. A
     *   generated list whose reason is hidden is indistinguishable from a
     *   random one, and then it does not deserve the name.
     */
    data class AutoMix(
        val id: String,
        val title: String,
        val reason: String,
        val songIds: List<String>,
        /** Watch ids, for the same rules run over the watch history. Empty on a music mix, which is what [size] counts both halves of. */
        val videoIds: List<String> = emptyList(),
        val seedArtist: String? = null
    ) {
        val size: Int get() = songIds.size + videoIds.size
        val isEmpty: Boolean get() = size == 0
    }

    /**
     * Every mix the current history supports, best first.
     *
     * Empty ones are left out rather than shown disabled: a page of "not
     * enough listening yet" teaches nothing and a mix that cannot be built
     * does not exist.
     */
    suspend fun buildMixes(): List<AutoMix> = withContext(Dispatchers.IO) {
        val history = statsRepository.loadHistory()
        val now = System.currentTimeMillis()
        val songs = groupBySong(history)
        val videos = videoRuns()

        // The music half is exactly what it was, including the rule that no
        // listening history means no music mixes. Video history is a separate
        // store: somebody who only watches gets the video lists and nothing
        // invented from the music rules, and somebody who only listens is
        // untouched by any of this.
        val music = if (history.isEmpty()) emptyList() else listOfNotNull(
            rediscover(songs, now),
            nightDrive(songs, now),
            weeklyRotation(songs, now),
            oneAndWonders(songs),
            slowAndLong(songs),
            backInRotation(history, now)
        )

        (music + videoMixes(videos, now)).filterNot { it.isEmpty }
    }

    // ---------------------------------------------------------------- mixes

    /**
     * Played long ago, not since.
     *
     * The mix people ask for without knowing they are asking for it: the songs
     * that were on repeat in spring and have not appeared since, because the
     * rotation drifted and nothing ever brings them back. Not a "similar to"
     * list - the songs themselves.
     */
    private fun rediscover(songs: List<SongRun>, now: Long): AutoMix {
        val forgotten = songs
            .filter { it.firstPlayedAt > 0 && daysSince(it.firstPlayedAt, now) >= FORGET_DAYS }
            .filter { daysSince(it.lastPlayedAt, now) >= RETURN_DAYS }
            .sortedByDescending { it.firstPlayedAt }
            .take(MIX_SIZE)
        return AutoMix(
            id = "rediscover",
            title = "Yeniden keşfet",
            reason = "${forgotten.size} şarkı uzun süredir çalmıyordu",
            songIds = forgotten.map { it.songId }
        )
    }

    /**
     * What gets played late.
     *
     * Read off the clock rather than off the mood: every play carries a
     * timestamp, and the hours between 22 and 04 are a person's own answer to
     * "what do I listen to at night". Excludes anything played in the last two
     * days, so it stays a discovery rather than a rerun of this morning.
     */
    private fun nightDrive(songs: List<SongRun>, now: Long): AutoMix {
        val night = songs
            .filter { it.hourOfDay in NIGHT_HOURS }
            .filter { daysSince(it.lastPlayedAt, now) >= 2 }
            .sortedByDescending { it.nightPlays }
            .take(MIX_SIZE)
        return AutoMix(
            id = "night",
            title = "Gece",
            reason = "Saat 22:00 - 04:00 arasında çalışanlar",
            songIds = night.map { it.songId }
        )
    }

    /**
     * This week's rotation, by how often it actually came up.
     *
     * The one mix that is true the moment it is opened and stops being true a
     * week later, which is the point: it is the answer to "what have I been
     * playing" without opening the stats screen to answer it.
     */
    private fun weeklyRotation(songs: List<SongRun>, now: Long): AutoMix {
        val week = songs
            .filter { daysSince(it.lastPlayedAt, now) <= 7 }
            .sortedByDescending { it.playsInLast7Days }
            .take(MIX_SIZE)
        return AutoMix(
            id = "this_week",
            title = "Bu hafta",
            reason = "Son 7 günde ${week.sumOf { it.playsInLast7Days }} dinleme",
            songIds = week.map { it.songId }
        )
    }

    /**
     * Artists heard exactly once.
     *
     * A play count of one is the interesting number: it is either a fluke or the
     * first time this person has ever heard the artist, and there is no way to
     * tell those apart from history alone. Both are worth hearing again, and
     * both are invisible to every algorithm that ranks by volume.
     */
    private fun oneAndWonders(songs: List<SongRun>): AutoMix {
        val once = songs.filter { it.plays == 1L }.sortedBy { it.lastPlayedAt }.take(MIX_SIZE)
        val artist = once.firstOrNull()?.artist
        return AutoMix(
            id = "one_and_wonders",
            title = "Bir kez dinlediklerin",
            reason = once.size.takeIf { it > 0 }
                ?.let { "$it sanatçıyı tam bir kez dinledin" }
                ?: "Henüz tek dinleme yok",
            songIds = once.map { it.songId },
            seedArtist = artist
        )
    }

    /**
     * The long ones.
     *
     * Anything over five minutes, most recently played first. A mix made of
     * long songs is the nearest thing this app has to a focus list, and it is
     * built from a number already in the data rather than from a guess about
     * attention spans.
     */
    private fun slowAndLong(songs: List<SongRun>): AutoMix {
        val long = songs
            .filter { it.durationMs >= LONG_MIN_MS }
            .sortedByDescending { it.lastPlayedAt }
            .take(MIX_SIZE)
        return AutoMix(
            id = "long_ones",
            title = "Uzunlar",
            reason = "5 dakikayı geçen ${long.size} parça",
            songIds = long.map { it.songId }
        )
    }

    /**
     * Liked, and not played lately.
     *
     * Built from the liked-songs store rather than the history, because a like
     * is an explicit statement and a play is only a vote. The gap between the
     * two - liked but unheard for weeks - is a queue somebody meant to get to.
     */
    private suspend fun backInRotation(
        history: List<PlayHistoryEntry>,
        now: Long
    ): AutoMix? {
        val liked = LikedSongsRepository(appContext).getAllLikedSongIds()
        if (liked.isEmpty()) return null
        val lastPlayed = history.groupBy { it.songId }
            .mapValues { (_, entries) -> entries.maxOf { it.timestamp } }
        val waiting = liked
            .filter { id -> (lastPlayed[id] ?: 0L).let { daysSince(it, now) >= RETURN_DAYS } }
            .take(MIX_SIZE)
        if (waiting.isEmpty()) return null
        return AutoMix(
            id = "liked_waiting",
            title = "Bekleyen beğeniler",
            reason = "${waiting.size} beğendiğin şarkı sırasını bekliyor",
            songIds = waiting
        )
    }

    // ---------------------------------------------------------------- video
    //
    // The same five questions asked of the watch history instead of the play
    // history. A watch is a play, so the windows are shared with the music
    // rules rather than reinvented: [FORGET_DAYS], [RETURN_DAYS] and
    // [MIX_SIZE] are the same numbers, and only what the video store can
    // actually say is asked of it.
    //
    // **What that store is** ([VideoHistoryRepository]): a newest-first list,
    // capped at 100 rows, holding one row per video - a rewatch lifts the
    // existing row to the top instead of adding a second one. So there is no
    // play count to read, and the two numbers a row does carry are how far the
    // watch got (`watchedProgress`, a fraction, null when it was never written)
    // and when progress was last saved (`watchProgressUpdatedAtMs`, 0 on a row
    // that never reached a progress write). The rules below are written against
    // exactly that, which is why "watched once" is read as "started and left
    // part-way" and why a row with no time on it is left to the untimed rules
    // rather than guessed a date.

    /**
     * The five video lists, best first.
     *
     * Each one is a wrapper over a pure rule in the companion object, where the
     * choosing happens and where the tests look; this only supplies the name
     * and the reason, so a rule cannot be changed by the code that labels it.
     */
    private fun videoMixes(videos: List<VideoRun>, now: Long): List<AutoMix> {
        if (videos.isEmpty()) return emptyList()
        val forgotten = videoRediscover(videos, now)
        val week = videoThisWeek(videos, now)
        val abandoned = videoOneAndDone(videos)
        val shorts = videoShortRuns(videos)
        val deep = videoDeepDives(videos)
        return listOf(
            AutoMix(
                id = "video_rediscover",
                title = "Yeniden izle",
                reason = "${forgotten.size} video uzun süredir açılmadı",
                songIds = emptyList(),
                videoIds = forgotten,
                seedArtist = loudestChannel(videos, forgotten)
            ),
            AutoMix(
                id = "video_week",
                title = "Bu hafta izlediklerin",
                reason = "Son 7 günde ${week.size} video",
                songIds = emptyList(),
                videoIds = week,
                seedArtist = loudestChannel(videos, week)
            ),
            AutoMix(
                id = "video_abandoned",
                title = "Yarım bıraktıkların",
                reason = "${abandoned.size} videoyu bir kez açıp yarım bıraktın",
                songIds = emptyList(),
                videoIds = abandoned,
                seedArtist = loudestChannel(videos, abandoned)
            ),
            AutoMix(
                id = "video_short",
                title = "Kısa rotalar",
                reason = "3 dakikanın altında ${shorts.size} video",
                songIds = emptyList(),
                videoIds = shorts,
                seedArtist = loudestChannel(videos, shorts)
            ),
            AutoMix(
                id = "video_deep",
                title = "Uzunlar",
                reason = "8 dakikayı geçen ${deep.size} video",
                songIds = emptyList(),
                videoIds = deep,
                seedArtist = loudestChannel(videos, deep)
            )
        )
    }

    /**
     * The watch history in the shape the rules read.
     *
     * The list arrives newest-first and keeps that order, so a row's position
     * is the store's own statement of recency and is carried through as
     * [VideoRun.index] rather than re-derived.
     */
    private fun videoRuns(): List<VideoRun> =
        VideoHistoryRepository(appContext).getHistory().mapIndexed { index, video ->
            VideoRun(
                videoId = video.videoId,
                channelName = video.channelName,
                durationSec = video.duration,
                watchedProgress = video.watchedProgress,
                lastWatchedAt = video.watchProgressUpdatedAtMs,
                index = index
            )
        }

    /** The channel [ids] lean to, so a mix can seed a channel radio. */
    private fun loudestChannel(videos: List<VideoRun>, ids: List<String>): String? {
        if (ids.isEmpty()) return null
        val chosen = ids.toSet()
        return videos.filter { it.videoId in chosen && it.channelName.isNotBlank() }
            .groupingBy { it.channelName }
            .eachCount()
            .maxByOrNull { it.value }
            ?.key
    }

    // ------------------------------------------------------------ internals

    /** Every play of one song, collapsed into the numbers the rules need. */
    internal data class SongRun(
        val songId: String,
        val artist: String,
        val plays: Long,
        val firstPlayedAt: Long,
        val lastPlayedAt: Long,
        val durationMs: Long,
        /** 0-23 in the device's own time zone, which is where "night" comes from. */
        val hourOfDay: Int,
        val nightPlays: Long,
        val playsInLast7Days: Long,
    )

    /**
     * Every watch of one video, collapsed into what the video rules can use.
     *
     * The sibling of [SongRun] and just as boring on purpose: plain values,
     * behaviour nowhere in it, so the rules that read it are functions of its
     * contents alone and can be run by a test with no Android anywhere near.
     *
     * @param durationSec the clip's length in **seconds** - the video store
     *   counts in seconds where [SongRun.durationMs] counts in milliseconds.
     *   Mixing the two up is the one mistake this shape exists to prevent, so
     *   it is in the name.
     * @param watchedProgress how far the last watch got, 0f..1f, null when the
     *   row never recorded progress.
     * @param lastWatchedAt when progress was last saved, epoch millis; 0 when
     *   the row carries no time at all. A rule that needs a date asks for
     *   `lastWatchedAt > 0` instead of treating 0 as "a very long time ago".
     * @param index position in the newest-first watch list, 0 for the most
     *   recent; only ever used to order a list, never to decide membership.
     */
    internal data class VideoRun(
        val videoId: String,
        val channelName: String,
        val durationSec: Long,
        val watchedProgress: Float?,
        val lastWatchedAt: Long,
        val index: Int,
    )

    private fun groupBySong(history: List<PlayHistoryEntry>): List<SongRun> {
        val now = System.currentTimeMillis()
        val weekAgo = now - 7L * DAY_MS
        return history.groupBy { it.songId }
            .map { (id, entries) ->
                val times = entries.map { it.timestamp }
                val nightCount = entries.count { hourOfDay(it.timestamp) in NIGHT_HOURS }
                SongRun(
                    songId = id,
                    artist = entries.first().artist,
                    plays = entries.size.toLong(),
                    firstPlayedAt = times.min(),
                    lastPlayedAt = times.max(),
                    durationMs = entries.maxOf { it.duration },
                    hourOfDay = hourOfDay(times.max()),
                    nightPlays = nightCount.toLong(),
                    playsInLast7Days = entries.count { it.timestamp >= weekAgo }.toLong(),
                )
            }
            .sortedByDescending { it.lastPlayedAt }
    }

    internal companion object {
        const val DAY_MS = 24L * 60 * 60 * 1000
        const val MIX_SIZE = 25

        /** A song unseen for this long counts as forgotten. */
        const val FORGET_DAYS = 30

        /** ...and needs this many days away before it can come back. */
        const val RETURN_DAYS = 14

        const val LONG_MIN_MS = 5L * 60 * 1000

        /** 22:00 through 03:59. Two ranges, because the night wraps midnight. */
        val NIGHT_HOURS: Set<Int> = ((22..23) + (0..3)).toSet()

        fun daysSince(timestamp: Long, now: Long): Double =
            max(0.0, (now - timestamp).toDouble() / DAY_MS)

        fun hourOfDay(timestamp: Long): Int =
            Calendar.getInstance().apply { timeInMillis = timestamp }.get(Calendar.HOUR_OF_DAY)

        // -------------------------------------------------------- video rules
        //
        // The five rules over the watch history, pure in the same sense the
        // rules over SongRun are: each takes the collapsed list and `now` and
        // returns ids in order, with no store, no Context and no clock beyond
        // the parameter. That is what lets AutoMixRulesTest run them, and it is
        // what keeps a rule from quietly deciding something the caller cannot
        // see it decide.
        //
        // RETURN_DAYS has no video analogue and is deliberately not used: the
        // music rule needs it because a song has a first play and a last play
        // weeks apart, while a watch is one row per video, so the single gate -
        // how long since it was watched - is FORGET_DAYS.

        /** A clip of under three minutes is one idea and then over. */
        const val SHORT_MAX_SEC = 3L * 60

        /** ...and past eight minutes is a sit-down, not something to skip through. */
        const val LONG_MIN_SEC = 8L * 60

        /** Below this a watch was a mis-tap; above that it was heard and needs nothing. */
        const val ABANDONED_MIN_PROGRESS = 0.05f
        const val ABANDONED_MAX_PROGRESS = 0.85f

        /**
         * Watched long ago and not opened since - the music [FORGET_DAYS]
         * window over the watch list, oldest watch first.
         *
         * A row that carries no time at all is left out rather than assumed
         * old: the store's order says what has been watched *after* it, which
         * is not the same statement as how long ago it was.
         */
        internal fun videoRediscover(videos: List<VideoRun>, now: Long): List<String> =
            videos.filter { it.lastWatchedAt > 0L && daysSince(it.lastWatchedAt, now) >= FORGET_DAYS }
                .sortedBy { it.lastWatchedAt }
                .take(MIX_SIZE)
                .map { it.videoId }

        /**
         * Watched inside the last seven days, newest watch first.
         *
         * Counted rather than ranked, because there is no play count to rank
         * by: this is the list of what the last week actually was, and its size
         * is the number the mix's reason gives.
         */
        internal fun videoThisWeek(videos: List<VideoRun>, now: Long): List<String> =
            videos.filter { it.lastWatchedAt > 0L && daysSince(it.lastWatchedAt, now) <= 7.0 }
                .sortedWith(compareByDescending<VideoRun> { it.lastWatchedAt }.thenBy { it.index })
                .take(MIX_SIZE)
                .map { it.videoId }

        /**
         * Opened once and left part-way: the video's one-and-wonders.
         *
         * "Watched once" cannot be read as a count here, because a rewatch
         * lifts the existing row instead of adding a second one. What the row
         * does say is how far the watch got, and a video left between 5% and
         * 85% of its length is the same human situation as a song played
         * exactly once - one try, and no decision made about it since. Finished
         * rows were heard; rows under 5% were a mis-tap. Most recent first.
         */
        internal fun videoOneAndDone(videos: List<VideoRun>): List<String> =
            videos.filter { isAbandonedWatch(it) }
                .sortedBy { it.index }
                .take(MIX_SIZE)
                .map { it.videoId }

        /** The short ones, under three minutes, most recently watched first. */
        internal fun videoShortRuns(videos: List<VideoRun>): List<String> =
            videos.filter { it.durationSec in 1 until SHORT_MAX_SEC }
                .sortedBy { it.index }
                .take(MIX_SIZE)
                .map { it.videoId }

        /** The deep ones, past eight minutes, longest first. */
        internal fun videoDeepDives(videos: List<VideoRun>): List<String> =
            videos.filter { it.durationSec > LONG_MIN_SEC }
                .sortedWith(compareByDescending<VideoRun> { it.durationSec }.thenBy { it.index })
                .take(MIX_SIZE)
                .map { it.videoId }

        /** Enough of a watch to count as a try, and not enough to have heard it. */
        private fun isAbandonedWatch(video: VideoRun): Boolean {
            val progress = video.watchedProgress ?: return false
            return progress >= ABANDONED_MIN_PROGRESS && progress <= ABANDONED_MAX_PROGRESS
        }
    }
}