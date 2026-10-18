package com.sentup.android.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.sentup.android.R
import com.sentup.android.data.MusicQueueItem
import com.sentup.android.data.Song
import com.sentup.android.data.isUnknownAlbum
import com.sentup.android.data.isUnknownArtist
import com.sentup.android.data.isUnknownTitle
import com.sentup.android.ui.components.LikeBurstIcon
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage
import com.sentup.android.ui.theme.sentupSpecularEdge
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Gesture - the carousel player, in the Sentüp surface language.
 *
 * The idea this style exists for is kept exactly as it was: the covers are a
 * carousel you drag sideways, each neighbour tilting, shrinking and dropping
 * away from the centre so the depth of the queue is something you can *see*,
 * and there is no transport row at all - a tap on the centred cover is
 * play/pause. What changed is what those covers sit on.
 *
 * The flat two-tone spread and the `HorizontalFloatingToolbar` are gone. The
 * page is [sentupPage], the chrome is one floating capsule bar, the centred
 * cover is a [sentupCard] at elevation 2 with a specular edge while its
 * neighbours are flat, and the action row is a single floating deck whose
 * cells press *inward* rather than flashing. The times are monospace.
 *
 * The carousel maths is untouched: same continuous position, same pre-render
 * window, same clamping at both ends, same external sync.
 */
@Composable
fun GesturePlayerSheetContent(
    viewModel: PlayerViewModel,
    ambientBackground: Boolean = true,
    onCollapse: () -> Unit,
    onLoadMore: () -> Unit = {},
    onArtistClick: (String) -> Unit = {},
    /** Hand this song to the video player; null where there is no video pipeline. */
    onWatchAsVideo: (() -> Unit)? = null,
    onAlbumClick: (String) -> Unit = {},
    onOpenAlbum: (com.sentup.android.data.PlaylistDisplayItem) -> Unit = {}
) {
    // Back is handled once by ExpandablePlayer, which previews the collapse as
    // a gesture instead of firing at the end of one. A BackHandler here would
    // be registered later and silently win.
    //
    // ambientBackground is kept in the signature for style parity: the page
    // tone comes from sentupPage() now, so no ambient wash paints behind it.
    val currentSong by viewModel.currentSong.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    var showQueue by remember { mutableStateOf(false) }
    var showOptions by rememberNowPlayingOptionsOpen()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .sentupPage()
    ) {
        Crossfade(targetState = showQueue, label = "GesturePlayerQueueTransition") { queueVisible ->
            if (queueVisible) {
                SentupQueueView(
                    viewModel = viewModel,
                    onCollapse = onCollapse,
                    onBackToPlayer = { showQueue = false },
                    onLoadMore = onLoadMore
                )
            } else {
                GestureNowPlayingView(
                    viewModel = viewModel,
                    onCollapse = onCollapse,
                    onShowQueue = { showQueue = true },
                    onShowOptions = { showOptions = true },
                    onArtistClick = onArtistClick
                )
            }
        }
    }

    // The overflow menu, gated on a song so it can never open against nothing.
    if (showOptions) {
        currentSong?.let { song ->
            NowPlayingOptionsSheet(
                song = song,
                viewModel = viewModel,
                onDismiss = { showOptions = false },
                onArtistClick = onArtistClick,
                onWatchAsVideo = onWatchAsVideo,
                onAlbumClick = onAlbumClick,
                onOpenAlbum = onOpenAlbum
            )
        }
    }
}

