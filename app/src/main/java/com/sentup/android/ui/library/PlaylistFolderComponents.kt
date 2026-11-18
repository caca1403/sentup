package com.sentup.android.ui.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DriveFileMove
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOff
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sentup.android.R
import com.sentup.android.data.CreatePlaylistFolderResult
import com.sentup.android.data.PlaylistDisplayItem
import com.sentup.android.data.PlaylistFolder
import com.sentup.android.data.PlaylistFolderLogic
import com.sentup.android.data.PlaylistFolderSortOrder
import com.sentup.android.data.PlaylistSourceFilter
import com.sentup.android.ui.components.displaySubtitle
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard
import java.util.Locale

@Composable
fun PlaylistFolderArtwork(
    thumbnails: List<String>,
    modifier: Modifier = Modifier,
    iconSize: Int = 38
) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        when {
            thumbnails.isEmpty() -> {
                Icon(
                    imageVector = Icons.Rounded.FolderOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(iconSize.dp)
                )
            }
            thumbnails.size == 1 -> {
                AsyncImage(
                    model = thumbnails.first(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            else -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        CollageCell(thumbnails.getOrNull(0), Modifier.weight(1f).fillMaxHeight())
                        CollageCell(thumbnails.getOrNull(1), Modifier.weight(1f).fillMaxHeight())
                    }
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        CollageCell(thumbnails.getOrNull(2), Modifier.weight(1f).fillMaxHeight())
                        CollageCell(thumbnails.getOrNull(3), Modifier.weight(1f).fillMaxHeight())
                    }
                }
            }
        }
    }
}

@Composable
private fun CollageCell(url: String?, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center
    ) {
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun PlaylistFolderCard(
    folder: PlaylistFolder,
    playlistsInFolder: List<PlaylistDisplayItem>,
    onClick: () -> Unit,
    onAddPlaylists: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    selected: Boolean = false
) {
    var showMenu by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "folderCardScale"
    )
    val coverShape = RoundedCornerShape(SentupRadii.tile)
    val thumbnails = remember(playlistsInFolder) {
        playlistsInFolder
            .mapNotNull { it.thumbnailUrl?.takeIf { u -> u.isNotBlank() && u != "null" } }
            .distinct()
            .take(4)
    }
    val totalSongs = remember(playlistsInFolder) {
        playlistsInFolder.sumOf { it.itemCount.coerceAtLeast(0) }
    }
    val countLabel = if (playlistsInFolder.size == 1) {
        stringResource(R.string.pl_folder_count_one_list)
    } else {
        stringResource(R.string.pl_folder_count_lists, playlistsInFolder.size)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
    ) {
        Box(
            modifier = Modifier
                .aspectRatio(1f)
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
                .sentupCard(shape = coverShape, elevation = 1, tinted = false)
                .then(
                    if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, coverShape)
                    else Modifier
                )
        ) {
            PlaylistFolderArtwork(
                thumbnails = thumbnails,
                modifier = Modifier.fillMaxSize()
            )
            // Top-start folder badge + pin indicator
            Row(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(SentupRadii.pill)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                            .size(14.dp)
                    )
                }
                if (folder.isPinned) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(SentupRadii.pill)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier
                                .padding(horizontal = 6.dp, vertical = 4.dp)
                                .size(14.dp)
                        )
                    }
                }
            }
            // Bottom-end playlist count pill
            Surface(
                color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f),
                shape = RoundedCornerShape(SentupRadii.pill),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
            ) {
                Text(
                    text = countLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                val subtitleText = if (playlistsInFolder.isEmpty()) {
                    stringResource(R.string.pl_folder_empty_title)
                } else if (totalSongs > 0) {
                    "$countLabel • ${pluralStringResource(R.plurals.n_songs, totalSongs, totalSongs)}"
                } else {
                    countLabel
                }
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.cd_playlist_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                FolderDropdownMenu(
                    expanded = showMenu,
                    isPinned = folder.isPinned,
                    onDismiss = { showMenu = false },
                    onOpen = { showMenu = false; onClick() },
                    onAddPlaylists = { showMenu = false; onAddPlaylists() },
                    onTogglePin = { showMenu = false; onTogglePin() },
                    onRename = { showMenu = false; onRename() },
                    onDelete = { showMenu = false; onDelete() }
                )
            }
        }
    }
}

@Composable
fun PlaylistFolderListRow(
    folder: PlaylistFolder,
    playlistsInFolder: List<PlaylistDisplayItem>,
    onClick: () -> Unit,
    onAddPlaylists: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val thumbnails = remember(playlistsInFolder) {
        playlistsInFolder
            .mapNotNull { it.thumbnailUrl?.takeIf { u -> u.isNotBlank() && u != "null" } }
            .distinct()
            .take(4)
    }
    val totalSongs = remember(playlistsInFolder) {
        playlistsInFolder.sumOf { it.itemCount.coerceAtLeast(0) }
    }
    val countLabel = if (playlistsInFolder.size == 1) {
        stringResource(R.string.pl_folder_count_one_list)
    } else {
        stringResource(R.string.pl_folder_count_lists, playlistsInFolder.size)
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(SentupRadii.card),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaylistFolderArtwork(
                thumbnails = thumbnails,
                iconSize = 24,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
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
                val subtitleText = if (playlistsInFolder.isEmpty()) {
                    stringResource(R.string.pl_folder_empty_title)
                } else if (totalSongs > 0) {
                    "$countLabel • ${pluralStringResource(R.plurals.n_songs, totalSongs, totalSongs)}"
                } else {
                    countLabel
                }
                Text(
                    text = subtitleText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.cd_playlist_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FolderDropdownMenu(
                    expanded = showMenu,
                    isPinned = folder.isPinned,
                    onDismiss = { showMenu = false },
                    onOpen = { showMenu = false; onClick() },
                    onAddPlaylists = { showMenu = false; onAddPlaylists() },
                    onTogglePin = { showMenu = false; onTogglePin() },
                    onRename = { showMenu = false; onRename() },
                    onDelete = { showMenu = false; onDelete() }
                )
            }
        }
    }
}

@Composable
private fun FolderDropdownMenu(
    expanded: Boolean,
    isPinned: Boolean,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onAddPlaylists: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.pl_folder_open)) },
            onClick = onOpen,
            leadingIcon = { Icon(Icons.Rounded.FolderOpen, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.pl_folder_add_playlists)) },
            onClick = onAddPlaylists,
            leadingIcon = { Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null) }
        )
        DropdownMenuItem(
            text = {
                Text(
                    stringResource(
                        if (isPinned) R.string.pl_folder_unpin else R.string.pl_folder_pin
                    )
                )
            },
            onClick = onTogglePin,
            leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.pl_folder_rename)) },
            onClick = onRename,
            leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.pl_folder_delete)) },
            onClick = onDelete,
            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) }
        )
    }
}

