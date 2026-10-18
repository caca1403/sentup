package com.sentup.android.ui.video
import androidx.compose.ui.res.stringResource
import com.sentup.android.R

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.SdStorage
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sentup.android.util.KLog
import com.sentup.android.data.CaptionTrack
import com.sentup.android.data.DownloadRepository
import com.sentup.android.data.DownloadStatus
import com.sentup.android.data.DownloadedCaptionStore
import com.sentup.android.data.ThemePreferences
import com.sentup.android.data.VideoItem
import com.sentup.android.data.VideoQuality
import com.sentup.android.data.YouTubeRepository
import com.sentup.android.ui.downloads.formatDownloadSize
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupGroupCard
import com.sentup.android.ui.theme.SentupGroupHeader
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.SentupRow
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupSpecularEdge
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Download a video: the quality ladder as one card of ticked rows, the captions
 * to keep as a multi-select capsule strip, what it will cost, and one filled
 * capsule that starts it.
 *
 * The old sheet had the qualities as a `FlowRow` of 14dp pills with no
 * hierarchy between them, so the difference between 360p and 4K was the width
 * of a chip rather than anything the eye could scan - and the one fact that
 * actually decides the choice, how big the file will be, was in an unlabelled
 * row under the middle of the sheet, below the fold in landscape.
 *
 * The ladder is a list because it is one decision taken from a handful, and a
 * list can say things a row of chips cannot: which one is chosen, how big it is
 * beside its own label, and that nothing is available when nothing is. Captions
 * stay chips, because there can be a hundred languages and the sheet is asking
 * "which of these", not "which one".
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalLayoutApi::class)
@Composable
fun VideoDownloadSheet(
    video: VideoItem,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val themePreferences = remember(context) { ThemePreferences(context) }
    val youtubeRepository = remember(context) { YouTubeRepository(context) }
    val downloadRepository = remember(context) { DownloadRepository.getInstance(context) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var options by remember(video.videoId) { mutableStateOf<List<VideoQuality>?>(null) }
    var loadFailed by remember(video.videoId) { mutableStateOf(false) }
    var selectedLabel by remember(video.videoId) { mutableStateOf<String?>(null) }
    var rememberQuality by remember { mutableStateOf(false) }
    var queued by remember(video.videoId) { mutableStateOf(false) }
    var estimatedBytes by remember(video.videoId) { mutableStateOf<Long?>(null) }
    var sizeLoading by remember(video.videoId) { mutableStateOf(false) }
    var sizeResolved by remember(video.videoId) { mutableStateOf(false) }

    // Captions to keep with the download. Null while unknown; the section only
    // appears once there is something to choose, so a video without captions
    // shows nothing rather than an empty heading.
    var captionTracks by remember(video.videoId) { mutableStateOf<List<CaptionTrack>?>(null) }
    var selectedCaptions by remember(video.videoId) { mutableStateOf<Set<String>>(emptySet()) }
    var showAllCaptions by remember(video.videoId) { mutableStateOf(false) }

    val downloadedVideos by downloadRepository.downloadedVideos.collectAsState()
    val progressMap by downloadRepository.downloadProgress.collectAsState()
    val alreadyDownloaded = downloadedVideos.any { it.id == video.videoId }
    val activeEntry = progressMap[video.videoId]
    val inFlight = activeEntry?.status.let {
        it == DownloadStatus.DOWNLOADING || it == DownloadStatus.QUEUED
    }
    // A paused download is still held by the queue, which refuses a second
    // request for it. Offered as a fresh download, the button said "Added" and
    // nothing happened.
    val paused = activeEntry?.status == DownloadStatus.PAUSED

    fun height(label: String): Int = label.takeWhile { it.isDigit() }.toIntOrNull() ?: 0

    LaunchedEffect(video.videoId) {
        try {
            val qualities = youtubeRepository.getVideoStreamQualities(video.videoId)
            // Same eligibility rules as the download worker: MP4 adaptive
            // pairs first, else the muxed progressive stream (typically 360p).
            val mp4Adaptive = qualities.filter {
                !it.isDASH && it.audioUrl != null && it.isMp4DownloadCompatible
            }
            val downloadable = mp4Adaptive.ifEmpty {
                qualities.filter { !it.isDASH && it.audioUrl == null && it.isMp4Container }
            }.distinctBy { it.resolution }
            options = downloadable
            loadFailed = downloadable.isEmpty() && qualities.isEmpty()

            // Preselect the stored default: best label at or below its height,
            // "auto" (or no match below) meaning the best available.
            val preferred = themePreferences.getDownloadVideoQuality()
            val targetHeight = height(preferred)
            selectedLabel = when {
                downloadable.isEmpty() -> null
                preferred == ThemePreferences.VIDEO_QUALITY_AUTO || targetHeight == 0 ->
                    downloadable.first().resolution
                else -> downloadable.firstOrNull { height(it.resolution) in 1..targetHeight }?.resolution
                    ?: downloadable.first().resolution
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            KLog.w("VideoDownloadSheet", "Failed to load qualities", e)
            options = emptyList()
            loadFailed = true
        }
    }

    // After the qualities: that resolution reads the same /player response the
    // caption list comes from, so this is normally answered from memory.
    LaunchedEffect(video.videoId, options != null) {
        if (options == null) return@LaunchedEffect
        val tracks = youtubeRepository.getCaptionTracks(video.videoId)
        val locales = context.resources.configuration.locales
        selectedCaptions = DownloadedCaptionStore.preferredTracks(
            available = tracks,
            savedLanguage = themePreferences.getCaptionLanguageCode(),
            deviceLanguages = (0 until locales.size()).map { locales[it].language },
        ).mapTo(HashSet()) { DownloadedCaptionStore.keyOf(it) }
        captionTracks = tracks
    }

    LaunchedEffect(selectedLabel, options) {
        val selected = options.orEmpty().firstOrNull { it.resolution == selectedLabel }
        if (selected == null) {
            estimatedBytes = null
            sizeLoading = false
            sizeResolved = false
            return@LaunchedEffect
        }
        estimatedBytes = null
        sizeResolved = false
        sizeLoading = true
        estimatedBytes = downloadRepository.estimateVideoDownloadBytes(selected)
        sizeLoading = false
        sizeResolved = true
    }

    // Linger on the queued state for a beat, then close (same rhythm as the
    // save sheet's post-save dismissal)
    LaunchedEffect(queued) {
        if (queued) {
            delay(900)
            onDismiss()
        }
    }

    VideoSheetScaffold(
        onDismiss = onDismiss,
        sheetState = sheetState,
        title = stringResource(R.string.vd_title)
    ) {
        Column(
            modifier = Modifier
                // Qualities, captions and the rest run past a short or
                // landscape window; unscrolled, the Download button was the
                // part that got clipped away.
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = SentupGroup.gap)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = SentupGroup.inset),
                verticalAlignment = Alignment.CenterVertically
            ) {
                VideoSheetThumbnail(
                    url = video.thumbnailUrl,
                    modifier = Modifier.width(104.dp),
                    height = 60.dp
                )
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = video.channelName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(SentupGroup.gap))

            SentupGroupHeader(
                text = stringResource(R.string.sp_video_quality),
                subtitle = stringResource(R.string.vd_pick_quality)
            )

            when {
                options == null -> Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    ContainedLoadingIndicator()
                }
                options.orEmpty().isEmpty() -> {
                    VideoSheetEmptyState(
                        title = if (loadFailed) {
                            stringResource(R.string.vd_load_failed)
                        } else {
                            stringResource(R.string.vd_no_sizes)
                        },
                        icon = Icons.Rounded.ErrorOutline
                    )
                }
                else -> {
                    // One card of ticked rows rather than a row of chips: a
                    // ladder is a single decision out of a handful, and a list
                    // can carry the frame size and the file size beside each
                    // label - which is what actually decides the choice.
                    SentupGroupCard(showDividers = false) {
                        options.orEmpty().forEachIndexed { index, quality ->
                            val chosen = quality.resolution == selectedLabel
                            val bytes = if (chosen) estimatedBytes else null
                            VideoChoiceRow(
                                title = quality.resolution,
                                selected = chosen,
                                onClick = { selectedLabel = quality.resolution },
                                value = when {
                                    chosen && sizeLoading -> stringResource(R.string.vd_calculating)
                                    bytes != null -> formatDownloadSize(bytes)
                                    else -> null
                                }
                            )
                            if (index < options.orEmpty().lastIndex) SentupDivider()
                        }
                    }
                }
            }

            // Captions to keep offline. Only for a video that can be downloaded
            // and has some: tap to add or drop a language, none is allowed.
            val tracks = captionTracks.orEmpty()
            if (selectedLabel != null && tracks.isNotEmpty()) {
                Spacer(modifier = Modifier.height(SentupGroup.gap))
                SentupGroupHeader(
                    text = stringResource(R.string.vp_captions),
                    subtitle = stringResource(R.string.vd_captions_pick_sub)
                )
                // Some videos list a hundred translations. The ticked ones and
                // the first few lead; the rest wait behind one pill.
                val collapsed = remember(tracks, showAllCaptions) {
                    if (showAllCaptions || tracks.size <= CAPTION_PILLS_COLLAPSED) tracks
                    else {
                        val ticked = tracks.filter { DownloadedCaptionStore.keyOf(it) in selectedCaptions }
                        (ticked + tracks.filterNot { it in ticked })
                            .take(maxOf(CAPTION_PILLS_COLLAPSED, ticked.size))
                    }
                }
                SentupGroupCard(showDividers = false) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        collapsed.forEach { track ->
                            val key = DownloadedCaptionStore.keyOf(track)
                            QualityPill(
                                label = if (track.isAutoGenerated) "${track.name} (auto)" else track.name,
                                selected = key in selectedCaptions,
                                onClick = {
                                    selectedCaptions =
                                        if (key in selectedCaptions) selectedCaptions - key
                                        else selectedCaptions + key
                                }
                            )
                        }
                        val hidden = tracks.size - collapsed.size
                        if (hidden > 0) {
                            QualityPill(
                                label = stringResource(R.string.vd_captions_more, hidden),
                                selected = false,
                                onClick = { showAllCaptions = true }
                            )
                        }
                    }
                }
            }

            if (selectedLabel != null) {
                Spacer(modifier = Modifier.height(SentupGroup.gap))
                SentupGroupHeader(text = stringResource(R.string.vd_estimated))
                SentupGroupCard(showDividers = false) {
                    SentupRow(
                        icon = Icons.Rounded.Download,
                        title = when {
                            sizeLoading -> stringResource(R.string.vd_calculating)
                            estimatedBytes != null -> formatDownloadSize(estimatedBytes!!)
                            sizeResolved -> stringResource(R.string.vd_size_unavailable)
                            else -> ""
                        },
                        subtitle = stringResource(R.string.vd_estimated),
                        value = options.orEmpty()
                            .firstOrNull { it.resolution == selectedLabel }
                            ?.let { it.resolution },
                        tint = if (estimatedBytes != null) {
                            MaterialTheme.colorScheme.primary
                        } else null
                    )
                }
            }

            // Remember-as-default switch: persisted only when the download
            // actually starts, so browsing the ladder never rewrites the default.
            Spacer(modifier = Modifier.height(SentupGroup.gap))
            SentupGroupCard(showDividers = false) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SentupGroup.dividerInset, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.vd_remember_quality),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.vd_remember_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                    Switch(
                        checked = rememberQuality,
                        onCheckedChange = { rememberQuality = it },
                        enabled = selectedLabel != null
                    )
                }
            }

            Spacer(modifier = Modifier.height(SentupGroup.gap))

            VideoSheetPrimaryButton(
                label = when {
                    alreadyDownloaded -> stringResource(R.string.vd_already)
                    queued || inFlight -> when {
                        queued -> stringResource(R.string.vd_added)
                        activeEntry?.status == DownloadStatus.QUEUED ->
                            stringResource(R.string.dl_waiting)
                        activeEntry?.finishing == true ->
                            stringResource(R.string.dl_finishing)
                        (activeEntry?.totalBytes ?: 0L) > 0L -> stringResource(
                            R.string.vd_progress,
                            ((activeEntry?.progress ?: 0f) * 100).toInt()
                        )
                        else -> stringResource(R.string.dl_preparing)
                    }
                    paused -> stringResource(R.string.dl_resume)
                    estimatedBytes != null ->
                        "${stringResource(R.string.song_options_download)} · ${formatDownloadSize(estimatedBytes ?: 0L)}"
                    else -> stringResource(R.string.song_options_download)
                },
                onClick = {
                    if (paused) {
                        downloadRepository.resumeDownload(video.videoId)
                        queued = true
                        return@VideoSheetPrimaryButton
                    }
                    val label = selectedLabel ?: return@VideoSheetPrimaryButton
                    if (rememberQuality) themePreferences.setDownloadVideoQuality(label)
                    // Null while the caption list has not arrived: the
                    // download then keeps the likely ones rather than none.
                    val captions = selectedCaptions.takeIf { captionTracks != null }
                    scope.launch { downloadRepository.downloadVideo(video, label, captions) }
                    queued = true
                },
                // Resuming needs neither the quality ladder nor the size: the
                // paused request already carries its choice.
                enabled = !queued && !alreadyDownloaded && !inFlight &&
                    (paused || (selectedLabel != null && !sizeLoading && sizeResolved)),
                modifier = Modifier.padding(horizontal = SentupGroup.inset),
                leading = {
                    when {
                        alreadyDownloaded -> Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        queued || inFlight -> LoadingIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        else -> Icon(
                            imageVector = Icons.Rounded.Download,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            )
        }
    }
}

/** Caption pills shown before the rest fold behind a "more" pill. */
private const val CAPTION_PILLS_COLLAPSED = 6

/**
 * One caption language, as a capsule that ticks when it will be kept.
 *
 * Chips rather than rows, and that is the honest shape for this control rather
 * than an inconsistency in the sheet: a video can carry a hundred
 * translations, they are named in the language they are *for* rather than in a
 * resolution, and the question being asked is which of these rather than which
 * one. A hundred-row list for "would you also like Turkish" would be a worse
 * interface, not a more consistent one.
 *
 * The press is the house contract - scale, no ripple - so a strip of sixty
 * capsules reads as sixty objects on the page rather than sixty flashes.
 */
@Composable
private fun QualityPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(SentupRadii.pill)
    Row(
        modifier = Modifier
            .videoPressTarget(onClick = onClick, shape = shape, scaleTo = 0.95f)
            .background(
                if (selected) scheme.primaryContainer else scheme.surfaceContainerHigh,
                shape
            )
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (selected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = null,
                tint = scheme.onPrimaryContainer,
                modifier = Modifier.size(15.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) scheme.onPrimaryContainer else scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
