package com.sentup.android.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCut
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sentup.android.R
import com.sentup.android.data.ArtistItem
import com.sentup.android.data.CustomFolderTree
import com.sentup.android.data.MusicFolder
import com.sentup.android.data.PlaylistDisplayItem
import com.sentup.android.data.Song
import com.sentup.android.data.googleImageAtSize
import com.sentup.android.data.isUnknownAlbum
import com.sentup.android.data.isUnknownArtist
import com.sentup.android.data.sortedInAlbumOrder
import com.sentup.android.ui.home.HomeViewModel
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii
import java.util.Locale
import kotlinx.coroutines.delay

enum class FolderSourceTab {
    Custom,
    ChannelsAuto
}

val FolderColorPalette: List<Long> = listOf(
    0xFF4CAF50L, // Green
    0xFF2196F3L, // Blue
    0xFF9C27B0L, // Purple
    0xFFE91E63L, // Pink
    0xFFFF9800L, // Orange
    0xFF009688L  // Teal
)

/**
 * Emits the user-customizable [MusicFolder] tree (create, rename, color, pin, move, add/remove songs).
 */
fun LazyListScope.customFolderBrowserItems(
    folders: List<MusicFolder>,
    allSongs: List<Song>,
    songsById: Map<String, Song>,
    currentFolderId: String?,
    searchQuery: String,
    likedIds: Set<String>,
    downloadedIds: Set<String>,
    playCounts: Map<String, Int> = emptyMap(),
    onNavigateFolder: (String?) -> Unit,
    onCreateFolderRequest: (parentId: String?) -> Unit,
    onRenameFolderRequest: (MusicFolder) -> Unit,
    onMoveFolderRequest: (MusicFolder) -> Unit,
    onDeleteFolder: (MusicFolder) -> Unit,
    onTogglePinFolder: (MusicFolder) -> Unit,
    onAddSongsRequest: (MusicFolder) -> Unit,
    onFolderAudioProfileRequest: ((MusicFolder) -> Unit)? = null,
    onFolderAutoRuleRequest: ((MusicFolder) -> Unit)? = null,
    onPlayFolderWithProfile: ((MusicFolder, List<Song>, Boolean) -> Unit)? = null,
    onOpenTrackTools: ((Song) -> Unit)? = null,
    onExportImportFoldersRequest: (() -> Unit)? = null,
    onRemoveArtistFromFolder: (folderId: String, artistName: String) -> Unit,
    onRemoveAlbumFromFolder: (folderId: String, albumName: String) -> Unit,
    onRemoveSongFromFolder: (folderId: String, songId: String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenAlbum: (String, List<Song>) -> Unit,
    onPlayQueue: (List<Song>, Song?) -> Unit,
    onShuffleQueue: (List<Song>) -> Unit,
    onSongLongPress: ((Song) -> Unit)?
) {
    val breadcrumbs = CustomFolderTree.breadcrumbPath(folders, currentFolderId)
    val currentFolder = breadcrumbs.lastOrNull()
    val normalizedQuery = searchQuery.trim().lowercase(Locale.ROOT)

    val childFolders = CustomFolderTree.childFoldersOf(folders, currentFolder?.id)
        .filter { folder ->
            normalizedQuery.isEmpty() || folder.name.lowercase(Locale.ROOT).contains(normalizedQuery)
        }

    val folderArtists = currentFolder?.artistNames.orEmpty()
        .filter { normalizedQuery.isEmpty() || it.lowercase(Locale.ROOT).contains(normalizedQuery) }
    val folderAlbums = currentFolder?.albumNames.orEmpty()
        .filter { normalizedQuery.isEmpty() || it.lowercase(Locale.ROOT).contains(normalizedQuery) }
    val directSongs = if (currentFolder == null) {
        emptyList()
    } else {
        val explicit = currentFolder.songIds.mapNotNull { songsById[it] }
        val autoMatched = if (currentFolder.autoRule != null && !currentFolder.autoRule.isEmpty) {
            CustomFolderTree.resolveFolderSongs(currentFolder, allSongs, playCounts, downloadedIds)
        } else {
            emptyList()
        }
        (explicit + autoMatched)
            .distinctBy { it.id }
            .filter { song ->
                normalizedQuery.isEmpty() ||
                    song.title.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                    song.artist.lowercase(Locale.ROOT).contains(normalizedQuery)
            }
    }

    // Breadcrumb & action bar
    item(key = "custom_folder_toolbar") {
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
                    selected = currentFolder == null,
                    onClick = { onNavigateFolder(null) },
                    leadingIcon = {
                        Icon(Icons.Rounded.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                    },
                    label = { Text(stringResource(R.string.lib_custom_folder_root)) }
                )
                breadcrumbs.forEach { crumb ->
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    FilterChip(
                        selected = crumb.id == currentFolder?.id,
                        onClick = { onNavigateFolder(crumb.id) },
                        label = {
                            Text(
                                text = crumb.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilledTonalButton(
                    onClick = { onCreateFolderRequest(currentFolder?.id) },
                    shape = RoundedCornerShape(SentupRadii.pill),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.lib_custom_folder_new))
                }
                if (onExportImportFoldersRequest != null) {
                    OutlinedButton(
                        onClick = onExportImportFoldersRequest,
                        shape = RoundedCornerShape(SentupRadii.pill),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.lib_folder_export_import))
                    }
                }
                if (currentFolder != null) {
                    FilledTonalButton(
                        onClick = { onAddSongsRequest(currentFolder) },
                        shape = RoundedCornerShape(SentupRadii.pill),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.lib_custom_folder_add_content))
                    }
                    val allSubtreeSongs = CustomFolderTree.allDescendantSongs(folders, currentFolder.id, allSongs)
                    if (allSubtreeSongs.isNotEmpty()) {
                        FilledTonalIconButton(
                            onClick = { onPlayQueue(allSubtreeSongs, allSubtreeSongs.firstOrNull()) }
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.lib_folder_play_all))
                        }
                        FilledTonalIconButton(
                            onClick = { onShuffleQueue(allSubtreeSongs) }
                        ) {
                            Icon(Icons.Rounded.Shuffle, contentDescription = stringResource(R.string.lib_folder_shuffle))
                        }
                    }
                }
            }
        }
    }

    if (childFolders.isEmpty() && folderArtists.isEmpty() && folderAlbums.isEmpty() && directSongs.isEmpty()) {
        item(key = "custom_folder_empty") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SentupGroup.inset, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier.size(48.dp)
                )
                Text(
                    text = stringResource(R.string.lib_custom_folder_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = { onCreateFolderRequest(currentFolder?.id) },
                    shape = RoundedCornerShape(SentupRadii.pill)
                ) {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.lib_custom_folder_new))
                }
            }
        }
        return
    }

    items(childFolders, key = { "custom_folder_${it.id}" }) { folder ->
        val subfolderCount = folders.count { it.parentId == folder.id }
        val descendantSongs = CustomFolderTree.allDescendantSongs(
            folders = folders,
            folderId = folder.id,
            allSongs = allSongs,
            playCounts = playCounts,
            downloadedIds = downloadedIds
        )
        CustomMusicFolderCard(
            folder = folder,
            subfolderCount = subfolderCount,
            songCount = descendantSongs.size,
            totalDurationMs = descendantSongs.sumOf { it.duration.coerceAtLeast(0L) },
            onOpen = { onNavigateFolder(folder.id) },
            onPlayAll = {
                if (descendantSongs.isNotEmpty()) {
                    if (onPlayFolderWithProfile != null) {
                        onPlayFolderWithProfile(folder, descendantSongs, false)
                    } else {
                        onPlayQueue(descendantSongs, descendantSongs.firstOrNull())
                    }
                }
            },
            onShuffle = {
                if (descendantSongs.isNotEmpty()) {
                    if (onPlayFolderWithProfile != null) {
                        onPlayFolderWithProfile(folder, descendantSongs, true)
                    } else {
                        onShuffleQueue(descendantSongs)
                    }
                }
            },
            onTogglePin = { onTogglePinFolder(folder) },
            onRename = { onRenameFolderRequest(folder) },
            onMove = { onMoveFolderRequest(folder) },
            onAudioProfile = onFolderAudioProfileRequest?.let { cb -> { cb(folder) } },
            onAutoRule = onFolderAutoRuleRequest?.let { cb -> { cb(folder) } },
            onPlayHarmonic = if (descendantSongs.size > 1) {
                {
                    val ordered = com.sentup.android.ml.AudioFeatureAndDspEngine.optimizeQueueTransitions(descendantSongs)
                    onPlayQueue(ordered, ordered.firstOrNull())
                }
            } else null,
            onPlayEnergyArc = if (descendantSongs.size > 1) {
                {
                    val ordered = com.sentup.android.ml.AudioFeatureAndDspEngine.sortFolderByEnergyArc(descendantSongs)
                    onPlayQueue(ordered, ordered.firstOrNull())
                }
            } else null,
            onDelete = { onDeleteFolder(folder) }
        )
    }

    if (currentFolder != null && folderArtists.isNotEmpty()) {
        items(folderArtists, key = { "custom_artist_${currentFolder.id}_$it" }) { artistName ->
            val artistSongs = allSongs.filter { it.artist.trim().equals(artistName.trim(), ignoreCase = true) }
            CustomFolderLinkedItemRow(
                title = artistName,
                subtitle = pluralStringResource(R.plurals.n_songs, artistSongs.size, artistSongs.size),
                icon = Icons.Rounded.Person,
                onClick = { onOpenArtist(artistName) },
                onPlay = { if (artistSongs.isNotEmpty()) onPlayQueue(artistSongs, artistSongs.firstOrNull()) },
                onRemove = { onRemoveArtistFromFolder(currentFolder.id, artistName) }
            )
        }
    }

    if (currentFolder != null && folderAlbums.isNotEmpty()) {
        items(folderAlbums, key = { "custom_album_${currentFolder.id}_$it" }) { albumName ->
            val albumSongs = allSongs.filter { it.album.trim().equals(albumName.trim(), ignoreCase = true) }
                .sortedInAlbumOrder()
            CustomFolderLinkedItemRow(
                title = albumName,
                subtitle = pluralStringResource(R.plurals.n_songs, albumSongs.size, albumSongs.size),
                icon = Icons.Rounded.Album,
                onClick = { onOpenAlbum(albumName, albumSongs) },
                onPlay = { if (albumSongs.isNotEmpty()) onPlayQueue(albumSongs, albumSongs.firstOrNull()) },
                onRemove = { onRemoveAlbumFromFolder(currentFolder.id, albumName) }
            )
        }
    }

    if (currentFolder != null && directSongs.isNotEmpty()) {
        items(directSongs, key = { "custom_song_${currentFolder.id}_${it.id}" }) { song ->
            CustomFolderSongRow(
                song = song,
                isLiked = song.id in likedIds,
                isDownloaded = song.id in downloadedIds,
                onClick = { onPlayQueue(directSongs, song) },
                onLongClick = { onSongLongPress?.invoke(song) },
                onOpenTrackTools = onOpenTrackTools?.let { cb -> { cb(song) } },
                onRemove = { onRemoveSongFromFolder(currentFolder.id, song.id) }
            )
        }
    }
}

