package com.sentup.android.ui.library
import com.sentup.android.ui.components.DismissibleSnackbarHost
import androidx.annotation.StringRes
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.sentup.android.R
import com.sentup.android.ui.components.displaySubtitle
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupGroupCard
import com.sentup.android.ui.theme.SentupGroupHeader
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.SentupRow
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.List
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import com.sentup.android.ui.components.PredictiveBackStack
import com.sentup.android.data.PlaylistDisplayItem
import com.sentup.android.data.Song
import com.sentup.android.data.SongSource
import com.sentup.android.data.ThemePreferences
import com.sentup.android.data.albumArtistLabel
import com.sentup.android.data.googleImageAtSize
import com.sentup.android.data.groupSongsByAlbum
import com.sentup.android.data.groupSongsByArtist
import com.sentup.android.data.isUnknownAlbum
import com.sentup.android.data.isUnknownArtist
import com.sentup.android.data.sortedInAlbumOrder
import com.sentup.android.ui.artist.ArtistScreen
import com.sentup.android.ui.components.ExpressivePullToRefresh
import com.sentup.android.ui.components.VideoThumbnail
import com.sentup.android.ui.downloads.MusicPlaylistDownloadAction
import com.sentup.android.ui.player.rememberArtworkColorScheme
import com.sentup.android.ui.theme.MontserratFamily
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.runtime.mutableFloatStateOf
import com.sentup.android.ui.home.HomeViewModel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.FilterChip
import androidx.compose.material.icons.rounded.Shuffle
import kotlinx.coroutines.async
import androidx.lifecycle.viewModelScope
import androidx.compose.material.icons.rounded.CloudUpload
import kotlin.math.roundToInt

/**
 * Feeds the app synthesizes rather than fetches, and the account's own
 * built-ins. None of them is a playlist that could be kept: "LM" (Your Likes)
 * and "LL" are assembled locally, "RTM" is the generated Supermix, and Watch
 * Later is the account's own list, already in the library by definition.
 */
private val NON_SAVABLE_PLAYLIST_IDS =
    setOf("LM", "VLLM", "LL", "VLLL", "RTM", "WL", "VLWL", READY_OFFLINE_ID)

/**
 * The synthetic playlist id behind "Ready offline".
 *
 * Deliberately not id-shaped: it never reaches YouTube, and the point of a
 * value nothing upstream could return is that a mistake routing it somewhere
 * that expects a real playlist id fails loudly instead of quietly fetching
 * someone else's list.
 */
const val READY_OFFLINE_ID = "READY_OFFLINE"

/**
 * Playlists with no page anywhere to link to.
 *
 * A share url is built by pasting the id into a music.youtube.com playlist
 * link, which for a feed assembled on this device produces a link that opens
 * nothing. "LM" is absent because Liked Music genuinely is a YouTube playlist
 * with that id; "RTM" and "Ready offline" are not.
 */
private val NON_SHAREABLE_PLAYLIST_IDS = setOf("RTM", READY_OFFLINE_ID)

/**
 * The Main Library Navigation Hub.
 * Manages transitions between:
 * - Main Library View (Lists/Grid)
 * - Playlist/Album Details
 * - Artist Details
 * - Statistics
 */
@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryContent(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit = {},
    /** Open a musician's video-mode channel page, from the artist screen. */
    onOpenChannel: ((String) -> Unit)? = null,
    onPlayQueue: (List<Song>, Song?) -> Unit,
    /** Play a collection with shuffle mode on; see PlayerViewModel.playQueueShuffled. */
    onShuffleQueue: (List<Song>) -> Unit,
    contentPadding: PaddingValues,
    viewModel: HomeViewModel,
    isDarkMode: Boolean,
    isLocalLibrary: Boolean = true,
    onDownloadsClick: () -> Unit = {},
    initialArtist: String? = null,
    onInitialArtistConsumed: () -> Unit = {},
    /**
     * Whether the next hand-off ([initialArtist], [initialPlaylist] or
     * [initialAlbum]) came from another tab. Back from what it opens - or from
     * anything reached inside it - returns to that tab through
     * [onReturnToCaller] instead of the Library root: the excursion was never
     * part of this tab's flow, and dropping the user on the root reads as the
     * navigation having lost their place.
     */
    initialReturnToCaller: Boolean = false,
    /** End an excursion that began on another tab: go back to that tab. */
    onReturnToCaller: () -> Unit = {},
    /**
     * Open straight onto a playlist, for callers outside the Library that have
     * one in hand - Spotlight's shortcut grid and shelves. Same hand-off shape
     * as [initialArtist]: the caller clears it through the consumed callback so
     * coming back to the tab later does not re-open the playlist.
     */
    initialPlaylist: PlaylistDisplayItem? = null,
    onInitialPlaylistConsumed: () -> Unit = {},
    /**
     * Open straight onto a device album, named rather than passed whole: the
     * album route is built from a name plus that album's songs, and the player
     * - the one caller - has a [Song] and no list. The songs are gathered here
     * from the library this screen already holds.
     */
    initialAlbum: String? = null,
    onInitialAlbumConsumed: () -> Unit = {},
    onStatsClick: () -> Unit = {},
    /**
     * Long-press on a song row. Hoisted rather than handled here because the
     * options sheet acts on the player, and HomeScreen is where the
     * PlayerViewModel lives; hosting one sheet up there also keeps a single
     * instance alive across the Library's own sub-routes.
     */
    onSongLongPress: ((Song) -> Unit)? = null,
    /**
     * Queue a song from a playlist row. Same hoisting as [onSongLongPress]:
     * the queue lives in HomeScreen's PlayerViewModel. Null disarms the queue
     * swipe action. The boolean is play-next.
     */
    onEnqueueSong: ((Song, Boolean) -> Unit)? = null,
    /**
     * Hoisted by HomeScreen for the main route's All tab, which is the one the
     * tab opens on. The other sub-tabs and the playlist/artist/album routes keep
     * their own states, since each is a different list and sharing one would
     * restore another list's index.
     */
    allSongsListState: LazyListState = rememberLazyListState()
) {
    // Navigation State
    var currentRoute by rememberSaveable { mutableStateOf(LibraryRoute.Main) }

    // Cached-in-full songs. Re-read whenever the tab comes back to its root,
    // because the cache changes as a side effect of listening: songs arrive by
    // being played and leave by being evicted, so a value collected once at
    // first composition would be stale by the time anyone looked at it again.
    val readyOffline by viewModel.readyOffline.collectAsState()
    LaunchedEffect(currentRoute) {
        if (currentRoute == LibraryRoute.Main) viewModel.refreshReadyOffline()
    }

    // Arguments for routes
    var selectedPlaylist by remember { mutableStateOf<PlaylistDisplayItem?>(null) }
    var selectedArtistName by remember { mutableStateOf<String?>(null) }
    // The UC id behind the artist route when one resolved it (similar-artist
    // taps carry it); null means the screen resolves from the name itself.
    var selectedArtistId by remember { mutableStateOf<String?>(null) }
    // True while the open route is an excursion handed over from another tab:
    // back from it, or from anything opened inside it, ends the excursion on
    // that tab rather than on the Library root. Set with the hand-off that
    // opened it, cleared when back leaves.
    var returnsToCaller by remember { mutableStateOf(false) }
    var selectedAlbumName by remember { mutableStateOf<String?>(null) }
    var selectedAlbumSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    // Handle initial deep link to artist
    LaunchedEffect(initialArtist) {
        if (initialArtist != null) {
            selectedArtistName = initialArtist
            selectedArtistId = null
            returnsToCaller = initialReturnToCaller
            currentRoute = LibraryRoute.Artist
            onInitialArtistConsumed()
        }
    }

    // Handle initial deep link to a playlist
    LaunchedEffect(initialPlaylist) {
        if (initialPlaylist != null) {
            selectedPlaylist = initialPlaylist
            returnsToCaller = initialReturnToCaller
            currentRoute = LibraryRoute.Playlist
            onInitialPlaylistConsumed()
        }
    }

    // Handle initial deep link to an album. Keyed on the song list as well, so
    // a request arriving before the device scan finishes is not resolved to an
    // empty album; an album with no songs is not opened at all, which is the
    // honest outcome for a name the library does not have.
    LaunchedEffect(initialAlbum, songs) {
        if (initialAlbum != null) {
            val requested = initialAlbum.trim()
            val albumSongs = songs.filter {
                it.album.trim().equals(requested, ignoreCase = true)
            }.sortedInAlbumOrder()
            if (albumSongs.isNotEmpty()) {
                selectedAlbumName = initialAlbum
                selectedAlbumSongs = albumSongs
                returnsToCaller = initialReturnToCaller
                currentRoute = LibraryRoute.Album
                onInitialAlbumConsumed()
            }
        }
    }

    // A Spotify link shared into the app opens the import screen on it. The
    // link is held here rather than read from the flow by the screen, so it
    // survives being consumed.
    var spotifyImportLink by remember { mutableStateOf<String?>(null) }
    val pendingSpotifyImport by viewModel.pendingSpotifyImport.collectAsState()
    LaunchedEffect(pendingSpotifyImport) {
        pendingSpotifyImport?.let { link ->
            spotifyImportLink = link
            returnsToCaller = false
            currentRoute = LibraryRoute.SpotifyImport
            viewModel.consumeSpotifyImportRequest()
        }
    }

    // Expressive motion physics for screen pushes/pops
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    // Back from any Library route: out to the tab an excursion began on, or
    // to the root. One function, so every screen's back control and the
    // system gesture agree with each other. ImportSongs and AddSongs are the
    // two-level routes: they are only ever reached from an open playlist, so
    // back returns there - and deliberately before the excursion check, which
    // must keep waiting for the back that leaves the playlist itself.
    fun back() {
        if (currentRoute == LibraryRoute.ImportSongs || currentRoute == LibraryRoute.AddSongs) {
            currentRoute = LibraryRoute.Playlist
        } else if (returnsToCaller) {
            returnsToCaller = false
            onReturnToCaller()
        } else {
            currentRoute = LibraryRoute.Main
        }
    }

    // The root and the routes, as blocks both layouts below draw from.
    val mainScreen: @Composable () -> Unit = {
            LibraryMainScreen(
                songs = songs,
                isLocalLibrary = isLocalLibrary,
                viewModel = viewModel,
                contentPadding = contentPadding,
                onSongClick = onSongClick,
                onPlayQueue = onPlayQueue,
                onShuffleQueue = onShuffleQueue,
                onDownloadsClick = onDownloadsClick,
                onNavigateToPlaylist = { playlist ->
                    selectedPlaylist = playlist
                    currentRoute = LibraryRoute.Playlist
                },
                onNavigateToArtist = { artist ->
                    selectedArtistName = artist
                    selectedArtistId = null
                    returnsToCaller = false
                    currentRoute = LibraryRoute.Artist
                },
                // A followed artist carries the id it was followed by, so
                // the page opens on that artist rather than on whoever a
                // search for the name finds first.
                onNavigateToFollowedArtist = { artist ->
                    selectedArtistName = artist.name
                    selectedArtistId = artist.id.takeIf { it.startsWith("UC") }
                    returnsToCaller = false
                    currentRoute = LibraryRoute.Artist
                },
                onNavigateToAlbum = { album, songs ->
                    selectedAlbumName = album
                    selectedAlbumSongs = songs
                    currentRoute = LibraryRoute.Album
                },
                onNavigateToStats = {
                    currentRoute = LibraryRoute.Stats
                },
                onNavigateToHistory = {
                    currentRoute = LibraryRoute.History
                },
                readyOfflineCount = readyOffline.songs.size,
                readyOfflineBytes = readyOffline.totalBytes,
                readyOfflineUnnamed = readyOffline.unnamedCount,
                readyOfflineHistoryDisabled = readyOffline.historyDisabled,
                onReadyOfflineClick = {
                    currentRoute = LibraryRoute.ReadyOffline
                },
                onCreatePlaylist = {
                    currentRoute = LibraryRoute.CreatePlaylist
                },
                onSongLongPress = onSongLongPress,
                allSongsListState = allSongsListState
            )
    }

    val routeContent: @Composable (LibraryRoute) -> Unit = { route ->
        when (route) {
            // Main lives underneath now; this layer is empty over it, and full
            // size so both states measure the same.
            LibraryRoute.Main -> Spacer(Modifier.fillMaxSize())
            LibraryRoute.Playlist -> {
                selectedPlaylist?.let { playlist ->
                    PlaylistDetailScreen(
                        playlist = playlist,
                        onBack = { back() },
                        onPlayQueue = onPlayQueue,
                        onShuffleQueue = onShuffleQueue,
                        viewModel = viewModel,
                        isAlbum = false,
                        onSongLongPress = onSongLongPress,
                        onEnqueueSong = onEnqueueSong,
                        onAddSongsRequest = { currentRoute = LibraryRoute.ImportSongs },
                        onSearchSongsRequest = { currentRoute = LibraryRoute.AddSongs }
                    )
                }
            }
            LibraryRoute.Album -> {
                selectedAlbumName?.let { album ->
                    // Construct a pseudo-playlist item for the album wrapper.
                    // The artist line names the album's artist, or Various
                    // Artists for a compilation - never one contributor.
                    val albumItem = PlaylistDisplayItem(
                        name = album,
                        url = album, // ID is the name for local albums usually
                        uploaderName = albumArtistLabel(
                            selectedAlbumSongs,
                            stringResource(R.string.various_artists)
                        ),
                        itemCount = selectedAlbumSongs.size,
                        thumbnailUrl = selectedAlbumSongs.firstOrNull()?.albumArtUri.toString()
                    )
                    PlaylistDetailScreen(
                        playlist = albumItem,
                        onBack = { back() },
                        onPlayQueue = onPlayQueue,
                        onShuffleQueue = onShuffleQueue,
                        viewModel = viewModel,
                        preloadedSongs = selectedAlbumSongs,
                        isAlbum = true,
                        onSongLongPress = onSongLongPress,
                        onEnqueueSong = onEnqueueSong,
                        // An album is not an editable local playlist.
                        onAddSongsRequest = null,
                        onSearchSongsRequest = null
                    )
                }
            }
            LibraryRoute.Artist -> {
                selectedArtistName?.let { artist ->
                    ArtistScreen(
                        artistName = artist,
                        artistId = selectedArtistId ?: artist,
                        songs = songs, // Pass all songs, screen filters locally or fetches
                        onBack = { back() },
                        onPlayQueue = onPlayQueue,
                        onShuffleQueue = onShuffleQueue,
                        onSongClick = onSongClick,
                        onAlbumClick = { album, songs ->
                            selectedAlbumName = album
                            selectedAlbumSongs = songs
                            currentRoute = LibraryRoute.Album
                        },
                        onOpenAlbum = { albumItem ->
                            selectedPlaylist = albumItem
                            currentRoute = LibraryRoute.Playlist
                        },
                        viewModel = viewModel,
                        onSongLongPress = onSongLongPress,
                        onOpenChannel = onOpenChannel,
                        onOpenArtist = { name, id ->
                            selectedArtistName = name
                            selectedArtistId = id
                            currentRoute = LibraryRoute.Artist
                        },
                        onOpenPlaylist = { playlist ->
                            selectedPlaylist = playlist
                            currentRoute = LibraryRoute.Playlist
                        }
                    )
                }
            }
            LibraryRoute.Stats -> {
                StatsScreen(
                    onBack = { back() },
                    viewModel = viewModel,
                    contentPadding = contentPadding
                )
            }
            LibraryRoute.History -> {
                ListeningHistoryScreen(
                    onBack = { back() },
                    viewModel = viewModel,
                    onPlayQueue = onPlayQueue,
                    contentPadding = contentPadding
                )
            }
            LibraryRoute.ReadyOffline -> {
                // Same shape as the Album route: a synthetic playlist item over
                // preloaded songs, so play-all, shuffle, search and the
                // long-press options sheet all come for free. The sheet is what
                // makes the list actionable - Download on a row promotes a song
                // out of the evictable cache and into a permanent copy.
                val readyOfflineItem = PlaylistDisplayItem(
                    name = "Ready offline",
                    url = READY_OFFLINE_ID,
                    uploaderName = "On this device",
                    itemCount = readyOffline.songs.size,
                    thumbnailUrl = readyOffline.songs.firstOrNull()?.thumbnailUrl
                )
                PlaylistDetailScreen(
                    playlist = readyOfflineItem,
                    onBack = { back() },
                    onPlayQueue = onPlayQueue,
                    onShuffleQueue = onShuffleQueue,
                    viewModel = viewModel,
                    preloadedSongs = readyOffline.songs,
                    isAlbum = false,
                    onSongLongPress = onSongLongPress,
                    onEnqueueSong = onEnqueueSong,
                    // A cache view, not an editable local playlist.
                    onAddSongsRequest = null,
                    onSearchSongsRequest = null
                )
            }
            LibraryRoute.CreatePlaylist -> {
                PlaylistStudioScreen(
                    viewModel = viewModel,
                    onBack = { back() },
                    onImportFromSpotify = {
                        spotifyImportLink = null
                        currentRoute = LibraryRoute.SpotifyImport
                    },
                    onCreated = { created ->
                        // Straight onto the new playlist; back from there goes
                        // to the root, the same as any playlist opened from it.
                        selectedPlaylist = created
                        returnsToCaller = false
                        currentRoute = LibraryRoute.Playlist
                    }
                )
            }
            LibraryRoute.ImportSongs -> {
                selectedPlaylist?.let { target ->
                    PlaylistImportScreen(
                        targetPlaylist = target,
                        viewModel = viewModel,
                        onBack = { back() },
                        onDone = { back() }
                    )
                }
            }
            LibraryRoute.SpotifyImport -> {
                SpotifyImportScreen(
                    initialLink = spotifyImportLink,
                    viewModel = viewModel,
                    onBack = { back() },
                    onCreated = { created ->
                        selectedPlaylist = created
                        returnsToCaller = false
                        currentRoute = LibraryRoute.Playlist
                    }
                )
            }
            LibraryRoute.AddSongs -> {
                selectedPlaylist?.let { target ->
                    val localIds by viewModel.localPlaylistIds.collectAsState()
                    PlaylistAddSongsScreen(
                        targetPlaylist = target,
                        targetIsLocal = target.id in localIds,
                        viewModel = viewModel,
                        onBack = { back() },
                        onDone = { back() }
                    )
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
    if (maxWidth >= LIBRARY_TWO_PANE_MIN_WIDTH) {
        // List and detail side by side: the library stays where it is and
        // what it opens appears beside it, so moving between playlists is a
        // tap each rather than a push and a pop. Back closes the detail in
        // place; nothing departs the screen, so it is not a peel.
        BackHandler(enabled = currentRoute != LibraryRoute.Main) { back() }
        val listWidth = (maxWidth * 0.36f).coerceIn(340.dp, 440.dp)
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .width(listWidth)
                    .fillMaxHeight()
            ) {
                // The list marks what is open beside it.
                val openItem = when (currentRoute) {
                    LibraryRoute.Playlist, LibraryRoute.ImportSongs, LibraryRoute.AddSongs ->
                        LibraryOpenItem(playlistId = selectedPlaylist?.id)
                    LibraryRoute.Album -> LibraryOpenItem(albumName = selectedAlbumName)
                    LibraryRoute.Artist -> LibraryOpenItem(artistName = selectedArtistName)
                    else -> null
                }
                CompositionLocalProvider(LocalLibraryOpenItem provides openItem) {
                    mainScreen()
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
            ) {
                AnimatedContent(
                    targetState = currentRoute,
                    label = "LibraryDetailPane",
                    transitionSpec = {
                        (fadeIn(animationSpec = effectsSpec) +
                            slideInHorizontally(animationSpec = spatialSpec) { it / 12 }) togetherWith
                            fadeOut(animationSpec = effectsSpec) using
                            SizeTransform(clip = false) { _, _ -> snap() }
                    }
                ) { route ->
                    if (route == LibraryRoute.Main) {
                        LibraryDetailPlaceholder(contentPadding = contentPadding)
                    } else {
                        routeContent(route)
                    }
                }
            }
        }
    } else
    PredictiveBackStack(
        childOpen = currentRoute != LibraryRoute.Main,
        onBack = { back() },
        // An excursion's back lands on another tab, not on the root the peel
        // would reveal, so it is not previewed; the tab slide carries it.
        // ImportSongs and AddSongs back onto the playlist, not the root the
        // peel shows, so they are not previewed either.
        previewable = currentRoute != LibraryRoute.Main && !returnsToCaller &&
            currentRoute != LibraryRoute.ImportSongs && currentRoute != LibraryRoute.AddSongs,
        background = mainScreen
    ) { committedByGesture ->
    AnimatedContent(
        targetState = currentRoute,
        label = "LibraryNavigation",
        transitionSpec = {
            val content = when {
                // The finger already performed this exit.
                committedByGesture -> EnterTransition.None togetherWith ExitTransition.None
                targetState == LibraryRoute.Main ->
                    fadeIn(animationSpec = effectsSpec) togetherWith
                        (slideOutHorizontally(animationSpec = spatialSpec) { it } +
                            fadeOut(animationSpec = effectsSpec))
                else ->
                    (slideInHorizontally(animationSpec = spatialSpec) { it } +
                        fadeIn(animationSpec = effectsSpec)) togetherWith
                        (slideOutHorizontally(animationSpec = spatialSpec) { -it / 3 } +
                            fadeOut(animationSpec = effectsSpec))
            }
            // Main is empty on this layer, so the default SizeTransform would
            // animate the container between nothing and full screen and clip
            // the route to it on the way.
            content using SizeTransform(clip = false) { _, _ -> snap() }
        }
    ) { route ->
        routeContent(route)
    }
    }
    }
}

enum class LibraryRoute {
    Main, Playlist, Album, Artist, Stats, History, ReadyOffline,
    /** The immersive local-playlist creation flow (PlaylistStudioScreen). */
    CreatePlaylist,
    /** "Add from your playlists" into the open local playlist (PlaylistImportScreen). */
    ImportSongs,
    /** Recents, likes and a YouTube Music search into the open playlist (PlaylistAddSongsScreen). */
    AddSongs,
    /** A Spotify playlist or album, matched on YouTube Music into a new device playlist (SpotifyImportScreen). */
    SpotifyImport
}

// Labels were hardcoded English ("All", "Playlists") while every sibling enum
// in this file resolves its label through resources, which is why the segment
// was the one Turkish-unfriendly row in the Library.
enum class LibraryTab(@param:StringRes val label: Int) {
    All(R.string.lib_tab_all),
    Playlists(R.string.lib_tab_playlists),
    Artists(R.string.lib_tab_artists),
    Albums(R.string.lib_tab_albums)
}

/**
 * Sort orders for the All tab's track list. Labels state their own direction
 * ("Most played", not "Play count") so the list needs no asc/desc toggle.
 */
enum class PlaylistSortAction(val label: Int) {
    Title(R.string.lib_sort_title),
    Artist(R.string.lib_sort_artist),
    Album(R.string.lib_sort_album),
    Reverse(R.string.lib_sort_reverse),
    Shuffle(R.string.lib_sort_shuffle),
}

/**
 * Sort orders for the Artists and Albums tabs, which had none (feedback,
 * September 2026: "organize them by the number of songs"). Persisted by name,
 * so the constants are frozen.
 */
enum class LibraryGroupSort(val label: Int, val icon: ImageVector) {
    Name(R.string.lib_group_sort_name, Icons.Rounded.SortByAlpha),
    MostSongs(R.string.lib_group_sort_songs, Icons.Rounded.LibraryMusic),
    MostPlayed(R.string.lib_group_sort_played, Icons.Rounded.TrendingUp);

    /** [groups] (a name and its songs) in this order; ties fall back to the name. */
    fun <T> apply(groups: List<Pair<String, List<T>>>, plays: (T) -> Int): List<Pair<String, List<T>>> {
        val byName = compareBy<Pair<String, List<T>>> { it.first.lowercase() }
        return when (this) {
            Name -> groups.sortedWith(byName)
            MostSongs -> groups.sortedWith(compareByDescending<Pair<String, List<T>>> { it.second.size }.then(byName))
            MostPlayed -> groups.sortedWith(
                compareByDescending<Pair<String, List<T>>> { g -> g.second.sumOf(plays) }.then(byName)
            )
        }
    }

    companion object {
        fun from(name: String?): LibraryGroupSort = entries.firstOrNull { it.name == name } ?: Name
    }
}

enum class LibrarySortOption(val label: String, val icon: ImageVector) {
    Title("Title", Icons.Rounded.SortByAlpha),
    Artist("Artist", Icons.Rounded.Person),
    Album("Album", Icons.Rounded.Album),
    MostPlayed("Most played", Icons.Rounded.TrendingUp),
    RecentlyAdded("Recently added", Icons.Rounded.NewReleases)
}

/** Gap between cover tiles. One value so every grid and rail breathes alike. */
private val LibraryTileGap = 14.dp

/**
 * The corner shape for one row of a section drawn as a single continuous card.
 *
 * A track list can run to thousands of rows, so it has to stay virtualized,
 * and a virtualized list cannot literally live inside one [SentupGroupCard].
 * What it can do is draw the card per row: the first row rounds its top, the
 * last its bottom, the middle rows fill flat, and since every row paints the
 * same tone the seams are invisible. Hairlines between rows stay inset - a
 * full-bleed line across the card would cut the group back into slices.
 */
private fun libraryCardSectionShape(first: Boolean, last: Boolean) =
    RoundedCornerShape(
        topStart = if (first) SentupRadii.card else 0.dp,
        topEnd = if (first) SentupRadii.card else 0.dp,
        bottomStart = if (last) SentupRadii.card else 0.dp,
        bottomEnd = if (last) SentupRadii.card else 0.dp
    )

/**
 * The screen's identity, iOS-style: a large title with its count line beneath.
 * Aligned to the text edge of the cards below it (screen inset plus the card's
 * own row inset), so the title, the rows and their hairlines share one x.
 */
@Composable
private fun LibraryLargeTitle(subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SentupGroup.inset + SentupGroup.dividerInset,
                end = SentupGroup.inset + SentupGroup.dividerInset,
                top = 12.dp,
                bottom = 2.dp
            )
    ) {
        Text(
            text = stringResource(R.string.tab_library),
            fontFamily = MontserratFamily,
            fontSize = 34.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

/**
 * The tab switcher as a floating capsule rather than a full-bleed strip: the
 * pill card it sits on [sentupCard]s a step off the page, and only the chosen
 * segment is filled - emerald under the word, everything else quiet. Pressing
 * a segment pushes it in slightly; nothing flashes.
 */
@Composable
private fun LibrarySegmentedPill(
    selected: LibraryTab,
    onSelect: (LibraryTab) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset)
            .sentupCard(
                shape = RoundedCornerShape(SentupRadii.pill),
                elevation = 1,
                tinted = false
            )
            .padding(4.dp)
    ) {
        LibraryTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val interactionSource = remember(tab) { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (pressed) 0.96f else 1f,
                animationSpec = tween(durationMillis = 90),
                label = "segmentPress"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(SentupRadii.pill))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    )
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onSelect(tab) }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(tab.label),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp)
                )
            }
        }
    }
}

