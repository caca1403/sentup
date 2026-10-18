package com.sentup.android.ui.video
import androidx.compose.ui.res.stringResource
import com.sentup.android.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotInterested
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.sentup.android.ui.components.ExpressivePullToRefresh
import com.sentup.android.ui.components.SkeletonBox
import com.sentup.android.ui.components.SkeletonList
import com.sentup.android.ui.components.VideoCardSkeleton
import com.sentup.android.ui.components.rememberSkeletonAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import coil.compose.AsyncImage
import com.sentup.android.ui.components.VideoThumbnail
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupGroupHeader
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage
import com.sentup.android.data.DownloadedVideo
import com.sentup.android.data.ShortsItem
import com.sentup.android.data.VideoItem
import com.sentup.android.ui.components.MusicVideoToggle
import com.sentup.android.ui.components.MusicVideoToggleState
import com.sentup.android.ui.components.rememberMusicVideoToggleState
import com.sentup.android.ui.home.HomeViewModel

/**
 * Video Home Screen Content for Video Mode.
 * Displays trending/recommended videos with thumbnails, channel names, views, etc.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VideoHomeContent(
    videos: List<VideoItem>,
    isLoading: Boolean,
    isOffline: Boolean = false,
    downloadedVideos: List<DownloadedVideo> = emptyList(),
    onVideoClick: (VideoItem) -> Unit,
    onDownloadedVideoClick: (DownloadedVideo) -> Unit = {},
    onEnqueueVideo: ((VideoItem, Boolean) -> Unit)? = null,
    /** Open a video's creator, from the long-press sheet. */
    onOpenChannel: ((String) -> Unit)? = null,
    shortsEnabled: Boolean = false,
    shorts: List<ShortsItem> = emptyList(),
    onShortClick: (Int) -> Unit = {},
    /**
     * Open Shorts that are not the Home shelf's - a Short from the
     * notification inbox. Required, so the inbox cannot ship with Shorts
     * that do nothing on a tap.
     */
    onOpenShorts: (List<ShortsItem>, Int) -> Unit,
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDownloadsClick: () -> Unit = {},
    onRefresh: () -> Unit,
    recommendationsEnabled: Boolean = true,
    compact: Boolean = false,
    isDarkMode: Boolean,
    contentPadding: PaddingValues,
    viewModel: HomeViewModel,
    videoMode: Boolean = true,
    onVideoModeToggle: (Boolean) -> Unit = {},
    showModeToggle: Boolean = true,
    modeToggleState: MusicVideoToggleState = rememberMusicVideoToggleState(videoMode),
    // Hoisted by HomeScreen so the position survives a tab switch and the nav
    // bar can send it back to the top on a re-tap. Defaulted for previews and
    // any caller that does not care.
    listState: LazyListState = rememberLazyListState(),
    /**
     * Whether a card plays a silent preview when the feed is rested on it.
     * Off by default here as well as in preferences: a caller that has not
     * thought about it gets the feed that does not move on its own.
     */
    inlinePreviews: Boolean = false
) {
    // Null when the setting is off, and then the cards read a null local and
    // draw exactly what they drew before this existed.
    val previewController = rememberInlinePreviewController(enabled = inlinePreviews)
    if (previewController != null) {
        // The dwell timer only runs while the list is still. Written here
        // rather than read per card, so one state read feeds every card.
        previewController.isListSettled = !listState.isScrollInProgress
    }

    // Anything that opens over the feed stops the preview under it. The video
    // player and the Shorts overlay are drawn above the NavHost, so this screen
    // is never paused and the card's own visibility still reads as on screen -
    // nothing else here would notice that a full-screen player is covering it.
    val openVideo: (VideoItem) -> Unit = { video ->
        previewController?.release()
        onVideoClick(video)
    }
    val openShort: (Int) -> Unit = { index ->
        previewController?.release()
        onShortClick(index)
    }

    val isYouTubeConnected by viewModel.isYouTubeConnected.collectAsState()
    val localSubscriptions by viewModel.localSubscriptions.collectAsState()
    val subscribedChannels by viewModel.subscribedChannels.collectAsState()
    val showOfflineDownloads = isOffline && downloadedVideos.isNotEmpty()
    val isShortsLoading by viewModel.isShortsLoading.collectAsState()
    val listLayout = LocalVideoListLayout.current
    val shortsFeedFailed by viewModel.shortsFeedFailed.collectAsState()

    // Notifications sheet state
    var showNotificationsSheet by remember { mutableStateOf(false) }
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
    val notifications by viewModel.notifications.collectAsState()
    val isNotificationsLoading by viewModel.isNotificationsLoading.collectAsState()
    val hiddenNotificationCount by viewModel.hiddenNotificationCount.collectAsState()

    // Options sheet (long-press on a video card)
    var saveTargetVideo by remember { mutableStateOf<VideoItem?>(null) }

    fun onVideoLongPress(video: VideoItem) {
        saveTargetVideo = video
    }

    // Animation state for staggered entry
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        isVisible = true
    }

    if (showNotificationsSheet) {
        NotificationsSheet(
            notifications = notifications,
            isLoading = isNotificationsLoading,
            hiddenCount = hiddenNotificationCount,
            onHide = viewModel::hideNotification,
            onRestoreHidden = viewModel::restoreHiddenNotifications,
            onRefresh = { viewModel.loadNotifications(force = true) },
            onNotificationClick = { notification ->
                when (val target = notification.target) {
                    is com.sentup.android.data.NotificationTarget.Video -> {
                        showNotificationsSheet = false
                        openVideo(
                            VideoItem(
                                videoId = target.videoId,
                                title = notification.message,
                                channelName = "",
                                channelIconUrl = notification.channelAvatarUrl,
                                thumbnailUrl = notification.videoThumbnailUrl,
                                duration = 0L,
                                viewCount = ""
                            )
                        )
                    }
                    // A Short opens in the Shorts player on its own, the way a
                    // Short does everywhere else in the app.
                    is com.sentup.android.data.NotificationTarget.Short -> {
                        showNotificationsSheet = false
                        previewController?.release()
                        onOpenShorts(listOf(com.sentup.android.data.ShortsItem(videoId = target.videoId)), 0)
                    }
                    // A community post opens here, pinned above its comments.
                    // The inbox stays up for the moment the post takes to
                    // arrive, so the tap is never followed by an empty screen;
                    // only a post that cannot be read leaves for its web page.
                    is com.sentup.android.data.NotificationTarget.Post -> {
                        viewModel.openNotificationPost(target.detailParams) { opened ->
                            showNotificationsSheet = false
                            if (!opened) target.url?.let(uriHandler::openUri)
                        }
                    }
                    // Through the in-app link handler, which keeps anything Sentüp
                    // can open (a channel) and hands the rest to the YouTube
                    // app or the browser.
                    is com.sentup.android.data.NotificationTarget.Link -> {
                        showNotificationsSheet = false
                        uriHandler.openUri(target.url)
                    }
                    null -> Unit
                }
            },
            onDismiss = { showNotificationsSheet = false }
        )
    }

    saveTargetVideo?.let { video ->
        VideoOptionsSheetHost(
            video = video,
            viewModel = viewModel,
            onDismiss = { saveTargetVideo = null },
            onEnqueue = onEnqueueVideo?.let { enqueue -> { next -> enqueue(video, next) } },
            onOpenChannel = onOpenChannel
        )
    }

    // Community posts from followed channels, scattered between the videos.
    // What the Video feed settings let through. Posts that are switched off
    // are not asked for either, since each batch costs channel browses.
    val feedFilter = rememberHomeFeedFilter()
    val allFeedPosts by viewModel.feedPosts.collectAsState()
    val feedPosts = if (feedFilter.showPosts) allFeedPosts else emptyList()

    // Compact Home lays the feed out as one continuous card rather than a stack
    // of them: the rows butt together and the inset hairlines do the
    // separating. Two things stop the chaining: a run that a community post
    // interrupts is not one object, and a two-column grid has no single run to
    // round off - each row there holds two cards side by side.
    val chainedFeed = compact && !showOfflineDownloads && feedPosts.isEmpty() &&
        listLayout != com.sentup.android.data.ThemePreferences.VIDEO_LAYOUT_GRID
    // Which videos go either side of the Shorts shelf, worked out here rather
    // than in the list: the run's edges decide which rows round off, and a
    // composable cannot be called from the lazy scope.
    // Feed chips: a filter over the rows the feed already holds - the same
    // isLive signal and watch history the feed settings act on, no new data.
    // 0 = everything, 1 = live only, 2 = hide what has been watched.
    var feedChip by rememberSaveable { mutableStateOf(0) }
    val chipContext = LocalContext.current
    val chipWatchHistory by remember(chipContext) {
        com.sentup.android.data.VideoHistoryRepository(chipContext)
    }.history.collectAsState()
    val feedVideos = feedFilter.apply(videos).let { base ->
        when (feedChip) {
            1 -> base.filter { it.isLive }
            2 -> {
                val watched = chipWatchHistory.mapTo(HashSet()) { it.videoId }
                base.filterNot { it.videoId in watched }
            }
            else -> base
        }
    }
    val leadingVideos = if (!shortsEnabled) feedVideos else feedVideos.take(2)
    val trailingVideos = if (!shortsEnabled) emptyList() else feedVideos.drop(2)
    val leadingRowEdges = rememberVideoRowEdges(leadingVideos, chainedFeed)
    val trailingRowEdges = rememberVideoRowEdges(trailingVideos, chainedFeed)
    val postPhotoViewer = remember { mutableStateOf<Pair<List<String>, Int>?>(null) }
    FeedPostOverlays(viewModel, postPhotoViewer, onOpenChannel)
    FeedPostDemand(
        viewModel = viewModel,
        listState = listState,
        videoCount = videos.size,
        postCount = allFeedPosts.size,
        enabled = !showOfflineDownloads && feedFilter.showPosts
    )
    val feedPostCard: @Composable (com.sentup.android.data.ChannelPost) -> Unit = { post ->
        FeedPostCard(post, viewModel, openVideo, onOpenChannel, postPhotoViewer)
    }

    ExpressivePullToRefresh(
        // Only let the pull-to-refresh spinner represent a refresh over existing
        // content. The empty-feed case shows its own centered indicator below, and
        // driving both off the same flag renders two spinners at once.
        isRefreshing = isLoading && (videos.isNotEmpty() || showOfflineDownloads),
        onRefresh = {
            viewModel.refreshFeedPosts()
            onRefresh()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        if (isLoading && videos.isEmpty() && !showOfflineDownloads) {
            // Skeleton rather than a lone spinner: the placeholders mirror the
            // cards that will land, so the swap does not move the page.
            SkeletonList(
                count = 3,
                spacing = FEED_CARD_GAP,
                modifier = Modifier
                    .fillMaxSize()
                    .sentupPage()
            ) { alpha ->
                VideoCardSkeleton(alpha = alpha)
            }
        } else {
            val isRecommendationsLoadingMore by viewModel.isVideoLoadingMore.collectAsState()
            val isMixLoadingMore by viewModel.isSubscriptionMixLoadingMore.collectAsState()
            val isLoadingMore = if (recommendationsEnabled) isRecommendationsLoadingMore else isMixLoadingMore
            // Read through updated state: the effect below is keyed on the list
            // alone and outlives a change of this setting.
            val currentRecommendationsEnabled by rememberUpdatedState(recommendationsEnabled)

            // Endless feed: ask for the next page whenever the last visible
            // item is within 5 of the end. The ViewModel guards against
            // duplicate/exhausted loads, so firing on every scroll frame is fine.
            LaunchedEffect(listState) {
                snapshotFlow {
                    val info = listState.layoutInfo
                    (info.visibleItemsInfo.lastOrNull()?.index ?: -1) to info.totalItemsCount
                }.collect { (lastVisible, totalCount) ->
                    if (totalCount > 0 && lastVisible >= totalCount - 5) {
                        if (currentRecommendationsEnabled) {
                            viewModel.loadMoreTrendingVideos()
                        } else {
                            viewModel.loadMoreSubscriptionMix()
                        }
                    }
                }
            }

            androidx.compose.runtime.CompositionLocalProvider(
                LocalInlinePreview provides previewController
            ) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .sentupPage(),
                contentPadding = contentPadding,
                // Chained rows share one card, so the only thing between them is
                // the inset hairline each one draws. Cards that stand on the
                // page keep a gap wide enough to read as separate objects.
                verticalArrangement = Arrangement.spacedBy(
                    if (chainedFeed) 0.dp else FEED_CARD_GAP
                )
            ) {
                // Top Bar
                item {
                    VideoTopBarSection(
                        onProfileClick = onProfileClick,
                        onSettingsClick = onSettingsClick,
                        onDownloadsClick = onDownloadsClick,
                        onNotificationsClick = {
                            if (isYouTubeConnected) {
                                viewModel.loadNotifications(force = true)
                                showNotificationsSheet = true
                            } else {
                                onProfileClick()
                            }
                        },
                        viewModel = viewModel,
                        videoMode = videoMode,
                        onVideoModeToggle = onVideoModeToggle,
                        showModeToggle = showModeToggle,
                        modeToggleState = modeToggleState
                    )
                }

                // Filter chips, in the same pill language the Subscriptions
                // filter row uses. The selected one wears the palette accent;
                // a tap filters the rows below in place.
                if (videoMode && !showOfflineDownloads) item(key = "feed_chips") {
                    LazyRow(
                        modifier = Modifier.padding(vertical = 2.dp),
                        contentPadding = PaddingValues(horizontal = SentupGroup.inset),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item {
                            SentupActionPill(
                                label = stringResource(R.string.filter_all),
                                onClick = { feedChip = 0 },
                                filled = feedChip == 0
                            )
                        }
                        item {
                            SentupActionPill(
                                label = stringResource(R.string.topic_live),
                                onClick = { feedChip = 1 },
                                filled = feedChip == 1
                            )
                        }
                        item {
                            SentupActionPill(
                                label = stringResource(R.string.vh_chip_unwatched),
                                onClick = { feedChip = 2 },
                                filled = feedChip == 2
                            )
                        }
                    }
                }
                
                // Section title - changes based on whether user is logged in
                // or recommendations are disabled (in which case subscriptions are shown).
                if (showOfflineDownloads || recommendationsEnabled || videos.isNotEmpty()) item {
                    AnimatedVisibility(
                        visible = isVisible,
                        enter = fadeIn() + slideInVertically(
                            initialOffsetY = { it / 2 },
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        )
                    ) {
                        SentupGroupHeader(
                            text = when {
                                showOfflineDownloads -> stringResource(R.string.vh_available_offline)
                                !recommendationsEnabled -> stringResource(R.string.vh_subscription_mix)
                                isYouTubeConnected -> stringResource(R.string.vh_recommended_for_you)
                                else -> stringResource(R.string.vh_trending_videos)
                            }
                        )
                    }
                }
                
                if (showOfflineDownloads) {
                    items(downloadedVideos, key = { "download_${it.id}" }) { downloaded ->
                        VideoCard(
                            video = downloaded.asOfflineVideoItem(),
                            onClick = { onDownloadedVideoClick(downloaded) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    // Video cards, with the Shorts shelf slotted in after the
                    // first two like the YouTube home feed. Each row is told
                    // which end of the run it sits at so the run reads as one
                    // card; unchained, every card stands on its own.
                    // Posts go in whichever run is the feed proper: after
                    // the Shorts shelf when there is one, never between the
                    // two videos above it.
                    videoListItemsWithPosts(
                        videos = leadingVideos,
                        posts = if (shortsEnabled) emptyList() else feedPosts,
                        layout = listLayout,
                        keyPrefix = "lead_",
                        post = feedPostCard
                    ) { video, cell ->
                        androidx.compose.runtime.CompositionLocalProvider(
                            LocalVideoRowEdge provides leadingRowEdges[video.videoId]
                        ) {
                            VideoCard(
                                video = video,
                                onClick = { openVideo(video) },
                                onLongClick = { onVideoLongPress(video) },
                                onOpenChannel = onOpenChannel,
                                modifier = cell
                            )
                        }
                    }

                    if (shortsEnabled) {
                        item(key = "shorts_shelf") {
                            ShortsShelf(
                                shorts = shorts,
                                onShortClick = openShort,
                                // The Short becomes a plain VideoItem: the
                                // sheet's actions all key on the video id.
                                onShortLongClick = { short -> saveTargetVideo = short.toVideoItem() },
                                isLoading = isShortsLoading,
                                failed = shortsFeedFailed,
                                onRefresh = { viewModel.loadShortsFeed(force = true) },
                                // Rows butt together in a chained feed, so the
                                // shelf carries its own gap to the run above and
                                // below it.
                                modifier = if (chainedFeed) {
                                    Modifier.padding(vertical = FEED_CARD_GAP)
                                } else {
                                    Modifier
                                }
                            )
                        }
                    }

                    // Pick up where you left off: part-watched videos carry their
                    // resume point on the feed's head rather than hiding it on
                    // the history page. The player already resumes on its own,
                    // so this shelf is an entrance, not a second mechanism.
                    if (videoMode) {
                        item(key = "resume_shelf") {
                            ResumeShelf(
                                refreshKey = videos.size,
                                onVideoClick = openVideo,
                                modifier = if (chainedFeed) {
                                    Modifier.padding(vertical = FEED_CARD_GAP)
                                } else {
                                    Modifier
                                }
                            )
                        }
                    }

                    videoListItemsWithPosts(
                        videos = trailingVideos,
                        posts = feedPosts,
                        layout = listLayout,
                        keyPrefix = "trail_",
                        post = feedPostCard
                    ) { video, cell ->
                        androidx.compose.runtime.CompositionLocalProvider(
                            LocalVideoRowEdge provides trailingRowEdges[video.videoId]
                        ) {
                            VideoCard(
                                video = video,
                                onClick = { openVideo(video) },
                                onLongClick = { onVideoLongPress(video) },
                                onOpenChannel = onOpenChannel,
                                modifier = cell
                            )
                        }
                    }
                }
                
                // Empty state
                if (videos.isEmpty() &&
                    !isLoading && !showOfflineDownloads && shorts.isEmpty()
                ) {
                    val hasSubscriptions = isYouTubeConnected || subscribedChannels.isNotEmpty() || localSubscriptions.isNotEmpty()

                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = if (recommendationsEnabled) {
                                        Icons.Rounded.VideoLibrary
                                    } else {
                                        Icons.Rounded.Subscriptions
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = when {
                                        !recommendationsEnabled ->
                                            if (hasSubscriptions) {
                                                stringResource(R.string.vh_subscription_mix_empty)
                                            } else {
                                                stringResource(R.string.vh_no_subscriptions)
                                            }
                                        isOffline -> stringResource(R.string.vh_youre_offline)
                                        else -> stringResource(R.string.vh_no_videos_found)
                                    },
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!recommendationsEnabled && !hasSubscriptions) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.vh_no_subscriptions_sub),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else if (isOffline) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.vh_downloaded_hint),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // An empty feed is usually a failed fetch that
                                // already gave up quietly; one button beats
                                // making the user pull-to-refresh to find out.
                                Spacer(modifier = Modifier.height(12.dp))
                                TextButton(onClick = onRefresh) {
                                    Text(stringResource(R.string.action_retry))
                                }
                            }
                        }
                    }
                }
                
                // Load-more footer
                if (isLoadingMore) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(
                                modifier = Modifier.size(36.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
            }
        }
    }
}