@Composable
private fun CustomFolderLinkedItemRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
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
            }
            FilledTonalIconButton(onClick = onPlay) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.lib_folder_play_all))
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.lib_custom_folder_remove_song),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CustomMusicFolderCard(
    folder: MusicFolder,
    subfolderCount: Int,
    songCount: Int,
    totalDurationMs: Long,
    onOpen: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onAudioProfile: (() -> Unit)? = null,
    onAutoRule: (() -> Unit)? = null,
    onPlayHarmonic: (() -> Unit)? = null,
    onPlayEnergyArc: (() -> Unit)? = null,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val folderColor = folder.colorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primary

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
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(folderColor.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Folder,
                    contentDescription = null,
                    tint = folderColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (folder.isPinned) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.lib_custom_folder_meta, subfolderCount, songCount) +
                        if (songCount > 0) " · ${formatCompactDuration(totalDurationMs)}" else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (songCount > 0) {
                FilledTonalIconButton(onClick = onPlayAll) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = stringResource(R.string.lib_folder_play_all))
                }
                IconButton(onClick = onShuffle) {
                    Icon(Icons.Rounded.Shuffle, contentDescription = stringResource(R.string.lib_folder_shuffle))
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = null)
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(
                                    if (folder.isPinned) R.string.lib_folder_unpin else R.string.lib_folder_pin
                                )
                            )
                        },
                        leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onTogglePin()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.lib_custom_folder_rename)) },
                        leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onRename()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.lib_custom_folder_move)) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Rounded.DriveFileMove, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onMove()
                        }
                    )
                    if (onAudioProfile != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lib_folder_audio_profile)) },
                            leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onAudioProfile()
                            }
                        )
                    }
                    if (onAutoRule != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lib_folder_auto_rule)) },
                            leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onAutoRule()
                            }
                        )
                    }
                    if (onPlayHarmonic != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lib_folder_sort_harmonic)) },
                            leadingIcon = { Icon(Icons.Rounded.Shuffle, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onPlayHarmonic()
                            }
                        )
                    }
                    if (onPlayEnergyArc != null) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lib_folder_sort_arc)) },
                            leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onPlayEnergyArc()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.lib_custom_folder_delete)) },
                        leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                        onClick = {
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomFolderSongRow(
    song: Song,
    isLiked: Boolean,
    isDownloaded: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onOpenTrackTools: (() -> Unit)? = null,
    onRemove: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
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
                    text = "${song.artist} · ${formatTrackDuration(song.duration)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (onOpenTrackTools != null) {
                IconButton(onClick = onOpenTrackTools) {
                    Icon(
                        imageVector = Icons.Rounded.ContentCut,
                        contentDescription = stringResource(R.string.lib_track_tools),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            IconButton(onClick = onRemove) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.lib_custom_folder_remove_song),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Dialog to create a new custom folder or rename/recolor an existing one.
 */
@Composable
fun CreateOrRenameFolderDialog(
    initialName: String = "",
    initialColorHex: Long? = FolderColorPalette.first(),
    isRename: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: Long?) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var selectedColor by remember { mutableStateOf(initialColorHex ?: FolderColorPalette.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (isRename) R.string.lib_custom_folder_rename else R.string.lib_custom_folder_new
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.lib_custom_folder_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = stringResource(R.string.lib_custom_folder_color),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    FolderColorPalette.forEach { hex ->
                        val isSelected = selectedColor == hex
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(hex))
                                .then(
                                    if (isSelected) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else Modifier
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim(), selectedColor) },
                enabled = name.trim().isNotEmpty()
            ) {
                Text(
                    stringResource(
                        if (isRename) R.string.lib_custom_folder_save else R.string.lib_custom_folder_create
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * Dialog to pick a target folder (for moving a folder or adding songs to a folder).
 */
@Composable
fun PickTargetFolderDialog(
    title: String,
    folders: List<MusicFolder>,
    includeRoot: Boolean,
    excludedFolderIds: Set<String> = emptySet(),
    onCreateNewFolder: (() -> Unit)? = null,
    onSelectFolder: (folderId: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val available = folders.filterNot { it.id in excludedFolderIds }
        .sortedBy { it.name.lowercase(Locale.ROOT) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(modifier = Modifier.heightIn(max = 320.dp)) {
                if (includeRoot) {
                    item(key = "pick_root") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { onSelectFolder(null) }
                                .padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Rounded.FolderOpen, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Text(stringResource(R.string.lib_custom_folder_root), fontWeight = FontWeight.Medium)
                        }
                    }
                }
                items(available, key = { "pick_${it.id}" }) { folder ->
                    val folderColor = folder.colorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectFolder(folder.id) }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(Icons.Rounded.Folder, contentDescription = null, tint = folderColor)
                        Text(folder.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        confirmButton = {
            if (onCreateNewFolder != null) {
                TextButton(onClick = onCreateNewFolder) {
                    Text(stringResource(R.string.lib_custom_folder_new))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

private enum class FolderContentPickerTab { Artists, Albums, Songs }

/**
 * Bottom sheet with 3 tabs (Artists, Albums, Songs) to add any combination into [targetFolder].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickContentForFolderSheet(
    targetFolder: MusicFolder,
    allSongs: List<Song>,
    /** Runs the same live YouTube Music search the main search screen runs. */
    viewModel: HomeViewModel,
    onConfirm: (artists: List<String>, albums: List<String>, songIds: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf(FolderContentPickerTab.Artists) }
    var query by remember { mutableStateOf("") }
    var selectedArtists by remember { mutableStateOf(targetFolder.artistNames.toSet()) }
    var selectedAlbums by remember { mutableStateOf(targetFolder.albumNames.toSet()) }
    var selectedSongIds by remember { mutableStateOf(targetFolder.songIds.toSet()) }
    var showPreviewList by remember { mutableStateOf(false) }

    val previewSongs = remember(allSongs, selectedArtists, selectedAlbums, selectedSongIds, targetFolder) {
        CustomFolderTree.resolveFolderSongs(
            folder = targetFolder.copy(
                artistNames = selectedArtists.toList(),
                albumNames = selectedAlbums.toList(),
                songIds = selectedSongIds.toList()
            ),
            allSongs = allSongs
        )
    }
    val previewDurationMs = remember(previewSongs) {
        previewSongs.sumOf { it.duration.coerceAtLeast(0L) }
    }

    val q = query.trim().lowercase(Locale.ROOT)

    val artistEntries = remember(allSongs, q) {
        allSongs.filterNot { isUnknownArtist(it.artist) }
            .groupBy { it.artist.trim() }
            .entries
            .filter { q.isEmpty() || it.key.lowercase(Locale.ROOT).contains(q) }
            .sortedBy { it.key.lowercase(Locale.ROOT) }
    }

    val albumEntries = remember(allSongs, q) {
        allSongs.filterNot { isUnknownAlbum(it.album) }
            .groupBy { it.album.trim() }
            .entries
            .filter { (albumName, songs) ->
                q.isEmpty() ||
                    albumName.lowercase(Locale.ROOT).contains(q) ||
                    songs.any { it.artist.lowercase(Locale.ROOT).contains(q) }
            }
            .sortedBy { it.key.lowercase(Locale.ROOT) }
    }

    val filteredSongs = remember(allSongs, q) {
        if (q.isEmpty()) allSongs
        else allSongs.filter {
            it.title.lowercase(Locale.ROOT).contains(q) ||
                it.artist.lowercase(Locale.ROOT).contains(q) ||
                it.album.lowercase(Locale.ROOT).contains(q)
        }
    }

    // Live YouTube Music preview - the same search the main music search
    // runs, scoped to the active tab (Artists / Albums / Songs). Typing past
    // two characters pulls YouTube results in after the same 500 ms debounce,
    // so a folder can be filled from things that are not on the device yet.
    // The preview is best-effort: offline or refused, the local filter above
    // stays the only list and nothing here crashes the sheet.
    var ytPreviewLoading by remember { mutableStateOf(false) }
    var ytArtists by remember { mutableStateOf<List<ArtistItem>>(emptyList()) }
    var ytAlbums by remember { mutableStateOf<List<PlaylistDisplayItem>>(emptyList()) }
    var ytSongs by remember { mutableStateOf<List<Song>>(emptyList()) }

    LaunchedEffect(query, activeTab) {
        val onlineQuery = query.trim()
        if (onlineQuery.length < 2) {
            ytArtists = emptyList()
            ytAlbums = emptyList()
            ytSongs = emptyList()
            ytPreviewLoading = false
            return@LaunchedEffect
        }
        // Only the active tab's list is allowed to linger; the other two
        // would otherwise resurface whenever the user flips back mid-word.
        when (activeTab) {
            FolderContentPickerTab.Artists -> {
                ytAlbums = emptyList(); ytSongs = emptyList()
            }
            FolderContentPickerTab.Albums -> {
                ytArtists = emptyList(); ytSongs = emptyList()
            }
            FolderContentPickerTab.Songs -> {
                ytArtists = emptyList(); ytAlbums = emptyList()
            }
        }
        ytPreviewLoading = true
        delay(500) // Same debounce as the main search.
        try {
            when (activeTab) {
                FolderContentPickerTab.Artists -> ytArtists = viewModel.searchArtists(onlineQuery)
                FolderContentPickerTab.Albums -> ytAlbums = viewModel.searchAlbums(onlineQuery)
                FolderContentPickerTab.Songs -> ytSongs = viewModel.searchYouTube(onlineQuery)
            }
        } catch (_: Exception) {
            // Network refused or offline: keep whatever is local.
        }
        ytPreviewLoading = false
    }

    val ytPreviewVisible = query.trim().length >= 2

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${targetFolder.name} · ${stringResource(R.string.lib_custom_folder_add_content)}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Button(
                    onClick = {
                        onConfirm(
                            selectedArtists.toList(),
                            selectedAlbums.toList(),
                            selectedSongIds.toList()
                        )
                    },
                    shape = RoundedCornerShape(SentupRadii.pill)
                ) {
                    Text(stringResource(R.string.lib_custom_folder_save))
                }
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val tabs = listOf(
                    Triple(FolderContentPickerTab.Artists, R.string.lib_picker_tab_artists, Icons.Rounded.Person),
                    Triple(FolderContentPickerTab.Albums, R.string.lib_picker_tab_albums, Icons.Rounded.Album),
                    Triple(FolderContentPickerTab.Songs, R.string.lib_picker_tab_songs, Icons.Rounded.MusicNote)
                )
                tabs.forEachIndexed { idx, (tab, labelRes, icon) ->
                    SegmentedButton(
                        selected = activeTab == tab,
                        onClick = { activeTab = tab },
                        shape = SegmentedButtonDefaults.itemShape(index = idx, count = tabs.size),
                        icon = { Icon(icon, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    ) {
                        Text(stringResource(labelRes), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.lib_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(SentupRadii.pill),
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (previewSongs.isEmpty()) {
                                stringResource(R.string.lib_folder_preview_empty)
                            } else {
                                stringResource(
                                    R.string.lib_folder_preview_summary,
                                    previewSongs.size,
                                    formatCompactDuration(previewDurationMs),
                                    selectedArtists.size,
                                    selectedAlbums.size,
                                    selectedSongIds.size
                                )
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        if (previewSongs.isNotEmpty()) {
                            TextButton(onClick = { showPreviewList = !showPreviewList }) {
                                Text(
                                    stringResource(
                                        if (showPreviewList) R.string.lib_folder_preview_hide
                                        else R.string.lib_folder_preview_show
                                    )
                                )
                            }
                        }
                    }
                    if (showPreviewList && previewSongs.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.heightIn(max = 140.dp)) {
                            items(previewSongs, key = { "preview_${it.id}" }) { song ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.MusicNote,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "${song.title} · ${song.artist}",
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = formatTrackDuration(song.duration),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                when (activeTab) {
                    FolderContentPickerTab.Artists -> {
                        if (ytPreviewVisible) {
                            item(key = "yt_head_ar") {
                                FolderPickerSectionHeader(
                                    title = stringResource(R.string.lib_picker_yt_preview),
                                    isLoading = ytPreviewLoading
                                )
                            }
                            items(ytArtists, key = { "yt_ar_${it.id}" }) { artist ->
                                val artistName = artist.name.trim()
                                val checked = selectedArtists.any { it.equals(artistName, ignoreCase = true) }
                                PickerCheckRow(
                                    title = artistName,
                                    subtitle = artist.subscriberCount ?: "",
                                    checked = checked,
                                    thumbnailUrl = artist.thumbnailUrl,
                                    roundArt = true,
                                    onToggle = {
                                        selectedArtists = if (checked) {
                                            selectedArtists.filterNot { it.equals(artistName, ignoreCase = true) }.toSet()
                                        } else {
                                            selectedArtists + artistName
                                        }
                                    }
                                )
                            }
                            if (artistEntries.isNotEmpty()) {
                                item(key = "local_head_ar") {
                                    FolderPickerSectionHeader(
                                        title = stringResource(R.string.source_local_library),
                                        isLoading = false
                                    )
                                }
                            }
                        }
                        items(artistEntries, key = { "pick_ar_${it.key}" }) { (artistName, songs) ->
                            val checked = artistName in selectedArtists
                            PickerCheckRow(
                                title = artistName,
                                subtitle = pluralStringResource(R.plurals.n_songs, songs.size, songs.size),
                                checked = checked,
                                roundArt = true,
                                onToggle = {
                                    selectedArtists = if (checked) selectedArtists - artistName else selectedArtists + artistName
                                }
                            )
                        }
                    }
                    FolderContentPickerTab.Albums -> {
                        if (ytPreviewVisible) {
                            item(key = "yt_head_al") {
                                FolderPickerSectionHeader(
                                    title = stringResource(R.string.lib_picker_yt_preview),
                                    isLoading = ytPreviewLoading
                                )
                            }
                            items(ytAlbums, key = { "yt_al_${it.id}" }) { album ->
                                val albumName = album.name.trim()
                                val checked = selectedAlbums.any { it.equals(albumName, ignoreCase = true) }
                                PickerCheckRow(
                                    title = albumName,
                                    subtitle = album.uploaderName,
                                    checked = checked,
                                    thumbnailUrl = album.thumbnailUrl,
                                    onToggle = {
                                        selectedAlbums = if (checked) {
                                            selectedAlbums.filterNot { it.equals(albumName, ignoreCase = true) }.toSet()
                                        } else {
                                            selectedAlbums + albumName
                                        }
                                    }
                                )
                            }
                            if (albumEntries.isNotEmpty()) {
                                item(key = "local_head_al") {
                                    FolderPickerSectionHeader(
                                        title = stringResource(R.string.source_local_library),
                                        isLoading = false
                                    )
                                }
                            }
                        }
                        items(albumEntries, key = { "pick_al_${it.key}" }) { (albumName, songs) ->
                            val checked = albumName in selectedAlbums
                            val artistLabel = songs.firstOrNull()?.artist.orEmpty()
                            PickerCheckRow(
                                title = albumName,
                                subtitle = "$artistLabel · " + pluralStringResource(R.plurals.n_songs, songs.size, songs.size),
                                checked = checked,
                                thumbnailUrl = songs.firstOrNull()?.let { it.thumbnailUrl ?: it.albumArtUri?.toString() },
                                onToggle = {
                                    selectedAlbums = if (checked) selectedAlbums - albumName else selectedAlbums + albumName
                                }
                            )
                        }
                    }
                    FolderContentPickerTab.Songs -> {
                        if (ytPreviewVisible) {
                            item(key = "yt_head_so") {
                                FolderPickerSectionHeader(
                                    title = stringResource(R.string.lib_picker_yt_preview),
                                    isLoading = ytPreviewLoading
                                )
                            }
                            items(ytSongs, key = { "yt_song_${it.id}" }) { song ->
                                val checked = song.id in selectedSongIds
                                PickerCheckRow(
                                    title = song.title,
                                    subtitle = song.artist,
                                    checked = checked,
                                    thumbnailUrl = song.thumbnailUrl,
                                    onToggle = {
                                        selectedSongIds = if (checked) selectedSongIds - song.id else selectedSongIds + song.id
                                    }
                                )
                            }
                            if (filteredSongs.isNotEmpty()) {
                                item(key = "local_head_so") {
                                    FolderPickerSectionHeader(
                                        title = stringResource(R.string.source_local_library),
                                        isLoading = false
                                    )
                                }
                            }
                        }
                        items(filteredSongs, key = { "pick_song_${it.id}" }) { song ->
                            val checked = song.id in selectedSongIds
                            PickerCheckRow(
                                title = song.title,
                                subtitle = "${song.artist} · ${song.album}",
                                checked = checked,
                                thumbnailUrl = song.thumbnailUrl ?: song.albumArtUri?.toString(),
                                onToggle = {
                                    selectedSongIds = if (checked) selectedSongIds - song.id else selectedSongIds + song.id
                                }
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

/**
 * The small section label inside the folder content picker: the live
 * "YouTube Music preview" header and, below it, the "From your library"
 * divider. The spinner rides on the preview header only.
 */
@Composable
private fun FolderPickerSectionHeader(title: String, isLoading: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.weight(1f)
        )
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

@Composable
private fun PickerCheckRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: () -> Unit,
    thumbnailUrl: String? = null,
    roundArt: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        PickerRowArtwork(url = thumbnailUrl, round = roundArt)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.Medium)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * The artwork slot of a picker row. Every row carries one: a device entry with
 * no cover still reserves the space, so YouTube and local rows line up in the
 * same column. Google images are asked for at the size they are drawn, and an
 * artist wears a circle while a release wears a rounded square.
 */
@Composable
private fun PickerRowArtwork(url: String?, round: Boolean) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(if (round) CircleShape else RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (url.isNullOrBlank()) {
            Icon(
                imageVector = if (round) Icons.Rounded.Person else Icons.Rounded.MusicNote,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            AsyncImage(
                model = googleImageAtSize(url, PICKER_ART_PX),
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/** A 40dp slot on the densest supported screen. */
private const val PICKER_ART_PX = 132
