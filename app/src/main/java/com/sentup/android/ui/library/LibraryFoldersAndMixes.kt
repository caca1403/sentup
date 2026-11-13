package com.sentup.android.ui.library

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.NightlightRound
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sentup.android.R
import com.sentup.android.data.AlbumFolder
import com.sentup.android.data.ChannelFolder
import com.sentup.android.data.FolderSortOrder
import com.sentup.android.data.LibraryDnaStats
import com.sentup.android.data.LibraryFilterChip
import com.sentup.android.data.LibrarySmartMix
import com.sentup.android.data.SmartMixKind
import com.sentup.android.data.Song
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard

/**
 * Display modes at the top of the Music Library.
 */
enum class LibraryViewMode {
    Flow,
    Folders,
    Mixes
}

/**
 * Formats a duration in milliseconds into a compact human-readable label (e.g. `42m` or `3h 15m`).
 */
internal fun formatCompactDuration(durationMs: Long): String {
    val totalMinutes = (durationMs / 60_000L).coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return when {
        hours > 0L -> "${hours}h ${minutes}m"
        totalMinutes > 0L -> "${totalMinutes}m"
        else -> "<1m"
    }
}

/**
 * Formats a track duration in milliseconds as `m:ss`.
 */
internal fun formatTrackDuration(durationMs: Long): String {
    val totalSeconds = (durationMs / 1000L).coerceAtLeast(0L)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return "%d:%02d".format(minutes, seconds)
}

/**
 * Mode switcher (Flow / Folders / Smart Mixes), instant search bar, and quick filter chips.
 */
