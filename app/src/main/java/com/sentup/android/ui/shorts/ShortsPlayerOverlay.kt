package com.sentup.android.ui.shorts
import com.sentup.android.R
import androidx.compose.ui.res.stringResource

import android.content.Intent
import androidx.activity.compose.PredictiveBackHandler
import kotlinx.coroutines.CancellationException
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupGroupCard
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.SentupRow
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupSpecularEdge
import com.sentup.android.ui.video.PlayerChromeIconButton
import com.sentup.android.ui.video.PlayerChromePill
import com.sentup.android.ui.video.SentupActionPill
import com.sentup.android.ui.video.VideoSheetScaffold
import com.sentup.android.ui.video.playerPillShape
import com.sentup.android.ui.video.sentupPressScale
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.outlined.ThumbDown
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.NotInterested
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ThumbDown
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.sentup.android.data.LikeStatus
import com.sentup.android.data.ThemePreferences
import com.sentup.android.ui.video.CommentsSheet
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive as coroutineIsActive

/**
 * Fullscreen vertical-swipe Shorts player, layered above the NavHost like
 * VideoPlayerOverlay. One shared ExoPlayer follows the settled pager page;
 * neighbouring pages show their portrait thumbnails.
 *
 * Speaks Sentüp's Material 3 Expressive dialect: standalone round action
 * buttons that morph to a cookie shape while active, secondaryContainer
 * active states, a Burst-shape flash on like (LikeBurstIcon pattern),
 * MaterialShapes for the pause badge and channel avatar, and a wavy
 * progress line that flattens while paused.
 *
 * [hiddenActions] holds ThemePreferences.SHORTS_ACTION_* ids the user chose
 * to hide from the action rail in Settings.
 */
