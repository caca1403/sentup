package com.sentup.android.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import com.sentup.android.ui.library.InsightsCard
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Category
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.sentup.android.R
import com.sentup.android.ui.library.listNameLabel
import com.sentup.android.data.PlaylistDisplayItem
import com.sentup.android.data.Song
import com.sentup.android.data.isUnknownArtist
import com.sentup.android.ui.components.ExpressivePullToRefresh
import com.sentup.android.ui.components.ModeMorphButton
import com.sentup.android.ui.components.SkeletonBox
import com.sentup.android.ui.components.SkeletonTextLine
import com.sentup.android.ui.components.TopBarDownloadsButton
import com.sentup.android.ui.components.rememberHomeTopBarOptions
import com.sentup.android.ui.components.rememberSkeletonAlpha
import com.sentup.android.ui.theme.QuickPaletteButton
import com.sentup.android.ui.theme.SentupChevron
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupGroupCard
import com.sentup.android.ui.theme.SentupGroupHeader
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.SentupRow
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage
import com.sentup.android.ui.theme.sentupSpecularEdge

// YouTube Music's moods & genres page; doubles as the musicBrowse state slot key.
private const val BROWSE_MOODS_PAGE = "FEmusic_moods_and_genres"

/**
 * The music Home, rebuilt as a page of floating objects.
 *
 * Everything here is the same screen the old classic Home was - the mix, the
 * recommendations, the history - restated in the app's own surface language:
 *
 * - the hour's greeting as the page's large title, with the account and
 *   settings buttons beside it as circles that float on their own shadow;
 * - one hero for the mix: cover art cropped into a panel that catches light
 *   along its edge, the copy read through a scrim rather than beside it;
 * - every collection as a shelf: an iOS inset-group header, then tiles that
 *   are cards (shadow, hairline, continuous corners), or a single inset group
 *   card of rows for things you open rather than watch (playlists);
 * - a single feature - the one thing picked for you tonight - in place of the
 *   collage of loose circles that used to sit here, which drew the eye
 *   nowhere and touched nothing.
 *
 * Touch feedback is a press inward - 0.97 for 90ms with a trace less alpha -
 * never a colour flash: the same reading as [com.sentup.android.ui.theme.SentupRow]
 * at tile scale.
 *
 * It owns no data. Everything arrives through [SentupHomeFeed] from the Home
 * shell, which keeps owning tabs, the player sheet and the video side.
 */

/** The page caps itself on very wide windows; padding, not width, so fling works. */
private val FEED_PAGE_MAX_WIDTH = 940.dp

/** The hero panel's height: tall enough for cover, scrim and a control row. */
private val FEED_HERO_HEIGHT = 256.dp

/** Cover edge inside a rail tile; the card wraps it with its own padding. */
private val FEED_TILE_COVER = 126.dp

/** The feature's cover: bigger than a rail tile, smaller than the hero. */
private val FEED_FEATURE_COVER = 138.dp

/** Artist photos are circles, and circles read as people, not releases. */
private val FEED_ARTIST_PHOTO = 112.dp

/**
 * Rail content padding. The start inset matches [SentupGroupHeader]'s text
 * inset so a shelf's first tile begins exactly under the header's first word;
 * the end keeps only the page inset so tiles can scroll almost to the edge.
 */
private val FEED_RAIL_START = SentupGroup.inset + SentupGroup.dividerInset

