package com.sentup.android.ui.player

import com.sentup.android.ui.components.DismissibleSnackbarHost
import androidx.compose.ui.res.stringResource
import com.sentup.android.R
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
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import androidx.media3.common.Player
import com.sentup.android.data.MusicQueueItem
import com.sentup.android.data.Song
import com.sentup.android.data.isUnknownArtist
import com.sentup.android.data.isUnknownTitle
import com.sentup.android.data.queuePlaylistDateText
import com.sentup.android.ui.components.LikeBurstIcon
import com.sentup.android.ui.components.QueueDragHandle
import com.sentup.android.ui.components.QueueRowContainer
import com.sentup.android.ui.components.SongArtwork
import com.sentup.android.ui.components.queueDragLongPress
import com.sentup.android.ui.components.rememberQueueRemoval
import com.sentup.android.ui.components.rememberQueueReorderState
import com.sentup.android.ui.components.rememberFocusedQueueListState
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupGroupCard
import com.sentup.android.ui.theme.SentupGroupHeader
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.SentupRow
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage
import com.sentup.android.ui.theme.sentupSpecularEdge
import kotlin.math.abs

/**
 * Now Playing - the Sentüp surface language end to end.
 *
 * The screen is built from the app's own depth vocabulary rather than the old
 * flat two-tone magazine spread: [sentupPage] behind everything, one floating
 * capsule bar on top, the cover as a large rounded solid with a specular edge,
 * and the reachable actions collected into a single inset group card the way
 * iOS collects rows - [SentupRow] hairlines inset to the text, whole row is
 * the target, press scales inward instead of flashing a colour.
 *
 * Spacing is the design: blocks breathe [SentupGroup.gap] apart, the group
 * card keeps [SentupGroup.inset] from the page edge, and nothing overlaps -
 * on a short screen the artwork shrinks toward its floor before the column
 * scrolls, so the transport controls stay on screen.
 *
 * Shared internals at the bottom of this file ([EditorialCircleButton],
 * [EditorialChip], [EditorialQueueView], [EditorialMorphShape],
 * [EditorialPolygonShape], [formatEditorialTime]) are still consumed by the
 * other player styles; they remain here verbatim.
 */