@Composable
fun LibraryModeAndSearchHeader(
    viewMode: LibraryViewMode,
    onViewModeChange: (LibraryViewMode) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    activeFilter: LibraryFilterChip,
    onFilterChange: (LibraryFilterChip) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            val modes = listOf(
                Triple(LibraryViewMode.Mixes, R.string.lib_mode_mixes, Icons.Rounded.AutoAwesome),
                Triple(LibraryViewMode.Folders, R.string.lib_mode_folders, Icons.Rounded.FolderOpen),
                Triple(LibraryViewMode.Flow, R.string.lib_mode_flow, Icons.Rounded.LibraryMusic)
            )
            modes.forEachIndexed { index, (mode, labelRes, icon) ->
                SegmentedButton(
                    selected = viewMode == mode,
                    onClick = { onViewModeChange(mode) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    icon = {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                ) {
                    Text(
                        text = stringResource(labelRes),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text(stringResource(R.string.lib_search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.lib_action_clear))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(SentupRadii.pill),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
            ),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            LibraryFilterChip.entries.forEach { chip ->
                val labelRes = when (chip) {
                    LibraryFilterChip.All -> R.string.lib_filter_all
                    LibraryFilterChip.Downloaded -> R.string.lib_filter_downloaded
                    LibraryFilterChip.Liked -> R.string.lib_filter_liked
                    LibraryFilterChip.Local -> R.string.lib_filter_local
                    LibraryFilterChip.YouTube -> R.string.lib_filter_youtube
                    LibraryFilterChip.Unplayed -> R.string.lib_filter_unplayed
                }
                FilterChip(
                    selected = activeFilter == chip,
                    onClick = { onFilterChange(chip) },
                    label = { Text(stringResource(labelRes)) }
                )
            }
        }
    }
}

/**
 * Compact "Library DNA" summary card with a one-tap Surprise Me action.
 */
@Composable
fun LibraryDnaCard(
    dna: LibraryDnaStats,
    onSurpriseMe: () -> Unit,
    onOpenCleaner: (() -> Unit)? = null,
    onOpenNeuralLab: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (dna.totalSongs == 0) return
    Surface(
        shape = RoundedCornerShape(SentupRadii.card),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.lib_dna_title),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "${dna.totalChannels} · ${dna.totalAlbums} · ${formatCompactDuration(dna.totalDurationMs)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = stringResource(R.string.lib_dna_offline_pct, dna.offlinePercent),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.lib_dna_unplayed_count, dna.unplayedCount),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onOpenNeuralLab != null) {
                    IconButton(onClick = onOpenNeuralLab) {
                        Icon(
                            imageVector = Icons.Rounded.Psychology,
                            contentDescription = stringResource(R.string.lib_neural_lab_title),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (onOpenCleaner != null) {
                    IconButton(onClick = onOpenCleaner) {
                        Icon(
                            imageVector = Icons.Rounded.CleaningServices,
                            contentDescription = stringResource(R.string.lib_cleaner_title),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                FilledTonalButton(
                    onClick = onSurpriseMe,
                    shape = RoundedCornerShape(SentupRadii.pill),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Casino,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.lib_dna_surprise))
                }
            }
        }
    }
}

/**
 * Emits the Channel -> Album -> Song folder browser items into the parent [LazyListScope].
 */
fun LazyListScope.channelFolderBrowserItems(
    channels: List<ChannelFolder>,
    selectedChannelName: String?,
    selectedAlbumName: String?,
    sortOrder: FolderSortOrder,
    selectionMode: Boolean,
    selectedSongIds: Set<String>,
    likedIds: Set<String>,
    downloadedIds: Set<String>,
    onSelectChannel: (String?) -> Unit,
    onSelectAlbum: (String?) -> Unit,
    onSortOrderChange: (FolderSortOrder) -> Unit,
    onTogglePinChannel: (String) -> Unit,
    onPlayQueue: (List<Song>, Song?) -> Unit,
    onShuffleQueue: (List<Song>) -> Unit,
    onToggleSelectionMode: () -> Unit,
    onToggleSongSelected: (String) -> Unit,
    onSongLongPress: ((Song) -> Unit)?,
    onSaveChannelAsFolder: ((ChannelFolder) -> Unit)? = null
) {
    val activeChannel = selectedChannelName?.let { name ->
        channels.firstOrNull { it.name == name }
    }
    val activeAlbum = if (activeChannel != null && selectedAlbumName != null) {
        activeChannel.albums.firstOrNull { it.name == selectedAlbumName }
    } else null

    // Breadcrumb & Sort Bar
    item(key = "folder_breadcrumbs_and_sort") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                FilterChip(
                    selected = activeChannel == null,
                    onClick = {
                        onSelectAlbum(null)
                        onSelectChannel(null)
                    },
                    leadingIcon = {
                        Icon(Icons.Rounded.Folder, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text(stringResource(R.string.lib_folder_root)) }
                )
                if (activeChannel != null) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    FilterChip(
                        selected = activeAlbum == null,
                        onClick = { onSelectAlbum(null) },
                        label = {
                            Text(
                                text = activeChannel.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
                if (activeAlbum != null) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    FilterChip(
                        selected = true,
                        onClick = {},
                        label = {
                            Text(
                                text = activeAlbum.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            if (activeChannel == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FolderSortOrder.entries.forEach { order ->
                        val labelRes = when (order) {
                            FolderSortOrder.Name -> R.string.lib_folder_sort_name
                            FolderSortOrder.MostSongs -> R.string.lib_folder_sort_songs
                            FolderSortOrder.MostPlayed -> R.string.lib_folder_sort_played
                            FolderSortOrder.TotalDuration -> R.string.lib_folder_sort_duration
                        }
                        FilterChip(
                            selected = sortOrder == order,
                            onClick = { onSortOrderChange(order) },
                            label = { Text(stringResource(labelRes)) }
                        )
                    }
                }
            }
        }
    }

    if (channels.isEmpty()) {
        item(key = "folder_empty") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp, horizontal = SentupGroup.inset),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.lib_folder_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    when {
        activeChannel == null -> {
            // Level 1: Channel folders
            items(channels, key = { "ch_folder_${it.name}" }) { channel ->
                ChannelFolderCard(
                    channel = channel,
                    onOpen = { onSelectChannel(channel.name) },
                    onPlayAll = { onPlayQueue(channel.allSongs, channel.allSongs.firstOrNull()) },
                    onShuffle = { onShuffleQueue(channel.allSongs) },
                    onTogglePin = { onTogglePinChannel(channel.name) },
                    onSaveAsFolder = onSaveChannelAsFolder?.let { save -> { save(channel) } }
                )
            }
        }
        activeAlbum == null -> {
            // Level 2: Album folders inside the selected channel
            item(key = "ch_banner_${activeChannel.name}") {
                FolderActionBanner(
                    title = activeChannel.name,
                    subtitle = stringResource(
                        R.string.lib_folder_albums_songs,
                        activeChannel.albums.size,
                        activeChannel.allSongs.size
                    ) + " · " + formatCompactDuration(activeChannel.totalDurationMs),
                    onPlayAll = { onPlayQueue(activeChannel.allSongs, activeChannel.allSongs.firstOrNull()) },
                    onShuffle = { onShuffleQueue(activeChannel.allSongs) }
                )
            }
            items(activeChannel.albums, key = { "al_folder_${activeChannel.name}_${it.name}" }) { album ->
                AlbumFolderCard(
                    album = album,
                    onOpen = { onSelectAlbum(album.name) },
                    onPlayAll = { onPlayQueue(album.songs, album.songs.firstOrNull()) },
                    onShuffle = { onShuffleQueue(album.songs) }
                )
            }
        }
        else -> {
            // Level 3: Songs inside the selected album
            item(key = "al_banner_${activeChannel.name}_${activeAlbum.name}") {
                FolderActionBanner(
                    title = activeAlbum.name,
                    subtitle = buildString {
                        append(activeChannel.name)
                        activeAlbum.releaseYear?.let { append(" · $it") }
                        append(" · ${activeAlbum.songs.size} · ${formatCompactDuration(activeAlbum.totalDurationMs)}")
                    },
                    onPlayAll = { onPlayQueue(activeAlbum.songs, activeAlbum.songs.firstOrNull()) },
                    onShuffle = { onShuffleQueue(activeAlbum.songs) },
                    trailingExtra = {
                        TextButton(onClick = onToggleSelectionMode) {
                            Text(
                                text = if (selectionMode) {
                                    stringResource(R.string.lib_action_clear)
                                } else {
                                    stringResource(R.string.lib_select_mode)
                                }
                            )
                        }
                    }
                )
            }
            items(activeAlbum.songs, key = { "al_song_${it.id}" }) { song ->
                FolderSongRow(
                    song = song,
                    isLiked = song.id in likedIds,
                    isDownloaded = song.id in downloadedIds,
                    selectionMode = selectionMode,
                    isSelected = song.id in selectedSongIds,
                    onClick = {
                        if (selectionMode) {
                            onToggleSongSelected(song.id)
                        } else {
                            onPlayQueue(activeAlbum.songs, song)
                        }
                    },
                    onLongClick = {
                        if (!selectionMode) {
                            onToggleSelectionMode()
                        }
                        onToggleSongSelected(song.id)
                        if (selectionMode) onSongLongPress?.invoke(song)
                    }
                )
            }
        }
    }
}

@Composable
private fun ChannelFolderCard(
    channel: ChannelFolder,
    onOpen: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onTogglePin: () -> Unit,
    onSaveAsFolder: (() -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(SentupRadii.card),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset, vertical = 5.dp)
            .clip(RoundedCornerShape(SentupRadii.card))
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FolderArtworkBox(
                artworkUrl = channel.artworkUrl,
                fallbackIcon = Icons.Rounded.Folder,
                circular = true
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = channel.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (channel.isPinned) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(
                        R.string.lib_folder_albums_songs,
                        channel.albums.size,
                        channel.allSongs.size
                    ) + " · " + formatCompactDuration(channel.totalDurationMs),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onSaveAsFolder != null) {
                IconButton(onClick = onSaveAsFolder) {
                    Icon(
                        imageVector = Icons.Rounded.CreateNewFolder,
                        contentDescription = stringResource(R.string.lib_custom_folder_save_auto),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            IconButton(onClick = onTogglePin) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = stringResource(
                        if (channel.isPinned) R.string.lib_folder_unpin else R.string.lib_folder_pin
                    ),
                    tint = if (channel.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            FilledTonalIconButton(onClick = onPlayAll) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.lib_folder_play_all))
            }
            IconButton(onClick = onShuffle) {
                Icon(Icons.Rounded.Shuffle, contentDescription = stringResource(R.string.lib_folder_shuffle))
            }
        }
    }
}

@Composable
private fun AlbumFolderCard(
    album: AlbumFolder,
    onOpen: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(SentupRadii.card),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset, vertical = 5.dp)
            .clip(RoundedCornerShape(SentupRadii.card))
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FolderArtworkBox(
                artworkUrl = album.artworkUrl,
                fallbackIcon = Icons.Rounded.Album,
                circular = false
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = album.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = buildString {
                        album.releaseYear?.let { append("$it · ") }
                        append(pluralStringResource(R.plurals.n_songs, album.songs.size, album.songs.size))
                        append(" · ${formatCompactDuration(album.totalDurationMs)}")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledTonalIconButton(onClick = onPlayAll) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.lib_folder_play_all))
            }
            IconButton(onClick = onShuffle) {
                Icon(Icons.Rounded.Shuffle, contentDescription = stringResource(R.string.lib_folder_shuffle))
            }
        }
    }
}

@Composable
private fun FolderActionBanner(
    title: String,
    subtitle: String,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    trailingExtra: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(SentupRadii.card),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset, vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(onClick = onPlayAll, shape = RoundedCornerShape(SentupRadii.pill)) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.lib_folder_play_all))
                }
                FilledTonalButton(onClick = onShuffle, shape = RoundedCornerShape(SentupRadii.pill)) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.lib_folder_shuffle))
                }
                Spacer(Modifier.weight(1f))
                trailingExtra?.invoke()
            }
        }
    }
}