private fun DownloadedVideo.asOfflineVideoItem() = VideoItem(
    videoId = id,
    title = title,
    channelName = channelName,
    thumbnailUrl = thumbnailUrl,
    duration = durationMs / 1000L,
    viewCount = quality.orEmpty()
)

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun VideoTopBarSection(
    onProfileClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    viewModel: HomeViewModel,
    videoMode: Boolean = true,
    onVideoModeToggle: (Boolean) -> Unit = {},
    showModeToggle: Boolean = true,
    modeToggleState: MusicVideoToggleState = rememberMusicVideoToggleState(videoMode),
    /**
     * The bell belongs to the Home feed it reports on, and its sheet is owned
     * by [VideoHomeContent]. The shell copy of this bar - the one that appears
     * when Home is hidden - leaves it out rather than drawing a button with
     * nowhere to go.
     */
    showNotifications: Boolean = true
) {
    val surfaceColor = MaterialTheme.colorScheme.surfaceContainer
    // The bar's buttons wear the secondary container, not a grey surface
    // tone: on the tinted page a grey button read as part of the background.
    val iconColor = MaterialTheme.colorScheme.onSecondaryContainer
    val containerColor = MaterialTheme.colorScheme.secondaryContainer
    
    val userAvatar by viewModel.userAvatar.collectAsState()
    val downloadingIds by viewModel.downloadingIds.collectAsState()
    val context = LocalContext.current
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
        // Profile avatar. Incognito shows as a badge on the avatar itself
        // rather than a chip beside it, so the bar keeps its shape whether
        // history is paused or not.
        Box(modifier = Modifier.align(Alignment.CenterStart)) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(surfaceColor)
                    .clickable(onClick = onProfileClick),
                contentAlignment = Alignment.Center
            ) {
                if (userAvatar != null) {
                    AsyncImage(
                        model = userAvatar,
                        contentDescription = stringResource(R.string.cd_profile),
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = stringResource(R.string.cd_profile),
                        tint = iconColor,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
            // The history-paused mark. Tonal fill so it reads as "a mode is on"
            // without taking over the bar; the badge itself carries the label,
            // so a screen reader announces it alongside the profile button.
            androidx.compose.animation.AnimatedVisibility(
                visible = incognito,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
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

        // Right side icons.
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

            // Notifications Button
            if (showNotifications) IconButton(
                onClick = onNotificationsClick,
                shapes = IconButtonDefaults.shapes(),
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = containerColor,
                    contentColor = iconColor
                ),
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Notifications,
                    contentDescription = stringResource(R.string.settings_notifications),
                    modifier = Modifier.size(22.dp)
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
                        (if (showNotifications) 1 else 0) +
                        (if (showDownloads) 1 else 0))
                )
        )
    }
}

