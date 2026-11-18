package com.sentup.android.ui.video

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.sentup.android.data.DownloadedVideo
import com.sentup.android.data.SubscribedChannel
import com.sentup.android.data.VideoItem
import com.sentup.android.data.VideoPlaylist
import com.sentup.android.data.googleImageAtSize
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class VideoFeedSortMode(val label: String) {
    Default("Akış"),
    Shortest("En Kısa"),
    Longest("En Uzun"),
    Newest("En Yeni"),
    Shuffled("Karışık")
}

internal fun applyEnhancedVideoFeedFilters(
    base: List<VideoItem>,
    searchQuery: String,
    selectedCreatorFilter: String?,
    sortMode: VideoFeedSortMode,
    shuffleSeed: Int
): List<VideoItem> {
    val q = searchQuery.trim()
    val filtered = base.filter { video ->
        val matchesQuery = q.isEmpty() ||
            video.title.contains(q, ignoreCase = true) ||
            video.channelName.contains(q, ignoreCase = true)
        val matchesCreator = selectedCreatorFilter.isNullOrBlank() ||
            video.channelId == selectedCreatorFilter ||
            video.channelName.equals(selectedCreatorFilter, ignoreCase = true)
        matchesQuery && matchesCreator
    }
    return when (sortMode) {
        VideoFeedSortMode.Default -> filtered
        VideoFeedSortMode.Shortest -> filtered.sortedBy {
            if (it.duration <= 0L) Long.MAX_VALUE else it.duration
        }
        VideoFeedSortMode.Longest -> filtered.sortedByDescending { it.duration }
        VideoFeedSortMode.Newest -> filtered.sortedByDescending {
            it.publishedAtMs ?: 0L
        }
        VideoFeedSortMode.Shuffled -> filtered.shuffled(kotlin.random.Random(shuffleSeed))
    }
}

private fun formatDurationCompact(seconds: Long): String {
    if (seconds <= 0L) return "Canlı / Süre Yok"
    val h = seconds / 3600L
    val m = (seconds % 3600L) / 60L
    val s = seconds % 60L
    return if (h > 0) String.format("%d:%02d:%02d", h, m, s)
    else String.format("%d:%02d", m, s)
}

private fun isVideoUploadedRecently(video: VideoItem): Boolean {
    val pub = video.publishedAtMs
    if (pub != null && pub > 0L) {
        return System.currentTimeMillis() - pub <= 48L * 3600_000L
    }
    val date = video.uploadedDate?.lowercase().orEmpty()
    return listOf("hour", "minute", "saat", "dakika", "saniye", "bugün", "today", "just now").any { it in date }
}

/**
 * Top command center for Video Home:
 * Includes inline live search, multi-sort selector, break reminder timer,
 * subscribed creator filter strip, watchlists hub, telemetry & queue launcher,
 * and instant topic launcher.
 */
