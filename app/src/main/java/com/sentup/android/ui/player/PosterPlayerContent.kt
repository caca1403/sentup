package com.sentup.android.ui.player

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.sentup.android.R
import com.sentup.android.data.isUnknownArtist
import com.sentup.android.data.isUnknownTitle
import com.sentup.android.data.PlaylistDisplayItem
import com.sentup.android.ui.components.LikeBurstIcon
import com.sentup.android.ui.theme.sentupPage
import com.sentup.android.ui.theme.sentupSpecularEdge

/** Where the cover stops being sharp and starts dissolving into the frosted foot. */
private const val CANVAS_ART_FRACTION = 0.70f

/**
 * Canvas Player - the full-bleed artwork style (replaces the old kinetic
 * type poster; the style key stays "poster").
 *
 * The album art IS the screen: edge-to-edge artwork with a quiet monochrome
 * UI floating over a bottom scrim. Nothing depends on how long the song
 * title is.
 *
 * Signature moves:
 * - The resting screen is pure artwork: while music plays the chrome slips
 *   away after a few seconds and a tap anywhere summons it back.
 * - Swiping the artwork horizontally skips; the art follows the finger,
 *   springs back if abandoned, and the next cover slides in from the swipe
 *   direction.
 * - All controls are white-on-scrim, so they read over any cover.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PosterPlayerSheetContent(
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
    // Back is handled once by ExpandablePlayer, which previews the collapse
    // as a gesture instead of firing at the end of one. A BackHandler here
    // would be registered later and silently win.

    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val playWhenReady by viewModel.playWhenReady.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val currentQueue by viewModel.playOrderQueue.collectAsState()
    val currentQueueItemId by viewModel.currentQueueItemId.collectAsState()
    val isFavorite by viewModel.isCurrentSongLiked.collectAsState()
    val lyricsResult by viewModel.lyricsResult.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()

    var showQueue by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showOptions by rememberNowPlayingOptionsOpen()

    val playerHaptics = rememberPlayerHaptics()
    val styleWheel = LocalPlayerStyleWheelController.current

    // Swipe-to-skip: the artwork follows the finger, springs back if the
    // drag is abandoned, and the incoming cover slides in from the swipe
    // direction. Canvas is full-bleed, so the gesture sits on the whole
    // screen and the song information is already inside it — the title block
    // only has to be told to move with it.
    var skipDirection by remember { mutableIntStateOf(1) }
    val swipeToSkip = rememberSwipeToSkip(
        onNext = { skipDirection = 1; playerHaptics.skip(); viewModel.skipToNext() },
        onPrevious = { skipDirection = -1; playerHaptics.skip(); viewModel.skipToPrevious() }
    )

    // Measured down the art box, not the screen: the cover is solid for most of its
    // height and gives out over the last fifth, so there is no seam to see.
    val artFootMask = remember {
        Brush.verticalGradient(
            0.72f to Color.Black,
            0.88f to Color.Black.copy(alpha = 0.35f),
            1f to Color.Transparent,
        )
    }

    // Monochrome UI over the art: white glyphs, black scrims. Reads over
    // any cover without fighting the artwork's own palette.
    val glyph = Color.White
    val scrim = Color.Black

    // Canvas is white type on black over the artwork, and the picker keeps
    // that: a light themed sheet under a full-bleed cover reads as a different
    // app opening on top of this one.
    val sleepTimer = rememberSleepTimerControl(
        viewModel = viewModel,
        accent = glyph,
        onAccent = scrim,
        container = scrim,
        onContainer = glyph
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .sentupPage()
    ) {
        Crossfade(targetState = showQueue, label = "CanvasQueueTransition") { queueVisible ->
            if (queueVisible) {
                SentupQueueView(
                    viewModel = viewModel,
                    onCollapse = onCollapse,
                    onBackToPlayer = { showQueue = false },
                    onLoadMore = onLoadMore
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        // Off while lyrics are up: that view scrolls and seeks.
                        .swipeToSkip(swipeToSkip, enabled = !showLyrics)
                        // Last in the chain: consumes the post-long-press
                        // hold stream before the tap and swipe detectors.
                        .styleWheelHold(styleWheel)
                ) {
                    // ========== THE FIELD ==========
                    // Built from the cover's own bottom edge so the join is continuous
                    // by construction - see CanvasField. ChromaticMistBackground was the
                    // obvious reuse and is wrong here twice over: it answers a null model
                    // with a flat rectangle, and it is fed highResThumbnailUrl, which is
                    // null for every device-library song, so it drew plain white. Its
                    // colours are also palette-derived rather than edge-derived, which
                    // cannot close a seam even when it does find artwork.
                    CanvasField(
                        song = currentSong,
                        joinAt = CANVAS_ART_FRACTION,
                        scrim = scrim,
                        modifier = Modifier.fillMaxSize()
                    )

                    // ========== THE CANVAS: the cover itself ==========
                    AnimatedContent(
                        targetState = currentSong,
                        transitionSpec = {
                            val dir = skipDirection
                            (slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) { (it / 3) * dir } + fadeIn()) togetherWith
                                (slideOutHorizontally(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioNoBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) { (-it / 3) * dir } + fadeOut())
                        },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(CANVAS_ART_FRACTION)
                            .graphicsLayer {
                                // Art moves less than the finger: at 0.5x the crop shows.
                                translationX = swipeToSkip.offset * 0.35f
                                // So the foot can be masked away without taking the
                                // field behind it out too.
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithContent {
                                drawContent()
                                drawRect(brush = artFootMask, blendMode = BlendMode.DstIn)
                            },
                        label = "CanvasSongSwitch"
                    ) { song ->
                        if (song != null && (song.thumbnailUrl != null || song.albumArtUri != null)) {
                            PlayerArtwork(
                                song = song,
                                contentDescription = stringResource(R.string.cd_album_art),
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MusicNote,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                    modifier = Modifier.size(120.dp)
                                )
                            }
                        }
                    }

                    // The chrome never leaves, so the scrim that keeps white glyphs
                    // legible over an arbitrary cover never leaves either. It also
                    // carries the whole bottom on API 30, where blur is a no-op.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    0f to scrim.copy(alpha = 0.35f),
                                    0.25f to Color.Transparent,
                                    0.55f to Color.Transparent,
                                    1f to scrim.copy(alpha = 0.75f)
                                )
                            )
                    )

                    // ========== LYRICS OVERLAY ==========
                    AnimatedVisibility(
                        visible = showLyrics,
                        enter = fadeIn(),
                        exit = fadeOut(),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(scrim.copy(alpha = 0.6f))
                        ) {
                            SyncedLyricsView(
                                lyricsResult = lyricsResult,
                                currentPositionMs = progress,
                                isPlaying = isPlaying,
                                onSeekTo = { viewModel.seekTo(it) },
                                primaryColor = glyph,
                                onSurfaceColor = glyph.copy(alpha = 0.9f),
                                onSurfaceVariantColor = glyph.copy(alpha = 0.6f)
                            )
                        }
                    }

                    // ========== TOP BAR ==========
                    Box(modifier = Modifier.align(Alignment.TopCenter)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 20.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            EditorialCircleButton(
                                onClick = onCollapse,
                                accent = scrim.copy(alpha = 0.35f),
                                field = glyph,
                                size = 44.dp
                            ) {
                                Icon(
                                    Icons.Default.KeyboardArrowDown, "Collapse",
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Solid fill while armed - the only contrast
                                // available over artwork is the scrim's alpha.
                                EditorialCircleButton(
                                    onClick = sleepTimer.open,
                                    accent = if (sleepTimer.active) glyph else scrim.copy(alpha = 0.35f),
                                    field = if (sleepTimer.active) scrim else glyph,
                                    size = 44.dp
                                ) {
                                    Icon(
                                        Icons.Rounded.Bedtime, "Sleep timer",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                EditorialCircleButton(
                                    onClick = { showQueue = true },
                                    accent = scrim.copy(alpha = 0.35f),
                                    field = glyph,
                                    size = 44.dp
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.QueueMusic, "Queue",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                                EditorialCircleButton(
                                    onClick = { showOptions = true },
                                    accent = scrim.copy(alpha = 0.35f),
                                    field = glyph,
                                    size = 44.dp
                                ) {
                                    Icon(
                                        Icons.Rounded.MoreVert, "More options",
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }

                    // ========== BOTTOM CLUSTER ==========
                    Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp)
                                .navigationBarsPadding()
                                .padding(bottom = 16.dp)
                        ) {
                            // Title + like/lyrics chips
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .swipeToSkipFollow(swipeToSkip)
                                ) {
                                    Text(
                                        text = currentSong?.title
                                            ?.takeIf { !isUnknownTitle(it) } ?: "Untitled",
                                        style = MaterialTheme.typography.headlineMedium.copy(
                                            fontWeight = FontWeight.Black
                                        ),
                                        color = glyph,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val artistName = currentSong?.artist
                                        ?.takeIf { !isUnknownArtist(it) } ?: "Unknown Artist"
                                    Text(
                                        text = artistName,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = glyph.copy(alpha = 0.75f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable(enabled = artistName != "Unknown Artist") {
                                                onArtistClick(artistName)
                                            }
                                            .padding(vertical = 2.dp)
                                    )
                                }
                                EditorialChip(
                                    checked = isFavorite,
                                    onClick = { viewModel.toggleCurrentSongLike() },
                                    accent = glyph,
                                    field = scrim
                                ) {
                                    LikeBurstIcon(isFavorite = isFavorite, iconSize = 20.dp)
                                }
                                EditorialChip(
                                    checked = showLyrics,
                                    onClick = { showLyrics = !showLyrics },
                                    accent = glyph,
                                    field = scrim
                                ) {
                                    Icon(Icons.Rounded.Lyrics, "Lyrics", modifier = Modifier.size(20.dp))
                                }
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            // Seek bar + times
                            var scrubFraction by remember { mutableStateOf<Float?>(null) }
                            // Canvas is the one style whose thumb is a real
                            // Material thumb rather than a drawn playhead, so it
                            // is where a once-a-second value was most visible.
                            // It follows the finger while scrubbing and the
                            // clock otherwise - see rememberSmoothProgress.
                            val smoothProgress = rememberSmoothProgress(
                                positionMs = progress,
                                durationMs = duration,
                                isPlaying = isPlaying,
                                scrubPositionMs = scrubFraction?.let { it * duration }
                            )
                            // Calibrated with the other styles' bar: 4dp rail
                            // inset in a thick touch strip, primary fill, and
                            // a thumb that grows when paused. The gesture
                            // stays on the (now invisible) Slider.
                            val posterScheme = MaterialTheme.colorScheme
                            val posterThumbRadius by animateDpAsState(
                                targetValue = if (isPlaying) 6.dp else 10.dp,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                label = "PosterThumbPause"
                            )
                            Box(contentAlignment = Alignment.Center) {
                                Canvas(modifier = Modifier.fillMaxWidth().height(22.dp)) {
                                    val fraction = smoothProgress.value.coerceIn(0f, 1f)
                                    val centerY = size.height / 2f
                                    val railWidth = 4.dp.toPx()
                                    drawLine(
                                        color = posterScheme.surfaceVariant.copy(alpha = 0.6f),
                                        start = Offset(0f, centerY),
                                        end = Offset(size.width, centerY),
                                        strokeWidth = railWidth,
                                        cap = StrokeCap.Round
                                    )
                                    if (fraction > 0f) {
                                        drawLine(
                                            color = glyph,
                                            start = Offset(0f, centerY),
                                            end = Offset(size.width * fraction, centerY),
                                            strokeWidth = railWidth,
                                            cap = StrokeCap.Round
                                        )
                                    }
                                    drawCircle(
                                        color = glyph,
                                        radius = posterThumbRadius.toPx(),
                                        center = Offset(size.width * fraction, centerY)
                                    )
                                }
                                Slider(
                                    value = smoothProgress.value,
                                    onValueChange = { scrubFraction = it },
                                    onValueChangeFinished = {
                                        scrubFraction?.let {
                                            if (duration > 0) viewModel.seekTo((it * duration).toLong())
                                        }
                                        scrubFraction = null
                                    },
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.Transparent,
                                        activeTrackColor = Color.Transparent,
                                        inactiveTrackColor = Color.Transparent
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = formatEditorialTime(progress),
                                    // Monospace: the clock ticks here, and a
                                    // proportional face makes the label jitter
                                    // twice a second.
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    ),
                                    color = glyph.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = formatEditorialTime(duration),
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp
                                    ),
                                    color = glyph.copy(alpha = 0.7f)
                                )
                            }

                            PlayerVisualizerSlot(modifier = Modifier.fillMaxWidth())

                            Spacer(modifier = Modifier.height(8.dp))

                            // Floating pill toolbar
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    modifier = Modifier.sentupSpecularEdge(CircleShape),
                                    shape = CircleShape,
                                    color = scrim.copy(alpha = 0.45f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        EditorialChip(
                                            checked = shuffleModeEnabled,
                                            onClick = { viewModel.toggleShuffle() },
                                            accent = glyph,
                                            field = scrim
                                        ) {
                                            Icon(
                                                Icons.Default.Shuffle, "Shuffle",
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        EditorialCircleButton(
                                            onClick = {
                                                skipDirection = -1
                                                playerHaptics.skip()
                                                viewModel.skipToPrevious()
                                            },
                                            accent = Color.Transparent,
                                            field = glyph,
                                            size = 52.dp
                                        ) {
                                            Icon(
                                                Icons.Default.SkipPrevious, "Previous",
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                        EditorialCircleButton(
                                            onClick = {
                                                playerHaptics.playPause(!viewModel.isPlaying.value)
                                                viewModel.togglePlayPause()
                                            },
                                            accent = glyph,
                                            field = scrim,
                                            size = 64.dp
                                        ) {
                                            if (isBuffering && playWhenReady && !isPlaying) {
                                                LoadingIndicator(
                                                    modifier = Modifier.size(28.dp),
                                                    color = scrim,
                                                    polygons = listOf(
                                                        MaterialShapes.SoftBurst,
                                                        MaterialShapes.Cookie9Sided,
                                                        MaterialShapes.Pill,
                                                        MaterialShapes.Sunny
                                                    )
                                                )
                                            } else {
                                                Icon(
                                                    if (isPlaying) Icons.Rounded.Pause
                                                    else Icons.Rounded.PlayArrow,
                                                    if (isPlaying) "Pause" else "Play",
                                                    modifier = Modifier.size(34.dp)
                                                )
                                            }
                                        }
                                        EditorialCircleButton(
                                            onClick = {
                                                skipDirection = 1
                                                playerHaptics.skip()
                                                viewModel.skipToNext()
                                            },
                                            accent = Color.Transparent,
                                            field = glyph,
                                            size = 52.dp
                                        ) {
                                            Icon(
                                                Icons.Default.SkipNext, "Next",
                                                modifier = Modifier.size(30.dp)
                                            )
                                        }
                                        EditorialChip(
                                            checked = repeatMode != Player.REPEAT_MODE_OFF,
                                            onClick = { viewModel.toggleRepeat() },
                                            accent = glyph,
                                            field = scrim
                                        ) {
                                            Icon(
                                                if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Default.RepeatOne
                                                else Icons.Default.Repeat,
                                                "Repeat",
                                                modifier = Modifier.size(20.dp)
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
    }

    // The overflow menu. Hosted here rather than above the style dispatch so
    // it sits inside this player's own composition, and gated on a song so the
    // sheet can never open against nothing.
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
