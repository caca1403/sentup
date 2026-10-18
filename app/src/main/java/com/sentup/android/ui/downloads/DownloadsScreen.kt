package com.sentup.android.ui.downloads

import com.sentup.android.R
import androidx.annotation.StringRes
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sentup.android.data.DownloadFailure
import com.sentup.android.data.DownloadMediaType
import com.sentup.android.data.DownloadProgress
import com.sentup.android.data.DownloadRepository
import com.sentup.android.data.DownloadRequest
import com.sentup.android.data.DownloadStatus
import com.sentup.android.data.DownloadedVideo
import com.sentup.android.data.Song
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupGroupHeader
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage

/**
 * The two libraries this screen holds.
 *
 * No label on the enum: the segments are text, so their wording is a string
 * resource and belongs with every other one. [labelRes] is the id the segment
 * is drawn from.
 */
private enum class DownloadsTab(@StringRes val labelRes: Int) {
    MUSIC(R.string.dl_tab_music),
    VIDEO(R.string.dl_tab_video)
}

/**
 * The outer radius of the expandable playlist group.
 *
 * Internal rather than private because [DownloadedPlaylistCard] is the head of
 * one of these groups and has to round its top by the same amount; a second
 * literal there is how the header and its tracks drift into two containers.
 * It matches the Sentup card corner so the group reads as one object with the
 * sections beside it.
 */
internal val SEGMENT_CORNER = SentupRadii.card

/**
 * Corner shape for one row of a section drawn as a single continuous card:
 * the first row rounds its top, the last its bottom, the middle rows fill
 * flat, and the shared tone hides the seams. Hairlines between rows stay
 * inset - a full-bleed line would cut the group back into slices.
 */
private fun sectionRowShape(index: Int, count: Int) = RoundedCornerShape(
    topStart = if (index == 0) SentupRadii.card else 0.dp,
    topEnd = if (index == 0) SentupRadii.card else 0.dp,
    bottomStart = if (index == count - 1) SentupRadii.card else 0.dp,
    bottomEnd = if (index == count - 1) SentupRadii.card else 0.dp
)

/**
 * Shape for a row inside a group whose top is already drawn by a header - an
 * expanded playlist's tracks. Only the last row rounds, because the header owns
 * the group's top corners and rounding the first row too would put a seam
 * across the middle of one container.
 */
private fun expandedTrackShape(index: Int, count: Int) =
    if (index == count - 1) {
        RoundedCornerShape(bottomStart = SEGMENT_CORNER, bottomEnd = SEGMENT_CORNER)
    } else {
        RoundedCornerShape(0.dp)
    }

/**
 * Downloads library, split by media type.
 *
 * Music and video are genuinely different things - one feeds the queue, the
 * other opens the video player - so they get separate segments rather than one
 * mixed list. In-flight transfers appear under the segment they belong to, so
 * a video downloading while the Music tab is open is not silently invisible.
 *
 * The screen speaks the shared surface language: a large title with its count
 * line, a floating capsule to switch what you are looking at, and every
 * section as one inset-grouped card. Touch actions sit as icons at the right
 * of their row; there is nothing else to reach for.
 */