@Composable
fun EditorialPlayerSheetContent(
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
    //
    // ambientBackground and onLoadMore are kept in the signature for style
    // parity: the page tone comes from sentupPage() now, and the queue view
    // reaches loadMoreRecommendations through the same view model.
    val currentSong by viewModel.currentSong.collectAsState()

    var showQueue by remember { mutableStateOf(false) }
    var showOptions by rememberNowPlayingOptionsOpen()

    // The picker sheet is emitted by the helper itself; the theme defaults are
    // the whole design now, so it arrives in the app's own colours.
    val sleepTimer = rememberSleepTimerControl(viewModel = viewModel)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .sentupPage()
    ) {
        Crossfade(targetState = showQueue, label = "SentupQueueTransition") { queueVisible ->
            if (queueVisible) {
                SentupQueueView(
                    viewModel = viewModel,
                    onCollapse = onCollapse,
                    onBackToPlayer = { showQueue = false }
                )
            } else {
                SentupNowPlayingView(
                    viewModel = viewModel,
                    onShowQueue = { showQueue = true },
                    onShowOptions = { showOptions = true },
                    onShowSleepTimer = sleepTimer.open,
                    sleepTimerActive = sleepTimer.active,
                    onCollapse = onCollapse,
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
 * The now-playing page: floating capsule bar over a scrollable column of
 * cover, title, progress, transport, the next few songs, and one group card
 * of actions. The column can scroll on short screens; the bar floats above it
 * and never moves.
 */
@Composable
private fun SentupNowPlayingView(
    viewModel: PlayerViewModel,
    onShowQueue: () -> Unit,
    onShowOptions: () -> Unit,
    onShowSleepTimer: () -> Unit,
    sleepTimerActive: Boolean,
    onCollapse: () -> Unit,
    onArtistClick: (String) -> Unit
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val playerHaptics = rememberPlayerHaptics()
    val isBuffering by viewModel.isBuffering.collectAsState()
    val playWhenReady by viewModel.playWhenReady.collectAsState()
    val progress by viewModel.progress.collectAsState()
    val duration by viewModel.duration.collectAsState()
    val shuffleModeEnabled by viewModel.shuffleModeEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val isFavorite by viewModel.isCurrentSongLiked.collectAsState()
    val playOrder by viewModel.playOrderQueue.collectAsState()
    val currentQueueItemId by viewModel.currentQueueItemId.collectAsState()
    val playbackSpeed by viewModel.playbackSpeed.collectAsState()
    val scheme = MaterialTheme.colorScheme
    // The shared lyrics screen's flag, read here only to tint the capsule
    // bar's button; the screen itself lives above the styles in
    // ExpandablePlayer.
    val lyricsOpen = LocalLyricsOpen.current

    var showSpeedDialog by remember { mutableStateOf(false) }

    // The few songs that actually come next, in play order, for the strip.
    val upNext = remember(playOrder, currentQueueItemId) {
        val index = playOrder.indexOfFirst { it.id == currentQueueItemId }
        playOrder.drop(index + 1).take(6)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            // The design's share of the width (0.78), but never so tall that
            // the transport slides off a short screen; below the floor the
            // column simply scrolls.
            val artSize = minOf(maxWidth * 0.78f, (maxHeight - 460.dp).coerceAtLeast(216.dp))

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    // Clear the floating bar; the bar itself is aligned on top
                    // of this Box and does not scroll.
                    .padding(top = 76.dp)
                    .padding(bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Swipe left/right on the cover moves to the previous/next
                // song; the title follows the same gesture state.
                val swipeToSkip = rememberSwipeToSkip(
                    onNext = { playerHaptics.skip(); viewModel.skipToNext() },
                    onPrevious = { playerHaptics.skip(); viewModel.skipToPrevious() }
                )

                // ========== ARTWORK ==========
                // The lyrics are not drawn here any more: they are a screen of
                // their own, opened from the capsule bar above (and from the
                // mini pill's long press). Keeping a second, in-place copy of
                // the same lyrics would be two places that can disagree.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .swipeToSkip(swipeToSkip),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(artSize)
                            .graphicsLayer {
                                translationX = swipeToSkip.offset * SwipeToSkipDefaults.ArtFollow
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        SentupArtwork(
                            currentSong = currentSong,
                            onTap = {
                                playerHaptics.playPause(!viewModel.isPlaying.value)
                                viewModel.togglePlayPause()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(SentupGroup.gap))

                // ========== TITLE / ARTIST ==========
                val title = currentSong?.title?.takeIf { !isUnknownTitle(it) } ?: stringResource(R.string.np_art_untitled)
                val artistName = currentSong?.artist?.takeIf { !isUnknownArtist(it) }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .swipeToSkipFollow(swipeToSkip)
                        .padding(horizontal = SentupGroup.inset)
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
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(SentupRadii.pill))
                            .clickable(enabled = artistName != null) {
                                artistName?.let(onArtistClick)
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // ========== PROGRESS ==========
                SentupProgressBlock(
                    progress = progress,
                    duration = duration,
                    isPlaying = isPlaying,
                    onSeekTo = { viewModel.seekTo(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SentupGroup.inset + 4.dp)
                )

                Spacer(modifier = Modifier.height(22.dp))

                // ========== TRANSPORT ==========
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SentupControlButton(
                        icon = Icons.Outlined.Shuffle,
                        contentDescription = "Shuffle",
                        active = shuffleModeEnabled,
                        onClick = { viewModel.toggleShuffle() }
                    )
                    SentupControlButton(
                        icon = Icons.Outlined.SkipPrevious,
                        contentDescription = "Previous",
                        onClick = { playerHaptics.skip(); viewModel.skipToPrevious() },
                        buttonSize = 56.dp,
                        glyphSize = 28.dp
                    )
                    SentupPlayButton(
                        isPlaying = isPlaying,
                        isBuffering = isBuffering,
                        playWhenReady = playWhenReady,
                        onClick = {
                            playerHaptics.playPause(!viewModel.isPlaying.value)
                            viewModel.togglePlayPause()
                        }
                    )
                    SentupControlButton(
                        icon = Icons.Outlined.SkipNext,
                        contentDescription = "Next",
                        onClick = { playerHaptics.skip(); viewModel.skipToNext() },
                        buttonSize = 56.dp,
                        glyphSize = 28.dp
                    )
                    SentupControlButton(
                        icon = if (repeatMode == Player.REPEAT_MODE_ONE) Icons.Outlined.RepeatOne
                        else Icons.Outlined.Repeat,
                        contentDescription = "Repeat",
                        active = repeatMode != Player.REPEAT_MODE_OFF,
                        onClick = { viewModel.toggleRepeat() }
                    )
                }

                Spacer(modifier = Modifier.height(SentupGroup.gap))

                // ========== UP NEXT STRIP ==========
                if (upNext.isNotEmpty()) {
                    SentupGroupHeader(text = stringResource(R.string.ps_up_next))
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = SentupGroup.inset),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(upNext, key = { item -> item.id }) { item ->
                            SentupUpNextTile(
                                song = item.song,
                                onClick = { viewModel.skipToQueueItem(item.id) }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(SentupGroup.gap))
                }

                // ========== ACTIONS (one inset group card) ==========
                // Lyrics is not a row here: it is a screen opened from the
                // capsule bar, so it does not sit below the fold of this
                // scroll where a user would not find it. The card keeps the
                // rest of the actions.
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
                        value = if (sleepTimerActive) null else stringResource(R.string.sleep_timer_off),
                        tint = if (sleepTimerActive) scheme.primary else null,
                        showChevron = true,
                        onClick = onShowSleepTimer
                    )
                    SentupDivider()
                    SentupRow(
                        title = stringResource(R.string.np_row_playback_speed),
                        icon = Icons.Rounded.Speed,
                        value = formatSentupSpeed(playbackSpeed),
                        showChevron = true,
                        onClick = { showSpeedDialog = true }
                    )
                    SentupDivider()
                    SentupRow(
                        title = stringResource(R.string.like),
                        tint = if (isFavorite) scheme.primary else null,
                        onClick = {
                            playerHaptics.confirm()
                            viewModel.toggleCurrentSongLike()
                        },
                        trailing = {
                            LikeBurstIcon(
                                isFavorite = isFavorite,
                                iconSize = 22.dp,
                                tint = if (isFavorite) scheme.primary else scheme.onSurfaceVariant
                            )
                        }
                    )
                    SentupDivider()
                    val isDownloaded = currentSong?.let { viewModel.isDownloaded(it.id) } ?: false
                    val isDownloading = currentSong?.let { viewModel.isDownloading(it.id) } ?: false
                    val isLocalOriginal = currentSong?.let { viewModel.isLocalOriginal(it) } ?: false
                    SentupRow(
                        title = when {
                            isLocalOriginal -> stringResource(R.string.np_row_local_file)
                            isDownloading -> stringResource(R.string.np_tile_downloading)
                            isDownloaded -> stringResource(R.string.np_tile_downloaded)
                            else -> stringResource(R.string.np_tile_download)
                        },
                        icon = when {
                            isLocalOriginal -> Icons.Rounded.Smartphone
                            isDownloaded -> Icons.Rounded.CheckCircle
                            else -> Icons.Rounded.Download
                        },
                        enabled = !isDownloading && !isLocalOriginal,
                        tint = if (isDownloaded) scheme.primary else null,
                        onClick = { currentSong?.let { viewModel.toggleDownload(it) } }
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
                    Icons.Default.KeyboardArrowDown,
                    contentDescription = "Collapse",
                    tint = scheme.onSurface,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = stringResource(R.string.now_playing_channel_name),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = scheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
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
            IconButton(onClick = onShowOptions) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "More options",
                    tint = scheme.onSurface,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }

    if (showSpeedDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialog = false },
            title = { Text(stringResource(R.string.np_row_playback_speed), style = MaterialTheme.typography.titleMedium) },
            text = {
                Column {
                    listOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { speed ->
                        SentupRow(
                            title = formatSentupSpeed(speed),
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
 * The cover: the page's largest object. It sits on [sentupCard] depth with a
 * specular edge, and pressing it pushes it inward (0.97) instead of tinting -
 * same contract as a pressed [SentupRow]. A tap toggles playback; holding it
 * summons the style wheel, as in every other style.
 */
@Composable
private fun SentupArtwork(
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
        label = "SentupArtPress"
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
                contentDescription = "Album Art",
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
 * A thin, round, draggable progress line: hairline track, primary played
 * side, one small thumb that swells under the finger. Scrubbing is local and
 * the seek lands on release (streamed tracks rebuffer if seeked per frame);
 * the drag travels through [LocalScrubReturnPoint] so letting go where the
 * drag started means "never mind", same as the rest of the app.
 */
@Composable
private fun SentupProgressBlock(
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
        label = "SentupThumbBloom"
    )
    var scrubPosition by remember { mutableStateOf<Float?>(null) }
    val returnPoint = LocalScrubReturnPoint.current
    // Advanced from the clock each frame rather than sprung toward a
    // once-a-second sample - see rememberSmoothProgress. Read inside the
    // Canvas lambda, i.e. in the draw phase, which is where it may be read.
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
                    color = scheme.onSurfaceVariant.copy(alpha = 0.16f),
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
                drawCircle(
                    color = scheme.primary,
                    radius = (5.dp + 3.dp * bloom).toPx(),
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
                    fontFamily = FontFamily.Monospace
                ),
                color = scheme.onSurfaceVariant
            )
            Text(
                text = formatEditorialTime(duration),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = FontFamily.Monospace
                ),
                color = scheme.onSurfaceVariant
            )
        }
    }
}

/** Secondary transport button: outline glyph, primary only when the mode is on. */
@Composable
private fun SentupControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    active: Boolean = false,
    buttonSize: androidx.compose.ui.unit.Dp = 48.dp,
    glyphSize: androidx.compose.ui.unit.Dp = 24.dp
) {
    val scheme = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(buttonSize)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) scheme.primary else scheme.onSurfaceVariant,
            modifier = Modifier.size(glyphSize)
        )
    }
}

/**
 * The one loud object in the transport: a primary-filled circle lifted by a
 * coloured shadow and finished with the page's specular edge. It presses
 * inward rather than flashing, same contract as everything else on the page.
 */
@Composable
private fun SentupPlayButton(
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
        label = "SentupPlayPress"
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
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint = scheme.onPrimary,
                modifier = Modifier.size(42.dp)
            )
        }
    }
}

/** One card in the horizontal up-next strip: tile-radius cover over its two lines. */
@Composable
private fun SentupUpNextTile(
    song: Song,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val tileShape = RoundedCornerShape(SentupRadii.tile)
    Column(
        modifier = Modifier
            .width(128.dp)
            .sentupCard(shape = tileShape, elevation = 1)
            .clip(tileShape)
            .clickable(onClick = onClick)
    ) {
        Box(modifier = Modifier.size(128.dp)) {
            val artSong = song.takeIf { it.thumbnailUrl != null || it.albumArtUri != null }
            if (artSong != null) {
                SongArtwork(
                    song = artSong,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(scheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.35f),
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 8.dp)
        )
        Text(
            text = song.artist,
            style = MaterialTheme.typography.labelSmall,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 10.dp, end = 10.dp, top = 1.dp, bottom = 10.dp)
        )
    }
}

/**
 * The queue as the rest of the app reads: one inset card holding inset rows,
 * hairlines to the text, the playing row tinted rather than inverted. Drag,
 * swipe-away, undo and save-as-playlist behave exactly as they did; only the
 * page they live on changed.
 */
@Composable
private fun SentupQueueView(
    viewModel: PlayerViewModel,
    onCollapse: () -> Unit,
    onBackToPlayer: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val queue by viewModel.playOrderQueue.collectAsState()
    val currentQueueItemId by viewModel.currentQueueItemId.collectAsState()
    val isLoadingMore by viewModel.isLoadingMore.collectAsState()
    val currentQueueIndex = remember(queue, currentQueueItemId) {
        queue.indexOfFirst { it.id == currentQueueItemId }
    }
    val listState = rememberFocusedQueueListState(currentQueueIndex)
    val rowKeys = remember(queue) { queue.map { it.id } }
    val reorder = rememberQueueReorderState(
        listState = listState,
        keys = rowKeys,
        onMove = { from, to -> viewModel.movePlayOrderItem(from, to, persist = false) },
        onSettle = { viewModel.commitQueueOrder() }
    )
    val removal = rememberQueueRemoval(onUndo = { viewModel.undoQueueRemoval() })
    var showSaveDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .sentupPage()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = SentupGroup.inset)
                .padding(top = 8.dp)
        ) {
            // The same floating capsule as the now-playing page.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .sentupCard(shape = CircleShape, elevation = 1)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackToPlayer) {
                    Icon(
                        Icons.Rounded.MusicNote,
                        contentDescription = "Now Playing",
                        tint = scheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Text(
                    text = stringResource(R.string.ps_up_next),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = { showSaveDialog = true },
                    enabled = queue.isNotEmpty()
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = stringResource(R.string.queue_save_as_playlist),
                        tint = scheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onCollapse) {
                    Icon(
                        Icons.Default.KeyboardArrowDown,
                        contentDescription = "Collapse",
                        tint = scheme.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (queue.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = stringResource(R.string.np_queue_empty),
                        style = MaterialTheme.typography.titleMedium,
                        color = scheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(
                        start = 4.dp,
                        end = 4.dp,
                        top = 2.dp,
                        bottom = 24.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    itemsIndexed(
                        queue,
                        key = { index, _ -> rowKeys.getOrElse(index) { "sentup_queue_$index" } }
                    ) { index, queueItem ->
                        val song = queueItem.song
                        val isCurrent = queueItem.id == currentQueueItemId
                        val key = rowKeys.getOrElse(index) { "sentup_queue_$index" }
                        val isDragging = reorder.draggingKey == key
                        val rowShape = RoundedCornerShape(SentupRadii.tile)

                        QueueRowContainer(
                            isDragging = isDragging,
                            dragOffset = reorder.offsetFor(key),
                            removeEnabled = queue.size > 1,
                            onRemove = {
                                viewModel.removeQueueItem(queueItem.id)
                                removal.onRemoved(song.title)
                            },
                            modifier = if (isDragging) Modifier else Modifier.animateItem()
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(rowShape)
                                    .background(
                                        if (isCurrent) scheme.primary.copy(alpha = 0.10f)
                                        else Color.Transparent
                                    )
                                    .queueDragLongPress(reorder, key)
                                    .clickable { viewModel.skipToQueueItem(queueItem.id) }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.width(26.dp), contentAlignment = Alignment.Center) {
                                    if (isCurrent) {
                                        Icon(
                                            Icons.Rounded.GraphicEq,
                                            contentDescription = "Playing",
                                            tint = scheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = scheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                SongArtwork(
                                    song = song,
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(rowShape),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                                        color = scheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = scheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.padding(top = 1.dp)
                                    )
                                }
                                QueueDragHandle(
                                    state = reorder,
                                    rowKey = key,
                                    tint = scheme.onSurfaceVariant.copy(alpha = 0.6f)
                                )
                                IconButton(
                                    onClick = {
                                        viewModel.removeQueueItem(queueItem.id)
                                        removal.onRemoved(song.title)
                                    },
                                    enabled = queue.size > 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close,
                                        contentDescription = "Remove from queue",
                                        tint = scheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            SentupDivider()
                            }
                        }
                    }

                    item(key = "sentup_load_more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val pillShape = RoundedCornerShape(SentupRadii.pill)
                            Box(
                                modifier = Modifier
                                    .sentupCard(shape = pillShape, elevation = 1)
                                    .clip(pillShape)
                                    .clickable(enabled = !isLoadingMore) {
                                        viewModel.loadMoreRecommendations()
                                    }
                                    .padding(horizontal = 26.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoadingMore) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = scheme.primary,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Text(
                                        text = stringResource(R.string.np_load_more),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = scheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        DismissibleSnackbarHost(
            hostState = removal.hostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        )

        if (showSaveDialog) {
            CreatePlaylistDialog(
                initialName = stringResource(
                    R.string.queue_save_default_name,
                    remember { queuePlaylistDateText() }
                ),
                onDismiss = { showSaveDialog = false },
                onCreate = { name, description ->
                    showSaveDialog = false
                    viewModel.saveQueueAsPlaylist(name, description) { savedName, trackCount ->
                        removal.announce(
                            context.resources.getQuantityString(
                                R.plurals.queue_saved_to_playlist,
                                trackCount,
                                trackCount,
                                savedName
                            )
                        )
                    }
                }
            )
        }
    }
}

/** "1x" for whole speeds, "1.25x" otherwise; the speed row's value and dialog labels. */
private fun formatSentupSpeed(speed: Float): String =
    if (speed == speed.toLong().toFloat()) "${speed.toLong()}x" else "${speed}x"

/** Flat accent circle button with field-colored content. */
@Composable
internal fun EditorialCircleButton(
    onClick: () -> Unit,
    accent: Color,
    field: Color,
    size: androidx.compose.ui.unit.Dp,
    enabled: Boolean = true,
    content: @Composable () -> Unit
) {
    FilledIconButton(
        onClick = onClick,
        enabled = enabled,
        shape = CircleShape,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = accent,
            contentColor = field
        ),
        modifier = Modifier.size(size)
    ) {
        content()
    }
}

/**
 * Two-tone toggle chip: checked = accent fill with field glyph, unchecked =
 * bare accent glyph on the field. No third color, no outline.
 */
@Composable
internal fun EditorialChip(
    checked: Boolean,
    onClick: () -> Unit,
    accent: Color,
    field: Color,
    content: @Composable () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (checked) accent else Color.Transparent,
        contentColor = if (checked) field else accent,
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}

/**
 * Editorial queue: the same two-tone spread. The current track is the only
 * inverted row (accent fill, field text) - print-style emphasis. Shared with
 * the Poster style, which passes its own field/accent pair.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun EditorialQueueView(
    queue: List<MusicQueueItem>,
    currentQueueItemId: String?,
    onQueueItemClick: (MusicQueueItem) -> Unit,
    onRemoveItem: (MusicQueueItem) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    onCollapse: () -> Unit,
    onBackToPlayer: () -> Unit,
    field: Color,
    accent: Color,
    onSaveQueue: (name: String, description: String?, onSaved: (String, Int) -> Unit) -> Unit,
    onMoveSong: (from: Int, to: Int) -> Unit = { _, _ -> },
    onCommitOrder: () -> Unit = {},
    onUndoRemove: () -> Unit = {}
) {
    val currentQueueIndex = remember(queue, currentQueueItemId) {
        queue.indexOfFirst { it.id == currentQueueItemId }
    }
    val listState = rememberFocusedQueueListState(currentQueueIndex)
    val rowKeys = remember(queue) { queue.map { it.id } }
    val reorder = rememberQueueReorderState(
        listState = listState,
        keys = rowKeys,
        onMove = onMoveSong,
        onSettle = onCommitOrder
    )
    val removal = rememberQueueRemoval(onUndo = onUndoRemove)
    var showSaveDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(modifier = Modifier.fillMaxSize()) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(field)
    ) {
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
                accent = accent,
                field = field,
                size = 44.dp
            ) {
                Icon(Icons.Default.KeyboardArrowDown, "Collapse", modifier = Modifier.size(26.dp))
            }
            Text(
                text = stringResource(R.string.ps_up_next),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = FontFamily.Serif,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Bold
                ),
                color = accent
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                EditorialCircleButton(
                    onClick = { showSaveDialog = true },
                    accent = accent,
                    field = field,
                    size = 44.dp,
                    enabled = queue.isNotEmpty()
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.PlaylistAdd,
                        stringResource(R.string.queue_save_as_playlist),
                        modifier = Modifier.size(22.dp)
                    )
                }
                EditorialCircleButton(
                    onClick = onBackToPlayer,
                    accent = accent,
                    field = field,
                    size = 44.dp
                ) {
                    Icon(Icons.Rounded.MusicNote, "Now Playing", modifier = Modifier.size(22.dp))
                }
            }
        }

        if (queue.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = stringResource(R.string.np_queue_empty),
                    style = MaterialTheme.typography.titleMedium,
                    color = accent.copy(alpha = 0.7f)
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                itemsIndexed(
                    queue,
                    key = { index, _ -> rowKeys.getOrElse(index) { "editorial_queue_$index" } }
                ) { index, queueItem ->
                    val song = queueItem.song
                    val isCurrent = queueItem.id == currentQueueItemId
                    val key = rowKeys.getOrElse(index) { "editorial_queue_$index" }
                    val isDragging = reorder.draggingKey == key

                    QueueRowContainer(
                        isDragging = isDragging,
                        dragOffset = reorder.offsetFor(key),
                        removeEnabled = queue.size > 1,
                        onRemove = {
                            onRemoveItem(queueItem)
                            removal.onRemoved(song.title)
                        },
                        // animateItem sits on the container rather than the row,
                        // so the slide-out-of-the-way applies to the whole
                        // swipeable cell. It is disabled for the row under the
                        // finger, which is positioned by the drag instead.
                        modifier = if (isDragging) Modifier else Modifier.animateItem()
                    ) {
                        Surface(
                            onClick = { onQueueItemClick(queueItem) },
                            shape = RoundedCornerShape(24.dp),
                            color = if (isCurrent) accent else field,
                            contentColor = if (isCurrent) field else accent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .queueDragLongPress(reorder, key)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) {
                                    if (isCurrent) {
                                        Icon(
                                            Icons.Rounded.GraphicEq, "Playing",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    } else {
                                        Text(
                                            text = "${index + 1}",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = FontFamily.Serif,
                                                fontStyle = FontStyle.Italic
                                            )
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = song.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = song.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.graphicsLayer { alpha = 0.75f }
                                    )
                                }
                                QueueDragHandle(
                                    state = reorder,
                                    rowKey = key,
                                    tint = LocalContentColor.current.copy(alpha = 0.7f)
                                )
                                IconButton(
                                    onClick = {
                                        onRemoveItem(queueItem)
                                        removal.onRemoved(song.title)
                                    },
                                    enabled = queue.size > 1,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Close, "Remove from queue",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                item(key = "editorial_load_more") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                            .navigationBarsPadding(),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            onClick = onLoadMore,
                            enabled = !isLoadingMore,
                            shape = RoundedCornerShape(50),
                            color = accent,
                            contentColor = field
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 32.dp, vertical = 14.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isLoadingMore) {
                                    LoadingIndicator(
                                        modifier = Modifier.size(20.dp),
                                        color = field,
                                        polygons = listOf(
                                            MaterialShapes.Cookie9Sided,
                                            MaterialShapes.Pill,
                                            MaterialShapes.Sunny
                                        )
                                    )
                                } else {
                                    Text(
                                        text = "MORE",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            letterSpacing = 3.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

        DismissibleSnackbarHost(
            hostState = removal.hostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(16.dp)
        )

        if (showSaveDialog) {
            CreatePlaylistDialog(
                initialName = stringResource(
                    R.string.queue_save_default_name,
                    remember { queuePlaylistDateText() }
                ),
                onDismiss = { showSaveDialog = false },
                onCreate = { name, description ->
                    showSaveDialog = false
                    onSaveQueue(name, description) { savedName, trackCount ->
                        removal.announce(
                            context.resources.getQuantityString(
                                R.plurals.queue_saved_to_playlist,
                                trackCount,
                                trackCount,
                                savedName
                            )
                        )
                    }
                }
            )
        }
    }
}

/**
 * Shape backed by a graphics-shapes Morph at a given progress, scaled to
 * fill the composable bounds (same idiom as OnboardingScreen's MorphShape).
 */
internal class EditorialMorphShape(
    private val morph: Morph,
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = morph.toPath(progress).asComposePath()
        val matrix = Matrix()
        val bounds = morph.calculateBounds()
        val boundsWidth = bounds[2] - bounds[0]
        val boundsHeight = bounds[3] - bounds[1]

        matrix.scale(size.width / boundsWidth, size.height / boundsHeight)
        matrix.translate(-bounds[0], -bounds[1])
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

/**
 * Shape backed by a single RoundedPolygon scaled to fill the composable
 * bounds. Static sibling of EditorialMorphShape.
 */
internal class EditorialPolygonShape(
    private val polygon: RoundedPolygon
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = polygon.toPath().asComposePath()
        val matrix = Matrix()
        val bounds = polygon.calculateBounds()
        val boundsWidth = bounds[2] - bounds[0]
        val boundsHeight = bounds[3] - bounds[1]

        matrix.scale(size.width / boundsWidth, size.height / boundsHeight)
        matrix.translate(-bounds[0], -bounds[1])
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

internal fun formatEditorialTime(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}