/** One row of the sort sheet, whatever enum it came from. */
private data class LibrarySortChoice(
    val key: String,
    val label: String,
    val icon: ImageVector
)

/**
 * Sort options in a bottom sheet, not a dropdown pinned to a header button.
 * The options belong under the thumb that opened them, and a sheet gives each
 * one a whole row instead of a cramped menu line.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibrarySortSheet(
    title: String,
    choices: List<LibrarySortChoice>,
    selectedKey: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = SentupGroup.inset + SentupGroup.dividerInset,
                    end = SentupGroup.inset + SentupGroup.dividerInset,
                    bottom = 12.dp
                )
            )
            SentupGroupCard(showDividers = false) {
                choices.forEachIndexed { index, choice ->
                    if (index > 0) SentupDivider()
                    val isCurrent = choice.key == selectedKey
                    SentupRow(
                        title = choice.label,
                        icon = choice.icon,
                        onClick = { onSelect(choice.key) },
                        trailing = {
                            if (isCurrent) {
                                Icon(
                                    Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }
            }
        }
    }
}

/**
 * The library's only chrome below the title: one thin floating capsule that
 * says how the list is sorted. Tapping it opens the sheet; the shuffle button
 * beside it acts directly. The old design hung two icon buttons off the header
 * and a sort menu off a section label - the capsule replaces all three.
 */