@Composable
fun DownloadsScreen(
    downloadedSongs: List<Song>,
    downloadedVideos: List<DownloadedVideo>,
    activeDownloads: Map<String, DownloadProgress>,
    onBack: () -> Unit,
    onPlaySong: (Song) -> Unit,
    onPlayQueue: (List<Song>, Song) -> Unit = { _, song -> onPlaySong(song) },
    onPlayVideo: (List<DownloadedVideo>, DownloadedVideo) -> Unit,
    onDeleteDownload: (String) -> Unit,
    onDeleteVideo: (String) -> Unit,
    onCancelDownload: (String) -> Unit,
    onRetryDownload: (DownloadRequest) -> Unit,
    onPauseDownload: (String) -> Unit,
    onResumeDownload: (String) -> Unit,
    initiallyShowVideos: Boolean = false,
    onCancelAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by rememberSaveable {
        mutableStateOf(if (initiallyShowVideos) DownloadsTab.VIDEO else DownloadsTab.MUSIC)
    }

    // Split by the media type of the request itself rather than by which list
    // the finished item lands in, so queued and failed entries route correctly
    // before anything has completed.
    val musicProgress = activeDownloads.values
        .filter { it.request.type == DownloadMediaType.MUSIC }
        .sortedBy { it.status.ordinal }
    val videoProgress = activeDownloads.values
        .filter { it.request.type == DownloadMediaType.VIDEO }
        .sortedBy { it.status.ordinal }

    val activeCount = activeDownloads.values.count {
        it.status == DownloadStatus.DOWNLOADING || it.status == DownloadStatus.QUEUED
    }

    val tabHaptics = com.sentup.android.util.rememberSentupHaptics()

    Box(
        modifier = modifier
            .fillMaxSize()
            .sentupPage()
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Back as a small floating disc and, while something is running,
            // one equally small capsule. A TopAppBar would stack a second
            // title over the large one below it.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = SentupGroup.inset + 4.dp,
                        end = SentupGroup.inset,
                        top = 4.dp,
                        bottom = 6.dp
                    ),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .sentupCard(shape = CircleShape, elevation = 1, tinted = false)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onBack
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                if (activeCount > 0) {
                    Box(
                        modifier = Modifier
                            .sentupCard(
                                shape = RoundedCornerShape(SentupRadii.pill),
                                elevation = 1,
                                tinted = false
                            )
                            .clip(RoundedCornerShape(SentupRadii.pill))
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onCancelAll
                            )
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.stop_all),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.padding(
                    start = SentupGroup.inset + SentupGroup.dividerInset,
                    end = SentupGroup.inset + SentupGroup.dividerInset,
                    top = 6.dp
                )
            ) {
                Text(
                    text = stringResource(R.string.fab_downloads),
                    fontSize = 34.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        append(
                            pluralStringResource(
                                R.plurals.n_songs,
                                downloadedSongs.size,
                                downloadedSongs.size
                            )
                        )
                        append(" \u00b7 ")
                        append(
                            pluralStringResource(
                                R.plurals.n_videos,
                                downloadedVideos.size,
                                downloadedVideos.size
                            )
                        )
                        if (activeCount > 0) {
                            append(" \u00b7 ")
                            append(
                                pluralStringResource(
                                    R.plurals.dl_active_count,
                                    activeCount,
                                    activeCount
                                )
                            )
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            DownloadSegmentedPill(
                selected = selectedTab,
                pendingMusic = musicProgress.size,
                pendingVideo = videoProgress.size,
                onSelect = { tab ->
                    if (tab != selectedTab) {
                        tabHaptics.subtle()
                        selectedTab = tab
                    }
                }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Swipeable, the same as the Library tabs and for the same reason:
            // two tabs, each its own scroller, and nothing shared above them
            // that a pager would displace.
            val pagerState = rememberPagerState(
                initialPage = selectedTab.ordinal,
                pageCount = { DownloadsTab.entries.size }
            )
            LaunchedEffect(selectedTab) {
                if (pagerState.currentPage != selectedTab.ordinal) {
                    pagerState.animateScrollToPage(selectedTab.ordinal)
                }
            }
            val openTab by rememberUpdatedState(selectedTab)
            LaunchedEffect(pagerState) {
                snapshotFlow { pagerState.settledPage }.collect { page ->
                    val settled = DownloadsTab.entries.getOrNull(page)
                    if (settled != null && settled != openTab) {
                        tabHaptics.subtle()
                        selectedTab = settled
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                beyondViewportPageCount = 0,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                key = { DownloadsTab.entries[it].name }
            ) { page ->
                when (DownloadsTab.entries[page]) {
                    DownloadsTab.MUSIC -> MusicTab(
                        songs = downloadedSongs,
                        progress = musicProgress,
                        onPlayQueue = onPlayQueue,
                        onDelete = onDeleteDownload,
                        onCancel = onCancelDownload,
                        onRetry = onRetryDownload,
                        onPause = onPauseDownload,
                        onResume = onResumeDownload
                    )

                    DownloadsTab.VIDEO -> VideoTab(
                        videos = downloadedVideos,
                        progress = videoProgress,
                        onPlay = { video -> onPlayVideo(downloadedVideos, video) },
                        onDelete = onDeleteVideo,
                        onCancel = onCancelDownload,
                        onRetry = onRetryDownload,
                        onPause = onPauseDownload,
                        onResume = onResumeDownload
                    )
                }
            }
        }
    }
}

/**
 * Music and video as a floating capsule: only the chosen segment is filled,
 * emerald under the word, and a pending count rides inside the label so each
 * tab admits what it is still doing.
 */
@Composable
private fun DownloadSegmentedPill(
    selected: DownloadsTab,
    pendingMusic: Int,
    pendingVideo: Int,
    onSelect: (DownloadsTab) -> Unit
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
        DownloadsTab.entries.forEach { tab ->
            val isSelected = tab == selected
            val pending = if (tab == DownloadsTab.MUSIC) pendingMusic else pendingVideo
            val label = stringResource(tab.labelRes)
            val interactionSource = remember(tab) { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            val scale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (pressed) 0.96f else 1f,
                animationSpec = androidx.compose.animation.core.tween(durationMillis = 90),
                label = "downloadSegmentPress"
            )
            Row(
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
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (tab == DownloadsTab.MUSIC) {
                        Icons.Rounded.MusicNote
                    } else {
                        Icons.Rounded.Videocam
                    },
                    contentDescription = null,
                    tint = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(7.dp))
                Text(
                    text = if (pending > 0) "$label \u00b7 $pending" else label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 9.dp)
                )
            }
        }
    }
}

