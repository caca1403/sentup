package com.sentup.android.ui.video
import androidx.compose.ui.res.stringResource
import com.sentup.android.R

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sentup.android.ui.components.DismissibleSnackbarHost
import com.sentup.android.ui.components.QueueDragHandle
import com.sentup.android.ui.components.QueueRowContainer
import com.sentup.android.ui.components.VideoThumbnailBadge
import com.sentup.android.ui.components.queueRowKeys
import com.sentup.android.ui.components.rememberQueueRemoval
import com.sentup.android.ui.components.rememberQueueReorderState
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.data.VideoQueue

/**
 * The playlist behind the video that is playing, as a list you can jump around
 * in.
 *
 * The player is a single-video surface, so a playlist had nowhere to be seen at
 * all: the only list on the watch page is Up Next, which is recommendations. A
 * sheet rather than a panel wedged into the info column because a playlist runs
 * to hundreds of entries and the info column is a `verticalScroll` - the same
 * reason chapters and captions are sheets.
 *
 * The rows are the player's own language rather than a list of their own: a
 * position marker that doubles as the now-playing indicator, tile-radius
 * artwork, title over channel, and one thin drag handle. The whole list sits
 * inside a single inset card, so it reads as one object on the page and the
 * hairlines between rows are inset to the text rather than running edge to
 * edge. Addressed by index throughout, never by video id: playlists may list
 * the same video twice (see [VideoQueue]).
 *
 * Open on what is playing, not at the top: on a long playlist the current
 * video is usually off-screen, and the first thing anyone does here is look
 * for where they are. Two rows of lead-in so it does not sit against the
 * header with no context above it.
 *
 * Keyed on the sheet opening rather than on queue.index, or every reorder
 * that shifts the playing row would yank the list back under the finger.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoQueueSheet(
    queue: VideoQueue,
    onSelect: (index: Int) -> Unit,
    onDismiss: () -> Unit,
    keepSystemBarsHidden: Boolean = false,
    onMove: (from: Int, to: Int) -> Unit = { _, _ -> },
    onRemove: (index: Int) -> Unit = {},
    onUndoRemove: () -> Unit = {},
    /**
     * Keep this queue as a local video playlist - music's "Save queue as
     * playlist", asked for in video mode too. Null hides the button.
     */
    onSaveAsPlaylist: ((name: String, onSaved: (String, Int) -> Unit) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current
    var showSaveDialog by rememberSaveable { mutableStateOf(false) }
    val rowKeys = remember(queue.videos) {
        queueRowKeys(queue.videos.map { it.videoId }, "video_queue")
    }
    val reorder = rememberQueueReorderState(
        listState = listState,
        keys = rowKeys,
        onMove = onMove,
        // Nothing to persist: the video queue is in-memory session state, not
        // a restored playback session the way music's is.
        onSettle = {}
    )
    val removal = rememberQueueRemoval(onUndo = onUndoRemove)

    LaunchedEffect(Unit) {
        listState.scrollToItem((queue.index - 2).coerceAtLeast(0))
    }

    VideoSheetScaffold(
        onDismiss = onDismiss,
        title = stringResource(R.string.vq_playing_from),
        subtitle = queue.title,
        trailing = if (onSaveAsPlaylist != null) {
            {
                IconButton(
                    onClick = { showSaveDialog = true },
                    enabled = queue.videos.isNotEmpty()
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = stringResource(R.string.queue_save_as_playlist),
                        tint = if (queue.videos.isNotEmpty()) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        },
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null
    ) {
        KeepSystemBarsHidden(keepSystemBarsHidden)

        if (queue.videos.isEmpty()) {
            VideoSheetEmptyState(
                title = stringResource(R.string.np_queue_empty),
                icon = Icons.AutoMirrored.Rounded.QueueMusic
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = SentupGroup.inset)
                    .sentupCard(
                        shape = RoundedCornerShape(SentupRadii.card),
                        elevation = 0,
                        tinted = false
                    )
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(vertical = 4.dp)
                    ) {
                        // Occurrence-qualified keys: the same video may appear
                        // twice in one playlist so a bare id crashes, but a key
                        // carrying the row index would change on every reorder.
                        // See queueRowKeys.
                        itemsIndexed(
                            items = queue.videos,
                            key = { index, _ -> rowKeys.getOrElse(index) { "video_queue_$index" } }
                        ) { index, video ->
                            val key = rowKeys.getOrElse(index) { "video_queue_$index" }
                            val isDragging = reorder.isDragging(key)
                            val isCurrent = index == queue.index

                            QueueRowContainer(
                                isDragging = isDragging,
                                dragOffset = reorder.offsetFor(key),
                                // The playing row is not removable, so it is
                                // not swipeable either - see VideoQueue.removedAt.
                                removeEnabled = queue.canRemoveAt(index),
                                onRemove = {
                                    onRemove(index)
                                    removal.onRemoved(video.title)
                                },
                                modifier = if (isDragging) Modifier else Modifier.animateItem()
                            ) {
                                QueueRow(
                                    video = video,
                                    position = index + 1,
                                    isCurrent = isCurrent,
                                    showDivider = index < queue.videos.lastIndex,
                                    onClick = { onSelect(index) },
                                    dragHandle = {
                                        QueueDragHandle(
                                            state = reorder,
                                            rowKey = key,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                )
                            }
                        }
                    }

                    DismissibleSnackbarHost(
                        hostState = removal.hostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }

    if (showSaveDialog && onSaveAsPlaylist != null) {
        com.sentup.android.ui.player.CreatePlaylistDialog(
            initialName = stringResource(
                R.string.queue_save_default_name,
                remember { com.sentup.android.data.queuePlaylistDateText() }
            ),
            withDescription = false,
            onDismiss = { showSaveDialog = false },
            onCreate = { name, _ ->
                showSaveDialog = false
                onSaveAsPlaylist(name) { savedName, count ->
                    removal.announce(
                        context.resources.getQuantityString(
                            R.plurals.queue_saved_videos_to_playlist, count, count, savedName
                        )
                    )
                }
            }
        )
    }
}

/**
 * One video in the queue: where it sits, what it looks like, and the handle
 * that moves it.
 *
 * The position marker doubles as the now-playing indicator, so the row that is
 * playing reads at a glance without a second badge - it turns emerald and grows
 * an equaliser glyph in the space the number was in. The drag handle is the
 * shared one, a 44dp target around an 18dp outlined glyph, because a grab target
 * people miss is a feature they do not have.
 */
@Composable
private fun QueueRow(
    video: com.sentup.android.data.VideoItem,
    position: Int,
    isCurrent: Boolean,
    onClick: () -> Unit,
    showDivider: Boolean = false,
    dragHandle: @Composable () -> Unit = {}
) {
    val scheme = MaterialTheme.colorScheme
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .videoPressTarget(onClick = onClick)
                .background(if (isCurrent) scheme.primary.copy(alpha = 0.10f) else Color.Transparent)
                .padding(start = SentupGroup.dividerInset, end = 4.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The position marker doubles as the now-playing indicator, so the
            // row that is playing reads at a glance without a second badge.
            Box(
                modifier = Modifier.width(22.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isCurrent) {
                    Icon(
                        Icons.Rounded.Equalizer,
                        contentDescription = stringResource(R.string.cd_now_playing),
                        tint = scheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                } else {
                    Text(
                        text = position.toString(),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            VideoSheetThumbnail(
                url = video.thumbnailUrl,
                modifier = Modifier.width(96.dp),
                height = 54.dp
            )

            Box {
                VideoThumbnailBadge(
                    video = video,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(3.dp)
                )
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else FontWeight.Normal,
                    color = scheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (video.channelName.isNotBlank()) {
                    Text(
                        text = video.channelName,
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 1.dp)
                    )
                }
            }

            dragHandle()
        }
        if (showDivider) SentupDivider()
    }
}