/**
 * Shorts shelf: the group header every shelf on every screen uses, over the
 * same MultiBrowseCarousel treatment the music home uses for albums — masked
 * items that morph between sizes as they scroll. Tapping a card opens the
 * fullscreen Shorts player.
 */
@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalMaterial3ExpressiveApi::class
)
@Composable
fun ShortsShelf(
    shorts: List<ShortsItem>,
    onShortClick: (Int) -> Unit,
    /** Long-press on a shelf card: the same options sheet the video cards get. */
    onShortLongClick: (ShortsItem) -> Unit,
    isLoading: Boolean,
    failed: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val refreshLabel = stringResource(R.string.shorts_refresh)
    Column(modifier = modifier.fillMaxWidth()) {
        // The header is [SentupGroupHeader], like the feed's own title and every
        // shelf in the music home. The refresh button keeps the card edge rather
        // than the header's text edge, so it lines up with the covers below it.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SentupGroupHeader(
                text = stringResource(R.string.sp_shorts),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = onRefresh,
                enabled = !isLoading,
                modifier = Modifier
                    .padding(end = SentupGroup.inset)
                    .semantics { contentDescription = refreshLabel }
            ) {
                if (isLoading) {
                    LoadingIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Icon(Icons.Rounded.Refresh, contentDescription = null)
                }
            }
        }

        if (failed || (shorts.isEmpty() && !isLoading)) {
            // One mark, one sentence, one way out - the retry does the same
            // thing the header's refresh button did, in the place the user is
            // already looking.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SentupGroup.inset, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = if (failed) Icons.Rounded.CloudOff else Icons.Rounded.VideoLibrary,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(if (failed) R.string.shorts_refresh_failed else R.string.shorts_feed_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (failed) {
                    TextButton(onClick = onRefresh) {
                        Text(stringResource(R.string.action_retry))
                    }
                }
            }
        }
        if (shorts.isEmpty()) {
            // Fetching: three tiles in the shelf's own shape, so the carousel
            // replaces them without moving anything.
            if (isLoading) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SentupGroup.inset),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val skeletonAlpha = rememberSkeletonAlpha()
                    repeat(3) {
                        SkeletonBox(
                            modifier = Modifier
                                .weight(1f)
                                .height(220.dp),
                            shape = RoundedCornerShape(SentupRadii.tile),
                            alpha = skeletonAlpha
                        )
                    }
                }
            }
            return@Column
        }

        val carouselState = rememberCarouselState { shorts.size }
        HorizontalMultiBrowseCarousel(
            state = carouselState,
            preferredItemWidth = 160.dp,
            itemSpacing = 8.dp,
            contentPadding = PaddingValues(horizontal = SentupGroup.inset),
            // Bounded by its own box. The carousel grows an item as it reaches
            // the centre, and nothing stops that drawing from leaving the band
            // it was measured in - which is a Short's cover riding up over the
            // header above it. The band is what moves; the cover never does.
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .clipToBounds()
        ) { index ->
            val item = shorts[index]
            Box(
                modifier = Modifier
                    .maskClip(RoundedCornerShape(SentupRadii.tile))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .combinedClickable(
                        onClick = { onShortClick(index) },
                        onLongClick = { onShortLongClick(item) }
                    )
            ) {
                AsyncImage(
                    model = item.portraitThumbnailUrl,
                    contentDescription = item.title.ifBlank { stringResource(R.string.sp_shorts) },
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(96.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f))
                            )
                        )
                )

                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    if (item.title.isNotBlank()) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (item.viewCount.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = item.viewCount,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f),
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/**
 * Where a row sits inside a run of rows that share one card.
 *
 * [Top] rounds its corners at the top only and [Middle] not at all, so a run of
 * rows reads as one continuous card rather than a stack of separate ones, and
 * the hairlines between them are inset rather than full-bleed. Null - the
 * default, and what every other surface in the app gets - means no chain: the
 * row is a card standing on the page on its own.
 *
 * A LazyColumn gives each row its own item, so a row cannot see its neighbours
 * and cannot round the ends of the group by itself. The Home feed knows the run
 * it is emitting, so it hands each row its place through this local rather than
 * the card having to guess.
 */
