package com.sentup.android.ui.home

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sentup.android.data.ArtistItem
import com.sentup.android.data.CustomFolderTree
import com.sentup.android.data.MusicFolder
import com.sentup.android.data.PlaylistDisplayItem
import com.sentup.android.data.PlaylistFolder
import com.sentup.android.data.PlaylistFolderLogic
import com.sentup.android.data.SmartMixRules
import com.sentup.android.data.Song
import com.sentup.android.data.SongSource
import com.sentup.android.data.VideoPlaylist
import com.sentup.android.data.googleImageAtSize
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

data class HomePinnedArtist(
    val name: String,
    val thumbnailUrl: String? = null
)

data class HomePinsSnapshot(
    val customFolderIds: List<String> = emptyList(),
    val playlistFolderIds: List<String> = emptyList(),
    val watchlistIds: List<String> = emptyList(),
    val playlistIds: List<String> = emptyList(),
    val artists: List<HomePinnedArtist> = emptyList()
)

/**
 * Shared preference-backed store for YouTube Music Home pins:
 * supports pinning Custom Folders, Playlist Folders, Music Playlists,
 * Video Watchlists / Watch Later, and an UNLIMITED list of Artists.
 */
class HomePinStore(context: Context) {
    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        synchronized(LOCK) {
            if (sharedState == null) {
                sharedState = MutableStateFlow(readSnapshot())
            }
        }
    }

    val snapshot: StateFlow<HomePinsSnapshot> get() = sharedState!!.asStateFlow()

    private fun readStringList(key: String): List<String> {
        val raw = prefs.getString(key, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    arr.optString(i).takeIf { it.isNotBlank() }?.let(::add)
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeStringList(key: String, values: List<String>) {
        val arr = JSONArray().apply { values.distinct().forEach(::put) }
        prefs.edit().putString(key, arr.toString()).apply()
    }

    private fun readArtists(): List<HomePinnedArtist> {
        val raw = prefs.getString(KEY_ARTISTS, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return runCatching {
            val arr = JSONArray(raw)
            buildList(arr.length()) {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val name = obj.optString("name").trim()
                    if (name.isNotEmpty()) {
                        add(
                            HomePinnedArtist(
                                name = name,
                                thumbnailUrl = obj.optString("thumb").takeIf { it.isNotBlank() }
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun writeArtists(list: List<HomePinnedArtist>) {
        val arr = JSONArray().apply {
            list.distinctBy { it.name.lowercase() }.forEach { artist ->
                put(
                    JSONObject()
                        .put("name", artist.name)
                        .put("thumb", artist.thumbnailUrl.orEmpty())
                )
            }
        }
        prefs.edit().putString(KEY_ARTISTS, arr.toString()).apply()
    }

    private fun readSnapshot(): HomePinsSnapshot = HomePinsSnapshot(
        customFolderIds = readStringList(KEY_CUSTOM_FOLDERS),
        playlistFolderIds = readStringList(KEY_PLAYLIST_FOLDERS),
        watchlistIds = readStringList(KEY_WATCHLISTS),
        playlistIds = runCatching { prefs.getString(KEY_PLAYLISTS, "") }
            .getOrNull()
            .orEmpty()
            .split(",")
            .filter { it.isNotBlank() },
        artists = readArtists()
    )

    private fun publish() {
        sharedState?.value = readSnapshot()
    }

    fun toggleCustomFolder(folderId: String): Boolean {
        val cur = readStringList(KEY_CUSTOM_FOLDERS)
        val pinned = folderId !in cur
        val next = if (pinned) listOf(folderId) + cur else cur.filterNot { it == folderId }
        writeStringList(KEY_CUSTOM_FOLDERS, next)
        publish()
        return pinned
    }

    fun togglePlaylistFolder(folderId: String): Boolean {
        val cur = readStringList(KEY_PLAYLIST_FOLDERS)
        val pinned = folderId !in cur
        val next = if (pinned) listOf(folderId) + cur else cur.filterNot { it == folderId }
        writeStringList(KEY_PLAYLIST_FOLDERS, next)
        publish()
        return pinned
    }

    fun toggleWatchlist(playlistId: String): Boolean {
        val cur = readStringList(KEY_WATCHLISTS)
        val pinned = playlistId !in cur
        val next = if (pinned) listOf(playlistId) + cur else cur.filterNot { it == playlistId }
        writeStringList(KEY_WATCHLISTS, next)
        publish()
        return pinned
    }

    fun togglePlaylist(playlistId: String): Boolean {
        val cur = runCatching { prefs.getString(KEY_PLAYLISTS, "") }
            .getOrNull()
            .orEmpty()
            .split(",")
            .filter { it.isNotBlank() }
        val pinned = playlistId !in cur
        val next = if (pinned) cur + playlistId else cur - playlistId
        prefs.edit().putString(KEY_PLAYLISTS, next.joinToString(",")).apply()
        publish()
        return pinned
    }

    /** Pins or unpins an artist with NO count limit. */
    fun toggleArtist(name: String, thumbnailUrl: String? = null): Boolean {
        val normalized = name.trim()
        if (normalized.isEmpty()) return false
        val cur = readArtists()
        val exists = cur.any { it.name.equals(normalized, ignoreCase = true) }
        val next = if (exists) {
            cur.filterNot { it.name.equals(normalized, ignoreCase = true) }
        } else {
            listOf(HomePinnedArtist(normalized, thumbnailUrl)) + cur
        }
        writeArtists(next)
        publish()
        return !exists
    }

    fun updateArtistThumb(name: String, thumbnailUrl: String?) {
        if (thumbnailUrl.isNullOrBlank()) return
        val cur = readArtists()
        var changed = false
        val next = cur.map { item ->
            if (item.name.equals(name, ignoreCase = true) && item.thumbnailUrl != thumbnailUrl) {
                changed = true
                item.copy(thumbnailUrl = thumbnailUrl)
            } else {
                item
            }
        }
        if (changed) {
            writeArtists(next)
            publish()
        }
    }

    companion object {
        private const val PREFS_NAME = "sentup_home_pins"
        private const val KEY_PLAYLISTS = "pinned"
        private const val KEY_CUSTOM_FOLDERS = "pinned_custom_folders"
        private const val KEY_PLAYLIST_FOLDERS = "pinned_playlist_folders"
        private const val KEY_WATCHLISTS = "pinned_watchlists"
        private const val KEY_ARTISTS = "pinned_artists_unlimited"
        private val LOCK = Any()
        @Volatile private var sharedState: MutableStateFlow<HomePinsSnapshot>? = null
    }
}

enum class HomeSonicFilter(val label: String) {
    All("Tümü"),
    Offline("Çevrimdışı Hazır"),
    Turkish("Türkçe / Yerel"),
    Energetic("Enerjik Hit"),
    Calm("Sakin / Akustik")
}

private val CALM_KEYWORDS = listOf("acoustic", "akustik", "chill", "lofi", "lo-fi", "piano", "piyano", "calm", "sleep", "slow", "ballad")
private val ENERGY_KEYWORDS = listOf("remix", "mix", "phonk", "rock", "metal", "rap", "trap", "dance", "club", "live", "enerji", "workout", "gym")
private val TURKISH_CHARS = setOf('ç', 'ğ', 'ı', 'ö', 'ş', 'ü', 'Ç', 'Ğ', 'İ', 'Ö', 'Ş', 'Ü')

internal fun filterSongsForHomeLens(
    songs: List<Song>,
    filter: HomeSonicFilter,
    offlineIds: Set<String>
): List<Song> = when (filter) {
    HomeSonicFilter.All -> songs
    HomeSonicFilter.Offline -> songs.filter { it.id in offlineIds || it.source == SongSource.LOCAL }
    HomeSonicFilter.Turkish -> songs.filter { s ->
        val text = "${s.title} ${s.artist} ${s.album}"
        text.any { it in TURKISH_CHARS } || s.source == SongSource.LOCAL
    }
    HomeSonicFilter.Energetic -> songs.filter { s ->
        val low = "${s.title} ${s.artist}".lowercase()
        ENERGY_KEYWORDS.any { it in low } || s.duration in 120_000L..250_000L
    }
    HomeSonicFilter.Calm -> songs.filter { s ->
        val low = "${s.title} ${s.artist}".lowercase()
        CALM_KEYWORDS.any { it in low }
    }
}.ifEmpty { songs.take(10) }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomePinHubSection(
    pins: HomePinsSnapshot,
    customFolders: List<MusicFolder>,
    playlistFolders: List<PlaylistFolder>,
    musicPlaylists: List<PlaylistDisplayItem>,
    videoWatchlists: List<VideoPlaylist>,
    allSongs: List<Song>,
    artistPhotos: Map<String, String>,
    onOpenPinManager: () -> Unit,
    onOpenCustomFolder: (MusicFolder) -> Unit,
    onPlayCustomFolder: (MusicFolder) -> Unit,
    onPlayPlaylistFolder: (PlaylistFolder) -> Unit,
    onOpenMusicPlaylist: (PlaylistDisplayItem) -> Unit,
    onOpenWatchlist: (VideoPlaylist) -> Unit,
    onOpenArtist: (String) -> Unit,
    onArtistRadio: (String) -> Unit,
    onUnpinCustomFolder: (String) -> Unit,
    onUnpinPlaylistFolder: (String) -> Unit,
    onUnpinWatchlist: (String) -> Unit,
    onUnpinMusicPlaylist: (String) -> Unit,
    onUnpinArtist: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val pinnedCustom = remember(pins.customFolderIds, customFolders) {
        pins.customFolderIds.mapNotNull { id -> customFolders.firstOrNull { it.id == id } }
    }
    val pinnedPFolders = remember(pins.playlistFolderIds, playlistFolders) {
        pins.playlistFolderIds.mapNotNull { id -> playlistFolders.firstOrNull { it.id == id } }
    }
    val pinnedWatch = remember(pins.watchlistIds, videoWatchlists) {
        pins.watchlistIds.mapNotNull { id -> videoWatchlists.firstOrNull { it.playlistId == id } }
    }
    val pinnedMusicPl = remember(pins.playlistIds, musicPlaylists) {
        pins.playlistIds.mapNotNull { id -> musicPlaylists.firstOrNull { it.id == id } }
    }
    val totalPins = pinnedCustom.size + pinnedPFolders.size + pinnedWatch.size +
        pinnedMusicPl.size + pins.artists.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = SentupGroup.gap)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.PushPin,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = if (totalPins > 0) "Sabitlenenler ($totalPins)" else "Sabitlenenler Merkezi",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            FilledTonalButton(
                onClick = onOpenPinManager,
                shape = RoundedCornerShape(SentupRadii.pill),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Sabitle / Yönet", style = MaterialTheme.typography.labelMedium)
            }
        }

        if (totalPins == 0) {
            Surface(
                onClick = onOpenPinManager,
                shape = RoundedCornerShape(SentupRadii.card),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SentupGroup.inset, vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Klasör, İzleme Listesi ve Sınırsız Sanatçı Sabitle",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Buraya dokunarak sevdiğin klasörleri, izleme listelerini ve istediğin kadar sanatçıyı ana sayfaya ekle.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                    }
                }
            }
        } else {
            LazyRow(
                contentPadding = PaddingValues(horizontal = SentupGroup.inset, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(pinnedCustom, key = { "pin_cf_${it.id}" }) { folder ->
                    val folderSongs = remember(folder, allSongs) {
                        CustomFolderTree.resolveFolderSongs(folder, allSongs)
                    }
                    PinnedTileCard(
                        badge = "Klasör",
                        title = folder.name,
                        subtitle = "${folderSongs.size} şarkı • Aç / Çal",
                        icon = Icons.Rounded.FolderOpen,
                        coverUrl = folder.coverUri?.takeIf { it.isNotBlank() }
                            ?: folderSongs.firstOrNull()?.let { it.albumArtUri?.toString() ?: it.thumbnailUrl },
                        circular = false,
                        onClick = { onOpenCustomFolder(folder) },
                        onPlayClick = { onPlayCustomFolder(folder) },
                        onLongClick = { onUnpinCustomFolder(folder.id) }
                    )
                }
                items(pinnedPFolders, key = { "pin_pf_${it.id}" }) { folder ->
                    val count = PlaylistFolderLogic.totalSongsInFolder(folder, musicPlaylists)
                    PinnedTileCard(
                        badge = "Liste Klasörü",
                        title = folder.name,
                        subtitle = "${folder.playlistIds.size} liste • $count şarkı",
                        icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                        coverUrl = PlaylistFolderLogic.folderPreviewThumbnails(folder, musicPlaylists, 1).firstOrNull(),
                        circular = false,
                        onClick = { onPlayPlaylistFolder(folder) },
                        onPlayClick = { onPlayPlaylistFolder(folder) },
                        onLongClick = { onUnpinPlaylistFolder(folder.id) }
                    )
                }
                items(pinnedWatch, key = { "pin_wl_${it.playlistId}" }) { wl ->
                    PinnedTileCard(
                        badge = if (wl.playlistId.contains("WL")) "İzleme Listesi" else "Video Listesi",
                        title = wl.title,
                        subtitle = wl.videoCountText ?: wl.subtitle ?: "İzlemek için dokun",
                        icon = Icons.Rounded.WatchLater,
                        coverUrl = wl.thumbnailUrl,
                        circular = false,
                        onClick = { onOpenWatchlist(wl) },
                        onPlayClick = { onOpenWatchlist(wl) },
                        onLongClick = { onUnpinWatchlist(wl.playlistId) }
                    )
                }
                items(pinnedMusicPl, key = { "pin_mp_${it.id}" }) { pl ->
                    PinnedTileCard(
                        badge = "Çalma Listesi",
                        title = pl.name,
                        subtitle = if (pl.itemCount > 0) "${pl.itemCount} şarkı" else pl.uploaderName,
                        icon = Icons.Rounded.MusicNote,
                        coverUrl = pl.thumbnailUrl,
                        circular = false,
                        onClick = { onOpenMusicPlaylist(pl) },
                        onPlayClick = { onOpenMusicPlaylist(pl) },
                        onLongClick = { onUnpinMusicPlaylist(pl.id) }
                    )
                }
                items(pins.artists, key = { "pin_art_${it.name.lowercase()}" }) { artist ->
                    val resolvedCover = artistPhotos[artist.name] ?: artist.thumbnailUrl
                    PinnedTileCard(
                        badge = "Sanatçı",
                        title = artist.name,
                        subtitle = "Basılı tut: Radyo / Kaldır",
                        icon = Icons.Rounded.Person,
                        coverUrl = resolvedCover,
                        circular = true,
                        onClick = { onOpenArtist(artist.name) },
                        onPlayClick = { onArtistRadio(artist.name) },
                        onLongClick = { onUnpinArtist(artist.name) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PinnedTileCard(
    badge: String,
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    coverUrl: String?,
    circular: Boolean,
    onClick: () -> Unit,
    onPlayClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(SentupRadii.card),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .width(142.dp)
            .clip(RoundedCornerShape(SentupRadii.card))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(92.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (circular) 86.dp else 92.dp)
                        .clip(if (circular) CircleShape else RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!coverUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = googleImageAtSize(coverUrl, 240),
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(30.dp)
                        .clip(CircleShape)
                        .clickable(onClick = onPlayClick)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Başlat",
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Text(
                text = badge,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePinManagerSheet(
    pins: HomePinsSnapshot,
    customFolders: List<MusicFolder>,
    playlistFolders: List<PlaylistFolder>,
    musicPlaylists: List<PlaylistDisplayItem>,
    videoWatchlists: List<VideoPlaylist>,
    topArtists: List<TopArtist>,
    artistPhotos: Map<String, String>,
    onSearchArtists: suspend (String) -> List<ArtistItem>,
    onToggleCustomFolder: (String) -> Unit,
    onTogglePlaylistFolder: (String) -> Unit,
    onToggleWatchlist: (String) -> Unit,
    onToggleMusicPlaylist: (String) -> Unit,
    onToggleArtist: (name: String, thumb: String?) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    var artistQuery by rememberSaveable { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<ArtistItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }

    LaunchedEffect(artistQuery) {
        val trimmed = artistQuery.trim()
        if (trimmed.length < 2) {
            searchResults = emptyList()
            isSearching = false
            return@LaunchedEffect
        }
        isSearching = true
        delay(320)
        searchResults = runCatching { onSearchArtists(trimmed) }.getOrDefault(emptyList())
        isSearching = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Ana Sayfaya Sabitle",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Klasörler, izleme listeleri ve sınırsız sanatçı",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Bitti")
                }
            }

            PrimaryTabRow(selectedTabIndex = tabIndex) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    text = { Text("Klasörler (${customFolders.size + playlistFolders.size})") }
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = { Text("Listeler & İzleme") }
                )
                Tab(
                    selected = tabIndex == 2,
                    onClick = { tabIndex = 2 },
                    text = { Text("Sanatçılar (${pins.artists.size})") }
                )
            }

            when (tabIndex) {
                0 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (customFolders.isEmpty() && playlistFolders.isEmpty()) {
                            item {
                                Text(
                                    text = "Henüz klasör yok. Kitaplık > Klasörler bölümünden oluşturabilirsiniz.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(12.dp)
                                )
                            }
                        }
                        if (customFolders.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Müzik Klasörlerin",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(customFolders, key = { "mgr_cf_${it.id}" }) { folder ->
                                val pinned = folder.id in pins.customFolderIds
                                PinCheckRow(
                                    title = folder.name,
                                    subtitle = "${folder.artistNames.size} sanatçı • ${folder.songIds.size} doğrudan şarkı",
                                    icon = Icons.Rounded.FolderOpen,
                                    pinned = pinned,
                                    onClick = { onToggleCustomFolder(folder.id) }
                                )
                            }
                        }
                        if (playlistFolders.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Oynatma Listesi Klasörleri",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            items(playlistFolders, key = { "mgr_pf_${it.id}" }) { folder ->
                                val pinned = folder.id in pins.playlistFolderIds
                                PinCheckRow(
                                    title = folder.name,
                                    subtitle = "${folder.playlistIds.size} liste içeriyor",
                                    icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                                    pinned = pinned,
                                    onClick = { onTogglePlaylistFolder(folder.id) }
                                )
                            }
                        }
                    }
                }
                1 -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (videoWatchlists.isNotEmpty()) {
                            item {
                                Text(
                                    text = "İzleme Listeleri & Daha Sonra İzle",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            items(videoWatchlists, key = { "mgr_wl_${it.playlistId}" }) { wl ->
                                val pinned = wl.playlistId in pins.watchlistIds
                                PinCheckRow(
                                    title = wl.title,
                                    subtitle = wl.videoCountText ?: wl.subtitle ?: "Video izleme listesi",
                                    icon = Icons.Rounded.WatchLater,
                                    pinned = pinned,
                                    onClick = { onToggleWatchlist(wl.playlistId) }
                                )
                            }
                        }
                        if (musicPlaylists.isNotEmpty()) {
                            item {
                                Text(
                                    text = "Müzik Çalma Listeleri",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                            items(musicPlaylists, key = { "mgr_mp_${it.id}" }) { pl ->
                                val pinned = pl.id in pins.playlistIds
                                PinCheckRow(
                                    title = pl.name,
                                    subtitle = if (pl.itemCount > 0) "${pl.itemCount} şarkı" else pl.uploaderName,
                                    icon = Icons.Rounded.MusicNote,
                                    pinned = pinned,
                                    onClick = { onToggleMusicPlaylist(pl.id) }
                                )
                            }
                        }
                    }
                }
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = artistQuery,
                            onValueChange = { artistQuery = it },
                            placeholder = { Text("Sınırsız sanatçı ara ve sabitle...") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                            trailingIcon = {
                                if (isSearching) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                } else if (artistQuery.isNotBlank()) {
                                    IconButton(onClick = { artistQuery = ""; searchResults = emptyList() }) {
                                        Icon(Icons.Rounded.Close, contentDescription = "Temizle")
                                    }
                                }
                            },
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (searchResults.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "YouTube Music Arama Sonuçları",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                items(searchResults, key = { "srch_${it.id}_${it.name}" }) { artist ->
                                    val pinned = pins.artists.any { it.name.equals(artist.name, ignoreCase = true) }
                                    PinArtistRow(
                                        name = artist.name,
                                        subtitle = artist.subscriberCount ?: "YouTube Music sanatçısı",
                                        thumb = artist.thumbnailUrl,
                                        pinned = pinned,
                                        onClick = { onToggleArtist(artist.name, artist.thumbnailUrl) }
                                    )
                                }
                            }
                            if (pins.artists.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Sabitlenen Sanatçılar (${pins.artists.size} — Sınırsız)",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                items(pins.artists, key = { "pinned_${it.name.lowercase()}" }) { artist ->
                                    PinArtistRow(
                                        name = artist.name,
                                        subtitle = "Ana sayfaya sabitlendi",
                                        thumb = artistPhotos[artist.name] ?: artist.thumbnailUrl,
                                        pinned = true,
                                        onClick = { onToggleArtist(artist.name, artist.thumbnailUrl) }
                                    )
                                }
                            }
                            if (topArtists.isNotEmpty()) {
                                item {
                                    Text(
                                        text = "Çok Dinlediğin Sanatçılar",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                items(topArtists, key = { "top_${it.name.lowercase()}" }) { artist ->
                                    val pinned = pins.artists.any { it.name.equals(artist.name, ignoreCase = true) }
                                    val photo = artistPhotos[artist.name]
                                    PinArtistRow(
                                        name = artist.name,
                                        subtitle = "${artist.plays} dinlenme geçmişinde",
                                        thumb = photo,
                                        pinned = pinned,
                                        onClick = { onToggleArtist(artist.name, photo) }
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

@Composable
private fun PinCheckRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    pinned: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (pinned) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Icon(
                imageVector = if (pinned) Icons.Rounded.CheckCircle else Icons.Rounded.PushPin,
                contentDescription = null,
                tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun PinArtistRow(
    name: String,
    subtitle: String,
    thumb: String?,
    pinned: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (pinned) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerHigh
        },
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center
            ) {
                if (!thumb.isNullOrBlank()) {
                    AsyncImage(
                        model = googleImageAtSize(thumb, 140),
                        contentDescription = name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(Icons.Rounded.Person, contentDescription = null)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(
                shape = RoundedCornerShape(SentupRadii.pill),
                color = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    text = if (pinned) "Sabitli" else "Sabitle",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (pinned) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }
    }
}

/**
 * Live mini console + playback speed + sleep timer bar + smart lens filter +
 * sonic energy launcher — supercharges YouTube Music Home above stock UX.
 */
@Composable
fun MusicHomeProControlDeck(
    currentSong: Song?,
    isPlaying: Boolean,
    sleepEndsAtMs: Long?,
    sleepEndOfTrack: Boolean,
    selectedFilter: HomeSonicFilter,
    onFilterChange: (HomeSonicFilter) -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSetSpeed: (Float) -> Unit,
    onStartSleepMinutes: (Int) -> Unit,
    onStartSleepEndOfTrack: () -> Unit,
    onCancelSleep: () -> Unit,
    onLaunchEnergyMix: (energyLevel: Int) -> Unit,
    onLaunchArtistSpaced: () -> Unit,
    onLaunchOfflineVault: () -> Unit,
    offlineCount: Int,
    modifier: Modifier = Modifier
) {
    var selectedSpeed by rememberSaveable { mutableStateOf(1.0f) }
    var selectedEnergy by rememberSaveable { mutableIntStateOf(1) } // 0 calm, 1 balanced, 2 energy
    val hasSleep = sleepEndsAtMs != null || sleepEndOfTrack

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = SentupGroup.gap),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Feature: Instant Smart Lens Filter Bar
        LazyRow(
            contentPadding = PaddingValues(horizontal = SentupGroup.inset),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(HomeSonicFilter.entries, key = { "lens_${it.name}" }) { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { onFilterChange(filter) },
                    label = { Text(filter.label) },
                    leadingIcon = if (selectedFilter == filter) {
                        { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    } else null
                )
            }
        }

        // Feature: Live Now-Playing Quick Transport + Tempo + Sleep Timer Console
        Surface(
            shape = RoundedCornerShape(SentupRadii.card),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Equalizer,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Column {
                            Text(
                                text = currentSong?.title ?: "Canlı Ses Kontrol Konsolu",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentSong?.artist ?: "Tempo, uyku zamanlayıcı ve enerji profili",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    if (currentSong != null) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilledTonalIconButton(onClick = onPrevious, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.SkipPrevious, contentDescription = "Önceki", modifier = Modifier.size(18.dp))
                            }
                            FilledTonalIconButton(onClick = onPlayPause, modifier = Modifier.size(34.dp)) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = "Oynat/Duraklat",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            FilledTonalIconButton(onClick = onNext, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.SkipNext, contentDescription = "Sonraki", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Speed / Tempo Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Speed,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Hız:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(0.9f to "0.9x Gece", 1.0f to "1.0x Normal", 1.1f to "1.1x Spor", 1.25f to "1.25x Hızlı").forEach { (spd, label) ->
                        FilterChip(
                            selected = kotlin.math.abs(selectedSpeed - spd) < 0.02f,
                            onClick = {
                                selectedSpeed = spd
                                onSetSpeed(spd)
                            },
                            label = { Text(label) }
                        )
                    }
                }

                // Sleep Timer Strip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = null,
                        tint = if (hasSleep) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Uyku:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    listOf(15, 30, 45).forEach { mins ->
                        FilterChip(
                            selected = false,
                            onClick = { onStartSleepMinutes(mins) },
                            label = { Text("$mins dk") }
                        )
                    }
                    FilterChip(
                        selected = sleepEndOfTrack,
                        onClick = onStartSleepEndOfTrack,
                        label = { Text("Şarkı Sonu") }
                    )
                    if (hasSleep) {
                        FilterChip(
                            selected = true,
                            onClick = onCancelSleep,
                            label = { Text("İptal Et") }
                        )
                    }
                }

                // Sonic Energy & Quick Action Launchers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(0 to "Sakin", 1 to "Dengeli", 2 to "Enerjik").forEach { (level, title) ->
                        val active = selectedEnergy == level
                        Surface(
                            onClick = {
                                selectedEnergy = level
                                onLaunchEnergyMix(level)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHighest,
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (level == 2) Icons.Rounded.Bolt else if (level == 0) Icons.Rounded.Bedtime else Icons.Rounded.Tune,
                                    contentDescription = null,
                                    tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilledTonalButton(
                        onClick = onLaunchArtistSpaced,
                        shape = RoundedCornerShape(SentupRadii.pill),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Sanatçı Aralıklı", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(
                        onClick = onLaunchOfflineVault,
                        enabled = offlineCount > 0,
                        shape = RoundedCornerShape(SentupRadii.pill),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Rounded.OfflinePin, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Hazır ($offlineCount)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun MusicHomeProShelves(
    allSongs: List<Song>,
    likedSongs: List<Song>,
    recentlyPlayed: List<Song>,
    playCounts: Map<String, Int>,
    customFolders: List<MusicFolder>,
    topArtists: List<TopArtist>,
    artistPhotos: Map<String, String>,
    videoWatchlists: List<VideoPlaylist>,
    pins: HomePinsSnapshot,
    weekMinutes: Long,
    weekTopArtist: String?,
    onSongClick: (Song) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onPlayQueue: (List<Song>, Song?) -> Unit,
    onShuffleQueue: (List<Song>) -> Unit,
    onOpenCustomFolder: (MusicFolder) -> Unit,
    onPlayCustomFolder: (MusicFolder) -> Unit,
    onStartArtistRadio: (String) -> Unit,
    onOpenWatchlist: (VideoPlaylist) -> Unit,
    onTogglePinWatchlist: (String) -> Unit,
    onOpenStats: () -> Unit,
    onSaveDurationMix: (String, List<Song>) -> Unit,
    onRunSeedDiscovery: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = SentupGroup.gap),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Feature: Folders Quick-Launch Rail
        if (customFolders.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Klasörlerinden Hemen Çal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = SentupGroup.inset)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = SentupGroup.inset),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(customFolders.take(12), key = { "hf_f_${it.id}" }) { folder ->
                        val fSongs = remember(folder, allSongs) {
                            CustomFolderTree.resolveFolderSongs(folder, allSongs, playCounts)
                        }
                        Surface(
                            onClick = { onOpenCustomFolder(folder) },
                            shape = RoundedCornerShape(SentupRadii.card),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.width(196.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.FolderOpen,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(folder.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${fSongs.size} şarkı", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                FilledTonalIconButton(
                                    onClick = { onPlayCustomFolder(folder) },
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Icon(Icons.Rounded.PlayArrow, contentDescription = "Çal", modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Feature: En Çok Tekrarlananlar (Most Replayed with play count badges)
        val replayed = remember(allSongs, playCounts) {
            allSongs
                .map { s -> s to (playCounts[s.id] ?: 0) }
                .filter { it.second > 0 }
                .sortedByDescending { it.second }
                .take(12)
        }
        if (replayed.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SentupGroup.inset),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Döngüdeki Favorilerin",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onShuffleQueue(replayed.map { it.first }) }) {
                        Text("Karıştır")
                    }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = SentupGroup.inset),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(replayed, key = { "rep_${it.first.id}" }) { (song, plays) ->
                        val cover = song.albumArtUri?.toString() ?: song.thumbnailUrl
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier
                                .width(136.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSongClick(song) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(92.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!cover.isNullOrBlank()) {
                                        AsyncImage(
                                            model = googleImageAtSize(cover, 200),
                                            contentDescription = song.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(SentupRadii.pill),
                                        color = MaterialTheme.colorScheme.primaryContainer,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = "${plays}x",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(song.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // Feature: Unutulan Hazineler (Liked/Library songs not in recent plays)
        val forgotten = remember(likedSongs, allSongs, recentlyPlayed) {
            val recentSet = recentlyPlayed.mapTo(HashSet()) { it.id }
            (likedSongs + allSongs)
                .distinctBy { it.id }
                .filter { it.id !in recentSet }
                .take(12)
        }
        if (forgotten.size >= 4) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SentupGroup.inset),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Unutulan Hazineler",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    TextButton(onClick = { onPlayQueue(forgotten, forgotten.first()) }) {
                        Text("Tümünü Çal")
                    }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = SentupGroup.inset),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(forgotten, key = { "forg_${it.id}" }) { song ->
                        val cover = song.albumArtUri?.toString() ?: song.thumbnailUrl
                        Surface(
                            onClick = { onPlayQueue(forgotten, song) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            modifier = Modifier.width(128.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(88.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                ) {
                                    if (!cover.isNullOrBlank()) {
                                        AsyncImage(
                                            model = googleImageAtSize(cover, 180),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                                Text(song.title, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
        }

        // Feature: Time-Boxed Instant Duration Mixes (15 / 30 / 60 / 90 dk)
        Surface(
            shape = RoundedCornerShape(SentupRadii.card),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Timer, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        text = "Süreye Göre Anında Karışım",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(15 to "15 dk Hızlı", 30 to "30 dk Yol", 60 to "60 dk Odak", 90 to "90 dk Uzun").forEach { (mins, label) ->
                        FilledTonalButton(
                            onClick = {
                                val built = SmartMixRules.durationMix(
                                    (allSongs + likedSongs + recentlyPlayed).distinctBy { it.id },
                                    mins * 60_000L,
                                    label
                                )
                                if (built.tracks.isNotEmpty()) {
                                    onPlayQueue(built.tracks, built.tracks.first())
                                }
                            },
                            shape = RoundedCornerShape(SentupRadii.pill)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(label)
                        }
                    }
                    OutlinedButton(
                        onClick = {
                            val built = SmartMixRules.durationMix(
                                (allSongs + likedSongs + recentlyPlayed).distinctBy { it.id },
                                45 * 60_000L,
                                "45 dk Özel Karışım"
                            )
                            if (built.tracks.isNotEmpty()) {
                                onSaveDurationMix("45 dk Özel Karışım", built.tracks)
                                Toast.makeText(context, "45 dk karışım kitaplığa kaydedildi", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(SentupRadii.pill)
                    ) {
                        Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("45 dk Kaydet")
                    }
                }
            }
        }

        // Feature: Artist Radio Roulette + Discovery Seeds
        val radioPool = remember(pins.artists, topArtists) {
            (pins.artists.map { it.name } + topArtists.map { it.name }).distinct()
        }
        if (radioPool.isNotEmpty()) {
            val spotlightArtist = radioPool.first()
            Surface(
                shape = RoundedCornerShape(SentupRadii.card),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SentupGroup.inset)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val photo = artistPhotos[spotlightArtist]
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!photo.isNullOrBlank()) {
                            AsyncImage(
                                model = googleImageAtSize(photo, 140),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Rounded.Radio, contentDescription = null)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Sanatçı Radyosu Ruleti",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${radioPool.size} sanatçı havuzundan anında radyo aç",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                    Button(
                        onClick = { onStartArtistRadio(radioPool.random()) },
                        shape = RoundedCornerShape(SentupRadii.pill)
                    ) {
                        Icon(Icons.Rounded.Radio, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Başlat")
                    }
                }
            }
        }

        // Feature: Discovery Seed Launcher (instant live query vibes)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Anlık Keşif Tohumları",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = SentupGroup.inset)
            )
            LazyRow(
                contentPadding = PaddingValues(horizontal = SentupGroup.inset),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    listOf(
                        "Yeni Çıkanlar" to "2026 yeni çıkan Türkçe müzikler",
                        "Akustik Gece" to "turkish acoustic sessions live",
                        "Yüksek Tempo" to "workout phonk electro energy hits",
                        "80s / 90s Nostalji" to "90lar türkçe pop klasikleri",
                        "Enstrümantal Odak" to "deep focus instrumental study beats"
                    ),
                    key = { "seed_${it.first}" }
                ) { (title, query) ->
                    FilledTonalButton(
                        onClick = { onRunSeedDiscovery(query) },
                        shape = RoundedCornerShape(SentupRadii.pill)
                    ) {
                        Icon(Icons.Rounded.Explore, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(title)
                    }
                }
            }
        }

        // Feature: Video Watchlist / İzleme Listesi Köprüsü right inside Music Home
        if (videoWatchlists.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "İzleme Listelerin (Tek Dokunuş Geçiş)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = SentupGroup.inset)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = SentupGroup.inset),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(videoWatchlists.take(10), key = { "mw_${it.playlistId}" }) { wl ->
                        val isPinned = wl.playlistId in pins.watchlistIds
                        Surface(
                            onClick = { onOpenWatchlist(wl) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.width(210.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.VideoLibrary,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(wl.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(wl.videoCountText ?: wl.subtitle ?: "İzleme listesi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                IconButton(
                                    onClick = { onTogglePinWatchlist(wl.playlistId) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isPinned) Icons.Rounded.CheckCircle else Icons.Rounded.PushPin,
                                        contentDescription = "Sabitle",
                                        tint = if (isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Feature: Weekly Listening Goal Progress Card
        val goalMinutes = 300L
        val progress = (weekMinutes.toFloat() / goalMinutes.toFloat()).coerceIn(0f, 1f)
        Surface(
            onClick = onOpenStats,
            shape = RoundedCornerShape(SentupRadii.card),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.Insights, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("Haftalık Ritim Hedefi", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "$weekMinutes / $goalMinutes dk",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = FontFamily.Monospace),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(SentupRadii.pill))
                )
                weekTopArtist?.let {
                    Text(
                        text = "Bu haftanın lider sanatçısı: $it",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
