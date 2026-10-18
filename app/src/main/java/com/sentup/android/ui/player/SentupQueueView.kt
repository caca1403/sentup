package com.sentup.android.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sentup.android.R
import com.sentup.android.data.Song
import com.sentup.android.data.isUnknownArtist
import com.sentup.android.data.isUnknownTitle
import com.sentup.android.data.queuePlaylistDateText
import com.sentup.android.ui.components.DismissibleSnackbarHost
import com.sentup.android.ui.components.QueueDragHandle
import com.sentup.android.ui.components.QueueReorderState
import com.sentup.android.ui.components.QueueRowContainer
import com.sentup.android.ui.components.SongArtwork
import com.sentup.android.ui.components.queueDragLongPress
import com.sentup.android.ui.components.rememberFocusedQueueListState
import com.sentup.android.ui.components.rememberQueueRemoval
import com.sentup.android.ui.components.rememberQueueReorderState
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupGroupHeader
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage
import com.sentup.android.ui.theme.sentupSpecularEdge

/**
 * The queue, in the app's own surface language, shared by every player style.
 *
 * Until now each style drew its own: six copies of the same list, in six
 * slightly different greys, six flat bars across the top and six different
 * answers to "what does an empty queue look like". That is exactly the drift
 * the design system exists to stop, and it is why the styles did not feel
 * like one app.
 *
 * This is the one page they now share. A floating capsule bar, the cover that
 * is playing as a card you can tap to go back, inset rows with hairlines to
 * the text, the playing row tinted rather than inverted, and a load-more
 * pill. Empty state is one sentence.
 *
 * It reads the view model itself, so a style only has to say where "back" and
 * "load more" go. Drag to reorder, swipe to remove, undo and save-as-playlist
 * are the same [QueueRowContainer] every queue in the app already uses, so the
 * behaviour is unchanged.
 */
@Composable
internal fun SentupQueueView(
    viewModel: PlayerViewModel,
    onCollapse: () -> Unit,
    onBackToPlayer: () -> Unit,
    onLoadMore: () -> Unit
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
            // The floating capsule every player style opens its queue under.
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
                        contentDescription = stringResource(R.string.cd_now_playing),
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
                // One tap turns the playing song into a radio seed: the same
                // mix the song's own long-press menu offers, reachable from
                // the queue without going back to the player.
                IconButton(
                    onClick = {
                        queue.find { it.id == currentQueueItemId }?.song?.let { viewModel.playSongRadio(it) }
                    },
                    enabled = queue.any { it.id == currentQueueItemId }
                ) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = stringResource(R.string.ar_start_radio),
                        tint = scheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = { showSaveDialog = true }, enabled = queue.isNotEmpty()) {
                    Icon(
                        Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = stringResource(R.string.queue_save_as_playlist),
                        tint = scheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onCollapse) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        contentDescription = stringResource(R.string.cd_collapse),
                        tint = scheme.onSurface,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
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
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    queue.find { it.id == currentQueueItemId }?.song?.let { song ->
                        item(key = "sentup_queue_featured") {
                            SentupFeaturedTrack(song = song, onClick = onBackToPlayer)
                        }
                    }

                    item(key = "sentup_queue_header") {
                        SentupGroupHeader(text = stringResource(R.string.ps_up_next))
                    }

                    itemsIndexed(
                        queue,
                        key = { index, _ -> rowKeys.getOrElse(index) { "sentup_queue_$index" } }
                    ) { index, queueItem ->
                        val song = queueItem.song
                        val isCurrent = queueItem.id == currentQueueItemId
                        val key = rowKeys.getOrElse(index) { "sentup_queue_$index" }
                        val isDragging = reorder.draggingKey == key

                        QueueRowContainer(
                            isDragging = isDragging,
                            dragOffset = reorder.offsetFor(key),
                            removeEnabled = queue.size > 1,
                            onRemove = {
                                viewModel.removeQueueItem(queueItem.id)
                                removal.onRemoved(song.title)
                            },
                            // animateItem sits on the container rather than the
                            // row, so the slide-out-of-the-way applies to the
                            // whole swipeable cell.
                            modifier = if (isDragging) Modifier else Modifier.animateItem()
                        ) {
                            SentupQueueRow(
                                song = song,
                                position = index + 1,
                                isCurrent = isCurrent,
                                isDownloaded = viewModel.isDownloaded(song.id),
                                isDownloading = viewModel.isDownloading(song.id),
                                isLocalOriginal = viewModel.isLocalOriginal(song),
                                reorder = reorder,
                                rowKey = key,
                                onClick = { viewModel.skipToQueueItem(queueItem.id) },
                                onRemove = {
                                    viewModel.removeQueueItem(queueItem.id)
                                    removal.onRemoved(song.title)
                                },
                                removeEnabled = queue.size > 1
                            )
                        }
                    }

                    item(key = "sentup_queue_load_more") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .navigationBarsPadding()
                                .padding(top = 18.dp, bottom = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val pillShape = RoundedCornerShape(SentupRadii.pill)
                            Box(
                                modifier = Modifier
                                    .sentupCard(shape = pillShape, elevation = 1)
                                    .clip(pillShape)
                                    .clickable(enabled = !isLoadingMore) { onLoadMore() }
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

/**
 * The cover of the track that is playing, as a card you can tap to go back to
 * the player. It is the same object the now-playing page shows, one size down.
 */
@Composable
private fun SentupFeaturedTrack(
    song: Song,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(SentupRadii.card)
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 110),
        label = "SentupFeaturePress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = SentupGroup.gap)
            .graphicsLayer {
                scaleX = press
                scaleY = press
            },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .sentupCard(shape = shape, elevation = 2, tinted = false)
                .sentupSpecularEdge(shape)
                .clip(shape)
                .clickable(
                    interactionSource = interaction,
                    indication = null,
                    onClick = onClick
                )
        ) {
            val artSong = song.takeIf { it.thumbnailUrl != null || it.albumArtUri != null }
            if (artSong != null) {
                PlayerArtwork(
                    song = artSong,
                    contentDescription = stringResource(R.string.cd_now_playing),
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
                        tint = scheme.onSurfaceVariant.copy(alpha = 0.3f),
                        modifier = Modifier.size(56.dp)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = song.title.takeIf { !isUnknownTitle(it) }
                ?: stringResource(R.string.np_art_untitled),
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            color = scheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = SentupGroup.inset)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = song.artist.takeIf { !isUnknownArtist(it) }
                ?: stringResource(R.string.np_artist_unknown),
            style = MaterialTheme.typography.bodyMedium,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = SentupGroup.inset)
        )
    }
}