@Composable
private fun LibrarySortCapsule(
    label: String,
    canShuffle: Boolean,
    onShuffle: () -> Unit,
    onOpenSort: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "capsulePress"
    )
    Row(
        modifier = Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .sentupCard(shape = RoundedCornerShape(SentupRadii.pill), elevation = 2)
            .clip(RoundedCornerShape(SentupRadii.pill))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onOpenSort
            )
            .padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.SwapVert,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(17.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (canShuffle) {
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onShuffle
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = stringResource(R.string.cd_shuffle),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

/**
 * The Library root. A large title and a count, a floating capsule that switches
 * what the library is looking at, the content, and one thin sort capsule
 * floating at the bottom. Everything the old header carried - create playlist,
 * stats, shuffle, sort - lives inside the content or under the thumb now; the
 * top of the screen says one thing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LibrarySingleFlow(
    songs: List<Song>,
    librarySongs: List<Song>,
    likedSongs: List<Song>,
    downloadedSongs: List<Song>,
    playlists: List<PlaylistDisplayItem>,
    localPlaylistIds: Set<String>,
    savedPlaylistIds: Set<String>,
    hiddenPlaylists: List<com.sentup.android.data.HiddenPlaylist>,
    isLoggedIn: Boolean,
    playCounts: Map<String, Int>,
    contentPadding: PaddingValues,
    listState: LazyListState,
    onSongClick: (Song) -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onLikedSongsClick: () -> Unit,
    onArtistClick: (String) -> Unit,
    onAlbumClick: (String, List<Song>) -> Unit,
    onDownloadsClick: () -> Unit,
    onNavigateToHistory: () -> Unit,
    onNavigateToStats: () -> Unit,
    onEditPlaylist: (PlaylistDisplayItem, String, String?) -> Unit,
    onDeletePlaylist: (PlaylistDisplayItem) -> Unit,
    onRemoveSavedPlaylist: (PlaylistDisplayItem) -> Unit,
    onHidePlaylist: (PlaylistDisplayItem) -> Unit,
    onUnhidePlaylist: (String) -> Unit,
    importAction: @Composable () -> Unit,
    onCreatePlaylist: () -> Unit,
    onSongLongPress: ((Song) -> Unit)?,
    readyOfflineCount: Int,
    readyOfflineBytes: Long,
    readyOfflineUnnamed: Int,
    readyOfflineHistoryDisabled: Boolean,
    onReadyOfflineClick: () -> Unit,
    viewModel: HomeViewModel,
    onShuffle: () -> Unit,
) {
    val artists = remember(librarySongs) {
        librarySongs.filterNot { isUnknownArtist(it.artist) }.groupSongsByArtist()
    }
    val albums = remember(librarySongs) {
        librarySongs.filterNot { isUnknownAlbum(it.album) }.groupSongsByAlbum()
    }
    var showHiddenSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Flat item order below, so a chip can scroll to a section header:
    // 0 chips, 1 insights, 2 shortcuts, 3 playlist header, then its rows,
    // then (optional) artist and album sections, then the song list.
    val playlistRows = if (playlists.isEmpty()) 1 else (playlists.size + 1) / 2
    var idx = 4 + playlistRows
    val artistsHeaderIdx = if (artists.isNotEmpty()) idx else -1
    if (artists.isNotEmpty()) idx += 1 + (artists.size + 1) / 2
    val albumsHeaderIdx = if (albums.isNotEmpty()) idx else -1
    if (albums.isNotEmpty()) idx += 1 + (albums.size + 1) / 2
    val songsHeaderIdx = idx
    val anchors = buildList {
        add("Analiz" to 1)
        add("Listeler" to 3)
        if (artistsHeaderIdx >= 0) add("Sanatçılar" to artistsHeaderIdx)
        if (albumsHeaderIdx >= 0) add("Albümler" to albumsHeaderIdx)
        add("Şarkılar" to songsHeaderIdx)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 120.dp
        )
    ) {
        // 0: quick jump. Pick what you want on top; the list scrolls to it.
        item(key = "flow_chips") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = SentupGroup.inset, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = onShuffle,
                    leadingIcon = {
                        Icon(
                            Icons.Rounded.Shuffle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Karıştır") }
                )
                anchors.forEach { (label, target) ->
                    FilterChip(
                        selected = false,
                        onClick = { scope.launch { listState.animateScrollToItem(target) } },
                        label = { Text(label) }
                    )
                }
            }
        }

        // 1: the analytics block, the first thing you scroll past into data.
        item(key = "flow_insights") {
            InsightsCard(
                viewModel = viewModel,
                onOpenStats = onNavigateToStats,
                modifier = Modifier.padding(
                    start = SentupGroup.inset,
                    end = SentupGroup.inset,
                    top = 4.dp,
                    bottom = SentupGroup.gap
                )
            )
        }

        // A: destinations, the same grouped card the tab used to show.
        item(key = "flow_shortcuts") {
            LibraryShortcutsCard(
                likedCount = likedSongs.size,
                onLikedClick = onLikedSongsClick,
                downloadCount = downloadedSongs.size,
                onDownloadsClick = onDownloadsClick,
                readyOfflineCount = readyOfflineCount,
                readyOfflineBytes = readyOfflineBytes,
                readyOfflineUnnamed = readyOfflineUnnamed,
                readyOfflineHistoryDisabled = readyOfflineHistoryDisabled,
                onReadyOfflineClick = onReadyOfflineClick,
                historyPlayCount = playCounts.values.sum(),
                onHistoryClick = onNavigateToHistory,
                onStatsClick = onNavigateToStats,
                onCreatePlaylist = onCreatePlaylist
            )
        }

        // B: playlists, two per row.
        item(key = "flow_playlists_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = SentupGroup.inset + SentupGroup.dividerInset,
                        end = SentupGroup.inset + SentupGroup.dividerInset,
                        top = SentupGroup.gap
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SentupGroupHeader(
                    text = pluralStringResource(R.plurals.n_playlists, playlists.size, playlists.size),
                    modifier = Modifier.weight(1f)
                )
                importAction()
                if (hiddenPlaylists.isNotEmpty()) {
                    TextButton(onClick = { showHiddenSheet = true }) {
                        Icon(
                            Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(stringResource(R.string.lib_hidden_count, hiddenPlaylists.size))
                    }
                }
            }
        }
        if (playlists.isEmpty()) {
            item(key = "flow_playlists_empty") {
                EmptyLibraryState(
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    title = stringResource(R.string.spotlight_empty_no_playlists),
                    subtitle = stringResource(R.string.library_start_playlist_hint),
                    action = {
                        Button(
                            onClick = onCreatePlaylist,
                            shape = RoundedCornerShape(SentupRadii.pill)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_new_playlist))
                        }
                    }
                )
            }
        } else {
            items(playlists.chunked(2), key = { row -> "pl_" + row.joinToString("|") { it.id } }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = SentupGroup.inset,
                            end = SentupGroup.inset,
                            top = 4.dp,
                            bottom = 18.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(LibraryTileGap)
                ) {
                    row.forEach { playlist ->
                        val isLocalPlaylist = localPlaylistIds.contains(playlist.id)
                        val isSavedPlaylist = savedPlaylistIds.contains(playlist.id)
                        val isYouTubeEditable = !isLocalPlaylist && !isSavedPlaylist &&
                            playlist.id != "RTM" && playlist.id != "LM"
                        Box(Modifier.weight(1f)) {
                            ExpressivePlaylistCard(
                                name = playlist.name?.let { listNameLabel(it) }
                                    ?: stringResource(R.string.untitled_song),
                                count = playlist.itemCount,
                                subtitle = when {
                                    isSavedPlaylist || playlist.itemCount < 0 ->
                                        playlist.displaySubtitle()
                                            .ifBlank { stringResource(R.string.label_playlist) }
                                    else -> null
                                },
                                thumbnailUrl = playlist.thumbnailUrl,
                                description = playlist.description,
                                isEditable = isLocalPlaylist || isYouTubeEditable,
                                isSaved = isSavedPlaylist,
                                affectsYouTubeAccount = isYouTubeEditable && isLoggedIn,
                                onEditConfirmed = { name, description ->
                                    onEditPlaylist(playlist, name, description)
                                },
                                onDeleteConfirmed = { onDeletePlaylist(playlist) },
                                onRemoveSaved = { onRemoveSavedPlaylist(playlist) },
                                onHide = { onHidePlaylist(playlist) },
                                selected = LocalLibraryOpenItem.current?.playlistId == playlist.id,
                                onClick = { onPlaylistClick(playlist) }
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // C: artists, two per row.
        if (artists.isNotEmpty()) {
            item(key = "flow_artists_header") {
                SentupGroupHeader(
                    text = pluralStringResource(R.plurals.lib_artist_count, artists.size, artists.size),
                    modifier = Modifier.padding(top = SentupGroup.gap)
                )
            }
            items(artists.chunked(2), key = { row -> "ar_" + row.joinToString("|") { it.first } }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = SentupGroup.inset,
                            end = SentupGroup.inset,
                            top = 4.dp,
                            bottom = 20.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(LibraryTileGap)
                ) {
                    row.forEach { (artist, artistSongs) ->
                        Box(Modifier.weight(1f)) {
                            ArtistTile(
                                name = artist,
                                caption = pluralStringResource(
                                    R.plurals.n_songs, artistSongs.size, artistSongs.size
                                ),
                                captionIsPrimary = false,
                                thumbnailUrl = artistSongs.firstOrNull { it.albumArtUri != null }
                                    ?.albumArtUri?.toString()
                                    ?: artistSongs.firstOrNull { it.thumbnailUrl != null }?.thumbnailUrl,
                                selected = LocalLibraryOpenItem.current?.artistName == artist,
                                onClick = { onArtistClick(artist) }
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // D: albums, two per row.
        if (albums.isNotEmpty()) {
            item(key = "flow_albums_header") {
                SentupGroupHeader(
                    text = pluralStringResource(R.plurals.lib_album_count, albums.size, albums.size),
                    modifier = Modifier.padding(top = SentupGroup.gap)
                )
            }
            items(albums.chunked(2), key = { row -> "al_" + row.joinToString("|") { it.first } }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = SentupGroup.inset,
                            end = SentupGroup.inset,
                            top = 4.dp,
                            bottom = 20.dp
                        ),
                    horizontalArrangement = Arrangement.spacedBy(LibraryTileGap)
                ) {
                    row.forEach { (album, albumSongs) ->
                        val art = albumSongs.firstOrNull { it.albumArtUri != null }?.albumArtUri?.toString()
                            ?: albumSongs.firstOrNull { it.thumbnailUrl != null }?.thumbnailUrl
                        Box(Modifier.weight(1f)) {
                            ExpressivePlaylistCard(
                                name = album,
                                count = albumSongs.size,
                                thumbnailUrl = art,
                                subtitle = albumArtistLabel(
                                    albumSongs, stringResource(R.string.various_artists)
                                ),
                                selected = LocalLibraryOpenItem.current?.albumName == album,
                                onClick = { onAlbumClick(album, albumSongs) }
                            )
                        }
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        // E: every song, one row each.
        item(key = "flow_songs_header") {
            SentupGroupHeader(
                text = pluralStringResource(R.plurals.n_songs, songs.size, songs.size),
                modifier = Modifier.padding(top = SentupGroup.gap)
            )
        }
        items(songs, key = { "song_" + it.id }) { song ->
            SongListItem(
                song = song,
                isLiked = likedSongs.any { it.id == song.id },
                isDownloaded = downloadedSongs.any { it.id == song.id },
                onLongClick = onSongLongPress?.let { press -> { press(song) } },
                onClick = { onSongClick(song) }
            )
        }
    }

    if (showHiddenSheet) {
        HiddenPlaylistsSheet(
            hidden = hiddenPlaylists,
            onUnhide = onUnhidePlaylist,
            onDismiss = { showHiddenSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun LibraryMainScreen(
    songs: List<Song>,
    isLocalLibrary: Boolean,
    viewModel: HomeViewModel,
    contentPadding: PaddingValues,
    onSongClick: (Song) -> Unit,
    onPlayQueue: (List<Song>, Song?) -> Unit,
    /** Play a collection with shuffle mode on; see PlayerViewModel.playQueueShuffled. */
    onShuffleQueue: (List<Song>) -> Unit,
    onDownloadsClick: () -> Unit,
    onNavigateToPlaylist: (PlaylistDisplayItem) -> Unit,
    onNavigateToArtist: (String) -> Unit,
    onNavigateToFollowedArtist: (com.sentup.android.data.TasteArtist) -> Unit = {},
    onNavigateToAlbum: (String, List<Song>) -> Unit,
    onNavigateToStats: () -> Unit,
    onNavigateToHistory: () -> Unit,
    /** Songs cached in full; the entry point hides itself when nothing is. */
    readyOfflineCount: Int = 0,
    readyOfflineBytes: Long = 0L,
    readyOfflineUnnamed: Int = 0,
    readyOfflineHistoryDisabled: Boolean = false,
    onReadyOfflineClick: () -> Unit = {},
    /** Opens the immersive creation flow. Required: a silent default here is
     * a New playlist button that does nothing. */
    onCreatePlaylist: () -> Unit,
    onSongLongPress: ((Song) -> Unit)? = null,
    allSongsListState: LazyListState = rememberLazyListState()
) {
    val userPlaylists by viewModel.userPlaylists.collectAsState()
    val localPlaylistIds by viewModel.localPlaylistIds.collectAsState()
    val savedPlaylistIds by viewModel.savedPlaylistIds.collectAsState()
    val hiddenPlaylists by viewModel.hiddenPlaylists.collectAsState()
    val likedSongs by viewModel.likedSongs.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()
    val playCounts by viewModel.playCounts.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isYouTubeConnected by viewModel.isYouTubeConnected.collectAsState()

    // Sort order sticks across launches, so it lives in prefs rather than in
    // rememberSaveable. Matched by name instead of valueOf so an option
    // removed in a later version falls back to Title instead of throwing.
    val context = LocalContext.current
    val themePreferences = remember(context) { ThemePreferences(context) }

    // The open tab is persisted for the same reason, and it is the same bug:
    // rememberSaveable survived rotation but not leaving the screen, so every
    // trip out to a playlist and back landed on All again.
    // Single flow: the tab strip is gone, so the whole Library is one scroll.
    val selectedTab = LibraryTab.All
    val tabHaptics = com.sentup.android.util.rememberSentupHaptics()
    val storedSortName by themePreferences.librarySortOption.collectAsState()
    val sortOption = remember(storedSortName) {
        LibrarySortOption.entries.firstOrNull { it.name == storedSortName }
            ?: LibrarySortOption.Title
    }
    val artistSort by themePreferences.libraryArtistSort.collectAsState()
    val albumSort by themePreferences.libraryAlbumSort.collectAsState()
    var showSortSheet by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshRecentlyPlayed()
    }

    // The library is what the user OWNS or SAVED - never the recommendation
    // feed. Local mode: device songs + downloads. YouTube mode: downloads +
    // liked songs (the `songs` param carries home recommendations there).
    val librarySongs = remember(songs, downloadedSongs, likedSongs, isLocalLibrary) {
        if (isLocalLibrary) {
            (songs + downloadedSongs).distinctBy { it.id }
        } else {
            (downloadedSongs + likedSongs).distinctBy { it.id }
        }
    }

    val sortedSongs = remember(librarySongs, sortOption, playCounts) {
        when (sortOption) {
            LibrarySortOption.Title -> librarySongs.sortedBy { it.title.lowercase() }
            LibrarySortOption.Artist -> librarySongs.sortedBy { it.artist.lowercase() }
            LibrarySortOption.Album -> librarySongs.sortedBy { it.album.lowercase() }
            // Never-played songs land at the bottom in a stable alphabetical
            // order rather than whatever order the library happened to build in
            LibrarySortOption.MostPlayed -> librarySongs.sortedWith(
                compareByDescending<Song> { playCounts[it.id] ?: 0 }
                    .thenBy { it.title.lowercase() }
            )
            // Songs with no known add date (see Song.dateAdded) sort last
            LibrarySortOption.RecentlyAdded -> librarySongs.sortedWith(
                compareByDescending<Song> { it.dateAdded ?: Long.MIN_VALUE }
                    .thenBy { it.title.lowercase() }
            )
        }
    }

    // The capsule speaks the current tab's language: track sorts on All, group
    // sorts on Artists and Albums; Playlists has nothing to sort, so it has no
    // capsule at all.
    val groupSortNow = when (selectedTab) {
        LibraryTab.Artists -> LibraryGroupSort.from(artistSort)
        LibraryTab.Albums -> LibraryGroupSort.from(albumSort)
        else -> LibraryGroupSort.Name
    }
    val capsuleLabel = when (selectedTab) {
        LibraryTab.All -> sortOption.label
        LibraryTab.Artists, LibraryTab.Albums -> stringResource(groupSortNow.label)
        LibraryTab.Playlists -> ""
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .sentupPage()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
        ) {
            LibraryLargeTitle(
                subtitle = buildString {
                    append(
                        pluralStringResource(
                            R.plurals.n_songs,
                            librarySongs.size,
                            librarySongs.size
                        )
                    )
                    append(" \u00b7 ")
                    append(
                        pluralStringResource(
                            R.plurals.n_playlists,
                            userPlaylists.size,
                            userPlaylists.size
                        )
                    )
                }
            )

            Spacer(Modifier.height(6.dp))

            ExpressivePullToRefresh(
                // Pulling means "refresh what is already here"; the empty
                // first load gets skeletons instead of a spinner over nothing.
                isRefreshing = isLoading && librarySongs.isNotEmpty(),
                onRefresh = { viewModel.refresh() }
            ) {
                // No pager here, by decision: the Home shell owns the
                // horizontal flick. Tab moves go through the capsule above,
                // with a directional slide for orientation.
                AnimatedContent(
                    targetState = selectedTab,
                    label = "LibraryTab",
                    transitionSpec = {
                        val forward = targetState.ordinal > initialState.ordinal
                        if (forward) {
                            (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> -width / 3 } + fadeOut())
                        } else {
                            (slideInHorizontally { width -> -width / 3 } + fadeIn()) togetherWith
                                (slideOutHorizontally { width -> width } + fadeOut())
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { tab ->
                    when (tab) {
                        LibraryTab.All -> LibrarySingleFlow(
                            songs = sortedSongs,
                            librarySongs = librarySongs,
                            likedSongs = likedSongs,
                            downloadedSongs = downloadedSongs,
                            playlists = userPlaylists,
                            localPlaylistIds = localPlaylistIds,
                            savedPlaylistIds = savedPlaylistIds,
                            hiddenPlaylists = hiddenPlaylists,
                            isLoggedIn = isYouTubeConnected,
                            playCounts = playCounts,
                            contentPadding = contentPadding,
                            listState = allSongsListState,
                            onSongClick = onSongClick,
                            onPlaylistClick = onNavigateToPlaylist,
                            onLikedSongsClick = {
                                onNavigateToPlaylist(
                                    PlaylistDisplayItem("Liked Songs", "LM", "You", likedSongs.size, null)
                                )
                            },
                            onArtistClick = onNavigateToArtist,
                            onAlbumClick = onNavigateToAlbum,
                            onDownloadsClick = onDownloadsClick,
                            onNavigateToHistory = onNavigateToHistory,
                            onNavigateToStats = onNavigateToStats,
                            onEditPlaylist = { playlist, newName, newDescription ->
                                if (localPlaylistIds.contains(playlist.id)) {
                                    viewModel.updateLocalPlaylist(playlist.id, newName, newDescription)
                                } else {
                                    viewModel.renameYouTubePlaylist(playlist.id, newName, newDescription)
                                }
                            },
                            onDeletePlaylist = { playlist ->
                                if (localPlaylistIds.contains(playlist.id)) {
                                    viewModel.deleteLocalPlaylist(playlist.id)
                                } else {
                                    viewModel.deleteYouTubePlaylist(playlist.id)
                                }
                            },
                            onRemoveSavedPlaylist = { playlist ->
                                viewModel.removeSavedPlaylist(playlist.id)
                            },
                            onHidePlaylist = { playlist -> viewModel.hidePlaylist(playlist) },
                            onUnhidePlaylist = { id -> viewModel.unhidePlaylist(id) },
                            importAction = { PlaylistImportButton(viewModel) },
                            onCreatePlaylist = onCreatePlaylist,
                            onSongLongPress = onSongLongPress,
                            readyOfflineCount = readyOfflineCount,
                            readyOfflineBytes = readyOfflineBytes,
                            readyOfflineUnnamed = readyOfflineUnnamed,
                            readyOfflineHistoryDisabled = readyOfflineHistoryDisabled,
                            onReadyOfflineClick = onReadyOfflineClick,
                            viewModel = viewModel,
                            onShuffle = { onShuffleQueue(sortedSongs) }
                        )
                        LibraryTab.Playlists -> {
                            PlaylistsGrid(
                                playlists = userPlaylists,
                                localPlaylistIds = localPlaylistIds,
                                savedPlaylistIds = savedPlaylistIds,
                                likedSongs = likedSongs,
                                onPlaylistClick = onNavigateToPlaylist,
                                onRemoveSavedPlaylist = { playlist ->
                                    viewModel.removeSavedPlaylist(playlist.id)
                                },
                                onEditPlaylist = { playlist, newName, newDescription ->
                                    if (localPlaylistIds.contains(playlist.id)) {
                                        viewModel.updateLocalPlaylist(
                                            playlist.id,
                                            newName,
                                            newDescription
                                        )
                                    } else {
                                        viewModel.renameYouTubePlaylist(
                                            playlist.id,
                                            newName,
                                            newDescription
                                        )
                                    }
                                },
                                onDeletePlaylist = { playlist ->
                                    if (localPlaylistIds.contains(playlist.id)) {
                                        viewModel.deleteLocalPlaylist(playlist.id)
                                    } else {
                                        viewModel.deleteYouTubePlaylist(playlist.id)
                                    }
                                },
                                onLikedSongsClick = {
                                    onNavigateToPlaylist(
                                        PlaylistDisplayItem(
                                            "Liked Songs",
                                            "LM",
                                            "You",
                                            likedSongs.size,
                                            null
                                        )
                                    )
                                },
                                hiddenPlaylists = hiddenPlaylists,
                                onHidePlaylist = { playlist -> viewModel.hidePlaylist(playlist) },
                                onUnhidePlaylist = { id -> viewModel.unhidePlaylist(id) },
                                contentPadding = contentPadding,
                                isLoggedIn = isYouTubeConnected,
                                importAction = { PlaylistImportButton(viewModel) },
                                onCreatePlaylist = onCreatePlaylist,
                                fromVideoMode =
                                    viewModel.videoPlaylistsForMusic.collectAsState().value
                            )
                        }
                        LibraryTab.Artists -> {
                            val tasteContext = LocalContext.current
                            val tasteStore = remember(tasteContext) {
                                com.sentup.android.data.TasteProfileStore(tasteContext)
                            }
                            val tasteProfile by tasteStore.profile.collectAsState()
                            ArtistsGrid(
                                songs = librarySongs,
                                followed = tasteProfile.artists,
                                onFollowedClick = onNavigateToFollowedArtist,
                                onArtistClick = onNavigateToArtist,
                                contentPadding = contentPadding,
                                sort = LibraryGroupSort.from(artistSort),
                                onSortChange = {
                                    themePreferences.setLibraryArtistSort(it.name)
                                },
                                playCounts = playCounts
                            )
                        }
                        LibraryTab.Albums -> {
                            AlbumsGrid(
                                songs = librarySongs,
                                onAlbumClick = onNavigateToAlbum,
                                contentPadding = contentPadding,
                                sort = LibraryGroupSort.from(albumSort),
                                onSortChange = {
                                    themePreferences.setLibraryAlbumSort(it.name)
                                },
                                playCounts = playCounts
                            )
                        }
                    }
                }
            }
        }

        // The broken sort capsule and its sheets are gone: the single flow needs
        // no per-tab sort button, and shuffle-all lives on the "Karıştır" chip.

    }
}

// ============ SUB-SCREENS (Lists & Grids) ============

/**
 * The All tab: one grouped card of destinations, the recently played rail,
 * then the tracks as one continuous card. The tracks do not each own a tile -
 * that is what made the old list read as a stack of separate buttons; here
 * they are rows of a single object, exactly like the destinations above them.
 */
@Composable
fun AllSongsList(
    songs: List<Song>,
    likedSongs: List<Song>,
    downloadedSongs: List<Song>,
    recentlyPlayed: List<Song>,
    playCounts: Map<String, Int>,
    sortOption: LibrarySortOption,
    onSortOptionChange: (LibrarySortOption) -> Unit,
    onSongClick: (Song) -> Unit,
    onPlayQueue: (List<Song>, Song?) -> Unit,
    /** Play a collection with shuffle mode on; see PlayerViewModel.playQueueShuffled. */
    onShuffleQueue: (List<Song>) -> Unit,
    onDownloadsClick: () -> Unit,
    onLikedSongsClick: () -> Unit,
    onNavigateToHistory: () -> Unit,
    /** Songs cached in full; the card hides the row when nothing is cached. */
    readyOfflineCount: Int,
    readyOfflineBytes: Long,
    readyOfflineUnnamed: Int,
    readyOfflineHistoryDisabled: Boolean,
    onReadyOfflineClick: () -> Unit,
    contentPadding: PaddingValues,
    onSongLongPress: ((Song) -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    onNavigateToStats: () -> Unit = {},
    onCreatePlaylist: (() -> Unit)? = null
) {
    val todayLabel = stringResource(R.string.sc_today)
    val yesterdayLabel = stringResource(R.string.lh_yesterday)
    val likedIds = remember(likedSongs) { likedSongs.map { it.id }.toSet() }
    val downloadedIds = remember(downloadedSongs) { downloadedSongs.map { it.id }.toSet() }

    // Extra bottom room so the last row clears the floating sort capsule.
    LazyColumn(
        state = listState,
        contentPadding = PaddingValues(
            top = 12.dp,
            bottom = contentPadding.calculateBottomPadding() + 104.dp
        ),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "library_shortcuts") {
            LibraryShortcutsCard(
                likedCount = likedSongs.size,
                onLikedClick = onLikedSongsClick,
                downloadCount = downloadedSongs.size.takeIf { it > 0 },
                onDownloadsClick = onDownloadsClick,
                readyOfflineCount = readyOfflineCount,
                readyOfflineBytes = readyOfflineBytes,
                readyOfflineUnnamed = readyOfflineUnnamed,
                readyOfflineHistoryDisabled = readyOfflineHistoryDisabled,
                onReadyOfflineClick = onReadyOfflineClick,
                historyPlayCount = playCounts.values.sum(),
                onHistoryClick = onNavigateToHistory,
                onStatsClick = onNavigateToStats,
                onCreatePlaylist = onCreatePlaylist
            )
        }

        // Recently played rail: tiles on the page, the same object language as
        // the grids, titles kept to two lines so the rail has one height.
        if (recentlyPlayed.isNotEmpty()) {
            item(key = "recent_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = SentupGroup.gap - 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SentupGroupHeader(
                        text = stringResource(R.string.recently_played),
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = onNavigateToHistory) {
                        Text(stringResource(R.string.action_see_all))
                    }
                }
            }
            item(key = "recent_row") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(LibraryTileGap),
                    contentPadding = PaddingValues(
                        start = SentupGroup.inset,
                        end = SentupGroup.inset,
                        top = 2.dp,
                        bottom = 4.dp
                    )
                ) {
                    items(recentlyPlayed, key = { "recent_${it.id}" }) { song ->
                        RecentSongCard(
                            song = song,
                            onClick = { onPlayQueue(recentlyPlayed, song) },
                            onLongPress = onSongLongPress?.let { press -> { press(song) } }
                        )
                    }
                }
            }
        }

        item(key = "tracks_header") {
            SentupGroupHeader(
                text = pluralStringResource(R.plurals.n_songs, songs.size, songs.size),
                subtitle = sortOption.label,
                modifier = Modifier.padding(top = SentupGroup.gap - 8.dp)
            )
        }

        if (songs.isEmpty()) {
            item(key = "empty_state") {
                EmptyLibraryState(
                    icon = Icons.Rounded.MusicNote,
                    title = stringResource(R.string.library_empty_title),
                    subtitle = stringResource(R.string.library_empty_subtitle),
                    action = onCreatePlaylist?.let { create ->
                        {
                            Button(
                                onClick = create,
                                shape = RoundedCornerShape(SentupRadii.pill)
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.action_new_playlist))
                            }
                        }
                    }
                )
            }
        } else {
            itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                // Under a non-alphabetical sort the order looks arbitrary
                // unless the value it sorted on is visible, so surface it in
                // place of the duration.
                val sortLabel = when (sortOption) {
                    LibrarySortOption.MostPlayed -> playCounts[song.id]
                        ?.takeIf { it > 0 }
                        ?.let { pluralStringResource(R.plurals.n_plays, it, it) }
                    LibrarySortOption.RecentlyAdded ->
                        song.dateAdded?.let {
                            formatRelativeDate(it, todayLabel, yesterdayLabel)
                        }
                    else -> null
                }
                val isFirst = index == 0
                val isLast = index == songs.lastIndex
                Box(
                    modifier = Modifier
                        .padding(horizontal = SentupGroup.inset)
                        .clip(libraryCardSectionShape(isFirst, isLast))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier.padding(
                            top = if (isFirst) 4.dp else 0.dp,
                            bottom = if (isLast) 4.dp else 0.dp
                        )
                    ) {
                        if (index > 0) SentupDivider()
                        LibraryTrackRow(
                            song = song,
                            isLiked = song.id in likedIds,
                            isDownloaded = song.id in downloadedIds,
                            showDuration = sortLabel == null,
                            trailingLabel = sortLabel,
                            onLongPress = onSongLongPress?.let { press -> { press(song) } },
                            onClick = { onPlayQueue(songs, song) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Where the Library goes, as one inset-grouped card: Liked Songs, Downloads,
 * Ready offline, Listening history, Stats and New playlist are destinations,
 * not banners, and destinations read as rows. Each row is label, one line of
 * substance, and a chevron; the group's hairlines stop short of the edge.
 */
@Composable
private fun LibraryShortcutsCard(
    likedCount: Int,
    onLikedClick: () -> Unit,
    downloadCount: Int?,
    onDownloadsClick: () -> Unit,
    readyOfflineCount: Int,
    readyOfflineBytes: Long,
    readyOfflineUnnamed: Int,
    readyOfflineHistoryDisabled: Boolean,
    onReadyOfflineClick: () -> Unit,
    historyPlayCount: Int,
    onHistoryClick: () -> Unit,
    onStatsClick: () -> Unit,
    onCreatePlaylist: (() -> Unit)?
) {
    val showReadyOffline =
        readyOfflineCount > 0 || (readyOfflineHistoryDisabled && readyOfflineUnnamed > 0)
    val readyOfflineSubtitle = when {
        readyOfflineCount > 0 -> buildString {
            append(pluralStringResource(R.plurals.n_songs, readyOfflineCount, readyOfflineCount))
            if (readyOfflineBytes > 0) append(" \u00b7 ${formatCacheSize(readyOfflineBytes)}")
        }
        // Deliberately names the setting rather than saying "unavailable": the
        // fix is one toggle and the user is the only one who can make it.
        readyOfflineUnnamed == 1 -> "1 cached song. Turn on listening history to list it"
        else -> "$readyOfflineUnnamed cached songs. Turn on listening history to list them"
    }
    // Not clickable in the explanatory state - there is nothing behind it,
    // and a row that opens an empty screen is worse than one that does not move.
    val readyOfflineClickable = readyOfflineCount > 0

    SentupGroupCard(showDividers = false) {
        var shown = false
        if (likedCount > 0) {
            SentupRow(
                title = stringResource(R.string.liked_songs),
                icon = Icons.Rounded.Favorite,
                value = pluralStringResource(R.plurals.n_songs, likedCount, likedCount),
                showChevron = true,
                onClick = onLikedClick
            )
            shown = true
        }
        // Always listed, even with nothing finished: Downloads is also where
        // a download in progress is watched and paused, and this row is the
        // Library's only way there.
        if (shown) SentupDivider()
        SentupRow(
            title = stringResource(R.string.fab_downloads),
            icon = Icons.Rounded.DownloadDone,
            subtitle = if (downloadCount != null) {
                pluralStringResource(R.plurals.n_songs, downloadCount, downloadCount)
            } else {
                stringResource(R.string.library_downloads_none)
            },
            showChevron = true,
            onClick = onDownloadsClick
        )
        shown = true
        if (showReadyOffline) {
            if (shown) SentupDivider()
            SentupRow(
                title = stringResource(R.string.ready_offline),
                icon = Icons.Rounded.OfflineBolt,
                subtitle = readyOfflineSubtitle,
                showChevron = readyOfflineClickable,
                onClick = if (readyOfflineClickable) onReadyOfflineClick else null
            )
            shown = true
        }
        if (shown) SentupDivider()
        SentupRow(
            title = stringResource(R.string.fab_listening_history),
            icon = Icons.Rounded.History,
            subtitle = when {
                historyPlayCount <= 0 -> stringResource(R.string.lib_everything_in_order)
                else -> pluralStringResource(R.plurals.n_plays, historyPlayCount, historyPlayCount)
            },
            showChevron = true,
            onClick = onHistoryClick
        )
        SentupDivider()
        SentupRow(
            title = stringResource(R.string.cd_listening_stats),
            icon = Icons.Rounded.Insights,
            showChevron = true,
            onClick = onStatsClick
        )
        if (onCreatePlaylist != null) {
            SentupDivider()
            SentupRow(
                title = stringResource(R.string.action_new_playlist),
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                showChevron = true,
                onClick = onCreatePlaylist
            )
        }
    }
}

/**
 * One track inside the continuous card. Same press language as [SentupRow] -
 * 0.985 for 90ms, no ripple - with a cover tile and a quiet right-hand value:
 * the duration, or whatever the current sort actually sorted on.
 */
@Composable
private fun LibraryTrackRow(
    song: Song,
    isLiked: Boolean,
    isDownloaded: Boolean,
    showDuration: Boolean,
    trailingLabel: String?,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "trackRowPress"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(SentupRadii.tile))
            .then(
                if (onLongPress == null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    val haptics = com.sentup.android.util.rememberSentupHaptics()
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress()
                        }
                    )
                }
            )
            .heightIn(min = 56.dp)
            .padding(horizontal = SentupGroup.dividerInset, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(SentupRadii.tile))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            val art = song.highResThumbnailUrl ?: song.albumArtUri?.toString() ?: song.thumbnailUrl
            if (!art.isNullOrBlank()) {
                AsyncImage(
                    model = art,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Spacer(Modifier.width(13.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = song.artist,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 1.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (isDownloaded) {
                Icon(
                    Icons.Rounded.DownloadDone,
                    contentDescription = stringResource(R.string.song_options_downloaded),
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            if (isLiked) {
                Icon(
                    Icons.Rounded.Favorite,
                    contentDescription = stringResource(R.string.cd_favorite),
                    modifier = Modifier.size(15.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            val value = trailingLabel
                ?: if (showDuration && song.duration > 0) formatSongDuration(song.duration) else null
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.labelMedium,
                    color = if (trailingLabel != null) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 110.dp)
                )
            }
        }
    }
}

/** A recently played cover: tile corners, 2-line title, artist beneath. */
@Composable
private fun RecentSongCard(
    song: Song,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "recentTilePress"
    )
    Column(
        modifier = Modifier
            .width(116.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(SentupRadii.tile))
            .then(
                if (onLongPress == null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    val haptics = com.sentup.android.util.rememberSentupHaptics()
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                        onLongClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onLongPress()
                        }
                    )
                }
            )
    ) {
        Box(
            modifier = Modifier
                .size(116.dp)
                .clip(RoundedCornerShape(SentupRadii.tile))
                .sentupCard(
                    shape = RoundedCornerShape(SentupRadii.tile),
                    elevation = 1,
                    tinted = false
                ),
            contentAlignment = Alignment.Center
        ) {
            val art = song.albumArtUri ?: song.highResThumbnailUrl ?: song.thumbnailUrl
            if (art != null) {
                AsyncImage(
                    model = art,
                    contentDescription = song.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Rounded.MusicNote,
                    contentDescription = null,
                    modifier = Modifier.size(30.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            song.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            minLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 2.dp)
        )
        Text(
            song.artist,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 2.dp, end = 2.dp, bottom = 6.dp)
        )
    }
}

@Composable
fun PlaylistsGrid(
    playlists: List<PlaylistDisplayItem>,
    localPlaylistIds: Set<String>,
    likedSongs: List<Song>,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onEditPlaylist: (playlist: PlaylistDisplayItem, newName: String, newDescription: String?) -> Unit,
    onDeletePlaylist: (playlist: PlaylistDisplayItem) -> Unit,
    onLikedSongsClick: () -> Unit,
    contentPadding: PaddingValues,
    savedPlaylistIds: Set<String> = emptySet(),
    onRemoveSavedPlaylist: (PlaylistDisplayItem) -> Unit = {},
    /**
     * Playlists the user told Sentüp not to list, and the two ways back. Passed
     * in rather than read from a repository here so this grid stays a pure
     * rendering of what it is given, as the rest of its arguments are.
     */
    hiddenPlaylists: List<com.sentup.android.data.HiddenPlaylist> = emptyList(),
    onHidePlaylist: (PlaylistDisplayItem) -> Unit = {},
    onUnhidePlaylist: (String) -> Unit = {},
    /** Signed in, so the account's own playlists live on YouTube for real. */
    isLoggedIn: Boolean = false,
    importAction: @Composable () -> Unit,
    /** The empty state's own way out: it tells the user to make a playlist. */
    onCreatePlaylist: () -> Unit,
    /** Video mode's device playlists; see HomeViewModel.videoPlaylistsForMusic. */
    fromVideoMode: List<PlaylistDisplayItem> = emptyList()
) {
    var showHiddenSheet by remember { mutableStateOf(false) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 104.dp,
            start = SentupGroup.inset,
            end = SentupGroup.inset
        ),
        horizontalArrangement = Arrangement.spacedBy(LibraryTileGap),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Liked Songs as one quiet grouped row, not a poster: the same object
        // language as the All tab's destinations card.
        item(key = "liked_hero", span = { GridItemSpan(maxLineSpan) }) {
            ExpressiveLikedSongsCard(
                count = likedSongs.size,
                onClick = onLikedSongsClick
            )
        }

        // The header stands whenever there is either a playlist or something
        // hidden, because the hidden count is the only route back and a
        // library whose every playlist is hidden is exactly when it is needed.
        item(key = "playlists_header", span = { GridItemSpan(maxLineSpan) }) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SentupGroupHeader(
                    text = pluralStringResource(
                        R.plurals.n_playlists,
                        playlists.size,
                        playlists.size
                    ),
                    modifier = Modifier.weight(1f)
                )
                importAction()
                if (hiddenPlaylists.isNotEmpty()) {
                    TextButton(onClick = { showHiddenSheet = true }) {
                        Icon(
                            Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            stringResource(
                                R.string.lib_hidden_count,
                                hiddenPlaylists.size
                            )
                        )
                    }
                }
            }
        }
        if (playlists.isEmpty()) {
            item(key = "playlists_empty", span = { GridItemSpan(maxLineSpan) }) {
                EmptyLibraryState(
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    title = stringResource(R.string.spotlight_empty_no_playlists),
                    subtitle = stringResource(R.string.library_start_playlist_hint),
                    action = {
                        Button(
                            onClick = onCreatePlaylist,
                            shape = RoundedCornerShape(SentupRadii.pill)
                        ) {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.action_new_playlist))
                        }
                    }
                )
            }
        }

        items(playlists, key = { it.id }) { playlist ->
            val isLocalPlaylist = localPlaylistIds.contains(playlist.id)
            // Kept, not owned: renaming or deleting one would be a write
            // against somebody else's playlist, so it gets "Remove from
            // library" in place of Edit/Delete.
            val isSavedPlaylist = savedPlaylistIds.contains(playlist.id)
            // YouTube playlists are editable through InnerTube; the synthesized
            // Supermix ("RTM") and Your Likes ("LM") entries are not real playlists
            val isYouTubeEditable = !isLocalPlaylist && !isSavedPlaylist &&
                playlist.id != "RTM" && playlist.id != "LM"
            ExpressivePlaylistCard(
                name = playlist.name?.let { listNameLabel(it) } ?: stringResource(R.string.untitled_song),
                count = playlist.itemCount,
                // A saved playlist's author is the thing that tells it apart
                // from the user's own at a glance, so it wins the subtitle.
                subtitle = when {
                    isSavedPlaylist || playlist.itemCount < 0 ->
                        playlist.displaySubtitle().ifBlank { stringResource(R.string.label_playlist) }
                    else -> null
                },
                thumbnailUrl = playlist.thumbnailUrl,
                description = playlist.description,
                isEditable = isLocalPlaylist || isYouTubeEditable,
                isSaved = isSavedPlaylist,
                affectsYouTubeAccount = isYouTubeEditable && isLoggedIn,
                onEditConfirmed = { name, description -> onEditPlaylist(playlist, name, description) },
                onDeleteConfirmed = { onDeletePlaylist(playlist) },
                onRemoveSaved = { onRemoveSavedPlaylist(playlist) },
                onHide = { onHidePlaylist(playlist) },
                selected = LocalLibraryOpenItem.current?.playlistId == playlist.id,
                onClick = { onPlaylistClick(playlist) }
            )
        }

        // Video mode's own device playlists, playable here as songs. Read-only
        // in this mode (no edit menu): they are edited where they live.
        if (fromVideoMode.isNotEmpty()) {
            item(key = "from_video_header", span = { GridItemSpan(maxLineSpan) }) {
                SentupGroupHeader(
                    text = stringResource(R.string.lib_from_video_mode),
                    modifier = Modifier.padding(top = SentupGroup.gap - 8.dp)
                )
            }
            items(fromVideoMode, key = { "from_video_${it.id}" }) { playlist ->
                ExpressivePlaylistCard(
                    name = playlist.name?.let { listNameLabel(it) } ?: stringResource(R.string.untitled_song),
                    count = playlist.itemCount,
                    subtitle = playlist.displaySubtitle(),
                    thumbnailUrl = playlist.thumbnailUrl,
                    onHide = { onHidePlaylist(playlist) },
                    selected = LocalLibraryOpenItem.current?.playlistId == playlist.id,
                    onClick = { onPlaylistClick(playlist) }
                )
            }
        }
    }

    if (showHiddenSheet) {
        HiddenPlaylistsSheet(
            hidden = hiddenPlaylists,
            onUnhide = onUnhidePlaylist,
            onDismiss = { showHiddenSheet = false }
        )
    }
}