@Composable
internal fun SentupHomeFeed(
    songs: List<Song>,
    /** First load with nothing yet: data-backed shelves stand in as skeletons. */
    isInitialLoading: Boolean,
    /** Local play history, for the "jump back in" rail. */
    recentlyPlayed: List<Song>,
    onRecentClick: (Song) -> Unit,
    /** Albums behind the play history, newest first. */
    recentAlbums: List<RecentAlbum>,
    onRecentAlbumClick: (RecentAlbum) -> Unit,
    /** The merged local, saved and account playlists. */
    playlists: List<PlaylistDisplayItem>,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    /** Liked songs, leading the playlist group; 0 hides the row. */
    likedCount: Int,
    onLikedClick: () -> Unit,
    topArtists: List<TopArtist>,
    artistPhotos: Map<String, String>,
    onArtistClick: (TopArtist) -> Unit,
    /** Whether to lead with [offlineSongs]; the shelf exists only offline. */
    isOffline: Boolean,
    /** Downloads, then songs cached in full, newest first. */
    offlineSongs: List<Song>,
    onOfflineSongClick: (Song) -> Unit,
    onShowAllInLibrary: () -> Unit,
    onSongClick: (Song) -> Unit,
    onSongLongPress: ((Song) -> Unit)? = null,
    onPlayClick: () -> Unit,
    /** Opens Spin, the wheel over this Home. */
    onSpinClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDownloadsClick: () -> Unit = {},
    contentPadding: PaddingValues,
    viewModel: HomeViewModel,
    excludedFolders: Set<String> = emptySet(),
    manualScan: Boolean = false,
    videoMode: Boolean = false,
    onVideoModeToggle: (Boolean) -> Unit = {},
    showModeToggle: Boolean = true,
    /** Hoisted by HomeScreen: survives tab switches, reachable by the nav bar. */
    listState: LazyListState = rememberLazyListState(),
    /**
     * The music player, where the shell has one. Given, the feed can resume
     * the current song, start radios and shuffles, and answer a tile's long
     * press with the play/queue/like popout; left null, every one of those
     * affordances hides itself rather than offering a dead tap. HomeScreen
     * already owns the instance and can pass it through.
     */
    playerViewModel: com.sentup.android.ui.player.PlayerViewModel? = null,
    /** Opens the listening statistics page; the weekly strip's destination. */
    onStatsClick: () -> Unit = {}
) {
    val isRefreshing by viewModel.isLoading.collectAsState()
    val context = LocalContext.current

    // The feed's own appearance switches, read from the same snapshot the
    // top bar reads, so a change in Settings lands here without a restart.
    val homeOptions = rememberHomeTopBarOptions()

    // What the user made or saved, and the generated mixes, each to its own
    // shelf: the group card holds the chosen ones, a rail the churny ones.
    val (mixes, ownPlaylists) = remember(playlists) { playlists.partition { isAutoMix(it.name) } }

    // One shared pulse while placeholders are actually on screen; reading a
    // running animation behind loaded content costs a recomposition per frame.
    val skeletonAlpha = if (isInitialLoading) rememberSkeletonAlpha() else 1f

    // The long-press popout over a song tile, and the artist's own options
    // list over an artist tile. One open at a time, by construction: opening
    // either clears the other.
    val popoutSong = remember { mutableStateOf<Song?>(null) }
    val artistOptions = remember { mutableStateOf<TopArtist?>(null) }
    var popoutLikedTick by remember { mutableIntStateOf(0) }

    val openSongPreview: (Song) -> Unit = { song ->
        if (playerViewModel != null) {
            artistOptions.value = null
            popoutSong.value = song
        } else {
            onSongLongPress?.invoke(song)
        }
    }

    // The player's live state, collected once here so the resume hero and the
    // capsules read one snapshot per frame instead of each collecting their own.
    val currentSong = playerViewModel?.currentSong?.collectAsState()?.value
    val isPlayingNow = playerViewModel?.isPlaying?.collectAsState()?.value == true
    val progressNow = playerViewModel?.progress?.collectAsState()?.value ?: 0L
    val durationNow = playerViewModel?.duration?.collectAsState()?.value ?: 0L
    val likedSongs = viewModel.likedSongs.collectAsState().value
    val recentlyPlayedState = viewModel.recentlyPlayed.collectAsState().value
    val radioSeed = recentlyPlayedState.firstOrNull {
        it.source == com.sentup.android.data.SongSource.YOUTUBE
    } ?: songs.firstOrNull { it.source == com.sentup.android.data.SongSource.YOUTUBE }

    // The account's own mixes, from the same signed-in home page Spotlight
    // reads. loadMusicBrowse caches internally and no-ops offline or signed
    // out, so asking unconditionally is the cheap correct call.
    val musicBrowse by viewModel.musicBrowse.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadMusicBrowse(BROWSE_HOME) }
    LaunchedEffect(Unit) { viewModel.loadMusicBrowse(BROWSE_NEW) }
    LaunchedEffect(Unit) { viewModel.loadMusicBrowse(BROWSE_CHARTS) }
    // YouTube Music's own moods/genres page; the key doubles as the state slot.
    LaunchedEffect(Unit) { viewModel.loadMusicBrowse(BROWSE_MOODS_PAGE) }
    LaunchedEffect(Unit) { viewModel.loadDiscovery() }
    val discoverySongs by viewModel.discoverySongs.collectAsState()
    val moodTiles = remember(musicBrowse) {
        musicBrowse[BROWSE_MOODS_PAGE]?.shelves.orEmpty().flatMap { it.items }
            .filterIsInstance<com.sentup.android.data.MusicShelfItem.Mood>()
            .distinctBy { it.title }
            .take(10)
    }

    // Home personalisation: which lists the user pinned to the front, kept in
    // its own prefs file so it survives (and does not touch) theme storage.
    var showMixMaker by remember { mutableStateOf(false) }
    val pinPrefs = remember(context) {
        context.getSharedPreferences("sentup_home_pins", android.content.Context.MODE_PRIVATE)
    }
    var pinnedIds by remember {
        mutableStateOf(pinPrefs.getString("pinned", "").orEmpty().split(",").filter { it.isNotBlank() })
    }
    val togglePin: (String) -> Unit = { id ->
        val next = if (id in pinnedIds) pinnedIds - id else (pinnedIds + id)
        pinnedIds = next
        pinPrefs.edit().putString("pinned", next.joinToString(",")).apply()
    }
    val serverMixes = remember(musicBrowse) {
        musicBrowse[BROWSE_HOME]?.shelves.orEmpty()
            .filter { shelf ->
                val t = shelf.title.lowercase()
                t.contains("karışım") || t.contains("mix") || t.contains("radyo") || t.contains("radio")
            }
            .flatMap { it.items }
            .filterIsInstance<com.sentup.android.data.MusicShelfItem.Collection>()
            .filter { it.playlist.id.startsWith("RD") || it.playlist.id.startsWith("RL") }
            .map { it.playlist }
            .distinctBy { it.id }
            .take(8)
    }

    // The locally generated mixes, built once for the shelf. The engine reads
    // only play history; nothing here writes.
    val generatedMixes = remember { mutableStateOf<List<com.sentup.android.data.AutoMixEngine.AutoMix>?>(null) }
    LaunchedEffect(Unit) {
        generatedMixes.value = com.sentup.android.data.AutoMixEngine(context)
            .buildMixes()
            .filter { it.songIds.isNotEmpty() }
            .take(4)
    }

    // "Bu hafta: 4 sa 12 dk · Sezen Aksu". Weekly here means since Monday's
    // midnight, which is what "this week" says in a person's own calendar;
    // history is read once on entry, through the same store Stats uses.
    val weekSummary = remember { mutableStateOf<Pair<Long, String?>?>(null) }
    // The same read also backs the smart mixes and the time capsule, so history
    // is loaded once and shared rather than fetched again per feature.
    val historySnapshot = remember {
        mutableStateOf<List<com.sentup.android.data.PlayHistoryEntry>>(emptyList())
    }
    LaunchedEffect(Unit) {
        val history = com.sentup.android.data.StatsRepository(context).loadHistory()
        historySnapshot.value = history
        val monday = java.util.Calendar.getInstance().apply {
            add(
                java.util.Calendar.DAY_OF_YEAR,
                -((get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7)
            )
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }.timeInMillis
        val week = history.filter { it.timestamp >= monday }
        if (week.isEmpty()) return@LaunchedEffect
        val minutes = week.sumOf { it.duration } / 60_000L
        val topArtist = week.filterNot { isUnknownArtist(it.artist) }
            .groupBy { it.artist }
            .maxByOrNull { it.value.size }
            ?.key
        if (minutes > 0 || topArtist != null) weekSummary.value = minutes to topArtist
    }

    val shelfScope = androidx.compose.runtime.rememberCoroutineScope()

    // One tap on a mood plays YouTube Music's own page for it: fetch the
    // browse, take its tracks, queue them. Shared by the mood rail below and
    // by any mood tiles inside YouTube Music's own home shelves.
    val playMood: (com.sentup.android.data.MusicShelfItem.Mood) -> Unit = { mood ->
        shelfScope.launch {
            val page = viewModel.fetchMusicShelves(mood.browseId, mood.params)
            val songs = page?.shelves.orEmpty()
                .flatMap { it.items }
                .filterIsInstance<com.sentup.android.data.MusicShelfItem.Track>()
                .map { it.song }
            if (songs.isNotEmpty()) playerViewModel?.playQueue(songs.take(50), songs.first())
        }
    }

    // The smart mixes: the lists YouTube Music does not build. They read the
    // pools already on screen - its own shelves, the charts, the local history -
    // and never fetch, so a tile is never a spinner.
    val smartMixes = remember(musicBrowse, songs, recentlyPlayed, historySnapshot.value) {
        val ytPools = listOf(BROWSE_HOME, BROWSE_NEW, BROWSE_CHARTS).flatMap { key ->
            musicBrowse[key]?.shelves.orEmpty().map { shelf ->
                shelf.items
                    .filterIsInstance<com.sentup.android.data.MusicShelfItem.Track>()
                    .map { it.song }
            }
        }.filter { it.isNotEmpty() }
        val ytTracks = ytPools.flatten().distinctBy { it.id }
        val history = historySnapshot.value
        val playedIds = history.map { it.songId }.toSet()
        val day = System.currentTimeMillis() / (24L * 60 * 60 * 1000)
        val familiar = (recentlyPlayed + songs).distinctBy { it.id }
        listOf(
            com.sentup.android.data.SmartMixRules.durationMix(
                ytTracks, 30L * 60_000, "30 dakikalık karışım"
            ),
            com.sentup.android.data.SmartMixRules.durationMix(
                ytTracks, 60L * 60_000, "1 saatlik karışım"
            ),
            com.sentup.android.data.SmartMixRules.shelfBlend(ytPools),
            com.sentup.android.data.SmartMixRules.dailyTen(ytPools, day),
            com.sentup.android.data.SmartMixRules.discoveryBridge(familiar, ytTracks),
            com.sentup.android.data.SmartMixRules.neverPlayed(ytTracks, playedIds),
            com.sentup.android.data.SmartMixRules.timeCapsule(history, System.currentTimeMillis()),
            com.sentup.android.data.SmartMixRules.longTail(history),
            // A real, saveable mix: the artist-spaced shuffle, computed from the
            // library so its tiles and its saved playlist both carry real songs.
            com.sentup.android.data.SmartMixRules.Built(
                id = "artist_spaced",
                title = "Sanatçı aralıklı karıştır",
                reason = "aynı sanatçı üst üste gelmez",
                tracks = com.sentup.android.data.SmartMixRules.artistSpaced(
                    songs, System.nanoTime()
                )
            ),
        ).filterNot { it.isEmpty }
    }

    // Long-press a smart tile to keep it: a built list becomes an ordinary
    // playlist in the Library, which YouTube Music cannot do with its mixes.
    val saveMix: (com.sentup.android.data.SmartMixRules.Built) -> Unit = { mix ->
        shelfScope.launch {
            val repo = com.sentup.android.data.PlaylistRepository(context)
            val id = repo.createPlaylist(mix.title, null)
            repo.addSongsToPlaylist(id, mix.tracks)
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.mixmaker_created, mix.title),
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    ExpressivePullToRefresh(
        // Placeholders already say "loading" on first load; the spinner is
        // only for a refresh the user asked for.
        isRefreshing = isRefreshing && !isInitialLoading,
        onRefresh = { viewModel.refresh(excludedFolders, manualScan) },
        modifier = Modifier.fillMaxSize()
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Centred past the page cap rather than widened: the whole window
            // still scrolls and flings.
            val pageSidePadding = ((maxWidth - FEED_PAGE_MAX_WIDTH) / 2).coerceAtLeast(0.dp)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .sentupPage()
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = pageSidePadding,
                        end = pageSidePadding,
                        top = contentPadding.calculateTopPadding(),
                        bottom = contentPadding.calculateBottomPadding()
                    )
                ) {
                    item(key = "feed-greeting") {
                        FeedGreeting(
                            onProfileClick = onProfileClick,
                            onSettingsClick = onSettingsClick,
                            onDownloadsClick = onDownloadsClick,
                            videoMode = videoMode,
                            onVideoModeToggle = onVideoModeToggle,
                            showModeToggle = showModeToggle,
                            viewModel = viewModel
                        )
                    }

                    item(key = "feed-quick") {
                        FeedQuickCapsules(
                            canShuffleLiked = playerViewModel != null && likedSongs.isNotEmpty(),
                            canStartRadio = playerViewModel != null && radioSeed != null,
                            onShuffleLiked = { playerViewModel?.playQueueShuffled(likedSongs) },
                            onRadio = { radioSeed?.let { playerViewModel?.playSongRadio(it) } },
                            onDownloads = onDownloadsClick,
                            onMakeMix = { showMixMaker = true }
                        )
                        if (showMixMaker) {
                            SentupMixMakerSheet(viewModel = viewModel, onDismiss = { showMixMaker = false })
                        }
                    }

                    item(key = "feed-insights") {
                        Column(
                            modifier = Modifier.padding(
                                start = SentupGroup.inset,
                                end = SentupGroup.inset,
                                top = SentupGroup.gap,
                                bottom = SentupGroup.gap
                            )
                        ) {
                            InsightsCard(viewModel = viewModel, onOpenStats = onStatsClick)
                        }
                    }

                    // One hero, whichever story is true: a paused song gets
                    // "resume where you left off", anything else gets the mix.
                    val resumeActive = playerViewModel != null && currentSong != null &&
                        durationNow > 0 && homeOptions.homeResumeShelf
                    if (resumeActive) {
                        item(key = "feed-resume") {
                            FeedResumeHero(
                                song = currentSong!!,
                                isPlaying = isPlayingNow,
                                progress = progressNow,
                                duration = durationNow,
                                onResume = { playerViewModel?.togglePlayPause() }
                            )
                        }
                    } else if (homeOptions.homeHeroCard) {
                        item(key = "feed-hero") {
                            FeedHero(
                                songs = songs,
                                isLoading = isInitialLoading,
                                skeletonAlpha = skeletonAlpha,
                                onPlayClick = onPlayClick,
                                onSpinClick = onSpinClick
                            )
                        }
                    }

                    // Offline, the mix below mostly needs a connection. What
                    // will actually play goes first, and only while it is true.
                    if (isOffline && offlineSongs.isNotEmpty()) {
                        item(key = "feed-offline") {
                            FeedSongsShelf(
                                title = stringResource(R.string.ready_offline),
                                subtitle = stringResource(R.string.home_offline_subtitle),
                                songs = offlineSongs.take(12),
                                onClick = onOfflineSongClick,
                                onLongClick = openSongPreview
                            )
                        }
                    }

                    if (!isInitialLoading && homeOptions.homeListeningStrip) {
                        weekSummary.value?.let { (minutes, artist) ->
                            item(key = "feed-summary") {
                                FeedWeekStrip(
                                    minutes = minutes,
                                    topArtist = artist,
                                    onClick = onStatsClick
                                )
                            }
                        }
                    }

                    // YT Music leads with a personal cover strip after the
                    // hero; server playlists already carry real covers, so
                    // those become the strip. Text-only lists keep the group
                    // further down.
                    item(key = "feed-personal") {
                        if (!isInitialLoading) {
                            val allCovered = (serverMixes + ownPlaylists)
                                .filter { it.thumbnailUrl != null }
                                .distinctBy { it.id }
                            // Pinned lists lead, in the order they were pinned;
                            // the rest keep feed order.
                            val coverLists = (
                                pinnedIds.mapNotNull { id -> allCovered.firstOrNull { it.id == id } } +
                                    allCovered.filterNot { it.id in pinnedIds }
                                ).take(12)
                            if (coverLists.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = SentupGroup.gap)
                                ) {
                                    SentupGroupHeader(text = stringResource(R.string.hf_personal_title))
                                    LazyRow(
                                        contentPadding = PaddingValues(
                                            start = FEED_RAIL_START,
                                            end = SentupGroup.inset
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(coverLists, key = { "pers_" + it.id }) { list ->
                                            val baseCaption = list.itemCount.takeIf { it > 0 }
                                                ?.let { pluralStringResource(R.plurals.n_songs, it, it) }
                                                ?: list.uploaderName
                                            FeedArtworkTile(
                                                cover = list.thumbnailUrl,
                                                title = listNameLabel(list.name),
                                                caption = if (list.id in pinnedIds)
                                                    stringResource(R.string.pinned_caption) + " · " + baseCaption
                                                else baseCaption,
                                                onClick = { onPlaylistClick(list) },
                                                onLongClick = { togglePin(list.id) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // The biggest personal library otherwise hides one tap
                    // deep in the Library; on covers it belongs on the front.
                    item(key = "feed-liked") {
                        if (!isInitialLoading && likedSongs.isNotEmpty()) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = SentupGroup.gap)
                            ) {
                                SentupGroupHeader(text = stringResource(R.string.liked_songs))
                                LazyRow(
                                    contentPadding = PaddingValues(
                                        start = FEED_RAIL_START,
                                        end = SentupGroup.inset
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(likedSongs.take(12), key = { "liked_" + it.id }) { song ->
                                        FeedArtworkTile(
                                            cover = feedCoverModel(song),
                                            title = song.title,
                                            caption = feedArtistLabel(song),
                                            onClick = { onSongClick(song) },
                                            onLongClick = { openSongPreview(song) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // YouTube Music's own new-releases page, on the front of
                    // the personal feed: fresh tracks and fresh albums the
                    // user's feeds would never surface on their own.
                    item(key = "feed-new-releases") {
                        if (!isInitialLoading) {
                            val fresh = musicBrowse[BROWSE_NEW]?.shelves.orEmpty()
                                .flatMap { it.items }
                                .take(12)
                            if (fresh.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = SentupGroup.gap)
                                ) {
                                    SentupGroupHeader(text = stringResource(R.string.browse_new_title))
                                    LazyRow(
                                        contentPadding = PaddingValues(
                                            start = FEED_RAIL_START,
                                            end = SentupGroup.inset
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(fresh, key = { "new_" + it.key }) { entry ->
                                            when (entry) {
                                                is com.sentup.android.data.MusicShelfItem.Track ->
                                                    FeedSongTile(
                                                        song = entry.song,
                                                        onClick = { onSongClick(entry.song) },
                                                        onLongClick = { openSongPreview(entry.song) }
                                                    )
                                                is com.sentup.android.data.MusicShelfItem.Collection ->
                                                    FeedArtworkTile(
                                                        cover = entry.playlist.thumbnailUrl,
                                                        title = entry.playlist.name,
                                                        caption = stringResource(
                                                            if (entry.isAlbum) R.string.lib_tab_albums
                                                            else R.string.lib_tab_playlists
                                                        ),
                                                        onClick = { onPlaylistClick(entry.playlist) },
                                                        onLongClick = null
                                                    )
                                                else -> Unit
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Two more real-data fronts: what is climbing the charts
                    // now, and songs the taste engine says the user has not
                    // heard yet but would like.
                    item(key = "feed-discovery") {
                        if (!isInitialLoading && discoverySongs.isNotEmpty()) {
                            FeedSongsShelf(
                                title = stringResource(R.string.home_discovery_title),
                                subtitle = null,
                                songs = discoverySongs.take(12),
                                onClick = onSongClick,
                                onLongClick = openSongPreview
                            )
                        }
                    }

                    item(key = "feed-charts") {
                        if (!isInitialLoading) {
                            val chartTracks = musicBrowse[BROWSE_CHARTS]?.shelves.orEmpty()
                                .flatMap { it.items }
                                .filterIsInstance<com.sentup.android.data.MusicShelfItem.Track>()
                                .take(12)
                            if (chartTracks.isNotEmpty()) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(top = SentupGroup.gap)
                                ) {
                                    SentupGroupHeader(text = stringResource(R.string.browse_charts_title))
                                    LazyRow(
                                        contentPadding = PaddingValues(
                                            start = FEED_RAIL_START,
                                            end = SentupGroup.inset
                                        ),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(chartTracks, key = { "chart_" + it.key }) { entry ->
                                            FeedSongTile(
                                                song = entry.song,
                                                onClick = { onSongClick(entry.song) },
                                                onLongClick = { openSongPreview(entry.song) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // The smart mixes - the lists YouTube Music does not build.
                    item(key = "feed-smart") {
                        if (!isInitialLoading && playerViewModel != null) {
                            val tiles = smartMixes.map { mix ->
                                SmartTile(
                                    title = mix.title,
                                    reason = mix.reason,
                                    tracks = mix.tracks,
                                    onPlay = {
                                        playerViewModel?.playQueue(mix.tracks, mix.tracks.first())
                                    }
                                )
                            }
                            if (tiles.isNotEmpty()) FeedSmartShelf(tiles = tiles, onSave = saveMix)
                        }
                    }

                    // YouTube Music's own home page, shelf for shelf. It ships
                    // a page of personalised carousels - quick picks, forgotten
                    // listens, mood mixes - and re-deriving three shelves from
                    // it left the rest of the page on the service. Rendering
                    // the page itself is what puts the same headings here as on
                    // YouTube Music. Kept out while empty so local-only mode,
                    // where the fetch is skipped, shows nothing rather than a
                    // spinner that never fills.
                    if (musicBrowse[BROWSE_HOME]?.shelves?.isNotEmpty() == true) {
                        musicBrowseItems(
                            state = musicBrowse[BROWSE_HOME],
                            moodTitle = null,
                            onCloseMood = {},
                            onRetry = { viewModel.loadMusicBrowse(BROWSE_HOME, force = true) },
                            onLoadMore = { viewModel.loadMoreMusicBrowse(BROWSE_HOME) },
                            onPlaylistClick = onPlaylistClick,
                            onArtistClick = { viewModel.requestArtistPage(it.name) },
                            onPlayTracks = { queue, start ->
                                if (queue.isNotEmpty()) {
                                    playerViewModel?.playQueue(queue, start ?: queue.first())
                                }
                            },
                            onMoodClick = playMood,
                            onSongLongPress = onSongLongPress,
                        )
                    }

                    // Playable moods: tapping one plays YouTube Music's own
                    // mood page straight away, no intermediate screen.
                    item(key = "feed-moods") {
                        if (!isInitialLoading && moodTiles.isNotEmpty() && playerViewModel != null) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(top = SentupGroup.gap)
                            ) {
                                SentupGroupHeader(text = stringResource(R.string.home_moods_title))
                                LazyRow(
                                    contentPadding = PaddingValues(
                                        start = FEED_RAIL_START,
                                        end = SentupGroup.inset
                                    ),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    items(moodTiles, key = { "mood_" + it.browseId + "_" + it.title }) { mood ->
                                        SentupQuickCapsule(
                                            icon = Icons.Rounded.Category,
                                            label = mood.title,
                                            onClick = { playMood(mood) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item(key = "feed-feature") {
                        if (isInitialLoading) {
                            FeedFeatureSkeleton(skeletonAlpha)
                        } else {
                            // The second recommendation, not the first: the
                            // hero already wears the first one's cover, and
                            // the same artwork twice on one screen reads as
                            // a bug, not as emphasis.
                            val picked = songs.getOrNull(1) ?: songs.firstOrNull()
                            if (picked != null) {
                                FeedFeature(
                                    song = picked,
                                    onClick = { onSongClick(picked) },
                                    onLongClick = openSongPreview.let { f -> { f(picked) } }
                                )
                            }
                        }
                    }

                    item(key = "feed-up-next") {
                        if (isInitialLoading) {
                            FeedRailSkeleton(skeletonAlpha)
                        } else {
                            val upNext = songs.drop(2).take(12)
                            if (upNext.isNotEmpty()) {
                                FeedSongsShelf(
                                    title = stringResource(R.string.home_mix_up_next),
                                    subtitle = null,
                                    songs = upNext,
                                    onClick = onSongClick,
                                    onLongClick = openSongPreview
                                )
                            }
                        }
                    }

                    item(key = "feed-recent") {
                        if (isInitialLoading) {
                            FeedRailSkeleton(skeletonAlpha)
                        } else if (recentlyPlayed.isNotEmpty()) {
                            FeedSongsShelf(
                                title = stringResource(R.string.home_section_jump_back_in),
                                subtitle = null,
                                songs = recentlyPlayed.take(12),
                                onClick = onRecentClick,
                                onLongClick = openSongPreview
                            )
                        }
                    }

                    item(key = "feed-albums") {
                        if (!isInitialLoading && recentAlbums.isNotEmpty()) {
                            FeedAlbumsShelf(recentAlbums, onRecentAlbumClick)
                        }
                    }

                    item(key = "feed-artists") {
                        if (!isInitialLoading && homeOptions.homeArtistShelf &&
                            topArtists.isNotEmpty()
                        ) {
                            FeedArtistsShelf(
                                artists = topArtists,
                                photos = artistPhotos,
                                large = homeOptions.homeArtistTilesLarge,
                                onArtistClick = onArtistClick,
                                onArtistLongClick = { artist ->
                                    popoutSong.value = null
                                    artistOptions.value = artist
                                }
                            )
                        }
                    }

                    item(key = "feed-playlists") {
                        if (!isInitialLoading) {
                            FeedPlaylistsGroup(
                                playlists = ownPlaylists,
                                likedCount = likedCount,
                                downloadsCount = offlineSongs.size,
                                onLikedClick = onLikedClick,
                                onDownloadsClick = onDownloadsClick,
                                onPlaylistClick = onPlaylistClick,
                                onShowAllInLibrary = onShowAllInLibrary
                            )
                        }
                    }

                    item(key = "feed-mixes") {
                        if (!isInitialLoading && homeOptions.homeMixShelf) {
                            FeedMixesShelf(
                                serverMixes = serverMixes,
                                storedMixes = mixes,
                                generatedMixes = if (playerViewModel != null) {
                                    generatedMixes.value.orEmpty()
                                } else {
                                    emptyList()
                                },
                                collageCovers = (recentlyPlayed + songs).mapNotNull {
                                    feedCoverModel(it)
                                }.take(4),
                                onPlayServerMix = { playlist ->
                                    shelfScope.launch {
                                        val tracks = viewModel.fetchPlaylistSongs(playlist.id)
                                        if (tracks.isNotEmpty()) {
                                            playerViewModel?.playQueue(tracks, tracks.first())
                                        }
                                    }
                                },
                                onOpenServerMix = onPlaylistClick,
                                onPlayGeneratedMix = { mix ->
                                    shelfScope.launch {
                                        val plays = com.sentup.android.data
                                            .StatsRepository(context).loadHistory()
                                        val queue = mix.songIds.mapNotNull { id ->
                                            plays.firstOrNull { it.songId == id }?.let { p ->
                                                Song.fromYouTube(
                                                    p.songId, p.title, p.artist, p.album,
                                                    p.duration, p.thumbnailUrl
                                                )
                                            }
                                        }
                                        if (queue.isNotEmpty()) {
                                            playerViewModel?.playQueue(queue, queue.first())
                                        }
                                    }
                                }
                            )
                        }
                    }

                    item(key = "feed-bottom") {
                        Spacer(Modifier.height(48.dp))
                    }
                }
            }
        }
    }

    // Long-press answers. The song popout keeps the tile's song visible while
    // it acts; the artist list is the two things an artist is asked for.
    popoutSong.value?.let { song ->
        val liked = androidx.compose.runtime.remember(popoutSong.value, popoutLikedTick) {
            playerViewModel?.isSongLiked(song.id) == true
        }
        SentupSongPopout(
            song = song,
            isLiked = liked,
            onPlay = {
                popoutSong.value = null
                val queue = if (song in songs) songs else listOf(song)
                playerViewModel?.playQueue(queue, song)
            },
            onQueue = {
                popoutSong.value = null
                playerViewModel?.addToQueue(song)
            },
            onLike = {
                playerViewModel?.toggleLike(song)
                popoutLikedTick++
            },
            onMore = onSongLongPress?.let { f -> { popoutSong.value = null; f(song) } },
            onDismiss = { popoutSong.value = null }
        )
    }

    artistOptions.value?.let { artist ->
        val radioSeedForArtist = (recentlyPlayedState + songs).firstOrNull {
            it.artist.equals(artist.name, ignoreCase = true)
        }
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { artistOptions.value = null }
        ) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .sentupCard(
                        shape = RoundedCornerShape(SentupRadii.panel),
                        elevation = 2,
                        tinted = false
                    )
                    .padding(vertical = 4.dp)
            ) {
                SentupRow(
                    title = artist.name,
                    subtitle = stringResource(R.string.hf_open_artist),
                    showChevron = true,
                    onClick = {
                        artistOptions.value = null
                        onArtistClick(artist)
                    }
                )
                if (playerViewModel != null && radioSeedForArtist != null) {
                    SentupDivider()
                    SentupRow(
                        title = stringResource(R.string.ar_start_radio),
                        icon = Icons.Rounded.Radio,
                        onClick = {
                            artistOptions.value = null
                            playerViewModel.playSongRadio(radioSeedForArtist)
                        }
                    )
                }
            }
        }
    }
}

/**
 * The page's title: the hour's greeting large and bold, one quiet line under
 * it, and the account and settings circles floating at its end on their own
 * shadows. A row of flat buttons printed on the page is what the old bar was;
 * these sit above it.
 */
@Composable
private fun FeedGreeting(
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    videoMode: Boolean,
    onVideoModeToggle: (Boolean) -> Unit,
    showModeToggle: Boolean,
    viewModel: HomeViewModel
) {
    val context = LocalContext.current
    val hour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greetingRes = when (hour) {
        in 5..11 -> R.string.home_greeting_morning
        in 12..16 -> R.string.home_greeting_afternoon
        in 17..21 -> R.string.home_greeting_evening
        else -> R.string.home_greeting_night
    }

    val userAvatar by viewModel.userAvatar.collectAsState()
    val downloadingIds by viewModel.downloadingIds.collectAsState()
    val incognito by com.sentup.android.data.IncognitoMode.enabled(context).collectAsState()
    val topBarOptions = rememberHomeTopBarOptions()
    val showDownloads = topBarOptions.alwaysShowDownloads || downloadingIds.isNotEmpty()

    val accountSwitcher = remember(context) {
        com.sentup.android.data.AccountSwitcher(context)
    }
    val isSwitching by accountSwitcher.switching.collectAsState()
    val openAccountSwitcher = com.sentup.android.ui.profile.LocalOpenAccountSwitcher.current
    val haptics = com.sentup.android.util.rememberSentupHaptics()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SentupGroup.inset,
                end = SentupGroup.inset,
                top = 10.dp,
                bottom = 2.dp
            )
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(greetingRes),
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = stringResource(R.string.home_greeting_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        // The circles get their own line under the greeting: five of them in
        // one row with a 34sp title left "Günaydın" clipped to "Güna...".
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
        Spacer(Modifier.size(2.dp))

        // The profile circle: a tap opens the profile, a hold is the account
        // switcher (same contract as the old bar), and the history-paused mark
        // rides on the circle rather than sitting beside it.
        Box {
            Box(
                modifier = Modifier
                    .feedPress(onClick = onProfileClick) {
                        val switcher = openAccountSwitcher != null ||
                            accountSwitcher.quickSwitchTarget() != null
                        if (!switcher) return@feedPress
                        haptics.performHapticFeedback(
                            androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                        )
                        if (openAccountSwitcher != null) {
                            openAccountSwitcher()
                        } else {
                            accountSwitcher.quickSwitch()
                        }
                    }
                    .sentupCard(shape = CircleShape, elevation = 1, tinted = false)
                    .size(46.dp),
                contentAlignment = Alignment.Center
            ) {
                com.sentup.android.ui.profile.ActiveProfileAvatar(
                    modifier = Modifier.fillMaxSize(),
                    accountAvatarUrl = userAvatar
                )
                // The feeds refetch behind a switch; the status belongs where
                // the user just tapped, not in a blocking overlay.
                androidx.compose.animation.AnimatedVisibility(
                    visible = isSwitching,
                    enter = androidx.compose.animation.fadeIn(),
                    exit = androidx.compose.animation.fadeOut()
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.fillMaxSize(),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = incognito,
                enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 2.dp, y = 2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .border(
                            width = 2.dp,
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.VisibilityOff,
                        contentDescription = stringResource(R.string.incognito_active),
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(11.dp)
                    )
                }
            }
        }

        Spacer(Modifier.size(10.dp))

        if (showModeToggle) {
            com.sentup.android.ui.components.ModeMorphButton(
                videoMode = videoMode,
                onVideoModeChange = onVideoModeToggle
            )
            Spacer(Modifier.size(10.dp))
        }

        // Downloads is a status, not a destination shortcut: it shows while
        // something downloads, or when Customization pins it on. Shared
        // component, drawn exactly as it draws itself.
        com.sentup.android.ui.components.TopBarDownloadsButton(
            visible = showDownloads,
            downloading = downloadingIds.isNotEmpty(),
            onClick = onDownloadsClick,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        )

        Spacer(Modifier.size(10.dp))

        // Recolour the app without leaving it: one tap here, one tap on a
        // colour, and the same setting Settings writes is written.
        QuickPaletteButton()

        Spacer(Modifier.size(10.dp))

        Box(
            modifier = Modifier
                .feedPress(onClick = onSettingsClick)
                .sentupCard(shape = CircleShape, elevation = 1, tinted = false)
                .size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.cd_settings),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(21.dp)
            )
        }
        }
    }
}

/**
 * The mix as one panel: its cover cropped in from the top-left corner and
 * larger than the card that holds it, so the art continues past the copy it
 * stands behind; the words read through a scrim rather than next to a
 * thumbnail. Play is a pill of the brand's own emerald; Spin stays as the
 * badge on its corner, the other way to start the same mix.
 */
@Composable
private fun FeedHero(
    songs: List<Song>,
    isLoading: Boolean,
    skeletonAlpha: Float,
    onPlayClick: () -> Unit,
    onSpinClick: () -> Unit
) {
    val shape = RoundedCornerShape(SentupRadii.panel)
    val first = songs.firstOrNull()
    val cover = first?.let { feedCoverModel(it) }

    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = SentupGroup.inset, end = SentupGroup.inset, top = 18.dp)
            .sentupCard(shape = shape, elevation = 2)
            .sentupSpecularEdge(shape)
            .height(FEED_HERO_HEIGHT)
    ) {
        if (cover != null) {
            // Deliberately bigger than the panel and pinned to the top-left:
            // the cover is a surface the card is cut out of, not a thumbnail
            // inside a box. The card's own clip does the cropping.
            AsyncImage(
                model = cover,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .requiredSize(330.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                scheme.primaryContainer.copy(alpha = 0.45f),
                                scheme.secondaryContainer.copy(alpha = 0.25f)
                            )
                        )
                    )
            )
        }

        // The scrim is the typography's safety: no headline sits on a random
        // cover without it.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.05f),
                        0.45f to Color.Black.copy(alpha = 0.38f),
                        1f to Color.Black.copy(alpha = 0.80f)
                    )
                )
        )

        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp)) {
                if (isLoading) {
                    SkeletonTextLine(width = 168.dp, height = 24.dp, alpha = skeletonAlpha)
                    Spacer(Modifier.height(8.dp))
                    SkeletonTextLine(width = 120.dp, height = 13.dp, alpha = skeletonAlpha)
                } else {
                    Text(
                        text = stringResource(R.string.your_mix_line1) + " " +
                            stringResource(R.string.your_mix_line2),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = feedHeroArtists(songs),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // The pill and the Spin badge sit side by side: an overlap at the
            // pill's corner read as a rendering bug on device.
            Row(
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 22.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier
                        .feedPress(onClick = onPlayClick)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(SentupRadii.pill)
                        )
                        .padding(start = 22.dp, end = 22.dp, top = 12.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = stringResource(R.string.cd_play),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
                com.sentup.android.ui.home.SpinBadge(onClick = onSpinClick)
            }
        }
    }
}

/**
 * The row of verbs above the hero: shuffle the liked songs, start a radio
 * from what was last played, open the downloads. Capsules, not buttons in a
 * bar - they are Home's shortcuts, each one tap, each pressing in like every
 * other object on the page. A capsule whose thing does not exist yet hides
 * itself; three dead pills are worse than one live one.
 */
@Composable
private fun FeedQuickCapsules(
    canShuffleLiked: Boolean,
    canStartRadio: Boolean,
    onShuffleLiked: () -> Unit,
    onRadio: () -> Unit,
    onDownloads: () -> Unit,
    onMakeMix: () -> Unit
) {
    LazyRow(
        modifier = Modifier.padding(top = 6.dp),
        contentPadding = PaddingValues(
            start = FEED_RAIL_START,
            end = SentupGroup.inset
        ),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (canShuffleLiked) {
            item(key = "caps-shuffle") {
                SentupQuickCapsule(
                    icon = Icons.Rounded.Shuffle,
                    label = stringResource(R.string.hf_quick_shuffle_liked),
                    onClick = onShuffleLiked
                )
            }
        }
        if (canStartRadio) {
            item(key = "caps-radio") {
                SentupQuickCapsule(
                    icon = Icons.Rounded.Radio,
                    label = stringResource(R.string.ar_start_radio),
                    onClick = onRadio
                )
            }
        }
        item(key = "caps-downloads") {
            SentupQuickCapsule(
                icon = Icons.Rounded.Download,
                label = stringResource(R.string.cd_downloads),
                onClick = onDownloads
            )
        }
        item(key = "caps-otonom") {
            SentupQuickCapsule(
                icon = Icons.Rounded.AutoAwesome,
                label = stringResource(R.string.capsule_otonom),
                onClick = onMakeMix
            )
        }
    }
}

/**
 * The hero when a song is mid-life: its cover cropped the way the mix hero
 * crops its own, a progress line that says where you stopped, and one tap
 * that continues exactly there. Times sit in the page's monospace so the
 * numbers hold their columns. It replaces the mix hero only while a song is
 * actually loaded - the mix returns the moment the player is empty.
 */
@Composable
private fun FeedResumeHero(
    song: Song,
    isPlaying: Boolean,
    progress: Long,
    duration: Long,
    onResume: () -> Unit
) {
    val shape = RoundedCornerShape(SentupRadii.panel)
    val fraction = (progress.toFloat() / duration.toFloat()).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = SentupGroup.inset, end = SentupGroup.inset, top = 14.dp)
            .sentupCard(shape = shape, elevation = 2)
            .sentupSpecularEdge(shape)
            .height(216.dp)
            .feedPress(onClick = onResume)
    ) {
        AsyncImage(
            model = feedCoverModel(song),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .align(Alignment.TopStart)
                .requiredSize(280.dp)
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.05f),
                        0.45f to Color.Black.copy(alpha = 0.38f),
                        1f to Color.Black.copy(alpha = 0.82f)
                    )
                )
        )

        Column(modifier = Modifier.align(Alignment.BottomStart)) {
            Column(modifier = Modifier.padding(start = 22.dp, end = 22.dp)) {
                Text(
                    text = stringResource(R.string.home_section_jump_back_in),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = song.title,
                    fontSize = 24.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = feedArtistLabel(song),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(14.dp))

            Row(
                modifier = Modifier.padding(start = 22.dp, end = 22.dp, bottom = 20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(SentupRadii.pill))
                            .background(Color.White.copy(alpha = 0.28f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction.coerceAtLeast(0.02f))
                                .height(4.dp)
                                .clip(RoundedCornerShape(SentupRadii.pill))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = feedClock(progress),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Text(
                            text = feedClock(duration),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = FontFamily.Monospace
                            ),
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
                Spacer(Modifier.size(16.dp))
                // The button is decoration here: the whole card is the
                // target, and its state is what the glyph says.
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/** m:ss, or h:mm:ss past the hour. Monospace-safe: fields never jitter. */
private fun feedClock(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val s = totalSec % 60
    val m = (totalSec / 60) % 60
    val h = totalSec / 3600
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}

/**
 * The week in one line: how long, and who owned it. Tapping opens the full
 * analytics page; the strip is the question, that page is the answer.
 */
@Composable
private fun FeedWeekStrip(
    minutes: Long,
    topArtist: String?,
    onClick: () -> Unit
) {
    val hours = minutes / 60
    val mins = minutes % 60
    val timeText = if (hours > 0) "${hours}sa ${mins}dk" else "${mins}dk"
    val showTime = minutes > 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset)
            .padding(top = SentupGroup.gap)
            .feedPress(onClick = onClick)
            .sentupCard(
                shape = RoundedCornerShape(SentupRadii.card),
                elevation = 1,
                tinted = false
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.Insights,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.hf_week_label) + ":",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
        if (showTime) {
            Spacer(Modifier.size(6.dp))
            Text(
                text = timeText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
        if (topArtist != null) {
            Spacer(Modifier.size(6.dp))
            Text(
                text = if (showTime) "· $topArtist" else topArtist,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        } else {
            Spacer(Modifier.weight(1f))
        }
        Spacer(Modifier.size(8.dp))
        SentupChevron()
    }
}

/**
 * The one thing picked tonight: a single cover, what it is, one line about
 * it, and one tap that starts it. This shelf exists because a collage of
 * loose circles suggests nothing and asks for nothing; a feature asks.
 */
@Composable
private fun FeedFeature(
    song: Song,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = SentupGroup.gap)) {
        SentupGroupHeader(text = stringResource(R.string.home_picked_for_you))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset)
                .feedPress(onClick = onClick, onLongClick = onLongClick)
                .sentupCard(
                    shape = RoundedCornerShape(SentupRadii.card),
                    elevation = 1,
                    tinted = false
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(FEED_FEATURE_COVER)
                    .clip(RoundedCornerShape(SentupRadii.tile))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                FeedCoverImage(model = feedCoverModel(song), modifier = Modifier.matchParentSize())
            }
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = feedArtistLabel(song),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(12.dp))
                // Not a second button: the whole card already plays. This is
                // the promise made visible, so the affordance is not hidden.
                Row(
                    modifier = Modifier
                        .background(
                            MaterialTheme.colorScheme.primary,
                            RoundedCornerShape(SentupRadii.pill)
                        )
                        .padding(start = 14.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.size(6.dp))
                    Text(
                        text = stringResource(R.string.cd_play),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedFeatureSkeleton(skeletonAlpha: Float) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = SentupGroup.gap)) {
        SentupGroupHeader(text = stringResource(R.string.home_picked_for_you))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset)
                .sentupCard(
                    shape = RoundedCornerShape(SentupRadii.card),
                    elevation = 1,
                    tinted = false
                )
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SkeletonBox(
                modifier = Modifier.size(FEED_FEATURE_COVER),
                shape = RoundedCornerShape(SentupRadii.tile),
                alpha = skeletonAlpha
            )
            Spacer(Modifier.size(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                SkeletonTextLine(width = 172.dp, height = 18.dp, alpha = skeletonAlpha)
                Spacer(Modifier.height(8.dp))
                SkeletonTextLine(width = 120.dp, height = 13.dp, alpha = skeletonAlpha)
            }
        }
    }
}

/**
 * The playlist group: what the user chose, as one inset card of rows. Lists
 * you *open* are rows, not thumbnails - the shelf language is for things you
 * browse past, this is for things you go into. Liked and Downloads lead as a
 * pair of count cards: they are the two lists whose whole reason is a number.
 */
@Composable
private fun FeedPlaylistsGroup(
    playlists: List<PlaylistDisplayItem>,
    likedCount: Int,
    downloadsCount: Int,
    onLikedClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onShowAllInLibrary: () -> Unit
) {
    val shown = playlists.take(7)
    if (likedCount <= 0 && downloadsCount <= 0 && shown.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(top = SentupGroup.gap)) {
        SentupGroupHeader(text = stringResource(R.string.spotlight_your_playlists))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FeedCountCard(
                title = stringResource(R.string.liked_songs),
                count = likedCount,
                icon = Icons.Rounded.Favorite,
                onClick = onLikedClick,
                modifier = Modifier.weight(1f)
            )
            FeedCountCard(
                title = stringResource(R.string.cd_downloads),
                count = downloadsCount,
                icon = Icons.Rounded.Download,
                onClick = onDownloadsClick,
                modifier = Modifier.weight(1f)
            )
        }
        if (shown.isNotEmpty()) {
            Spacer(Modifier.height(12.dp))
            SentupGroupCard {
                shown.forEachIndexed { index, playlist ->
                    SentupRow(
                        title = listNameLabel(playlist.name),
                        value = playlist.itemCount.takeIf { it > 0 }?.let {
                            pluralStringResource(R.plurals.n_songs, it, it)
                        },
                        showChevron = true,
                        onClick = { onPlaylistClick(playlist) }
                    )
                    SentupDivider()
                }
                SentupRow(
                    title = stringResource(R.string.tab_library),
                    showChevron = true,
                    onClick = onShowAllInLibrary
                )
            }
        }
    }
}

/** A destination you measure in counts: name, count, icon - a card, a tap. */
@Composable
private fun FeedCountCard(
    title: String,
    count: Int,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .feedPress(onClick = onClick)
            .sentupCard(
                shape = RoundedCornerShape(SentupRadii.card),
                elevation = 1,
                tinted = false
            )
            .padding(14.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.height(1.dp))
        Text(
            text = pluralStringResource(R.plurals.n_songs, count, count),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1
        )
    }
}

@Composable
private fun FeedSongsShelf(
    title: String,
    subtitle: String?,
    songs: List<Song>,
    onClick: (Song) -> Unit,
    onLongClick: ((Song) -> Unit)?
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = SentupGroup.gap)) {
        SentupGroupHeader(text = title, subtitle = subtitle)
        LazyRow(
            contentPadding = PaddingValues(
                start = FEED_RAIL_START,
                end = SentupGroup.inset
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(songs) { song ->
                FeedSongTile(
                    song = song,
                    onClick = { onClick(song) },
                    onLongClick = onLongClick?.let { f -> { f(song) } }
                )
            }
        }
    }
}

@Composable
private fun FeedAlbumsShelf(
    albums: List<RecentAlbum>,
    onAlbumClick: (RecentAlbum) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = SentupGroup.gap)) {
        SentupGroupHeader(text = stringResource(R.string.home_section_recent_albums))
        LazyRow(
            contentPadding = PaddingValues(
                start = FEED_RAIL_START,
                end = SentupGroup.inset
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(albums) { album ->
                FeedArtworkTile(
                    cover = album.artwork,
                    title = album.title,
                    caption = album.artist.ifBlank { stringResource(R.string.unknown_artist) },
                    onClick = { onAlbumClick(album) },
                    onLongClick = null
                )
            }
        }
    }
}

/**
 * "Karışımların": every mix the app can start with one tap, in three
 * families. The account's own auto-mixes come from the signed-in YouTube
 * Music home page (tap plays, holding opens the list); mixes the user kept
 * open as lists; and the locally generated ones, which wear a collage of
 * what is actually on the page as their cover because nothing behind them
 * draws one. A family with no data simply does not render.
 */
@Composable
private fun FeedMixesShelf(
    serverMixes: List<PlaylistDisplayItem>,
    storedMixes: List<PlaylistDisplayItem>,
    generatedMixes: List<com.sentup.android.data.AutoMixEngine.AutoMix>,
    collageCovers: List<Any?>,
    onPlayServerMix: (PlaylistDisplayItem) -> Unit,
    onOpenServerMix: (PlaylistDisplayItem) -> Unit,
    onPlayGeneratedMix: (com.sentup.android.data.AutoMixEngine.AutoMix) -> Unit
) {
    if (serverMixes.isEmpty() && storedMixes.isEmpty() && generatedMixes.isEmpty()) return

    Column(modifier = Modifier.fillMaxWidth().padding(top = SentupGroup.gap)) {
        SentupGroupHeader(text = stringResource(R.string.home_section_my_mixes))
        LazyRow(
            contentPadding = PaddingValues(
                start = FEED_RAIL_START,
                end = SentupGroup.inset
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(serverMixes, key = { "srv_" + it.id }) { playlist ->
                FeedArtworkTile(
                    cover = playlist.thumbnailUrl,
                    title = listNameLabel(playlist.name),
                    caption = playlist.itemCount.takeIf { it > 0 }
                        ?.let { pluralStringResource(R.plurals.n_songs, it, it) }
                        ?: playlist.uploaderName,
                    onClick = { onPlayServerMix(playlist) },
                    onLongClick = { onOpenServerMix(playlist) }
                )
            }
            items(storedMixes, key = { "mix_" + it.id }) { playlist ->
                FeedArtworkTile(
                    cover = playlist.thumbnailUrl,
                    title = listNameLabel(playlist.name),
                    caption = playlist.uploaderName,
                    onClick = { onOpenServerMix(playlist) },
                    onLongClick = null
                )
            }
            items(generatedMixes, key = { "gen_" + it.id }) { mix ->
                FeedMixTile(
                    title = mix.title,
                    reason = mix.reason,
                    covers = collageCovers,
                    onClick = { onPlayGeneratedMix(mix) },
                    onLongClick = null
                )
            }
        }
    }
}

/** A generated mix's tile: a 2×2 collage of real covers, its name, its reason. */
@Composable
private fun FeedMixTile(
    title: String,
    reason: String,
    covers: List<Any?>,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .feedPress(onClick = onClick, onLongClick = onLongClick)
            .sentupCard(
                shape = RoundedCornerShape(SentupRadii.tile),
                elevation = 1,
                tinted = false
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(FEED_TILE_COVER)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(scheme.primaryContainer, scheme.tertiaryContainer)
                    )
                )
        ) {
            if (covers.size >= 4) {
                Column {
                    Row {
                        FeedCoverImage(model = covers[0], modifier = Modifier.size(FEED_TILE_COVER / 2))
                        FeedCoverImage(model = covers[1], modifier = Modifier.size(FEED_TILE_COVER / 2))
                    }
                    Row {
                        FeedCoverImage(model = covers[2], modifier = Modifier.size(FEED_TILE_COVER / 2))
                        FeedCoverImage(model = covers[3], modifier = Modifier.size(FEED_TILE_COVER / 2))
                    }
                }
            }
            // The shuffle glyph keeps the tile reading as a mix even when the
            // collage is complete; it rides a scrim so it survives any cover.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(Color.Black.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(30.dp)
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(FEED_TILE_COVER)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = reason,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(FEED_TILE_COVER)
        )
        Spacer(Modifier.height(4.dp))
    }
}

/**
 * The artists as a grid of big circles: two rows, sized by the density the
 * user picked in Settings. A rail scatters them past the edge where nobody
 * long-presses; a grid gives every face a target and a second meaning under
 * the finger - a hold opens the artist's two real actions instead of a
 * single blind tap.
 */
@Composable
private fun FeedArtistsShelf(
    artists: List<TopArtist>,
    photos: Map<String, String>,
    large: Boolean,
    onArtistClick: (TopArtist) -> Unit,
    onArtistLongClick: (TopArtist) -> Unit
) {
    val perRow = if (large) 3 else 4
    val shown = artists.take(perRow * 2)

    Column(modifier = Modifier.fillMaxWidth().padding(top = SentupGroup.gap)) {
        SentupGroupHeader(text = stringResource(R.string.home_section_top_artists))
        shown.chunked(perRow).forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SentupGroup.inset),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                row.forEach { artist ->
                    FeedArtistTile(
                        name = artist.name,
                        photo = photos[artist.name] ?: artist.fallbackArtwork,
                        large = large,
                        onClick = { onArtistClick(artist) },
                        onLongClick = { onArtistLongClick(artist) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // A short last row keeps the columns straight.
                repeat(perRow - row.size) { Spacer(Modifier.weight(1f)) }
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

/** A song as a card: its cover, its title, its artist. Pressed, it plays. */
@Composable
private fun FeedSongTile(
    song: Song,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    FeedArtworkTile(
        cover = feedCoverModel(song),
        title = song.title,
        caption = feedArtistLabel(song),
        onClick = onClick,
        onLongClick = onLongClick
    )
}

/**
 * The shelf tile, shared by songs, albums and playlists: a card of
 * [SentupRadii.tile] corners with the cover sunk into it and two lines of
 * caption on the card's own surface - white on a scrim would be a lottery
 * across a hundred different covers.
 */
@Composable
private fun FeedArtworkTile(
    cover: Any?,
    title: String,
    caption: String,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .feedPress(onClick = onClick, onLongClick = onLongClick)
            .sentupCard(
                shape = RoundedCornerShape(SentupRadii.tile),
                elevation = 1,
                tinted = false
            )
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(FEED_TILE_COVER)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            FeedCoverImage(model = cover, modifier = Modifier.matchParentSize())
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(FEED_TILE_COVER)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = caption,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(FEED_TILE_COVER)
        )
        Spacer(Modifier.height(4.dp))
    }
}

/** An artist: a circle (people are round here, releases are not) and a name. */
@Composable
private fun FeedArtistTile(
    name: String,
    photo: String?,
    large: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val diameter = if (large) FEED_ARTIST_PHOTO else 84.dp
    Column(
        modifier = modifier
            .feedPress(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(diameter)
                .sentupCard(shape = CircleShape, elevation = 1, tinted = false)
                .padding(3.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
        ) {
            FeedCoverImage(model = photo, modifier = Modifier.matchParentSize())
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/** The rail's shape, before the real data arrives: cards of the same size. */
@Composable
private fun FeedRailSkeleton(skeletonAlpha: Float) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = SentupGroup.gap)) {
        LazyRow(
            contentPadding = PaddingValues(
                start = FEED_RAIL_START,
                end = SentupGroup.inset
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(3) {
                Column(
                    modifier = Modifier
                        .sentupCard(
                            shape = RoundedCornerShape(SentupRadii.tile),
                            elevation = 1,
                            tinted = false
                        )
                        .padding(8.dp)
                ) {
                    SkeletonBox(
                        modifier = Modifier.size(FEED_TILE_COVER),
                        shape = RoundedCornerShape(12.dp),
                        alpha = skeletonAlpha
                    )
                    Spacer(Modifier.height(9.dp))
                    SkeletonTextLine(width = 96.dp, height = 12.dp, alpha = skeletonAlpha)
                    Spacer(Modifier.height(4.dp))
                    SkeletonTextLine(width = 64.dp, height = 10.dp, alpha = skeletonAlpha)
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
    }
}

/**
 * A press that goes *inward*: 0.97 for 90ms with a trace less alpha, no
 * ripple, no colour flash. A flash repaints the tile and its cover as a
 * different object; a scale is the same object moving away from the glass.
 */
@Composable
private fun Modifier.feedPress(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "feedTilePress"
    )
    val fade by animateFloatAsState(
        targetValue = if (pressed) 0.86f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "feedTileFade"
    )
    return this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
            alpha = fade
        }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            onClick = onClick,
            onLongClick = onLongClick
        )
}

@Composable
private fun FeedCoverImage(model: Any?, modifier: Modifier = Modifier) {
    if (model == null) return
    AsyncImage(
        model = model,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

/** Best cover a song can show: hi-res stream art, stream art, or the file's tag. */
private fun feedCoverModel(song: Song): Any? =
    song.highResThumbnailUrl ?: song.thumbnailUrl ?: song.albumArtUri?.toString()

/** The artist line for a tile: the unknown marker never reads as a credit. */
@Composable
private fun feedArtistLabel(song: Song): String =
    song.artist.takeIf { !isUnknownArtist(it) } ?: stringResource(R.string.unknown_artist)

/** The hero's second line: the first two real artists of the mix, or the fallback. */
@Composable
private fun feedHeroArtists(songs: List<Song>): String {
    val first = songs.getOrNull(0)?.artist?.takeIf { !isUnknownArtist(it) }
        ?: stringResource(R.string.unknown_artist)
    val second = songs.getOrNull(1)?.artist?.takeIf { !isUnknownArtist(it) }
    return if (second != null) {
        stringResource(R.string.artists_joined, first, second)
    } else {
        first
    }
}

/** One smart-mix tile: a name, a reason, and a tap that plays it. */
private data class SmartTile(
    val title: String,
    val reason: String,
    val tracks: List<Song>,
    val onPlay: () -> Unit,
)

/**
 * The smart mixes as a rail of cards. A tap plays the list; a long press, when
 * the list is already built, keeps it as an ordinary Library playlist - the one
 * thing a YouTube Music mix cannot become.
 */
@Composable
private fun FeedSmartShelf(
    tiles: List<SmartTile>,
    onSave: (com.sentup.android.data.SmartMixRules.Built) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = SentupGroup.gap)) {
        SentupGroupHeader(
            text = "Akıllı karışımlar",
            subtitle = "YouTube Music'te olmayan listeler"
        )
        LazyRow(
            contentPadding = PaddingValues(start = FEED_RAIL_START, end = SentupGroup.inset),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(tiles, key = { it.title }) { tile ->
                Column(
                    modifier = Modifier
                        .width(190.dp)
                        .height(92.dp)
                        .sentupCard(
                            shape = RoundedCornerShape(SentupRadii.card),
                            elevation = 1,
                            tinted = false
                        )
                        .combinedClickable(
                            onClick = tile.onPlay,
                            onLongClick = {
                                if (tile.tracks.isNotEmpty()) {
                                    onSave(
                                        com.sentup.android.data.SmartMixRules.Built(
                                            id = tile.title,
                                            title = tile.title,
                                            reason = tile.reason,
                                            tracks = tile.tracks
                                        )
                                    )
                                }
                            }
                        )
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = tile.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tile.reason,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