/**
 * One queue line: the position (or the playing glyph), the cover, the two
 * lines of text, whatever download state applies, a drag handle and the remove
 * button. The playing row is tinted rather than inverted, and the hairline
 * under it is inset to the text.
 */
@Composable
private fun SentupQueueRow(
    song: Song,
    position: Int,
    isCurrent: Boolean,
    isDownloaded: Boolean,
    isDownloading: Boolean,
    isLocalOriginal: Boolean,
    reorder: QueueReorderState,
    rowKey: String,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    removeEnabled: Boolean
) {
    val scheme = MaterialTheme.colorScheme
    val rowShape = RoundedCornerShape(SentupRadii.tile)

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(rowShape)
                .background(
                    if (isCurrent) scheme.primary.copy(alpha = 0.10f) else Color.Transparent
                )
                .queueDragLongPress(reorder, rowKey)
                .clickable(onClick = onClick)
                .padding(horizontal = 4.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.width(30.dp), contentAlignment = Alignment.Center) {
                if (isCurrent) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = stringResource(R.string.cd_playing),
                        tint = scheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = "$position",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = FontFamily.Monospace
                        ),
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
            when {
                isDownloading -> CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    color = scheme.primary,
                    strokeWidth = 2.dp
                )
                isDownloaded -> Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.song_options_downloaded),
                    tint = scheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(18.dp)
                )
                isLocalOriginal -> Icon(
                    imageVector = Icons.Rounded.Smartphone,
                    contentDescription = stringResource(R.string.cd_local_file),
                    tint = scheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.size(16.dp)
                )
            }
            if (isDownloading || isDownloaded || isLocalOriginal) {
                Spacer(modifier = Modifier.width(8.dp))
            }
            QueueDragHandle(
                state = reorder,
                rowKey = rowKey,
                tint = scheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            IconButton(
                onClick = onRemove,
                enabled = removeEnabled,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = stringResource(R.string.cd_remove_from_queue),
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        SentupDivider()
    }
}