@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShortsPlayerOverlay(
    viewModel: ShortsPlayerViewModel,
    hiddenActions: Set<String> = emptySet(),
    /**
     * Open the current Short's creator. Routed to the host, which closes this
     * overlay on the way: it is full-bleed with no minimised form, so unlike the
     * video player there is nothing to leave running behind the channel page.
     */
    onOpenChannel: (String) -> Unit = {},
    /**
     * Playlists, downloads and Watch Later live on the home ViewModel, which is
     * what `VideoOptionsSheetHost` is built around. Shorts is the sixth surface
     * to open that sheet and goes through the same host as the other five -
     * per-call-site wiring is what let one of them ship with its queue section
     * silently missing.
     */
    homeViewModel: com.sentup.android.ui.home.HomeViewModel
) {
    val isActive by viewModel.isActive.collectAsState()
    if (!isActive) return

    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity

    val shorts by viewModel.shorts.collectAsState()
    val currentVideo by viewModel.currentVideo.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val isResolving by viewModel.isResolving.collectAsState()
    val playbackError by viewModel.playbackError.collectAsState()
    val connectionAdvice by viewModel.connectionAdvice.collectAsState()
    val engagement by viewModel.engagement.collectAsState()
    // Account subscription OR device subscription - engagement only knows the
    // first, and read alone it showed "Subscribe" for locally followed channels.
    val isSubscribed by viewModel.isSubscribedToChannel.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()

    val comments by viewModel.comments.collectAsState()
    val commentReplies by viewModel.replies.collectAsState()
    val loadingReplyIds by viewModel.loadingReplyIds.collectAsState()
    val isCommentsLoading by viewModel.isCommentsLoading.collectAsState()
    val isLoadingMoreComments by viewModel.isLoadingMoreComments.collectAsState()
    val canComment by viewModel.canComment.collectAsState()
    val isPostingComment by viewModel.isPostingComment.collectAsState()

    var showCommentsSheet by remember { mutableStateOf(false) }
    var showSignInDialog by remember { mutableStateOf(false) }
    var showDismissSheet by remember { mutableStateOf(false) }
    var showDescriptionSheet by remember { mutableStateOf(false) }
    var showOptionsSheet by remember { mutableStateOf(false) }

    fun requireLogin(action: () -> Unit) {
        if (isLoggedIn) action() else showSignInDialog = true
    }

    /**
     * Subscribing has a signed-out path now - it saves to the device unless
     * the user explicitly picked the YouTube-account target - so it gets its
     * own gate instead of the blanket login wall the other actions use.
     */
    fun requireSubscribeLogin(action: () -> Unit) {
        if (viewModel.subscribeNeedsLogin()) showSignInDialog = true else action()
    }

    /**
     * Back previews the Shorts overlay leaving.
     *
     * The odd one out among the overlays: the two players collapse into a mini
     * pill and already own a progress value describing that journey, but this
     * one has no smaller resting state - it simply closes, and what is behind
     * it is the app it was opened from. So the peel is its own value rather
     * than a scrub of an existing one, and it shrinks the whole overlay
     * inward, which is the shape the system uses for leaving a screen with no
     * parent inside the app.
     */
    val shortsScope = rememberCoroutineScope()
    val shortsPeel = remember { Animatable(0f) }
    PredictiveBackHandler(enabled = true) { events ->
        try {
            events.collect { event ->
                shortsPeel.snapTo(event.progress.coerceIn(0f, 1f))
            }
            viewModel.close()
        } catch (cancelled: CancellationException) {
            // From the overlay's own scope: this coroutine is the one being
            // cancelled, and a spring started here would leave the Shorts
            // sitting shrunken with no way back.
            shortsScope.launch {
                shortsPeel.animateTo(
                    targetValue = 0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
            }
        }
    }

    // Pause when the app stops being visible. Shorts are short-form video with
    // no audio-only expectation, and decoding on into a Surface the system is
    // about to destroy is what throws inside MediaCodecVideoRenderer.
    DisposableEffect(activity, viewModel) {
        val lifecycle = activity?.lifecycle
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> viewModel.onEnterBackground()
                androidx.lifecycle.Lifecycle.Event.ON_START -> viewModel.onEnterForeground()
                else -> Unit
            }
        }
        lifecycle?.addObserver(observer)
        onDispose { lifecycle?.removeObserver(observer) }
    }

    // Keep the screen awake while a Short is playing
    DisposableEffect(isPlaying) {
        val window = activity?.window
        if (isPlaying) {
            window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
        onDispose {
            window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // The whole overlay leaves composition on close, so this pager (and its
    // initial page) is recreated fresh on every open
    val pagerState = rememberPagerState(
        initialPage = viewModel.currentIndex.value
    ) { shorts.size }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.onPageSelected(page)
        }
    }

    // Playback progress for the wavy line at the bottom
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(currentVideo) {
        progress = 0f
        while (coroutineIsActive) {
            val player = viewModel.exoPlayer
            val duration = player?.duration ?: 0L
            progress = if (duration > 0) {
                (player?.currentPosition ?: 0L).toFloat() / duration.toFloat()
            } else 0f
            delay(250)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = shortsPeel.value
                val scale = androidx.compose.ui.util.lerp(1f, 0.88f, p)
                scaleX = scale
                scaleY = scale
                // Rounds off as it shrinks, so it reads as a card being put
                // down rather than the screen contents scaling.
                shape = androidx.compose.foundation.shape.RoundedCornerShape(
                    androidx.compose.ui.util.lerp(0f, 32f, p).dp
                )
                clip = true
            }
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { /* consume clicks so the app underneath stays inert */ }
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            beyondViewportPageCount = 1
        ) { page ->
            val item = shorts[page]
            val isCurrent = page == pagerState.settledPage

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { if (isCurrent) viewModel.togglePlayPause() }
            ) {
                // Portrait thumbnail behind the surface: visible on
                // neighbouring pages and while the current one buffers
                AsyncImage(
                    model = item.portraitThumbnailUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )

                if (isCurrent && playbackError == null) {
                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                useController = false
                                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                            }
                        },
                        update = { pv -> pv.player = viewModel.exoPlayer },
                        onRelease = { pv -> pv.player = null },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                if (isCurrent) {
                    // Buffering: the expressive shape-morphing loader on a
                    // tonal puck so it reads on any video frame.
                    //
                    // `isResolving` counts as loading. The player is IDLE while
                    // stream URLs are being fetched - not playing and not
                    // buffering - so that window used to show neither this nor
                    // anything else, while the badge below read it as paused.
                    AnimatedVisibility(
                        visible = (isBuffering || isResolving) && playbackError == null,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(
                                modifier = Modifier.size(44.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Paused badge: expressive cookie shape with a springy pop.
                    // Only for a Short the user actually paused - a Short that
                    // is still resolving is loading, and saying "paused" there
                    // invites a tap that does nothing.
                    AnimatedVisibility(
                        visible = !isPlaying && !isBuffering && !isResolving &&
                            playbackError == null,
                        enter = scaleIn(
                            initialScale = 0.6f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                        ) + fadeIn(),
                        exit = scaleOut(targetScale = 0.6f) + fadeOut(),
                        modifier = Modifier.align(Alignment.Center)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(MaterialShapes.Cookie12Sided.toShape())
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = stringResource(R.string.cd_play),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    val advice = connectionAdvice
                    if (playbackError != null && advice != null) {
                        // Refused connection: network advice in place of the
                        // failure card, whose Retry would fail the same way.
                        com.sentup.android.ui.components.ConnectionAdviceCard(
                            advice = advice,
                            onRetry = { viewModel.retryCurrent() },
                        )
                    } else if (playbackError != null) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .padding(horizontal = 32.dp),
                            shape = RoundedCornerShape(28.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(MaterialShapes.Cookie9Sided.toShape())
                                        .background(MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = playbackError?.message ?: stringResource(R.string.sh_playback_failed),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                FilledTonalButton(onClick = { viewModel.retryCurrent() }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(stringResource(R.string.action_retry))
                                }
                            }
                        }
                    }
                }

            }
        }

        // Top chrome: a floating capsule with the way out and the name of the
        // surface, the overflow in a capsule of its own. The 140dp gradient this
        // replaces darkened a fifth of the frame so white text could sit on it;
        // the text carries its own background now and the frame stays whole.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlayerChromePill {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlayerChromeIconButton(
                        icon = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_close_shorts),
                        onClick = { viewModel.close() }
                    )
                    Text(
                        text = stringResource(R.string.sp_shorts),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(end = 12.dp, start = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            // The overflow, in the one piece of chrome this player has that is
            // not the action rail. It cannot go in the rail: that column is a
            // set of single-purpose taps the thumb rests on while swiping, and
            // a menu opened by accident there is a menu over the video.
            PlayerChromePill {
                PlayerChromeIconButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.cd_more_options),
                    enabled = currentVideo != null,
                    onClick = { showOptionsSheet = true }
                )
            }
        }

        // Metadata (bottom-left) + floating action pill (bottom-right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars)
                // Clear of the scrub strip, which has the space below.
                .padding(start = 16.dp, end = 10.dp, bottom = SHORTS_SCRUB_ZONE + 6.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val video = currentVideo
                if (video != null && video.channelName.isNotBlank()) {
                    // The avatar and name are one target, the way they are on the
                    // watch page, and they sit on a capsule now instead of directly
                    // on the picture: white text over whatever the video happens to
                    // be showing is a legibility gamble, not a design.
                    val channelId = video.channelId?.takeIf { it.startsWith("UC") }
                    val channelPress = remember(video.videoId) { MutableInteractionSource() }
                    val capsule = playerPillShape()
                    Row(
                        modifier = Modifier
                            .sentupCard(shape = capsule, elevation = 2)
                            .sentupSpecularEdge(capsule)
                            .sentupPressScale(channelPress)
                            .clip(capsule)
                            .then(
                                if (channelId != null) {
                                    Modifier.clickable(
                                        interactionSource = channelPress,
                                        indication = null
                                    ) { onOpenChannel(channelId) }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(start = 5.dp, end = 5.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!video.channelIconUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = video.channelIconUrl,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.Person,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(17.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(9.dp))
                        Text(
                            text = video.channelName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        SentupActionPill(
                            label = stringResource(
                                if (isSubscribed) R.string.subscribed else R.string.subscribe
                            ),
                            onClick = { requireSubscribeLogin { viewModel.toggleSubscribe() } },
                            filled = !isSubscribed,
                            icon = if (isSubscribed) Icons.Rounded.Check else null
                        )
                    }
                }
                if (video != null && video.title.isNotBlank()) {
                    // Title plus stats are one target, and the target is what
                    // opens the description. On its own floating panel now, so the
                    // affordance is the object itself rather than a truncated line
                    // of white text over a gradient.
                    val stats = shortsStatsLine(video)
                    val hasMore = stats != null || !video.description.isNullOrBlank()
                    val titlePress = remember(video.videoId) { MutableInteractionSource() }
                    val titleShape = RoundedCornerShape(SentupRadii.card)
                    Column(
                        modifier = Modifier
                            .sentupCard(shape = titleShape, elevation = 2)
                            .sentupSpecularEdge(titleShape)
                            .then(
                                if (hasMore) {
                                    Modifier
                                        .sentupPressScale(titlePress)
                                        .clip(titleShape)
                                        .clickable(
                                            interactionSource = titlePress,
                                            indication = null
                                        ) { showDescriptionSheet = true }
                                } else {
                                    Modifier
                                }
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (stats != null) {
                            Spacer(modifier = Modifier.height(3.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stats,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Rounded.ExpandMore,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Standalone round action buttons; each can be hidden in Settings
            val likeStatus = engagement?.likeStatus ?: LikeStatus.INDIFFERENT
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (ThemePreferences.SHORTS_ACTION_LIKE !in hiddenActions) {
                    ShortsAction(
                        icon = if (likeStatus == LikeStatus.LIKE) Icons.Rounded.ThumbUp
                            else Icons.Outlined.ThumbUp,
                        label = engagement?.likeCount ?: stringResource(R.string.like),
                        active = likeStatus == LikeStatus.LIKE,
                        contentDescription = stringResource(R.string.like),
                        onClick = { requireLogin { viewModel.toggleLike() } }
                    )
                }
                if (ThemePreferences.SHORTS_ACTION_DISLIKE !in hiddenActions) {
                    ShortsAction(
                        icon = if (likeStatus == LikeStatus.DISLIKE) Icons.Rounded.ThumbDown
                            else Icons.Outlined.ThumbDown,
                        label = stringResource(R.string.sp_action_dislike),
                        active = likeStatus == LikeStatus.DISLIKE,
                        contentDescription = stringResource(R.string.cd_dislike),
                        onClick = { requireLogin { viewModel.toggleDislike() } }
                    )
                }
                if (ThemePreferences.SHORTS_ACTION_COMMENTS !in hiddenActions) {
                    ShortsAction(
                        icon = Icons.Rounded.ChatBubble,
                        label = stringResource(R.string.cd_comments),
                        contentDescription = stringResource(R.string.cd_comments),
                        onClick = {
                            viewModel.ensureCommentsLoaded()
                            showCommentsSheet = true
                        }
                    )
                }
                if (ThemePreferences.SHORTS_ACTION_SHARE !in hiddenActions) {
                    ShortsAction(
                        icon = Icons.Rounded.Share,
                        label = stringResource(R.string.video_options_share),
                        contentDescription = stringResource(R.string.video_options_share),
                        onClick = {
                            val videoId = currentVideo?.videoId ?: return@ShortsAction
                            val send = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, "https://youtube.com/shorts/$videoId")
                            }
                            context.startActivity(Intent.createChooser(send, context.getString(R.string.share_short)))
                        },
                        // Long-press skips the chooser and drops the link
                        // straight on the clipboard - the one-share gesture
                        // people actually repeat.
                        onLongClick = currentVideo?.let { video ->
                            {
                                val link = "https://youtube.com/shorts/${video.videoId}"
                                context.getSystemService(android.content.ClipboardManager::class.java)
                                    ?.setPrimaryClip(android.content.ClipData.newPlainText(null, link))
                                android.widget.Toast
                                    .makeText(context, R.string.sh_link_copied, android.widget.Toast.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    )
                }
                if (ThemePreferences.SHORTS_ACTION_NOT_INTERESTED !in hiddenActions) {
                    ShortsAction(
                        icon = Icons.Rounded.NotInterested,
                        label = stringResource(R.string.video_options_not_interested),
                        contentDescription = stringResource(R.string.video_options_not_interested),
                        // Opens a chooser rather than acting straight away: this
                        // button sits in a rail the thumb rests on while
                        // swiping, and a one-tap irreversible-looking dismissal
                        // there would go off by accident constantly.
                        onClick = { showDismissSheet = true }
                    )
                }
            }
        }

        // Wavy playback progress, Sentüp's player signature; the wave settles
        // flat while paused, the same as the music player's bar.
        //
        // **The amplitude is read straight from `isPlaying` and not animated
        // here.** [scar] This used to feed an `animateFloatAsState` through
        // `amplitude = { waveAmplitude }`, and the wave never flattened: that
        // lambda captures only the animation's `State` object, whose identity
        // never changes, so the compiler memoizes it, the progress node's
        // element compares equal on every recomposition and its `update` never
        // runs. The node went on drawing the amplitude it had captured while
        // playing. The nine music players had it right by accident - their
        // `{ if (isPlaying) 1f else 0f }` captures the *boolean*, so pausing
        // produces a different lambda and the node is updated.
        //
        // Nothing is lost by dropping the animation: M3's wavy indicator holds
        // its own amplitude `Animatable` and animates towards whatever this
        // returns, which is why the music bars settle smoothly without one.
        // Animating it here was always a second animation driving the first.
        ShortsScrubBar(
            mediaId = currentVideo?.videoId,
            progress = { progress },
            durationMs = { viewModel.exoPlayer?.duration?.takeIf { it > 0L } ?: 0L },
            isPlaying = isPlaying,
            onSeek = { viewModel.seekTo(it, precise = false) },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.navigationBars),
        )
    }

    if (showCommentsSheet) {
        CommentsSheet(
            comments = comments,
            replies = commentReplies,
            loadingReplyIds = loadingReplyIds,
            isLoading = isCommentsLoading,
            isLoadingMore = isLoadingMoreComments,
            commentsAvailable = engagement?.commentsToken != null,
            canComment = canComment,
            isPosting = isPostingComment,
            onLoadMore = { viewModel.loadMoreComments() },
            onLoadReplies = { viewModel.loadReplies(it) },
            onPostComment = { viewModel.postComment(it) },
            onPostReply = { target, threadParent, text ->
                viewModel.postReply(target, threadParent, text)
            },
            onLikeComment = { comment -> requireLogin { viewModel.toggleCommentLike(comment) } },
            onDeleteComment = { comment -> viewModel.deleteComment(comment) },
            onDismiss = { showCommentsSheet = false },
            onOpenAuthor = { channelId ->
                showCommentsSheet = false
                onOpenChannel(channelId)
            }
        )
    }

    if (showDescriptionSheet) {
        currentVideo?.let { video ->
            ShortsDescriptionSheet(
                video = video,
                onSeekTo = { viewModel.seekTo(it) },
                onOpenChannel = { channelId ->
                    // Terminal, like every channel tap in Shorts: the overlay
                    // has no minimised form, so it closes rather than sitting
                    // on top of the page it just opened.
                    showDescriptionSheet = false
                    onOpenChannel(channelId)
                },
                onDismiss = { showDescriptionSheet = false }
            )
        }
    }

    if (showOptionsSheet) {
        currentVideo?.let { video ->
            com.sentup.android.ui.video.VideoOptionsSheetHost(
                video = video,
                viewModel = homeViewModel,
                onDismiss = { showOptionsSheet = false },
                // The dismissals belong to this ViewModel, not the home feeds:
                // Shorts filters on ingestion, so the hide has to remove the
                // item from _shorts and move the pager on.
                onNotInterestedOverride = {
                    showOptionsSheet = false
                    viewModel.markCurrentNotInterested()
                },
                onBlockChannelOverride = {
                    showOptionsSheet = false
                    viewModel.blockChannelForCurrent()
                },
                onOpenChannel = { channelId ->
                    showOptionsSheet = false
                    onOpenChannel(channelId)
                }
            )
        }
    }

    if (showDismissSheet) {
        ShortsDismissSheet(
            channelName = currentVideo?.channelName.orEmpty(),
            onNotInterested = {
                showDismissSheet = false
                viewModel.markCurrentNotInterested()
            },
            onBlockChannel = {
                showDismissSheet = false
                viewModel.blockChannelForCurrent()
            },
            onDismiss = { showDismissSheet = false }
        )
    }

    if (showSignInDialog) {
        com.sentup.android.ui.auth.YouTubeAuthDialog(
            onDismiss = { showSignInDialog = false },
            onAuthSuccess = {
                showSignInDialog = false
                viewModel.onLoginStateChanged()
            }
        )
    }
}

/**
 * One action of the Shorts rail: a floating capsule - icon over its count -
 * that presses inward when tapped. The active state is the palette, not a
 * container colour.
 *
 * It used to morph from a circle to a nine-sided cookie, pop the icon on a
 * spring, and throw a one-shot "burst" of colour across the frame when a like
 * landed. A flash over a playing video is exactly the thing you cannot stop
 * looking at, and the shape morph meant the button was never the same object
 * twice. The whole action is now one transform: the capsule moves in.
 */
@Composable
private fun ShortsAction(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    active: Boolean = false,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = playerPillShape()

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .sentupPressScale(interactionSource)
            .sentupCard(shape = shape, elevation = 2)
            .sentupSpecularEdge(shape)
            .clip(shape)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 11.dp, vertical = 9.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 68.dp)
        )
    }
}

/**
 * The two "stop recommending" choices for the Short on screen.
 *
 * A sheet rather than a direct action on the rail button: that rail is where
 * the thumb rests between swipes, and a single tap that made a video vanish
 * would fire by accident often enough to be the thing people remember about
 * the feature. Both choices are still undoable from the app-wide snackbar.
 *
 * On the new page language the two choices are rows of one inset card instead
 * of two stacked slabs: they are alternatives, so they read as one object,
 * and the channel row goes under the same error tint as "don't recommend"
 * does everywhere else in the app.
 */
@Composable
private fun ShortsDismissSheet(
    channelName: String,
    onNotInterested: () -> Unit,
    onBlockChannel: () -> Unit,
    onDismiss: () -> Unit
) {
    VideoSheetScaffold(
        onDismiss = onDismiss,
        title = stringResource(R.string.stop_recommending)
    ) {
        SentupGroupCard(modifier = Modifier.padding(vertical = SentupGroup.gap)) {
            SentupRow(
                icon = Icons.Rounded.NotInterested,
                title = stringResource(R.string.video_options_not_interested),
                subtitle = stringResource(R.string.sh_hide_short_sub),
                onClick = onNotInterested
            )
            SentupDivider()
            SentupRow(
                icon = Icons.Rounded.RemoveCircleOutline,
                title = stringResource(R.string.video_options_dont_recommend_channel),
                subtitle = channelName.takeIf { it.isNotBlank() }
                    ?.let { "Hide everything from $it" }
                    ?: stringResource(R.string.sh_hide_channel_sub),
                tint = MaterialTheme.colorScheme.error,
                onClick = onBlockChannel
            )
        }
    }
}