@Composable
fun VideoHomeCommandHeader(
    rawVideos: List<VideoItem>,
    filteredVideos: List<VideoItem>,
    watchHistory: List<VideoItem>,
    subscribedChannels: List<SubscribedChannel>,
    watchlists: List<VideoPlaylist>,
    downloadedVideos: List<DownloadedVideo>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedCreatorFilter: String?,
    onSelectCreatorFilter: (String?) -> Unit,
    sortMode: VideoFeedSortMode,
    onSortModeChange: (VideoFeedSortMode) -> Unit,
    isCompactLayout: Boolean,
    onToggleCompactLayout: () -> Unit,
    onShufflePlay: () -> Unit,
    onEnqueueAllVisible: () -> Unit,
    onOpenChannel: (String) -> Unit,
    onOpenWatchlist: (VideoPlaylist) -> Unit,
    onPlayVideo: (VideoItem) -> Unit,
    onPlayDownloaded: (DownloadedVideo) -> Unit,
    onQuickWatchLater: (VideoItem) -> Unit,
    onLoadTopicTop10: suspend (String) -> List<VideoItem>,
    onEnqueueVideoList: ((List<VideoItem>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val topicScope = androidx.compose.runtime.rememberCoroutineScope()
    var expandedTopic by rememberSaveable { mutableStateOf<String?>(null) }
    var topicVideosCache by remember { mutableStateOf<Map<String, List<VideoItem>>>(emptyMap()) }
    var isTopicLoading by remember { mutableStateOf(false) }
    var showInlineSearch by rememberSaveable { mutableStateOf(false) }
    var breakLimitMinutes by rememberSaveable { mutableIntStateOf(0) }
    var breakElapsedSec by rememberSaveable { mutableIntStateOf(0) }
    var breakAlertShown by remember { mutableStateOf(false) }

    LaunchedEffect(breakLimitMinutes) {
        breakElapsedSec = 0
        breakAlertShown = false
        if (breakLimitMinutes <= 0) return@LaunchedEffect
        while (true) {
            delay(1000L)
            breakElapsedSec++
            if (!breakAlertShown && breakElapsedSec >= breakLimitMinutes * 60) {
                breakAlertShown = true
                Toast.makeText(
                    context,
                    "$breakLimitMinutes dakikalık izleme hedefin doldu — kısa bir mola ver!",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    val totalSeconds = remember(filteredVideos) {
        filteredVideos.sumOf { it.duration.coerceAtLeast(0L) }
    }
    val totalMinutes = totalSeconds / 60L
    val liveCount = remember(rawVideos) { rawVideos.count { it.isLive } }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Feature 1 + 2: Inline Search Bar + Telemetry / Action Strip
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(SentupRadii.pill),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = buildString {
                            append("${filteredVideos.size} video")
                            if (totalMinutes > 0) append(" • ${totalMinutes / 60}sa ${totalMinutes % 60}dk")
                            if (liveCount > 0) append(" • $liveCount CANLI")
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            FilledTonalIconButton(
                onClick = { showInlineSearch = !showInlineSearch },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = if (showInlineSearch || searchQuery.isNotBlank()) Icons.Rounded.Close else Icons.Rounded.Search,
                    contentDescription = "Akışta Ara",
                    modifier = Modifier.size(18.dp)
                )
            }
            FilledTonalIconButton(
                onClick = onToggleCompactLayout,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.ViewAgenda,
                    contentDescription = if (isCompactLayout) "Geniş Kart" else "Kompakt Kart",
                    modifier = Modifier.size(18.dp)
                )
            }
            FilledTonalIconButton(
                onClick = onEnqueueAllVisible,
                enabled = filteredVideos.isNotEmpty(),
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = "Sıraya Ekle",
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        if (showInlineSearch || searchQuery.isNotBlank()) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Ana akışta başlık veya kanal ara...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = if (searchQuery.isNotBlank()) {
                    {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Rounded.Close, contentDescription = "Temizle", modifier = Modifier.size(16.dp))
                        }
                    }
                } else null,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SentupGroup.inset)
            )
        }

        // Feature 3: Multi-Sort Chips + Watch Break Reminder Timer + Shuffle Action
        LazyRow(
            contentPadding = PaddingValues(horizontal = SentupGroup.inset),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(VideoFeedSortMode.entries, key = { "vsort_${it.name}" }) { mode ->
                FilterChip(
                    selected = sortMode == mode,
                    onClick = { onSortModeChange(mode) },
                    label = { Text(mode.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (mode == VideoFeedSortMode.Shuffled) Icons.Rounded.Shuffle else Icons.AutoMirrored.Rounded.Sort,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                )
            }
            item(key = "v_break_timer") {
                val activeText = when {
                    breakLimitMinutes <= 0 -> "Mola Zamanlayıcı"
                    breakAlertShown -> "Mola Vakti!"
                    else -> {
                        val remSec = (breakLimitMinutes * 60 - breakElapsedSec).coerceAtLeast(0)
                        "Mola: ${remSec / 60}:${(remSec % 60).toString().padStart(2, '0')}"
                    }
                }
                FilterChip(
                    selected = breakLimitMinutes > 0,
                    onClick = {
                        breakLimitMinutes = when (breakLimitMinutes) {
                            0 -> 20
                            20 -> 45
                            45 -> 60
                            else -> 0
                        }
                    },
                    label = { Text(activeText) },
                    leadingIcon = { Icon(Icons.Rounded.Timer, contentDescription = null, modifier = Modifier.size(15.dp)) }
                )
            }
            item(key = "v_shuffle_all") {
                FilterChip(
                    selected = false,
                    enabled = filteredVideos.isNotEmpty(),
                    onClick = onShufflePlay,
                    label = { Text("Rastgele Başlat") },
                    leadingIcon = { Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) }
                )
            }
        }

        // Feature 4: Instant Topic Discovery Launcher Strip + Netflix-Style Top 10 Ranked Showcase
        val topicSpecs = remember {
            listOf(
                "Teknoloji & AI" to "türkiye teknoloji yapay zeka popüler",
                "Belgesel & Bilim" to "türkiye bilim tarih belgesel",
                "Oyun & E-Spor" to "türkiye oyun popüler videolar",
                "Konser & Müzik" to "türkiye canlı konser performans",
                "Podcast" to "türkiye en popüler podcast sohbet",
                "Sinema & Dizi" to "türkiye sinema dizi inceleme",
                "Spor & Gündem" to "türkiye spor gündem özet"
            )
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = SentupGroup.inset),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(topicSpecs, key = { "vtopic_${it.first}" }) { (label, query) ->
                val active = expandedTopic == label
                Surface(
                    onClick = {
                        if (active) {
                            expandedTopic = null
                        } else {
                            expandedTopic = label
                            if (topicVideosCache[label].isNullOrEmpty()) {
                                isTopicLoading = true
                                topicScope.launch {
                                    val fetched = runCatching { onLoadTopicTop10(query) }
                                        .getOrDefault(emptyList())
                                        .distinctBy { it.videoId }
                                        .take(10)
                                    topicVideosCache = topicVideosCache + (label to fetched)
                                    if (expandedTopic == label) isTopicLoading = false
                                }
                            } else {
                                isTopicLoading = false
                            }
                        }
                    },
                    shape = RoundedCornerShape(SentupRadii.pill),
                    color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Explore,
                            contentDescription = null,
                            tint = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            color = if (active) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        androidx.compose.animation.AnimatedVisibility(visible = expandedTopic != null) {
            val topicTitle = expandedTopic ?: return@AnimatedVisibility
            val rankedList = topicVideosCache[topicTitle].orEmpty()
            NetflixTop10RankedDrawer(
                topicTitle = topicTitle,
                videos = rankedList,
                isLoading = isTopicLoading && rankedList.isEmpty(),
                onPlayVideo = onPlayVideo,
                onEnqueueAll = if (rankedList.isNotEmpty() && onEnqueueVideoList != null) {
                    { onEnqueueVideoList(rankedList) }
                } else null,
                onWatchLater = onQuickWatchLater,
                onOpenChannel = onOpenChannel,
                onClose = { expandedTopic = null }
            )
        }

        // Feature 5: Followed Creators Quick Filter & Avatar Rail
        if (subscribedChannels.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = SentupGroup.inset),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(subscribedChannels.take(18), key = { "vh_sub_${it.channelId}" }) { ch ->
                    val isSelected = selectedCreatorFilter == ch.channelId ||
                        selectedCreatorFilter.equals(ch.name, ignoreCase = true)
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(64.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onSelectCreatorFilter(if (isSelected) null else ch.channelId)
                            }
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!ch.avatarUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = googleImageAtSize(ch.avatarUrl, 120),
                                    contentDescription = ch.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Icon(Icons.Rounded.Person, contentDescription = null)
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = ch.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            if (selectedCreatorFilter != null) {
                Row(
                    modifier = Modifier.padding(horizontal = SentupGroup.inset),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = true,
                        onClick = { onSelectCreatorFilter(null) },
                        label = { Text("Kanal filtresini kaldır") },
                        trailingIcon = { Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(15.dp)) }
                    )
                    TextButton(onClick = { onOpenChannel(selectedCreatorFilter) }) {
                        Text("Kanala Git")
                    }
                }
            }
        }

        // Feature 6: Video Watchlists & Watch Later Shortcut Strip
        if (watchlists.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = SentupGroup.inset),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(watchlists.take(10), key = { "vh_wl_${it.playlistId}" }) { wl ->
                    Surface(
                        onClick = { onOpenWatchlist(wl) },
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (wl.playlistId.contains("WL")) Icons.Rounded.WatchLater else Icons.AutoMirrored.Rounded.PlaylistPlay,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(wl.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                wl.videoCountText?.let { count ->
                                    Text(count, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Feature 7: Offline Downloads Vault Bar (visible online too)
        if (downloadedVideos.isNotEmpty()) {
            Surface(
                onClick = { onPlayDownloaded(downloadedVideos.first()) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.72f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SentupGroup.inset)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DownloadDone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Çevrimdışı Hazır Video Kasası (${downloadedVideos.size})",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Son indirilen: ${downloadedVideos.first().title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    FilledTonalButton(
                        onClick = { onPlayDownloaded(downloadedVideos.first()) },
                        shape = RoundedCornerShape(SentupRadii.pill)
                    ) {
                        Text("İzle")
                    }
                }
            }
        }

        // Feature 8: Watch History / Last Session Resume Summary Card
        val latestWatched = watchHistory.firstOrNull()
        if (latestWatched != null) {
            val historyMins = watchHistory.sumOf { (it.duration / 60L).coerceAtLeast(0L) }
            Surface(
                onClick = { onPlayVideo(latestWatched) },
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SentupGroup.inset)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "İzleme Geçmişi • ${watchHistory.size} video (~$historyMins dk)",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Son izlenen: ${latestWatched.title}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { onQuickWatchLater(latestWatched) }) {
                        Icon(Icons.Rounded.WatchLater, contentDescription = "Daha Sonra İzle")
                    }
                }
            }
        }

        // Feature 9: Live Streams Radar Shelf (if any live videos are present)
        val liveVideos = remember(rawVideos) { rawVideos.filter { it.isLive }.take(10) }
        if (liveVideos.isNotEmpty()) {
            VideoHomeShowcaseCarousel(
                title = "Şu An Canlı Yayınlar",
                badge = "CANLI",
                videos = liveVideos,
                onVideoClick = onPlayVideo,
                onWatchLater = onQuickWatchLater,
                onOpenChannel = onOpenChannel
            )
        }
    }
}

/**
 * Mid/lower feed enrichment carousels for Video Home:
 * Fresh Today Uploads, Podcast / Long-Form Audio Mode (>15m), Quick Break Bites (1-6m),
 * and Creator Diversity Spotlight.
 */
@Composable
fun VideoHomeShowcaseShelves(
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onWatchLater: (VideoItem) -> Unit,
    onEnqueueVideo: ((VideoItem) -> Unit)?,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Feature 10: Fresh Uploads Today
        val freshToday = remember(videos) {
            videos.filter(::isVideoUploadedRecently).take(12)
        }
        if (freshToday.size >= 2) {
            VideoHomeShowcaseCarousel(
                title = "Bugünün Taze Yüklemeleri",
                badge = "YENİ",
                videos = freshToday,
                onVideoClick = onVideoClick,
                onWatchLater = onWatchLater,
                onOpenChannel = onOpenChannel,
                onEnqueueVideo = onEnqueueVideo
            )
        }

        // Feature 11: Podcast / Long-Form Focus Shelf (15+ min)
        val longForm = remember(videos) {
            videos.filter { !it.isLive && it.duration >= 900L }.take(12)
        }
        if (longForm.size >= 2) {
            VideoHomeShowcaseCarousel(
                title = "Podcast & Derinlemesine (15+ dk)",
                badge = "UZUN",
                videos = longForm,
                onVideoClick = onVideoClick,
                onWatchLater = onWatchLater,
                onOpenChannel = onOpenChannel,
                onEnqueueVideo = onEnqueueVideo
            )
        }

        // Feature 12: Quick Break Bites (1-6 min)
        val quickBites = remember(videos) {
            videos.filter { !it.isLive && it.duration in 60L..360L }.take(12)
        }
        if (quickBites.size >= 2) {
            VideoHomeShowcaseCarousel(
                title = "Hızlı Mola Videoları (1-6 dk)",
                badge = "KISA",
                videos = quickBites,
                onVideoClick = onVideoClick,
                onWatchLater = onWatchLater,
                onOpenChannel = onOpenChannel,
                onEnqueueVideo = onEnqueueVideo
            )
        }

        // Feature 13: Unique Creators Spotlight Carousel (one latest pick per channel)
        val creatorPicks = remember(videos) {
            videos.filter { it.channelName.isNotBlank() }
                .distinctBy { it.channelId ?: it.channelName.lowercase() }
                .take(12)
        }
        if (creatorPicks.size >= 3) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Farklı Kanallardan Seçkiler",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = SentupGroup.inset)
                )
                LazyRow(
                    contentPadding = PaddingValues(horizontal = SentupGroup.inset),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(creatorPicks, key = { "vcre_${it.videoId}" }) { video ->
                        Surface(
                            onClick = { onOpenChannel(video.channelNavigationReference) },
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.width(170.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (!video.channelIconUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = googleImageAtSize(video.channelIconUrl, 120),
                                            contentDescription = video.channelName,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(video.channelName, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(video.title, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoHomeShowcaseCarousel(
    title: String,
    badge: String,
    videos: List<VideoItem>,
    onVideoClick: (VideoItem) -> Unit,
    onWatchLater: (VideoItem) -> Unit,
    onOpenChannel: (String) -> Unit,
    onEnqueueVideo: ((VideoItem) -> Unit)? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Surface(
                shape = RoundedCornerShape(SentupRadii.pill),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = badge,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = SentupGroup.inset),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(videos, key = { "vc_${badge}_${it.videoId}" }) { video ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .width(214.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .combinedClickable(
                            onClick = { onVideoClick(video) },
                            onLongClick = { onWatchLater(video) }
                        )
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(114.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        ) {
                            if (!video.thumbnailUrl.isNullOrBlank()) {
                                AsyncImage(
                                    model = googleImageAtSize(video.thumbnailUrl, 480),
                                    contentDescription = video.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = if (video.isLive) "CANLI" else formatDurationCompact(video.duration),
                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = video.channelName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onOpenChannel(video.channelNavigationReference) }
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                if (onEnqueueVideo != null) {
                                    IconButton(
                                        onClick = { onEnqueueVideo(video) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = "Sıraya ekle", modifier = Modifier.size(16.dp))
                                    }
                                }
                                IconButton(
                                    onClick = { onWatchLater(video) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.WatchLater, contentDescription = "Daha sonra izle", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Netflix-style expandable Top-10 Ranked drawer showing ranked Turkey results for a selected topic.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun NetflixTop10RankedDrawer(
    topicTitle: String,
    videos: List<VideoItem>,
    isLoading: Boolean,
    onPlayVideo: (VideoItem) -> Unit,
    onEnqueueAll: (() -> Unit)?,
    onWatchLater: (VideoItem) -> Unit,
    onOpenChannel: (String) -> Unit,
    onClose: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
                shape = RoundedCornerShape(22.dp)
            )
    ) {
        Column(
            modifier = Modifier.padding(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.primary
                        ) {
                            Text(
                                text = "TOP 10",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = "Türkiye • $topicTitle",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = "İzlenme sırasına göre en popüler 10 video",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (onEnqueueAll != null) {
                        FilledTonalButton(
                            onClick = onEnqueueAll,
                            shape = RoundedCornerShape(SentupRadii.pill),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Tümünü Sırala", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, contentDescription = "Kapat", modifier = Modifier.size(18.dp))
                    }
                }
            }

            when {
                isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.material3.CircularProgressIndicator(strokeWidth = 2.5.dp)
                    }
                }
                videos.isEmpty() -> {
                    Text(
                        text = "Bu başlıkta şu an video getirilemedi — lütfen tekrar dene.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                    )
                }
                else -> {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(videos.take(10).withIndex().toList(), key = { "nt10_${it.index}_${it.value.videoId}" }) { (idx, video) ->
                            val rank = idx + 1
                            Box(
                                modifier = Modifier
                                    .width(236.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .combinedClickable(
                                        onClick = { onPlayVideo(video) },
                                        onLongClick = { onWatchLater(video) }
                                    )
                            ) {
                                // Main Ranked Card offset right so the giant rank digit overlaps cleanly like Netflix Top 10
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainer,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = 24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(114.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                        ) {
                                            if (!video.thumbnailUrl.isNullOrBlank()) {
                                                AsyncImage(
                                                    model = googleImageAtSize(video.thumbnailUrl, 480),
                                                    contentDescription = video.title,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.primaryContainer,
                                                modifier = Modifier
                                                    .align(Alignment.TopStart)
                                                    .padding(6.dp)
                                            ) {
                                                Text(
                                                    text = "#$rank",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Black,
                                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.88f),
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(6.dp)
                                            ) {
                                                Text(
                                                    text = if (video.isLive) "CANLI" else formatDurationCompact(video.duration),
                                                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = video.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = video.channelName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.clickable { onOpenChannel(video.channelNavigationReference) }
                                                )
                                                if (video.viewCount.isNotBlank()) {
                                                    Text(
                                                        text = video.viewCount,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.primary,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            IconButton(
                                                onClick = { onWatchLater(video) },
                                                modifier = Modifier.size(28.dp)
                                            ) {
                                                Icon(Icons.Rounded.WatchLater, contentDescription = "Daha Sonra İzle", modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                // Oversized Stylized Netflix Top-10 Rank Numeral tucked overlapping the left edge
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.94f),
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(top = 62.dp)
                                        .border(
                                            1.5.dp,
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            RoundedCornerShape(14.dp)
                                        )
                                ) {
                                    Text(
                                        text = "$rank",
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Black,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 2.dp)
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