@Composable
private fun MusicTab(
    songs: List<Song>,
    progress: List<DownloadProgress>,
    onPlayQueue: (List<Song>, Song) -> Unit,
    onDelete: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRetry: (DownloadRequest) -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit
) {
    val context = LocalContext.current
    val repository = remember(context) { DownloadRepository.getInstance(context) }
    val playlists by repository.playlistStore.playlists.collectAsState()
    var expandedPlaylist by rememberSaveable { mutableStateOf<String?>(null) }

    // Resolved once per change rather than inside the lazy scope: offlineSongs
    // indexes the whole downloads list, and doing that per playlist on every
    // recomposition of the list body is a pass over every download for every
    // card on screen.
    val offline = remember(playlists, songs) {
        playlists.map { playlist -> playlist to playlist.offlineSongs(songs) }
    }

    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomInset + 28.dp)
    ) {
        if (offline.isNotEmpty()) {
            item(key = "playlists_header") {
                SentupGroupHeader(
                    text = stringResource(R.string.section_playlists),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            offline.forEach { (playlist, available) ->
                val isExpanded = expandedPlaylist == playlist.id
                item(key = "pl_${playlist.id}") {
                    DownloadedPlaylistCard(
                        playlist = playlist,
                        available = available,
                        expanded = isExpanded,
                        onClick = {
                            // An accordion: opening one closes the other, so a
                            // long playlist can never bury the rest of the tab.
                            expandedPlaylist = if (isExpanded) null else playlist.id
                        },
                        onPlay = {
                            available.firstOrNull()?.let { onPlayQueue(available, it) }
                        },
                        modifier = Modifier.animateItem()
                    )
                }
                if (isExpanded) {
                    if (available.isEmpty()) {
                        item(key = "pl_${playlist.id}_empty") {
                            DownloadedPlaylistEmptyRow(modifier = Modifier.animateItem())
                        }
                    } else {
                        // The tracks stay separate lazy items rather than living
                        // inside the card behind an AnimatedVisibility: a
                        // downloaded playlist runs to hundreds of rows, and an
                        // expand animation that composes all of them at once is
                        // paid on every open. animateItem gives the same fade
                        // and the same settle of everything below, one row at a
                        // time, off the list itself.
                        itemsIndexed(
                            items = available,
                            // Occurrence-qualified: a playlist may legitimately
                            // list the same track twice, so the id alone would
                            // collide and crash the list.
                            key = { index, song -> "pl_${playlist.id}_${song.id}_$index" }
                        ) { index, song ->
                            Box(
                                modifier = Modifier
                                    .animateItem()
                                    .padding(horizontal = SentupGroup.inset)
                                    .clip(expandedTrackShape(index, available.size))
                                    .background(MaterialTheme.colorScheme.surfaceContainer)
                            ) {
                                Column {
                                    if (index > 0) SentupDivider(
                                        Modifier.padding(start = 60.dp)
                                    )
                                    DownloadedRow(
                                        title = song.title,
                                        subtitle = song.artist,
                                        artworkUrl = song.albumArtUri?.toString()
                                            ?: song.thumbnailUrl,
                                        fallbackIcon = Icons.Rounded.MusicNote,
                                        onPlay = { onPlayQueue(available, song) },
                                        onDelete = { onDelete(song.id) }
                                    )
                                }
                            }
                        }
                    }
                }
                item(key = "pl_${playlist.id}_gap") {
                    Spacer(modifier = Modifier.height(SentupGroup.gap - 12.dp))
                }
            }
        }

        if (progress.isNotEmpty()) {
            item(key = "progress_header") {
                SentupGroupHeader(
                    text = stringResource(R.string.in_progress),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            itemsIndexed(progress, key = { _, it -> "p_${it.songId}" }) { index, item ->
                Box(
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = SentupGroup.inset)
                        .clip(sectionRowShape(index, progress.size))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column {
                        if (index > 0) SentupDivider()
                        ProgressRow(
                            item = item,
                            onCancel = onCancel,
                            onRetry = onRetry,
                            onPause = onPause,
                            onResume = onResume
                        )
                    }
                }
            }
        }

        if (songs.isNotEmpty()) {
            item(key = "songs_header") {
                SentupGroupHeader(
                    text = stringResource(R.string.song_options_downloaded),
                    modifier = Modifier.padding(top = SentupGroup.gap - 8.dp)
                )
            }
            itemsIndexed(songs, key = { _, it -> "s_${it.id}" }) { index, song ->
                Box(
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = SentupGroup.inset)
                        .clip(sectionRowShape(index, songs.size))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column {
                        if (index > 0) SentupDivider(Modifier.padding(start = 60.dp))
                        DownloadedRow(
                            title = song.title,
                            subtitle = song.artist,
                            artworkUrl = song.thumbnailUrl ?: song.albumArtUri?.toString(),
                            fallbackIcon = Icons.Rounded.MusicNote,
                            onPlay = { onPlayQueue(songs, song) },
                            onDelete = { onDelete(song.id) }
                        )
                    }
                }
            }
        }

        if (songs.isEmpty() && progress.isEmpty() && offline.isEmpty()) {
            item(key = "empty") {
                EmptyState(stringResource(R.string.dl_empty_music), Icons.Rounded.MusicNote)
            }
        }
    }
}

@Composable
private fun VideoTab(
    videos: List<DownloadedVideo>,
    progress: List<DownloadProgress>,
    onPlay: (DownloadedVideo) -> Unit,
    onDelete: (String) -> Unit,
    onCancel: (String) -> Unit,
    onRetry: (DownloadRequest) -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit
) {
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = bottomInset + 28.dp)
    ) {
        if (progress.isNotEmpty()) {
            item(key = "progress_header") {
                SentupGroupHeader(
                    text = stringResource(R.string.in_progress),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            itemsIndexed(progress, key = { _, it -> "p_${it.songId}" }) { index, item ->
                Box(
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = SentupGroup.inset)
                        .clip(sectionRowShape(index, progress.size))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column {
                        if (index > 0) SentupDivider()
                        ProgressRow(
                            item = item,
                            onCancel = onCancel,
                            onRetry = onRetry,
                            onPause = onPause,
                            onResume = onResume
                        )
                    }
                }
            }
        }

        if (videos.isNotEmpty()) {
            item(key = "videos_header") {
                SentupGroupHeader(
                    text = stringResource(R.string.song_options_downloaded),
                    modifier = Modifier.padding(top = SentupGroup.gap - 8.dp)
                )
            }
            itemsIndexed(videos, key = { _, it -> "v_${it.id}" }) { index, video ->
                Box(
                    modifier = Modifier
                        .animateItem()
                        .padding(horizontal = SentupGroup.inset)
                        .clip(sectionRowShape(index, videos.size))
                        .background(MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column {
                        if (index > 0) SentupDivider(Modifier.padding(start = 100.dp))
                        DownloadedRow(
                            title = video.title,
                            subtitle = listOfNotNull(video.channelName, video.quality)
                                .filter { it.isNotBlank() }
                                .joinToString(" \u00b7 "),
                            artworkUrl = video.thumbnailUrl,
                            fallbackIcon = Icons.Rounded.Videocam,
                            wideArtwork = true,
                            onPlay = { onPlay(video) },
                            onDelete = { onDelete(video.id) }
                        )
                    }
                }
            }
        }

        if (videos.isEmpty() && progress.isEmpty()) {
            item(key = "empty") {
                EmptyState(stringResource(R.string.dl_empty_videos), Icons.Rounded.Videocam)
            }
        }
    }
}

/**
 * One transfer inside the continuous In progress card: state, what it says
 * about itself, the controls as icons on the right, and a thin rounded bar
 * that fills as the bytes arrive. A queued row's bar runs indeterminate; a
 * failed row trades the bar for the reason.
 */
@Composable
private fun ProgressRow(
    item: DownloadProgress,
    onCancel: (String) -> Unit,
    onRetry: (DownloadRequest) -> Unit,
    onPause: (String) -> Unit,
    onResume: (String) -> Unit
) {
    val failed = item.status == DownloadStatus.FAILED
    val queued = item.status == DownloadStatus.QUEUED
    val paused = item.status == DownloadStatus.PAUSED

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(
                    start = SentupGroup.dividerInset,
                    end = 6.dp,
                    top = 10.dp,
                    bottom = 2.dp
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when {
                        failed -> Icons.Rounded.ErrorOutline
                        paused -> Icons.Rounded.Pause
                        queued -> Icons.Rounded.Schedule
                        else -> Icons.Rounded.Download
                    },
                    contentDescription = null,
                    tint = if (failed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.request.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when {
                        failed -> stringResource(R.string.dl_failed)
                        paused -> stringResource(R.string.dl_paused)
                        queued -> stringResource(R.string.dl_waiting)
                        item.finishing -> stringResource(R.string.dl_finishing)
                        item.totalBytes > 0 -> stringResource(
                            R.string.dl_size_progress,
                            formatMegabytes(item.bytesDownloaded),
                            formatMegabytes(item.totalBytes)
                        )
                        else -> stringResource(R.string.dl_preparing)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (failed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // A pill only where the state is an answer (waiting, paused,
            // failed); a transfer that is simply moving says it with its bar.
            val badge = when {
                failed -> stringResource(R.string.dl_failed)
                paused -> stringResource(R.string.dl_paused)
                queued -> stringResource(R.string.dl_waiting)
                else -> null
            }
            if (badge != null) {
                Surface(
                    color = if (failed) {
                        MaterialTheme.colorScheme.errorContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    shape = RoundedCornerShape(SentupRadii.pill)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (failed) {
                            MaterialTheme.colorScheme.onErrorContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            if (paused || queued || item.status == DownloadStatus.DOWNLOADING) {
                IconButton(onClick = {
                    if (paused) onResume(item.songId) else onPause(item.songId)
                }) {
                    Icon(
                        if (paused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                        contentDescription = stringResource(
                            if (paused) R.string.dl_resume else R.string.cd_pause
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            if (failed) {
                // Only offered when a retry can plausibly work. A refused
                // stream, a bot check or a full disk fail the same way a
                // second time, and a button that cannot help reads as a
                // broken app rather than as an answer.
                if (item.failure?.retryable != false) {
                    IconButton(onClick = { onRetry(item.request) }) {
                        Icon(
                            Icons.Rounded.Refresh,
                            contentDescription = stringResource(R.string.action_retry),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            IconButton(onClick = { onCancel(item.songId) }) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.action_cancel),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        if (!failed) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = SentupGroup.dividerInset + 34.dp + 12.dp,
                        end = SentupGroup.dividerInset,
                        top = 4.dp,
                        bottom = 12.dp
                    )
            ) {
                if (queued) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(SentupRadii.pill)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                } else {
                    val fraction = item.progress.coerceIn(0f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(SentupRadii.pill))
                            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fraction.coerceIn(0.02f, 1f))
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(SentupRadii.pill))
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        } else if (item.failure != null) {
            // "Failed" alone left the user with one obvious move and no way
            // to know whether it would work, so the row names the cause and,
            // when the cause is final, says so instead of offering a retry
            // that is guaranteed to fail.
            Column(
                modifier = Modifier.padding(
                    start = SentupGroup.dividerInset + 34.dp + 12.dp,
                    end = SentupGroup.dividerInset,
                    top = 2.dp,
                    bottom = 12.dp
                )
            ) {
                Text(
                    text = stringResource(item.failure.reasonRes()),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                val detail = item.failureDetail
                if (detail != null) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (!item.failure.retryable) {
                    Text(
                        text = stringResource(R.string.dl_fail_not_retryable),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/**
 * The line shown for a failure cause.
 *
 * A property of the code rather than a lookup in the row, so a new [DownloadFailure]
 * cannot be added without the screen deciding what it says.
 */
@StringRes
private fun DownloadFailure.reasonRes(): Int = when (this) {
    DownloadFailure.BOT_CHECK -> R.string.dl_fail_bot_check
    DownloadFailure.MEDIA_REFUSED -> R.string.dl_fail_media_refused
    DownloadFailure.NO_STREAM -> R.string.dl_fail_no_stream
    DownloadFailure.STORAGE -> R.string.dl_fail_storage
    DownloadFailure.NETWORK -> R.string.dl_fail_network
    DownloadFailure.UNKNOWN -> R.string.dl_fail_unknown
}

/**
 * A byte count as the transfer text wants it, in the current locale.
 *
 * The number goes through [String.format] rather than Kotlin's own because the
 * fraction has to follow the device's decimal separator - a dot in a locale
 * that writes a comma reads as a different number entirely.
 */
private fun formatMegabytes(bytes: Long): String =
    String.format("%.1f", bytes / (1024f * 1024f))

/**
 * One finished download: cover, what it is, and delete as an icon on the
 * right. Tapping plays it and presses the row in; there is no ripple and no
 * play triangle shouting over the artwork.
 */
@Composable
private fun DownloadedRow(
    title: String,
    subtitle: String,
    artworkUrl: String?,
    fallbackIcon: ImageVector,
    onPlay: () -> Unit,
    onDelete: () -> Unit,
    wideArtwork: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 90),
        label = "downloadedRowPress"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(SentupRadii.tile))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onPlay
            )
            .heightIn(min = 56.dp)
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
                .size(width = if (wideArtwork) 88.dp else 48.dp, height = 48.dp)
                .clip(RoundedCornerShape(SentupRadii.tile))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            if (!artworkUrl.isNullOrBlank()) {
                AsyncImage(
                    model = artworkUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = fallbackIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(13.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 1.dp)
                )
            }
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.action_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Nothing here yet, said once: the icon in a soft disc and one sentence. */
@Composable
private fun EmptyState(
    message: String,
    icon: ImageVector
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 80.dp, start = 28.dp, end = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(104.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(40.dp)
            )
        }
        Spacer(modifier = Modifier.height(18.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}