/**
 * What has been hidden, and the way back.
 *
 * This screen is the whole reason hiding is safe to offer. Without it a
 * mis-tap weeks ago is an invisible hole in the library that the user has no
 * way to explain or repair - the same reason `NotInterestedScreen` exists for
 * blocked channels. The entry point is a count beside the section header,
 * shown only when there is something to show, so it costs nothing until it
 * matters.
 *
 * Un-hiding is immediate and needs no confirmation: nothing is destroyed in
 * either direction, and the playlist reappears in the grid behind this sheet
 * on the next frame.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HiddenPlaylistsSheet(
    hidden: List<com.sentup.android.data.HiddenPlaylist>,
    onUnhide: (String) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.lib_hidden_playlists),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = SentupGroup.inset + SentupGroup.dividerInset,
                    end = SentupGroup.inset + SentupGroup.dividerInset,
                    bottom = 4.dp
                )
            )
            Text(
                text = stringResource(R.string.lib_hidden_explainer),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = SentupGroup.inset + SentupGroup.dividerInset,
                    end = SentupGroup.inset + SentupGroup.dividerInset,
                    bottom = 12.dp
                )
            )

            // A LazyColumn inside the inset card, height-capped: someone can
            // hide a great many playlists, and an unbounded Column would run
            // the sheet off the bottom of the screen.
            SentupGroupCard(showDividers = false) {
                LazyColumn(modifier = Modifier.heightIn(max = 420.dp)) {
                    itemsIndexed(hidden, key = { _, entry -> entry.playlistId }) { index, entry ->
                        if (index > 0) SentupDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = SentupGroup.dividerInset,
                                    end = 6.dp,
                                    top = 8.dp,
                                    bottom = 8.dp
                                ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(SentupRadii.tile))
                                    .background(
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!entry.thumbnailUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = entry.thumbnailUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.AutoMirrored.Rounded.PlaylistPlay,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(13.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = entry.name.ifBlank {
                                        stringResource(R.string.label_playlist)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                entry.uploaderName.takeIf { it.isNotBlank() }?.let { by ->
                                    Text(
                                        text = by,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { onUnhide(entry.playlistId) }) {
                                Text(stringResource(R.string.lib_unhide))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ArtistsGrid(
    songs: List<Song>,
    onArtistClick: (String) -> Unit,
    contentPadding: PaddingValues,
    sort: LibraryGroupSort = LibraryGroupSort.Name,
    onSortChange: (LibraryGroupSort) -> Unit = {},
    playCounts: Map<String, Int> = emptyMap(),
    /** Artists followed on this device, shown above the ones the library's songs name. */
    followed: List<com.sentup.android.data.TasteArtist> = emptyList(),
    onFollowedClick: (com.sentup.android.data.TasteArtist) -> Unit = {}
) {
    val artists = remember(songs, sort, playCounts) {
        sort.apply(
            songs.filterNot { isUnknownArtist(it.artist) }.groupSongsByArtist()
        ) { song -> playCounts[song.id] ?: 0 }
    }

    if (artists.isEmpty() && followed.isEmpty()) {
        EmptyLibraryState(
            icon = Icons.Rounded.Person,
            title = stringResource(R.string.no_artists_yet),
            subtitle = stringResource(R.string.no_artists_yet_subtitle)
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 132.dp),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 104.dp,
            start = SentupGroup.inset,
            end = SentupGroup.inset
        ),
        horizontalArrangement = Arrangement.spacedBy(LibraryTileGap),
        verticalArrangement = Arrangement.spacedBy(22.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Following first: these are the artists the user chose by name,
        // where the list below is whoever their songs happen to credit. An
        // artist can be in both, and is: one says "I follow them", the other
        // "I have their songs".
        if (followed.isNotEmpty()) {
            item(key = "followed_header", span = { GridItemSpan(maxLineSpan) }) {
                SentupGroupHeader(text = stringResource(R.string.lib_following_header))
            }
            items(followed, key = { "followed_" + it.key }) { artist ->
                ArtistTile(
                    name = artist.name,
                    caption = stringResource(R.string.artist_following),
                    captionIsPrimary = true,
                    thumbnailUrl = artist.thumbnailUrl,
                    selected = LocalLibraryOpenItem.current?.artistName == artist.name,
                    onClick = { onFollowedClick(artist) }
                )
            }
        }
        if (artists.isNotEmpty()) {
            item(key = "artists_header", span = { GridItemSpan(maxLineSpan) }) {
                SentupGroupHeader(
                    text = pluralStringResource(R.plurals.lib_artist_count, artists.size, artists.size),
                    modifier = Modifier.padding(
                        top = if (followed.isNotEmpty()) SentupGroup.gap - 8.dp else 0.dp
                    )
                )
            }
        }
        items(artists, key = { (name, _) -> name }) { (artist, artistSongs) ->
            ArtistTile(
                name = artist,
                caption = pluralStringResource(
                    R.plurals.n_songs,
                    artistSongs.size,
                    artistSongs.size
                ),
                captionIsPrimary = false,
                thumbnailUrl = artistSongs.firstOrNull { it.albumArtUri != null }?.albumArtUri?.toString()
                    ?: artistSongs.firstOrNull { it.thumbnailUrl != null }?.thumbnailUrl,
                selected = LocalLibraryOpenItem.current?.artistName == artist,
                onClick = { onArtistClick(artist) }
            )
        }
    }
}

/**
 * An artist as a circle on the page. Circles read as people while the square
 * tiles read as things, and the press language is the same inward push as
 * every other object here.
 */