@Composable
fun PlaylistListRow(
    playlist: PlaylistDisplayItem,
    isLocal: Boolean,
    isSaved: Boolean,
    isEditable: Boolean,
    onClick: () -> Unit,
    onMoveToFolder: (() -> Unit)? = null,
    onRemoveFromFolder: (() -> Unit)? = null,
    onHide: (() -> Unit)? = null,
    onEditConfirmed: (String, String?) -> Unit = { _, _ -> },
    onDeleteConfirmed: () -> Unit = {},
    onRemoveSaved: () -> Unit = {}
) {
    var showMenu by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val displayName = playlist.name.let { listNameLabel(it) }
    val countText = if (playlist.itemCount >= 0) {
        pluralStringResource(R.plurals.n_songs, playlist.itemCount, playlist.itemCount)
    } else {
        playlist.uploaderName.ifBlank { stringResource(R.string.label_playlist) }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(SentupRadii.card),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                val thumb = playlist.thumbnailUrl
                if (thumb != null && thumb != "null" && thumb.isNotBlank()) {
                    AsyncImage(
                        model = thumb,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = if (playlist.id == "LM") Icons.Rounded.Favorite
                        else Icons.AutoMirrored.Rounded.PlaylistPlay,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isSaved) {
                        Icon(
                            imageVector = Icons.Rounded.Bookmark,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                Text(
                    text = countText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.cd_playlist_options),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    onMoveToFolder?.let { move ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pl_folder_move_to)) },
                            onClick = { showMenu = false; move() },
                            leadingIcon = {
                                Icon(Icons.AutoMirrored.Rounded.DriveFileMove, contentDescription = null)
                            }
                        )
                    }
                    onRemoveFromFolder?.let { remove ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.pl_folder_remove_from)) },
                            onClick = { showMenu = false; remove() },
                            leadingIcon = {
                                Icon(Icons.Rounded.FolderOff, contentDescription = null)
                            }
                        )
                    }
                    onHide?.let { hide ->
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.lib_hide_playlist)) },
                            onClick = { showMenu = false; hide() },
                            leadingIcon = { Icon(Icons.Rounded.Close, contentDescription = null) }
                        )
                    }
                    if (isSaved) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.cd_remove_from_library)) },
                            onClick = { showMenu = false; onRemoveSaved() },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) }
                        )
                    } else if (isEditable) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_edit)) },
                            onClick = { showMenu = false; showEditDialog = true },
                            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_delete)) },
                            onClick = { showMenu = false; showDeleteDialog = true },
                            leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null) }
                        )
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        EditPlaylistDialog(
            initialName = displayName,
            initialDescription = playlist.description,
            onDismiss = { showEditDialog = false },
            onConfirm = { newName, newDescription ->
                showEditDialog = false
                onEditConfirmed(newName, newDescription)
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_playlist_q)) },
            text = {
                Text(
                    if (!isLocal) {
                        "This will permanently remove \"$displayName\" from your YouTube account. " +
                            stringResource(R.string.lib_delete_body)
                    } else {
                        "This will permanently remove \"$displayName\"."
                    }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDeleteConfirmed()
                    }
                ) { Text(stringResource(R.string.action_delete)) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

@Composable
fun PlaylistFilterAndSortBar(
    sourceFilter: PlaylistSourceFilter,
    onSourceFilterChange: (PlaylistSourceFilter) -> Unit,
    sortOrder: PlaylistFolderSortOrder,
    onSortOrderChange: (PlaylistFolderSortOrder) -> Unit,
    isGridView: Boolean,
    onToggleGridView: () -> Unit,
    showFoldersChip: Boolean = true,
    modifier: Modifier = Modifier
) {
    var showSortMenu by remember { mutableStateOf(false) }
    val hasActiveFilter = sourceFilter != PlaylistSourceFilter.All ||
        sortOrder != PlaylistFolderSortOrder.Default

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = SentupGroup.inset),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val chips = buildList {
                add(PlaylistSourceFilter.All to R.string.pl_filter_all)
                if (showFoldersChip) {
                    add(PlaylistSourceFilter.FoldersOnly to R.string.pl_filter_folders)
                }
                add(PlaylistSourceFilter.Local to R.string.pl_filter_local)
                add(PlaylistSourceFilter.YouTube to R.string.pl_filter_youtube)
                add(PlaylistSourceFilter.Saved to R.string.pl_filter_saved)
                add(PlaylistSourceFilter.Offline to R.string.pl_filter_offline)
            }
            chips.forEach { (filter, labelRes) ->
                FilterChip(
                    selected = sourceFilter == filter,
                    onClick = { onSourceFilterChange(filter) },
                    label = { Text(stringResource(labelRes)) }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box {
                FilledTonalButton(
                    onClick = { showSortMenu = true },
                    shape = RoundedCornerShape(SentupRadii.pill),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SwapVert,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = sortOrderLabel(sortOrder),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
                DropdownMenu(
                    expanded = showSortMenu,
                    onDismissRequest = { showSortMenu = false }
                ) {
                    PlaylistFolderSortOrder.entries.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(sortOrderLabel(option)) },
                            onClick = {
                                showSortMenu = false
                                onSortOrderChange(option)
                            },
                            trailingIcon = {
                                if (option == sortOrder) {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        )
                    }
                }
            }

            if (hasActiveFilter) {
                TextButton(
                    onClick = {
                        onSourceFilterChange(PlaylistSourceFilter.All)
                        onSortOrderChange(PlaylistFolderSortOrder.Default)
                    },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp)
                ) {
                    Text(
                        text = stringResource(R.string.pl_filter_reset),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = onToggleGridView,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isGridView) Icons.AutoMirrored.Rounded.ViewList
                    else Icons.Rounded.GridView,
                    contentDescription = stringResource(
                        if (isGridView) R.string.pl_view_list else R.string.pl_view_grid
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun sortOrderLabel(order: PlaylistFolderSortOrder): String = when (order) {
    PlaylistFolderSortOrder.Default -> stringResource(R.string.pl_sort_default)
    PlaylistFolderSortOrder.NameAsc -> stringResource(R.string.pl_sort_name_asc)
    PlaylistFolderSortOrder.NameDesc -> stringResource(R.string.pl_sort_name_desc)
    PlaylistFolderSortOrder.MostSongs -> stringResource(R.string.pl_sort_most_songs)
    PlaylistFolderSortOrder.Newest -> stringResource(R.string.pl_sort_newest)
}

@Composable
fun CreateOrRenamePlaylistFolderDialog(
    initialName: String = "",
    isRename: Boolean = false,
    onDismiss: () -> Unit,
    onSubmit: (String) -> String?
) {
    var text by rememberSaveable { mutableStateOf(initialName) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = {
            Icon(
                imageVector = if (isRename) Icons.Rounded.DriveFileRenameOutline else Icons.Rounded.CreateNewFolder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        title = {
            Text(
                stringResource(
                    if (isRename) R.string.pl_folder_rename else R.string.pl_folder_create
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = {
                        text = it
                        errorText = null
                    },
                    label = { Text(stringResource(R.string.pl_folder_name_hint)) },
                    singleLine = true,
                    isError = errorText != null,
                    supportingText = errorText?.let { err -> { Text(err) } },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val err = onSubmit(text)
                    if (err != null) {
                        errorText = err
                    }
                }
            ) {
                Text(
                    stringResource(
                        if (isRename) R.string.action_save else R.string.action_create
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovePlaylistToFolderSheet(
    playlist: PlaylistDisplayItem,
    folders: List<PlaylistFolder>,
    allPlaylists: List<PlaylistDisplayItem>,
    onMoveToFolder: (targetFolderId: String?) -> Unit,
    onCreateAndMove: (folderName: String) -> CreatePlaylistFolderResult,
    onDismiss: () -> Unit
) {
    val currentFolder = remember(folders, playlist.id) {
        PlaylistFolderLogic.folderForPlaylist(folders, playlist.id)
    }
    var showCreateInline by remember { mutableStateOf(false) }
    var newFolderName by remember { mutableStateOf("") }
    var createError by remember { mutableStateOf<String?>(null) }
    val emptyError = stringResource(R.string.pl_folder_empty_error)
    val dupError = stringResource(R.string.pl_folder_duplicate_error)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 20.dp)
        ) {
            Text(
                text = stringResource(R.string.pl_folder_move_to),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                text = playlist.name.let { listNameLabel(it) },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
            Spacer(Modifier.height(10.dp))

            if (showCreateInline) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newFolderName,
                        onValueChange = {
                            newFolderName = it
                            createError = null
                        },
                        label = { Text(stringResource(R.string.pl_folder_name_hint)) },
                        singleLine = true,
                        isError = createError != null,
                        supportingText = createError?.let { err -> { Text(err) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showCreateInline = false }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                when (val res = onCreateAndMove(newFolderName)) {
                                    is CreatePlaylistFolderResult.Created -> onDismiss()
                                    CreatePlaylistFolderResult.BlankName -> createError = emptyError
                                    CreatePlaylistFolderResult.DuplicateName -> createError = dupError
                                }
                            }
                        ) {
                            Text(stringResource(R.string.action_create))
                        }
                    }
                }
            } else {
                Surface(
                    onClick = { showCreateInline = true },
                    shape = RoundedCornerShape(SentupRadii.card),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CreateNewFolder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.pl_folder_create_and_move),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            if (currentFolder != null) {
                Surface(
                    onClick = {
                        onMoveToFolder(null)
                        onDismiss()
                    },
                    shape = RoundedCornerShape(SentupRadii.card),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FolderOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.pl_folder_remove_from),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(folders, key = { it.id }) { folder ->
                    val inside = PlaylistFolderLogic.playlistsInFolder(folder, allPlaylists)
                    val thumbs = PlaylistFolderLogic.folderPreviewThumbnails(folder, allPlaylists)
                    val isCurrent = currentFolder?.id == folder.id
                    Surface(
                        onClick = {
                            onMoveToFolder(folder.id)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(SentupRadii.card),
                        color = if (isCurrent) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainer
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlaylistFolderArtwork(
                                thumbnails = thumbs,
                                iconSize = 20,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = folder.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (inside.size == 1) {
                                        stringResource(R.string.pl_folder_count_one_list)
                                    } else {
                                        stringResource(R.string.pl_folder_count_lists, inside.size)
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isCurrent) {
                                Icon(
                                    imageVector = Icons.Rounded.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPlaylistsToFolderSheet(
    targetFolder: PlaylistFolder,
    allPlaylists: List<PlaylistDisplayItem>,
    allFolders: List<PlaylistFolder>,
    onConfirmAdd: (List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var selectedIds by remember { mutableStateOf(setOf<String>()) }

    val candidates = remember(allPlaylists, targetFolder.playlistIds, query) {
        val inTarget = targetFolder.playlistIds.toSet()
        val q = query.trim().lowercase(Locale.ROOT)
        allPlaylists.filter { pl ->
            pl.id !in inTarget && (q.isEmpty() || pl.name.lowercase(Locale.ROOT).contains(q))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 16.dp)
        ) {
            Text(
                text = stringResource(R.string.pl_folder_add_playlists),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Text(
                text = targetFolder.name,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
            )
            Spacer(Modifier.height(10.dp))

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text(stringResource(R.string.lib_search_hint)) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(Icons.Rounded.Clear, contentDescription = null)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(SentupRadii.pill),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            if (candidates.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.pl_folder_no_candidates),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = false),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(candidates, key = { it.id }) { pl ->
                        val isSelected = pl.id in selectedIds
                        val existingFolder = PlaylistFolderLogic.folderForPlaylist(allFolders, pl.id)
                        Surface(
                            onClick = {
                                selectedIds = if (isSelected) selectedIds - pl.id else selectedIds + pl.id
                            },
                            shape = RoundedCornerShape(SentupRadii.card),
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            } else {
                                MaterialTheme.colorScheme.surfaceContainer
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val thumb = pl.thumbnailUrl
                                    if (thumb != null && thumb != "null" && thumb.isNotBlank()) {
                                        AsyncImage(
                                            model = thumb,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Rounded.PlaylistPlay,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = pl.name.let { listNameLabel(it) },
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    val sub = buildString {
                                        if (pl.itemCount >= 0) {
                                            append(pluralStringResource(R.plurals.n_songs, pl.itemCount, pl.itemCount))
                                        }
                                        if (existingFolder != null) {
                                            if (isNotEmpty()) append(" • ")
                                            append(stringResource(R.string.pl_folder_currently_in, existingFolder.name))
                                        }
                                    }
                                    if (sub.isNotEmpty()) {
                                        Text(
                                            text = sub,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Icon(
                                    imageVector = if (isSelected) Icons.Rounded.CheckCircle
                                    else Icons.Rounded.RadioButtonUnchecked,
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            if (selectedIds.isNotEmpty()) {
                Button(
                    onClick = {
                        onConfirmAdd(selectedIds.toList())
                        onDismiss()
                    },
                    shape = RoundedCornerShape(SentupRadii.pill),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("${stringResource(R.string.pl_folder_add_playlists)} (${selectedIds.size})")
                }
            }
        }
    }
}

@Composable
fun PlaylistFolderDetailScreen(
    folder: PlaylistFolder,
    allPlaylists: List<PlaylistDisplayItem>,
    allFolders: List<PlaylistFolder>,
    localPlaylistIds: Set<String>,
    savedPlaylistIds: Set<String>,
    offlinePlaylistIds: Set<String>,
    isLoggedIn: Boolean,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onPlaylistClick: (PlaylistDisplayItem) -> Unit,
    onAddPlaylistsToFolder: (List<String>) -> Unit,
    onMovePlaylistToFolder: (playlistId: String, targetFolderId: String?) -> Unit,
    onCreateFolderAndMove: (playlistId: String, folderName: String) -> CreatePlaylistFolderResult,
    onRenameFolder: (String) -> String?,
    onDeleteFolder: () -> Unit,
    onTogglePinFolder: () -> Unit,
    onEditPlaylist: (PlaylistDisplayItem, String, String?) -> Unit,
    onDeletePlaylist: (PlaylistDisplayItem) -> Unit,
    onRemoveSavedPlaylist: (PlaylistDisplayItem) -> Unit,
    onHidePlaylist: (PlaylistDisplayItem) -> Unit
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var sourceFilter by rememberSaveable { mutableStateOf(PlaylistSourceFilter.All) }
    var sortOrder by rememberSaveable { mutableStateOf(PlaylistFolderSortOrder.Default) }
    var isGridView by rememberSaveable { mutableStateOf(true) }

    var showAddSheet by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var moveTargetPlaylist by remember { mutableStateOf<PlaylistDisplayItem?>(null) }

    val playlistsInFolder = remember(folder, allPlaylists) {
        PlaylistFolderLogic.playlistsInFolder(folder, allPlaylists)
    }
    val visiblePlaylists = remember(
        playlistsInFolder,
        searchQuery,
        sourceFilter,
        sortOrder,
        localPlaylistIds,
        savedPlaylistIds,
        offlinePlaylistIds
    ) {
        PlaylistFolderLogic.filterAndSortPlaylists(
            playlists = playlistsInFolder,
            query = searchQuery,
            sourceFilter = sourceFilter,
            sortOrder = sortOrder,
            localPlaylistIds = localPlaylistIds,
            savedPlaylistIds = savedPlaylistIds,
            offlinePlaylistIds = offlinePlaylistIds
        )
    }
    val thumbnails = remember(playlistsInFolder) {
        PlaylistFolderLogic.folderPreviewThumbnails(folder, allPlaylists)
    }
    val totalSongs = remember(playlistsInFolder) {
        playlistsInFolder.sumOf { it.itemCount.coerceAtLeast(0) }
    }
    val countLabel = if (playlistsInFolder.size == 1) {
        stringResource(R.string.pl_folder_count_one_list)
    } else {
        stringResource(R.string.pl_folder_count_lists, playlistsInFolder.size)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
            Text(
                text = folder.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = { showAddSheet = true }) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = stringResource(R.string.pl_folder_add_playlists)
                )
            }
            IconButton(onClick = onTogglePinFolder) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = stringResource(
                        if (folder.isPinned) R.string.pl_folder_unpin else R.string.pl_folder_pin
                    ),
                    tint = if (folder.isPinned) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { showRenameDialog = true }) {
                Icon(
                    imageVector = Icons.Rounded.DriveFileRenameOutline,
                    contentDescription = stringResource(R.string.pl_folder_rename)
                )
            }
            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.pl_folder_delete)
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                bottom = contentPadding.calculateBottomPadding() + 120.dp
            )
        ) {
            // Hero header card
            item(key = "folder_hero") {
                Surface(
                    shape = RoundedCornerShape(SentupRadii.card),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SentupGroup.inset, vertical = 6.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PlaylistFolderArtwork(
                            thumbnails = thumbnails,
                            iconSize = 36,
                            modifier = Modifier
                                .size(88.dp)
                                .clip(RoundedCornerShape(SentupRadii.tile))
                        )
                        Spacer(Modifier.width(14.dp))
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = folder.name,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = if (totalSongs > 0) {
                                    "$countLabel • ${pluralStringResource(R.plurals.n_songs, totalSongs, totalSongs)}"
                                } else {
                                    countLabel
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(2.dp))
                            FilledTonalButton(
                                onClick = { showAddSheet = true },
                                shape = RoundedCornerShape(SentupRadii.pill),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.Add,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.pl_folder_add_playlists),
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }
                }
            }

            // Search + Advanced Filter & Sort Bar
            item(key = "folder_search_filter") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(stringResource(R.string.lib_search_hint)) },
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Rounded.Clear, contentDescription = null)
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(SentupRadii.pill),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = SentupGroup.inset)
                    )

                    PlaylistFilterAndSortBar(
                        sourceFilter = sourceFilter,
                        onSourceFilterChange = { sourceFilter = it },
                        sortOrder = sortOrder,
                        onSortOrderChange = { sortOrder = it },
                        isGridView = isGridView,
                        onToggleGridView = { isGridView = !isGridView },
                        showFoldersChip = false
                    )
                }
            }

            if (playlistsInFolder.isEmpty()) {
                item(key = "folder_empty") {
                    EmptyLibraryState(
                        icon = Icons.Rounded.FolderOpen,
                        title = stringResource(R.string.pl_folder_empty_title),
                        subtitle = stringResource(R.string.pl_folder_empty_sub),
                        action = {
                            Button(
                                onClick = { showAddSheet = true },
                                shape = RoundedCornerShape(SentupRadii.pill)
                            ) {
                                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.pl_folder_add_playlists))
                            }
                        }
                    )
                }
            } else if (visiblePlaylists.isEmpty()) {
                item(key = "folder_no_filter_matches") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp, horizontal = SentupGroup.inset),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.pl_filter_no_matches),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (isGridView) {
                items(
                    visiblePlaylists.chunked(2),
                    key = { row -> "fgrid_" + row.joinToString("|") { it.id } }
                ) { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(
                                start = SentupGroup.inset,
                                end = SentupGroup.inset,
                                top = 4.dp,
                                bottom = 18.dp
                            ),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        row.forEach { playlist ->
                            val isLocal = playlist.id in localPlaylistIds
                            val isSaved = playlist.id in savedPlaylistIds
                            val isYouTubeEditable = !isLocal && !isSaved &&
                                playlist.id != "RTM" && playlist.id != "LM"
                            Box(Modifier.weight(1f)) {
                                ExpressivePlaylistCard(
                                    name = playlist.name.let { listNameLabel(it) },
                                    count = playlist.itemCount,
                                    subtitle = when {
                                        isSaved || playlist.itemCount < 0 ->
                                            playlist.displaySubtitle()
                                                .ifBlank { stringResource(R.string.label_playlist) }
                                        else -> null
                                    },
                                    thumbnailUrl = playlist.thumbnailUrl,
                                    description = playlist.description,
                                    isEditable = isLocal || isYouTubeEditable,
                                    isSaved = isSaved,
                                    affectsYouTubeAccount = isYouTubeEditable && isLoggedIn,
                                    onMoveToFolder = { moveTargetPlaylist = playlist },
                                    onRemoveFromFolder = {
                                        onMovePlaylistToFolder(playlist.id, null)
                                    },
                                    onEditConfirmed = { name, desc ->
                                        onEditPlaylist(playlist, name, desc)
                                    },
                                    onDeleteConfirmed = { onDeletePlaylist(playlist) },
                                    onRemoveSaved = { onRemoveSavedPlaylist(playlist) },
                                    onHide = { onHidePlaylist(playlist) },
                                    selected = LocalLibraryOpenItem.current?.playlistId == playlist.id,
                                    onClick = { onPlaylistClick(playlist) }
                                )
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            } else {
                items(visiblePlaylists, key = { "flist_" + it.id }) { playlist ->
                    val isLocal = playlist.id in localPlaylistIds
                    val isSaved = playlist.id in savedPlaylistIds
                    val isYouTubeEditable = !isLocal && !isSaved &&
                        playlist.id != "RTM" && playlist.id != "LM"
                    PlaylistListRow(
                        playlist = playlist,
                        isLocal = isLocal,
                        isSaved = isSaved,
                        isEditable = isLocal || isYouTubeEditable,
                        onClick = { onPlaylistClick(playlist) },
                        onMoveToFolder = { moveTargetPlaylist = playlist },
                        onRemoveFromFolder = { onMovePlaylistToFolder(playlist.id, null) },
                        onHide = { onHidePlaylist(playlist) },
                        onEditConfirmed = { name, desc -> onEditPlaylist(playlist, name, desc) },
                        onDeleteConfirmed = { onDeletePlaylist(playlist) },
                        onRemoveSaved = { onRemoveSavedPlaylist(playlist) }
                    )
                }
            }
        }
    }

    if (showAddSheet) {
        AddPlaylistsToFolderSheet(
            targetFolder = folder,
            allPlaylists = allPlaylists,
            allFolders = allFolders,
            onConfirmAdd = onAddPlaylistsToFolder,
            onDismiss = { showAddSheet = false }
        )
    }

    if (showRenameDialog) {
        CreateOrRenamePlaylistFolderDialog(
            initialName = folder.name,
            isRename = true,
            onDismiss = { showRenameDialog = false },
            onSubmit = { newName ->
                val err = onRenameFolder(newName)
                if (err == null) showRenameDialog = false
                err
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.pl_folder_delete)) },
            text = { Text(stringResource(R.string.pl_folder_delete_confirm, folder.name)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteFolder()
                    }
                ) {
                    Text(stringResource(R.string.action_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    moveTargetPlaylist?.let { targetPl ->
        MovePlaylistToFolderSheet(
            playlist = targetPl,
            folders = allFolders,
            allPlaylists = allPlaylists,
            onMoveToFolder = { targetFolderId ->
                onMovePlaylistToFolder(targetPl.id, targetFolderId)
            },
            onCreateAndMove = { name ->
                onCreateFolderAndMove(targetPl.id, name)
            },
            onDismiss = { moveTargetPlaylist = null }
        )
    }
}

@Composable
fun LibraryQuickAccessGrid(
    likedCount: Int,
    downloadCount: Int,
    readyOfflineCount: Int,
    historyPlayCount: Int,
    onLikedClick: () -> Unit,
    onDownloadsClick: () -> Unit,
    onReadyOfflineClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onStatsClick: () -> Unit,
    onCreatePlaylist: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickAccessTile(
                title = stringResource(R.string.liked_songs),
                subtitle = pluralStringResource(R.plurals.n_songs, likedCount, likedCount),
                icon = Icons.Rounded.Favorite,
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                onClick = onLikedClick,
                modifier = Modifier.weight(1f)
            )
            QuickAccessTile(
                title = stringResource(R.string.fab_downloads),
                subtitle = pluralStringResource(R.plurals.n_songs, downloadCount, downloadCount),
                icon = Icons.Rounded.DownloadDone,
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.78f),
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = onDownloadsClick,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickAccessTile(
                title = stringResource(R.string.ready_offline),
                subtitle = pluralStringResource(R.plurals.n_songs, readyOfflineCount, readyOfflineCount),
                icon = Icons.Rounded.OfflinePin,
                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.72f),
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = onReadyOfflineClick,
                modifier = Modifier.weight(1f)
            )
            QuickAccessTile(
                title = stringResource(R.string.fab_listening_history),
                subtitle = pluralStringResource(R.plurals.n_plays, historyPlayCount, historyPlayCount),
                icon = Icons.Rounded.History,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                contentColor = MaterialTheme.colorScheme.onSurface,
                onClick = onHistoryClick,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilledTonalButton(
                onClick = onStatsClick,
                shape = RoundedCornerShape(SentupRadii.pill),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Icon(Icons.Rounded.BarChart, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.cd_listening_stats),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            FilledTonalButton(
                onClick = onCreatePlaylist,
                shape = RoundedCornerShape(SentupRadii.pill),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.action_new_playlist),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickAccessTile(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(SentupRadii.card),
        color = containerColor,
        contentColor = contentColor,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.78f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun GenreClusterSheet(
    artistNames: List<String>,
    allSongs: List<com.sentup.android.data.Song>,
    artistPhotos: Map<String, String>,
    artistGenres: Map<String, com.sentup.android.ml.GenreClassifierMlEngine.GenrePrediction> = emptyMap(),
    folders: List<com.sentup.android.data.MusicFolder> = emptyList(),
    scopeFolderName: String? = null,
    scopeArtistNames: List<String>? = null,
    isAnalyzing: Boolean = false,
    onCreateGenreFolder: (genreName: String, artists: List<String>) -> Unit,
    onInspectArtistGenre: ((String) -> Unit)? = null,
    onOpenArtist: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scopedArtists = scopeArtistNames.orEmpty()
    val canScopeToFolder = !scopeFolderName.isNullOrBlank() && scopedArtists.isNotEmpty()
    var limitToCurrentFolder by remember(scopeFolderName, scopedArtists) {
        mutableStateOf(canScopeToFolder)
    }
    val activeArtistNames = if (limitToCurrentFolder && canScopeToFolder) {
        scopedArtists
    } else {
        artistNames
    }
    val clusters = remember(activeArtistNames, allSongs, artistGenres, folders) {
        com.sentup.android.ml.GenreClassifierMlEngine.clusterArtistsByGenre(
            artistNames = activeArtistNames,
            allSongs = allSongs,
            precomputed = artistGenres,
            folders = folders
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 20.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Akıllı Tür Ortaklığı (ML)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (limitToCurrentFolder && canScopeToFolder) {
                            "$scopeFolderName klasöründeki ${activeArtistNames.size} sanatçı analiz edildi"
                        } else {
                            "iTunes/Deezer + sözcük + klasör ortaklığı ile ${activeArtistNames.size} sanatçı gruplandı"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (canScopeToFolder) {
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = limitToCurrentFolder,
                        onClick = { limitToCurrentFolder = true },
                        label = {
                            Text(
                                text = "Bu Klasör ($scopeFolderName)",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                    FilterChip(
                        selected = !limitToCurrentFolder,
                        onClick = { limitToCurrentFolder = false },
                        label = { Text("Tüm Kitaplık (${artistNames.size})") }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (isAnalyzing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                )
                Spacer(Modifier.height(8.dp))
            }

            if (clusters.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Gruplanacak sanatçı bulunamadı.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(clusters, key = { it.genre.name }) { cluster ->
                        val confPct = (cluster.averageConfidence * 100).toInt()
                        Surface(
                            shape = RoundedCornerShape(SentupRadii.card),
                            color = MaterialTheme.colorScheme.surfaceContainer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cluster.genre.displayNameTr,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "${cluster.artists.size} sanatçı • %$confPct güven",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    FilledTonalButton(
                                        onClick = {
                                            onCreateGenreFolder(cluster.genre.displayNameTr, cluster.artists)
                                        },
                                        shape = RoundedCornerShape(SentupRadii.pill),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                        modifier = Modifier.height(34.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.CreateNewFolder,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = "Klasör Yap",
                                            style = MaterialTheme.typography.labelMedium
                                        )
                                    }
                                }
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    cluster.artists.forEach { artistName ->
                                        val art = artistPhotos[artistName]
                                        val pred = artistGenres[artistName]
                                        val artistPct = pred?.let { (it.confidence * 100).toInt() }
                                        Surface(
                                            onClick = {
                                                onDismiss()
                                                onOpenArtist(artistName)
                                            },
                                            shape = RoundedCornerShape(SentupRadii.pill),
                                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                if (!art.isNullOrBlank()) {
                                                    AsyncImage(
                                                        model = com.sentup.android.data.googleImageAtSize(art, 120),
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier
                                                            .size(22.dp)
                                                            .clip(CircleShape)
                                                    )
                                                }
                                                Text(
                                                    text = artistName,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    maxLines = 1
                                                )
                                                if (artistPct != null) {
                                                    Text(
                                                        text = "%$artistPct",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary
                                                    )
                                                }
                                                if (onInspectArtistGenre != null) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Edit,
                                                        contentDescription = "Türü düzenle",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier
                                                            .size(14.dp)
                                                            .clickable {
                                                                onInspectArtistGenre(artistName)
                                                            }
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
        }
    }
}

private enum class AutonomousFocusMode(val label: String) {
    Balanced("Dengeli"),
    Discovery("Keşif"),
    Favorites("Favoriler"),
    Energetic("Enerjik"),
    Chill("Sakin / Gece"),
    Offline("Çevrimdışı")
}

@Composable
fun AutonomousMixStudioHeader(
    poolSongs: List<com.sentup.android.data.Song>,
    folderArtists: List<String> = emptyList(),
    likedIds: Set<String>,
    downloadedIds: Set<String>,
    playCounts: Map<String, Int>,
    onOpenMixMaker: () -> Unit,
    onSearchArtists: suspend (String) -> List<com.sentup.android.data.ArtistItem> = { emptyList() },
    onGenerateOnlineMix: suspend (
        prompt: String,
        genreLabel: String?,
        focusLabel: String,
        artists: List<String>,
        targetCount: Int,
        perArtistCount: Int
    ) -> List<com.sentup.android.data.Song>,
    onPlayMix: (List<com.sentup.android.data.Song>) -> Unit,
    onShuffleMix: (List<com.sentup.android.data.Song>) -> Unit,
    onSaveAsPlaylist: (name: String, songs: List<com.sentup.android.data.Song>) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var targetMinutes by rememberSaveable { mutableStateOf(30) }
    var focusMode by rememberSaveable { mutableStateOf(AutonomousFocusMode.Balanced) }
    var selectedGenreName by rememberSaveable { mutableStateOf<String?>(null) }
    var perArtistCountText by rememberSaveable { mutableStateOf("10") }
    val perArtistCount = (perArtistCountText.toIntOrNull() ?: 10).coerceIn(1, 100)
    var manualArtists by rememberSaveable { mutableStateOf(listOf<String>()) }
    var artistQuery by rememberSaveable { mutableStateOf("") }
    var artistResults by remember { mutableStateOf<List<com.sentup.android.data.ArtistItem>>(emptyList()) }
    var isSearchingArtists by remember { mutableStateOf(false) }

    LaunchedEffect(artistQuery) {
        val trimmed = artistQuery.trim()
        if (trimmed.length < 2) {
            artistResults = emptyList()
            isSearchingArtists = false
            return@LaunchedEffect
        }
        isSearchingArtists = true
        kotlinx.coroutines.delay(350)
        artistResults = runCatching { onSearchArtists(trimmed) }.getOrDefault(emptyList())
        isSearchingArtists = false
    }
    var customPrompt by rememberSaveable { mutableStateOf("") }
    var pickedArtists by remember { mutableStateOf<Set<String>>(emptySet()) }
    var isGenerating by remember { mutableStateOf(false) }
    var generatedOnlineMix by remember { mutableStateOf<List<com.sentup.android.data.Song>>(emptyList()) }
    var generateError by remember { mutableStateOf<String?>(null) }
    var savedToast by remember { mutableStateOf(false) }

    val selectedGenre = remember(selectedGenreName) {
        selectedGenreName?.let { name ->
            com.sentup.android.ml.GenreClassifierMlEngine.MusicGenre.entries.firstOrNull { it.name == name }
        }
    }

    val localBuiltMix = remember(poolSongs, targetMinutes, focusMode, selectedGenre, likedIds, downloadedIds, playCounts) {
        val base = when (focusMode) {
            AutonomousFocusMode.Balanced -> poolSongs
            AutonomousFocusMode.Discovery -> poolSongs.sortedBy { playCounts[it.id] ?: 0 }
            AutonomousFocusMode.Favorites -> {
                val favs = poolSongs.filter { it.id in likedIds || (playCounts[it.id] ?: 0) > 1 }
                favs.ifEmpty { poolSongs }
            }
            AutonomousFocusMode.Energetic -> poolSongs.filter {
                val g = com.sentup.android.ml.GenreClassifierMlEngine.classifySong(it).genre
                g == com.sentup.android.ml.GenreClassifierMlEngine.MusicGenre.AnadoluRock ||
                    g == com.sentup.android.ml.GenreClassifierMlEngine.MusicGenre.RockMetal ||
                    g == com.sentup.android.ml.GenreClassifierMlEngine.MusicGenre.ElectronicDance ||
                    g == com.sentup.android.ml.GenreClassifierMlEngine.MusicGenre.RapHipHop
            }.ifEmpty { poolSongs }
            AutonomousFocusMode.Chill -> poolSongs.filter {
                val g = com.sentup.android.ml.GenreClassifierMlEngine.classifySong(it).genre
                g == com.sentup.android.ml.GenreClassifierMlEngine.MusicGenre.LofiChill ||
                    g == com.sentup.android.ml.GenreClassifierMlEngine.MusicGenre.AcousticIndie ||
                    g == com.sentup.android.ml.GenreClassifierMlEngine.MusicGenre.JazzBlues ||
                    g == com.sentup.android.ml.GenreClassifierMlEngine.MusicGenre.ClassicalInstrumental
            }.ifEmpty { poolSongs }
            AutonomousFocusMode.Offline -> poolSongs.filter { it.id in downloadedIds }
        }

        val genreFiltered = if (selectedGenre != null) {
            base.filter {
                com.sentup.android.ml.GenreClassifierMlEngine.classifySong(it).genre == selectedGenre
            }
        } else {
            base
        }

        val targetMs = targetMinutes * 60_000L
        val picked = mutableListOf<com.sentup.android.data.Song>()
        var accMs = 0L
        val candidates = if (focusMode == AutonomousFocusMode.Discovery) {
            genreFiltered
        } else {
            genreFiltered.shuffled()
        }
        for (song in candidates) {
            if (accMs >= targetMs && picked.size >= 5) break
            picked.add(song)
            accMs += song.duration.coerceAtLeast(180_000L)
            if (picked.size >= 60) break
        }
        picked
    }

    val builtMix = if (generatedOnlineMix.isNotEmpty()) generatedOnlineMix else localBuiltMix
    val totalMins = (builtMix.sumOf { it.duration.coerceAtLeast(180_000L) } / 60_000L).coerceAtLeast(0L)
    val desiredCount = (targetMinutes / 3).coerceIn(8, 50)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(SentupRadii.card),
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(28.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Otonom Karışım Stüdyosu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "İster YouTube Music'ten anlık üret, ister kitaplığından harmanla",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
                FilledTonalButton(
                    onClick = onOpenMixMaker,
                    shape = RoundedCornerShape(SentupRadii.pill),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("Sanatçı Seç")
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(SentupRadii.card),
            color = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = customPrompt,
                    onValueChange = {
                        customPrompt = it
                        generateError = null
                    },
                    placeholder = { Text("İstek / ruh hali / sanatçı yaz (ör. Gece Sürüşü, Anadolu Rock)") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (customPrompt.isNotEmpty()) {
                            IconButton(onClick = { customPrompt = "" }) {
                                Icon(Icons.Rounded.Close, contentDescription = null)
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(SentupRadii.pill),
                    modifier = Modifier.fillMaxWidth()
                )

                if (folderArtists.isNotEmpty()) {
                    Text(
                        text = "Klasörlerindeki Sanatçılar",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        folderArtists.take(15).forEach { artist ->
                            val isPicked = artist in pickedArtists
                            FilterChip(
                                selected = isPicked,
                                onClick = {
                                    pickedArtists = if (isPicked) pickedArtists - artist else pickedArtists + artist
                                    savedToast = false
                                    generateError = null
                                },
                                label = { Text(artist) }
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Manuel Sanatçı Ekle (YouTube Music)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    val extraManualArtists = manualArtists.filter { it !in folderArtists }
                    if (extraManualArtists.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            extraManualArtists.forEach { artist ->
                                val isPicked = artist in pickedArtists
                                FilterChip(
                                    selected = isPicked,
                                    onClick = {
                                        pickedArtists = if (isPicked) pickedArtists - artist else pickedArtists + artist
                                        savedToast = false
                                        generateError = null
                                    },
                                    label = { Text(artist) },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = "Kaldır",
                                            modifier = Modifier
                                                .size(16.dp)
                                                .clickable {
                                                    manualArtists = manualArtists - artist
                                                    pickedArtists = pickedArtists - artist
                                                    savedToast = false
                                                }
                                        )
                                    }
                                )
                            }
                        }
                    }

                    androidx.compose.material3.OutlinedTextField(
                        value = artistQuery,
                        onValueChange = { value -> artistQuery = value },
                        placeholder = { Text("Sanatçı ara (örn: Tarkan, Sezen Aksu)...") },
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        },
                        trailingIcon = {
                            if (isSearchingArtists) {
                                androidx.compose.material3.CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else if (artistQuery.isNotBlank()) {
                                IconButton(onClick = {
                                    artistQuery = ""
                                    artistResults = emptyList()
                                }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Temizle", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (artistResults.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .padding(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            artistResults.take(6).forEach { artistItem ->
                                val added = artistItem.name in pickedArtists
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable {
                                            if (added) {
                                                pickedArtists = pickedArtists - artistItem.name
                                            } else {
                                                if (artistItem.name !in manualArtists) {
                                                    manualArtists = manualArtists + artistItem.name
                                                }
                                                pickedArtists = pickedArtists + artistItem.name
                                            }
                                            savedToast = false
                                            generateError = null
                                        }
                                        .padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!artistItem.thumbnailUrl.isNullOrBlank()) {
                                            AsyncImage(
                                                model = com.sentup.android.data.googleImageAtSize(artistItem.thumbnailUrl, 120),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = artistItem.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (artistItem.isVerified) {
                                                Icon(
                                                    imageVector = Icons.Rounded.CheckCircle,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                            }
                                        }
                                        artistItem.subscriberCount?.takeIf { it.isNotBlank() }?.let { subs ->
                                            Text(
                                                text = subs,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(SentupRadii.pill),
                                        color = if (added) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
                                    ) {
                                        Text(
                                            text = if (added) "Seçili" else "+ Ekle",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (added) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Sanatçı Başına Şarkı (Maks. 100)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold
                        )
                        Surface(
                            shape = RoundedCornerShape(SentupRadii.pill),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$perArtistCount şarkı / sanatçı",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilledTonalIconButton(
                            onClick = {
                                val next = (perArtistCount - 5).coerceIn(1, 100)
                                perArtistCountText = next.toString()
                                savedToast = false
                            },
                            enabled = perArtistCount > 1
                        ) {
                            Icon(Icons.Rounded.Remove, contentDescription = "Azalt")
                        }
                        androidx.compose.material3.OutlinedTextField(
                            value = perArtistCountText,
                            onValueChange = { raw ->
                                val digits = raw.filter { it.isDigit() }
                                if (digits.isEmpty()) {
                                    perArtistCountText = ""
                                } else {
                                    val clamped = (digits.toIntOrNull() ?: 100).coerceIn(1, 100)
                                    perArtistCountText = clamped.toString()
                                }
                                savedToast = false
                            },
                            label = { Text("1 - 100") },
                            singleLine = true,
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.width(104.dp)
                        )
                        FilledTonalIconButton(
                            onClick = {
                                val next = (perArtistCount + 5).coerceIn(1, 100)
                                perArtistCountText = next.toString()
                                savedToast = false
                            },
                            enabled = perArtistCount < 100
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = "Artır")
                        }
                        Row(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(5, 10, 25, 50, 100).forEach { count ->
                                FilterChip(
                                    selected = perArtistCount == count,
                                    onClick = {
                                        perArtistCountText = count.toString()
                                        savedToast = false
                                    },
                                    label = { Text("$count") }
                                )
                            }
                        }
                    }
                }

                Text(
                    text = "Süre Hedefi",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15, 30, 60, 120).forEach { mins ->
                        FilterChip(
                            selected = targetMinutes == mins,
                            onClick = { targetMinutes = mins; savedToast = false },
                            label = { Text(if (mins >= 60) "${mins / 60} saat" else "$mins dk") }
                        )
                    }
                }

                Text(
                    text = "Odak Modu",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AutonomousFocusMode.entries.forEach { mode ->
                        FilterChip(
                            selected = focusMode == mode,
                            onClick = { focusMode = mode; savedToast = false },
                            label = { Text(mode.label) }
                        )
                    }
                }

                Text(
                    text = "Tür Filtresi (ML)",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedGenreName == null,
                        onClick = { selectedGenreName = null; savedToast = false },
                        label = { Text("Tümü") }
                    )
                    com.sentup.android.ml.GenreClassifierMlEngine.MusicGenre.entries.forEach { genre ->
                        FilterChip(
                            selected = selectedGenreName == genre.name,
                            onClick = {
                                selectedGenreName = if (selectedGenreName == genre.name) null else genre.name
                                savedToast = false
                            },
                            label = { Text(genre.displayNameTr) }
                        )
                    }
                }

                Button(
                    onClick = {
                        if (isGenerating) return@Button
                        isGenerating = true
                        generateError = null
                        savedToast = false
                        scope.launch {
                            val fetched = runCatching {
                                onGenerateOnlineMix(
                                    customPrompt.trim(),
                                    selectedGenre?.displayNameTr,
                                    focusMode.label,
                                    pickedArtists.toList(),
                                    desiredCount,
                                    perArtistCount
                                )
                            }.getOrDefault(emptyList())
                            val maxMerged = if (pickedArtists.isNotEmpty()) {
                                (pickedArtists.size * perArtistCount).coerceIn(desiredCount, 400)
                            } else {
                                100
                            }
                            val merged = (fetched + localBuiltMix).distinctBy { it.id }.take(maxMerged)
                            generatedOnlineMix = merged
                            if (merged.isEmpty()) {
                                generateError = "Şarkı bulunamadı — internet bağlantısını veya isteği kontrol edin."
                            }
                            isGenerating = false
                        }
                    },
                    enabled = !isGenerating,
                    shape = RoundedCornerShape(SentupRadii.pill),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isGenerating) {
                        androidx.compose.material3.CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Otonom Karışım Hazırlanıyor...")
                    } else {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Otonom Karışım Oluştur")
                    }
                }

                if (generateError != null) {
                    Text(
                        text = generateError!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = if (builtMix.isEmpty()) {
                            "Oluştur'a dokunun veya aşağıdan hazır karışım seçin"
                        } else {
                            "${builtMix.size} şarkı hazır • ~$totalMins dk"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (savedToast) {
                        Text(
                            text = "Çalma listesi kaydedildi",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                if (builtMix.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        builtMix.take(5).forEach { song ->
                            val cover = song.albumArtUri?.toString() ?: song.thumbnailUrl
                            Surface(
                                onClick = { onPlayMix(listOf(song) + builtMix.filterNot { it.id == song.id }) },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (!cover.isNullOrBlank()) {
                                            AsyncImage(
                                                model = com.sentup.android.data.googleImageAtSize(cover, 160),
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = song.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        if (builtMix.size > 5) {
                            Text(
                                text = "+${builtMix.size - 5} şarkı daha karışımın içinde",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { if (builtMix.isNotEmpty()) onPlayMix(builtMix) },
                        enabled = builtMix.isNotEmpty(),
                        shape = RoundedCornerShape(SentupRadii.pill),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Çal")
                    }
                    FilledTonalButton(
                        onClick = { if (builtMix.isNotEmpty()) onShuffleMix(builtMix) },
                        enabled = builtMix.isNotEmpty(),
                        shape = RoundedCornerShape(SentupRadii.pill),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Karıştır")
                    }
                    FilledTonalButton(
                        onClick = {
                            if (builtMix.isNotEmpty()) {
                                val title = buildString {
                                    if (customPrompt.isNotBlank()) {
                                        append(customPrompt.trim().take(28))
                                    } else {
                                        append("Otonom • ${focusMode.label}")
                                        if (selectedGenre != null) append(" (${selectedGenre.displayNameTr})")
                                    }
                                }
                                onSaveAsPlaylist(title, builtMix)
                                savedToast = true
                            }
                        },
                        enabled = builtMix.isNotEmpty(),
                        shape = RoundedCornerShape(SentupRadii.pill),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Kaydet")
                    }
                }
            }
        }
    }
}