/**
 * The now-playing page: a floating capsule bar, the "playing from" line, the
 * carousel of covers, the title block, the progress line, and one floating
 * deck of actions.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun GestureNowPlayingView(
    viewModel: PlayerViewModel,
    onCollapse: () -> Unit,
    onShowQueue: () -> Unit,
    onShowOptions: () -> Unit,
    onArtistClick: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val playerHaptics = rememberPlayerHaptics()
    val currentSong by viewModel.currentSong.collectAsState()
    val queue by viewModel.playOrderQueue.collectAsState()
    val currentQueueItemId by viewModel.currentQueueItemId.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val playWhenReady by viewModel.playWhenReady.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isFavorite by viewModel.isCurrentSongLiked.collectAsState()
    val lyricsOpen = LocalLyricsOpen.current

    val isDownloaded = currentSong?.let { viewModel.isDownloaded(it.id) } ?: false
    val isDownloading = currentSong?.let { viewModel.isDownloading(it.id) } ?: false
    val isLocalOriginal = currentSong?.let { viewModel.isLocalOriginal(it) } ?: false

    val sleepTimer = rememberSleepTimerControl(viewModel = viewModel)

    val currentIndex = remember(currentQueueItemId, queue) {
        if (queue.isEmpty()) {
            0
        } else {
            queue.indexOfFirst { it.id == currentQueueItemId }
                .coerceIn(0, queue.lastIndex.coerceAtLeast(0))
        }
    }

    // Swipe-to-skip on the song information. It steps the queue rather than
    // calling the ViewModel's skip, because that is what the carousel above
    // does - and the carousel follows currentIndex, so it slides across to
    // meet the new song instead of the two disagreeing. Both ends clamp: this
    // style has no prev/next buttons, so running off the queue must be a
    // no-op, not a wrap.
    fun onQueueStep(item: MusicQueueItem) {
        playerHaptics.skip()
        viewModel.skipToQueueItem(item.id)
    }

    val swipeToSkip = rememberSwipeToSkip(
        onNext = { queue.getOrNull(currentIndex + 1)?.let { onQueueStep(it) } },
        onPrevious = { queue.getOrNull(currentIndex - 1)?.let { onQueueStep(it) } }
    )

    val albumName = currentSong?.album?.takeIf { !isUnknownAlbum(it) }
        ?: stringResource(R.string.unknown_album)

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        // Don't render content if dimensions are too small (during collapse)
        if (maxWidth < 100.dp || maxHeight < 100.dp) {
            return@BoxWithConstraints
        }

        // The progress line aligns with the carousel's own edge.
        val progressBarWidth = if (queue.isNotEmpty()) maxWidth * 0.90f else maxWidth * 0.98f

        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    // Clear the floating bar; the bar is aligned on top of
                    // this Box and does not scroll.
                    .padding(top = 76.dp)
                    .padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // ========== PLAYING FROM ==========
                // This style's one piece of chrome the others do not have: the
                // queue's source named above the carousel, because a carousel
                // without it is just a wall of covers.
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SentupGroup.inset),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = stringResource(R.string.vq_playing_from),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = albumName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center
                    )
                }

                // ========== CAROUSEL ==========
                BoxWithConstraints(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    if (maxWidth < 50.dp || maxHeight < 50.dp) {
                        return@BoxWithConstraints
                    }
                    val albumSize = maxWidth.coerceAtLeast(1.dp)
                    Box(
                        modifier = Modifier.size(albumSize),
                        contentAlignment = Alignment.Center
                    ) {
                        if (queue.isNotEmpty()) {
                            SwipeableAlbumCarousel(
                                queue = queue,
                                currentIndex = currentIndex,
                                isPlaying = isPlaying,
                                isBuffering = isBuffering && playWhenReady,
                                onPlayPauseToggle = {
                                    playerHaptics.playPause(!viewModel.isPlaying.value)
                                    viewModel.togglePlayPause()
                                },
                                onQueueItemChange = { onQueueStep(it) }
                            )
                        } else if (currentSong != null) {
                            SingleAlbumArt(
                                song = currentSong,
                                isPlaying = isPlaying,
                                isBuffering = isBuffering && playWhenReady,
                                onPlayPauseToggle = {
                                    playerHaptics.playPause(!viewModel.isPlaying.value)
                                    viewModel.togglePlayPause()
                                }
                            )
                        }
                    }
                }

                // ========== TITLE / ARTIST / PROGRESS ==========
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val title = currentSong?.title?.takeIf { !isUnknownTitle(it) }
                        ?: stringResource(R.string.np_art_untitled)
                    val artistName = currentSong?.artist?.takeIf { !isUnknownArtist(it) }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .swipeToSkip(swipeToSkip)
                            .swipeToSkipFollow(swipeToSkip)
                            .padding(horizontal = SentupGroup.inset),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontSize = 28.sp,
                                lineHeight = 34.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.4).sp
                            ),
                            color = scheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = artistName ?: stringResource(R.string.np_artist_unknown),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (artistName != null) scheme.primary else scheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(RoundedCornerShape(SentupRadii.pill))
                                .clickable(enabled = artistName != null) {
                                    artistName?.let(onArtistClick)
                                }
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(22.dp))

                    // Track the finger locally while scrubbing; seek once on
                    // release instead of on every drag frame (rebuffer storms).
                    var scrubPosition by remember { mutableStateOf<Float?>(null) }
                    val scrubReturnPoint = LocalScrubReturnPoint.current
                    // Advanced from the clock each frame rather than sprung
                    // toward a once-a-second sample - see rememberSmoothProgress.
                    val smoothProgress = rememberSmoothProgress(
                        positionMs = progress,
                        durationMs = duration,
                        isPlaying = isPlaying,
                        scrubPositionMs = scrubPosition
                    )

                    Column(modifier = Modifier.width(progressBarWidth)) {
                        Box(contentAlignment = Alignment.Center) {
                            val railStroke = Stroke(
                                width = with(LocalDensity.current) { 4.dp.toPx() },
                                cap = StrokeCap.Round
                            )
                            val thumbRadius by animateDpAsState(
                                targetValue = if (isPlaying) 6.dp else 10.dp,
                                animationSpec = MaterialTheme.motionScheme.fastSpatialSpec(),
                                label = "GestureThumbPause"
                            )
                            ExpressiveScrubber(
                                progress = { smoothProgress.value },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(scrubberTrackHeight(14.dp)),
                                stroke = railStroke,
                                trackStroke = railStroke,
                                color = scheme.primary,
                                trackColor = scheme.surfaceVariant.copy(alpha = 0.6f)
                            )
                            // The shared bar's thumb: 12dp playing, 20dp paused.
                            Canvas(modifier = Modifier.matchParentSize()) {
                                val fraction = smoothProgress.value.coerceIn(0f, 1f)
                                drawCircle(
                                    color = scheme.primary,
                                    radius = thumbRadius.toPx(),
                                    center = Offset(size.width * fraction, size.height / 2f)
                                )
                            }
                            Slider(
                                interactionSource = LocalPlayerScrubInteraction.current,
                                value = scrubPosition ?: progress.toFloat(),
                                onValueChange = {
                                    scrubPosition = scrubReturnPoint.follow(
                                        value = it,
                                        rangeEnd = duration.toFloat().coerceAtLeast(1f)
                                    ) { smoothProgress.value }
                                },
                                onValueChangeFinished = {
                                    val cancelled = scrubReturnPoint.release()
                                    if (!cancelled) {
                                        scrubPosition?.let { viewModel.seekTo(it.toLong()) }
                                    }
                                    scrubPosition = null
                                },
                                valueRange = 0f..(duration.toFloat().coerceAtLeast(1f)),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.Transparent,
                                    activeTrackColor = Color.Transparent,
                                    inactiveTrackColor = Color.Transparent
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = formatEditorialTime(scrubPosition?.toLong() ?: progress),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                ),
                                color = scheme.onSurfaceVariant
                            )
                            Text(
                                text = formatEditorialTime(duration),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.sp
                                ),
                                color = scheme.onSurfaceVariant
                            )
                        }
                    }
                }

                PlayerVisualizerSlot(modifier = Modifier.fillMaxWidth())

                // ========== ACTION DECK ==========
                GestureControlDeck(
                    shuffleModeEnabled = shuffleModeEnabled,
                    repeatMode = repeatMode,
                    isFavorite = isFavorite,
                    isDownloaded = isDownloaded,
                    isDownloading = isDownloading,
                    isLocalOriginal = isLocalOriginal,
                    sleepTimerActive = sleepTimer.active,
                    onToggleShuffle = { viewModel.toggleShuffle() },
                    onToggleRepeat = { viewModel.toggleRepeat() },
                    onToggleFavorite = {
                        playerHaptics.confirm()
                        viewModel.toggleCurrentSongLike()
                    },
                    onToggleDownload = { currentSong?.let { viewModel.toggleDownload(it) } },
                    onSleepTimerClick = sleepTimer.open,
                    onShowOptions = onShowOptions
                )
            }

            // ========== FLOATING CAPSULE BAR ==========
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = SentupGroup.inset)
                    .padding(top = 8.dp)
                    .sentupCard(shape = CircleShape, elevation = 1)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onCollapse) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.cd_collapse),
                        tint = scheme.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.now_playing_channel_name),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { lyricsOpen.value = true }) {
                    Icon(
                        Icons.Rounded.Lyrics,
                        contentDescription = stringResource(R.string.ps_lyrics),
                        tint = if (lyricsOpen.value) scheme.primary else scheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onShowQueue) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = stringResource(R.string.cd_now_playing),
                        tint = scheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onShowOptions) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_options),
                        tint = scheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

/**
 * The action row: one floating deck with the favourite as its raised, filled
 * cell and everything else as glyph cells. It replaces the
 * `HorizontalFloatingToolbar`, which was the one control cluster in this app
 * that was still a grey slab: five targets with no depth between them and no
 * state you could read at a glance.
 */
