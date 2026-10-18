package com.sentup.android.ui.player

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.RepeatOne
import androidx.compose.material.icons.outlined.Shuffle
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import com.sentup.android.R
import com.sentup.android.data.Song
import com.sentup.android.data.isUnknownArtist
import com.sentup.android.data.isUnknownTitle
import com.sentup.android.ui.components.LikeBurstIcon
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupGroupCard
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.SentupRow
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage
import com.sentup.android.ui.theme.sentupSpecularEdge
import kotlin.math.abs

/**
 * Classic - the app's default style, rebuilt in the Sentüp surface language.
 *
 * What this style is *for*: the player you do not have to think about. Every
 * control is a real button in a real grid, the way it has been since the first
 * music player, and that identity is deliberately kept - five transport cells
 * with the loud one in the middle, a connected row of secondary toggles under
 * it, and the lyrics crossfading in place above the controls where you already
 * expect them.
 *
 * What changed is everything the buttons sit on. The flat two-tone spread is
 * gone: the page is [sentupPage], one floating capsule bar holds the chrome,
 * the cover is a [sentupCard] with a specular edge at elevation 2, and every
 * button presses *inward* (scale, no ripple) the way a pressed
 * [SentupRow] does. The times are monospace so they do not jitter as the
 * digits change, and the reachable actions are collected into one inset
 * [SentupGroupCard] the way iOS collects rows.
 *
 * Nothing about playback moved. The view model is read exactly as before and
 * every control still commits through the same call.
 */
