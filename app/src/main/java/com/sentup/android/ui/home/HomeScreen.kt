package com.sentup.android.ui.home

import android.Manifest
import android.os.Build
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Subscriptions
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Surface
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.material3.carousel.CarouselState
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.IconToggleButton
import com.sentup.android.ui.components.ExpressivePullToRefresh
import androidx.activity.compose.BackHandler
import androidx.compose.material3.carousel.CarouselDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sentup.android.ui.settings.openExternal
import coil.compose.AsyncImage
import com.sentup.android.data.Song
import com.sentup.android.ui.library.songRowClick
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupSpecularEdge
import com.sentup.android.data.PlayerStyle
import com.sentup.android.ui.components.MusicVideoToggle
import com.sentup.android.ui.components.navBarScrub
import com.sentup.android.ui.components.navBarScrubItem
import com.sentup.android.ui.components.MusicVideoToggleState
import com.sentup.android.ui.components.rememberMusicVideoToggleState
import com.sentup.android.ui.components.rememberPermissionState
import com.sentup.android.ui.components.scrollToTop
import com.sentup.android.ui.player.PlayerViewModel
import com.sentup.android.ui.player.ExpandablePlayer
import com.sentup.android.ui.player.PlayerSheetContent
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import androidx.compose.material3.MaterialShapes
import androidx.compose.animation.with
import kotlinx.coroutines.launch
import com.sentup.android.data.VideoItem
import com.sentup.android.ui.video.VideoHomeContent
import com.sentup.android.data.VideoHomeConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.sentup.android.ui.components.hiddenFraction
import com.sentup.android.data.VideoHomeDestination
import com.sentup.android.ui.library.LibraryContent
import androidx.compose.animation.ExperimentalAnimationApi
import com.sentup.android.BuildConfig
import com.sentup.android.R
import androidx.compose.ui.res.stringResource
import com.sentup.android.data.UpdateRepository
import com.sentup.android.data.UpdateResult
import com.sentup.android.data.isUnknownArtist
import com.sentup.android.data.isUnknownTitle

/**
 * Height of the video top bar when the shell draws it: a 44dp control row with
 * 16dp above and below. Kept beside the padding that reserves room for it, so
 * the two cannot drift apart.
 */
private val VIDEO_SHELL_TOP_BAR_HEIGHT = 76.dp

/** Gap the mini player keeps above the system navigation bar once the floating toolbar is gone. */
private val MINI_PLAYER_RESTING_GAP = 16.dp

/** The bubble's width plus its margin and a gap: what a bar beside it leaves free. */
private val MINI_BUBBLE_END_RESERVE = 92.dp

/** Scrolling down this far closes the music pill into its bubble. */
private val MINI_BUBBLE_COLLAPSE_SCROLL = 56.dp

/**
 * Scrolling back up this far opens the bubble into the pill again: about one
 * deliberate swipe. Less than this is somebody re-reading the row they just
 * passed, and the pill opening and closing under that read as a twitch.
 */
private val MINI_BUBBLE_EXPAND_SCROLL = 200.dp

/**
 * What the Home shell is currently showing. The mode rides along with the tab
 * because switching mode replaces the content behind the same tab index, and
 * the transition between the two is not the one a tab move deserves.
 */
private data class HomeTabKey(val tab: Int, val videoMode: Boolean)

/**
 * The widest the collapsed music pill grows beside a rail. Past this it stops
 * reading as a pill and starts reading as a banner, so on a tablet it centres
 * over the page instead of spanning it.
 */
private val MINI_PLAYER_MAX_WIDE_WIDTH = 560.dp

/**
 * A tab a hand-off asked for, and the mode that tab belongs to.
 *
 * The mode is half the destination rather than a detail: tab 2 is the music
 * Library with the video toggle off and the Subscriptions feed with it on, so a
 * request that named only the index would be ambiguous - and is exactly how the
 * player's artist row ended up opening Subscriptions.
 */
private data class TabRequest(val tab: Int, val videoMode: Boolean)

@OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun HomeScreen(
    onSongClick: (Song) -> Unit,
    playerViewModel: PlayerViewModel,
    viewModel: HomeViewModel = viewModel(),
    isDarkMode: Boolean = true,
    onThemeToggle: (Boolean) -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToStats: () -> Unit = {},
    onNavigateToSubscriptions: () -> Unit = {},
    onNavigateToUpdate: () -> Unit = {},
    onNavigateToVideoPlayer: (VideoItem) -> Unit = {},
    /** Play completed video downloads as a local queue, without URL resolution. */
    onPlayDownloadedVideos: (
        List<com.sentup.android.data.DownloadedVideo>,
        com.sentup.android.data.DownloadedVideo
    ) -> Unit = { _, _ -> },
    /** The same, for the device's own video files from the Library tab. */
    onPlayDeviceVideos: (
        List<com.sentup.android.data.LocalVideo>,
        com.sentup.android.data.LocalVideo
    ) -> Unit = { _, _ -> },
    /**
     * Open the video player on a whole playlist rather than one video, so the
     * rest of it plays after this one and is browsable from the player.
     *
     * Nullable rather than defaulting to `{}`: the playlist screens fall back to
     * [onNavigateToVideoPlayer] when this is absent, and a do-nothing default
     * would make a tap on a playlist row silently do nothing at all.
     */
    /**
     * Queue a video from a long press, either next or at the end. Routed to the
     * video player's ViewModel, which owns the queue.
     */
    onEnqueueVideo: ((com.sentup.android.data.VideoItem, Boolean) -> Unit)? = null,
    onPlayVideoQueue: ((com.sentup.android.data.VideoQueue) -> Unit)? = null,
    onOpenShorts: (List<com.sentup.android.data.ShortsItem>, Int) -> Unit = { _, _ -> },
    /**
     * Open a creator's channel page. Threaded down to every surface that shows
     * a channel name, so tapping one means the same thing everywhere.
     */
    onOpenChannel: (String) -> Unit = {},
    shortsEnabled: Boolean = false,
    compactVideoHome: Boolean = false,
    /** Cards on the video feed play a silent preview when rested on. */
    inlinePreviews: Boolean = false,
    /**
     * Hand the playing song to the video player, from the player's overflow.
     * Null where the host has no video pipeline, which is every caller but
     * MainActivity.
     */
    onWatchAsVideo: (() -> Unit)? = null,
    loadLocalSongs: Boolean = false,
    excludedFolders: Set<String> = emptySet(),
    ambientBackground: Boolean = true,
    playerArtworkColors: Boolean = true,
    videoMode: Boolean = false,
    onVideoModeToggle: (Boolean) -> Unit = {},
    showModeToggle: Boolean = true,
    playerStyle: PlayerStyle = PlayerStyle.EDITORIAL,
    onPlayerStyleChange: (PlayerStyle) -> Unit = {},
    manualScan: Boolean = false,
    localOnly: Boolean = false,
    hasVideoMiniPlayer: Boolean = false,
    /** Prevent a rebuilt Search tab from focusing behind the full video player. */
    hasExpandedVideoPlayer: Boolean = false,
    /** Spotlight: the alternative music Home. Off by default. */
    spotlightHome: Boolean = false,
    /** Use Material 3's compact bar instead of the default floating toolbar. */
    nonExpressiveNavigationBar: Boolean = false,
    /** Visibility, order and recommendation policy for Video mode. */
    videoHomeConfiguration: VideoHomeConfiguration = VideoHomeConfiguration(),
    /**
     * Hoisted so the video mini player - an overlay above the NavHost, outside
     * this composable - can follow the same toolbar this shell hides on scroll.
     */
    floatingToolbarState: androidx.compose.material3.FloatingToolbarState =
        androidx.compose.material3.rememberFloatingToolbarState(),
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val homePreferences = remember(context) { com.sentup.android.data.ThemePreferences(context) }
    val localSongs by viewModel.songs.collectAsState()
    val youtubeSongs by viewModel.youtubeSongs.collectAsState()
    val isYouTubeConnected by viewModel.isYouTubeConnected.collectAsState()
    
    // Use local songs or YouTube songs (which includes fallback search results if not logged in)
    val songs = if (loadLocalSongs) localSongs else youtubeSongs

    // Local play history, for the "Jump back in" rail. Free (a file read, no
    // network), and refreshed whenever the Home tab comes back into view so a
    // song played since the last look shows up.
    val recentlyPlayed by viewModel.recentlyPlayed.collectAsState()

    val currentSong by playerViewModel.currentSong.collectAsState()
    val isPlaying by playerViewModel.isPlaying.collectAsState()
    val isBuffering by playerViewModel.isBuffering.collectAsState()
    val playWhenReady by playerViewModel.playWhenReady.collectAsState()
    val progress by playerViewModel.progress.collectAsState()
    val duration by playerViewModel.duration.collectAsState()
    
    val progressFraction = if (duration > 0) progress.toFloat() / duration.toFloat() else 0f
    
    // Bottom sheet state for player - skip partial expand for direct full-screen
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showPlayerSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val permissionState = rememberPermissionState(
        permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )

    // Load songs based on setting
    LaunchedEffect(Unit, loadLocalSongs, excludedFolders, manualScan) {
        viewModel.checkYouTubeConnection()
        if (loadLocalSongs) {
            if (!permissionState.isGranted) {
                permissionState.launchPermissionRequest()
            } else {
                viewModel.loadSongs(excludedFolders, manualScan)
            }
        } else {
            // Load YouTube recommendations when not using local songs
            viewModel.loadYouTubeRecommendations()
        }
    }

    LaunchedEffect(permissionState.isGranted, loadLocalSongs, excludedFolders, manualScan) {
        if (permissionState.isGranted && loadLocalSongs) {
            viewModel.loadSongs(excludedFolders, manualScan)
        }
    }
    
    // Video mode state
    val trendingVideos by viewModel.trendingVideos.collectAsState()
    val isVideoLoading by viewModel.isVideoLoading.collectAsState()
    val isVideoHomeOffline by viewModel.isVideoHomeOffline.collectAsState()
    val downloadedVideos by viewModel.downloadedVideos.collectAsState()
    val shortsFeed by viewModel.shortsFeed.collectAsState()
    val subscriptionMix by viewModel.subscriptionMix.collectAsState()
    val isSubscriptionMixLoading by viewModel.isSubscriptionMixLoading.collectAsState()
    val localSubscriptions by viewModel.localSubscriptions.collectAsState()
    
    // Load videos when video mode is enabled. Hiding the Home destination
    // takes the only surface these feeds are drawn on with it, so neither the
    // recommendations nor the Shorts shelf are worth a request in that state.
    val videoHomeVisible =
        VideoHomeDestination.HOME in videoHomeConfiguration.visibleDestinations
    // The account avatar, downloads and Settings live in video Home's top bar,
    // and Home can be hidden - which used to strand the viewer, since the
    // switch that brings Home back is itself inside Settings. Hiding Home
    // moves that bar onto the shell instead, where every destination has it.
    val videoShellTopBar = videoMode && !videoHomeVisible
    LaunchedEffect(
        videoMode,
        videoHomeVisible,
        videoHomeConfiguration.recommendationsEnabled,
    ) {
        if (videoMode && videoHomeVisible) {
            viewModel.applyVideoRecommendationsPreference(
                videoHomeConfiguration.recommendationsEnabled
            )
        }
    }

    // Fetch the Home-only Shorts shelf when the user opts in mid-session (the
    // load itself also gates on the preference).
    LaunchedEffect(
        videoMode,
        videoHomeVisible,
        videoHomeConfiguration.recommendationsEnabled,
        isYouTubeConnected,
        localSubscriptions.size,
    ) {
        if (videoMode && videoHomeVisible && !videoHomeConfiguration.recommendationsEnabled) {
            viewModel.loadSubscriptions()
            viewModel.loadSubscriptionMix()
        }
    }

    LaunchedEffect(videoMode, shortsEnabled) {
        if (videoMode && shortsEnabled) {
            viewModel.loadShortsFeed()
        }
    }

    // Saveable, not just remembered. Opening a channel page is a real
    // navigation, so this composable is disposed and restored on the way back,
    // and a plainly-remembered tab index would drop the user on Home every time
    // they looked at a creator from the Subscriptions feed. The scroll states
    // below already survive it, because rememberLazyListState is saveable.
    val videoRootTab = videoHomeConfiguration.orderedVisibleDestinations
        .firstOrNull()?.tabId ?: VideoHomeDestination.HOME.tabId

    // The remembered tab may name a destination that has since been hidden, so
    // it is clamped as the state is created rather than only corrected by the
    // effect below - otherwise a cold entry composes the hidden tab's content
    // once before the correction lands.
    var selectedTab by androidx.compose.runtime.saveable.rememberSaveable(videoMode) {
        // The user's start screen, or the last tab they were on.
        val stored = homePreferences.getStartHomeTab(videoMode)
        val reachable = !videoMode ||
            videoHomeConfiguration.orderedVisibleDestinations.any { it.tabId == stored }
        mutableIntStateOf(if (reachable) stored else videoRootTab)
    }

    val rootTab = if (videoMode) videoRootTab else 0
    val visualTabOrder = if (videoMode) {
        videoHomeConfiguration.destinationOrder.map(VideoHomeDestination::tabId)
    } else {
        listOf(0, 1, 2)
    }

    // A destination can be hidden while it is selected (including from the
    // Settings screen on top of Home). Land on the first configured page when
    // Home becomes visible again rather than rendering an unreachable tab.
    LaunchedEffect(videoMode, videoHomeConfiguration) {
        if (videoMode && videoHomeConfiguration.orderedVisibleDestinations.none {
                it.tabId == selectedTab
            }
        ) {
            selectedTab = videoRootTab
        }
    }

    LaunchedEffect(selectedTab, videoMode) {
        homePreferences.setLastHomeTab(videoMode, selectedTab)
    }

    // A tab asked for by something that also had to switch modes, held until the
    // mode it belongs to is actually the live one.
    //
    // [scar] This is load-bearing and the reason a hand-off across the video
    // toggle cannot simply assign `selectedTab`. The mode switch is a
    // preference write, so it lands a recomposition later - and `selectedTab`
    // is keyed on `videoMode`, deliberately, so that switching modes lands on
    // that mode's own last tab. The key therefore *re-creates* the state after
    // the write, discarding anything assigned before it: the player's artist
    // row set tab 2, the toggle flipped, and the fresh state read the stored
    // music tab instead, dropping the user on Home with the artist request
    // still pending. The request is matched against the mode it was made for,
    // so it can never apply to the mode it was trying to leave.
    var pendingTabRequest by remember { mutableStateOf<TabRequest?>(null) }
    LaunchedEffect(videoMode, pendingTabRequest) {
        val request = pendingTabRequest ?: return@LaunchedEffect
        if (request.videoMode == videoMode) {
            selectedTab = request.tab
            pendingTabRequest = null
        }
    }

    /**
     * Land on [tab] of [wantVideoMode], switching modes first if that is where
     * the tab lives. The only correct way to open a tab from a hand-off: a tab
     * index names a different screen in each mode (2 is the music Library or the
     * Subscriptions feed), so the mode is part of the destination.
     */
    fun goToTab(tab: Int, wantVideoMode: Boolean = false) {
        if (videoMode == wantVideoMode) {
            selectedTab = tab
        } else {
            onVideoModeToggle(wantVideoMode)
            pendingTabRequest = TabRequest(tab, wantVideoMode)
        }
    }

    // Every tab's scroll position, remembered HERE rather than inside the tab
    // content, for two reasons.
    //
    // Anything remembered inside the AnimatedContent below is scoped to that
    // target's own composition and disposed once the transition settles, so a
    // state living down there is discarded on every tab switch - leave Home
    // halfway down, glance at Search, come back, and you are at the top. Above
    // the AnimatedContent the position survives.
    //
    // It also has to be reachable from the nav bar, so re-tapping the current
    // tab can send its list back to the top.
    //
    // One per tab AND per mode where the mode swaps the content: video Home and
    // music Home are different lists, and sharing a state between them would
    // restore one list's index into the other.
    val videoHomeScrollState = rememberLazyListState()
    val musicHomeScrollState = rememberLazyListState()
    // Search is one screen in both modes, but a mode switch on it has the
    // outgoing and the incoming copy composed together for the length of the
    // cross-fade, and a LazyListState can drive only one list (see the tab
    // AnimatedContent below).
    val musicSearchScrollState = rememberLazyListState()
    val videoSearchScrollState = rememberLazyListState()
    val subscriptionsScrollState = rememberLazyListState()
    val musicLibraryScrollState = rememberLazyListState()
    val videoLibraryScrollState = rememberLazyListState()
    // Explore's own list, for the same reason the others have one: anything
    // remembered inside the tab's own composition is discarded once the
    // transition settles, so leaving Explore halfway down and coming back
    // would land at the top.
    val exploreScrollState = rememberLazyListState()

    // Which of the above the visible tab is currently driving.
    val currentTabScrollState = when (selectedTab) {
        0 -> if (videoMode) videoHomeScrollState else musicHomeScrollState
        1 -> if (videoMode) videoSearchScrollState else musicSearchScrollState
        2 -> if (videoMode) subscriptionsScrollState else musicLibraryScrollState
        VideoHomeDestination.LIBRARY.tabId -> videoLibraryScrollState
        else -> exploreScrollState
    }

    // Lives outside the mode-swapped content so the thumb keeps animating
    // while the music/video home content cross-fades underneath it
    val modeToggleState = rememberMusicVideoToggleState(videoMode)

    // Material owns both the offset range and the settle/fling behavior. The
    // toolbar is bottom-centred, so its exit direction is down and the Home
    // shell only needs to forward nested scroll from whichever tab is active.
    // The user's choices for the bar: whether it hides, and when it names
    // its tabs.
    val homeNavigation by homePreferences.homeNavigation.collectAsState()
    val barHidesOnScroll = homeNavigation.barHidesOnScroll
    // A bar told to stay put is brought back if it was away, and everything
    // that follows it (the mini player, the bars floating above) with it.
    LaunchedEffect(barHidesOnScroll) {
        if (!barHidesOnScroll) {
            floatingToolbarState.offset = 0f
            floatingToolbarState.contentOffset = 0f
        }
    }
    val floatingToolbarScrollBehavior = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = FloatingToolbarExitDirection.Bottom,
        state = floatingToolbarState
    )

    // Handle back button to return to Home tab if on Search or Library
    BackHandler(enabled = selectedTab != rootTab) {
        selectedTab = rootTab
    }

    // The video-only tabs (2/3/4) do not exist in music mode, so a mode switch
    // away from one has to land on that mode's own root rather than keep a tab
    // number that names nothing.
    LaunchedEffect(videoMode) {
        if (!videoMode && selectedTab > VideoHomeDestination.SUBSCRIPTIONS.tabId) selectedTab = 0
    }

    // Re-read the history when Home becomes the active tab. Playback writes an
    // entry only after 15s, so coming back from the player is exactly when the
    // rail has something new to show.
    LaunchedEffect(selectedTab, videoMode) {
        if (selectedTab == 0 && !videoMode) viewModel.refreshRecentlyPlayed()
    }

    // Auth Dialog State
    var showAuthDialog by remember { mutableStateOf(false) }
    var addAuthAsNewProfile by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }

    // The avatar always opens the profile switcher now, signed in or not: with
    // device-only profiles there is always something to switch between, and
    // sending a signed-out user straight to a Google login was the app assuming
    // an account is the only way to have an identity.
    // In music mode the profile picture opens the Sentüp profile; holding it,
    // there and everywhere, is the account switcher. Video mode keeps the
    // tap as the switcher: its profile is the account, and the screen is
    // about listening.
    var showProfileScreen by remember { mutableStateOf(false) }
    val onProfileClick: () -> Unit = {
        if (videoMode) showAccountSheet = true else showProfileScreen = true
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    
    // Loading state for playlist fetch
    var isPlaylistLoading by remember { mutableStateOf(false) }
    val isLoading by viewModel.isLoading.collectAsState()
    
    // Artist screen state (for navigation from player)
    var viewedArtistFromPlayer by remember { mutableStateOf<String?>(null) }
    // Where a Library sub-screen handed over from another tab goes back to:
    // that tab, with the player left exactly as it was. Without it back
    // strands the user in a Library they never navigated through; the older
    // answer, reopening the player sheet, replayed its whole expand animation
    // over a screen the user was trying to leave. Null when the hand-off began
    // in the Library itself, or crossed a mode switch - the tab it began on
    // then belongs to the other mode.
    var libraryReturnTab by remember { mutableStateOf<Int?>(null) }
    fun noteLibraryReturn() {
        libraryReturnTab = if (videoMode) null else selectedTab.takeIf { it != 2 }
    }

    // The album counterpart, from the player's overflow menu. A name rather
    // than a PlaylistDisplayItem, because a device album is identified by its
    // name and the Library builds the rest from the songs it already has.
    var viewedAlbumFromPlayer by remember { mutableStateOf<String?>(null) }

    // The same hand-off, asked for from outside this screen entirely: the
    // "Open music artist page" cross-link on a creator's channel page pops back
    // to home and leaves the request on the ViewModel, because a NavHost
    // destination cannot reach the tab state that lives in here.
    val pendingArtistPage by viewModel.pendingArtistPage.collectAsState()
    LaunchedEffect(pendingArtistPage) {
        pendingArtistPage?.let { artist ->
            // The artist page is the music-mode view of this creator, and tab 2
            // belongs to the Subscriptions feed while the video toggle is on.
            // Asking for it is therefore asking to be in music mode.
            noteLibraryReturn()
            viewedArtistFromPlayer = artist
            goToTab(2)
            viewModel.consumeArtistPageRequest()
        }
    }
    // Set by Spotlight's shortcut grid and shelves; consumed by LibraryContent
    // as soon as the tab renders, so returning to Library later lands on the
    // list rather than re-opening the playlist.
    var viewedPlaylistFromHome by remember {
        mutableStateOf<com.sentup.android.data.PlaylistDisplayItem?>(null)
    }
    // The video-mode counterpart, handed to VideoLibraryContent the same way.
    var viewedVideoPlaylistFromHome by remember {
        mutableStateOf<com.sentup.android.data.VideoPlaylist?>(null)
    }

    // A playlist link shared or opened into the app, which lands at
    // MainActivity and cannot reach the tab state from there. Same hand-off as
    // pendingArtistPage, and it sets the mode for the same reason: the Library
    // that can show this playlist only exists on one side of the video toggle,
    // so leaving the toggle alone would land on the wrong tab and read as the
    // share having done nothing.
    val pendingPlaylistPage by viewModel.pendingPlaylistPage.collectAsState()
    LaunchedEffect(pendingPlaylistPage) {
        pendingPlaylistPage?.let { playlist ->
            noteLibraryReturn()
            viewedPlaylistFromHome = playlist
            goToTab(2)
            viewModel.consumePlaylistPageRequest()
        }
    }
    // A shared Spotify link: the Library opens its import screen on it and
    // consumes the request, so all this has to do is get to the music Library.
    val pendingSpotifyImport by viewModel.pendingSpotifyImport.collectAsState()
    LaunchedEffect(pendingSpotifyImport) {
        if (pendingSpotifyImport != null) goToTab(2)
    }
    val pendingVideoPlaylistPage by viewModel.pendingVideoPlaylistPage.collectAsState()
    LaunchedEffect(pendingVideoPlaylistPage) {
        pendingVideoPlaylistPage?.let { playlist ->
            viewedVideoPlaylistFromHome = playlist
            // Video mode's Library is tab 3; music's is tab 2.
            goToTab(3, wantVideoMode = true)
            viewModel.consumeVideoPlaylistPageRequest()
        }
    }
    // Long-pressed song, for the options sheet. Hosted here, once, rather than
    // per screen: the sheet acts on the PlayerViewModel, which lives at this
    // level, and the Library's sub-routes would each otherwise need their own.
    var songOptionsTarget by remember { mutableStateOf<Song?>(null) }

    // Update check state
    val updateRepository = remember { UpdateRepository() }
    var updateResult by remember { mutableStateOf<UpdateResult?>(null) }
    // Held separately so the pill's label survives its exit animation
    var latestVersion by remember { mutableStateOf("") }

    val updateContext = androidx.compose.ui.platform.LocalContext.current
    val updatePrompts = remember(updateContext) {
        com.sentup.android.data.UpdatePromptStore(updateContext)
    }

    // First launch after an update: this version's highlights, once, from
    // the file bundled in the APK. Decided before the update check so the
    // two never open over each other - the prompt waits for this to close.
    var whatsNew by remember {
        mutableStateOf(
            updatePrompts.whatsNewVersion(BuildConfig.VERSION_NAME)?.let { version ->
                val highlights = com.sentup.android.data.ReleaseHighlights.bundled(updateContext, version)
                if (highlights.isEmpty()) {
                    // A release that bundled no highlights has nothing to
                    // show; record it so the next one is not mistaken for it.
                    updatePrompts.markWhatsNewSeen(BuildConfig.VERSION_NAME)
                    null
                } else {
                    version to highlights
                }
            }
        )
    }
    var showUpdatePrompt by remember { mutableStateOf(false) }

    // Check for updates on launch and, for an app left open for days, when
    // it comes back to the foreground after UPDATE_RECHECK_MS. Release builds
    // only, and never in Local Only mode: a metadata request is a request.
    val updateLifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val currentLocalOnly by rememberUpdatedState(localOnly)
    LaunchedEffect(updateLifecycleOwner) {
        if (BuildConfig.DEBUG) return@LaunchedEffect
        // A build with no repository has no release feed to read, so the whole
        // check is skipped instead of asking GitHub for an empty path.
        if (BuildConfig.GITHUB_REPO.isBlank()) return@LaunchedEffect
        var lastCheckAtMs = 0L
        updateLifecycleOwner.lifecycle.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.RESUMED) {
            val now = android.os.SystemClock.elapsedRealtime()
            if (currentLocalOnly) return@repeatOnLifecycle
            if (lastCheckAtMs != 0L && now - lastCheckAtMs < UPDATE_RECHECK_MS) return@repeatOnLifecycle
            lastCheckAtMs = now
            val result = updateRepository.checkForUpdate(
                repoPath = BuildConfig.GITHUB_REPO,
                currentVersion = BuildConfig.VERSION_NAME
            )
            updateResult = result
            if (result is UpdateResult.UpdateAvailable) {
                latestVersion = result.latestVersion
                if (updatePrompts.shouldPromptFor(result.latestVersion)) showUpdatePrompt = true
            }
        }
    }

    whatsNew?.let { (version, highlights) ->
        com.sentup.android.ui.settings.WhatsNewSheet(
            version = version,
            highlights = highlights,
            onDismiss = {
                updatePrompts.markWhatsNewSeen(BuildConfig.VERSION_NAME)
                whatsNew = null
            }
        )
    }
    (updateResult as? UpdateResult.UpdateAvailable)?.takeIf { showUpdatePrompt && whatsNew == null }?.let { update ->
        val highlights = remember(update.releaseNotes) {
            com.sentup.android.data.ReleaseHighlights.fromReleaseBody(update.releaseNotes)
        }
        // Every way out of the dialog is recorded, so it comes back in two
        // days rather than on the next launch.
        val closePrompt = {
            updatePrompts.markPrompted(update.latestVersion)
            showUpdatePrompt = false
        }
        com.sentup.android.ui.settings.UpdatePromptDialog(
            installedVersion = com.sentup.android.data.ReleaseHighlights.releaseVersion(BuildConfig.VERSION_NAME),
            latestVersion = update.latestVersion,
            highlights = highlights,
            onUpdate = {
                closePrompt()
                // The same hand-off the update screen makes. When nothing can
                // take the link, that screen is where the reason is explained.
                val url = UpdateRepository.findBestApk(update.apkAssets)?.downloadUrl ?: update.htmlUrl
                if (!updateContext.openExternal(url)) onNavigateToUpdate()
            },
            onDetails = {
                closePrompt()
                onNavigateToUpdate()
            },
            onLater = closePrompt
        )
    }

    // How much clearance bottom-anchored UI needs above the nav bar inset to
    // stay clear of the floating overlays: the nav pill always, the music
    // pill (top edge at 180dp) and/or the video mini player (top edge at
    // 188dp, stacked to 284dp when the music pill is also alive). Animated so
    // FABs glide instead of jumping when a mini player appears.
    val musicPillVisible = currentSong != null
    // From 600dp across, navigation moves to a rail on the start edge and the
    // page starts where the rail ends. The horizontal safe-drawing inset is
    // taken here too: a phone on its side has its cutout and (with three-button
    // navigation) its system bar on the left or right, which a portrait-only
    // app never had to think about.
    val windowLayout = com.sentup.android.ui.theme.currentWindowLayout()
    val useRail = windowLayout.usesNavigationRail
    val shellLayoutDirection = androidx.compose.ui.platform.LocalLayoutDirection.current
    val safeHorizontalInsets = WindowInsets.safeDrawing
        .only(androidx.compose.foundation.layout.WindowInsetsSides.Horizontal)
        .asPaddingValues()
    val safeStartInset = safeHorizontalInsets.calculateStartPadding(shellLayoutDirection)
    val safeEndInset = safeHorizontalInsets.calculateEndPadding(shellLayoutDirection)
    val contentStartInset = safeStartInset + when {
        !useRail -> 0.dp
        nonExpressiveNavigationBar -> HOME_STANDARD_RAIL_RESERVE
        else -> HOME_FLOATING_RAIL_RESERVE
    }
    val contentEndInset = safeEndInset
    // The standard non-expressive NavigationBar is 80dp tall. The expressive
    // toolbar occupies 84dp including its bottom breathing room. Keep the same
    // clearance above either variant so overlaid controls do not jump or
    // collide when this preference changes. With a rail there is no bar at the
    // bottom at all, and the pill rests just above the system inset.
    val navigationOverlayInset = when {
        useRail -> 0.dp
        nonExpressiveNavigationBar -> 84.dp
        else -> 88.dp
    }
    val miniPlayerCollapsedSpacing = when {
        useRail -> 12.dp
        nonExpressiveNavigationBar -> 96.dp
        else -> 100.dp
    }
    // Only the floating toolbar hides on scroll; the standard NavigationBar
    // stays pinned, so there is nothing for the pill to follow there, and a
    // rail never hides.
    val miniPlayerFollowDistancePx = with(androidx.compose.ui.platform.LocalDensity.current) {
        if (nonExpressiveNavigationBar || useRail) 0f
        else (miniPlayerCollapsedSpacing - MINI_PLAYER_RESTING_GAP).toPx()
    }
    val miniPlayerFollowOffsetPx: () -> Float = {
        miniPlayerFollowDistancePx * floatingToolbarState.hiddenFraction()
    }
    // Whether the music pill is closed into its bubble. A latch with two
    // different distances rather than the toolbar's own hidden fraction: the
    // toolbar comes back on the first pixel of an upward scroll, which is
    // right for navigation and made the pill open and close on every small
    // correction. [scar October 2026] It closes after a short scroll down and
    // opens after a deliberate scroll up, or as soon as the list is back
    // where the downward scroll began - which is what opens it at the top of
    // a list, where there is no further up to scroll.
    val bubbleDensity = androidx.compose.ui.platform.LocalDensity.current
    val miniBubbleCollapsePx = with(bubbleDensity) { MINI_BUBBLE_COLLAPSE_SCROLL.toPx() }
    val miniBubbleExpandPx = with(bubbleDensity) { MINI_BUBBLE_EXPAND_SCROLL.toPx() }
    var miniBubble by remember { mutableStateOf(false) }
    // Keyed on the tab: the scroll total is shared by every tab's list, so a
    // new tab starts as a pill, measured from wherever the total stands.
    LaunchedEffect(floatingToolbarState, selectedTab, videoMode) {
        miniBubble = false
        // The content offset falls as the list scrolls down and rises as it
        // scrolls up. `turn` is the furthest point of the current run.
        var turn = floatingToolbarState.contentOffset
        var downRunStart = turn
        // The bubble is a mid-scroll shape, never a resting one: when the list
        // stops moving the pill opens back up, so nothing hangs beside the
        // navigation bar as a stray circle [user report, October 2026].
        var lastScrollAt = android.os.SystemClock.elapsedRealtime()
        kotlinx.coroutines.coroutineScope {
            launch {
                androidx.compose.runtime.snapshotFlow { floatingToolbarState.contentOffset }.collect { offset ->
                    lastScrollAt = android.os.SystemClock.elapsedRealtime()
                    if (!miniBubble) {
                        if (offset > turn) turn = offset
                        if (turn - offset >= miniBubbleCollapsePx) {
                            downRunStart = turn
                            turn = offset
                            miniBubble = true
                        }
                    } else {
                        if (offset < turn) turn = offset
                        if (offset - turn >= miniBubbleExpandPx || offset >= downRunStart) {
                            turn = offset
                            miniBubble = false
                        }
                    }
                }
            }
            launch {
                while (true) {
                    kotlinx.coroutines.delay(250L)
                    if (miniBubble && android.os.SystemClock.elapsedRealtime() - lastScrollAt > 600L) {
                        turn = floatingToolbarState.contentOffset
                        miniBubble = false
                    }
                }
            }
        }
    }
    // Only the floating toolbar feeds that scroll total; a pinned navigation
    // bar or a rail leaves the pill a pill.
    //
    // The user can also pin it either way: never a bubble, or always one.
    // Always still needs the floating toolbar, for the same reason.
    val miniShrinkContext = androidx.compose.ui.platform.LocalContext.current
    val miniShrink = remember(miniShrinkContext) {
        com.sentup.android.data.ThemePreferences(miniShrinkContext)
    }.miniPlayerCustomization.collectAsState().value.shrink
    val miniBubbleWanted = when (miniShrink) {
        com.sentup.android.data.MiniPlayerShrink.NEVER -> false
        com.sentup.android.data.MiniPlayerShrink.ALWAYS -> true
        com.sentup.android.data.MiniPlayerShrink.ON_SCROLL -> miniBubble
    }
    val miniBubbleFraction = androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (miniBubbleWanted && !nonExpressiveNavigationBar && !useRail) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "miniBubbleFraction"
    )
    val bottomOverlayInset by androidx.compose.animation.core.animateDpAsState(
        targetValue = when {
            musicPillVisible && hasVideoMiniPlayer -> navigationOverlayInset + 196.dp
            hasVideoMiniPlayer -> navigationOverlayInset + 108.dp
            musicPillVisible -> navigationOverlayInset + 100.dp
            else -> navigationOverlayInset
        },
        animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec(),
        label = "bottomOverlayInset"
    )
    // The same clearance as it moves, for a bar that floats above the content
    // and should follow the overlays rather than stand over the gap they
    // leave: down to the screen edge as the toolbar hides, and beside the
    // music bubble once the pill has closed into it. With the video mini bar
    // up, or a pinned bar or rail, nothing moves and it is the resting inset.
    val overlaysMove by rememberUpdatedState(
        !nonExpressiveNavigationBar && !useRail && !hasVideoMiniPlayer
    )
    val overlayNavInset by rememberUpdatedState(navigationOverlayInset)
    val overlayHasMusicPill by rememberUpdatedState(musicPillVisible)
    val overlayRestingInset = rememberUpdatedState(bottomOverlayInset)
    val bottomOverlayMotion = remember(floatingToolbarState, bubbleDensity) {
        com.sentup.android.ui.components.BottomOverlayMotion(
            bottomPx = {
                with(bubbleDensity) {
                    val resting = overlayRestingInset.value
                    if (!overlaysMove) {
                        resting.toPx()
                    } else {
                        val hidden = floatingToolbarState.hiddenFraction()
                        val bubble = miniBubbleFraction.value.coerceIn(0f, 1f)
                        val nav = overlayNavInset.toPx()
                        val base = nav + (MINI_PLAYER_RESTING_GAP.toPx() - nav) * hidden
                        // The pill rides down with the toolbar and closes the
                        // gap between them as it goes.
                        val pill = (resting.toPx() - nav) * (1f - 0.12f * hidden) * (1f - bubble)
                        base + pill
                    }
                }
            },
            endInsetPx = {
                if (!overlaysMove || !overlayHasMusicPill) 0f
                else with(bubbleDensity) {
                    MINI_BUBBLE_END_RESERVE.toPx() * miniBubbleFraction.value.coerceIn(0f, 1f)
                }
            }
        )
    }
    val navBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val statusBarInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // The one inset every tab list scrolls inside, top and bottom. It belongs
    // in each list's contentPadding and never on a parent modifier: padding a
    // scroll container shrinks its viewport, so content is clipped at the
    // status bar instead of passing under it, and the top of the screen reads
    // as a system title bar. The bottom already worked this way.
    // The floating toolbar and the pill above it both apply
    // `navigationBarsPadding()` themselves, so the clearance this list needs is
    // the overlay height alone. Adding `navBarInset` on top of that counted the
    // system inset twice and left a screenful of dead space under the last card
    // once the feed was scrolled to its end.
    val listContentPadding = PaddingValues(
        top = statusBarInset + if (videoShellTopBar) VIDEO_SHELL_TOP_BAR_HEIGHT else 0.dp,
        bottom = bottomOverlayInset + SentupGroup.gap
    )

    // Use Box overlay instead of Scaffold for truly floating navbar
    androidx.compose.runtime.CompositionLocalProvider(
        com.sentup.android.ui.components.LocalBottomOverlayInset provides bottomOverlayInset,
        com.sentup.android.ui.components.LocalBottomOverlayMotion provides bottomOverlayMotion,
        com.sentup.android.ui.profile.LocalOpenAccountSwitcher provides { showAccountSheet = true },
        com.sentup.android.ui.components.LocalNowPlaying provides
            com.sentup.android.ui.components.NowPlayingState(currentSong?.id, isPlaying)
    ) {
    // Experiment: flick between the main tabs with a swipe, same contract as
    // the channel page - a fast horizontal flick moves to the next/previous
    // visible destination and the existing AnimatedContent slide carries it,
    // so there is deliberately no follow-the-finger pager here. Observe-only:
    // nothing is consumed, taps and vertical scrolling are untouched. The flick
    // gates (fast, far, horizontal) alone did not keep shelf browsing or the
    // mini player's swipes from tripping it, so a gesture any child consumed
    // is never a tab flick. Uses the nav bar's own destination order, so hidden
    // destinations are never landed on.
    //
    // That alone made the gesture close to dead on music Home, which is mostly
    // shelves: nearly every swipe starts on one. So a shelf hands the flick on
    // the way a carousel inside a pager does - it scrolls first, and only a
    // flick it had nowhere to go with (already at its end in that direction)
    // reaches the tabs. That arrives through nested scroll as delta the shelf
    // left unconsumed; the mini player's swipes are not scrollables and never
    // dispatch it, so they stay excluded.
    val gestureHaptics = com.sentup.android.util.rememberSentupHaptics()
    val gestureDensity = LocalDensity.current
    val gestureTabIds = if (videoMode) {
        videoHomeConfiguration.orderedVisibleDestinations.map { it.tabId }
    } else {
        listOf(0, 1, 2)
    }
    val flickToTab: (towardNext: Boolean) -> Unit = { towardNext ->
        val index = gestureTabIds.indexOf(selectedTab)
        val target = if (towardNext) gestureTabIds.getOrNull(index + 1)
        else gestureTabIds.getOrNull(index - 1)
        if (target != null && target != selectedTab) {
            gestureHaptics.performHapticFeedback(HapticFeedbackType.ContextClick)
            selectedTab = target
        }
    }
    val currentFlickToTab by rememberUpdatedState(flickToTab)
    val shelfEdgeFlick = remember(gestureDensity) {
        object : NestedScrollConnection {
            val minDistancePx = with(gestureDensity) { 72.dp.toPx() }
            val minVelocityPx = with(gestureDensity) { 600.dp.toPx() }
            // Any real movement of the shelf means the swipe was browsing it.
            val slackPx = with(gestureDensity) { 2.dp.toPx() }
            var shelfMovedX = 0f
            var leftoverX = 0f

            fun reset() {
                shelfMovedX = 0f
                leftoverX = 0f
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                if (source == NestedScrollSource.UserInput) {
                    shelfMovedX += kotlin.math.abs(consumed.x)
                    leftoverX += available.x
                }
                return Offset.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val edgeFlick = shelfMovedX <= slackPx &&
                    kotlin.math.abs(leftoverX) >= minDistancePx &&
                    kotlin.math.abs(available.x) >= minVelocityPx &&
                    (available.x < 0f) == (leftoverX < 0f)
                val towardNext = leftoverX < 0f
                reset()
                if (edgeFlick) currentFlickToTab(towardNext)
                return Velocity.Zero
            }
        }
    }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .then(
                if (nonExpressiveNavigationBar || useRail || !barHidesOnScroll) Modifier
                else Modifier.nestedScroll(floatingToolbarScrollBehavior)
            )
            .nestedScroll(shelfEdgeFlick)
            .pointerInput(selectedTab, videoMode, gestureTabIds) {
                val minDistancePx = with(gestureDensity) { 96.dp.toPx() }
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // A drag that ended without a fling never reaches
                    // onPostFling, so each gesture starts from a clean slate.
                    shelfEdgeFlick.reset()
                    var totalX = 0f
                    var totalY = 0f
                    val startTime = down.uptimeMillis
                    var endTime = startTime
                    var aborted = false
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.changes.size > 1) {
                            aborted = true
                            break
                        }
                        // A child claimed this gesture: a shelf or carousel
                        // scrolling, the mini player's swipe-to-skip or its
                        // drag to dismiss. Their drag detectors consume every
                        // move once past touch slop (even at a list's end),
                        // and children see the Main pass before this parent,
                        // so a consumed change means the swipe was theirs.
                        if (event.changes.any { it.isConsumed }) {
                            aborted = true
                            break
                        }
                        for (change in event.changes) {
                            totalX += change.position.x - change.previousPosition.x
                            totalY += change.position.y - change.previousPosition.y
                        }
                        endTime = event.changes.firstOrNull()?.uptimeMillis ?: endTime
                        if (event.changes.all { !it.pressed }) break
                    }
                    if (aborted) return@awaitEachGesture
                    val duration = endTime - startTime
                    if (duration in 1..400 &&
                        kotlin.math.abs(totalX) >= minDistancePx &&
                        kotlin.math.abs(totalX) >= 1.5f * kotlin.math.abs(totalY)
                    ) {
                        flickToTab(totalX < 0f)
                    }
                }
            }
    ) {
        // Main content
        if (!loadLocalSongs || permissionState.isGranted) {
            androidx.compose.animation.AnimatedContent(
                modifier = Modifier.padding(start = contentStartInset, end = contentEndInset),
                // Keyed on the mode as well as the tab so the spec below can
                // tell a tab move from a mode switch. They deserve different
                // motion and used to share one.
                targetState = HomeTabKey(selectedTab, videoMode),
                label = "TabTransition",
                transitionSpec = {
                    // A mode switch cross-fades. Both Homes draw their header
                    // in the same place, so sliding the page sideways drags
                    // that header off with it and reads as the whole chrome
                    // leaving - when nothing about it was supposed to move.
                    // A fade leaves it apparently stationary.
                    // The page being left also grows a touch as it goes, the
                    // morph the mode swap has always had.
                    if (initialState.videoMode != targetState.videoMode) {
                        return@AnimatedContent androidx.compose.animation.fadeIn(
                            androidx.compose.animation.core.tween(220)
                        ) togetherWith (androidx.compose.animation.fadeOut(
                            androidx.compose.animation.core.tween(180)
                        ) + androidx.compose.animation.scaleOut(
                            targetScale = 1.05f,
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 220)
                        ))
                    }
                    val initialRank = visualTabOrder.indexOf(initialState.tab).takeIf { it >= 0 }
                        ?: initialState.tab
                    val targetRank = visualTabOrder.indexOf(targetState.tab).takeIf { it >= 0 }
                        ?: targetState.tab
                    val direction = if (targetRank > initialRank) 1 else -1
                    if (direction > 0) {
                        // Moving forward (Right): New enters from Right, Old leaves to Left
                        (androidx.compose.animation.slideInHorizontally { width -> width } + 
                                androidx.compose.animation.fadeIn()) togetherWith
                                (androidx.compose.animation.slideOutHorizontally { width -> -width / 3 } + 
                                        androidx.compose.animation.fadeOut())
                    } else {
                        // Moving backward (Left): New enters from Left, Old leaves to Right
                        (androidx.compose.animation.slideInHorizontally { width -> -width / 3 } + 
                                androidx.compose.animation.fadeIn()) togetherWith
                                (androidx.compose.animation.slideOutHorizontally { width -> width } + 
                                        androidx.compose.animation.fadeOut())
                    }
                }
            ) { tabKey ->
                val targetTab = tabKey.tab
                // Each page draws the mode it was keyed on, never the live one.
                // A mode switch keeps the page being left composed until its
                // fade ends; reading the live mode there made it rebuild itself
                // as the mode being entered, so two lists shared one
                // LazyListState for the length of the transition. The state
                // binds to whichever list attached last and is not handed back
                // when that list is disposed, so if the survivor was the other
                // one it stopped scrolling until the tab was rebuilt: every
                // drag reached it and remeasured a list no longer on screen.
                // [scar October 2026, read out of a heap dump of a stuck Home]
                @Suppress("NAME_SHADOWING")
                val videoMode = tabKey.videoMode
                when (targetTab) {
                    0 -> {
                        videoMode.let { videoModeContent ->
                            // Video Mode: Show video content
                            if (videoModeContent && localOnly) {
                                com.sentup.android.ui.components.LocalOnlyNotice(
                                    subtitle = stringResource(R.string.local_only_video_mode_subtitle),
                                    onOpenSettings = onNavigateToSettings
                                )
                            } else if (videoModeContent) {
                                VideoHomeContent(
                                    compact = compactVideoHome,
                                    inlinePreviews = inlinePreviews,
                                    // Recommendations off: a shuffle across the
                                    // followed channels' histories rather than a
                                    // second copy of the Subscriptions tab.
                                    videos = if (videoHomeConfiguration.recommendationsEnabled) {
                                        trendingVideos
                                    } else {
                                        subscriptionMix
                                    },
                                    isLoading = if (videoHomeConfiguration.recommendationsEnabled) {
                                        isVideoLoading
                                    } else {
                                        isSubscriptionMixLoading
                                    },
                                    isOffline = isVideoHomeOffline,
                                    downloadedVideos = downloadedVideos,
                                    onVideoClick = { video ->
                                        // Navigate to video player screen
                                        onNavigateToVideoPlayer(video)
                                    },
                                    onDownloadedVideoClick = { video ->
                                        onPlayDownloadedVideos(downloadedVideos, video)
                                    },
                                    shortsEnabled = shortsEnabled,
                                    shorts = if (shortsEnabled) shortsFeed else emptyList(),
                                    onShortClick = { index -> onOpenShorts(shortsFeed, index) },
                                    onOpenShorts = onOpenShorts,
                                    // Without this the long-press sheet loses
                                    // its whole queue section, on the one feed
                                    // people spend the most time in.
                                    onEnqueueVideo = onEnqueueVideo,
                                    onOpenChannel = onOpenChannel,
                                    onProfileClick = onProfileClick,
                                    onSettingsClick = onNavigateToSettings,
                                    onDownloadsClick = onNavigateToDownloads,
                                    onRefresh = { viewModel.refreshVideos() },
                                    recommendationsEnabled =
                                        videoHomeConfiguration.recommendationsEnabled,
                                    isDarkMode = isDarkMode,
                                    contentPadding = listContentPadding,
                                    viewModel = viewModel,
                                    videoMode = videoMode,
                                    onVideoModeToggle = onVideoModeToggle,
                                    showModeToggle = showModeToggle,
                                    modeToggleState = modeToggleState,
                                    listState = videoHomeScrollState
                                )
                            }
                            else {
                                // Spin, the wheel, rides over whichever music
                                // Home is showing as a child page: back peels it
                                // away to the Home underneath. Declared here so
                                // leaving the tab closes it, as it closes a
                                // Library sub-screen.
                                var spinOpen by remember { mutableStateOf(false) }
                                var spinSource by remember { mutableStateOf(SpinSource.Mix) }
                                com.sentup.android.ui.components.PredictiveBackStack(
                                    childOpen = spinOpen,
                                    onBack = { spinOpen = false },
                                    background = {
                                        // Music Mode, Spotlight: the shortcut-grid and
                                        // shelves alternative. Same flows, same overlays,
                                        // same tab system - only the composition of this one
                                        // tab differs, which is the move the video toggle
                                        // already established.
                                        if (spotlightHome) {
                                            val spotlightPlaylists by viewModel.userPlaylists.collectAsState()
                                            val spotlightLiked by viewModel.likedSongs.collectAsState()
                                            SpotlightHomeContent(
                                                songs = songs,
                                                recentlyPlayed = recentlyPlayed,
                                                likedSongs = spotlightLiked,
                                                playlists = spotlightPlaylists,
                                                isInitialLoading = isLoading && songs.isEmpty(),
                                                onSongLongPress = { song -> songOptionsTarget = song },
                                                onSongClick = { song ->
                                                    playerViewModel.playQueue(songs, song)
                                                    showPlayerSheet = true
                                                },
                                                onPlaySongs = { queue, start ->
                                                    playerViewModel.playQueue(queue, start)
                                                    showPlayerSheet = true
                                                },
                                                onRecentClick = { song ->
                                                    playerViewModel.playQueue(recentlyPlayed, song)
                                                    showPlayerSheet = true
                                                },
                                                // Playlist detail lives in LibraryContent, so
                                                // Spotlight hands the playlist over and
                                                // switches tab: the Library opens straight
                                                // onto it, the same deep-link shape the
                                                // player already uses for artists.
                                                onPlaylistClick = { playlist ->
                                                    noteLibraryReturn()
                                                    viewedPlaylistFromHome = playlist
                                                    selectedTab = 2
                                                },
                                                onOpenLiked = { selectedTab = 2 },
                                                onShowAllInLibrary = { selectedTab = 2 },
                                                onSpinClick = { source ->
                                                    spinSource = source
                                                    spinOpen = true
                                                },
                                                onProfileClick = onProfileClick,
                                                onSettingsClick = onNavigateToSettings,
                                                onDownloadsClick = onNavigateToDownloads,
                                                isDarkMode = isDarkMode,
                                                contentPadding = listContentPadding,
                                                viewModel = viewModel,
                                                excludedFolders = excludedFolders,
                                                manualScan = manualScan,
                                                videoMode = videoMode,
                                                onVideoModeToggle = onVideoModeToggle,
                                                showModeToggle = showModeToggle,
                                                modeToggleState = modeToggleState,
                                                listState = musicHomeScrollState
                                            )
                                        } else {
                                            // Music Mode: Show original content. The first load
                                            // renders the real screen with placeholders in the
                                            // data-backed sections rather than a full-screen
                                            // spinner - the top bar, titles and nav have
                                            // nothing to wait for.
                                            val classicPlaylists by viewModel.userPlaylists.collectAsState()
                                            val classicRecentAlbums by viewModel.recentAlbums.collectAsState()
                                            val classicLiked by viewModel.likedSongs.collectAsState()
                                            val classicTopArtists by viewModel.topArtists.collectAsState()
                                            val classicArtistPhotos by viewModel.artistPhotos.collectAsState()
                                            val classicOnline by viewModel.isOnline.collectAsState()
                                            val classicReadyOffline by viewModel.readyOffline.collectAsState()
                                            val classicDownloads by viewModel.downloadedSongs.collectAsState()
                                            // The cache changes as a side effect of
                                            // listening, so it is re-read each time the
                                            // connection goes, not held from earlier.
                                            LaunchedEffect(classicOnline) {
                                                if (!classicOnline) viewModel.refreshReadyOffline()
                                            }
                                            val classicOfflineSongs = remember(classicDownloads, classicReadyOffline) {
                                                (classicDownloads + classicReadyOffline.songs).distinctBy { it.id }
                                            }
                                            YourMixContent(
                                                playerViewModel = playerViewModel,
                                                likedCount = classicLiked.size,
                                                // Straight onto the likes, as the Library's
                                                // own Liked row opens them.
                                                onLikedClick = {
                                                    noteLibraryReturn()
                                                    viewedPlaylistFromHome = com.sentup.android.data.PlaylistDisplayItem(
                                                        context.getString(R.string.liked_songs), "LM", "You", classicLiked.size, null
                                                    )
                                                    selectedTab = 2
                                                },
                                                topArtists = classicTopArtists,
                                                artistPhotos = classicArtistPhotos,
                                                onArtistClick = { artist -> viewModel.requestArtistPage(artist.name) },
                                                isOffline = !classicOnline,
                                                offlineSongs = classicOfflineSongs,
                                                onOfflineSongClick = { song ->
                                                    playerViewModel.playQueue(classicOfflineSongs, song)
                                                    showPlayerSheet = true
                                                },
                                                recentAlbums = classicRecentAlbums,
                                                onRecentAlbumClick = { album ->
                                                    // A device album has a page of its own in
                                                    // the Library, opened by name.
                                                    if (album.source == com.sentup.android.data.SongSource.LOCAL) {
                                                        noteLibraryReturn()
                                                        viewedAlbumFromPlayer = album.title
                                                        selectedTab = 2
                                                        return@YourMixContent
                                                    }
                                                    scope.launch {
                                                        val page = viewModel.resolveRecentAlbum(album)
                                                        // The resolve can take a network round
                                                        // trip; if the user has moved on, a late
                                                        // jump to Library would yank them away.
                                                        if (selectedTab != 0 || videoMode) return@launch
                                                        if (page != null) {
                                                            noteLibraryReturn()
                                                            viewedPlaylistFromHome = page
                                                            selectedTab = 2
                                                        } else {
                                                            val queue = viewModel.recentAlbumQueue(album)
                                                            if (queue.isNotEmpty()) {
                                                                playerViewModel.playQueue(queue)
                                                                showPlayerSheet = true
                                                            }
                                                        }
                                                    }
                                                },
                                                songs = songs,
                                                isInitialLoading = isLoading && songs.isEmpty(),
                                                recentlyPlayed = recentlyPlayed,
                                                playlists = classicPlaylists,
                                                // The same Library hand-off Spotlight uses.
                                                onPlaylistClick = { playlist ->
                                                    noteLibraryReturn()
                                                    viewedPlaylistFromHome = playlist
                                                    selectedTab = 2
                                                },
                                                onRecentClick = { song ->
                                                    // Resume from the history rail: the
                                                    // recents are the queue, not the mix.
                                                    playerViewModel.playQueue(recentlyPlayed, song)
                                                    showPlayerSheet = true
                                                },
                                                onShowAllInLibrary = { selectedTab = 2 },
                                                onSpinClick = {
                                                    spinSource = SpinSource.Mix
                                                    spinOpen = true
                                                },
                                                onSongLongPress = { song -> songOptionsTarget = song },
                                                onSongClick = { song ->
                                                    playerViewModel.playQueue(songs, song)
                                                    showPlayerSheet = true
                                                },
                                                onPlayClick = {
                                                    if (songs.isNotEmpty()) {
                                                        playerViewModel.playQueue(songs)
                                                        showPlayerSheet = true
                                                    }
                                                },
                                                onProfileClick = onProfileClick,
                                                onSettingsClick = onNavigateToSettings,
                                                onDownloadsClick = onNavigateToDownloads,
                                                isDarkMode = isDarkMode,
                                                contentPadding = listContentPadding,
                                                viewModel = viewModel,
                                                excludedFolders = excludedFolders,
                                                manualScan = manualScan,
                                                videoMode = videoMode,
                                                onVideoModeToggle = onVideoModeToggle,
                                                showModeToggle = showModeToggle,
                                                modeToggleState = modeToggleState,
                                                listState = musicHomeScrollState
                                            )
                                        }
                                    }
                                ) { committedByGesture ->
                                    SpinOverlay(
                                        open = spinOpen,
                                        committedByGesture = committedByGesture,
                                        initialSource = spinSource,
                                        mix = songs,
                                        recent = recentlyPlayed,
                                        viewModel = viewModel,
                                        contentPadding = listContentPadding,
                                        onPlay = { queue, start ->
                                            playerViewModel.playQueue(queue, start)
                                            showPlayerSheet = true
                                        },
                                        onSongLongPress = { song -> songOptionsTarget = song },
                                        onBack = { spinOpen = false }
                                    )
                                }
                            }
                        }
                    }
                    1 -> if (videoMode && localOnly) {
                        com.sentup.android.ui.components.LocalOnlyNotice(
                            subtitle = stringResource(R.string.local_only_video_search_subtitle),
                            onOpenSettings = onNavigateToSettings
                        )
                    } else SearchContent(
                        songs = songs,
                        onSongClick = { song ->
                            // Fallback: Pass all songs to enable Next/Previous navigation
                            playerViewModel.playQueue(songs, song)
                            showPlayerSheet = true
                        },
                        onPlayQueue = { songList, song ->
                            // Use the visible song list (YouTube results or filtered local songs)
                            playerViewModel.playQueue(songList, song)
                            showPlayerSheet = true
                        },
                        // Wired explicitly even though the parameter has a
                        // default: the default plays in order, and search
                        // opens artists and playlists whose Shuffle buttons
                        // would otherwise have quietly stopped shuffling here
                        // while working everywhere else.
                        onShuffleQueue = { songList ->
                            playerViewModel.playQueueShuffled(songList)
                            showPlayerSheet = true
                        },
                        onPlayRadio = { song ->
                            playerViewModel.playSongRadio(song)
                            showPlayerSheet = true
                        },
                        onVideoClick = { video ->
                            // Navigate to video player screen
                            onNavigateToVideoPlayer(video)
                        },
                        onPlayVideoQueue = onPlayVideoQueue,
                        onEnqueueVideo = onEnqueueVideo,
                        onProfileClick = onProfileClick,
                        onOpenChannel = onOpenChannel,
                        onSongLongPress = { song -> songOptionsTarget = song },
                        onEnqueueSong = { song, playNext ->
                            if (playNext) playerViewModel.playNext(song)
                            else playerViewModel.addToQueue(song)
                        },
                        contentPadding = listContentPadding,
                        viewModel = viewModel,
                        isDarkMode = isDarkMode,
                        videoMode = videoMode,
                        localOnly = localOnly,
                        requestInitialFocus = !hasExpandedVideoPlayer && !showPlayerSheet,
                        listState = if (videoMode) videoSearchScrollState else musicSearchScrollState
                    )
                    2 -> {
                        if (videoMode && localOnly) {
                            com.sentup.android.ui.components.LocalOnlyNotice(
                                subtitle = stringResource(R.string.local_only_subscriptions_subtitle),
                                onOpenSettings = onNavigateToSettings
                            )
                        } else if (videoMode) {
                            com.sentup.android.ui.video.SubscriptionsContent(
                                viewModel = viewModel,
                                onEnqueueVideo = onEnqueueVideo,
                                onVideoClick = { video ->
                                    onNavigateToVideoPlayer(video)
                                },
                                onLoginClick = { showAuthDialog = true },
                                onOpenChannel = onOpenChannel,
                                onManageSubscriptions = onNavigateToSubscriptions,
                                contentPadding = listContentPadding,
                                feedListState = subscriptionsScrollState
                            )
                        } else {
                            LibraryContent(
                                songs = songs,
                                isLocalLibrary = loadLocalSongs,
                                onDownloadsClick = onNavigateToDownloads,
                                onSongClick = { song: Song ->
                                    // Pass all songs to enable Next/Previous navigation
                                    playerViewModel.playQueue(songs, song)
                                    showPlayerSheet = true
                                },
                                onPlaylistClick = { playlist: com.sentup.android.data.PlaylistDisplayItem ->
                                    // Optional: navigate to playlist detail or handled by parent
                                },
                                onPlayQueue = { songs: List<Song>, selectedSong: Song? ->
                                    playerViewModel.playQueue(songs, selectedSong)
                                    showPlayerSheet = true
                                },
                                onShuffleQueue = { songs: List<Song> ->
                                    playerViewModel.playQueueShuffled(songs)
                                    showPlayerSheet = true
                                },
                                contentPadding = listContentPadding,
                                viewModel = viewModel,
                                isDarkMode = isDarkMode,
                                initialArtist = viewedArtistFromPlayer,
                                onInitialArtistConsumed = { viewedArtistFromPlayer = null },
                                initialReturnToCaller = libraryReturnTab != null,
                                onReturnToCaller = {
                                    // The tab slide reads as going back; the
                                    // player keeps whatever state it was in.
                                    libraryReturnTab?.let { selectedTab = it }
                                    libraryReturnTab = null
                                },
                                initialPlaylist = viewedPlaylistFromHome,
                                onInitialPlaylistConsumed = { viewedPlaylistFromHome = null },
                                initialAlbum = viewedAlbumFromPlayer,
                                onInitialAlbumConsumed = { viewedAlbumFromPlayer = null },
                                onStatsClick = onNavigateToStats,
                                onOpenChannel = onOpenChannel,
                                onSongLongPress = { song -> songOptionsTarget = song },
                                onEnqueueSong = { song, playNext ->
                                    if (playNext) playerViewModel.playNext(song)
                                    else playerViewModel.addToQueue(song)
                                },
                                allSongsListState = musicLibraryScrollState
                            )
                        }
                    }
                    3 -> {
                        // Video mode only: Library (playlists, Watch Later,
                        // liked videos, watch history)
                        if (videoMode && localOnly) {
                            // With YouTube switched off the video Library is
                            // the device's own videos: playlists, history and
                            // the pinned account feeds all need a network the
                            // user has turned off, and the notice that used to
                            // stand here explained that while offering nothing.
                            val deviceVideos by viewModel.deviceVideos.collectAsState()
                            val deviceFolders by viewModel.deviceVideoFolders.collectAsState()
                            val isDeviceLoading by viewModel.isDeviceVideosLoading.collectAsState()
                            val hasScanned by viewModel.hasScannedDeviceVideos.collectAsState()
                            val videoAccess =
                                com.sentup.android.ui.components.rememberVideoMediaAccessState()
                            var deviceSort by remember {
                                mutableStateOf(com.sentup.android.data.LocalVideoSort.RECENT)
                            }
                            LaunchedEffect(videoAccess.access) {
                                if (videoAccess.isReadable) viewModel.loadDeviceVideos()
                            }
                            com.sentup.android.ui.video.DeviceVideosScreen(
                                videos = deviceVideos,
                                folders = deviceFolders,
                                access = videoAccess.access,
                                isLoading = isDeviceLoading,
                                hasScanned = hasScanned,
                                sort = deviceSort,
                                onSortChange = { deviceSort = it },
                                onRequestAccess = { videoAccess.launchRequest() },
                                onRefresh = { viewModel.loadDeviceVideos() },
                                onPlay = onPlayDeviceVideos,
                                // The top inset is spent by the header below,
                                // so the list must not pay it a second time.
                                contentPadding = PaddingValues(
                                    bottom = listContentPadding.calculateBottomPadding()
                                ),
                                topBar = {
                                    Column {
                                    com.sentup.android.ui.video.DownloadedContentLibraryCard(
                                        onClick = onNavigateToDownloads,
                                        modifier = Modifier.padding(
                                            start = 16.dp, end = 16.dp,
                                            top = listContentPadding.calculateTopPadding()
                                        )
                                    )
                                    Text(
                                        text = stringResource(R.string.dv_on_this_device),
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(
                                            start = 20.dp,
                                            end = 20.dp,
                                            top = 12.dp,
                                            bottom = 8.dp
                                        )
                                    )
                                    }
                                },
                                folderTopBar = { title, onBack ->
                                    com.sentup.android.ui.video.DeviceFolderTopBar(
                                        title = title,
                                        onBack = onBack,
                                        modifier = Modifier.padding(
                                            top = listContentPadding.calculateTopPadding()
                                        )
                                    )
                                }
                            )
                        } else if (videoMode) {
                            com.sentup.android.ui.video.VideoLibraryContent(
                                onOpenDownloads = onNavigateToDownloads,
                                viewModel = viewModel,
                                onOpenChannel = onOpenChannel,
                                onVideoClick = { video ->
                                    onNavigateToVideoPlayer(video)
                                },
                                onPlayQueue = onPlayVideoQueue,
                                onPlayDeviceVideos = onPlayDeviceVideos,
                                onLoginClick = { showAuthDialog = true },
                                contentPadding = listContentPadding,
                                onEnqueueVideo = onEnqueueVideo,
                                initialPlaylist = viewedVideoPlaylistFromHome,
                                onInitialPlaylistConsumed = { viewedVideoPlaylistFromHome = null },
                                rootListState = videoLibraryScrollState
                            )
                        }
                    }
                    4 -> if (videoMode) {
                        // Explore: shelves built from the feeds this device
                        // already has, arranged by rules the user writes.
                        //
                        // **No localOnly gate.** [judgement] Local Only exists
                        // to keep YouTube off the device, and every source an
                        // Explore list can read is local by then - the cached
                        // subscriptions feed, this device's watch history, this
                        // device's playlists. A notice saying "there is no
                        // YouTube here" over a screen assembled entirely from
                        // local data would be the switch misreporting itself.
                        // Explore's card long-press opens the shared options
                        // sheet rather than enqueueing straight away, like the
                        // other five surfaces. The screen offers one callback
                        // channel (onEnqueueVideo) and uses it only as the
                        // cards' long-press handler, so the sheet is hosted
                        // here and the channel is routed to it.
                        var exploreOptionsVideo by remember {
                            mutableStateOf<com.sentup.android.data.VideoItem?>(null)
                        }
                        com.sentup.android.ui.explore.ExploreContent(
                            onVideoClick = { video -> onNavigateToVideoPlayer(video) },
                            onOpenChannel = onOpenChannel,
                            contentPadding = listContentPadding,
                            onEnqueueVideo = { video, _ -> exploreOptionsVideo = video },
                            onPlayShelf = onPlayVideoQueue?.let { playQueue ->
                                { videos: List<com.sentup.android.data.VideoItem>, title: String ->
                                    playQueue(
                                        com.sentup.android.data.VideoQueue(
                                            videos = videos,
                                            index = 0,
                                            title = title
                                        )
                                    )
                                }
                            },
                            listState = exploreScrollState
                        )
                        exploreOptionsVideo?.let { video ->
                            com.sentup.android.ui.video.VideoOptionsSheetHost(
                                video = video,
                                viewModel = viewModel,
                                onDismiss = { exploreOptionsVideo = null },
                                onEnqueue = { playNext -> onEnqueueVideo?.invoke(video, playNext) },
                                onOpenChannel = onOpenChannel
                            )
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(stringResource(R.string.permission_required_title), color = MaterialTheme.colorScheme.onBackground)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { permissionState.launchPermissionRequest() }) {
                        Text(stringResource(R.string.action_grant_permission))
                    }
                }
            }
        }
        
        // Playlist Loading Overlay
        if (isPlaylistLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {}, // Block clicks
                contentAlignment = Alignment.Center
            ) {
                LoadingIndicator(
                    modifier = Modifier.size(48.dp),
                    color = Color.White
                )
            }
        }
        
        // Video Home carries the account avatar, downloads and Settings, so
        // hiding that destination took the only route to them - including the
        // switch that puts Home back. The bar becomes part of the shell in
        // that configuration, above every destination rather than inside one.
        // Opaque, because the lists underneath scroll beneath it.
        // Fades rather than popping, on the same curve the mode cross-fade
        // uses, so leaving video mode does not blink the chrome out a frame
        // before the content underneath it changes.
        androidx.compose.animation.AnimatedVisibility(
            visible = videoShellTopBar,
            enter = androidx.compose.animation.fadeIn(
                androidx.compose.animation.core.tween(220)
            ),
            exit = androidx.compose.animation.fadeOut(
                androidx.compose.animation.core.tween(180)
            ),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(backgroundColor)
                    .padding(top = statusBarInset)
                    .padding(start = contentStartInset, end = contentEndInset)
            ) {
                com.sentup.android.ui.video.VideoTopBarSection(
                    onProfileClick = onProfileClick,
                    onSettingsClick = onNavigateToSettings,
                    onDownloadsClick = onNavigateToDownloads,
                    // The bell reports on the Home feed and its sheet belongs
                    // to that screen, so the shell copy leaves it out.
                    onNotificationsClick = onProfileClick,
                    viewModel = viewModel,
                    videoMode = videoMode,
                    onVideoModeToggle = onVideoModeToggle,
                    showModeToggle = showModeToggle,
                    modeToggleState = modeToggleState,
                    showNotifications = false
                )
            }
        }

        // Both navigation variants use the same destinations and interaction
        // contract. Only their Material container and item presentation differ.
        val navBarHaptics = com.sentup.android.util.rememberSentupHaptics()
        val navTabs = if (videoMode) {
            videoHomeConfiguration.orderedVisibleDestinations.map { destination ->
                when (destination) {
                    VideoHomeDestination.HOME -> Triple(
                        destination.tabId,
                        stringResource(R.string.tab_home),
                        Pair(Icons.Rounded.Home, Icons.Outlined.Home)
                    )
                    VideoHomeDestination.SEARCH -> Triple(
                        destination.tabId,
                        stringResource(R.string.tab_search),
                        Pair(Icons.Filled.Search, Icons.Outlined.Search)
                    )
                    VideoHomeDestination.SUBSCRIPTIONS -> Triple(
                        destination.tabId,
                        stringResource(R.string.tab_subs),
                        Pair(Icons.Filled.Subscriptions, Icons.Outlined.Subscriptions)
                    )
                    VideoHomeDestination.LIBRARY -> Triple(
                        destination.tabId,
                        stringResource(R.string.tab_library),
                        Pair(Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary)
                    )
                    VideoHomeDestination.EXPLORE -> Triple(
                        destination.tabId,
                        stringResource(R.string.tab_explore),
                        Pair(Icons.Rounded.AutoAwesome, Icons.Rounded.AutoAwesome)
                    )
                }
            }
        } else listOf(
            Triple(0, stringResource(R.string.tab_home), Pair(Icons.Rounded.Home, Icons.Outlined.Home)),
            Triple(1, stringResource(R.string.tab_search), Pair(Icons.Filled.Search, Icons.Outlined.Search)),
            Triple(2, stringResource(R.string.tab_library), Pair(Icons.Filled.LibraryMusic, Icons.Outlined.LibraryMusic))
        )
        val selectNavTab: (Int) -> Unit = { index ->
            if (selectedTab == index) {
                if (currentTabScrollState.canScrollBackward) {
                    navBarHaptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    scope.launch { currentTabScrollState.scrollToTop() }
                }
            } else {
                navBarHaptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                selectedTab = index
            }
        }
        // Slide a thumb along the bar to pick a tab; the highlight follows the
        // finger and the tab opens on release. A release on the tab already
        // open does nothing - unlike a tap, it is not a scroll-to-top request.
        val navScrub = com.sentup.android.ui.components.rememberNavBarScrubState(
            tabIds = navTabs.map { it.first },
            onHoverChange = { navBarHaptics.performHapticFeedback(HapticFeedbackType.SegmentTick) },
            onCommit = { target ->
                if (target != selectedTab) {
                    navBarHaptics.performHapticFeedback(HapticFeedbackType.ContextClick)
                    selectedTab = target
                }
            }
        )

        if (useRail) {
            val railTabs = navTabs.map { (index, label, icons) ->
                HomeNavTab(index, label, icons.first, icons.second)
            }
            if (nonExpressiveNavigationBar) {
                HomeStandardRail(
                    tabs = railTabs,
                    selectedTab = selectedTab,
                    scrub = navScrub,
                    onSelect = selectNavTab,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            } else {
                HomeFloatingRail(
                    tabs = railTabs,
                    selectedTab = selectedTab,
                    scrub = navScrub,
                    onSelect = selectNavTab,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = safeStartInset + 12.dp)
                )
            }
        } else if (nonExpressiveNavigationBar) {
            NavigationBar(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navBarScrub(navScrub)
            ) {
                navTabs.forEach { (index, label, icons) ->
                    val selected = (navScrub.hoveredTab ?: selectedTab) == index
                    val (filledIcon, outlinedIcon) = icons
                    NavigationBarItem(
                        modifier = Modifier
                            .weight(1f)
                            .navBarScrubItem(navScrub, index),
                        selected = selected,
                        onClick = { selectNavTab(index) },
                        icon = {
                            Icon(
                                imageVector = if (selected) filledIcon else outlinedIcon,
                                contentDescription = label
                            )
                        },
                        // The standard bar names every tab unless the
                        // user asked otherwise.
                        label = if (homeNavigation.tabLabels == com.sentup.android.data.NavTabLabels.NEVER) {
                            null
                        } else {
                            { Text(label) }
                        },
                        alwaysShowLabel =
                            homeNavigation.tabLabels != com.sentup.android.data.NavTabLabels.SELECTED
                    )
                }
            }
        } else {
            HorizontalFloatingToolbar(
                expanded = true,
                scrollBehavior = if (barHidesOnScroll) floatingToolbarScrollBehavior else null,
                // The capsule stays where it was; only its depth joins the
                // page's language: the same tinted shadow the cards cast and
                // a light hairline along its top edge, so the bar reads as
                // one more object floating over the wash rather than UI
                // furniture printed on top of it.
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 20.dp)
                    // The bar's own exit slides its surface away, but the
                    // shadow and the specular hairline are painted by this
                    // wrapper, which never moves — so as the bar hid, an empty
                    // capsule outline stayed on screen (user: "o çerçeve hala
                    // duruyor"). Fading the wrapper with the same offset the
                    // bar exits with takes the frame down with it.
                    .graphicsLayer {
                        alpha = 1f - kotlin.math.abs(floatingToolbarState.offset.coerceIn(-1f, 1f))
                    }
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(percent = 50),
                        ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.30f),
                        spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    )
                    .sentupSpecularEdge(RoundedCornerShape(percent = 50))
                    .navBarScrub(navScrub),
                content = {
                    navTabs.forEach { (index, label, icons) ->
                        val selected = (navScrub.hoveredTab ?: selectedTab) == index
                        val (filledIcon, outlinedIcon) = icons

                        // fastSpatialSpec: snappy expressive motion — StiffnessLow
                        // springs took ~1s to settle and felt sluggish here.
                        val animatedPadding by androidx.compose.animation.core.animateDpAsState(
                            targetValue = if (selected) 20.dp else 12.dp,
                            animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                            label = "padding"
                        )

                        val animatedContainerColor by androidx.compose.animation.animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
                            label = "containerColor"
                        )

                        val animatedContentColor by androidx.compose.animation.animateColorAsState(
                            targetValue = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                            animationSpec = androidx.compose.animation.core.tween(durationMillis = 200),
                            label = "contentColor"
                        )

                        Surface(
                            selected = selected,
                            onClick = { selectNavTab(index) },
                            shape = CircleShape,
                            color = animatedContainerColor,
                            contentColor = animatedContentColor,
                            modifier = Modifier
                                .height(48.dp)
                                .navBarScrubItem(navScrub, index)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = animatedPadding)
                                    .animateContentSize(
                                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                                    ),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (selected) filledIcon else outlinedIcon,
                                    contentDescription = label,
                                    modifier = Modifier.size(24.dp)
                                )
                                androidx.compose.animation.AnimatedVisibility(
                                    // The floating bar names only the open
                                    // tab unless the user asked otherwise.
                                    visible = when (homeNavigation.tabLabels) {
                                        com.sentup.android.data.NavTabLabels.ALWAYS -> true
                                        com.sentup.android.data.NavTabLabels.NEVER -> false
                                        else -> selected
                                    },
                                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.expandHorizontally(
                                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                                    ),
                                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.shrinkHorizontally(
                                        animationSpec = MaterialTheme.motionScheme.fastSpatialSpec()
                                    )
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = label,
                                            style = MaterialTheme.typography.labelLarge,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            )
        }

        // Published for the video overlay, which is drawn above the NavHost and
        // therefore above this player. Cleared on dispose as well as on
        // collapse: leaving Home while the player is open drops the remembered
        // flag here, and a stale true would keep the video bar hidden for the
        // rest of the session.
        DisposableEffect(showPlayerSheet) {
            playerViewModel.setPlayerExpanded(showPlayerSheet)
            onDispose { playerViewModel.setPlayerExpanded(false) }
        }

        // A handover from an overlay above the NavHost ("Listen as music")
        // cannot open this sheet itself, because the state that owns it lives
        // here. It asks instead, and the ask is only honoured while Home is
        // composed - the flow has no replay, so a request made from another
        // route is dropped rather than springing the player open on return.
        LaunchedEffect(playerViewModel) {
            playerViewModel.playerExpandRequests.collect {
                if (playerViewModel.currentSong.value != null) showPlayerSheet = true
            }
        }

        // Expandable Player (Mini <-> Full Screen)
        ExpandablePlayer(
            isExpanded = showPlayerSheet,
            onExpandChange = { showPlayerSheet = it },
            currentSong = currentSong,
            isPlaying = isPlaying,
            isBuffering = isBuffering,
            playWhenReady = playWhenReady,
            progress = progressFraction,
            duration = playerViewModel.duration.collectAsState().value,
            onPlayPauseClick = { playerViewModel.togglePlayPause() },
            onNextClick = { playerViewModel.skipToNext() },
            viewModel = playerViewModel,
            ambientBackground = ambientBackground,
            artworkColors = playerArtworkColors,
            playerStyle = playerStyle,
            onPlayerStyleChange = onPlayerStyleChange,
            collapsedBottomSpacing = miniPlayerCollapsedSpacing,
            collapsedFollowOffsetPx = miniPlayerFollowOffsetPx,
            // Read in the lambda, so the bubble animating does not recompose
            // this screen.
            collapsedBubbleFraction = { miniBubbleFraction.value.coerceIn(0f, 1f) },
            collapsedStartInset = contentStartInset,
            collapsedEndInset = contentEndInset,
            collapsedMaxWidth = if (useRail) MINI_PLAYER_MAX_WIDE_WIDTH else androidx.compose.ui.unit.Dp.Unspecified,
            onWatchAsVideo = onWatchAsVideo,
            onArtistClick = { artistName ->
                // Collapse the player and open the artist inside the music
                // Library tab. The origin tab is remembered so back returns to
                // it rather than stranding the user in the Library, and
                // noteLibraryReturn() runs before goToTab because it reads
                // videoMode to decide whether back has a tab to go to.
                //
                // goToTab rather than an assignment is load-bearing: the music
                // player outlives a switch to video mode - its mini bar stays,
                // and expanding it from there is an ordinary thing to do - and
                // in that mode tab 2 is the Subscriptions feed, so this has to
                // switch modes and then land, in that order. See goToTab.
                noteLibraryReturn()
                showPlayerSheet = false
                viewedArtistFromPlayer = artistName
                goToTab(2) // Music Library tab
            },
            onAlbumClick = { albumName ->
                noteLibraryReturn()
                showPlayerSheet = false
                viewedAlbumFromPlayer = albumName
                goToTab(2)
            },
            onOpenAlbum = { albumItem ->
                // A stream's album opens the playlist detail the same way a
                // Spotlight shelf does: hand it over and switch tab.
                noteLibraryReturn()
                showPlayerSheet = false
                viewedPlaylistFromHome = albumItem
                goToTab(2)
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
        
        // Update available indicator. Anchored bottom-start above the floating
        // overlays (shared bottomOverlayInset) rather than the top bar, where it
        // used to sit on top of the settings/profile icons.
        androidx.compose.animation.AnimatedVisibility(
            visible = updateResult is UpdateResult.UpdateAvailable,
            enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(
                initialScale = 0.8f,
                animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
            ),
            exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut(
                targetScale = 0.8f
            ),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 20.dp + contentStartInset, bottom = bottomOverlayInset + 8.dp)
        ) {
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    // The pill is the way back to the dialog it replaced as
                    // the first thing an update shows.
                    .clickable { showUpdatePrompt = true },
                color = MaterialTheme.colorScheme.primaryContainer,
                tonalElevation = 4.dp,
                shadowElevation = 4.dp,
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.update_badge_version, latestVersion),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // The Sentüp profile, over everything on Home including the mini
        // player and the navigation bar: it is a full screen, hosted here
        // rather than on a route because the switcher sheet it hands to
        // lives in this composable.
        androidx.compose.animation.AnimatedVisibility(
            visible = showProfileScreen,
            enter = androidx.compose.animation.fadeIn() +
                androidx.compose.animation.slideInVertically(
                    animationSpec = MaterialTheme.motionScheme.defaultSpatialSpec()
                ) { it / 10 },
            exit = androidx.compose.animation.fadeOut() +
                androidx.compose.animation.slideOutVertically { it / 10 }
        ) {
            com.sentup.android.ui.profile.SentupProfileScreen(
                onBack = { showProfileScreen = false },
                onSwitchProfile = {
                    showProfileScreen = false
                    showAccountSheet = true
                },
                onOpenStats = {
                    showProfileScreen = false
                    onNavigateToStats()
                }
            )
        }
    }
    }

    // Auth Dialog
    // Long-press a song anywhere in music mode: play next, add to queue, add to
    // a playlist, like, download. The only route to addToQueue in the app.
    songOptionsTarget?.let { song ->
        com.sentup.android.ui.player.SongOptionsSheet(
            song = song,
            viewModel = playerViewModel,
            onDismiss = { songOptionsTarget = null },
            // The same hand-off the player already uses for an artist: the
            // artist page lives inside the Library tab rather than on a route,
            // so it is opened by handing the name over and switching tab.
            // Nothing else passed this, which left the sheet's own artist row
            // unreachable from every surface in the app.
            onArtistClick = { artist ->
                noteLibraryReturn()
                viewedArtistFromPlayer = artist
                goToTab(2)
            },
            onOpenAlbum = { albumItem ->
                noteLibraryReturn()
                viewedPlaylistFromHome = albumItem
                goToTab(2)
            },
            // Only for songs a feed could have recommended. A file on this
            // device was not recommended by anything, so "stop recommending
            // it" would be a control with nothing to act on - and the local
            // library is deliberately never filtered by this store.
            onNotInterested = if (song.source == com.sentup.android.data.SongSource.YOUTUBE) {
                { viewModel.hideSong(song) }
            } else null,
            onBlockArtist = if (song.source == com.sentup.android.data.SongSource.YOUTUBE) {
                { viewModel.blockArtist(song) }
            } else null
        )
    }

    if (showAuthDialog) {
        com.sentup.android.ui.auth.YouTubeAuthDialog(
            onDismiss = {
                showAuthDialog = false
                addAuthAsNewProfile = false
            },
            onAuthSuccess = {
                showAuthDialog = false
                addAuthAsNewProfile = false
                // Refresh login state, account info and the feeds so the UI
                // reflects the account immediately instead of after a restart
                viewModel.checkYouTubeConnection(force = true)
                if (videoMode) {
                    if (videoHomeConfiguration.recommendationsEnabled) {
                        viewModel.loadTrendingVideos()
                    } else {
                        viewModel.loadSubscriptions(force = true)
                        viewModel.loadSubscriptionMix(force = true)
                    }
                    viewModel.loadYouTubeHistory()
                } else {
                    viewModel.loadYouTubeRecommendations(force = true)
                }
            },
            addAsNewProfile = addAuthAsNewProfile
        )
    }

    // The profile switcher. Replaces the old account sheet, whose "Switch
    // account" logged you out and made you sign in again - with a roster of
    // stored sessions, switching is instant and needs no network at all.
    if (showAccountSheet) {
        com.sentup.android.ui.account.AccountSwitcherSheet(
            onDismiss = { showAccountSheet = false },
            onAddYouTubeAccount = {
                // Google's login page auto-continues as whoever the WebView jar
                // already holds, so adding a second account without clearing it
                // silently hands back the first one. Stored sessions live in
                // EncryptedSharedPreferences and are untouched by this.
                val cookieManager = android.webkit.CookieManager.getInstance()
                cookieManager.removeAllCookies {
                    // removeAllCookies is asynchronous. Opening the login page
                    // before its callback can immediately recapture the old
                    // account and make "Add account" appear to sign it out.
                    cookieManager.flush()
                    addAuthAsNewProfile = true
                    showAuthDialog = true
                }
            },
            onReconnectProfile = {
                // Same jar wipe as adding an account, for the same reason: the
                // login page auto-continues as whoever WebView still holds, so
                // without this a reconnect can silently store the *other*
                // account's session against this profile.
                val cookieManager = android.webkit.CookieManager.getInstance()
                cookieManager.removeAllCookies {
                    cookieManager.flush()
                    // False, unlike the add path: the profile already exists
                    // and is now active, so the session has to land on it
                    // rather than in a new row. addYouTubeProfileAndSwitch
                    // cannot match it either - the datasyncId is only known
                    // after an authenticated call, so it would mint a fresh id
                    // and orphan this profile's subscriptions and blocklist.
                    addAuthAsNewProfile = false
                    showAuthDialog = true
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun YourMixContent(
    songs: List<Song>,
    /**
     * First load with nothing to show yet. The screen still renders in full;
     * only the sections that are actually waiting on data are replaced by
     * placeholders of the same size.
     */
    isInitialLoading: Boolean = false,
    /** Local play history for the "Jump back in" rail. Empty for a new user. */
    recentlyPlayed: List<Song> = emptyList(),
    onRecentClick: (Song) -> Unit = {},
    /** Albums behind the play history, newest first. */
    recentAlbums: List<RecentAlbum>,
    /** Required: a shelf card left unwired is a dead card. */
    onRecentAlbumClick: (RecentAlbum) -> Unit,
    /** The merged local, saved and account playlists, as Spotlight shows them. */
    playlists: List<com.sentup.android.data.PlaylistDisplayItem>,
    /** Required: a shelf card left unwired is a dead card. */
    onPlaylistClick: (com.sentup.android.data.PlaylistDisplayItem) -> Unit,
    /** Liked songs, for the tile leading Your playlists; 0 hides it. */
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
    /**
     * Carousels on a vertically-scrolling page need a way to reach every item
     * without scrolling sideways; this backs the arrow button in each header.
     * The Library tab is that page - it renders the same [songs] list
     * vertically, plus its own recently-played rail.
     */
    onShowAllInLibrary: () -> Unit = {},
    onSongClick: (Song) -> Unit,
    onSongLongPress: ((Song) -> Unit)? = null,
    onPlayClick: () -> Unit,
    /** Opens Spin, the wheel over this Home. Required: an entry left unwired is a dead card. */
    onSpinClick: () -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDownloadsClick: () -> Unit = {},
    isDarkMode: Boolean,
    contentPadding: PaddingValues,
    viewModel: HomeViewModel,
    excludedFolders: Set<String> = emptySet(),
    manualScan: Boolean = false,
    videoMode: Boolean = false,
    onVideoModeToggle: (Boolean) -> Unit = {},
    showModeToggle: Boolean = true,
    modeToggleState: MusicVideoToggleState = rememberMusicVideoToggleState(videoMode),
    /**
     * The music player, threaded down to the feed. Without it the feed's
     * player-backed shelves (the mood rail, the resume hero, the long-press
     * popout) hide themselves rather than offer a dead tap.
     */
    playerViewModel: PlayerViewModel? = null,
    /** Hoisted by HomeScreen: survives tab switches, reachable by the nav bar. */
    listState: LazyListState = rememberLazyListState()
) {
    // The classic Home's layout was rebuilt end to end: the greeting is the
    // page's large title, the mix is one hero panel, and every collection is
    // a shelf in the app's own surface language. The old sections - the mix
    // card, the circle collage, the caption rails - are gone rather than
    // restyled. This call is the whole home now; the signature above stays
    // the contract HomeScreen's shell wires into.
    SentupHomeFeed(
        playerViewModel = playerViewModel,
        songs = songs,
        isInitialLoading = isInitialLoading,
        recentlyPlayed = recentlyPlayed,
        onRecentClick = onRecentClick,
        recentAlbums = recentAlbums,
        onRecentAlbumClick = onRecentAlbumClick,
        playlists = playlists,
        onPlaylistClick = onPlaylistClick,
        likedCount = likedCount,
        onLikedClick = onLikedClick,
        topArtists = topArtists,
        artistPhotos = artistPhotos,
        onArtistClick = onArtistClick,
        isOffline = isOffline,
        offlineSongs = offlineSongs,
        onOfflineSongClick = onOfflineSongClick,
        onShowAllInLibrary = onShowAllInLibrary,
        onSongClick = onSongClick,
        onSongLongPress = onSongLongPress,
        onPlayClick = onPlayClick,
        onSpinClick = onSpinClick,
        onProfileClick = onProfileClick,
        onSettingsClick = onSettingsClick,
        onDownloadsClick = onDownloadsClick,
        contentPadding = contentPadding,
        viewModel = viewModel,
        excludedFolders = excludedFolders,
        manualScan = manualScan,
        videoMode = videoMode,
        onVideoModeToggle = onVideoModeToggle,
        showModeToggle = showModeToggle,
        listState = listState
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TopBarSection(
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDownloadsClick: () -> Unit = {},
    isDarkMode: Boolean,
    viewModel: HomeViewModel,
    videoMode: Boolean = false,
    onVideoModeToggle: (Boolean) -> Unit = {},
    showModeToggle: Boolean = true,
    modeToggleState: MusicVideoToggleState = rememberMusicVideoToggleState(videoMode)
) {
    // The bar's buttons wear the secondary container, not a grey surface
    // tone: on the tinted page a grey button read as part of the background.
    val iconColor = MaterialTheme.colorScheme.onSecondaryContainer
    val containerColor = MaterialTheme.colorScheme.secondaryContainer
    val context = androidx.compose.ui.platform.LocalContext.current

    val userAvatar by viewModel.userAvatar.collectAsState()
    val profileLabel = stringResource(R.string.cd_profile)
    val downloadingIds by viewModel.downloadingIds.collectAsState()
    val incognito by com.sentup.android.data.IncognitoMode.enabled(context).collectAsState()
    val topBarOptions = com.sentup.android.ui.components.rememberHomeTopBarOptions()
    val showDownloads = topBarOptions.alwaysShowDownloads || downloadingIds.isNotEmpty()

    // A Box, not a spaced Row: the mode switch sits in the true centre of
    // the bar whatever is either side of it, so it does not shift when the
    // buttons on the right change between music and video.
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        // Profile avatar. Tap opens the switcher; long-press flips straight
        // back to the last profile, which is the whole point of a switcher for
        // someone bouncing between two accounts. Incognito shows as a badge on
        // the avatar itself rather than a chip beside it, so the bar keeps its
        // shape whether history is paused or not.
        val haptics = com.sentup.android.util.rememberSentupHaptics()
        val accountSwitcher = remember(context) {
            com.sentup.android.data.AccountSwitcher(context)
        }
        val isSwitching by accountSwitcher.switching.collectAsState()
        val openAccountSwitcher = com.sentup.android.ui.profile.LocalOpenAccountSwitcher.current
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    // No circle clip and no disc behind it: a Sentüp avatar has
                    // an outline of its own (a flower, a clover, a cookie),
                    // and a round frame cut its points off. The picture is
                    // drawn to its own shape; only the press ripple is round.
                    .combinedClickable(
                        interactionSource = remember {
                            androidx.compose.foundation.interaction.MutableInteractionSource()
                        },
                        indication = androidx.compose.material3.ripple(bounded = false, radius = 22.dp),
                        onClick = onProfileClick,
                        onLongClick = {
                            // Holding the picture is the account switcher, now
                            // that a tap opens the profile. Where no shell
                            // provides it, the old flip to the last profile
                            // stands, and only when there is one to flip to: a
                            // long-press that does nothing reads as broken.
                            if (openAccountSwitcher != null) {
                                haptics.performHapticFeedback(
                                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                                )
                                openAccountSwitcher()
                            } else if (accountSwitcher.quickSwitchTarget() != null) {
                                haptics.performHapticFeedback(
                                    androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
                                )
                                accountSwitcher.quickSwitch()
                            }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                // The active profile's own picture: its photo, its Sentüp
                // avatar, or the account's. A device-only profile used to
                // be a grey person icon here.
                com.sentup.android.ui.profile.ActiveProfileAvatar(
                    modifier = Modifier
                        .fillMaxSize()
                        .semantics { contentDescription = profileLabel },
                    accountAvatarUrl = userAvatar
                )
                // Progress rides on the avatar rather than blocking the screen: the
                // switch itself is instant, but the feeds behind it are refetching,
                // and the status belongs where the user just tapped.
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
            // The history-paused mark. Tonal fill so it reads as "a mode is on"
            // without taking over the bar; the badge itself carries the label,
            // so a screen reader announces it alongside the profile button.
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
        
        // Right side icons with shape morphing.
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Music/Video mode switch: one button the size of its neighbours
            // that changes shape, colour and icon with the mode, instead of
            // a two-part switch twice their width. [trial October 2026] Can
            // be hidden from Settings (Home Screen Mode Toggle).
            if (showModeToggle) {
                com.sentup.android.ui.components.ModeMorphButton(
                    videoMode = videoMode,
                    onVideoModeChange = onVideoModeToggle
                )
            }

            // Downloads appear here only while something is downloading: the
            // button is a status, and the downloads themselves live in the
            // Library. A permanent button for a place visited now and then
            // was one more grey circle in the row.
            // Customization can keep it there for good. It shares an unspaced
            // row with Settings because it brings its own gap (see
            // TopBarDownloadsButton).
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.sentup.android.ui.components.TopBarDownloadsButton(
                    visible = showDownloads,
                    downloading = downloadingIds.isNotEmpty(),
                    onClick = onDownloadsClick,
                    containerColor = containerColor,
                    contentColor = iconColor
                )
                IconButton(
                    onClick = onSettingsClick,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = containerColor,
                        contentColor = iconColor
                    ),
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = stringResource(R.string.cd_settings),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // A greeting beside the profile picture, so the bar has something to
        // say whether or not the mode switch is on it. It gives way to the
        // buttons: the end padding is their width, with or without the switch.
        if (topBarOptions.showGreeting) com.sentup.android.ui.components.HomeGreeting(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(
                    start = 56.dp,
                    // One 44dp button and its gap for each button drawn.
                    end = 56.dp * (1 + (if (showModeToggle) 1 else 0) +
                        (if (showDownloads) 1 else 0))
                )
        )
    }
}
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SearchContent(
    songs: List<Song>,
    onSongClick: (Song) -> Unit,
    onPlayQueue: (List<Song>, Song?) -> Unit = { _, song -> song?.let { onSongClick(it) } },
    /**
     * Play a collection with shuffle mode on. Defaults to playing it in
     * order rather than to a one-time shuffled copy: a host that has not
     * wired a player cannot turn shuffle on, and silently playing a
     * shuffled copy would leave the player's own toggle disagreeing with
     * the queue - the disagreement this parameter exists to end.
     */
    onShuffleQueue: (List<Song>) -> Unit = { songs -> onPlayQueue(songs, null) },
    onPlayRadio: (Song) -> Unit = { song -> onPlayQueue(listOf(song), song) },
    onVideoClick: (VideoItem) -> Unit = {},
    /**
     * Play a playlist found through search as a queue. Null falls back to
     * [onVideoClick], which plays the tapped video alone.
     */
    onPlayVideoQueue: ((com.sentup.android.data.VideoQueue) -> Unit)? = null,
    onEnqueueVideo: ((com.sentup.android.data.VideoItem, Boolean) -> Unit)? = null,
    /**
     * Queue a song from a playlist opened through search. Hoisted like
     * [onSongLongPress]: the queue lives in HomeScreen's PlayerViewModel.
     * Null disarms the queue swipe action. The boolean is play-next.
     */
    onEnqueueSong: ((Song, Boolean) -> Unit)? = null,
    onProfileClick: () -> Unit = {},
    /** Open a creator's page, from a Channels result or the long-press sheet. */
    onOpenChannel: (String) -> Unit = {},
    contentPadding: PaddingValues,
    viewModel: HomeViewModel,
    isDarkMode: Boolean,
    videoMode: Boolean = false,
    localOnly: Boolean = false,
    onSongLongPress: ((Song) -> Unit)? = null,
    requestInitialFocus: Boolean = true,
    /** Hoisted by HomeScreen: survives tab switches, reachable by the nav bar. */
    listState: LazyListState = rememberLazyListState()
) {
    var viewedPlaylist by remember { mutableStateOf<com.sentup.android.data.PlaylistDisplayItem?>(null) }
    var viewedArtist by remember { mutableStateOf<com.sentup.android.data.ArtistItem?>(null) }
    var viewedVideoPlaylist by remember { mutableStateOf<com.sentup.android.data.VideoPlaylist?>(null) }

    val currentScreen = when {
        viewedVideoPlaylist != null -> "videoPlaylist"
        viewedPlaylist != null -> "playlist"
        viewedArtist != null -> "artist"
        else -> "search"
    }

    // Expressive motion physics for screen pushes/pops
    val searchNavSpatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<androidx.compose.ui.unit.IntOffset>()
    val searchNavEffectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    // Playlist/album is the deepest layer (search -> artist -> album), so it
    // pops first; backing out of an album returns to the artist page. Only the
    // last step out is previewed: popping an album back to the artist reveals
    // another child, not the search screen underneath.
    com.sentup.android.ui.components.PredictiveBackStack(
        childOpen = currentScreen != "search",
        onBack = {
            when {
                viewedVideoPlaylist != null -> viewedVideoPlaylist = null
                viewedPlaylist != null -> viewedPlaylist = null
                viewedArtist != null -> viewedArtist = null
            }
        },
        previewable = listOfNotNull(viewedVideoPlaylist, viewedPlaylist, viewedArtist).size == 1,
        background = {
            com.sentup.android.ui.search.SearchScreen(
                songs = songs,
                onSongClick = onSongClick,
                onPlayQueue = onPlayQueue,
                onPlayRadio = onPlayRadio,
                onVideoClick = onVideoClick,
                onArtistClick = { artistItem -> viewedArtist = artistItem },
                onAlbumClick = { albumItem -> viewedPlaylist = albumItem },
                onPlaylistClick = { playlistItem -> viewedPlaylist = playlistItem },
                onVideoPlaylistClick = { videoPlaylist ->
                    viewModel.loadPlaylistVideos(videoPlaylist.playlistId)
                    viewedVideoPlaylist = videoPlaylist
                },
                onProfileClick = onProfileClick,
                onEnqueueVideo = onEnqueueVideo,
                onOpenChannel = onOpenChannel,
                onSongLongPress = onSongLongPress,
                contentPadding = contentPadding,
                viewModel = viewModel,
                isDarkMode = isDarkMode,
                videoMode = videoMode,
                localOnly = localOnly,
                requestInitialFocus = requestInitialFocus,
                listState = listState
            )
        }
    ) { committedByGesture ->
    androidx.compose.animation.AnimatedContent(
        targetState = currentScreen,
        label = "SearchNav",
        transitionSpec = {
            val content = when {
                // The finger already performed this exit.
                committedByGesture ->
                    androidx.compose.animation.EnterTransition.None togetherWith
                        androidx.compose.animation.ExitTransition.None
                targetState != "search" ->
                    // Push (Going deeper)
                    (androidx.compose.animation.slideInHorizontally(animationSpec = searchNavSpatialSpec) { width -> width } +
                            androidx.compose.animation.fadeIn(animationSpec = searchNavEffectsSpec)) togetherWith
                            (androidx.compose.animation.slideOutHorizontally(animationSpec = searchNavSpatialSpec) { width -> -width / 3 } +
                                    androidx.compose.animation.fadeOut(animationSpec = searchNavEffectsSpec))
                else ->
                    // Pop (Going back). The search screen is underneath and
                    // staying put, so the child carries the whole movement.
                    androidx.compose.animation.fadeIn(animationSpec = searchNavEffectsSpec) togetherWith
                            (androidx.compose.animation.slideOutHorizontally(animationSpec = searchNavSpatialSpec) { width -> width } +
                                    androidx.compose.animation.fadeOut(animationSpec = searchNavEffectsSpec))
            }
            // The search state of this layer is empty, so the default
            // SizeTransform would animate the container between nothing and
            // full screen and clip the child to it on the way.
            content using androidx.compose.animation.SizeTransform(clip = false) { _, _ ->
                androidx.compose.animation.core.snap()
            }
        }
    ) { screen ->
        when (screen) {
            "artist" -> {
                 viewedArtist?.let { artistItem ->
                    com.sentup.android.ui.artist.ArtistScreen(
                        artistName = artistItem.name,
                        artistId = artistItem.id,
                        songs = emptyList(), // We let the screen fetch songs via viewModel
                        onBack = { viewedArtist = null },
                        onPlayQueue = onPlayQueue,
                        onShuffleQueue = onShuffleQueue,
                        onSongClick = onSongClick,
                        onAlbumClick = { album, albumSongs ->
                             // Optional: Handle playing album from artist screen
                             onPlayQueue(albumSongs, null)
                        },
                        onOpenAlbum = { albumItem -> viewedPlaylist = albumItem },
                        viewModel = viewModel,
                        onSongLongPress = onSongLongPress,
                        onOpenChannel = onOpenChannel,
                        // Another artist replaces this one in place, as in the
                        // Library; a playlist stacks over it and pops back here.
                        onOpenArtist = { name, id ->
                            viewedArtist = com.sentup.android.data.ArtistItem(id = id, name = name)
                        },
                        onOpenPlaylist = { playlistItem -> viewedPlaylist = playlistItem }
                    )
                }
            }

            "playlist" -> {
                 viewedPlaylist?.let { playlist ->
                     com.sentup.android.ui.library.PlaylistDetailScreen(
                        playlist = playlist,
                        onBack = { viewedPlaylist = null },
                        onPlayQueue = onPlayQueue,
                        onShuffleQueue = onShuffleQueue,
                        viewModel = viewModel,
                        onSongLongPress = onSongLongPress,
                        onEnqueueSong = onEnqueueSong,
                        // Search opens other people's playlists; the import
                        // flow belongs to the Library's local ones.
                        onAddSongsRequest = null,
                        onSearchSongsRequest = null
                    )
                }
            }

            "videoPlaylist" -> {
                viewedVideoPlaylist?.let { playlist ->
                    com.sentup.android.ui.video.VideoPlaylistDetail(
                        playlist = playlist,
                        viewModel = viewModel,
                        onVideoClick = onVideoClick,
                        onBack = { viewedVideoPlaylist = null },
                        contentPadding = contentPadding,
                        // Search results aren't the user's own playlists
                        allowRemove = false,
                        onPlayQueue = onPlayVideoQueue,
                        onEnqueueVideo = onEnqueueVideo
                    )
                }
            }
            // Search lives underneath now; this layer is empty over it, and
            // full size so both states measure the same.
            else -> Spacer(Modifier.fillMaxSize())
        }
    }
    }
}

/** How long an app left running goes before a return to it checks for an update again. */
private const val UPDATE_RECHECK_MS = 6L * 60L * 60L * 1000L
