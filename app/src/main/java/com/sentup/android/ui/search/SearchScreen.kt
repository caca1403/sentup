package com.sentup.android.ui.search

import android.content.ActivityNotFoundException
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.KeyboardVoice
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sentup.android.R
import com.sentup.android.data.ArtistItem
import com.sentup.android.data.isMissingChannelName
import com.sentup.android.data.isUnknownArtist
import com.sentup.android.data.isUnknownTitle
import com.sentup.android.data.PlaylistDisplayItem
import com.sentup.android.data.Song
import com.sentup.android.data.VideoItem
import com.sentup.android.data.VideoPlaylist
import com.sentup.android.data.VideoSearchDateFilter
import com.sentup.android.data.VideoSearchSort
import com.sentup.android.data.YouTubeLinkParser
import com.sentup.android.ui.components.displaySubtitle
import com.sentup.android.ui.components.hasReleaseMetadata
import com.sentup.android.ui.components.queueRowKeys
import com.sentup.android.ui.components.VideoThumbnailBadge
import com.sentup.android.ui.home.HomeViewModel
import com.sentup.android.ui.theme.SentupChevron
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupGroupCard
import com.sentup.android.ui.theme.SentupGroupHeader
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.SentupRow
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage
import com.sentup.android.ui.theme.sentupSpecularEdge
import com.sentup.android.ui.video.VideoCard
import com.sentup.android.ui.video.VideoOptionsSheetHost
import com.sentup.android.ui.video.videoListItems
import com.sentup.android.util.rememberSentupHaptics
import kotlinx.coroutines.delay