@Composable
fun PlayerSheetContent(
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
    // tone now comes from sentupPage() like every other style, so the ambient
    // wash no longer paints behind it.
    val currentSong by viewModel.currentSong.collectAsState()

    var showQueue by remember { mutableStateOf(false) }
    var showOptions by rememberNowPlayingOptionsOpen()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .sentupPage()
    ) {
        Crossfade(targetState = showQueue, label = "ClassicQueueTransition") { queueVisible ->
            if (queueVisible) {
                SentupQueueView(
                    viewModel = viewModel,
                    onCollapse = onCollapse,
                    onBackToPlayer = { showQueue = false },
                    onLoadMore = onLoadMore
                )
            } else {
                ClassicNowPlayingView(
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
 * The now-playing page: a floating capsule bar over a scrollable column of
 * cover (or lyrics), title, progress, the classic five-cell transport grid, a
 * connected row of secondary toggles, and one inset group card of the
 * remaining actions.
 */
@Composable
private fun ClassicNowPlayingView(
    viewModel: PlayerViewModel,
    onCollapse: () -> Unit,
    onShowQueue: () -> Unit,
    onShowOptions: () -> Unit,
    onArtistClick: (String) -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val playWhenReady by viewModel.playWhenReady.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isFavorite by viewModel.isCurrentSongLiked.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val playOrder by viewModel.playOrderQueue.collectAsState()
    val lyricsResult by viewModel.lyricsResult.collectAsState()
    val playerHaptics = rememberPlayerHaptics()

    val isDownloaded = currentSong?.let { viewModel.isDownloaded(it.id) } ?: false
    val isDownloading = currentSong?.let { viewModel.isDownloading(it.id) } ?: false
    val isLocalOriginal = currentSong?.let { viewModel.isLocalOriginal(it) } ?: false

    val sleepTimer = rememberSleepTimerControl(viewModel = viewModel)
    var showLyrics by remember { mutableStateOf(false) }
    var showSpeedDialog by remember { mutableStateOf(false) }

    val swipeToSkip = rememberSwipeToSkip(
        onNext = { playerHaptics.skip(); viewModel.skipToNext() },
        onPrevious = { playerHaptics.skip(); viewModel.skipToPrevious() }
    )

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // Wide enough to dominate, never so tall that the transport slides
            // off a short screen; under the floor the column simply scrolls.
            val artSize = minOf(maxWidth * 0.80f, (maxHeight - 530.dp).coerceAtLeast(200.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    // Clear the floating bar; the bar is aligned on top of
                    // this Box and does not scroll.
                    .padding(top = 76.dp)
                    .padding(horizontal = SentupGroup.inset)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // ========== ARTWORK / LYRICS ==========
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .swipeToSkip(swipeToSkip, enabled = !showLyrics),
                    contentAlignment = Alignment.Center
                ) {
                    Crossfade(targetState = showLyrics, label = "ClassicArtLyrics") { lyricsVisible ->
                        if (lyricsVisible) {
                            SyncedLyricsView(
                                lyricsResult = lyricsResult,
                                currentPositionMs = progress,
                                isPlaying = isPlaying,
                                onSeekTo = { viewModel.seekTo(it) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(artSize)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(artSize)
                                    .graphicsLayer {
                                        translationX = swipeToSkip.offset * SwipeToSkipDefaults.ArtFollow
                                    }
                            ) {
                                ClassicArtwork(
                                    currentSong = currentSong,
                                    onTap = {
                                        playerHaptics.playPause(!viewModel.isPlaying.value)
                                        viewModel.togglePlayPause()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(SentupGroup.gap))

                // ========== TITLE / ARTIST ==========
                val title = currentSong?.title?.takeIf { !isUnknownTitle(it) }
                    ?: stringResource(R.string.np_art_untitled)
                val artistName = currentSong?.artist?.takeIf { !isUnknownArtist(it) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .swipeToSkipFollow(swipeToSkip),
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
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
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

                Spacer(modifier = Modifier.height(20.dp))

                // ========== PROGRESS ==========
                ClassicProgressBlock(
                    progress = progress,
                    duration = duration,
                    isPlaying = isPlaying,
                    onSeekTo = { viewModel.seekTo(it) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(18.dp))

                PlayerVisualizerSlot(modifier = Modifier.fillMaxWidth())

                // ========== CLASSIC TRANSPORT GRID ==========
                // The identity of this style: five cells, loud one in the
                // middle. The four small ones are floating capsules on the
                // page; the play cell is the one raised, filled object.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClassicCapsuleButton(
                            icon = Icons.Outlined.Shuffle,
                            contentDescription = stringResource(R.string.cd_shuffle),
                            active = shuffleModeEnabled,
                            onClick = { viewModel.toggleShuffle() }
                        )
                        ClassicCapsuleButton(
                            icon = Icons.Outlined.SkipPrevious,
                            contentDescription = stringResource(R.string.cd_previous),
                            onClick = { playerHaptics.skip(); viewModel.skipToPrevious() }
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    ClassicPlayDisc(
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        playWhenReady = playWhenReady,
                        onClick = {
                            playerHaptics.playPause(!viewModel.isPlaying.value)
                            viewModel.togglePlayPause()
                        }
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ClassicCapsuleButton(
                            icon = Icons.Outlined.SkipNext,
                            contentDescription = stringResource(R.string.cd_next),
                            onClick = { playerHaptics.skip(); viewModel.skipToNext() }
                        )
                        ClassicCapsuleButton(
                            icon = if (repeatMode == Player.REPEAT_MODE_ONE) {
                                Icons.Outlined.RepeatOne
                            } else {
                                Icons.Outlined.Repeat
                            },
                            contentDescription = stringResource(R.string.cd_repeat),
                            active = repeatMode != Player.REPEAT_MODE_OFF,
                            onClick = { viewModel.toggleRepeat() }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                // ========== CONNECTED SECONDARY ROW ==========
                // Still one connected strip of three, but the cells are now
                // objects on the page rather than slices of a container.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ClassicCapsuleButton(
                        icon = null,
                        contentDescription = stringResource(R.string.like),
                        active = isFavorite,
                        onClick = {
                            playerHaptics.confirm()
                            viewModel.toggleCurrentSongLike()
                        },
                        modifier = Modifier.weight(1f),
                        glyph = {
                            LikeBurstIcon(
                                isFavorite = isFavorite,
                                iconSize = 22.dp,
                                tint = if (isFavorite) scheme.primary else scheme.onSurface
                            )
                        }
                    )
                    ClassicCapsuleButton(
                        icon = when {
                            isLocalOriginal -> Icons.Rounded.Smartphone
                            isDownloaded -> Icons.Rounded.CheckCircle
                            else -> Icons.Rounded.Download
                        },
                        contentDescription = when {
                            isLocalOriginal -> stringResource(R.string.cd_local_file)
                            isDownloaded -> stringResource(R.string.song_options_downloaded)
                            else -> stringResource(R.string.song_options_download)
                        },
                        active = isDownloaded,
                        enabled = !isDownloading && !isLocalOriginal,
                        onClick = { currentSong?.let { viewModel.toggleDownload(it) } },
                        modifier = Modifier.weight(1f),
                        glyph = if (isDownloading) {
                            {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = scheme.primary,
                                    strokeWidth = 2.dp
                                )
                            }
                        } else {
                            null
                        }
                    )
                    ClassicCapsuleButton(
                        icon = Icons.Rounded.Bedtime,
                        contentDescription = stringResource(R.string.sleep_timer_title),
                        active = sleepTimer.active,
                        onClick = sleepTimer.open,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(SentupGroup.gap))

                // ========== ACTIONS (one inset group card) ==========
                SentupGroupCard {
                    SentupRow(
                        title = stringResource(R.string.ps_up_next),
                        icon = Icons.AutoMirrored.Filled.QueueMusic,
                        value = playOrder.size.toString(),
                        showChevron = true,
                        onClick = onShowQueue
                    )
                    SentupDivider()
                    SentupRow(
                        title = stringResource(R.string.sleep_timer_title),
                        icon = Icons.Rounded.Bedtime,
                        value = if (sleepTimer.active) null else {
                            stringResource(R.string.sleep_timer_off)
                        },
                        tint = if (sleepTimer.active) scheme.primary else null,
                        showChevron = true,
                        onClick = sleepTimer.open
                    )
                    SentupDivider()
                    SentupRow(
                        title = stringResource(R.string.np_row_playback_speed),
                        icon = Icons.Rounded.Speed,
                        value = formatClassicSpeed(playbackSpeed),
                        showChevron = true,
                        onClick = { showSpeedDialog = true }
                    )
                }

                Spacer(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(8.dp)
                )
            }
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
            // The one Classic keeps that the other styles do not have: the
            // centre label is a toggle, and the lyrics crossfade in place
            // rather than arriving as a separate screen.
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(SentupRadii.pill))
                    .clickable { showLyrics = !showLyrics }
                    .padding(horizontal = 14.dp, vertical = 7.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Crossfade(targetState = showLyrics, label = "ClassicBarToggle") { lyricsVisible ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Icon(
                            imageVector = if (lyricsVisible) {
                                Icons.Rounded.Lyrics
                            } else {
                                Icons.Rounded.GraphicEq
                            },
                            contentDescription = null,
                            tint = if (lyricsVisible) scheme.primary else scheme.onSurface,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = if (lyricsVisible) {
                                stringResource(R.string.ps_lyrics)
                            } else {
                                stringResource(R.string.now_playing_channel_name)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (lyricsVisible) scheme.primary else scheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
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

    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.np_row_playback_speed),
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Column {
                    listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                        SentupRow(
                            title = formatClassicSpeed(speed),
                            onClick = {
                                viewModel.setPlaybackSpeed(speed)
                                showSpeedDialog = false
                            },
                            trailing = {
                                if (abs(playbackSpeed - speed) < 0.01f) {
                                    Icon(
                                        Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = scheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}

/**
 * The cover: a large rounded solid at [SentupRadii.panel] with elevation 2 and
 * a specular edge. Pressing it pushes it inward (0.97) instead of tinting -
 * the same contract as a pressed [SentupRow]. A tap toggles playback; holding
 * it summons the style wheel, as in every other style.
 */
@Composable
private fun ClassicArtwork(
    currentSong: Song?,
    onTap: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val artShape = RoundedCornerShape(SentupRadii.panel)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "ClassicArtPress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .sentupCard(shape = artShape, elevation = 2, tinted = false)
            .sentupSpecularEdge(artShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onTap
            )
            .styleWheelHold(LocalPlayerStyleWheelController.current),
        contentAlignment = Alignment.Center
    ) {
        val artSong = currentSong?.takeIf { it.thumbnailUrl != null || it.albumArtUri != null }
        if (artSong != null) {
            PlayerArtwork(
                song = artSong,
                contentDescription = stringResource(R.string.cd_album_art),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                imageVector = Icons.Rounded.MusicNote,
                contentDescription = null,
                tint = scheme.onSurfaceVariant.copy(alpha = 0.3f),
                modifier = Modifier.size(96.dp)
            )
        }
    }
}

/**
 * A thin round progress line: hairline track, primary played side, one small
 * thumb that swells under the finger. Scrubbing is local and the seek lands on
 * release; the drag travels through [LocalScrubReturnPoint] so letting go
 * where the drag started means "never mind". The two times are monospace so
 * the digits do not shift the row as they change.
 */
@Composable
private fun ClassicProgressBlock(
    progress: Long,
    duration: Long,
    isPlaying: Boolean,
    onSeekTo: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val scheme = MaterialTheme.colorScheme
    val scrubInteraction = LocalPlayerScrubInteraction.current
    val dragged by scrubInteraction.collectIsDraggedAsState()
    val pressed by scrubInteraction.collectIsPressedAsState()
    val bloom by animateFloatAsState(
        targetValue = if (dragged || pressed) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ClassicThumbBloom"
    )
    val pauseBloom by animateFloatAsState(
        targetValue = if (isPlaying) 0f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ClassicThumbPause"
    )
    var scrubPosition by remember { mutableStateOf<Float?>(null) }
    val returnPoint = LocalScrubReturnPoint.current
    // Advanced from the clock each frame rather than sprung toward a
    // once-a-second sample - see rememberSmoothProgress. Read inside the Canvas
    // lambda, i.e. in the draw phase, which is where it may be read.
    val smoothProgress = rememberSmoothProgress(
        positionMs = progress,
        durationMs = duration,
        isPlaying = isPlaying,
        scrubPositionMs = scrubPosition
    )

    Column(modifier = modifier) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.fillMaxWidth().height(22.dp)) {
                val fraction = smoothProgress.value.coerceIn(0f, 1f)
                val centerY = size.height / 2f
                val trackWidth = 4.dp.toPx()
                drawLine(
                    color = scheme.surfaceVariant.copy(alpha = 0.6f),
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = trackWidth,
                    cap = StrokeCap.Round
                )
                if (fraction > 0f) {
                    drawLine(
                        color = scheme.primary,
                        start = Offset(0f, centerY),
                        end = Offset(size.width * fraction, centerY),
                        strokeWidth = trackWidth,
                        cap = StrokeCap.Round
                    )
                }
                // 12dp thumb while playing, 20dp while paused, a little more
                // while the finger owns it.
                drawCircle(
                    color = scheme.primary,
                    radius = (6.dp + 4.dp * pauseBloom + 3.dp * bloom).toPx(),
                    center = Offset(size.width * fraction, centerY)
                )
            }
            // Invisible Slider owns the gesture and publishes it to the
            // interaction source the bar above reads its bloom from.
            Slider(
                interactionSource = scrubInteraction,
                value = scrubPosition ?: progress.toFloat(),
                onValueChange = {
                    scrubPosition = returnPoint.follow(
                        value = it,
                        rangeEnd = duration.toFloat().coerceAtLeast(1f)
                    ) { smoothProgress.value }
                },
                onValueChangeFinished = {
                    val cancelled = returnPoint.release()
                    if (!cancelled) scrubPosition?.let { onSeekTo(it.toLong()) }
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
            modifier = Modifier.fillMaxWidth(),
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

/**
 * One cell of the classic grid: a floating capsule, glyph only. [active] turns
 * the glyph emerald, which is the only state that reads; the press scales the
 * capsule inward rather than flashing it.
 */
@Composable
private fun ClassicCapsuleButton(
    icon: ImageVector?,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    enabled: Boolean = true,
    glyph: (@Composable () -> Unit)? = null
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(SentupRadii.pill)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 100),
        label = "ClassicCellPress"
    )

    Box(
        modifier = modifier
            .height(56.dp)
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .sentupCard(shape = shape, elevation = 1, tinted = false)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (glyph != null) {
            glyph()
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = if (active) scheme.primary else scheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * The one loud object in the transport: an emerald disc lifted by a coloured
 * shadow and finished with the page's specular edge. It presses inward rather
 * than flashing, same contract as everything else on the page.
 */
@Composable
private fun ClassicPlayDisc(
    isPlaying: Boolean,
    isBuffering: Boolean,
    playWhenReady: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.94f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "ClassicPlayPress"
    )

    Box(
        modifier = Modifier
            .size(84.dp)
            .graphicsLayer {
                scaleX = press
                scaleY = press
            }
            .shadow(
                elevation = 22.dp,
                shape = CircleShape,
                ambientColor = scheme.primary.copy(alpha = 0.35f),
                spotColor = scheme.primary.copy(alpha = 0.55f)
            )
            .clip(CircleShape)
            .background(scheme.primary)
            .sentupSpecularEdge(CircleShape)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isBuffering && playWhenReady && !isPlaying) {
            CircularProgressIndicator(
                modifier = Modifier.size(34.dp),
                color = scheme.onPrimary,
                strokeWidth = 3.dp
            )
        } else {
            Icon(
                imageVector = if (isPlaying) Icons.Outlined.Pause else Icons.Outlined.PlayArrow,
                contentDescription = if (isPlaying) {
                    stringResource(R.string.cd_pause)
                } else {
                    stringResource(R.string.cd_play)
                },
                tint = scheme.onPrimary,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

/** "1x" for whole speeds, "1.25x" otherwise; the speed row's value and dialog labels. */
private fun formatClassicSpeed(speed: Float): String =
    if (speed == speed.toLong().toFloat()) "${speed.toLong()}x" else "${speed}x"