internal enum class VideoRowEdge { Top, Middle, Bottom, Both }

internal val LocalVideoRowEdge = staticCompositionLocalOf<VideoRowEdge?> { null }

/**
 * The gap between two floating cards. [com.sentup.android.ui.theme.SentupGroup]
 * fixes the inset and the group gap but not the gap between tiles, and 12dp is
 * what keeps two cards clearly separate objects without pushing the next one
 * off the screen.
 */
private val FEED_CARD_GAP = 12.dp

/**
 * Which end of the run each row sits at, keyed on the video id - the shape the
 * feed hands to [LocalVideoRowEdge].
 *
 * The keys follow the same de-duplication [videoListItemsWithPosts] applies, so
 * a feed that lists a video twice still rounds the ends of the run it actually
 * drew rather than the ones it was given.
 */
@Composable
private fun rememberVideoRowEdges(
    videos: List<VideoItem>,
    chained: Boolean
): Map<String, VideoRowEdge> = remember(videos, chained) {
    if (!chained) {
        emptyMap()
    } else {
        val unique = videos.distinctBy { it.videoId }
        unique.mapIndexed { index, video ->
            video.videoId to when {
                unique.size == 1 -> VideoRowEdge.Both
                index == 0 -> VideoRowEdge.Top
                index == unique.lastIndex -> VideoRowEdge.Bottom
                else -> VideoRowEdge.Middle
            }
        }.toMap()
    }
}

