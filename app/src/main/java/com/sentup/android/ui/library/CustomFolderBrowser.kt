package com.sentup.android.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.DriveFileRenameOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoCamera
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
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.sentup.android.ml.GenreClassifierMlEngine
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
    artistPhotos: Map<String, String> = emptyMap(),
    artistGenres: Map<String, GenreClassifierMlEngine.GenrePrediction> = emptyMap(),
    isEnrichingGenres: Boolean = false,
    selectedGenreFilter: GenreClassifierMlEngine.MusicGenre? = null,
    onSelectGenreFilter: ((GenreClassifierMlEngine.MusicGenre?) -> Unit)? = null,
    onInspectArtistGenre: ((String) -> Unit)? = null,
    onSplitFolderByGenreRequest: ((MusicFolder) -> Unit)? = null,
    onNavigateFolder: (String?) -> Unit,
    onCreateFolderRequest: (parentId: String?) -> Unit,
    onRenameFolderRequest: (MusicFolder) -> Unit,
    onMoveFolderRequest: (MusicFolder) -> Unit,
    onDeleteFolder: (MusicFolder) -> Unit,
    onTogglePinFolder: (MusicFolder) -> Unit,
    onAddSongsRequest: (MusicFolder) -> Unit,
    onFolderAudioProfileRequest: ((MusicFolder) -> Unit)? = null,
    onFolderAutoRuleRequest: ((MusicFolder) -> Unit)? = null,
    onChangeFolderCoverRequest: ((MusicFolder) -> Unit)? = null,
    onPlayFolderWithProfile: ((MusicFolder, List<Song>, Boolean) -> Unit)? = null,
    onOpenTrackTools: ((Song) -> Unit)? = null,
    onExportImportFoldersRequest: (() -> Unit)? = null,
    onOpenGenreClusters: (() -> Unit)? = null,
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

    val folderProfile = currentFolder?.let {
        GenreClassifierMlEngine.analyzeFolderGenreProfile(
            folder = it,
            allSongs = allSongs,
            artistPredictions = artistGenres,
            playCounts = playCounts,
            downloadedIds = downloadedIds
        )
    }

    val rawFolderArtists = currentFolder?.artistNames.orEmpty()
        .filter { normalizedQuery.isEmpty() || it.lowercase(Locale.ROOT).contains(normalizedQuery) }
    val folderArtists = if (selectedGenreFilter != null) {
        rawFolderArtists.filter { artistName ->
            val artistSongs = allSongs.filter { it.artist.trim().equals(artistName.trim(), ignoreCase = true) }
            val pred = artistGenres[artistName]
                ?: GenreClassifierMlEngine.classifyArtist(artistName, artistSongs)
            pred.genre == selectedGenreFilter
        }
    } else {
        rawFolderArtists
    }

    val rawFolderAlbums = currentFolder?.albumNames.orEmpty()
        .filter { normalizedQuery.isEmpty() || it.lowercase(Locale.ROOT).contains(normalizedQuery) }
    val folderAlbums = if (selectedGenreFilter != null) {
        rawFolderAlbums.filter { albumName ->
            val matching = allSongs.filter { it.album.trim().equals(albumName.trim(), ignoreCase = true) }
            val genre = matching.firstOrNull()?.let {
                artistGenres[it.artist]?.genre ?: GenreClassifierMlEngine.classifySong(it).genre
            } ?: GenreClassifierMlEngine.classifyArtist(albumName, matching).genre
            genre == selectedGenreFilter
        }
    } else {
        rawFolderAlbums
    }

    val rawDirectSongs = if (currentFolder == null) {
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
    val directSongs = if (selectedGenreFilter != null) {
        rawDirectSongs.filter { song ->
            val sp = GenreClassifierMlEngine.classifySong(song)
            val effective = if (sp.confidence >= 0.55f) sp.genre
            else artistGenres[song.artist]?.genre ?: sp.genre
            effective == selectedGenreFilter
        }
    } else {
        rawDirectSongs
    }

    // Breadcrumb & hero / studio header
    item(key = "custom_folder_toolbar") {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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

            if (currentFolder == null) {
                FolderRootStudioBanner(
                    folderCount = folders.size,
                    totalArtists = folders.flatMap { it.artistNames }.distinct().size,
                    totalSongs = folders.flatMap { it.songIds }.distinct().size,
                    isEnrichingGenres = isEnrichingGenres,
                    onCreateFolder = { onCreateFolderRequest(null) },
                    onOpenGenreClusters = onOpenGenreClusters,
                    onExportImport = onExportImportFoldersRequest
                )
            } else {
                val allSubtreeSongs = CustomFolderTree.allDescendantSongs(
                    folders,
                    currentFolder.id,
                    allSongs,
                    playCounts,
                    downloadedIds
                )
                val customHeroCover = currentFolder.coverUri?.takeIf { it.isNotBlank() }
                val heroArts = if (customHeroCover != null) {
                    listOf(customHeroCover)
                } else {
                    buildList {
                        currentFolder.artistNames.forEach { a -> artistPhotos[a]?.let { add(it) } }
                        allSubtreeSongs.forEach { s -> (s.albumArtUri?.toString() ?: s.thumbnailUrl)?.let { add(it) } }
                    }.distinct().take(4)
                }

                OpenFolderHeroCard(
                    folder = currentFolder,
                    subfolderCount = childFolders.size,
                    artistCount = currentFolder.artistNames.size,
                    albumCount = currentFolder.albumNames.size,
                    songCount = rawDirectSongs.size,
                    allSubtreeSongs = allSubtreeSongs,
                    heroThumbnails = heroArts,
                    dominantProfile = folderProfile,
                    onPlayAll = {
                        if (allSubtreeSongs.isNotEmpty()) {
                            if (onPlayFolderWithProfile != null) {
                                onPlayFolderWithProfile(currentFolder, allSubtreeSongs, false)
                            } else {
                                onPlayQueue(allSubtreeSongs, allSubtreeSongs.firstOrNull())
                            }
                        }
                    },
                    onShuffleAll = {
                        if (allSubtreeSongs.isNotEmpty()) {
                            if (onPlayFolderWithProfile != null) {
                                onPlayFolderWithProfile(currentFolder, allSubtreeSongs, true)
                            } else {
                                onShuffleQueue(allSubtreeSongs)
                            }
                        }
                    },
                    onAddContent = { onAddSongsRequest(currentFolder) },
                    onCreateSubfolder = { onCreateFolderRequest(currentFolder.id) },
                    onPlayHarmonic = if (allSubtreeSongs.size > 1) {
                        {
                            val ordered = com.sentup.android.ml.AudioFeatureAndDspEngine.optimizeQueueTransitions(allSubtreeSongs)
                            onPlayQueue(ordered, ordered.firstOrNull())
                        }
                    } else null,
                    onPlayEnergyArc = if (allSubtreeSongs.size > 1) {
                        {
                            val ordered = com.sentup.android.ml.AudioFeatureAndDspEngine.sortFolderByEnergyArc(allSubtreeSongs)
                            onPlayQueue(ordered, ordered.firstOrNull())
                        }
                    } else null,
                    onTogglePin = { onTogglePinFolder(currentFolder) },
                    onRename = { onRenameFolderRequest(currentFolder) },
                    onMove = { onMoveFolderRequest(currentFolder) },
                    onAudioProfile = onFolderAudioProfileRequest?.let { cb -> { cb(currentFolder) } },
                    onAutoRule = onFolderAutoRuleRequest?.let { cb -> { cb(currentFolder) } },
                    onChangeCover = onChangeFolderCoverRequest?.let { cb -> { cb(currentFolder) } },
                    onSplitByGenre = if ((folderProfile?.breakdown?.size ?: 0) >= 2 && onSplitFolderByGenreRequest != null) {
                        { onSplitFolderByGenreRequest(currentFolder) }
                    } else null,
                    onDelete = { onDeleteFolder(currentFolder) }
                )

                if (folderProfile != null && (currentFolder.artistNames.isNotEmpty() || currentFolder.albumNames.isNotEmpty() || rawDirectSongs.isNotEmpty())) {
                    FolderMlProfileCard(
                        profile = folderProfile,
                        isEnrichingGenres = isEnrichingGenres,
                        selectedGenreFilter = selectedGenreFilter,
                        onSelectGenreFilter = onSelectGenreFilter,
                        onOpenGenreClusters = onOpenGenreClusters,
                        onSplitByGenre = if (folderProfile.breakdown.size >= 2 && onSplitFolderByGenreRequest != null) {
                            { onSplitFolderByGenreRequest(currentFolder) }
                        } else null
                    )
                }
            }
        }
    }

    val totalUnfilteredItems = childFolders.size + rawFolderArtists.size + rawFolderAlbums.size + rawDirectSongs.size
    if (totalUnfilteredItems == 0) {
        item(key = "custom_folder_empty") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SentupGroup.inset, vertical = 36.dp),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (currentFolder != null) {
                        Button(
                            onClick = { onAddSongsRequest(currentFolder) },
                            shape = RoundedCornerShape(SentupRadii.pill)
                        ) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(stringResource(R.string.lib_custom_folder_add_content))
                        }
                    }
                    FilledTonalButton(
                        onClick = { onCreateFolderRequest(currentFolder?.id) },
                        shape = RoundedCornerShape(SentupRadii.pill)
                    ) {
                        Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.lib_custom_folder_new))
                    }
                }
            }
        }
        return
    }

    if (selectedGenreFilter != null && folderArtists.isEmpty() && folderAlbums.isEmpty() && directSongs.isEmpty() && childFolders.isEmpty()) {
        item(key = "custom_folder_genre_filter_empty") {
            Surface(
                shape = RoundedCornerShape(SentupRadii.card),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SentupGroup.inset, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${selectedGenreFilter.displayNameTr} türünde öğe bulunamadı.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    FilledTonalButton(
                        onClick = { onSelectGenreFilter?.invoke(null) },
                        shape = RoundedCornerShape(SentupRadii.pill)
                    ) {
                        Text("Filtreyi Temizle")
                    }
                }
            }
        }
    }

    if (childFolders.isNotEmpty()) {
        if (currentFolder != null) {
            item(key = "header_subfolders_${currentFolder.id}") {
                FolderBrowserSectionTitle("Alt Klasörler (${childFolders.size})")
            }
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
            val customCardCover = folder.coverUri?.takeIf { it.isNotBlank() }
            val cardThumbs = if (customCardCover != null) {
                listOf(customCardCover)
            } else {
                buildList {
                    folder.artistNames.forEach { artistName -> artistPhotos[artistName]?.let { add(it) } }
                    descendantSongs.forEach { s -> (s.albumArtUri?.toString() ?: s.thumbnailUrl)?.let { add(it) } }
                }.distinct().take(4)
            }
            val folderGenreProfile = GenreClassifierMlEngine.analyzeFolderGenreProfile(
                folder = folder,
                allSongs = allSongs,
                artistPredictions = artistGenres,
                playCounts = playCounts,
                downloadedIds = downloadedIds
            )
            CustomMusicFolderCard(
                folder = folder,
                subfolderCount = subfolderCount,
                songCount = descendantSongs.size,
                totalDurationMs = descendantSongs.sumOf { it.duration.coerceAtLeast(0L) },
                collageThumbnails = cardThumbs,
                dominantGenre = folderGenreProfile.dominantGenre,
                dominantConfidence = folderGenreProfile.dominantConfidence,
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
                onChangeCover = onChangeFolderCoverRequest?.let { cb -> { cb(folder) } },
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
    }

    if (currentFolder != null && folderArtists.isNotEmpty()) {
        item(key = "header_artists_${currentFolder.id}") {
            FolderBrowserSectionTitle("Sanatçılar (${folderArtists.size})")
        }
        items(
            folderArtists.chunked(2),
            key = { row -> "custom_artist_row_${currentFolder.id}_" + row.joinToString("|") }
        ) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SentupGroup.inset, vertical = 5.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { artistName ->
                    val artistSongs = allSongs.filter { it.artist.trim().equals(artistName.trim(), ignoreCase = true) }
                    val artistArt = artistPhotos[artistName]
                        ?: artistSongs.firstNotNullOfOrNull { it.albumArtUri?.toString() ?: it.thumbnailUrl }
                    val genrePred = artistGenres[artistName]
                        ?: GenreClassifierMlEngine.classifyArtist(
                            artistName = artistName,
                            songs = artistSongs
                        )
                    Box(modifier = Modifier.weight(1f)) {
                        CustomFolderArtistCard(
                            name = artistName,
                            genreLabel = "${genrePred.genre.displayNameTr} · %${(genrePred.confidence * 100).toInt()}",
                            isVerifiedGenre = genrePred.isVerifiedTag,
                            songCount = artistSongs.size,
                            thumbnailUrl = artistArt,
                            onClick = { onOpenArtist(artistName) },
                            onInspectGenre = onInspectArtistGenre?.let { cb -> { cb(artistName) } },
                            onPlay = {
                                if (artistSongs.isNotEmpty()) {
                                    onPlayQueue(artistSongs, artistSongs.firstOrNull())
                                } else {
                                    onOpenArtist(artistName)
                                }
                            },
                            onRemove = { onRemoveArtistFromFolder(currentFolder.id, artistName) }
                        )
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }

    if (currentFolder != null && folderAlbums.isNotEmpty()) {
        item(key = "header_albums_${currentFolder.id}") {
            FolderBrowserSectionTitle("Albümler (${folderAlbums.size})")
        }
        items(folderAlbums, key = { "custom_album_${currentFolder.id}_$it" }) { albumName ->
            val albumSongs = allSongs.filter { it.album.trim().equals(albumName.trim(), ignoreCase = true) }
                .sortedInAlbumOrder()
            val albumArt = albumSongs.firstNotNullOfOrNull { it.albumArtUri?.toString() ?: it.thumbnailUrl }
            val subtitleText = if (albumSongs.isEmpty()) {
                "YouTube Albümü"
            } else {
                val artistLabel = albumSongs.firstOrNull()?.artist?.takeIf { it.isNotBlank() }
                val countText = pluralStringResource(R.plurals.n_songs, albumSongs.size, albumSongs.size)
                if (artistLabel != null) "$artistLabel · $countText" else countText
            }
            CustomFolderLinkedItemRow(
                title = albumName,
                subtitle = subtitleText,
                icon = Icons.Rounded.Album,
                thumbnailUrl = albumArt,
                roundArt = false,
                onClick = { onOpenAlbum(albumName, albumSongs) },
                onPlay = {
                    if (albumSongs.isNotEmpty()) onPlayQueue(albumSongs, albumSongs.firstOrNull())
                    else onOpenAlbum(albumName, albumSongs)
                },
                onRemove = { onRemoveAlbumFromFolder(currentFolder.id, albumName) }
            )
        }
    }

    if (currentFolder != null && directSongs.isNotEmpty()) {
        item(key = "header_songs_${currentFolder.id}") {
            FolderBrowserSectionTitle("Şarkılar (${directSongs.size})")
        }
        items(directSongs, key = { "custom_song_${currentFolder.id}_${it.id}" }) { song ->
            val songGenre = remember(song.id, artistGenres[song.artist]) {
                val sp = GenreClassifierMlEngine.classifySong(song)
                if (sp.confidence >= 0.55f) sp.genre
                else artistGenres[song.artist]?.genre ?: sp.genre
            }
            CustomFolderSongRow(
                song = song,
                genreLabel = songGenre.displayNameTr,
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
private fun FolderBrowserSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset, vertical = 6.dp)
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderRootStudioBanner(
    folderCount: Int,
    totalArtists: Int,
    totalSongs: Int,
    isEnrichingGenres: Boolean,
    onCreateFolder: () -> Unit,
    onOpenGenreClusters: (() -> Unit)?,
    onExportImport: (() -> Unit)?
) {
    Surface(
        shape = RoundedCornerShape(SentupRadii.card),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Klasör & Tür Stüdyosu",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    val parts = buildList {
                        add("$folderCount klasör")
                        if (totalArtists > 0) add("$totalArtists sanatçı")
                        if (totalSongs > 0) add("$totalSongs şarkı")
                        if (isEnrichingGenres) add("ML tür analizi sürüyor...")
                    }
                    Text(
                        text = parts.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onCreateFolder,
                    shape = RoundedCornerShape(SentupRadii.pill),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.lib_custom_folder_new))
                }
                if (onOpenGenreClusters != null) {
                    FilledTonalButton(
                        onClick = onOpenGenreClusters,
                        shape = RoundedCornerShape(SentupRadii.pill),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tür Ortaklığı (ML)")
                    }
                }
                if (onExportImport != null) {
                    OutlinedButton(
                        onClick = onExportImport,
                        shape = RoundedCornerShape(SentupRadii.pill),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.lib_folder_export_import))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OpenFolderHeroCard(
    folder: MusicFolder,
    subfolderCount: Int,
    artistCount: Int,
    albumCount: Int,
    songCount: Int,
    allSubtreeSongs: List<Song>,
    heroThumbnails: List<String>,
    dominantProfile: GenreClassifierMlEngine.FolderGenreProfile?,
    onPlayAll: () -> Unit,
    onShuffleAll: () -> Unit,
    onAddContent: () -> Unit,
    onCreateSubfolder: () -> Unit,
    onPlayHarmonic: (() -> Unit)?,
    onPlayEnergyArc: (() -> Unit)?,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onAudioProfile: (() -> Unit)?,
    onAutoRule: (() -> Unit)?,
    onChangeCover: (() -> Unit)? = null,
    onSplitByGenre: (() -> Unit)?,
    onDelete: () -> Unit
) {
    val folderAccent = folder.colorHex?.let { Color(it) } ?: MaterialTheme.colorScheme.primary
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(SentupRadii.card),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, folderAccent.copy(alpha = 0.25f), RoundedCornerShape(SentupRadii.card))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            folderAccent.copy(alpha = 0.16f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .then(if (onChangeCover != null) Modifier.clickable(onClick = onChangeCover) else Modifier)
                ) {
                    PlaylistFolderArtwork(
                        thumbnails = heroThumbnails,
                        iconSize = 32,
                        modifier = Modifier.matchParentSize()
                    )
                    if (onChangeCover != null) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(4.dp)
                                .size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Rounded.PhotoCamera,
                                    contentDescription = "Kapak Değiştir",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = folder.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                    val totalDurationMs = allSubtreeSongs.sumOf { it.duration.coerceAtLeast(0L) }
                    val statParts = buildList {
                        if (subfolderCount > 0) add("$subfolderCount alt klasör")
                        if (artistCount > 0) add("$artistCount sanatçı")
                        if (albumCount > 0) add("$albumCount albüm")
                        if (songCount > 0) add("$songCount şarkı")
                        if (totalDurationMs > 0L) add(formatCompactDuration(totalDurationMs))
                    }
                    Text(
                        text = if (statParts.isEmpty()) "İçerik eklemeye hazır" else statParts.joinToString(" • "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (dominantProfile?.dominantGenre != null || folder.playbackSpeed != null || folder.eqPreset != null || folder.autoRule != null) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            dominantProfile?.dominantGenre?.let { dom ->
                                val pct = (dominantProfile.dominantConfidence * 100).toInt()
                                Surface(
                                    shape = RoundedCornerShape(SentupRadii.pill),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = "${dom.displayNameTr} · %$pct",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (folder.playbackSpeed != null || folder.eqPreset != null) {
                                Surface(
                                    shape = RoundedCornerShape(SentupRadii.pill),
                                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f)
                                ) {
                                    val profileBits = listOfNotNull(
                                        folder.playbackSpeed?.let { "${it}x" },
                                        folder.eqPreset?.name
                                    ).joinToString(" · ")
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            Icons.Rounded.GraphicEq,
                                            contentDescription = null,
                                            modifier = Modifier.size(12.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            text = profileBits,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                            if (folder.autoRule != null) {
                                Surface(
                                    shape = RoundedCornerShape(SentupRadii.pill),
                                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f)
                                ) {
                                    Text(
                                        text = "Akıllı Kural",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = onTogglePin, modifier = Modifier.size(34.dp)) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = null,
                            tint = if (folder.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.MoreVert, contentDescription = null)
                        }
                        DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.lib_custom_folder_rename)) },
                                leadingIcon = { Icon(Icons.Rounded.DriveFileRenameOutline, contentDescription = null) },
                                onClick = { menuExpanded = false; onRename() }
                            )
                            if (onAudioProfile != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.lib_folder_audio_profile)) },
                                    leadingIcon = { Icon(Icons.Rounded.Tune, contentDescription = null) },
                                    onClick = { menuExpanded = false; onAudioProfile() }
                                )
                            }
                            if (onAutoRule != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.lib_folder_auto_rule)) },
                                    leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                                    onClick = { menuExpanded = false; onAutoRule() }
                                )
                            }
                            if (onChangeCover != null) {
                                DropdownMenuItem(
                                    text = { Text("Kapak Fotoğrafı Seç / Değiştir") },
                                    leadingIcon = { Icon(Icons.Rounded.PhotoCamera, contentDescription = null) },
                                    onClick = { menuExpanded = false; onChangeCover() }
                                )
                            }
                            if (onSplitByGenre != null) {
                                DropdownMenuItem(
                                    text = { Text("ML ile Türlere Böl") },
                                    leadingIcon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = null) },
                                    onClick = { menuExpanded = false; onSplitByGenre() }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.lib_custom_folder_move)) },
                                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.DriveFileMove, contentDescription = null) },
                                onClick = { menuExpanded = false; onMove() }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.lib_custom_folder_delete)) },
                                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                                onClick = { menuExpanded = false; onDelete() }
                            )
                        }
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (allSubtreeSongs.isNotEmpty()) {
                    Button(
                        onClick = onPlayAll,
                        shape = RoundedCornerShape(SentupRadii.pill),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Tümünü Çal")
                    }
                    FilledTonalButton(
                        onClick = onShuffleAll,
                        shape = RoundedCornerShape(SentupRadii.pill),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Karıştır")
                    }
                }
                FilledTonalButton(
                    onClick = onAddContent,
                    shape = RoundedCornerShape(SentupRadii.pill),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.lib_custom_folder_add_content))
                }
                FilledTonalButton(
                    onClick = onCreateSubfolder,
                    shape = RoundedCornerShape(SentupRadii.pill),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Alt Klasör")
                }
                if (onPlayHarmonic != null) {
                    OutlinedButton(
                        onClick = onPlayHarmonic,
                        shape = RoundedCornerShape(SentupRadii.pill),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.lib_folder_sort_harmonic))
                    }
                }
                if (onPlayEnergyArc != null) {
                    OutlinedButton(
                        onClick = onPlayEnergyArc,
                        shape = RoundedCornerShape(SentupRadii.pill),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(stringResource(R.string.lib_folder_sort_arc))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FolderMlProfileCard(
    profile: GenreClassifierMlEngine.FolderGenreProfile,
    isEnrichingGenres: Boolean,
    selectedGenreFilter: GenreClassifierMlEngine.MusicGenre?,
    onSelectGenreFilter: ((GenreClassifierMlEngine.MusicGenre?) -> Unit)?,
    onOpenGenreClusters: (() -> Unit)?,
    onSplitByGenre: (() -> Unit)?
) {
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
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Klasör Tür Profili (ML)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = profile.vibeSummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (isEnrichingGenres) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            if (profile.breakdown.isNotEmpty()) {
                val segmentPalette = listOf(
                    MaterialTheme.colorScheme.primary,
                    MaterialTheme.colorScheme.secondary,
                    MaterialTheme.colorScheme.tertiary,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    profile.breakdown.take(5).forEachIndexed { idx, item ->
                        Box(
                            modifier = Modifier
                                .weight(item.percentage.coerceAtLeast(6).toFloat())
                                .fillMaxHeight()
                                .background(segmentPalette[idx % segmentPalette.size])
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onSelectGenreFilter != null) {
                        FilterChip(
                            selected = selectedGenreFilter == null,
                            onClick = { onSelectGenreFilter(null) },
                            label = { Text("Tümü") }
                        )
                    }
                    profile.breakdown.forEach { item ->
                        val selected = selectedGenreFilter == item.genre
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onSelectGenreFilter?.invoke(if (selected) null else item.genre)
                            },
                            label = { Text("${item.genre.displayNameTr} %${item.percentage}") }
                        )
                    }
                }
            }

            if (onOpenGenreClusters != null || onSplitByGenre != null) {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (onOpenGenreClusters != null) {
                        FilledTonalButton(
                            onClick = onOpenGenreClusters,
                            shape = RoundedCornerShape(SentupRadii.pill),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tür Ortaklığı & Kümele", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    if (onSplitByGenre != null) {
                        OutlinedButton(
                            onClick = onSplitByGenre,
                            shape = RoundedCornerShape(SentupRadii.pill),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("ML ile Türlere Böl", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ArtistGenreInspectorDialog(
    artistName: String,
    prediction: GenreClassifierMlEngine.GenrePrediction,
    artistPhotoUrl: String?,
    onSelectGenre: (GenreClassifierMlEngine.MusicGenre) -> Unit,
    onResetAuto: () -> Unit,
    onOpenArtist: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (!artistPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = googleImageAtSize(artistPhotoUrl, PICKER_ART_PX),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(Icons.Rounded.Person, contentDescription = null)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = artistName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${prediction.genre.displayNameTr} • %${(prediction.confidence * 100).toInt()} güven",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (prediction.matchedSignals.isNotEmpty()) {
                    Text(
                        text = "ML Sinyalleri",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        prediction.matchedSignals.forEach { signal ->
                            Surface(
                                shape = RoundedCornerShape(SentupRadii.pill),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = signal,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "Yanlışsa doğru türü seç (ML kalıcı olarak öğrenir):",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    GenreClassifierMlEngine.MusicGenre.entries.forEach { genre ->
                        FilterChip(
                            selected = prediction.genre == genre,
                            onClick = { onSelectGenre(genre) },
                            label = { Text(genre.displayNameTr, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDismiss()
                onOpenArtist()
            }) {
                Text("Sanatçıyı Aç")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (prediction.matchedSignals.any { it.contains("Kullanıcı doğrulaması") }) {
                    TextButton(onClick = onResetAuto) {
                        Text("Otomatik ML")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    )
}

@Composable
fun SplitFolderByGenreDialog(
    folder: MusicFolder,
    splits: List<GenreClassifierMlEngine.GenreSubfolderSplit>,
    onConfirmSplit: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("ML ile Tür Alt Klasörlerine Böl") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "\"${folder.name}\" içindeki içerikler otomatik ${splits.size} alt klasöre ayrılacak:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                splits.forEach { split ->
                    val stats = buildList {
                        if (split.artists.isNotEmpty()) add("${split.artists.size} sanatçı")
                        if (split.albums.isNotEmpty()) add("${split.albums.size} albüm")
                        if (split.songIds.isNotEmpty()) add("${split.songIds.size} şarkı")
                    }.joinToString(" · ")
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = split.folderName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stats,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirmSplit) {
                Text("Alt Klasörleri Oluştur")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun CustomFolderArtistCard(
    name: String,
    genreLabel: String,
    isVerifiedGenre: Boolean,
    songCount: Int,
    thumbnailUrl: String?,
    onClick: () -> Unit,
    onInspectGenre: (() -> Unit)?,
    onPlay: () -> Unit,
    onRemove: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(SentupRadii.card),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(82.dp)
                        .align(Alignment.Center)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer)
                        .then(
                            if (!thumbnailUrl.isNullOrBlank()) {
                                Modifier.border(
                                    1.5.dp,
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                                    CircleShape
                                )
                            } else Modifier
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (!thumbnailUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = googleImageAtSize(thumbnailUrl, PICKER_ART_PX),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.matchParentSize()
                        )
                    } else {
                        Icon(
                            Icons.Rounded.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                IconButton(
                    onClick = onRemove,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.lib_custom_folder_remove_song),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Text(
                text = name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                shape = RoundedCornerShape(SentupRadii.pill),
                modifier = if (onInspectGenre != null) Modifier.clip(RoundedCornerShape(SentupRadii.pill)).clickable(onClick = onInspectGenre) else Modifier
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isVerifiedGenre) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Text(
                        text = genreLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (onInspectGenre != null) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Türü incele",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.75f),
                            modifier = Modifier.size(10.dp)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (songCount > 0) {
                        pluralStringResource(R.plurals.n_songs, songCount, songCount)
                    } else {
                        "YouTube · Profil"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                FilledTonalIconButton(
                    onClick = onPlay,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = if (songCount > 0) Icons.Rounded.PlayArrow else Icons.Rounded.ChevronRight,
                        contentDescription = stringResource(R.string.lib_folder_play_all),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomFolderLinkedItemRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    thumbnailUrl: String? = null,
    roundArt: Boolean = false,
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
            val artShape = if (roundArt) CircleShape else RoundedCornerShape(10.dp)
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(artShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                if (!thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = googleImageAtSize(thumbnailUrl, PICKER_ART_PX),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer)
                }
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
    collageThumbnails: List<String> = emptyList(),
    dominantGenre: GenreClassifierMlEngine.MusicGenre? = null,
    dominantConfidence: Float = 0f,
    onOpen: () -> Unit,
    onPlayAll: () -> Unit,
    onShuffle: () -> Unit,
    onTogglePin: () -> Unit,
    onRename: () -> Unit,
    onMove: () -> Unit,
    onAudioProfile: (() -> Unit)? = null,
    onAutoRule: (() -> Unit)? = null,
    onChangeCover: (() -> Unit)? = null,
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
            if (collageThumbnails.isNotEmpty()) {
                PlaylistFolderArtwork(
                    thumbnails = collageThumbnails,
                    iconSize = 24,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, folderColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(folderColor.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = folderColor,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                val metaParts = buildList {
                    if (folder.artistNames.isNotEmpty()) add("${folder.artistNames.size} sanatçı")
                    if (subfolderCount > 0) add("$subfolderCount alt klasör")
                    if (songCount > 0) add("$songCount şarkı")
                    if (totalDurationMs > 0L) add(formatCompactDuration(totalDurationMs))
                }
                Text(
                    text = if (metaParts.isEmpty()) stringResource(R.string.lib_custom_folder_meta, subfolderCount, songCount)
                    else metaParts.joinToString(" • "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (dominantGenre != null && dominantConfidence > 0f) {
                    Surface(
                        shape = RoundedCornerShape(SentupRadii.pill),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = "${dominantGenre.displayNameTr} · %${(dominantConfidence * 100).toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                        )
                    }
                }
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
                    if (onChangeCover != null) {
                        DropdownMenuItem(
                            text = { Text("Kapak Fotoğrafı Seç / Değiştir") },
                            leadingIcon = { Icon(Icons.Rounded.PhotoCamera, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onChangeCover()
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
    genreLabel: String? = null,
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
            val songArt = song.albumArtUri?.toString() ?: song.thumbnailUrl
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                if (!songArt.isNullOrBlank()) {
                    AsyncImage(
                        model = googleImageAtSize(songArt, PICKER_ART_PX),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.matchParentSize()
                    )
                } else {
                    Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${song.artist} · ${formatTrackDuration(song.duration)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (!genreLabel.isNullOrBlank()) {
                        Surface(
                            shape = RoundedCornerShape(SentupRadii.pill),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
                        ) {
                            Text(
                                text = genreLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                    if (isLiked) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                    if (isDownloaded) {
                        Icon(
                            imageVector = Icons.Rounded.DownloadDone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }
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
    onSeedArtistPhotos: ((Map<String, String>) -> Unit)? = null,
    onConfirm: (artists: List<String>, albums: List<String>, songIds: List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableStateOf(FolderContentPickerTab.Artists) }
    var query by remember { mutableStateOf("") }
    var selectedArtists by remember { mutableStateOf(targetFolder.artistNames.toSet()) }
    var selectedAlbums by remember { mutableStateOf(targetFolder.albumNames.toSet()) }
    var selectedSongIds by remember { mutableStateOf(targetFolder.songIds.toSet()) }
    var seededArtistPhotos by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
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
                        if (seededArtistPhotos.isNotEmpty()) {
                            onSeedArtistPhotos?.invoke(seededArtistPhotos)
                        }
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
                                            artist.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { artUrl ->
                                                seededArtistPhotos = seededArtistPhotos + (artistName to artUrl)
                                            }
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