@Composable
private fun GestureControlDeck(
    shuffleModeEnabled: Boolean,
    repeatMode: Int,
    isFavorite: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isLocalOriginal: Boolean,
    sleepTimerActive: Boolean,
    onToggleShuffle: () -> Unit,
    onToggleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleDownload: () -> Unit,
    onSleepTimerClick: () -> Unit,
    onShowOptions: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val deckShape = RoundedCornerShape(SentupRadii.pill)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .sentupCard(shape = deckShape, elevation = 1)
                .padding(horizontal = 6.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The highlighted action keeps its raised, filled cell: the
                // favourite is the one thing here you reach for while a song
                // plays, so it stays the loudest object in the deck.
                GestureDeckCell(
                    highlighted = true,
                    active = isFavorite,
                    contentDescription = stringResource(R.string.cd_favorite),
                    onClick = onToggleFavorite
                ) {
                    LikeBurstIcon(
                        isFavorite = isFavorite,
                        iconSize = 22.dp,
                        tint = if (isFavorite) scheme.onPrimary else scheme.onSurface
                    )
                }
                GestureDeckCell(
                    active = shuffleModeEnabled,
                    contentDescription = stringResource(R.string.cd_shuffle),
                    onClick = onToggleShuffle
                ) {
                    Icon(
                        Icons.Outlined.Shuffle,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp)
                    )
                }
                GestureDeckCell(
                    active = repeatMode != Player.REPEAT_MODE_OFF,
                    contentDescription = stringResource(R.string.cd_repeat),
                    onClick = onToggleRepeat
                ) {
                    Icon(
                        imageVector = if (repeatMode == Player.REPEAT_MODE_ONE) {
                            Icons.Outlined.RepeatOne
                        } else {
                            Icons.Outlined.Repeat
                        },
                        contentDescription = null,
                        modifier = Modifier.size(21.dp)
                    )
                }
                GestureDeckCell(
                    active = isDownloaded,
                    enabled = !isDownloading && !isLocalOriginal,
                    contentDescription = if (isLocalOriginal) {
                        stringResource(R.string.cd_local_file)
                    } else {
                        stringResource(R.string.song_options_download)
                    },
                    onClick = onToggleDownload
                ) {
                    when {
                        isLocalOriginal -> Icon(
                            imageVector = Icons.Rounded.Smartphone,
                            contentDescription = null,
                            modifier = Modifier.size(21.dp)
                        )
                        isDownloading -> CircularProgressIndicator(
                            modifier = Modifier.size(19.dp),
                            color = scheme.primary,
                            strokeWidth = 2.dp
                        )
                        else -> Icon(
                            imageVector = if (isDownloaded) {
                                Icons.Rounded.CheckCircle
                            } else {
                                Icons.Rounded.Download
                            },
                            contentDescription = null,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                }
                GestureDeckCell(
                    active = sleepTimerActive,
                    contentDescription = stringResource(R.string.sleep_timer_title),
                    onClick = onSleepTimerClick
                ) {
                    Icon(
                        Icons.Rounded.Bedtime,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp)
                    )
                }
                GestureDeckCell(
                    contentDescription = stringResource(R.string.cd_more_options),
                    onClick = onShowOptions
                ) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = null,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}

/**
 * One cell of the action deck: 44dp round, glyph only, and the whole cell
 * presses inward instead of tinting. [highlighted] gives it the emerald fill;
 * [active] only turns the glyph emerald, which is enough to read the state.
 */
@Composable
private fun GestureDeckCell(
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    active: Boolean = false,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.90f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "GestureDeckPress"
    )
    val glyphColor = when {
        highlighted -> scheme.onPrimary
        active -> scheme.primary
        else -> scheme.onSurfaceVariant
    }

    Box(
        modifier = modifier
            .size(44.dp)
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .clip(CircleShape)
            .background(if (highlighted) scheme.primary else Color.Transparent)
            .then(
                if (highlighted) Modifier.sentupSpecularEdge(CircleShape) else Modifier
            )
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.runtime.CompositionLocalProvider(
            androidx.compose.material3.LocalContentColor provides glyphColor
        ) {
            content()
        }
    }
}