/**
 * Video Card component - one floating card: the cover on top, its text band
 * underneath, the two of them inside the same rounded surface.
 *
 * The cover no longer runs flush to the card's own edge. It sits inside the
 * card at [com.sentup.android.ui.theme.SentupGroup.dividerInset], the same x the
 * title and the row hairlines start at, which is what stops the cover's scrim
 * and duration badge from landing on the seam where the text band begins: they
 * are bounded by the cover's own [SentupRadii.tile] edge now, with the card's
 * surface between the cover and the text.
 *
 * [onLongClick] is optional; the home feed uses it for the save-to-playlist sheet.
 */
@Composable
fun VideoCard(
    video: VideoItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onLongClick: (() -> Unit)? = null,
    /** Opens the creator when the avatar is tapped, without invoking [onClick]. */
    onOpenChannel: ((String) -> Unit)? = null,
    /** Null follows the app-wide [LocalVideoListLayout]. */
    compact: Boolean? = null
) {
    val layout = when (compact) {
        true -> com.sentup.android.data.ThemePreferences.VIDEO_LAYOUT_COMPACT
        false -> com.sentup.android.data.ThemePreferences.VIDEO_LAYOUT_CARDS
        null -> LocalVideoListLayout.current
    }
    val edge = LocalVideoRowEdge.current
    // A collab card opens the list of credited channels rather than guessing
    // one; an ordinary card navigates as before. See videoChannelTap.
    val openChannel = videoChannelTap(video, onOpenChannel)

    // Only the ends of a chained run are rounded, and only a card standing on
    // the page casts a shadow: a row inside a group is flat (see [sentupCard]).
    val cardRadius = SentupRadii.card
    val shape = when (edge) {
        null, VideoRowEdge.Both -> RoundedCornerShape(cardRadius)
        VideoRowEdge.Top -> RoundedCornerShape(topStart = cardRadius, topEnd = cardRadius)
        VideoRowEdge.Bottom -> RoundedCornerShape(bottomStart = cardRadius, bottomEnd = cardRadius)
        VideoRowEdge.Middle -> RoundedCornerShape(0.dp)
    }

    PressableVideoCard(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        inChain = edge != null,
        onClick = onClick,
        onLongClick = onLongClick
    ) {
        if (layout == com.sentup.android.data.ThemePreferences.VIDEO_LAYOUT_COMPACT) {
            CompactVideoCardContent(video, openChannel, onLongClick)
        } else if (layout == com.sentup.android.data.ThemePreferences.VIDEO_LAYOUT_GRID) {
            GridVideoCardContent(video, openChannel)
        } else {
            StackedVideoCardContent(video, openChannel)
        }
    }
}