@Composable
private fun FolderSongRow(
    song: Song,
    isLiked: Boolean,
    isDownloaded: Boolean,
    selectionMode: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                else MaterialTheme.colorScheme.surfaceContainerLow
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (selectionMode) {
            Icon(
                imageVector = if (isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else if (song.trackNumber != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = song.trackNumber.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            FolderArtworkBox(
                artworkUrl = song.albumArtUri?.toString() ?: song.thumbnailUrl,
                fallbackIcon = Icons.Rounded.MusicNote,
                circular = false,
                sizeDp = 40
            )
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formatTrackDuration(song.duration),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        if (isDownloaded) {
            Icon(
                imageVector = Icons.Rounded.CloudDone,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        if (isLiked) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun FolderArtworkBox(
    artworkUrl: String?,
    fallbackIcon: ImageVector,
    circular: Boolean,
    sizeDp: Int = 52
) {
    val shape = if (circular) CircleShape else RoundedCornerShape(12.dp)
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        contentAlignment = Alignment.Center
    ) {
        if (artworkUrl != null) {
            AsyncImage(
                model = artworkUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = fallbackIcon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size((sizeDp / 2).dp)
            )
        }
    }
}

/**
 * Emits Smart Auto-Mix cards into the parent [LazyListScope].
 */
fun LazyListScope.smartMixesItems(
    mixes: List<LibrarySmartMix>,
    onPlayMix: (LibrarySmartMix) -> Unit,
    onShuffleMix: (LibrarySmartMix) -> Unit
) {
    if (mixes.isEmpty()) {
        item(key = "mixes_empty") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 48.dp, horizontal = SentupGroup.inset),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.lib_folder_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    items(mixes, key = { "smart_mix_${it.kind.name}" }) { mix ->
        SmartMixCard(
            mix = mix,
            onPlay = { onPlayMix(mix) },
            onShuffle = { onShuffleMix(mix) }
        )
    }
}

@Composable
private fun SmartMixCard(
    mix: LibrarySmartMix,
    onPlay: () -> Unit,
    onShuffle: () -> Unit
) {
    val (title, subtitle, icon) = when (mix.kind) {
        SmartMixKind.ArtistSpotlight -> Triple(
            stringResource(R.string.lib_mix_artist_spotlight, mix.focusLabel ?: ""),
            stringResource(R.string.lib_mix_artist_spotlight_sub),
            Icons.Rounded.Person
        )
        SmartMixKind.SmartDiscovery -> Triple(
            stringResource(R.string.lib_mix_unplayed_gems),
            stringResource(R.string.lib_mix_unplayed_gems_sub),
            Icons.Rounded.Explore
        )
        SmartMixKind.TopRotation -> Triple(
            stringResource(R.string.lib_mix_top_rotation),
            stringResource(R.string.lib_mix_top_rotation_sub),
            Icons.Rounded.TrendingUp
        )
        SmartMixKind.ForgottenFavorites -> Triple(
            stringResource(R.string.lib_mix_forgotten),
            stringResource(R.string.lib_mix_forgotten_sub),
            Icons.Rounded.History
        )
        SmartMixKind.FreshAdditions -> Triple(
            stringResource(R.string.lib_mix_fresh_additions),
            stringResource(R.string.lib_mix_fresh_additions_sub),
            Icons.Rounded.NewReleases
        )
        SmartMixKind.OfflineReady -> Triple(
            stringResource(R.string.lib_mix_offline_ready),
            stringResource(R.string.lib_mix_offline_ready_sub),
            Icons.Rounded.CloudDone
        )
        SmartMixKind.QuickBites -> Triple(
            stringResource(R.string.lib_mix_quick_bites),
            stringResource(R.string.lib_mix_quick_bites_sub),
            Icons.Rounded.Bolt
        )
        SmartMixKind.DeepDives -> Triple(
            stringResource(R.string.lib_mix_deep_dives),
            stringResource(R.string.lib_mix_deep_dives_sub),
            Icons.Rounded.NightlightRound
        )
    }
    val coverUrl = mix.songs.firstNotNullOfOrNull { it.albumArtUri?.toString() ?: it.thumbnailUrl }
    Surface(
        shape = RoundedCornerShape(SentupRadii.card),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset, vertical = 5.dp)
            .clip(RoundedCornerShape(SentupRadii.card))
            .clickable(onClick = onPlay)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (coverUrl != null) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = pluralStringResource(R.plurals.n_songs, mix.songs.size, mix.songs.size) +
                        " · " + formatCompactDuration(mix.totalDurationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            FilledTonalIconButton(onClick = onPlay) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.lib_folder_play_all))
            }
            IconButton(onClick = onShuffle) {
                Icon(Icons.Rounded.Shuffle, contentDescription = stringResource(R.string.lib_folder_shuffle))
            }
        }
    }
}

/**
 * Floating bottom action bar when multi-select mode is active in the Music Library.
 */
@Composable
fun MultiSelectActionBar(
    selectedCount: Int,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onShuffleSelected: () -> Unit,
    onAddToFolder: (() -> Unit)? = null,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(visible = selectedCount > 0, modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(SentupRadii.pill),
            color = MaterialTheme.colorScheme.primaryContainer,
            tonalElevation = 6.dp,
            shadowElevation = 6.dp,
            modifier = Modifier
                .padding(horizontal = SentupGroup.inset, vertical = 8.dp)
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.lib_selected_count, selectedCount),
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onPlayNext) {
                    Icon(
                        Icons.AutoMirrored.Rounded.PlaylistAdd,
                        contentDescription = stringResource(R.string.lib_action_queue_next),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onAddToQueue) {
                    Icon(
                        Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = stringResource(R.string.lib_action_queue_last),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                IconButton(onClick = onShuffleSelected) {
                    Icon(
                        Icons.Rounded.Shuffle,
                        contentDescription = stringResource(R.string.lib_folder_shuffle),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                if (onAddToFolder != null) {
                    IconButton(onClick = onAddToFolder) {
                        Icon(
                            Icons.Rounded.CreateNewFolder,
                            contentDescription = stringResource(R.string.lib_custom_folder_add_to),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                IconButton(onClick = onClear) {
                    Icon(
                        Icons.Rounded.Clear,
                        contentDescription = stringResource(R.string.lib_action_clear),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