@Composable
private fun ArtistTile(
    name: String,
    caption: String,
    captionIsPrimary: Boolean,
    thumbnailUrl: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "artistTilePress"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .sentupCard(shape = CircleShape, elevation = 1, tinted = false)
                .then(
                    if (selected) {
                        Modifier.border(
                            2.dp,
                            MaterialTheme.colorScheme.primary,
                            CircleShape
                        )
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (!thumbnailUrl.isNullOrBlank()) {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    Icons.Rounded.Person,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            caption,
            style = MaterialTheme.typography.bodySmall,
            color = if (captionIsPrimary) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AlbumsGrid(
    songs: List<Song>,
    onAlbumClick: (String, List<Song>) -> Unit,
    contentPadding: PaddingValues,
    sort: LibraryGroupSort = LibraryGroupSort.Name,
    onSortChange: (LibraryGroupSort) -> Unit = {},
    playCounts: Map<String, Int> = emptyMap()
) {
    // Filter BEFORE building grid items - filtering inside the item lambda
    // leaves blank holes in the grid for skipped albums.
    val albums = remember(songs, sort, playCounts) {
        sort.apply(
            songs.filterNot { isUnknownAlbum(it.album) }.groupSongsByAlbum()
        ) { song -> playCounts[song.id] ?: 0 }
    }

    if (albums.isEmpty()) {
        EmptyLibraryState(
            icon = Icons.Rounded.Album,
            title = stringResource(R.string.no_albums_yet),
            subtitle = stringResource(R.string.no_albums_yet_subtitle)
        )
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 150.dp),
        contentPadding = PaddingValues(
            top = 8.dp,
            bottom = contentPadding.calculateBottomPadding() + 104.dp,
            start = SentupGroup.inset,
            end = SentupGroup.inset
        ),
        horizontalArrangement = Arrangement.spacedBy(LibraryTileGap),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item(key = "albums_header", span = { GridItemSpan(maxLineSpan) }) {
            SentupGroupHeader(
                text = pluralStringResource(R.plurals.lib_album_count, albums.size, albums.size)
            )
        }
        items(albums, key = { (name, _) -> name }) { (album, albumSongs) ->
            val art = albumSongs.firstOrNull { it.albumArtUri != null }?.albumArtUri?.toString()
                ?: albumSongs.firstOrNull { it.thumbnailUrl != null }?.thumbnailUrl
            ExpressivePlaylistCard(
                name = album,
                count = albumSongs.size,
                thumbnailUrl = art,
                subtitle = albumArtistLabel(
                    albumSongs,
                    stringResource(R.string.various_artists)
                ),
                selected = LocalLibraryOpenItem.current?.albumName == album,
                onClick = { onAlbumClick(album, albumSongs) }
            )
        }
    }
}

// ============ UI COMPONENTS ============

/**
 * Liked Songs as a destination row, not a banner: one inset-grouped card, the
 * heart doing the identifying in the brand's own color, the count as the
 * row's value.
 */
@Composable
fun ExpressiveLikedSongsCard(count: Int, onClick: () -> Unit) {
    SentupGroupCard(showDividers = false) {
        SentupRow(
            title = stringResource(R.string.liked_songs),
            icon = Icons.Rounded.Favorite,
            value = pluralStringResource(R.plurals.n_songs, count, count),
            showChevron = true,
            onClick = onClick
        )
    }
}

/**
 * A playlist or album as a floating cover. The artwork is the object - tile
 * corners, one step of shadow, its count resting on it - and the text below
 * is two lines, never more. Tapping presses the whole thing in; nothing
 * flashes.
 */
@Composable
fun ExpressivePlaylistCard(
    name: String,
    count: Int,
    thumbnailUrl: String?,
    subtitle: String? = null,
    description: String? = null,
    isLiked: Boolean = false,
    isEditable: Boolean = false,
    isSaved: Boolean = false,
    /**
     * Deleting this card destroys a playlist on the signed-in YouTube account,
     * not just on the device. Signed out, or for a local playlist, it is false
     * and the confirmation says nothing about YouTube.
     */
    affectsYouTubeAccount: Boolean = false,
    /**
     * Stop listing this playlist in Sentüp. Null where hiding makes no sense;
     * non-null it is offered on every card, editable or not.
     */
    onHide: (() -> Unit)? = null,
    onEditConfirmed: (String, String?) -> Unit = { _, _ -> },
    onDeleteConfirmed: () -> Unit = {},
    onRemoveSaved: () -> Unit = {},
    /** Open in the detail pane beside the Library list; see [LocalLibraryOpenItem]. */
    selected: Boolean = false,
    onClick: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "playlistCardScale"
    )
    val coverShape = RoundedCornerShape(SentupRadii.tile)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                // The card paints its own shadow outside these bounds, so the
                // shadow lives on this node and the clip stays inside it.
                .sentupCard(shape = coverShape, elevation = 1, tinted = false)
                .then(
                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, coverShape)
                    else Modifier
                )
        ) {
            if (thumbnailUrl != null && thumbnailUrl != "null") {
                AsyncImage(
                    model = thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    if (isLiked) Icons.Rounded.Favorite
                    else Icons.AutoMirrored.Rounded.PlaylistPlay,
                    contentDescription = null,
                    modifier = Modifier.size(38.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // Saved marker, opposite corner to the track count. A saved
            // playlist otherwise looks exactly like one the user made, and the
            // actions behind it are not the same.
            if (isSaved) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(SentupRadii.pill),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Icon(
                        Icons.Rounded.Bookmark,
                        contentDescription = stringResource(R.string.cd_save_to_library),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .size(14.dp)
                    )
                }
            }
            // Track-count chip floating on the artwork
            if (count > 0) {
                Surface(
                    color = Color.Black.copy(alpha = 0.55f),
                    shape = RoundedCornerShape(SentupRadii.pill),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(8.dp)
                ) {
                    Text(
                        text = "$count",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    subtitle ?: pluralStringResource(R.plurals.n_songs, count, count),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (isEditable || isSaved || onHide != null) {
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.cd_playlist_options),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        // Hiding is offered on every card, including the ones
                        // with nothing else in this menu: the synthesized
                        // Supermix and Your Likes entries cannot be renamed,
                        // deleted or unsaved, and they are exactly the rows
                        // people want out of the way.
                        onHide?.let { hide ->
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.lib_hide_playlist)) },
                                onClick = {
                                    showMenu = false
                                    hide()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.VisibilityOff, contentDescription = null)
                                }
                            )
                        }
                        if (isSaved) {
                            // Removing the reference, not the playlist: no
                            // confirmation dialog, because nothing is destroyed
                            // and saving it again is one tap on its page.
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.cd_remove_from_library)) },
                                onClick = {
                                    showMenu = false
                                    onRemoveSaved()
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.BookmarkRemove, contentDescription = null)
                                }
                            )
                        } else {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_edit)) },
                                onClick = {
                                    showMenu = false
                                    showEditDialog = true
                                },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.action_delete)) },
                                onClick = {
                                    showMenu = false
                                    showDeleteDialog = true
                                },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Delete, contentDescription = null)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditPlaylistDialog(
            initialName = name,
            initialDescription = description,
            onDismiss = { showEditDialog = false },
            onConfirm = { newName, newDescription ->
                onEditConfirmed(newName, newDescription)
                showEditDialog = false
            }
        )
    }
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_playlist_q)) },
            text = {
                Text(
                    if (affectsYouTubeAccount) {
                        "This will permanently remove \"$name\" from your YouTube account. " +
                            stringResource(R.string.lib_delete_body)
                    } else {
                        "This will permanently remove \"$name\"."
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteConfirmed()
                    showDeleteDialog = false
                }) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
@Composable
private fun EditPlaylistDialog(
    initialName: String,
    initialDescription: String?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String?) -> Unit,
    title: String? = null
) {
    var name by remember(initialName) { mutableStateOf(initialName) }
    var description by remember(initialDescription) { mutableStateOf(initialDescription ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(SentupRadii.panel),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Surface(
                shape = RoundedCornerShape(SentupRadii.tile),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Rounded.Edit,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        },
        title = { Text(title ?: stringResource(R.string.lib_edit_playlist)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.name_label)) },
                    singleLine = true,
                    shape = RoundedCornerShape(SentupRadii.tile)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.description_optional_label)) },
                    maxLines = 2,
                    shape = RoundedCornerShape(SentupRadii.tile)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), description.trim().ifBlank { null }) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        }
    )
}

/**
 * A song as one row of an inset-grouped card: cover tile, title and artist,
 * and a quiet right-hand value. Used by the Library's sibling screens; the
 * Library's own list draws the same language through [LibraryTrackRow].
 */