/**
 * The card's own surface and its touch feedback.
 *
 * **The press is a scale, not a ripple.** The row language already presses
 * inward - see [com.sentup.android.ui.theme.SentupRow] - and a ripple painted
 * across a photograph reads as a wash over the picture rather than as the card
 * being pressed. So the whole card takes the tap: 0.97 for 90ms with a trace of
 * fade, which is a transform and costs nothing.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PressableVideoCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(SentupRadii.card),
    inChain: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "cardPress"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (pressed) 0.94f else 1f
            }
            // [sentupCard] clips as it fills, so everything the content draws -
            // scrim, badge, progress - is bounded by the card's own edge.
            .sentupCard(shape = shape, elevation = if (inChain) 0 else 1, tinted = !inChain)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick
            )
    ) {
        content()
    }
}

/** The feed's card: cover above, text band under it. See [VideoCard]. */
@Composable
private fun StackedVideoCardContent(video: VideoItem, openChannel: (() -> Unit)?) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tileShape = RoundedCornerShape(SentupRadii.tile)
    // Rested on with previews turned on, this card plays where it sits. The
    // fraction is written into a float state nothing reads during composition -
    // only the claim's polling lambda - so scrolling moves the number without
    // recomposing every card on screen every frame.
    val preview = LocalInlinePreview.current
    val visibleFraction = remember(video.videoId) { mutableFloatStateOf(0f) }
    val isPreviewing = preview?.previewingId == video.videoId

    Column(modifier = Modifier.fillMaxWidth()) {
        // The cover, inset inside the card so it never meets the text band: the
        // tile's own rounded edge holds the scrim and the duration badge, and
        // the card's surface is what the eye finds under them.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(SentupGroup.dividerInset)
                .aspectRatio(16f / 9f)
                .clip(tileShape)
                .then(
                    if (preview == null) Modifier
                    else Modifier.onGloballyPositioned { coordinates ->
                        visibleFraction.floatValue = coordinates.visibleVerticalFraction()
                    }
                )
        ) {
            if (preview != null) {
                InlinePreviewClaim(video = video) { visibleFraction.floatValue }
            }

            VideoThumbnail(
                video = video,
                modifier = Modifier.fillMaxSize()
            )

            // Drawn over the thumbnail rather than in place of it, and faded in
            // only once a frame has actually arrived, so the card never shows a
            // black hole where the picture was while the stream starts.
            //
            // **Faded, not conditionally composed.** [scar] Wrapping this in an
            // AnimatedVisibility keyed on isRendering was a deadlock and the
            // reason previews did nothing at all: the surface only existed once
            // a frame had rendered, and a frame can only render into a surface.
            if (isPreviewing && preview != null) {
                val previewAlpha by animateFloatAsState(
                    targetValue = if (preview.isRendering) 1f else 0f,
                    label = "previewAlpha"
                )
                InlinePreviewSurface(
                    controller = preview,
                    modifier = Modifier
                        .matchParentSize()
                        .graphicsLayer { alpha = previewAlpha }
                )
            }

            // The scrim the duration sits on, ending at the cover's edge rather
            // than at the card's.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.65f))
                        )
                    )
            )

            // Skipped entirely when the duration is unknown, rather than drawn
            // as "0:00". Aligned inside the clipped cover, so a badge can only
            // ever sit on the picture.
            VideoDurationBadge(
                video = video,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            )

            // How far the preview has got, on the card itself. Drawn only while
            // previewing, so a card at rest is the card it was before this
            // feature existed.
            if (isPreviewing && preview != null && preview.isRendering) {
                androidx.compose.material3.LinearProgressIndicator(
                    progress = { preview.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .align(Alignment.BottomCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.White.copy(alpha = 0.25f),
                    gapSize = 0.dp,
                    drawStopIndicator = {},
                )
            }

            // Previews start silent so they never fight music playing
            // underneath; one tap here makes this card audible, another silences
            // it again. Fixed black scrim like the duration badge, so it reads in
            // either theme. The button consumes the tap, so the card does not
            // open.
            if (isPreviewing && preview != null && preview.isRendering) {
                IconButton(
                    onClick = { preview.setPreviewMuted(!preview.isMuted) },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .size(36.dp),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = if (preview.isMuted) {
                            Icons.AutoMirrored.Rounded.VolumeOff
                        } else {
                            Icons.AutoMirrored.Rounded.VolumeUp
                        },
                        contentDescription = stringResource(
                            if (preview.isMuted) R.string.cd_volume_unmute else R.string.cd_volume_mute
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // The text band. The avatar is small and top-aligned so it sits on the
        // first line of it rather than floating beside the whole block.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SentupGroup.dividerInset,
                    end = SentupGroup.dividerInset,
                    bottom = SentupGroup.dividerInset
                ),
            horizontalArrangement = Arrangement.spacedBy(11.dp),
            verticalAlignment = Alignment.Top
        ) {
            VideoChannelAvatar(video = video, openChannel = openChannel)

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = videoFeedSubtitle(video),
                    style = MaterialTheme.typography.bodySmall,
                    color = secondaryColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .videoChannelTap(openChannel)
                )
            }
        }
    }
}