/**
 * Custom Swipeable Album Carousel with smooth sliding animations
 *
 * Architecture:
 * - Uses a continuous float position for the carousel
 * - Pre-renders 5 albums (-2 to +2) so transitions are instant
 * - External changes animate smoothly via spring physics
 * - No flickering, no delayed loading
 *
 * The centred cover now sits on [sentupCard] at elevation 2 with a specular
 * edge; its neighbours stay flat, because the depth cue is already carried by
 * the tilt, the shrink and the drop. Nothing else about the maths moved.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SwipeableAlbumCarousel(
    queue: List<MusicQueueItem>,
    currentIndex: Int,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPauseToggle: () -> Unit,
    onQueueItemChange: (MusicQueueItem) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val styleWheel = LocalPlayerStyleWheelController.current
    // The carousel position as a continuous float
    // Position 0 = first song centered, Position 1 = second song centered, etc.
    var targetPosition by remember { mutableFloatStateOf(currentIndex.toFloat()) }
    val animatedPosition by animateFloatAsState(
        targetValue = targetPosition,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "CarouselPosition"
    )

    // Track if user is dragging (to pause external sync during drag)
    var isDragging by remember { mutableStateOf(false) }

    // Sync with external index changes (e.g., from queue click, skip buttons)
    LaunchedEffect(currentIndex) {
        if (!isDragging && targetPosition.roundToInt() != currentIndex) {
            // Smoothly animate to the new position
            targetPosition = currentIndex.toFloat()
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Guard against invalid dimensions during transitions
        // Use minimum usable size (not just zero) for high DPI compatibility
        if (maxWidth < 50.dp || maxHeight < 50.dp) {
            return@BoxWithConstraints
        }

        val albumSize = (minOf(maxWidth * 0.90f, maxHeight * 0.95f)).coerceAtLeast(1.dp)

        // Spacing between album centers
        val spacing = albumSize * 0.75f
        val spacingPx = with(LocalDensity.current) { spacing.toPx() }

        // Track starting position when drag begins
        var dragStartPosition by remember { mutableFloatStateOf(0f) }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(albumSize)
                .pointerInput(queue.size) {
                    detectHorizontalDragGestures(
                        onDragStart = {
                            isDragging = true
                            dragStartPosition = targetPosition
                        },
                        onDragEnd = {
                            isDragging = false
                            // Snap to nearest integer position
                            val nearestIndex = targetPosition.roundToInt()
                                .coerceIn(0, queue.lastIndex)
                            val startIndex = dragStartPosition.roundToInt()

                            // Always trigger song change if we moved to different index
                            if (nearestIndex != startIndex && nearestIndex in 0..queue.lastIndex) {
                                onQueueItemChange(queue[nearestIndex])
                            }
                            targetPosition = nearestIndex.toFloat()
                        },
                        onDragCancel = {
                            isDragging = false
                            // Return to start position on cancel
                            targetPosition = dragStartPosition.roundToInt().toFloat()
                                .coerceIn(0f, queue.lastIndex.toFloat())
                        },
                        onHorizontalDrag = { _, amount ->
                            val delta = -amount / spacingPx // drag right = lower index
                            val newPosition = (targetPosition + delta)
                                .coerceIn(0f, queue.lastIndex.toFloat())
                            targetPosition = newPosition
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            // Render albums in range: visible ones around the current position
            // Pre-render 2 extra on each side for smooth transitions
            val centerIndex = animatedPosition.roundToInt()
            val visibleRange = (centerIndex - 2).coerceAtLeast(0)..
                (centerIndex + 2).coerceAtMost(queue.lastIndex)

            visibleRange.forEach { songIndex ->
                val queueItem = queue[songIndex]
                val song = queueItem.song

                // Position relative to center (0 = centered)
                val relativePosition = songIndex.toFloat() - animatedPosition
                val distanceFromCenter = relativePosition.absoluteValue

                // Skip rendering if too far off (optimization)
                if (distanceFromCenter > 2.5f) return@forEach

                // Size: full at center, smaller at sides
                val sizeMultiplier = (1f - distanceFromCenter * 0.30f).coerceIn(0.55f, 1f)
                val currentSize = albumSize * sizeMultiplier
                val currentCornerRadius = albumSize * 0.08f * sizeMultiplier

                // Alpha: full at center, faded at sides
                val alpha = (1f - distanceFromCenter * 0.45f).coerceIn(0.35f, 1f)

                // Rotation: tilted based on position
                val rotation = relativePosition * 8f

                // Z-index: center album on top
                val zIndex = (10f - distanceFromCenter * 3f).coerceAtLeast(0f)

                // X position from center
                val xOffset = spacing * relativePosition

                // Y offset for depth (side albums drop down)
                val yOffset = 10.dp * distanceFromCenter

                // Is this the "current" centered album?
                val isCentered = distanceFromCenter < 0.5f

                val artSong = song.takeIf {
                    it.thumbnailUrl != null ||
                        it.albumArtUri != null ||
                        it.highResThumbnailUrl != null
                }
                val cardShape = RoundedCornerShape(currentCornerRadius)

                // Use key for stable identity
                key(queueItem.id) {
                    Box(
                        modifier = Modifier
                            .size(currentSize)
                            .offset(x = xOffset, y = yOffset)
                            .zIndex(zIndex)
                            .alpha(alpha)
                            .graphicsLayer { rotationZ = rotation }
                            .sentupCard(
                                shape = cardShape,
                                elevation = if (isCentered) 2 else 0,
                                tinted = false
                            )
                            .then(
                                if (isCentered) Modifier.sentupSpecularEdge(cardShape)
                                else Modifier
                            )
                            .then(
                                if (isCentered) {
                                    Modifier
                                        .pointerInput(Unit) {
                                            detectTapGestures(onTap = { onPlayPauseToggle() })
                                        }
                                        .styleWheelHold(styleWheel)
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        if (artSong != null) {
                            PlayerArtwork(
                                song = artSong,
                                motionEligible = isCentered,
                                contentDescription = song.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    modifier = Modifier.size(currentSize * 0.35f),
                                    tint = scheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                            }
                        }

                        // Play/Pause overlay only for centered album when paused/buffering
                        if (isCentered && songIndex == currentIndex && (isBuffering || !isPlaying)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isBuffering) {
                                    LoadingIndicator(
                                        modifier = Modifier.size(60.dp),
                                        color = Color.White,
                                        polygons = listOf(
                                            MaterialShapes.SoftBurst,
                                            MaterialShapes.Cookie9Sided,
                                            MaterialShapes.Pill,
                                            MaterialShapes.Sunny
                                        )
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.PlayArrow,
                                        contentDescription = stringResource(R.string.cd_play),
                                        modifier = Modifier.size(68.dp),
                                        tint = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Single Album Art (fallback when no queue). The same card as the carousel's
 * centred cover: [SentupRadii.panel], elevation 2, a specular edge, and a
 * press-in rather than a tint.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun SingleAlbumArt(
    song: Song?,
    isPlaying: Boolean,
    isBuffering: Boolean,
    onPlayPauseToggle: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val styleWheel = LocalPlayerStyleWheelController.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "SingleArtPress"
    )

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Guard against invalid dimensions during transitions
        // Use minimum usable size (not just zero) for high DPI compatibility
        if (maxWidth < 50.dp || maxHeight < 50.dp) {
            return@BoxWithConstraints
        }

        val albumSize = (minOf(maxWidth, maxHeight) * 0.98f).coerceAtLeast(1.dp)
        val artShape = RoundedCornerShape(SentupRadii.panel)

        Box(
            modifier = Modifier
                .size(albumSize)
                .graphicsLayer {
                    scaleX = press
                    scaleY = press
                }
                .sentupCard(shape = artShape, elevation = 2, tinted = false)
                .sentupSpecularEdge(artShape)
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { onPlayPauseToggle() })
                }
                .styleWheelHold(styleWheel)
        ) {
            val artSong = song?.takeIf { it.thumbnailUrl != null || it.albumArtUri != null }

            if (artSong != null) {
                PlayerArtwork(
                    song = artSong,
                    contentDescription = artSong.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(albumSize * 0.35f),
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                }
            }

            // Play/Pause overlay
            if (isBuffering || !isPlaying) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isBuffering) {
                        LoadingIndicator(
                            modifier = Modifier.size(64.dp),
                            color = Color.White,
                            polygons = listOf(
                                MaterialShapes.SoftBurst,
                                MaterialShapes.Cookie9Sided,
                                MaterialShapes.Pill,
                                MaterialShapes.Sunny
                            )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.PlayArrow,
                            contentDescription = stringResource(R.string.cd_play),
                            modifier = Modifier.size(72.dp),
                            tint = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}