@Composable
fun SongListItem(
    song: Song,
    modifier: Modifier = Modifier,
    containerColor: Color = Color.Transparent,
    tonalElevation: Dp = 0.dp,
    shadowElevation: Dp = 0.dp,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(0.dp),
    isLiked: Boolean = false,
    isDownloaded: Boolean = false,
    showDuration: Boolean = false,
    /** Short metric shown where the duration normally sits (e.g. "12 plays"). */
    trailingLabel: String? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    /** Opens the song options sheet. Null leaves the row tap-only. */
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        shape = shape,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .songRowClick(onClick = onClick, onLongClick = onLongClick)
                .heightIn(min = 56.dp)
                .padding(horizontal = SentupGroup.dividerInset, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(SentupRadii.tile))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                val art = song.highResThumbnailUrl ?: song.albumArtUri?.toString() ?: song.thumbnailUrl
                if (!art.isNullOrBlank()) {
                    AsyncImage(
                        model = art,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Spacer(Modifier.width(13.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
            val slot = trailingContent
            if (slot != null) {
                Spacer(Modifier.width(10.dp))
                slot()
            } else if (isLiked || isDownloaded || showDuration || trailingLabel != null) {
                Spacer(Modifier.width(10.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    if (isDownloaded) {
                        Icon(
                            Icons.Rounded.DownloadDone,
                            contentDescription = stringResource(R.string.song_options_downloaded),
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (isLiked) {
                        Icon(
                            Icons.Rounded.Favorite,
                            contentDescription = stringResource(R.string.cd_favorite),
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    val value = trailingLabel
                        ?: if (showDuration && song.duration > 0) formatSongDuration(song.duration) else null
                    if (value != null) {
                        Text(
                            value,
                            style = MaterialTheme.typography.labelMedium,
                            color = if (trailingLabel != null) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tap to play, hold for the options sheet.
 *
 * Falls back to a plain `clickable` when there is no long-press action, because
 * `combinedClickable` with a null `onLongClick` still consumes the long press
 * and swallows it from anything underneath. Presses push the row in slightly
 * instead of flooding it with ripple - the row is an object being pressed, not
 * a light being switched.
 */
@Composable
internal fun Modifier.songRowClick(
    onClick: () -> Unit,
    onLongClick: (() -> Unit)?
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "songRowPress"
    )
    val press = this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clip(RoundedCornerShape(SentupRadii.tile))
    return if (onLongClick == null) {
        press.clickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
    } else {
        val haptics = com.sentup.android.util.rememberSentupHaptics()
        press.combinedClickable(
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick,
            onLongClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onLongClick()
            }
        )
    }
}

/** Compact "when was this added" label for the Recently added sort. */
private fun formatRelativeDate(
    timestamp: Long,
    todayLabel: String,
    yesterdayLabel: String
): String {
    val days = (System.currentTimeMillis() - timestamp) / 86_400_000L
    return when {
        days < 0L -> todayLabel // Clock skew or a file dated in the future
        days == 0L -> todayLabel
        days == 1L -> yesterdayLabel
        days < 7L -> "${days}d ago"
        days < 30L -> "${days / 7}w ago"
        days < 365L -> "${days / 30}mo ago"
        else -> "${days / 365}y ago"
    }
}

private fun formatSongDuration(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(java.util.Locale.US, "%d:%02d", minutes, seconds)
    }
}

/** Bytes as MB or GB, matching how the cache size reads in Settings. */
private fun formatCacheSize(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024) String.format("%.1f GB", mb / 1024) else "${mb.toInt()} MB"
}

/**
 * Floating toolbar shown instead of the play split button while the
 * playlist is in edit mode. Kept vibrant: it marks a temporary change of page
 * behavior, and it keeps Done reachable mid-list.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun ManageModeToolbar(
    hint: String,
    icon: ImageVector,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    HorizontalFloatingToolbar(
        expanded = true,
        modifier = modifier,
        colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        floatingActionButton = {
            FloatingToolbarDefaults.VibrantFloatingActionButton(onClick = onDone) {
                Icon(Icons.Rounded.Done, contentDescription = stringResource(R.string.cd_finish_editing))
            }
        }
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.padding(start = 8.dp)
        )
        Text(
            text = hint,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

/**
 * Nothing to show, said once: the icon seated in a soft disc, one sentence of
 * title, one line of why, and at most one way out. No illustration stack.
 */
@Composable
fun EmptyLibraryState(
    title: String,
    subtitle: String,
    icon: ImageVector = Icons.AutoMirrored.Rounded.List,
    /** Optional recovery action, e.g. a Retry button on a failed fetch. */
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 72.dp, start = 28.dp, end = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(44.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (action != null) {
            Spacer(Modifier.height(24.dp))
            action()
        }
    }
}

// ============ PLAYLIST DETAIL SCREEN ============

/** Row height used only to size the loading skeleton; real rows measure themselves. */
private val TrackRowHeight = 68.dp

/**
 * Section header above the track list. Carries the count and, for editable
 * playlists, the Edit/Done toggle - the mode switch sits with the content it
 * changes rather than in the app bar.
 */
@Composable
private fun TrackSectionHeader(
    countLabel: String,
    manageEnabled: Boolean,
    canEdit: Boolean,
    onToggleEdit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Tracks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                // Stands on the page's ground, not on a theme surface.
                color = playlistOnGround()
            )
            Text(
                text = countLabel,
                style = MaterialTheme.typography.bodySmall,
                color = playlistOnGround().copy(alpha = 0.78f)
            )
        }
        if (canEdit) {
            TextButton(
                onClick = onToggleEdit,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Icon(
                    imageVector = if (manageEnabled) Icons.Rounded.Done else Icons.Rounded.EditNote,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(if (manageEnabled) stringResource(R.string.action_done) else stringResource(R.string.action_edit))
            }
        }
    }
}

/**
 * Placeholder rows shown while the track list is being fetched. Skeletons are
 * the house pattern for content whose shape is known in advance - they say how
 * much is coming, which a bare spinner cannot.
 */
@Composable
internal fun TrackSkeletonList(rows: Int = 6) {
    val transition = rememberInfiniteTransition(label = "trackSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 0.70f,
        // A pulse is a timed effect, not spatial motion, so a tween is correct here.
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "trackSkeletonAlpha"
    )
    val placeholder = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = alpha)
    Column {
        repeat(rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TrackRowHeight)
                    .padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(placeholder)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(placeholder)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.36f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(placeholder)
                    )
                }
            }
        }
    }
}

/**
 * One track row on the playlist page.
 *
 * Entering edit mode only swaps the trailing controls - rows keep their place
 * and their geometry. Only the row actually being dragged lifts into a card
 * (tonal fill, rounded corners, shadow, slight scale). That is the standard
 * reorder grammar, and it keeps mode entry to a content swap on the handful of
 * visible rows instead of a list-wide layout animation.
 *
 * The drag handle is a real handle: dragging it starts a reorder immediately.
 * A long press anywhere on the row is the accelerator for the same gesture.
 * Because the handle works on touch, no row needs a "hold" caption explaining
 * itself, and the destructive Remove stays a calm outlined glyph instead of a
 * filled block competing with the track for attention.
 *
 * Note on specs: the Dp animations below deliberately use a non-bouncy spring.
 * Corner radius and shadow elevation are clamped non-negative by the framework,
 * and an Expressive *spatial* spec is bouncy - it undershoots past zero on the
 * way back down and throws. Bouncy specs belong on scale, which can overshoot
 * safely.
 */
@Composable
private fun PlaylistTrackRow(
    song: Song,
    /** The music player is on this song: the row stands out (see below). */
    isCurrent: Boolean = false,
    isPlaying: Boolean = false,
    manageEnabled: Boolean,
    reorderEnabled: Boolean,
    isDragging: Boolean,
    dragHandleModifier: Modifier,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    /**
     * Connected-group geometry: the list gives the first row a round top and
     * the last row a round bottom while the seams between rows stay small, so
     * the whole track list reads as one container like the library shortcuts
     * card. A dragged row still lifts into a fully round card of its own.
     */
    topCorner: Dp = 20.dp,
    bottomCorner: Dp = 20.dp
) {
    val liftSpec = spring<Dp>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMediumLow
    )
    // Rows stand on the page's own ground rather than on a neutral surface
    // role - see [playlistRaisedSurface] for why a `surfaceContainerLow` card
    // lands on the wrong side of a colored ground in a dark theme.
    // The playing row is the segmented list's "selected" state: a solid
    // `primary` fill and a fully rounded segment, so it stands out of the group
    // at a glance and moves when playback moves on. Not primaryContainer: this
    // page's ground *is* that colour (see playlistPageGround), so a container
    // fill vanished into it. `primary` is the cover's colour at the opposite
    // tone - bright on the dark ground, deep on the light one - which is the
    // contrast that survives every cover.
    val highlighted = isCurrent && !isDragging
    val containerColor by animateColorAsState(
        targetValue = when {
            isDragging -> MaterialTheme.colorScheme.secondaryContainer
            highlighted -> MaterialTheme.colorScheme.primary
            else -> playlistRaisedSurface()
        },
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "trackContainer"
    )
    val contentColor by animateColorAsState(
        targetValue = if (highlighted) MaterialTheme.colorScheme.onPrimary else playlistOnGround(),
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "trackContent"
    )
    val topRadius by animateDpAsState(
        targetValue = when {
            isDragging -> 24.dp
            highlighted -> 16.dp
            else -> topCorner
        },
        animationSpec = liftSpec,
        label = "trackTopCorner"
    )
    val bottomRadius by animateDpAsState(
        targetValue = when {
            isDragging -> 24.dp
            highlighted -> 16.dp
            else -> bottomCorner
        },
        animationSpec = liftSpec,
        label = "trackBottomCorner"
    )
    val shadowElevation by animateDpAsState(
        targetValue = if (isDragging) 12.dp else 0.dp,
        animationSpec = liftSpec,
        label = "trackShadow"
    )
    val liftScale by animateFloatAsState(
        targetValue = if (isDragging) 1.03f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "trackLift"
    )

    Surface(
        modifier = modifier.graphicsLayer {
            scaleX = liftScale
            scaleY = liftScale
        },
        color = containerColor,
        shape = RoundedCornerShape(
            topStart = topRadius.coerceAtLeast(0.dp),
            topEnd = topRadius.coerceAtLeast(0.dp),
            bottomStart = bottomRadius.coerceAtLeast(0.dp),
            bottomEnd = bottomRadius.coerceAtLeast(0.dp)
        ),
        tonalElevation = 2.dp,
        shadowElevation = shadowElevation.coerceAtLeast(0.dp)
    ) {
        // A plain Row rather than ListItem: on material3 1.5 alphas a long
        // headline starved the trailing duration to zero width.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = (if (manageEnabled) Modifier else Modifier.songRowClick(onClick = onClick, onLongClick = onLongClick))
                .fillMaxWidth()
                .heightIn(min = 72.dp)
                .padding(start = 16.dp, end = if (manageEnabled) 4.dp else 16.dp, top = 8.dp, bottom = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (song.albumArtUri != null || song.thumbnailUrl != null) {
                        AsyncImage(
                            model = song.highResThumbnailUrl ?: song.albumArtUri ?: song.thumbnailUrl,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    // Playing: the bars over a scrim on the artwork, moving
                    // while it plays and resting while paused.
                    androidx.compose.animation.AnimatedVisibility(
                        visible = highlighted,
                        enter = androidx.compose.animation.fadeIn() +
                            androidx.compose.animation.scaleIn(initialScale = 0.6f),
                        exit = androidx.compose.animation.fadeOut() +
                            androidx.compose.animation.scaleOut(targetScale = 0.6f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            com.sentup.android.ui.components.PlayingBars(
                                playing = isPlaying,
                                color = MaterialTheme.colorScheme.inverseOnSurface,
                                size = 20.dp
                            )
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontWeight = if (highlighted) FontWeight.Bold else FontWeight.SemiBold
                )
                Text(
                    song.artist,
                    style = MaterialTheme.typography.bodyMedium,
                    color = contentColor.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (manageEnabled) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRemove, enabled = !isDragging) {
                        Icon(
                            imageVector = Icons.Rounded.RemoveCircleOutline,
                            contentDescription = stringResource(R.string.cd_remove_song, song.title),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                    if (reorderEnabled) {
                        Box(
                            modifier = dragHandleModifier.size(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DragIndicator,
                                contentDescription = stringResource(R.string.cd_reorder_song, song.title),
                                tint = if (isDragging) {
                                    MaterialTheme.colorScheme.onSecondaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        }
                    }
                }
            } else if (song.duration > 0) {
                Text(
                    formatSongDuration(song.duration),
                    style = MaterialTheme.typography.labelMedium,
                    color = contentColor.copy(alpha = 0.7f),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
    }
}

/**
 * The swipe chrome around one playlist row. Both directions arm from the
 * swipe-actions setting, each naming its own action - remove, play or the
 * options sheet - so a right swipe can play while a left swipe removes.
 *
 * A swipe never keeps a dismissed row: removal leaves the list on its own
 * (a local row with an undo, a remote row through its confirm dialog), and
 * play and options leave nothing behind at all.
 */
@Composable
private fun PlaylistTrackSwipeContainer(
    startAction: String?,
    endAction: String?,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    // `rememberSwipeToDismissBoxState` holds the lambda it was first given,
    // so the actions resolve through updated state - a settings change must
    // reach rows that are already on screen.
    val currentStart by rememberUpdatedState(startAction)
    val currentEnd by rememberUpdatedState(endAction)
    val currentAction by rememberUpdatedState(onAction)
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            val action = when (value) {
                SwipeToDismissBoxValue.StartToEnd -> currentStart
                SwipeToDismissBoxValue.EndToStart -> currentEnd
                SwipeToDismissBoxValue.Settled -> null
            }
            if (action != null) currentAction(action)
            false
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        // A null direction does not arm rather than snapping back from a
        // gesture that looked like it worked.
        enableDismissFromStartToEnd = startAction != null,
        enableDismissFromEndToStart = endAction != null,
        backgroundContent = {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val widthPx = with(LocalDensity.current) { maxWidth.toPx() }
                // A snapshot read, so the backdrop recomposes with every
                // pixel of the drag rather than only past the threshold.
                val offsetPx = runCatching { dismissState.requireOffset() }.getOrDefault(0f)
                PlaylistTrackSwipeBackground(
                    offsetPx = offsetPx,
                    widthPx = widthPx,
                    startAction = startAction,
                    endAction = endAction,
                )
            }
        },
        modifier = modifier,
    ) {
        content()
    }
}

/**
 * What sits behind a playlist row being swiped: a tonal wash in the action's
 * color with the action riding it as a solid pill - icon plus name. The label
 * is the point: a wash alone does not say whether letting go plays the song
 * or removes it.
 *
 * The pill grows out of the drag - small and faint at first touch, full size
 * and ink at the threshold - leans slightly toward the finger, and lifts with
 * a shadow once past it, so "letting go runs this" is visible before it
 * happens. Finger-driven throughout, so values map straight off the offset
 * with no spring or tween involved.
 *
 * @param offsetPx live drag offset, 0 at rest. Its sign picks the side, its
 * magnitude the intensity: 1.0 is the dismiss threshold, half the row.
 */
@Composable
private fun PlaylistTrackSwipeBackground(
    offsetPx: Float,
    widthPx: Float,
    startAction: String?,
    endAction: String?,
) {
    val action = when {
        offsetPx > 0.5f -> startAction
        offsetPx < -0.5f -> endAction
        else -> null
    }
    val intensity = if (widthPx > 0f) {
        (kotlin.math.abs(offsetPx) / widthPx / 0.5f).coerceIn(0f, 1f)
    } else {
        0f
    }
    // Nothing to show at rest: the row covers this layer entirely, so settled
    // rows in a long list skip the icon and label work below.
    if (intensity <= 0f) return
    val container = when (action) {
        ThemePreferences.PLAYLIST_SWIPE_ACTION_REMOVE -> MaterialTheme.colorScheme.errorContainer
        ThemePreferences.PLAYLIST_SWIPE_ACTION_PLAY -> MaterialTheme.colorScheme.primaryContainer
        ThemePreferences.PLAYLIST_SWIPE_ACTION_QUEUE -> MaterialTheme.colorScheme.secondaryContainer
        ThemePreferences.PLAYLIST_SWIPE_ACTION_OPTIONS -> MaterialTheme.colorScheme.tertiaryContainer
        else -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val content = when (action) {
        ThemePreferences.PLAYLIST_SWIPE_ACTION_REMOVE -> MaterialTheme.colorScheme.onErrorContainer
        ThemePreferences.PLAYLIST_SWIPE_ACTION_PLAY -> MaterialTheme.colorScheme.onPrimaryContainer
        ThemePreferences.PLAYLIST_SWIPE_ACTION_QUEUE -> MaterialTheme.colorScheme.onSecondaryContainer
        ThemePreferences.PLAYLIST_SWIPE_ACTION_OPTIONS -> MaterialTheme.colorScheme.onTertiaryContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (action) {
        ThemePreferences.PLAYLIST_SWIPE_ACTION_REMOVE -> Icons.Rounded.DeleteOutline
        ThemePreferences.PLAYLIST_SWIPE_ACTION_PLAY -> Icons.Rounded.PlayArrow
        ThemePreferences.PLAYLIST_SWIPE_ACTION_QUEUE -> Icons.AutoMirrored.Rounded.QueueMusic
        else -> Icons.Rounded.MoreVert
    }
    val label = when (action) {
        ThemePreferences.PLAYLIST_SWIPE_ACTION_REMOVE -> stringResource(R.string.swipe_action_remove)
        ThemePreferences.PLAYLIST_SWIPE_ACTION_PLAY -> stringResource(R.string.swipe_action_play)
        ThemePreferences.PLAYLIST_SWIPE_ACTION_QUEUE -> stringResource(R.string.swipe_action_queue)
        ThemePreferences.PLAYLIST_SWIPE_ACTION_OPTIONS -> stringResource(R.string.swipe_action_options)
        else -> null
    }
    // Past the dismiss threshold the finger has committed.
    val committed = intensity >= 1f

    // No clip of its own: the swipe container is already clipped to the row's
    // connected-group shape, so this wash is exactly card-shaped for free.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(container.copy(alpha = 0.35f + 0.65f * intensity)),
        contentAlignment = if (offsetPx < 0f) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = container,
            contentColor = content,
            tonalElevation = 2.dp,
            shadowElevation = if (committed) 6.dp else 0.dp,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .graphicsLayer {
                    val scale = (0.6f + 0.4f * intensity) * (if (committed) 1.08f else 1f)
                    scaleX = scale
                    scaleY = scale
                    alpha = intensity
                    // The pill leans toward the finger without chasing it.
                    translationX = (offsetPx * 0.18f).coerceIn(-72f, 72f)
                }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                if (label != null) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

/**
 * Playlist / album detail page.
 *
 * Layout follows the house grammar: one hero (the artwork), one primary action
 * (the play split button), page actions in the app bar overflow, and the edit
 * mode toggle in the track section header next to what it changes. Destructive
 * actions (delete playlist, remove track) run calm - no bounce, plain glyphs,
 * and an undo where the change is locally reversible.
 *
 * Reordering measures the real laid-out row geometry from the lazy list rather
 * than assuming a fixed row height, swaps at most once per frame from the
 * frame loop (so a burst of pointer events cannot double-swap against stale
 * layout), and auto-scrolls when the dragged row nears a viewport edge.
 */
@OptIn(
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class,
    ExperimentalFoundationApi::class
)
@Composable
fun PlaylistDetailScreen(
    playlist: PlaylistDisplayItem,
    onBack: () -> Unit,
    onPlayQueue: (List<Song>, Song?) -> Unit,
    /** Play a collection with shuffle mode on; see PlayerViewModel.playQueueShuffled. */
    onShuffleQueue: (List<Song>) -> Unit,
    viewModel: HomeViewModel,
    preloadedSongs: List<Song>? = null,
    isAlbum: Boolean = false,
    onSongLongPress: ((Song) -> Unit)? = null,
    /**
     * Queue a song without playing it. Hoisted like [onSongLongPress] because
     * the queue lives in the PlayerViewModel up in HomeScreen. Null leaves the
     * queue swipe action disarmed. The boolean is play-next: true inserts after
     * the current song, false appends at the end.
     */
    onEnqueueSong: ((Song, Boolean) -> Unit)? = null,
    /**
     * Open the "add from your playlists" flow for this playlist. Only offered
     * on local playlists, which are the ones whose track list this app owns.
     * Deliberately no default: a null default is the half-wired-feature trap,
     * so every caller states whether its surface can host the flow.
     */
    onAddSongsRequest: (() -> Unit)?,
    /**
     * Open the add-songs screen (recents, likes, a YouTube Music search) for
     * this playlist: the plus in the app bar. Offered wherever tracks can be
     * added, a device playlist or one on the account. No default, for the
     * same reason as [onAddSongsRequest].
     */
    onSearchSongsRequest: (() -> Unit)?
) {
    val userPlaylists by viewModel.userPlaylists.collectAsState()
    val localPlaylistIds by viewModel.localPlaylistIds.collectAsState()
    val savedPlaylistIds by viewModel.savedPlaylistIds.collectAsState()
    val resolvedPlaylist = remember(playlist, userPlaylists) {
        userPlaylists.firstOrNull { it.id == playlist.id } ?: playlist
    }
    val isLocalPlaylist = remember(playlist.id, localPlaylistIds, isAlbum) {
        !isAlbum && localPlaylistIds.contains(playlist.id)
    }
    val isSavedPlaylist = savedPlaylistIds.contains(playlist.id)
    // A video-mode device playlist opened from the music Library's "From video
    // mode" section: a device list with nothing on YouTube behind it, so it is
    // neither savable nor a YouTube playlist, and it is edited in video mode.
    val isVideoModePlaylist = com.sentup.android.data.LocalVideoPlaylistsRepository.isLocal(playlist.id)
    /**
     * Whether this page can be kept.
     *
     * Anything already in the library that is not a saved reference is the
     * user's own - local or on their account - and saving it would be a second
     * copy of something they already have. The synthesized feeds have no
     * playlist behind them to keep at all.
     */
    val canSavePlaylist = remember(playlist.id, userPlaylists, isLocalPlaylist, isSavedPlaylist) {
        !isLocalPlaylist && !isVideoModePlaylist &&
            playlist.id !in NON_SAVABLE_PLAYLIST_IDS &&
            (isSavedPlaylist || userPlaylists.none { it.id == playlist.id })
    }
    // YouTube playlists can be edited through InnerTube. "LM" (Your Likes) only
    // supports song removal (= removing the like), not rename/delete, and the
    // synthesized Supermix ("RTM") and radio mixes are not editable at all.
    val isYouTubePlaylist = !isAlbum && !isLocalPlaylist && !isVideoModePlaylist
    // Whether the library actually holds this playlist. A "PL" prefix says an
    // id is a real YouTube playlist, not that it is yours: every editing
    // affordance below is a write, and this page is reached from search and
    // from a shared link as well as from the Library, so without this a
    // playlist somebody else owns offered rename, delete and track editing -
    // all of which fail at the endpoint, after the UI has already moved. Being
    // absent from the library is only ever grounds for offering less, so the
    // moment before the list loads costs a hidden button, not a failed write.
    val isInLibrary = userPlaylists.any { it.id == playlist.id }
    val canEditYouTubeSongs = isYouTubePlaylist && !isSavedPlaylist &&
        (playlist.id == "LM" || (isInLibrary && playlist.id.startsWith("PL")))
    // A saved playlist starts with "PL" like any other, but it belongs to
    // whoever made it: rename and delete would be writes the account has no
    // rights to, and offering them is the UI promising something it cannot do.
    val canRenameDeleteYouTube = isYouTubePlaylist && isInLibrary &&
        playlist.id.startsWith("PL") && !isSavedPlaylist
    // Real "PL" playlists can be reordered remotely via edit_playlist moves;
    // "LM" (Your Likes) only supports removal.
    val canReorderRemote = isYouTubePlaylist && isInLibrary && playlist.id.startsWith("PL")
    val canEditSongs = isLocalPlaylist || canEditYouTubeSongs

    val trackKeyPrefix = "playlist_${resolvedPlaylist.id}"
    // Which row is playing. By song id: the page cannot tell which occurrence
    // the queue is on, so a song listed twice marks both.
    val nowPlaying = com.sentup.android.ui.components.LocalNowPlaying.current
    var songRows by remember(resolvedPlaylist.id) {
        val initialSongs = preloadedSongs
            ?.let { if (isAlbum) it.sortedInAlbumOrder() else it }
            .orEmpty()
        mutableStateOf(playlistSongRows(initialSongs, trackKeyPrefix))
    }
    val songs = remember(songRows) { songRows.map { it.song } }
    val isFetching = remember { mutableStateOf(songs.isEmpty()) }
    var loadAttempted by remember { mutableStateOf(preloadedSongs != null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var songPendingRemoval by remember { mutableStateOf<Song?>(null) }
    var showOverflow by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var isReorderMode by remember { mutableStateOf(false) }
    var descriptionExpanded by remember { mutableStateOf(false) }
    var reorderDirty by remember { mutableStateOf(false) }
    // Fetched lazily the first time edit mode opens. The ids are attached to
    // PlaylistSongRow occurrences rather than keyed by videoId, because a
    // playlist may legitimately contain the same video twice.
    var remoteRowIdsLoaded by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val haptics = com.sentup.android.util.rememberSentupHaptics()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val isYouTubeConnected by viewModel.isYouTubeConnected.collectAsState()
    var isUploading by remember(playlist.id) { mutableStateOf(false) }
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    val shareContext = LocalContext.current

    // Swipe actions, read live so a settings change reaches rows that are
    // already on screen - no DI here, one instance per screen.
    val swipePrefs = remember(shareContext) { ThemePreferences(shareContext) }
    val swipeEnabled by swipePrefs.playlistSwipeEnabled.collectAsState()
    val swipeStartSetting by swipePrefs.playlistSwipeStartAction.collectAsState()
    val swipeEndSetting by swipePrefs.playlistSwipeEndAction.collectAsState()

    // Search state
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val searchFocus = remember { FocusRequester() }

    // Cover art. Only a playlist the user made has artwork of its own to
    // replace - a YouTube playlist's cover belongs to whoever published it.
    val hasCustomCover = resolvedPlaylist.thumbnailUrl?.contains("/custom_") == true
    val coverPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        // Null is the user backing out of the picker, which is not a failure
        // and must not clear the cover they already have.
        if (uri != null) viewModel.setLocalPlaylistCover(resolvedPlaylist.id, uri)
    }
    val pickCover: () -> Unit = {
        coverPicker.launch(
            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
        )
    }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("audio/x-mpegurl")
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = viewModel.exportLocalPlaylist(songRows.map { it.song }, uri)
            snackbarHostState.showSnackbar(
                shareContext.getString(if (ok) R.string.pl_export_done else R.string.pl_export_failed)
            )
        }
    }

    // Drag state. The dragged row is addressed by its lazy list key so the
    // reorder maths can read real offsets and sizes out of the layout.
    var draggingKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var dragStartIndex by remember { mutableIntStateOf(-1) }

    val filteredRows = remember(songRows, searchQuery) {
        if (searchQuery.isBlank()) songRows
        else songRows.filter { row ->
            row.song.title.contains(searchQuery, ignoreCase = true) ||
                row.song.artist.contains(searchQuery, ignoreCase = true)
        }
    }
    val filteredSongs = remember(filteredRows) { filteredRows.map { it.song } }

    // How far the hero header has scrolled away, 0..1. Drives the app bar fade
    // and the artwork parallax continuously - a boolean threshold makes the bar
    // pop, which is exactly the snap the motion rules forbid.
    //
    // These stay as State objects and are only ever read inside graphicsLayer /
    // drawBehind lambdas. Reading them in the composable body would recompose
    // the whole page (LazyColumn content included) on every scroll frame; read
    // from a deferred lambda, a scroll only re-runs the layer or the draw.
    val headerCollapse = remember {
        derivedStateOf {
            val header = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.index == 0 }
            when {
                header == null -> 1f
                header.size <= 0 -> 0f
                else -> (-header.offset.toFloat() / header.size).coerceIn(0f, 1f)
            }
        }
    }
    val barAlpha = remember {
        derivedStateOf {
            if (isSearchActive) 1f else (headerCollapse.value / 0.55f).coerceIn(0f, 1f)
        }
    }

    LaunchedEffect(resolvedPlaylist.id) {
        if (preloadedSongs == null) {
            isFetching.value = true
            songRows = playlistSongRows(
                viewModel.fetchPlaylistSongs(resolvedPlaylist.id),
                trackKeyPrefix
            )
            isFetching.value = false
            loadAttempted = true
        }
    }

    LaunchedEffect(resolvedPlaylist.id, isLocalPlaylist, isFetching.value) {
        if (!isLocalPlaylist || isFetching.value) return@LaunchedEffect
        val found = viewModel.backfillLocalPlaylistDurations(resolvedPlaylist.id, songRows.map { it.song })
        if (found.isNotEmpty()) {
            songRows = songRows.map { row ->
                found[row.song.id]?.takeIf { row.song.duration <= 0 }
                    ?.let { row.copy(song = row.song.copy(duration = it)) } ?: row
            }
        }
    }

    LaunchedEffect(isReorderMode) {
        if (isReorderMode && canReorderRemote && !remoteRowIdsLoaded) {
            val setVideoIds = viewModel.fetchYouTubePlaylistSetVideoIds(resolvedPlaylist.id)
            songRows = attachPlaylistSetVideoIds(songRows, setVideoIds)
            // A failed/empty fetch may be retried the next time edit mode is
            // opened; a successful mapping is stable for this screen session.
            remoteRowIdsLoaded = setVideoIds.isNotEmpty()
        }
    }

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            withFrameNanos { }
            runCatching { searchFocus.requestFocus() }
        } else {
            focusManager.clearFocus()
        }
    }

    val commitLocalOrder: () -> Unit = {
        if (reorderDirty) {
            viewModel.replaceLocalPlaylistSongs(
                resolvedPlaylist.id,
                songRows.map { it.song }
            )
            reorderDirty = false
        }
    }

    val exitReorderMode: () -> Unit = {
        isReorderMode = false
        draggingKey = null
        dragOffsetY = 0f
        commitLocalOrder()
    }

    // Back leaves the transient modes before it leaves the page.
    BackHandler(enabled = isSearchActive || isReorderMode) {
        if (isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        } else {
            exitReorderMode()
        }
    }

    // Shared end-of-drag handler: local playlists persist the whole new order,
    // YouTube playlists send one edit_playlist move for the dragged row (and
    // resync from the server if the row is not addressable or the move fails).
    val finishReorderDrag: () -> Unit = finish@{
        val movedKey = draggingKey
        val startIndex = dragStartIndex
        draggingKey = null
        dragOffsetY = 0f
        dragStartIndex = -1
        if (movedKey != null) haptics.performHapticFeedback(HapticFeedbackType.Confirm)
        if (isLocalPlaylist) {
            commitLocalOrder()
            return@finish
        }
        if (movedKey == null || startIndex < 0) return@finish
        val newIndex = songRows.indexOfFirst { it.key == movedKey }
        if (newIndex < 0 || newIndex == startIndex) return@finish
        val movedSetId = songRows[newIndex].setVideoId
        val successor = songRows.getOrNull(newIndex + 1)
        val successorSetId = successor?.setVideoId
        if (movedSetId == null || (successor != null && successorSetId == null)) {
            // Row beyond the first browse page - no setVideoId to move it by,
            // so restore the server's order instead of guessing.
            scope.launch {
                songRows = playlistSongRows(
                    viewModel.fetchPlaylistSongs(resolvedPlaylist.id),
                    trackKeyPrefix
                )
                remoteRowIdsLoaded = false
            }
        } else {
            scope.launch {
                val ok = viewModel.moveSongInYouTubePlaylist(
                    resolvedPlaylist.id, movedSetId, successorSetId
                )
                if (!ok) {
                    songRows = playlistSongRows(
                        viewModel.fetchPlaylistSongs(resolvedPlaylist.id),
                        trackKeyPrefix
                    )
                    remoteRowIdsLoaded = false
                }
            }
        }
    }

    val beginDrag: (PlaylistSongRow) -> Unit = { row ->
        draggingKey = row.key
        dragOffsetY = 0f
        dragStartIndex = songRows.indexOfFirst { it.key == row.key }
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    // Swaps the dragged row with whichever row its centre currently covers.
    // Offsets come from the live layout, so rows of any height (edit mode adds
    // padding) stay glued to the finger. Called once per frame from the drag
    // loop below, never straight from a pointer event, because layoutInfo only
    // refreshes after relayout and two swaps against one snapshot would jump.
    val settleSwap: () -> Unit = settle@{
        val key = draggingKey ?: return@settle
        val info = listState.layoutInfo
        val current = info.visibleItemsInfo.firstOrNull { it.key == key } ?: return@settle
        val centre = current.offset + current.size / 2f + dragOffsetY
        val target = info.visibleItemsInfo.firstOrNull { candidate ->
            val candidateKey = candidate.key
            candidateKey is String &&
                candidateKey != key &&
                candidateKey.startsWith(trackKeyPrefix) &&
                centre >= candidate.offset &&
                centre <= candidate.offset + candidate.size
        } ?: return@settle
        val from = songRows.indexOfFirst { it.key == key }
        val to = songRows.indexOfFirst { it.key == target.key }
        if (from < 0 || to < 0 || from == to) return@settle
        songRows = songRows.toMutableList().apply { add(to, removeAt(from)) }
        // The row's settled slot just moved to the target's; cancel that out of
        // the drag offset so the row does not visibly jump under the finger.
        dragOffsetY -= (target.offset - current.offset).toFloat()
        if (isLocalPlaylist) reorderDirty = true
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
    }

    // Per-frame drag loop: auto-scroll near the viewport edges, then resolve a
    // swap. Running the swap here (not in onDrag) also means a finger parked at
    // the edge keeps reordering while the list scrolls under it.
    LaunchedEffect(draggingKey) {
        val key = draggingKey ?: return@LaunchedEffect
        val edge = with(density) { 96.dp.toPx() }
        val maxStep = with(density) { 14.dp.toPx() }
        while (isActive) {
            withFrameNanos { }
            val info = listState.layoutInfo
            val current = info.visibleItemsInfo.firstOrNull { it.key == key }
            if (current != null) {
                val top = current.offset + dragOffsetY
                val bottom = top + current.size
                val delta = when {
                    bottom > info.viewportEndOffset - edge ->
                        (bottom - (info.viewportEndOffset - edge)).coerceAtMost(maxStep)
                    top < info.viewportStartOffset + edge ->
                        (top - (info.viewportStartOffset + edge)).coerceAtLeast(-maxStep)
                    else -> 0f
                }
                if (delta != 0f) listState.scrollBy(delta)
            }
            settleSwap()
        }
    }

    val sortLocal: (PlaylistSortAction) -> Unit = { action ->
        val previous = songRows
        songRows = when (action) {
            PlaylistSortAction.Title -> songRows.sortedBy { it.song.title.lowercase() }
            PlaylistSortAction.Artist -> songRows.sortedWith(
                compareBy({ it.song.artist.lowercase() }, { it.song.title.lowercase() })
            )
            PlaylistSortAction.Album -> songRows.sortedWith(
                compareBy({ it.song.album.lowercase() }, { it.song.title.lowercase() })
            )
            PlaylistSortAction.Reverse -> songRows.reversed()
            PlaylistSortAction.Shuffle -> songRows.shuffled()
        }
        if (songRows != previous) {
            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
            viewModel.replaceLocalPlaylistSongs(resolvedPlaylist.id, songRows.map { it.song })
            reorderDirty = false
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = shareContext.getString(R.string.lib_sorted),
                    actionLabel = shareContext.getString(R.string.undo),
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    songRows = previous
                    viewModel.replaceLocalPlaylistSongs(resolvedPlaylist.id, previous.map { it.song })
                }
            }
        }
    }

    val removeSong: (PlaylistSongRow) -> Unit = { row ->
        val song = row.song
        val previous = songRows
        songRows = songRows.filterNot { it.key == row.key }
        if (isLocalPlaylist) {
            viewModel.replaceLocalPlaylistSongs(
                resolvedPlaylist.id,
                songRows.map { it.song }
            )
            reorderDirty = false
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "Removed ${song.title}",
                    actionLabel = shareContext.getString(R.string.undo),
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    songRows = previous
                    viewModel.replaceLocalPlaylistSongs(
                        resolvedPlaylist.id,
                        previous.map { it.song }
                    )
                }
            }
        } else {
            // Remote removal is not locally reversible (a re-add would land at
            // the end of the playlist) and it is a write against the signed-in
            // YouTube account - for "LM" it removes the like outright - so it
            // is confirmed rather than offered with an undo.
            songPendingRemoval = song
        }
    }

    // A swipe direction names its action in settings. Remove only arms where
    // this page can actually remove, queue only where someone above handed a
    // queue over, options only where a sheet exists to open. A YouTube list
    // that is not yours cannot be edited from here at all, so only the two
    // actions that leave the playlist alone arm there: play it, or queue
    // from it.
    val queueOnly = isYouTubePlaylist && !canEditSongs
    val resolveSwipeSetting: (String) -> String? = { setting ->
        when {
            setting == ThemePreferences.PLAYLIST_SWIPE_ACTION_OFF -> null
            setting == ThemePreferences.PLAYLIST_SWIPE_ACTION_REMOVE && !canEditSongs -> null
            setting == ThemePreferences.PLAYLIST_SWIPE_ACTION_QUEUE && onEnqueueSong == null -> null
            setting == ThemePreferences.PLAYLIST_SWIPE_ACTION_OPTIONS &&
                (queueOnly || onSongLongPress == null) -> null
            setting != ThemePreferences.PLAYLIST_SWIPE_ACTION_PLAY &&
                setting != ThemePreferences.PLAYLIST_SWIPE_ACTION_QUEUE &&
                setting != ThemePreferences.PLAYLIST_SWIPE_ACTION_REMOVE &&
                setting != ThemePreferences.PLAYLIST_SWIPE_ACTION_OPTIONS -> null
            else -> setting
        }
    }

    // The page dresses in the cover. Palette accents extracted from the
    // artwork replace the theme's accent roles while surfaces stay on the app
    // theme (the player's own rememberArtworkColorScheme, reused), so Play,
    // Save, the FAB and the wash behind the list all match the art. The base
    // theme holds while extraction runs, when the art has no usable swatches,
    // and when the user has turned artwork colors off - someone who switched
    // that off did so because they want the app's own palette, and a page
    // that tints itself from the cover anyway is the switch not working.
    val heroArt = resolvedPlaylist.thumbnailUrl?.takeIf { it.isNotBlank() && it != "null" }
    val artworkColorsEnabled by swipePrefs.playerArtworkColors.collectAsState()
    val artworkScheme = rememberArtworkColorScheme(
        enabled = artworkColorsEnabled,
        albumArtUri = heroArt,
        base = MaterialTheme.colorScheme
    )
    MaterialTheme(colorScheme = artworkScheme) {
    Scaffold(
        topBar = {
            // The bar settles into the page's ground rather than onto it: an
            // untinted `surface` slab fading in over a cover-colored page
            // reads as a grey card sliding over it. It is the ground exactly,
            // so what the bar separates is the scrolling rows passing under
            // it, not itself from the page.
            val barColor = playlistPageGround()
            Box(modifier = Modifier.drawBehind { drawRect(barColor, alpha = barAlpha.value) }) {
                Column {
                    TopAppBar(
                        title = {
                            Text(
                                resolvedPlaylist.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.graphicsLayer {
                                    alpha = barAlpha.value
                                    translationY = (1f - barAlpha.value) * 20.dp.toPx()
                                }
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                commitLocalOrder()
                                onBack()
                            }) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                            }
                        },
                        actions = {
                            // Your Likes takes removals only: a song joins it
                            // by being liked, not by being added.
                            if (onSearchSongsRequest != null && !isSearchActive &&
                                (isLocalPlaylist || canReorderRemote)
                            ) {
                                IconButton(onClick = {
                                    if (isReorderMode) exitReorderMode()
                                    onSearchSongsRequest()
                                }) {
                                    Icon(
                                        Icons.Rounded.Add,
                                        contentDescription = stringResource(R.string.import_title)
                                    )
                                }
                            }
                            IconButton(onClick = {
                                if (isReorderMode) exitReorderMode()
                                isSearchActive = !isSearchActive
                                if (!isSearchActive) searchQuery = ""
                            }) {
                                Icon(
                                    imageVector = if (isSearchActive) Icons.Rounded.Close else Icons.Rounded.Search,
                                    contentDescription = if (isSearchActive) stringResource(R.string.cd_clear_search) else stringResource(R.string.lib_search_in_playlist)
                                )
                            }
                            // Page-level actions live behind one overflow so the
                            // bar keeps to the 1-2 visible actions M3E asks for,
                            // and Delete is not a peer of Rename on the page.
                            val hasRename = isLocalPlaylist || canRenameDeleteYouTube
                            val hasShare = !isLocalPlaylist && !isVideoModePlaylist &&
                                resolvedPlaylist.id !in NON_SHAREABLE_PLAYLIST_IDS
                            // Anything that is not already the user's own local
                            // playlist can be copied into one, once its tracks
                            // are on screen - an album and a generated mix
                            // included, since a snapshot of either is a
                            // perfectly good starting point for a playlist of
                            // your own. There is nothing to copy before the
                            // list loads, so the row waits for it rather than
                            // creating an empty playlist.
                            val canCopyToLocal = !isLocalPlaylist && songs.isNotEmpty()
                            val radioSeed = songs.firstOrNull { it.source == SongSource.YOUTUBE }
                            if (hasRename || hasShare || canCopyToLocal || radioSeed != null) {
                                Box {
                                    IconButton(onClick = { showOverflow = true }) {
                                        Icon(Icons.Rounded.MoreVert, "More options")
                                    }
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false }
                                    ) {
                                        PlaylistSortAction.entries.forEach { action ->
                                            DropdownMenuItem(
                                                text = { Text(stringResource(action.label)) },
                                                onClick = {
                                                    showSortMenu = false
                                                    sortLocal(action)
                                                }
                                            )
                                        }
                                    }
                                    DropdownMenu(
                                        expanded = showOverflow,
                                        onDismissRequest = { showOverflow = false }
                                    ) {
                                        if (radioSeed != null) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.ar_start_radio)) },
                                                leadingIcon = { Icon(Icons.Rounded.Radio, null) },
                                                onClick = {
                                                    showOverflow = false
                                                    scope.launch {
                                                        val radio = viewModel.getRadioSongs(radioSeed.id)
                                                        if (radio.isNotEmpty()) {
                                                            onPlayQueue(listOf(radioSeed) + radio, radioSeed)
                                                        }
                                                    }
                                                },
                                            )
                                        }
                                        if (hasRename) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.lib_edit_details)) },
                                                leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                                                onClick = {
                                                    showOverflow = false
                                                    showEditDialog = true
                                                }
                                            )
                                        }
                                        if (isLocalPlaylist && songs.size > 1) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.lib_sort_playlist)) },
                                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.Sort, null) },
                                                onClick = {
                                                    showOverflow = false
                                                    showSortMenu = true
                                                }
                                            )
                                        }
                                        if (isLocalPlaylist && songs.isNotEmpty()) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.pl_export)) },
                                                leadingIcon = { Icon(Icons.Rounded.FileUpload, null) },
                                                onClick = {
                                                    showOverflow = false
                                                    val base = (resolvedPlaylist.name ?: "playlist")
                                                        .replace(Regex("[\\\\/:*?\"<>|]"), "_")
                                                    exportLauncher.launch("$base.m3u8")
                                                }
                                            )
                                        }
                                        if (isLocalPlaylist && onAddSongsRequest != null) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.lib_add_from_playlists)) },
                                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null) },
                                                onClick = {
                                                    showOverflow = false
                                                    onAddSongsRequest()
                                                }
                                            )
                                        }
                                        if (isLocalPlaylist) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.lib_change_cover)) },
                                                leadingIcon = { Icon(Icons.Rounded.PhotoCamera, null) },
                                                onClick = {
                                                    showOverflow = false
                                                    pickCover()
                                                }
                                            )
                                            // Only offered once there is
                                            // something to undo: on a generated
                                            // cover it would do nothing visible.
                                            if (hasCustomCover) {
                                                DropdownMenuItem(
                                                    text = { Text(stringResource(R.string.lib_reset_cover)) },
                                                    leadingIcon = { Icon(Icons.Rounded.Refresh, null) },
                                                    onClick = {
                                                        showOverflow = false
                                                        viewModel.resetLocalPlaylistCover(resolvedPlaylist.id)
                                                    }
                                                )
                                            }
                                        }
                                        if (isLocalPlaylist && isYouTubeConnected && !isUploading &&
                                            songs.any { it.source == com.sentup.android.data.SongSource.YOUTUBE }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.lib_upload_ytm)) },
                                                leadingIcon = { Icon(Icons.Rounded.CloudUpload, null) },
                                                onClick = {
                                                    showOverflow = false
                                                    isUploading = true
                                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                                    scope.launch {
                                                        snackbarHostState.showSnackbar(
                                                            shareContext.getString(R.string.lib_upload_ytm_started)
                                                        )
                                                    }
                                                    // Uploaded in the ViewModel's scope, so leaving
                                                    // the page does not abandon it halfway; only
                                                    // the result message waits on this screen.
                                                    val upload = viewModel.viewModelScope.async {
                                                        viewModel.uploadLocalPlaylistToYouTube(
                                                            name = resolvedPlaylist.name ?: "",
                                                            description = resolvedPlaylist.description,
                                                            songs = songs,
                                                        )
                                                    }
                                                    scope.launch {
                                                        val (result, skipped) = upload.await()
                                                        isUploading = false
                                                        val message = when {
                                                            result == null -> shareContext.getString(R.string.lib_upload_ytm_failed)
                                                            result.uploaded < result.total -> shareContext.getString(
                                                                R.string.lib_upload_ytm_partial, result.uploaded, result.total
                                                            )
                                                            skipped > 0 -> shareContext.resources.getQuantityString(
                                                                R.plurals.lib_upload_ytm_done_skipped, skipped, skipped
                                                            )
                                                            else -> shareContext.getString(R.string.lib_upload_ytm_done)
                                                        }
                                                        snackbarHostState.showSnackbar(message)
                                                    }
                                                }
                                            )
                                        }
                                        if (canCopyToLocal) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.lib_copy_editable)) },
                                                leadingIcon = { Icon(Icons.Rounded.ContentCopy, null) },
                                                onClick = {
                                                    showOverflow = false
                                                    haptics.performHapticFeedback(
                                                        HapticFeedbackType.Confirm
                                                    )
                                                    scope.launch {
                                                        val copied = viewModel.copyPlaylistToLocal(
                                                            name = resolvedPlaylist.name,
                                                            description = resolvedPlaylist.description,
                                                            songs = songs
                                                        )
                                                        if (copied != null) {
                                                            snackbarHostState.showSnackbar(
                                                                shareContext.getString(
                                                                    R.string.lib_copy_editable_done
                                                                )
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        if (hasShare) {
                                            DropdownMenuItem(
                                                text = { Text(stringResource(R.string.video_options_share)) },
                                                leadingIcon = { Icon(Icons.Rounded.Share, null) },
                                                onClick = {
                                                    showOverflow = false
                                                    val shareUrl = if (isAlbum) {
                                                        "https://music.youtube.com/browse/${resolvedPlaylist.id}"
                                                    } else {
                                                        "https://music.youtube.com/playlist?list=${resolvedPlaylist.id}"
                                                    }
                                                    val send = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                                        type = "text/plain"
                                                        putExtra(android.content.Intent.EXTRA_TEXT, shareUrl)
                                                    }
                                                    shareContext.startActivity(
                                                        android.content.Intent.createChooser(send, shareContext.getString(R.string.share_playlist_chooser))
                                                    )
                                                }
                                            )
                                        }
                                        if (hasRename) {
                                            HorizontalDivider()
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        stringResource(R.string.delete_playlist),
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        Icons.Rounded.Delete,
                                                        null,
                                                        tint = MaterialTheme.colorScheme.error
                                                    )
                                                },
                                                onClick = {
                                                    showOverflow = false
                                                    showDeleteDialog = true
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                            scrolledContainerColor = Color.Transparent
                        )
                    )
                    // Pinned rather than sticky in the list: the field is always
                    // reachable while searching, and it composes immediately so
                    // it can take focus without waiting for a scroll.
                    // Default (non-bouncy) size springs on purpose: an
                    // Expressive spatial spec bounces, and a bouncing height
                    // undershoots negative. Bounce belongs on scale and
                    // translation, not on anything that measures.
                    AnimatedVisibility(
                        visible = isSearchActive,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Find in ${if (isAlbum) "album" else "playlist"}") },
                            leadingIcon = { Icon(Icons.Rounded.Search, null) },
                            trailingIcon = if (searchQuery.isNotEmpty()) {
                                {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Rounded.Close, "Clear search")
                                    }
                                }
                            } else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(bottom = 12.dp)
                                .focusRequester(searchFocus),
                            shape = RoundedCornerShape(28.dp),
                            colors = TextFieldDefaults.colors(
                                // Stands on the ground the same way the rows
                                // it filters do.
                                focusedContainerColor = playlistRaisedSurface(),
                                unfocusedContainerColor = playlistRaisedSurface(),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                    }
                }
            }
        },
        snackbarHost = {
            DismissibleSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(
                    bottom = com.sentup.android.ui.components.LocalBottomOverlayInset.current
                )
            )
        },
        floatingActionButton = {
            if (isReorderMode) {
                // Vibrant floating toolbar = the M3 Expressive signal for a
                // temporary edit mode; stays reachable however far the list scrolls
                val canReorder = isLocalPlaylist ||
                    (canReorderRemote && songRows.any { it.setVideoId != null })
                ManageModeToolbar(
                    hint = if (canReorder) stringResource(R.string.lib_drag_hint) else stringResource(R.string.lib_tap_remove_hint),
                    icon = if (canReorder) Icons.Rounded.DragIndicator else Icons.Rounded.RemoveCircleOutline,
                    onDone = exitReorderMode,
                    modifier = Modifier.padding(
                        bottom = com.sentup.android.ui.components.LocalBottomOverlayInset.current
                    )
                )
            }
        }
    ) { _ ->
        // The scaffold's inset is deliberately dropped: the cover draws under
        // the transparent top bar and the status bar, which is the whole point
        // of the hero, and the list carries its own bottom padding for the
        // navigation bar and the floating overlays.
        // The cover's color is the page, not a flourish at the top of it: one
        // flat ground the artwork blends into and the page then carries to the
        // bottom, however long the track list is. See [playlistPageGround] for
        // why this is not a gradient.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(playlistPageGround())
        ) {
        // Enough scroll clearance for the floating overlays plus the FAB
        val listBottomPadding = com.sentup.android.ui.components.LocalBottomOverlayInset.current +
            WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 88.dp
        // The page's items, as one block both layouts draw from: a phone gets
        // header and tracks in one list, a wide pane gets the header in a
        // column of its own beside the tracks (see the layout at the end).
        val pageItems: androidx.compose.foundation.lazy.LazyListScope.(
            showHeader: Boolean,
            showTracks: Boolean
        ) -> Unit = { showHeader, showTracks ->
            // Hero: the cover fills the top of the page edge to edge under
            // the status bar, then dissolves into the page's own tint, with
            // the title, the facts and the controls stacked on that tint
            // below it - the artist page's immersive top, for collections.
            //
            // The title sits *below* the cover rather than on it. On the
            // artist page a name can be set over a photo because a portrait
            // has a predictable shape; a playlist cover is any image at all,
            // including a white one, and no scrim tuning makes type over it
            // reliable across every cover. Below it, the title is drawn on
            // the page's own tint, so it reads in both themes on all of the
            // app's palettes.
            if (showHeader) item {
                // The status-bar scrim is drawn in the page's ground rather
                // than in the bare background, so the band the app bar fades
                // in over is already the page's own color.
                val ground = playlistPageGround()
                val windowHeight = com.sentup.android.ui.theme.windowDpSize().height
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Beside the tracks the cover is an object in a column,
                    // not a banner across the page: square, rounded, inset.
                    if (!showTracks) PlaylistSideCover(
                        heroArt = heroArt,
                        isAlbum = isAlbum,
                        onPickCover = if (isLocalPlaylist) pickCover else null
                    ) else BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        // Near-square on a phone, but never more than a
                        // comfortable share of the window: the title, the
                        // controls and the first track all have to fit under
                        // it, in landscape as much as in portrait.
                        val heroHeight = (maxWidth * 0.94f)
                            .coerceAtMost(windowHeight * 0.44f)
                            .coerceAtLeast(220.dp)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(heroHeight)
                                // The cover drifts under the scroll; clipped
                                // here so it never draws over the list.
                                .clipToBounds()
                        ) {
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .graphicsLayer {
                                        // Scroll-driven, not time-driven: the
                                        // cover trails the list rather than
                                        // animating on its own, so there is
                                        // nothing here to reduce.
                                        val collapse = headerCollapse.value
                                        translationY = collapse * size.height * 0.24f
                                        alpha = 1f - collapse * 0.55f
                                    }
                                    .then(
                                        // The cover is the affordance: a whole
                                        // banner of target, exactly where
                                        // someone looking to change the
                                        // picture looks.
                                        if (isLocalPlaylist) {
                                            Modifier.clickable(onClick = pickCover)
                                        } else Modifier
                                    )
                            ) {
                                if (heroArt != null) {
                                    // Asked for at the size it is drawn and
                                    // layered over the feed-size original, so
                                    // the banner is sharp rather than a 120px
                                    // playlist thumbnail stretched over it.
                                    VideoThumbnail(
                                        thumbnailUrl = heroArt,
                                        highResThumbnailUrl = googleImageAtSize(heroArt, HERO_COVER_PX)
                                            ?.takeIf { it != heroArt },
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        showProgress = false,
                                        placeholderColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                    )
                                } else {
                                    // No cover anywhere: the theme's own
                                    // containers with the collection's glyph
                                    // cut into an Expressive shape, so the
                                    // page still has a top.
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(
                                                Brush.linearGradient(
                                                    listOf(
                                                        MaterialTheme.colorScheme.primaryContainer,
                                                        MaterialTheme.colorScheme.tertiaryContainer
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Surface(
                                            shape = MaterialShapes.Cookie9Sided.toShape(),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f),
                                            modifier = Modifier.size(150.dp)
                                        ) {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = if (isAlbum) Icons.Rounded.Album else Icons.AutoMirrored.Rounded.PlaylistPlay,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(64.dp),
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            // Where the cover hands the page over: a plain
                            // fade, in the page's exact ground color.
                            //
                            // This was a `DstIn` mask that erased the cover's
                            // foot instead. [scar] On screen it composited to
                            // *black* rather than to transparency and ended
                            // the cover on a hard dark edge - the layer was
                            // not isolating the way `CompositingStrategy`
                            // promises. Painting the fade is only correct
                            // because the ground is one flat color: there is
                            // exactly one value to match, so a plain SrcOver
                            // gradient lands on it precisely, with no blend
                            // mode and no offscreen buffer to go wrong.
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .background(
                                        Brush.verticalGradient(
                                            0f to ground.copy(alpha = 0f),
                                            0.58f to ground.copy(alpha = 0f),
                                            0.86f to ground.copy(alpha = 0.85f),
                                            1f to ground
                                        )
                                    )
                            )

                            // Top fade: back, search and the overflow need
                            // ground to sit on whatever the cover is. Faded to
                            // the same color at zero alpha rather than to
                            // Transparent, which is transparent *black* and
                            // greys the band in a light theme.
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(132.dp)
                                    .align(Alignment.TopCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                ground.copy(alpha = 0.55f),
                                                ground.copy(alpha = 0f)
                                            )
                                        )
                                    )
                            )

                            // Badge on the cover's corner, so the artwork
                            // reads as editable without a caption telling
                            // people to tap it.
                            if (isLocalPlaylist) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    // It sits on a layer that moves with every
                                    // scroll frame, and a shadow under one of
                                    // those shimmers instead of grounding it.
                                    shadowElevation = 0.dp,
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(end = 20.dp, bottom = 12.dp)
                                        .clickable(onClick = pickCover)
                                ) {
                                    Icon(
                                        Icons.Rounded.PhotoCamera,
                                        contentDescription = stringResource(R.string.lib_change_cover_art),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier
                                            .padding(10.dp)
                                            .size(20.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Title and facts, set the way the artist page sets a
                    // name: poster face, start-aligned, sized down rather
                    // than cut when the name is a long one.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PLAYLIST_GUTTER)
                            .padding(top = 14.dp)
                    ) {
                        Text(
                            text = listNameLabel(resolvedPlaylist.name),
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontFamily = MontserratFamily,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.02).em,
                                lineHeight = 1.08.em
                            ),
                            color = playlistOnGround(),
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                            // Big for "Focus", smaller rather than cut for
                            // "Songs to sing in the shower at 3am".
                            autoSize = TextAutoSize.StepBased(
                                minFontSize = 24.sp,
                                maxFontSize = 38.sp,
                                stepSize = 2.sp
                            )
                        )

                        val totalDurationLabel = remember(songs) {
                            formatTotalDuration(songs.sumOf { it.duration })
                        }
                        val releaseType = songs.firstOrNull()?.releaseType ?: resolvedPlaylist.releaseType
                        val releaseYear = songs.firstOrNull()?.releaseYear ?: resolvedPlaylist.releaseYear
                        val kindLabel = stringResource(releaseType?.labelRes ?: R.string.label_album)
                        Text(
                            text = listOfNotNull(
                                if (isAlbum) kindLabel else stringResource(R.string.label_playlist),
                                resolvedPlaylist.uploaderName.takeIf { isAlbum && it.isNotBlank() },
                                releaseYear?.toString()?.takeIf { isAlbum },
                                pluralStringResource(R.plurals.n_tracks, songs.size, songs.size),
                                totalDurationLabel
                            ).joinToString("  ·  "),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium,
                            color = playlistOnGround().copy(alpha = 0.8f),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    // Playback is the page's one primary action, directly
                    // under the facts it acts on. Everything else is a
                    // quieter row beneath it.
                    if (songs.isNotEmpty() && !isReorderMode && !isSearchActive) {
                        CollectionPlaybackActions(
                            onPlay = { onPlayQueue(songs, songs.first()) },
                            onShuffle = { onShuffleQueue(songs) },
                            modifier = Modifier
                                .padding(horizontal = PLAYLIST_GUTTER)
                                .padding(top = 18.dp),
                        )
                    }

                    androidx.compose.foundation.layout.FlowRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = PLAYLIST_GUTTER)
                            .padding(top = 10.dp),
                        // Centred rather than on the title's rail: these wrap
                        // to their own line under two full-width controls, and
                        // a lone pill hanging off the left edge under them
                        // reads as a stray rather than as the row's second
                        // rank. Centring also keeps it looking deliberate
                        // whether there are one, two or three of them.
                        horizontalArrangement = Arrangement.spacedBy(
                            8.dp,
                            Alignment.CenterHorizontally
                        ),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        // Keeping a playlist is the whole point of arriving
                        // here from search, so it sits in the header rather
                        // than behind the overflow. Play is still the one
                        // primary action, above this secondary group.
                        if (canSavePlaylist) {
                            FilledTonalButton(
                                onClick = {
                                    val nowSaved = viewModel.toggleSavedPlaylist(
                                        resolvedPlaylist,
                                        isAlbum = isAlbum
                                    )
                                    haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                                    scope.launch {
                                        val snackbarMsg =
                                            if (nowSaved) shareContext.getString(R.string.lib_saved_to_library)
                                            else shareContext.getString(R.string.lib_removed_from_library)
                                        snackbarHostState.showSnackbar(snackbarMsg)
                                    }
                                },
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                // Crossfade rather than a spatial spec: the
                                // button must not resize under the finger that
                                // is still on it.
                                AnimatedContent(
                                    targetState = isSavedPlaylist,
                                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                                    label = "savePlaylist"
                                ) { saved ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (saved) Icons.Rounded.BookmarkAdded
                                                else Icons.Rounded.BookmarkAdd,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(if (saved) stringResource(R.string.cd_saved) else stringResource(R.string.action_save))
                                    }
                                }
                            }
                        }

                        // Saving keeps a live reference; downloading makes the
                        // loaded snapshot available offline. They stay separate
                        // actions because either can be useful without the other.
                        if (songs.isNotEmpty()) {
                            MusicPlaylistDownloadAction(
                                playlistId = resolvedPlaylist.id,
                                playlistTitle = resolvedPlaylist.name,
                                artworkUrl = resolvedPlaylist.thumbnailUrl,
                                songs = songs,
                                modifier = Modifier.heightIn(min = 48.dp)
                            )
                        }
                    }

                    // Tap to expand: a published playlist's description can
                    // run to paragraphs, and two lines is all the page owes
                    // it before the tracks.
                    if (!resolvedPlaylist.description.isNullOrBlank()) {
                        Text(
                            text = resolvedPlaylist.description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = playlistOnGround().copy(alpha = 0.78f),
                            maxLines = if (descriptionExpanded) Int.MAX_VALUE else 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = PLAYLIST_GUTTER)
                                .padding(top = 14.dp)
                                .animateContentSize()
                                .clickable { descriptionExpanded = !descriptionExpanded }
                        )
                    }

                    Spacer(Modifier.height(8.dp))
                }
            }

            if (showTracks) {
            // Track section header. Hidden while searching - the search field
            // already labels what the list below it is.
            if (!isSearchActive && !isFetching.value && songs.isNotEmpty()) {
                item {
                    TrackSectionHeader(
                        countLabel = pluralStringResource(R.plurals.n_tracks, songs.size, songs.size),
                        manageEnabled = isReorderMode,
                        canEdit = canEditSongs,
                        onToggleEdit = {
                            if (isReorderMode) {
                                exitReorderMode()
                            } else {
                                isReorderMode = true
                                isSearchActive = false
                                searchQuery = ""
                            }
                        }
                    )
                }
            }

            if (isSearchActive && searchQuery.isNotBlank() && filteredSongs.isNotEmpty()) {
                item {
                    Text(
                        text = pluralStringResource(R.plurals.n_results, filteredSongs.size, filteredSongs.size),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 24.dp, top = 8.dp, bottom = 4.dp)
                    )
                }
            }

            when {
                isFetching.value -> item { TrackSkeletonList() }

                filteredSongs.isEmpty() && searchQuery.isNotBlank() -> item {
                    EmptyLibraryState(
                        icon = Icons.Rounded.SearchOff,
                        title = stringResource(R.string.search_no_results),
                        subtitle = stringResource(
                            if (isAlbum) R.string.lib_empty_search_album
                            else R.string.lib_empty_search_playlist,
                            searchQuery
                        )
                    )
                }

                songs.isEmpty() && loadAttempted && !isLocalPlaylist && !isAlbum -> item {
                    // A remote playlist that came back empty is more often a
                    // failed fetch than a genuinely empty playlist, so this
                    // state offers the recovery instead of dead-ending.
                    EmptyLibraryState(
                        icon = Icons.Rounded.CloudOff,
                        title = stringResource(R.string.lib_load_failed_title),
                        subtitle = stringResource(R.string.lib_load_failed_body),
                        action = {
                            Button(onClick = {
                                scope.launch {
                                    isFetching.value = true
                                    songRows = playlistSongRows(
                                        viewModel.fetchPlaylistSongs(resolvedPlaylist.id),
                                        trackKeyPrefix
                                    )
                                    remoteRowIdsLoaded = false
                                    isFetching.value = false
                                }
                            }) {
                                Icon(Icons.Rounded.Refresh, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.action_retry))
                            }
                        }
                    )
                }

                songs.isEmpty() -> item {
                    // A fresh local playlist's empty state offers the way to
                    // fill it rather than only reporting that it is empty.
                    EmptyLibraryState(
                        icon = Icons.Rounded.MusicNote,
                        title = stringResource(if (isAlbum) R.string.lib_album_empty else R.string.lib_playlist_empty),
                        subtitle = stringResource(
                            if (isAlbum) R.string.lib_empty_album_hint
                            else R.string.lib_empty_playlist_hint
                        ),
                        action = if (isLocalPlaylist && onAddSongsRequest != null) {
                            {
                                Button(onClick = onAddSongsRequest) {
                                    Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, null, Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(stringResource(R.string.lib_add_from_playlists))
                                }
                            }
                        } else null
                    )
                }

                else -> itemsIndexed(
                    items = filteredRows,
                    key = { _, row -> row.key }
                ) { index, row ->
                    val song = row.song
                    // Manage mode: song removal for local and YouTube playlists.
                    // Drag reordering works locally and on owned "PL" playlists
                    // (remote rows become draggable once their setVideoId loads).
                    val manageEnabled = isReorderMode && searchQuery.isBlank() && canEditSongs
                    val reorderEnabled = manageEnabled &&
                        (isLocalPlaylist || (canReorderRemote && row.setVideoId != null))
                    val isDragging = draggingKey == row.key

                    // The handle drags on touch (the discoverable path); long
                    // pressing anywhere on the row is the accelerator for the
                    // same reorder. Both feed the one drag state.
                    val handleDrag = if (reorderEnabled) {
                        Modifier.pointerInput(row.key) {
                            detectDragGestures(
                                onDragStart = { beginDrag(row) },
                                onDragEnd = { finishReorderDrag() },
                                onDragCancel = { finishReorderDrag() },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffsetY += amount.y
                                }
                            )
                        }
                    } else Modifier
                    val rowDrag = if (reorderEnabled) {
                        Modifier.pointerInput(row.key) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = { beginDrag(row) },
                                onDragEnd = { finishReorderDrag() },
                                onDragCancel = { finishReorderDrag() },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffsetY += amount.y
                                }
                            )
                        }
                    } else Modifier

                    // Connected track group: rows sit flush inside one visual
                    // container - round outer corners, small seams, a divider
                    // between rows - instead of floating as separate cards.
                    // The dividers live outside the swipe container so a swipe
                    // moves only its own row, and indices are untouched because
                    // no extra lazy items are added (reorder math relies on that).
                    val isFirstTrack = index == 0
                    val isLastTrack = index == filteredRows.lastIndex
                    // Segmented list geometry (16dp outer, 4dp inner, 2dp gaps),
                    // the same tokens as every other grouped list in the app.
                    val trackTopCorner = if (isFirstTrack) 16.dp else 4.dp
                    val trackBottomCorner = if (isLastTrack) 16.dp else 4.dp
                    val trackShape = RoundedCornerShape(
                        topStart = trackTopCorner,
                        topEnd = trackTopCorner,
                        bottomStart = trackBottomCorner,
                        bottomEnd = trackBottomCorner
                    )

                    // The swipe disarms while the row is managed or dragged:
                    // both gestures start from the same finger, and a reorder
                    // that ends in an accidental removal is the worst outcome
                    // either of them can produce.
                    val swipeArmed = swipeEnabled && !manageEnabled && !isDragging
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .then(if (isDragging) Modifier else Modifier.animateItem())
                            // The group's 20dp inset and its breathing room
                            // live here, on the item, so the swipe wash behind
                            // each row shares the row's exact bounds instead
                            // of flooding past the card.
                            .padding(
                                start = 20.dp,
                                end = 20.dp,
                                top = if (isFirstTrack) 12.dp else 0.dp,
                                bottom = if (isLastTrack) 16.dp else ListItemDefaults.SegmentedGap
                            )
                    ) {
                    PlaylistTrackSwipeContainer(
                        startAction = if (swipeArmed) resolveSwipeSetting(swipeStartSetting) else null,
                        endAction = if (swipeArmed) resolveSwipeSetting(swipeEndSetting) else null,
                        onAction = { action ->
                            haptics.performHapticFeedback(HapticFeedbackType.Confirm)
                            when (action) {
                                ThemePreferences.PLAYLIST_SWIPE_ACTION_REMOVE -> removeSong(row)
                                ThemePreferences.PLAYLIST_SWIPE_ACTION_PLAY -> onPlayQueue(filteredSongs, song)
                                ThemePreferences.PLAYLIST_SWIPE_ACTION_QUEUE -> onEnqueueSong?.invoke(song, false)
                                ThemePreferences.PLAYLIST_SWIPE_ACTION_OPTIONS -> onSongLongPress?.invoke(song)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            // The swipe wash stays inside the group's rounded
                            // corners. Skipped while dragging so it never clips
                            // the lifted row's shadow.
                            .then(if (isDragging) Modifier else Modifier.clip(trackShape))
                            .zIndex(if (isDragging) 2f else 0f)
                            .offset { IntOffset(0, if (isDragging) dragOffsetY.roundToInt() else 0) }
                            .then(rowDrag)
                    ) {
                        PlaylistTrackRow(
                            song = song,
                            isCurrent = nowPlaying.songId == song.id,
                            isPlaying = nowPlaying.isPlaying,
                            manageEnabled = manageEnabled,
                            reorderEnabled = reorderEnabled,
                            isDragging = isDragging,
                            dragHandleModifier = handleDrag,
                            onClick = { onPlayQueue(filteredSongs, song) },
                            onLongClick = onSongLongPress?.let { press -> { press(song) } },
                            onRemove = { removeSong(row) },
                            topCorner = trackTopCorner,
                            bottomCorner = trackBottomCorner,
                            // No padding of its own: the inset lives on the
                            // item above so the swipe wash matches these bounds.
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    }
                }
            }
            } // showTracks
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wide = maxWidth >= PLAYLIST_WIDE_MIN_WIDTH
            if (wide) {
                // Under the transparent top bar, which the tracks scroll
                // beneath and which fades in over them as they do.
                val topClearance = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 64.dp
                val sideListState = androidx.compose.foundation.lazy.rememberLazyListState()
                val headerColumnWidth = (maxWidth * 0.4f).coerceIn(320.dp, 460.dp)
                Row(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = sideListState,
                        contentPadding = PaddingValues(top = topClearance, bottom = listBottomPadding),
                        modifier = Modifier
                            .width(headerColumnWidth)
                            .fillMaxHeight()
                    ) { pageItems(true, false) }
                    LazyColumn(
                        state = listState,
                        contentPadding = PaddingValues(top = topClearance, bottom = listBottomPadding),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    ) { pageItems(false, true) }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(bottom = listBottomPadding),
                    modifier = Modifier.fillMaxSize()
                ) { pageItems(true, true) }
            }
        }
        }
    }
    } // MaterialTheme(artworkScheme)

    if (showEditDialog && (isLocalPlaylist || canRenameDeleteYouTube)) {
        EditPlaylistDialog(
            initialName = resolvedPlaylist.name,
            initialDescription = resolvedPlaylist.description,
            onDismiss = { showEditDialog = false },
            onConfirm = { newName, newDescription ->
                if (isLocalPlaylist) {
                    viewModel.updateLocalPlaylist(resolvedPlaylist.id, newName, newDescription)
                } else {
                    viewModel.renameYouTubePlaylist(resolvedPlaylist.id, newName, newDescription)
                }
                showEditDialog = false
            }
        )
    }

    if (showDeleteDialog && (isLocalPlaylist || canRenameDeleteYouTube)) {
        // Destructive confirmation runs calm: no bounce, no scale-in entrance.
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            icon = {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Rounded.Delete,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
            },
            title = { Text(stringResource(R.string.delete_playlist_q)) },
            text = {
                Text(
                    // Only reached for the account's own playlists when it can
                    // actually be deleted there; a local one stays device-only.
                    if (!isLocalPlaylist) {
                        "This will permanently delete \"${resolvedPlaylist.name}\" from your YouTube account. " +
                            stringResource(R.string.lib_delete_body)
                    } else {
                        "This will permanently remove \"${resolvedPlaylist.name}\" from this device."
                    },
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isLocalPlaylist) {
                            viewModel.deleteLocalPlaylist(resolvedPlaylist.id)
                        } else {
                            viewModel.deleteYouTubePlaylist(resolvedPlaylist.id)
                        }
                        showDeleteDialog = false
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    songPendingRemoval?.let { song ->
        // Same calm destructive idiom as the delete dialog above: no bounce,
        // no scale-in entrance, error-colored confirm.
        AlertDialog(
            onDismissRequest = { songPendingRemoval = null },
            shape = RoundedCornerShape(32.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            title = { Text(stringResource(R.string.remove_from_account_q)) },
            text = {
                Text(
                    if (resolvedPlaylist.id == "LM") {
                        "Removing \"${song.title}\" from Your Likes removes the like from your YouTube account, everywhere you use YouTube."
                    } else {
                        "This removes \"${song.title}\" from \"${resolvedPlaylist.name}\" on your YouTube account, everywhere you use YouTube."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removeSongFromYouTubePlaylist(resolvedPlaylist.id, song)
                        scope.launch {
                            snackbarHostState.showSnackbar(
                                message = "Removed ${song.title}",
                                duration = SnackbarDuration.Short
                            )
                        }
                        songPendingRemoval = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text(stringResource(R.string.remove))
                }
            },
            dismissButton = {
                TextButton(onClick = { songPendingRemoval = null }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}