/** Two cards to a row: the same card, tightened. See [VideoCard]. */
@Composable
private fun GridVideoCardContent(video: VideoItem, openChannel: (() -> Unit)?) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
    val tileShape = RoundedCornerShape(SentupRadii.tile)

    Column(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .aspectRatio(16f / 9f)
                .clip(tileShape)
        ) {
            VideoThumbnail(
                video = video,
                modifier = Modifier.fillMaxSize(),
                indicatorSize = 28.dp
            )
            VideoDurationBadge(
                video = video,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(6.dp)
            )
        }
        Column(
            modifier = Modifier.padding(
                start = 12.dp,
                end = 12.dp,
                bottom = 12.dp
            )
        ) {
            Text(
                text = video.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = textColor,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = videoFeedSubtitle(video),
                style = MaterialTheme.typography.labelMedium,
                color = secondaryColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .videoChannelTap(openChannel)
            )
        }
    }
}

/**
 * The creator's picture, small enough to be a mark rather than a second object
 * on the row. It is the tap target for the channel when one can be opened.
 */
@Composable
internal fun VideoChannelAvatar(
    video: VideoItem,
    openChannel: (() -> Unit)?,
    size: androidx.compose.ui.unit.Dp = 30.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .videoChannelTap(openChannel),
        contentAlignment = Alignment.Center
    ) {
        if (!video.channelIconUrl.isNullOrBlank()) {
            AsyncImage(
                model = video.channelIconUrl,
                contentDescription = stringResource(R.string.cd_channel_avatar_named, video.channelName),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = video.channelName.take(1).uppercase(),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * The duration or LIVE label, in a pill at the cover's inner corner.
 *
 * The card draws its own rather than sharing
 * [com.sentup.android.ui.components.VideoThumbnailBadge] because the pill is
 * this app's surface language and the badge's square 4dp corners belong to
 * every other surface that draws it; the contrast rule is the same one that
 * file states - a thumbnail is the same photograph in every palette, so the
 * badge takes its contrast from a scrim, never from a container role.
 */
@Composable
internal fun VideoDurationBadge(video: VideoItem, modifier: Modifier = Modifier) {
    val live = video.isLive
    val text = when {
        live -> stringResource(R.string.badge_live)
        video.duration > 0L -> video.formattedDuration
        else -> null
    } ?: return

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(SentupRadii.pill),
        color = if (live) LIVE_BADGE_RED else MaterialTheme.colorScheme.scrim.copy(alpha = 0.75f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (live) FontWeight.Bold else FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
    }
}

/**
 * The nested channel tap, without a ripple.
 *
 * The card itself presses inward on touch (see [PressableVideoCard]) and the row
 * language is the same (see [com.sentup.android.ui.theme.SentupRow]); a ripple
 * from the avatar or the meta line would flash a second, unrelated piece of
 * feedback over the card while it was already being pressed.
 */
@Composable
fun Modifier.videoChannelTap(openChannel: (() -> Unit)?): Modifier {
    if (openChannel == null) return this
    val interactionSource = remember { MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClickLabel = stringResource(R.string.video_options_go_to_channel),
        onClick = openChannel
    )
}

/** Channel · views · age, on one line. Blank parts drop out rather than leaving a stray separator. */
internal fun videoFeedSubtitle(video: VideoItem): String =
    listOfNotNull(video.channelName, video.viewCount, video.uploadedDate)
        .filter { it.isNotBlank() }
        .joinToString(" · ")

private val LIVE_BADGE_RED = Color(0xFFFF0000)

/** Clock text for a number of whole seconds; mirrors [VideoItem.formattedDuration]. */
private fun formatRemainingClock(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val secs = safe % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, secs)
    } else {
        String.format("%d:%02d", minutes, secs)
    }
}

/**
 * "Pick up where you left off": the cards that carry a resume point in the
 * history repository, with the watched portion drawn as a line under the
 * thumbnail so "how much is left" answers itself. Tapping opens the video the
 * same way the feed does - and the player already seeks to the stored position
 * on its own, so this shelf adds an entrance, never a second resume path.
 */
@Composable
private fun ResumeShelf(
    refreshKey: Any?,
    onVideoClick: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val items = remember(refreshKey) {
        val repo = com.sentup.android.data.VideoHistoryRepository(context)
        repo.getHistory().mapNotNull { video ->
            val durationMs = video.duration * 1000L
            val resumeMs =
                if (durationMs > 0) repo.resumePosition(video.videoId, durationMs) else null
            if (resumeMs != null && resumeMs > 0 && resumeMs < durationMs * 95 / 100) {
                Triple(video, resumeMs, durationMs)
            } else {
                null
            }
        }.take(12)
    }
    if (items.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        SentupGroupHeader(
            text = stringResource(R.string.resume_shelf_title),
            modifier = Modifier.padding(bottom = 10.dp)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = SentupGroup.inset),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { it.first.videoId }) { (video, resumeMs, durationMs) ->
                Column(
                    modifier = Modifier
                        .width(150.dp)
                        .clip(RoundedCornerShape(SentupRadii.tile))
                        .clickable { onVideoClick(video) }
                ) {
                    Box {
                        AsyncImage(
                            model = video.thumbnailUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .clip(RoundedCornerShape(14.dp)),
                            contentScale = ContentScale.Crop
                        )
                        // How much is left, on the cover itself - the card
                        // answers "do I have time for this?" without a tap.
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(6.dp),
                            shape = RoundedCornerShape(SentupRadii.pill),
                            color = scheme.scrim.copy(alpha = 0.75f)
                        ) {
                            Text(
                                text = stringResource(
                                    R.string.resume_shelf_left,
                                    formatRemainingClock((durationMs - resumeMs) / 1000L)
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = Color.White,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        // The watched portion along the thumbnail's bottom edge.
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .fillMaxWidth()
                                .height(3.dp)
                                .background(scheme.outlineVariant.copy(alpha = 0.5f))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(
                                        (resumeMs.toFloat() / durationMs).coerceIn(0f, 1f)
                                    )
                                    .height(3.dp)
                                    .background(scheme.primary)
                            )
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