/**
 * Search, rebuilt on the app's own surface language (SentupSurfaces,
 * SentupComponents): an iOS large title that yields to a floating pill field
 * when it takes focus, categories as a strip of pills, and results as inset
 * group cards whose rows press inward instead of flashing. The M3-expressive
 * scaffolding this screen used to sit on - connected toggle groups,
 * segmented rows, organic-shape avatars - is gone; the emerald palette does
 * the accenting, with no category-specific hues left over.
 *
 * The behaviour is deliberately untouched: debounced per-category search with
 * cache-aware spinner suppression, endless paging keyed on scroll *and* list
 * size, saveable query state, pasted-link resolution as its own flow, search
 * history bound to focus, and the wide-window grid paths.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchScreen(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onPlayQueue: (List<Song>, Song) -> Unit = { _, song -> onSongClick(song) },
    /** Long-press a song result: opens the shared song options sheet. */
    onSongLongPress: ((Song) -> Unit)? = null,
    /** Queue a video result, next or at the end. */
    onEnqueueVideo: ((VideoItem, Boolean) -> Unit)? = null,
    /**
     * Play a single YouTube result and continue into its radio. Used instead of
     * [onPlayQueue] for loose result lists, where the neighbouring entries are
     * other uploads of the same title rather than a playlist worth queueing.
     */
    onPlayRadio: (Song) -> Unit = { song -> onPlayQueue(listOf(song), song) },
    onVideoClick: (VideoItem) -> Unit = {},
    onArtistClick: (ArtistItem) -> Unit = {},
    onAlbumClick: (PlaylistDisplayItem) -> Unit = {},
    onPlaylistClick: (PlaylistDisplayItem) -> Unit = {},
    onVideoPlaylistClick: (VideoPlaylist) -> Unit = {},
    /** Open a creator's page, from a Channels result or the long-press sheet. */
    onOpenChannel: (String) -> Unit = {},
    onProfileClick: () -> Unit = {},
    contentPadding: PaddingValues,
    viewModel: HomeViewModel,
    isDarkMode: Boolean,
    videoMode: Boolean = false,
    localOnly: Boolean = false,
    /** False while an expanded player covers this freshly composed screen. */
    requestInitialFocus: Boolean = true,
    modifier: Modifier = Modifier,
    /**
     * Hoisted by HomeScreen so the results keep their place across a tab switch
     * and the nav bar can send them back to the top on a re-tap. Still drives
     * the paging trigger below, which is what it was remembered here for.
     */
    listState: androidx.compose.foundation.lazy.LazyListState =
        androidx.compose.foundation.lazy.rememberLazyListState()
) {
    var initialFocusConsumed by remember { mutableStateOf(false) }
    // Saveable, not just remembered: this composable is disposed and rebuilt
    // both on a tab switch (AnimatedContent in HomeScreen only keeps the
    // target tab's subtree composed) and on process death, and a plainly-
    // remembered query silently threw away what was typed either way - see
    // ROADMAP.md, Surviving process death. Only the query and the filters
    // that shape it are saved; the result lists stay plain remember and are
    // re-fetched by the LaunchedEffect below once query is restored, which
    // avoids needing a Parcelable/serializer story for every result type.
    var query by rememberSaveable { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    // Set once a "load more" comes back empty, so scrolling at the bottom of
    // an exhausted result set stops firing requests forever.
    var songResultsExhausted by remember { mutableStateOf(false) }
    var videoResultsExhausted by remember { mutableStateOf(false) }
    var youtubeResults by remember { mutableStateOf<List<Song>>(emptyList()) }
    // Music mode's Videos tab: ordinary YouTube uploads (remixes, covers, fan
    // edits) that YouTube Music does not list, played as songs.
    var videoSongResults by remember { mutableStateOf<List<Song>>(emptyList()) }
    var videoResults by remember { mutableStateOf<List<VideoItem>>(emptyList()) }
    val searchListLayout = com.sentup.android.ui.video.LocalVideoListLayout.current
    var videoPlaylistResults by remember { mutableStateOf<List<VideoPlaylist>>(emptyList()) }
    var channelResults by remember {
        mutableStateOf<List<com.sentup.android.data.SubscribedChannel>>(emptyList())
    }
    var artistResults by remember { mutableStateOf<List<ArtistItem>>(emptyList()) }
    var albumResults by remember { mutableStateOf<List<PlaylistDisplayItem>>(emptyList()) }
    var playlistResults by remember { mutableStateOf<List<PlaylistDisplayItem>>(emptyList()) }
    var selectedCategory by rememberSaveable { mutableStateOf(SearchCategory.SONGS) }
    var selectedVideoCategory by rememberSaveable { mutableStateOf(VideoSearchCategory.VIDEOS) }
    var selectedDateFilter by rememberSaveable { mutableStateOf(VideoSearchDateFilter.ANY) }
    var selectedSort by rememberSaveable { mutableStateOf(VideoSearchSort.RELEVANCE) }

    var visibleLocalCount by remember { mutableIntStateOf(20) }
    val focusManager = LocalFocusManager.current

    // Search history and focus state
    val searchHistory by viewModel.searchHistory.collectAsState()
    // Read here rather than threaded from MainActivity: ThemePreferences
    // instances listen to the same preference file, so a flip in Settings
    // reaches this one, and the alternative is another parameter through a
    // screen that already takes a long list of them.
    val searchContext = LocalContext.current
    val searchThemePreferences = remember(searchContext) {
        com.sentup.android.data.ThemePreferences(searchContext)
    }
    val showRecentSearches by searchThemePreferences.showRecentSearches.collectAsState()
    var isSearchFocused by remember { mutableStateOf(false) }

    // Playlists and albums already kept, so a result the user saved earlier
    // comes back marked instead of inviting them to save it twice. One store
    // behind both, read through each mode's own shape.
    val savedPlaylistIds by viewModel.savedPlaylistIds.collectAsState()
    val savedVideoPlaylistIds by viewModel.savedVideoPlaylistIds.collectAsState()

    // Options sheet (long-press on a video search result), same wiring as the
    // video home feed
    var saveTargetVideo by remember { mutableStateOf<VideoItem?>(null) }

    fun onVideoLongPress(video: VideoItem) {
        saveTargetVideo = video
    }

    // Pasted YouTube link handling: when the query is a URL, resolve it into
    // a directly playable result instead of running a text search.
    val parsedLink = remember(query) { YouTubeLinkParser.parse(query) }
    var linkState by remember { mutableStateOf<LinkLookupState>(LinkLookupState.Idle) }
    var linkRetryToken by remember { mutableIntStateOf(0) }

    // Endless scroll: pull the next page once the bottom of the list is in
    // sight, rather than making the user hunt for a "load more" button. The
    // state itself is a parameter now, hoisted by HomeScreen.
    val isNearListEnd by remember {
        androidx.compose.runtime.derivedStateOf {
            val layout = listState.layoutInfo
            val lastVisible = layout.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            layout.totalItemsCount > 0 && lastVisible >= layout.totalItemsCount - 3
        }
    }

    // Video mode browse state: trending feed doubles as the explore list
    val trendingVideos by viewModel.trendingVideos.collectAsState()
    val isVideoFeedLoading by viewModel.isVideoLoading.collectAsState()
    LaunchedEffect(videoMode) {
        if (videoMode && trendingVideos.isEmpty()) {
            viewModel.loadTrendingVideos()
        }
    }

    // Filter local songs based on query
    val filteredLocalSongs = remember(query, songs) {
        if (query.isEmpty()) songs
        else songs.filter { song ->
            song.title.contains(query, ignoreCase = true) ||
            song.artist.contains(query, ignoreCase = true) ||
            song.album.contains(query, ignoreCase = true)
        }
    }

    // Search YouTube/Videos/Artists/Albums/Playlists when query changes.
    // Local-only mode never fetches: the local library filter below is
    // the entire search experience.
    LaunchedEffect(query, videoMode, selectedCategory, selectedVideoCategory, selectedDateFilter, selectedSort) {
        if (parsedLink != null) {
            // A pasted link is resolved by its own effect below; make sure no
            // stale text-search results linger behind the link result card.
            isLoading = false
            youtubeResults = emptyList()
            videoSongResults = emptyList()
            videoResults = emptyList()
            videoPlaylistResults = emptyList()
            channelResults = emptyList()
            artistResults = emptyList()
            albumResults = emptyList()
            playlistResults = emptyList()
            return@LaunchedEffect
        }
        if (query.length >= 2 && !localOnly) {
            // A category switch is not typing. The debounce exists to stop a
            // request per keystroke, and it was also being paid every time
            // somebody tapped between Songs and Artists - half a second of
            // nothing before a search that, below, usually needs no network at
            // all. The spinner is skipped on the same condition, because a
            // result already in memory would show one for a single frame.
            val cached = viewModel.hasCachedSearch(
                query = query,
                videoMode = videoMode,
                category = if (videoMode) {
                    selectedVideoCategory.name
                } else {
                    selectedCategory.name
                },
                dateFilter = selectedDateFilter,
                sort = selectedSort
            )
            if (!cached) {
                isLoading = true
                delay(500) // Debounce
            }

            // Clear previous results of other types
            youtubeResults = emptyList()
            videoSongResults = emptyList()
            videoResults = emptyList()
            videoPlaylistResults = emptyList()
            channelResults = emptyList()
            artistResults = emptyList()
            albumResults = emptyList()
            playlistResults = emptyList()
            // A new query gets a fresh pagination cursor in the repository,
            // so the exhausted flags have to come off with it.
            songResultsExhausted = false
            videoResultsExhausted = false

            if (videoMode) {
                when (selectedVideoCategory) {
                    VideoSearchCategory.VIDEOS -> videoResults = viewModel.searchVideos(query, selectedDateFilter, selectedSort)
                    VideoSearchCategory.PLAYLISTS -> videoPlaylistResults = viewModel.searchVideoPlaylists(query)
                    VideoSearchCategory.CHANNELS -> channelResults = viewModel.searchChannels(query)
                }
            } else {
                when (selectedCategory) {
                    SearchCategory.SONGS -> youtubeResults = viewModel.searchYouTube(query)
                    SearchCategory.VIDEOS -> videoSongResults = viewModel.searchVideos(query).asPlayableSongs()
                    SearchCategory.ARTISTS -> artistResults = viewModel.searchArtists(query)
                    SearchCategory.ALBUMS -> albumResults = viewModel.searchAlbums(query)
                    SearchCategory.PLAYLISTS -> playlistResults = viewModel.searchPlaylists(query)
                }
            }
            isLoading = false
        } else {
            isLoading = false
            youtubeResults = emptyList()
            videoSongResults = emptyList()
            videoResults = emptyList()
            videoPlaylistResults = emptyList()
            channelResults = emptyList()
            artistResults = emptyList()
            albumResults = emptyList()
            playlistResults = emptyList()
        }
    }

    // Endless scroll driver. Keyed on the result sizes as well as the scroll
    // position so that a page which lands while the bottom is still in view
    // immediately pulls the next one, instead of stalling until the user
    // nudges the list.
    LaunchedEffect(
        isNearListEnd, isLoading, youtubeResults.size, videoResults.size, videoSongResults.size,
        query, videoMode, selectedCategory, selectedVideoCategory, selectedDateFilter, selectedSort
    ) {
        if (!isNearListEnd || isLoading || isLoadingMore) return@LaunchedEffect
        if (localOnly || parsedLink != null || query.length < 2) return@LaunchedEffect

        val loadingVideos = videoMode && selectedVideoCategory == VideoSearchCategory.VIDEOS
        val loadingSongs = !videoMode && selectedCategory == SearchCategory.SONGS
        val loadingVideoSongs = !videoMode && selectedCategory == SearchCategory.VIDEOS
        try {
            when {
                loadingVideos && videoResults.isNotEmpty() && !videoResultsExhausted -> {
                    isLoadingMore = true
                    val more = viewModel.loadMoreVideoResults(query, selectedDateFilter, selectedSort)
                    if (more.isEmpty()) {
                        videoResultsExhausted = true
                    } else {
                        // Adjacent pages can overlap; never show a video twice
                        val merged = (videoResults + more).distinctBy { it.videoId }
                        // Nothing new survived de-duplication, so the feed is
                        // repeating itself - treat that as the end.
                        if (merged.size == videoResults.size) videoResultsExhausted = true
                        videoResults = merged
                    }
                }
                loadingVideoSongs && videoSongResults.isNotEmpty() && !videoResultsExhausted -> {
                    isLoadingMore = true
                    val more = viewModel.loadMoreVideoResults(query).asPlayableSongs()
                    val merged = (videoSongResults + more).distinctBy { it.id }
                    if (merged.size == videoSongResults.size) videoResultsExhausted = true
                    videoSongResults = merged
                }
                loadingSongs && youtubeResults.isNotEmpty() && !songResultsExhausted -> {
                    isLoadingMore = true
                    val more = viewModel.loadMoreResults(query)
                    if (more.isEmpty()) {
                        songResultsExhausted = true
                    } else {
                        val merged = (youtubeResults + more).distinctBy { it.id }
                        if (merged.size == youtubeResults.size) songResultsExhausted = true
                        youtubeResults = merged
                    }
                }
            }
        } finally {
            isLoadingMore = false
        }
    }

    saveTargetVideo?.let { video ->
        VideoOptionsSheetHost(
            video = video,
            viewModel = viewModel,
            onDismiss = { saveTargetVideo = null },
            onEnqueue = onEnqueueVideo?.let { enqueue -> { next -> enqueue(video, next) } },
            // Search results are never filtered - searching is explicit
            // intent - so "not interested" would visibly do nothing here and
            // is left out. Blocking the channel still has a real effect on
            // every feed, and the undo snackbar says so.
            allowNotInterested = false,
            onOpenChannel = onOpenChannel
        )
    }

    // Reset visible count when query changes
    LaunchedEffect(query) {
        visibleLocalCount = 20
    }

    // Resolve a pasted YouTube link (video, playlist or channel) off the normal
    // search path. A watch link resolves through one watch-next call; a playlist
    // link loads its items. Retriggered by the retry button via linkRetryToken.
    LaunchedEffect(parsedLink, videoMode, linkRetryToken) {
        if (parsedLink == null) {
            linkState = LinkLookupState.Idle
            return@LaunchedEffect
        }
        // A channel link has nothing to preview - there is no track to play and
        // no list to show - so it opens the creator's page instead of resolving
        // into a result card. The query is cleared on the way out so coming back
        // to search does not immediately reopen it.
        parsedLink.channelRef?.let { ref ->
            linkState = LinkLookupState.Idle
            query = ""
            onOpenChannel(ref)
            return@LaunchedEffect
        }
        linkState = LinkLookupState.Resolving
        // A playlist link points at something with a page of its own, exactly
        // like the channel link above, so it opens that page instead of
        // resolving into a result card. The page is where the title, the
        // author, the artwork and - the reason this matters - the Save button
        // are; the card had none of them, so a playlist arriving by link was
        // the one that could not be kept.
        //
        // A link that also names a video still resolves to that video: naming
        // one is asking for it. And a list with no page behind it - a generated
        // mix answers "This playlist type is unviewable" - falls through to the
        // preview below, which for a mix is the whole of what there is to show.
        val linkPlaylistId = parsedLink.playlistId
        if (linkPlaylistId != null && parsedLink.videoId == null) {
            val page = viewModel.resolvePlaylistPageFromLink(linkPlaylistId)
            if (page != null) {
                linkState = LinkLookupState.Idle
                // Cleared for the same reason the channel branch clears it:
                // coming back from the page must land on search, not reopen
                // what was just backed out of.
                query = ""
                if (videoMode) onVideoPlaylistClick(page.toVideoPlaylist())
                else onPlaylistClick(page.toDisplayItem())
                return@LaunchedEffect
            }
        }
        linkState = when {
            parsedLink.videoId != null -> {
                val video = viewModel.resolveVideoFromLink(parsedLink.videoId)
                if (video == null) LinkLookupState.Error
                else LinkLookupState.MediaResult(video)
            }
            parsedLink.playlistId != null && videoMode -> {
                val videos = viewModel.resolvePlaylistVideosFromLink(parsedLink.playlistId)
                if (videos.isEmpty()) LinkLookupState.Error
                else LinkLookupState.PlaylistVideosResult(videos)
            }
            parsedLink.playlistId != null -> {
                val playlistSongs = viewModel.resolvePlaylistSongsFromLink(parsedLink.playlistId)
                if (playlistSongs.isEmpty()) LinkLookupState.Error
                else LinkLookupState.PlaylistSongsResult(playlistSongs)
            }
            else -> LinkLookupState.Idle
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .sentupPage()
    ) {
        // Wide windows: results in columns rather than one long strip, and
        // the page centred past a reading width so a desktop-sized window does
        // not put a result's title a metre from its play button.
        val searchWide = maxWidth >= SEARCH_WIDE_MIN_WIDTH
        val pageSidePadding = ((maxWidth - SEARCH_PAGE_MAX_WIDTH) / 2).coerceAtLeast(0.dp)
        val pageWidth = maxWidth - pageSidePadding * 2
        val artistColumns = if (searchWide) (pageWidth / 200.dp).toInt().coerceIn(3, 7) else 2
        val collectionColumns = if (searchWide) (pageWidth / 420.dp).toInt().coerceIn(2, 3) else 1
        val songGridColumns = (pageWidth / 220.dp).toInt().coerceIn(2, 5)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = pageSidePadding,
                end = pageSidePadding,
                bottom = contentPadding.calculateBottomPadding() + 140.dp
            )
        ) {
            // The large title and the floating pill field. The title stands
            // while the field is idle and steps aside the moment it focuses.
            item {
                SearchScreenHeader(
                    query = query,
                    onQueryChange = { query = it },
                    onFocusChanged = { isSearchFocused = it },
                    onSearch = {
                        if (it.isNotBlank()) {
                            viewModel.addToSearchHistory(it)
                            focusManager.clearFocus()
                        }
                    },
                    placeholderText = if (videoMode) stringResource(R.string.search_placeholder_videos)
                    else stringResource(R.string.search_placeholder_music),
                    isLinkDetected = parsedLink != null,
                    requestInitialFocus = requestInitialFocus && !initialFocusConsumed,
                    onInitialFocusConsumed = { initialFocusConsumed = true }
                )
            }

            // Category pills (music mode only; pointless in local-only and
            // hidden while a pasted link resolves). A scrollable strip of
            // pills replaces the old connected button group: five labels fit
            // a phone only when each gets its own natural width.
            if (!videoMode && !localOnly && query.isNotEmpty() && parsedLink == null) {
                item {
                    SearchPillRow(
                        options = SearchCategory.entries.map { category ->
                            when (category) {
                                SearchCategory.SONGS -> stringResource(R.string.cat_songs)
                                SearchCategory.VIDEOS -> stringResource(R.string.cat_videos)
                                SearchCategory.ARTISTS -> stringResource(R.string.cat_artists)
                                SearchCategory.ALBUMS -> stringResource(R.string.cat_albums)
                                SearchCategory.PLAYLISTS -> stringResource(R.string.cat_playlists)
                            }
                        },
                        selectedIndex = selectedCategory.ordinal,
                        onSelect = { selectedCategory = SearchCategory.entries[it] },
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }
            }

            // Video Mode filters, hidden while a pasted link resolves: the
            // Videos / Playlists / Channels toggle, plus upload-date + sort
            // pills that only apply to video results
            if (videoMode && query.isNotEmpty() && parsedLink == null) {
                item {
                    SearchPillRow(
                        options = VideoSearchCategory.entries.map { category ->
                            when (category) {
                                VideoSearchCategory.VIDEOS -> stringResource(R.string.cat_videos)
                                VideoSearchCategory.PLAYLISTS -> stringResource(R.string.cat_playlists)
                                VideoSearchCategory.CHANNELS -> stringResource(R.string.cat_channels)
                            }
                        },
                        selectedIndex = selectedVideoCategory.ordinal,
                        onSelect = { selectedVideoCategory = VideoSearchCategory.entries[it] },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (selectedVideoCategory == VideoSearchCategory.VIDEOS) {
                    item {
                        SearchPillRow(
                            options = VideoSearchDateFilter.entries.map { it.label },
                            selectedIndex = selectedDateFilter.ordinal,
                            onSelect = { selectedDateFilter = VideoSearchDateFilter.entries[it] },
                            modifier = Modifier.padding(top = 6.dp)
                        )
                    }
                    item {
                        SearchPillRow(
                            options = VideoSearchSort.entries.map { it.label },
                            selectedIndex = selectedSort.ordinal,
                            onSelect = { selectedSort = VideoSearchSort.entries[it] },
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                        )
                    }
                }
            }

            // ========== CONTENT ==========
            when {
                // Pasted YouTube link: resolve it into a playable hero result
                // instead of running a text search.
                parsedLink != null -> {
                    item { LinkDetectedBanner(state = linkState) }
                    when (val state = linkState) {
                        is LinkLookupState.MediaResult -> {
                            item {
                                val video = state.video
                                val subtitle = listOfNotNull(
                                    video.channelName.takeUnless(::isMissingChannelName),
                                    video.viewCount.takeIf { it.isNotBlank() },
                                    video.uploadedDate
                                ).joinToString(" • ")
                                LinkHeroCard(
                                    title = video.title,
                                    subtitle = subtitle,
                                    thumbnailUrl = video.highResThumbnailUrl ?: video.thumbnailUrl,
                                    durationText = video.formattedDuration.takeIf { it.isNotBlank() },
                                    badgeText = if (videoMode) stringResource(R.string.badge_video) else stringResource(R.string.badge_song),
                                    onPlay = {
                                        if (videoMode) {
                                            onVideoClick(video)
                                        } else {
                                            onPlayRadio(video.toSong())
                                        }
                                    }
                                )
                            }
                        }

                        is LinkLookupState.PlaylistSongsResult -> {
                            item {
                                LinkPlaylistHeader(
                                    count = state.songs.size,
                                    isVideos = false,
                                    thumbnailUrl = state.songs.firstOrNull()?.highResThumbnailUrl
                                        ?: state.songs.firstOrNull()?.thumbnailUrl,
                                    onPlayAll = { onPlayQueue(state.songs, state.songs.first()) }
                                )
                            }
                            val linkSongKeys = queueRowKeys(state.songs.map { it.id }, "sr-link-songs")
                            itemsIndexed(state.songs, key = { index, _ -> linkSongKeys[index] }) { index, song ->
                                SearchSongRow(
                                    song = song,
                                    index = index,
                                    count = state.songs.size,
                                    onClick = { onPlayQueue(state.songs, song) },
                                    onLongClick = onSongLongPress?.let { press -> { press(song) } }
                                )
                            }
                        }

                        is LinkLookupState.PlaylistVideosResult -> {
                            item {
                                LinkPlaylistHeader(
                                    count = state.videos.size,
                                    isVideos = true,
                                    thumbnailUrl = state.videos.firstOrNull()?.thumbnailUrl,
                                    onPlayAll = { onVideoClick(state.videos.first()) }
                                )
                            }
                            val linkVideoKeys = queueRowKeys(state.videos.map { it.videoId }, "sr-link-videos")
                            itemsIndexed(state.videos, key = { index, _ -> linkVideoKeys[index] }) { index, video ->
                                CompactVideoRow(
                                    video = video,
                                    onClick = { onVideoClick(video) },
                                    index = index,
                                    count = state.videos.size
                                )
                            }
                        }

                        LinkLookupState.Error -> {
                            item { LinkErrorCard(onRetry = { linkRetryToken++ }) }
                        }

                        else -> {
                            item { LinkResolvingCard(videoMode = videoMode) }
                        }
                    }
                }

                // Search history belongs to the field, not to either media
                // mode. While it has focus, keep recent queries in the same
                // predictable place and reveal the mode's browse surface again
                // as soon as focus clears.
                isSearchFocused && query.isEmpty() && showRecentSearches &&
                    searchHistory.isNotEmpty() -> {
                    item {
                        // A list of short queries across a whole tablet puts
                        // each one's remove button a screen away from it; on
                        // a wide window it is a reading column instead.
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            Box(
                                modifier = Modifier
                                    .widthIn(max = SEARCH_HISTORY_MAX_WIDTH)
                                    .fillMaxWidth()
                            ) {
                                SearchHistorySection(
                                    history = searchHistory,
                                    onHistoryClick = { term ->
                                        query = term
                                        focusManager.clearFocus()
                                    },
                                    onRemoveClick = { viewModel.removeFromSearchHistory(it) },
                                    onClearAll = { viewModel.clearSearchHistory() }
                                )
                            }
                        }
                    }
                }

                isLoading -> {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                LoadingIndicator(
                                    modifier = Modifier.size(42.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    when {
                                        videoMode && selectedVideoCategory == VideoSearchCategory.PLAYLISTS -> stringResource(R.string.searching_playlists)
                                        videoMode || selectedCategory == SearchCategory.VIDEOS -> stringResource(R.string.searching_videos)
                                        selectedCategory == SearchCategory.ARTISTS -> stringResource(R.string.searching_artists)
                                        selectedCategory == SearchCategory.ALBUMS -> stringResource(R.string.searching_albums)
                                        selectedCategory == SearchCategory.PLAYLISTS -> stringResource(R.string.searching_playlists)
                                        else -> stringResource(R.string.searching_music)
                                    },
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Video mode browse: trending instead of the music library.
                // An empty feed that is not loading stays empty: recommendations
                // are switched off, the phone is offline, or the request came
                // back with nothing. Spinning there never ended.
                videoMode && query.isEmpty() && trendingVideos.isEmpty() && !isVideoFeedLoading -> {
                    item {
                        SearchStateMessage(
                            icon = Icons.Rounded.Search,
                            message = stringResource(R.string.search_video_idle_hint)
                        )
                    }
                }

                videoMode && query.isEmpty() -> {
                    item {
                        SentupGroupHeader(
                            text = stringResource(R.string.trending_now),
                            modifier = Modifier.padding(top = SentupGroup.gap)
                        )
                    }

                    if (trendingVideos.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(160.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                LoadingIndicator(
                                    modifier = Modifier.size(36.dp),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    } else {
                        val trendingVideoKeys = queueRowKeys(trendingVideos.map { it.videoId }, "sr-trending")
                        itemsIndexed(trendingVideos, key = { index, _ -> trendingVideoKeys[index] }) { index, video ->
                            CompactVideoRow(
                                video = video,
                                onClick = { onVideoClick(video) },
                                index = index,
                                count = trendingVideos.size
                            )
                        }
                    }
                }

                query.isEmpty() -> {
                    // The local library is the browse surface until a query
                    // arrives; the pill field above is already the focal
                    // point, so this opens with a group header, not a second
                    // title.
                    item {
                        SentupGroupHeader(
                            text = stringResource(R.string.browse_your_library),
                            modifier = Modifier.padding(top = SentupGroup.gap)
                        )
                    }

                    val displaySongs = songs.take(visibleLocalCount)
                    val hasMoreLocal = songs.size > visibleLocalCount

                    val browseSongKeys = queueRowKeys(displaySongs.map { it.id }, "sr-browse-songs")
                    itemsIndexed(displaySongs, key = { index, _ -> browseSongKeys[index] }) { index, song ->
                        SearchSongRow(
                            song = song,
                            index = index,
                            count = displaySongs.size,
                            continues = hasMoreLocal,
                            onClick = { onPlayQueue(songs, song) },
                            onLongClick = onSongLongPress?.let { press -> { press(song) } }
                        )
                    }

                    if (hasMoreLocal) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                SearchPillButton(stringResource(R.string.action_show_more)) {
                                    visibleLocalCount += 20
                                }
                            }
                        }
                    }
                }

                // Video Mode Results: the pill strip above decides which
                // search ran, so only one of these lists is ever populated.
                videoMode && (
                    videoResults.isNotEmpty() ||
                        videoPlaylistResults.isNotEmpty() ||
                        channelResults.isNotEmpty()
                    ) -> {
                    if (channelResults.isNotEmpty()) {
                        item { SearchResultHeader(stringResource(R.string.cat_channels), channelResults.size) }
                        itemsIndexed(channelResults, key = { _, channel -> channel.channelId }) { index, channel ->
                            ChannelResultRow(
                                channel = channel,
                                index = index,
                                count = channelResults.size,
                                onClick = {
                                    viewModel.addToSearchHistory(query)
                                    onOpenChannel(channel.channelId)
                                }
                            )
                        }
                    }

                    if (videoPlaylistResults.isNotEmpty()) {
                        item { SearchResultHeader(stringResource(R.string.cat_playlists), videoPlaylistResults.size) }
                        itemsIndexed(videoPlaylistResults, key = { _, playlist -> playlist.playlistId }) { index, playlist ->
                            VideoPlaylistRow(
                                playlist = playlist,
                                index = index,
                                count = videoPlaylistResults.size,
                                onClick = {
                                    viewModel.addToSearchHistory(query)
                                    onVideoPlaylistClick(playlist)
                                },
                                isSaved = savedVideoPlaylistIds.contains(playlist.playlistId),
                                onToggleSave = { viewModel.toggleSavedVideoPlaylist(playlist) }
                            )
                        }
                    }

                    if (videoResults.isNotEmpty()) {
                        item { SearchResultHeader(stringResource(R.string.source_youtube_videos), videoResults.size) }

                        // Display video results; long-press opens the save
                        // sheet. The shared grid decides cells; this only
                        // supplies the row and its gaps.
                        videoListItems(videoResults, searchListLayout) { video, cell ->
                            VideoCard(
                                video = video,
                                onClick = { onVideoClick(video) },
                                onLongClick = { onVideoLongPress(video) },
                                onOpenChannel = onOpenChannel,
                                modifier = cell.padding(vertical = SearchTileGap)
                            )
                        }

                        item {
                            SearchPagingFooter(
                                isLoadingMore = isLoadingMore,
                                isExhausted = videoResultsExhausted
                            )
                        }
                    }
                }

                artistResults.isNotEmpty() -> {
                    item { SearchResultHeader(stringResource(R.string.cat_artists), artistResults.size) }

                    val artistPairs = artistResults.chunked(artistColumns)
                    val artistPairKeys = queueRowKeys(
                        artistPairs.map { it.firstOrNull()?.id.orEmpty() },
                        "sr-artists"
                    )
                    itemsIndexed(artistPairs, key = { index, _ -> artistPairKeys[index] }) { _, pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SentupGroup.inset),
                            horizontalArrangement = Arrangement.spacedBy(SearchTileGap)
                        ) {
                            pair.forEach { artist ->
                                ArtistResultTile(
                                    artist = artist,
                                    onClick = {
                                        viewModel.addToSearchHistory(query)
                                        onArtistClick(artist)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // A short last row keeps the column width.
                            repeat(artistColumns - pair.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                        Spacer(modifier = Modifier.height(SearchTileGap))
                    }
                }

                // --- Album Results ---
                albumResults.isNotEmpty() -> {
                    item { SearchResultHeader(stringResource(R.string.cat_albums), albumResults.size) }

                    val albumRows = albumResults.chunked(collectionColumns)
                    val albumRowKeys = queueRowKeys(
                        albumRows.map { it.firstOrNull()?.id.orEmpty() },
                        "sr-albums"
                    )
                    itemsIndexed(albumRows, key = { index, _ -> albumRowKeys[index] }) { _, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SentupGroup.inset),
                            horizontalArrangement = Arrangement.spacedBy(SearchTileGap)
                        ) {
                            row.forEach { album ->
                                CollectionResultCard(
                                    item = album,
                                    onClick = {
                                        viewModel.addToSearchHistory(query)
                                        onAlbumClick(album)
                                    },
                                    isAlbum = true,
                                    isSaved = savedPlaylistIds.contains(album.id),
                                    onToggleSave = { viewModel.toggleSavedPlaylist(album, isAlbum = true) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(collectionColumns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(modifier = Modifier.height(SearchTileGap))
                    }
                }

                // --- Playlist Results ---
                playlistResults.isNotEmpty() -> {
                    item { SearchResultHeader(stringResource(R.string.cat_playlists), playlistResults.size) }

                    val playlistRows = playlistResults.chunked(collectionColumns)
                    val playlistRowKeys = queueRowKeys(
                        playlistRows.map { it.firstOrNull()?.id.orEmpty() },
                        "sr-playlists"
                    )
                    itemsIndexed(playlistRows, key = { index, _ -> playlistRowKeys[index] }) { _, row ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = SentupGroup.inset),
                            horizontalArrangement = Arrangement.spacedBy(SearchTileGap)
                        ) {
                            row.forEach { playlist ->
                                CollectionResultCard(
                                    item = playlist,
                                    onClick = {
                                        viewModel.addToSearchHistory(query)
                                        onPlaylistClick(playlist)
                                    },
                                    isAlbum = false,
                                    isSaved = savedPlaylistIds.contains(playlist.id),
                                    onToggleSave = { viewModel.toggleSavedPlaylist(playlist) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            repeat(collectionColumns - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(modifier = Modifier.height(SearchTileGap))
                    }
                }

                // Music mode's Videos tab: uploads that play as songs.
                !videoMode && selectedCategory == SearchCategory.VIDEOS && videoSongResults.isNotEmpty() -> {
                    item { SearchResultHeader(stringResource(R.string.cat_videos), videoSongResults.size) }
                    if (searchWide) {
                        wideSongResults(
                            songs = videoSongResults,
                            keyPrefix = "video-songs",
                            columns = songGridColumns,
                            onPlay = { onPlayRadio(it) },
                            onLongPress = onSongLongPress
                        )
                    } else {
                        val videoSongKeys = queueRowKeys(videoSongResults.map { it.id }, "sr-video-songs")
                        itemsIndexed(videoSongResults, key = { index, _ -> videoSongKeys[index] }) { index, song ->
                            SearchSongRow(
                                song = song,
                                index = index,
                                count = videoSongResults.size,
                                // The card stays open-ended while another page
                                // is on its way, and closes when it lands.
                                continues = isLoadingMore,
                                // Radio, as on Songs: an upload's related mix
                                // is what YouTube itself would play after it.
                                onClick = { onPlayRadio(song) },
                                onLongClick = onSongLongPress?.let { press -> { press(song) } }
                            )
                        }
                    }
                    item {
                        SearchPagingFooter(
                            isLoadingMore = isLoadingMore,
                            isExhausted = videoResultsExhausted
                        )
                    }
                }

                youtubeResults.isNotEmpty() -> {
                    item { SearchResultHeader(stringResource(R.string.source_youtube_music), youtubeResults.size) }

                    if (searchWide) {
                        wideSongResults(
                            songs = youtubeResults,
                            keyPrefix = "songs",
                            columns = songGridColumns,
                            onPlay = { onPlayRadio(it) },
                            onLongPress = onSongLongPress
                        )
                    } else {
                        val youtubeSongKeys = queueRowKeys(youtubeResults.map { it.id }, "sr-yt-songs")
                        itemsIndexed(youtubeResults, key = { index, _ -> youtubeSongKeys[index] }) { index, song ->
                            SearchSongRow(
                                song = song,
                                index = index,
                                count = youtubeResults.size,
                                continues = isLoadingMore,
                                // Radio, not the result list: the other hits
                                // are usually the same track from other
                                // uploaders.
                                onClick = { onPlayRadio(song) },
                                onLongClick = onSongLongPress?.let { press -> { press(song) } }
                            )
                        }
                    }

                    // Pages now arrive on scroll; this footer only reports state.
                    item {
                        SearchPagingFooter(
                            isLoadingMore = isLoadingMore,
                            isExhausted = songResultsExhausted
                        )
                    }

                    // Local Library matches section
                    if (filteredLocalSongs.isNotEmpty()) {
                        item {
                            SentupGroupHeader(
                                text = stringResource(R.string.source_local_library),
                                modifier = Modifier.padding(top = SentupGroup.gap),
                                subtitle = pluralStringResource(
                                    R.plurals.n_matches,
                                    filteredLocalSongs.size,
                                    filteredLocalSongs.size
                                )
                            )
                        }

                        val localDisplayed = filteredLocalSongs.take(visibleLocalCount)
                        val hasMoreLocalMatches = filteredLocalSongs.size > visibleLocalCount

                        val localDisplayedKeys = queueRowKeys(localDisplayed.map { it.id }, "sr-local-hit-songs")
                        itemsIndexed(localDisplayed, key = { index, _ -> localDisplayedKeys[index] }) { index, song ->
                            SearchSongRow(
                                song = song,
                                index = index,
                                count = localDisplayed.size,
                                continues = hasMoreLocalMatches,
                                onClick = { onPlayQueue(filteredLocalSongs, song) },
                                onLongClick = onSongLongPress?.let { press -> { press(song) } }
                            )
                        }

                        if (hasMoreLocalMatches) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp, bottom = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    SearchPillButton(stringResource(R.string.action_show_more)) {
                                        visibleLocalCount += 20
                                    }
                                }
                            }
                        }
                    }
                }

                filteredLocalSongs.isEmpty() && youtubeResults.isEmpty() && query.isNotEmpty() -> {
                    // One icon, one sentence, one way back to a clean field.
                    item {
                        SearchStateMessage(
                            icon = Icons.Rounded.Search,
                            message = stringResource(R.string.search_no_results),
                            actionLabel = stringResource(R.string.search_clear_query),
                            onAction = { query = "" }
                        )
                    }
                }

                else -> {
                    // Local search results only (when no YouTube results but
                    // have local matches)
                    item {
                        SentupGroupHeader(
                            text = pluralStringResource(
                                R.plurals.n_results,
                                filteredLocalSongs.size,
                                filteredLocalSongs.size
                            ),
                            modifier = Modifier.padding(top = SentupGroup.gap)
                        )
                    }

                    val displayedLocal = filteredLocalSongs.take(visibleLocalCount)
                    val hasMoreLocal = filteredLocalSongs.size > visibleLocalCount

                    val displayedLocalKeys = queueRowKeys(displayedLocal.map { it.id }, "sr-local-songs")
                    itemsIndexed(displayedLocal, key = { index, _ -> displayedLocalKeys[index] }) { index, song ->
                        SearchSongRow(
                            song = song,
                            index = index,
                            count = displayedLocal.size,
                            continues = hasMoreLocal,
                            onClick = { onPlayQueue(filteredLocalSongs, song) },
                            onLongClick = onSongLongPress?.let { press -> { press(song) } }
                        )
                    }

                    if (hasMoreLocal) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 16.dp, bottom = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                SearchPillButton(stringResource(R.string.action_show_more)) {
                                    visibleLocalCount += 20
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** What a search can be filtered to in music mode. */
enum class SearchCategory {
    SONGS, VIDEOS, ARTISTS, ALBUMS, PLAYLISTS
}

/** What a search can be filtered to in video mode. */
enum class VideoSearchCategory {
    VIDEOS, PLAYLISTS, CHANNELS
}

/** From this width results go into columns; below it, the phone's one list. */
private val SEARCH_WIDE_MIN_WIDTH = 840.dp

/** The widest the results page grows before it centres. */
private val SEARCH_PAGE_MAX_WIDTH = 1440.dp

/** Recent searches: a reading column, whatever the window. */
private val SEARCH_HISTORY_MAX_WIDTH = 720.dp

/** The gap between cards in the wide result grids. */
private val SearchTileGap = 12.dp

/**
 * The screen's opening: iOS large title above a search field that floats as
 * its own surface - inset from the edges, pill-round, lifted on a tinted
 * shadow with a specular hairline along its top edge. The title is what the
 * field interrupts: it steps aside (fade + collapse) the moment the field
 * takes focus, and returns when the field is left empty.
 *
 * Voice search stays a first-class button while the field is empty and yields
 * to a clear button the moment there is something to clear; a detected
 * YouTube URL morphs the magnifier into a link, because at that point the
 * field holds an address, not a query.
 */
@Composable
private fun SearchScreenHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    onSearch: (String) -> Unit,
    placeholderText: String,
    isLinkDetected: Boolean,
    requestInitialFocus: Boolean,
    onInitialFocusConsumed: () -> Unit
) {
    // Once per visit: this header is a lazy item, so it re-enters composition
    // on scroll and relayout, and each re-entry used to pop the keyboard.
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val stillWanted by rememberUpdatedState(requestInitialFocus)
    LaunchedEffect(Unit) {
        if (!requestInitialFocus) return@LaunchedEffect
        delay(150)
        if (!stillWanted) return@LaunchedEffect
        onInitialFocusConsumed()
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    val context = LocalContext.current
    val speechLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val spoken = result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
        if (!spoken.isNullOrBlank()) {
            onQueryChange(spoken)
            onSearch(spoken)
        }
        // A cancelled or empty recognition leaves the field exactly as it was:
        // nothing to clear, nothing to search, nothing to explain.
    }

    fun launchVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_PROMPT, "What do you want to listen to?")
        }
        try {
            speechLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            // No recognizer on the device. A toast beats a silent dead button,
            // and this is the one voice-search failure that is a device fact.
            Toast.makeText(context, "Voice search is not available on this device", Toast.LENGTH_SHORT).show()
        }
    }

    val pillShape = RoundedCornerShape(SentupRadii.pill)
    val searchInteraction = remember { MutableInteractionSource() }
    val isFieldFocused by searchInteraction.collectIsFocusedAsState()
    LaunchedEffect(isFieldFocused) { onFocusChanged(isFieldFocused) }
    val titleVisible = !isFieldFocused && query.isBlank()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        // The large title. Collapsing the height with the fade keeps the
        // field where the thumb already is when the keyboard arrives.
        AnimatedVisibility(
            visible = titleVisible,
            enter = fadeIn(tween(200)) + scaleIn(initialScale = 0.94f, animationSpec = tween(200)) +
                expandVertically(tween(220)),
            exit = fadeOut(tween(130)) + scaleOut(targetScale = 0.94f, animationSpec = tween(130)) +
                shrinkVertically(tween(160))
        ) {
            Text(
                text = stringResource(R.string.search_title),
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = SentupGroup.inset,
                    end = SentupGroup.inset,
                    top = 14.dp,
                    bottom = 14.dp
                )
            )
        }

        Row(
            modifier = Modifier
                .padding(horizontal = SentupGroup.inset)
                .fillMaxWidth()
                .heightIn(min = 58.dp)
                .sentupCard(shape = pillShape, elevation = 2)
                .sentupSpecularEdge(pillShape)
                .padding(start = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedContent(
                targetState = isLinkDetected,
                transitionSpec = {
                    (scaleIn(
                        initialScale = 0.4f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(tween(120))) togetherWith
                        (scaleOut(targetScale = 0.4f, animationSpec = tween(120)) + fadeOut(tween(120)))
                },
                label = "searchLeadingIcon"
            ) { linkDetected ->
                Icon(
                    if (linkDetected) Icons.Rounded.Link else Icons.Rounded.Search,
                    contentDescription = null,
                    tint = if (linkDetected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .size(42.dp)
                        .padding(10.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Box(modifier = Modifier.weight(1f)) {
                if (query.isEmpty()) {
                    Text(
                        placeholderText,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    interactionSource = searchInteraction,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )
            }

            AnimatedContent(
                targetState = query.isNotEmpty(),
                transitionSpec = {
                    (scaleIn(
                        initialScale = 0.4f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(tween(120))) togetherWith
                        (scaleOut(targetScale = 0.4f, animationSpec = tween(120)) + fadeOut(tween(120)))
                },
                label = "searchTrailingIcon"
            ) { hasQuery ->
                if (hasQuery) {
                    QuietIconButton(
                        icon = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_clear),
                        onClick = { onQueryChange("") }
                    )
                } else {
                    // 48dp touch target with a 40dp tonal cookie inside, so
                    // the visible button stays compact while the tap area
                    // meets the minimum. onPrimaryContainer, not primary:
                    // monochrome palettes fix their containers per palette
                    // while the ink follows the app mode, so primary can
                    // coincide with its container and the icon vanishes.
                    val micInteraction = remember { MutableInteractionSource() }
                    val micPressed by micInteraction.collectIsPressedAsState()
                    val micScale by animateFloatAsState(
                        targetValue = if (micPressed) 0.94f else 1f,
                        animationSpec = tween(90),
                        label = "micPress"
                    )
                    Box(
                        modifier = Modifier.size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .scale(micScale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable(
                                    interactionSource = micInteraction,
                                    indication = null,
                                    onClick = { launchVoiceSearch() }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.KeyboardVoice,
                                contentDescription = stringResource(R.string.cd_voice_search),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

/**
 * A scrollable strip of single-select pills - categories, upload dates, sort
 * orders. The selected pill fills with the emerald primary; the rest stay
 * transparent with a hairline outline, so the choice reads as one object
 * moving along the strip. Pills press inward, never ripple.
 */
@Composable
private fun SearchPillRow(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = SentupGroup.inset),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(options, key = { index, label -> "$index-$label" }) { index, label ->
            SearchPill(
                label = label,
                selected = index == selectedIndex,
                onClick = { onSelect(index) }
            )
        }
    }
}

@Composable
private fun SearchPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(90),
        label = "searchPillPress"
    )
    val fill by animateColorAsState(
        targetValue = if (selected) scheme.primary else Color.Transparent,
        animationSpec = tween(160),
        label = "searchPillFill"
    )
    val ink by animateColorAsState(
        targetValue = if (selected) scheme.onPrimary else scheme.onSurfaceVariant,
        animationSpec = tween(160),
        label = "searchPillInk"
    )
    val outline by animateColorAsState(
        targetValue = if (selected) Color.Transparent else scheme.onSurfaceVariant.copy(alpha = 0.22f),
        animationSpec = tween(160),
        label = "searchPillOutline"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(SentupRadii.pill))
            .background(fill)
            .border(1.dp, outline, RoundedCornerShape(SentupRadii.pill))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = ink,
            maxLines = 1
        )
    }
}

/**
 * The header above one result type. Small, bold, sentence-cased, with the
 * count as its subtitle - the group's name and size in one quiet line, with
 * the card of rows starting under it.
 */
@Composable
private fun SearchResultHeader(title: String, count: Int) {
    SentupGroupHeader(
        text = title,
        modifier = Modifier.padding(top = SentupGroup.gap),
        subtitle = pluralStringResource(R.plurals.n_results, count, count)
    )
}

/**
 * A state that is not a result: loading, nothing found, nothing to browse.
 * One quiet icon, one sentence, and at most one emerald pill to act on.
 * The old grey stack of a 100dp circle, a title and a hint line read as an
 * error report; this reads as a signpost.
 */
@Composable
private fun SearchStateMessage(
    icon: ImageVector,
    message: String,
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset)
            .padding(top = 56.dp, bottom = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .clip(CircleShape)
                .background(scheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.size(36.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = scheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (actionLabel != null && onAction != null) {
            Spacer(modifier = Modifier.height(22.dp))
            SearchPillButton(actionLabel, onClick = onAction)
        }
    }
}

/** The one action button this screen offers anywhere outside a row: an emerald pill that presses inward. */
@Composable
private fun SearchPillButton(
    label: String,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(90),
        label = "pillButtonPress"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(SentupRadii.pill))
            .background(scheme.primary)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onPrimary
        )
    }
}

/** A 40dp icon button that presses inward and never flashes. */
@Composable
private fun QuietIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    size: Dp = 40.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.9f else 1f,
        animationSpec = tween(90),
        label = "quietButtonPress"
    )
    Box(
        modifier = modifier
            .size(size)
            .scale(scale)
            .clip(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * One row of a group card, drawn across the LazyColumn instead of inside a
 * Column: every row carries the card's fill, the first owns its rounded top,
 * the last its rounded bottom, and [continues] leaves the bottom square
 * while pagination still has another page coming. The hairlines between rows
 * come from [SentupDivider], inset to the text exactly as they are inside a
 * real [SentupGroupCard] - a paginated list of lazy rows has to build the
 * card out of its rows.
 */
@Composable
private fun Modifier.searchGroupSegment(
    index: Int,
    count: Int,
    continues: Boolean = false
): Modifier {
    val radius = SentupRadii.card
    val closesGroup = index == count - 1 && !continues
    val shape = when {
        index == 0 && closesGroup -> RoundedCornerShape(radius)
        index == 0 -> RoundedCornerShape(topStart = radius, topEnd = radius)
        closesGroup -> RoundedCornerShape(bottomStart = radius, bottomEnd = radius)
        else -> RectangleShape
    }
    return this
        .padding(horizontal = SentupGroup.inset)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
}

/**
 * Press feedback for a whole row or tile: it yields inward at 0.97 and
 * springs back. A tint would repaint the row and its artwork as a flash of
 * colour; a transform costs nothing and reads as the object itself being
 * pressed. Long press keeps the shared confirm haptic.
 */
@Composable
private fun Modifier.searchRowPress(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
): Modifier {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(90),
        label = "searchRowPress"
    )
    val haptics = rememberSentupHaptics()
    return this
        .scale(scale)
        .then(
            if (onLongClick == null) {
                Modifier.clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                )
            } else {
                Modifier.combinedClickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick,
                    onLongClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
            }
        )
}

/**
 * A song inside a result group: artwork tile, title and artist, a quiet play
 * mark, inset hairline above. The row is the card's row - fill, rounded ends
 * and press all come from the group helpers, so this list and a settings
 * list are the same object.
 */
@Composable
private fun SearchSongRow(
    song: Song,
    index: Int,
    count: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    continues: Boolean = false
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .searchGroupSegment(index = index, count = count, continues = continues)
            .searchRowPress(onClick = onClick, onLongClick = onLongClick)
    ) {
        if (index > 0) SentupDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SentupGroup.dividerInset,
                    end = SentupGroup.dividerInset,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(SentupRadii.tile))
                    .background(scheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (song.albumArtUri != null || song.thumbnailUrl != null) {
                    AsyncImage(
                        model = song.highResThumbnailUrl ?: song.albumArtUri ?: song.thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(SentupRadii.tile)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = scheme.primary.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = song.title.takeIf { !isUnknownTitle(it) }
                        ?: stringResource(R.string.untitled_song),
                    color = scheme.onSurface,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = song.artist.takeIf { !isUnknownArtist(it) }
                        ?: stringResource(R.string.unknown_artist),
                    color = scheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                Icons.Rounded.PlayArrow,
                contentDescription = stringResource(R.string.cd_play),
                tint = scheme.onSurfaceVariant.copy(alpha = 0.65f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * Song results on a wide window: a grid of artwork tiles. Each tile is its
 * own small card (one step of lift, continuous corners) and the grid's gaps
 * are uniform, so breadth reads as room rather than as a stretched list.
 * Paging is unchanged: it fires when the last visible row nears the end of
 * the list, which rows of several reach the same way rows of one do.
 */
private fun androidx.compose.foundation.lazy.LazyListScope.wideSongResults(
    songs: List<Song>,
    keyPrefix: String,
    columns: Int,
    onPlay: (Song) -> Unit,
    onLongPress: ((Song) -> Unit)?
) {
    if (songs.isEmpty()) return
    val rows = songs.chunked(columns.coerceAtLeast(1))
    itemsIndexed(rows, key = { index, _ -> "$keyPrefix-row-$index" }) { _, row ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset),
            horizontalArrangement = Arrangement.spacedBy(SearchTileGap)
        ) {
            row.forEach { song ->
                SearchSongTile(
                    song = song,
                    onPlay = { onPlay(song) },
                    onLongClick = onLongPress?.let { press -> { press(song) } },
                    modifier = Modifier.weight(1f)
                )
            }
            // A short last row keeps the column width.
            repeat((columns - row.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
        }
        Spacer(modifier = Modifier.height(SearchTileGap))
    }
}

@Composable
private fun SearchSongTile(
    song: Song,
    onPlay: () -> Unit,
    onLongClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val tileShape = RoundedCornerShape(SentupRadii.tile)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .sentupCard(shape = tileShape, elevation = 1)
            .searchRowPress(onClick = onPlay, onLongClick = onLongClick)
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(tileShape)
                .background(scheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            if (song.albumArtUri != null || song.thumbnailUrl != null) {
                AsyncImage(
                    model = song.highResThumbnailUrl ?: song.albumArtUri ?: song.thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(tileShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = scheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = song.title.takeIf { !isUnknownTitle(it) }
                ?: stringResource(R.string.untitled_song),
            color = scheme.onSurface,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(1.dp))
        Text(
            text = song.artist.takeIf { !isUnknownArtist(it) }
                ?: stringResource(R.string.unknown_artist),
            color = scheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * An artist in a result grid: the portrait as a circle, name under it, and
 * the facts a chooser cares about - verified, subscribers - as one quiet
 * line. The random organic shapes this used to wear made fifteen artists
 * look like fifteen different apps.
 */
@Composable
private fun ArtistResultTile(
    artist: ArtistItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .searchRowPress(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(CircleShape)
                .background(scheme.surfaceVariant)
        ) {
            // 140dp of picture asked for at whatever size the search
            // response returned - usually =s48, blown up nine times. This
            // requests one worth drawing and keeps the original underneath.
            com.sentup.android.ui.components.AvatarImage(
                url = artist.thumbnailUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                indicatorSize = 24.dp,
                placeholderColor = scheme.surfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = artist.name,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (artist.isVerified || !artist.subscriberCount.isNullOrEmpty()) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (artist.isVerified) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = stringResource(R.string.cd_verified),
                        tint = scheme.primary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.size(4.dp))
                }
                if (!artist.subscriberCount.isNullOrEmpty()) {
                    Text(
                        text = "${artist.subscriberCount}",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * An album or playlist in a result grid: a card of its own at the standard
 * one step of lift. The save toggle stays a visible button rather than a
 * long-press - nothing else in these results hides an action behind a hold.
 */
@Composable
private fun CollectionResultCard(
    item: PlaylistDisplayItem,
    onClick: () -> Unit,
    isAlbum: Boolean,
    isSaved: Boolean?,
    onToggleSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .fillMaxWidth()
            .sentupCard(
                shape = RoundedCornerShape(SentupRadii.card),
                elevation = 1
            )
            .searchRowPress(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(
                    if (isAlbum) RoundedCornerShape(SentupRadii.tile)
                    else RoundedCornerShape(SentupRadii.card / 2)
                )
                .background(scheme.surfaceVariant)
        ) {
            AsyncImage(
                model = item.thumbnailUrl ?: "",
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isAlbum) Icons.Rounded.Album else Icons.Rounded.QueueMusic,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.size(6.dp))
                val metadata = if (isAlbum) {
                    if (item.hasReleaseMetadata) item.displaySubtitle()
                    else stringResource(
                        R.string.album_metadata,
                        item.uploaderName.ifBlank { stringResource(R.string.unknown_artist) },
                    )
                } else {
                    buildList {
                        add(stringResource(R.string.label_playlist))
                        item.uploaderName.takeIf { it.isNotBlank() }?.let(::add)
                        item.itemCount.takeIf { it >= 0 }?.let { count ->
                            add(pluralStringResource(R.plurals.n_songs, count, count))
                        }
                    }.joinToString(" • ")
                }
                Text(
                    text = metadata,
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (isSaved != null) {
            val haptics = rememberSentupHaptics()
            QuietIconButton(
                icon = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                contentDescription = if (isSaved) stringResource(R.string.cd_remove_from_library)
                else stringResource(R.string.cd_save_to_library),
                tint = if (isSaved) scheme.primary else scheme.onSurfaceVariant,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                    onToggleSave()
                }
            )
        }
    }
}

/**
 * Recent searches: one inset group card, each query a row with the clock on
 * its left and a full-size remove target on its right. The header carries a
 * single "clear all" action - one place to empty the list, not a button per
 * row plus a button for all of them dressed differently.
 */
@Composable
private fun SearchHistorySection(
    history: List<String>,
    onHistoryClick: (String) -> Unit,
    onRemoveClick: (String) -> Unit,
    onClearAll: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SentupGroupHeader(
                text = stringResource(R.string.recent_searches),
                modifier = Modifier.weight(1f)
            )
            val clearInteraction = remember { MutableInteractionSource() }
            Text(
                text = stringResource(R.string.action_clear_all),
                style = MaterialTheme.typography.labelLarge,
                color = scheme.primary,
                modifier = Modifier
                    .padding(end = SentupGroup.inset + SentupGroup.dividerInset)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = clearInteraction,
                        indication = null,
                        onClick = onClearAll
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        SentupGroupCard {
            history.forEachIndexed { index, term ->
                if (index > 0) SentupDivider()
                SentupRow(
                    title = term,
                    icon = Icons.Rounded.History,
                    onClick = { onHistoryClick(term) },
                    trailing = {
                        QuietIconButton(
                            icon = Icons.Rounded.Close,
                            contentDescription = stringResource(R.string.cd_remove),
                            onClick = { onRemoveClick(term) }
                        )
                    }
                )
            }
        }
    }
}

// ============================================================
// Pasted YouTube link UI
// ============================================================

/** UI state for a pasted YouTube link being resolved into playable content. */
private sealed interface LinkLookupState {
    data object Idle : LinkLookupState
    data object Resolving : LinkLookupState

    /** A watch/shorts/youtu.be link resolved to one video (or song in music mode). */
    data class MediaResult(val video: VideoItem) : LinkLookupState

    /** A playlist link resolved in music mode. */
    data class PlaylistSongsResult(val songs: List<Song>) : LinkLookupState

    /** A playlist link resolved in video mode. */
    data class PlaylistVideosResult(val videos: List<VideoItem>) : LinkLookupState

    data object Error : LinkLookupState
}

/** Music-mode representation of a video resolved from a pasted link. */
/**
 * Uploads that can play as a song. Live streams are dropped: the music path is
 * progressive, and a live stream is only playable through its HLS manifest.
 */
private fun List<VideoItem>.asPlayableSongs(): List<Song> =
    filter { !it.isLive && it.duration > 0L }.map { it.toSong().copy(isUpload = true) }

private fun VideoItem.toSong(): Song = Song.fromYouTube(
    videoId = videoId,
    title = title,
    artist = channelName,
    album = "",
    duration = duration * 1000,
    thumbnailUrl = thumbnailUrl
)

/** Shared entrance for the link result cards: a fade with a short rise, the
 *  way the platform presents something that arrived rather than scrolled in. */
private fun linkCardEnter() = fadeIn(tween(220)) +
    slideInVertically(
        initialOffsetY = { it / 8 },
        animationSpec = tween(220, easing = FastOutSlowInEasing)
    )

/**
 * Pill chip that fades in under the search field as soon as a YouTube URL is
 * detected, live-updating its label as the link resolves. Tonal, quiet, and
 * the same pill the rest of the screen speaks.
 */
@Composable
private fun LinkDetectedBanner(state: LinkLookupState) {
    val scheme = MaterialTheme.colorScheme
    val entrance = remember { MutableTransitionState(false).apply { targetState = true } }
    val isResolving = state is LinkLookupState.Resolving || state is LinkLookupState.Idle

    // Gentle heartbeat on the link icon while the lookup is in flight
    val pulse = rememberInfiniteTransition(label = "linkBannerPulse")
    val iconScale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(tween(550, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "linkBannerIconScale"
    )

    AnimatedVisibility(
        visibleState = entrance,
        enter = fadeIn(tween(180)) + slideInVertically(
            initialOffsetY = { -it / 2 },
            animationSpec = tween(180, easing = FastOutSlowInEasing)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(start = SentupGroup.inset, top = 2.dp, bottom = 2.dp)
                .clip(RoundedCornerShape(SentupRadii.pill))
                .background(scheme.primaryContainer)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Rounded.Link,
                contentDescription = null,
                tint = scheme.onPrimaryContainer,
                modifier = Modifier
                    .size(16.dp)
                    .scale(if (isResolving) iconScale else 1f)
            )
            AnimatedContent(
                targetState = when (state) {
                    is LinkLookupState.MediaResult -> stringResource(R.string.link_ready_to_play)
                    is LinkLookupState.PlaylistSongsResult,
                    is LinkLookupState.PlaylistVideosResult -> stringResource(R.string.link_playlist_loaded)
                    LinkLookupState.Error -> stringResource(R.string.link_error_banner)
                    else -> stringResource(R.string.link_detected)
                },
                transitionSpec = {
                    (fadeIn(tween(200)) + slideInVertically { it / 2 }) togetherWith
                        (fadeOut(tween(150)))
                },
                label = "linkBannerLabel"
            ) { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onPrimaryContainer
                )
            }
        }
    }
}

/**
 * Loading state for a pasted link: the link icon with two expanding sonar
 * rings while the metadata is fetched.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LinkResolvingCard(videoMode: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val sonar = rememberInfiniteTransition(label = "linkSonar")
    val ring1Progress by sonar.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing)),
        label = "linkSonarRing1"
    )
    val ring2Progress by sonar.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(1400, easing = FastOutSlowInEasing),
            initialStartOffset = StartOffset(700)
        ),
        label = "linkSonarRing2"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(80.dp), contentAlignment = Alignment.Center) {
                listOf(ring1Progress, ring2Progress).forEach { progress ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .scale(1f + progress)
                            .background(
                                scheme.primary.copy(alpha = 0.20f * (1f - progress)),
                                CircleShape
                            )
                    )
                }
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(scheme.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Link,
                        contentDescription = null,
                        tint = scheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                stringResource(R.string.link_fetching),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                if (videoMode) stringResource(R.string.link_preparing_video) else stringResource(R.string.link_preparing_song),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Hero result for a resolved video/song link: full-width thumbnail with a
 * play button, entering with a short rise. The whole card starts playback and
 * yields inward while pressed.
 */
@Composable
private fun LinkHeroCard(
    title: String,
    subtitle: String,
    thumbnailUrl: String?,
    durationText: String?,
    badgeText: String,
    onPlay: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val entrance = remember { MutableTransitionState(false).apply { targetState = true } }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "linkHeroPressScale"
    )
    val cardShape = RoundedCornerShape(SentupRadii.card)

    AnimatedVisibility(visibleState = entrance, enter = linkCardEnter()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset, vertical = 8.dp)
                .scale(pressScale)
                .sentupCard(shape = cardShape, elevation = 2)
                .sentupSpecularEdge(cardShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onPlay
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.45f)
                            )
                        )
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .clip(RoundedCornerShape(SentupRadii.pill))
                        .background(Color.Black.copy(alpha = 0.55f))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Link,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Text(
                            badgeText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
                if (durationText != null) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(10.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            durationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(scheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlayArrow,
                        contentDescription = stringResource(R.string.cd_play),
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Header card for a resolved playlist link: cover, item count and a play-all
 * button, entering with the same rise as the hero card.
 */
@Composable
private fun LinkPlaylistHeader(
    count: Int,
    isVideos: Boolean,
    thumbnailUrl: String?,
    onPlayAll: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val entrance = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(visibleState = entrance, enter = linkCardEnter()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset, vertical = 8.dp)
                .sentupCard(
                    shape = RoundedCornerShape(SentupRadii.card),
                    elevation = 1
                )
                .searchRowPress(onClick = onPlayAll)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(SentupRadii.tile))
            ) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.PlaylistPlay,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.link_header_playlist),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    if (isVideos) stringResource(R.string.link_tap_to_play_videos, count)
                    else stringResource(R.string.link_tap_to_play_songs, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant
                )
            }
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(scheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.PlayArrow,
                    contentDescription = stringResource(R.string.action_play_all),
                    tint = scheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * Error state for a pasted link: one quiet icon, one sentence, one pill.
 * Deliberately not red - a failed lookup is a dead end, not damage, and the
 * screen has no red in it anywhere else.
 */
@Composable
private fun LinkErrorCard(onRetry: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val entrance = remember { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(visibleState = entrance, enter = linkCardEnter()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset + SentupGroup.dividerInset)
                .padding(top = 28.dp, bottom = 40.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.LinkOff,
                    contentDescription = null,
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                stringResource(R.string.link_error_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                stringResource(R.string.link_error_body),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(22.dp))
            SearchPillButton(stringResource(R.string.action_try_again), onClick = onRetry)
        }
    }
}

/**
 * Compact video row inside a result group: 16:9 thumbnail with its badge on
 * the left, title and facts on the right, inset hairline above. Denser than
 * VideoCard so a browse list reads as a list.
 */
@Composable
private fun CompactVideoRow(
    video: VideoItem,
    onClick: () -> Unit,
    index: Int,
    count: Int
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .searchGroupSegment(index = index, count = count)
            .searchRowPress(onClick = onClick)
    ) {
        if (index > 0) SentupDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SentupGroup.dividerInset,
                    end = SentupGroup.dividerInset,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(SentupRadii.tile))
                    .background(scheme.surfaceContainerHighest)
            ) {
                AsyncImage(
                    model = video.thumbnailUrl,
                    contentDescription = video.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                VideoThumbnailBadge(
                    video = video,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(video.channelName)
                        if (video.viewCount.isNotEmpty()) {
                            append(" • ")
                            append(video.viewCount)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Playlist row inside a video-mode result group: the same density as
 * [CompactVideoRow] so mixed results read as one list, with the save toggle
 * visible rather than behind a long-press.
 */
@Composable
private fun VideoPlaylistRow(
    playlist: VideoPlaylist,
    onClick: () -> Unit,
    index: Int,
    count: Int,
    isSaved: Boolean?,
    onToggleSave: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .searchGroupSegment(index = index, count = count)
            .searchRowPress(onClick = onClick)
    ) {
        if (index > 0) SentupDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SentupGroup.dividerInset,
                    end = SentupGroup.dividerInset,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(SentupRadii.tile))
                    .background(scheme.surfaceContainerHighest)
            ) {
                AsyncImage(
                    model = playlist.thumbnailUrl,
                    contentDescription = playlist.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Playlist badge, like YouTube's stacked-videos corner chip
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color.Black.copy(alpha = 0.8f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Rounded.PlaylistPlay,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = playlist.videoCountText?.substringBefore(" ") ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(top = 2.dp)
            ) {
                Text(
                    text = playlist.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = buildString {
                        append(stringResource(R.string.label_playlist))
                        playlist.subtitle?.let { append(" • ").append(it) }
                        playlist.videoCountText?.let { append(" • ").append(it) }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = scheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (isSaved != null) {
                val haptics = rememberSentupHaptics()
                QuietIconButton(
                    icon = if (isSaved) Icons.Rounded.Bookmark else Icons.Rounded.BookmarkBorder,
                    contentDescription = if (isSaved) stringResource(R.string.cd_remove_from_library)
                    else stringResource(R.string.cd_save_to_library),
                    tint = if (isSaved) scheme.primary else scheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterVertically),
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                        onToggleSave()
                    }
                )
            }
        }
    }
}

/**
 * A channel in video-mode search results: one quiet row in the group, a
 * chevron saying it opens a page. Deliberately the only result kind with no
 * save or queue action - a channel is somewhere to go, not something to
 * keep - and the chevron is the shared one.
 */
@Composable
private fun ChannelResultRow(
    channel: com.sentup.android.data.SubscribedChannel,
    index: Int,
    count: Int,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .searchGroupSegment(index = index, count = count)
            .searchRowPress(onClick = onClick)
    ) {
        if (index > 0) SentupDivider()
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SentupGroup.dividerInset,
                    end = SentupGroup.dividerInset,
                    top = 10.dp,
                    bottom = 10.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            com.sentup.android.ui.channel.CreatorAvatar(
                avatarUrl = channel.avatarUrl,
                name = channel.name,
                modifier = Modifier.size(52.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitle = listOfNotNull(
                    channel.handle?.takeIf { it.isNotBlank() },
                    channel.subscriberCountText?.takeIf { it.isNotBlank() }
                ).joinToString(" • ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            SentupChevron()
        }
    }
}

/**
 * Footer under a paginated result list. Results load automatically as the
 * list nears its end, so this never invites a tap - it only says whether
 * more is on the way or the list has run out, thin and grey, below the
 * card rather than dressed as part of it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SearchPagingFooter(
    isLoadingMore: Boolean,
    isExhausted: Boolean
) {
    if (!isLoadingMore && !isExhausted) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                horizontal = SentupGroup.inset + SentupGroup.dividerInset,
                vertical = 18.dp
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isLoadingMore) {
            LoadingIndicator(
                modifier = Modifier.size(20.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        } else {
            Text(
                stringResource(R.string.thats_everything),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
        }
    }
}
