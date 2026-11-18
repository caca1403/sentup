package com.sentup.android.ui.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.automirrored.rounded.Comment
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QuestionAnswer
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.StayCurrentPortrait
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sentup.android.R
import com.sentup.android.data.PlayerTrackOption
import com.sentup.android.data.VideoQuality
import com.sentup.android.data.VideoShaderCustomization
import com.sentup.android.ui.theme.SentupRadii
import kotlin.math.roundToInt

/**
 * Modern, categorized Material 3 Expressive redesign of the video playback
 * settings sheet and fullscreen control drawer.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ModernVideoPlaybackSettings(
    isLoading: Boolean,
    qualities: List<VideoQuality>,
    currentQuality: VideoQuality?,
    onQualitySelected: (VideoQuality) -> Unit,
    audioTracks: List<PlayerTrackOption>,
    onAudioTrackSelected: (PlayerTrackOption) -> Unit,
    playbackSpeed: Float,
    onSpeedPreview: (Float) -> Unit,
    onSpeedSelected: (Float) -> Unit,
    showEndBehavior: Boolean,
    autoplayEnabled: Boolean,
    onAutoplayChanged: (Boolean) -> Unit,
    isLooping: Boolean,
    onLoopChanged: (Boolean) -> Unit,
    sleepTimerEndsAt: Long?,
    sleepTimerEndOfVideo: Boolean,
    onSleepTimerClick: () -> Unit,
    onAskVideoClick: () -> Unit,
    abLoopActive: Boolean,
    onPowerToolsClick: () -> Unit,
    showPip: Boolean,
    onPipClick: () -> Unit,
    showComments: Boolean,
    commentsActive: Boolean,
    onCommentsClick: () -> Unit,
    showQueue: Boolean,
    onQueueClick: () -> Unit,
    showTimedComments: Boolean,
    timedCommentsActive: Boolean,
    onTimedCommentsChanged: (Boolean) -> Unit,
    showLiveChat: Boolean,
    liveChatActive: Boolean,
    onLiveChatChanged: (Boolean) -> Unit,
    showVerticalLive: Boolean,
    onVerticalLiveClick: () -> Unit,
    showZoomToFill: Boolean,
    zoomToFillActive: Boolean,
    onZoomToFillChanged: (Boolean) -> Unit,
    showListenAsMusic: Boolean,
    onListenAsMusic: () -> Unit,
    showSmoothMotion: Boolean,
    smoothMotionOn: Boolean,
    onSmoothMotionChanged: (Boolean) -> Unit,
    smoothMotionStatus: String,
    videoShaders: VideoShaderCustomization,
    videoShadersInstalled: Boolean,
    onVideoShadersPreview: (VideoShaderCustomization) -> Unit,
    onVideoShadersChanged: (VideoShaderCustomization) -> Unit,
    speedLabel: (Float) -> String
) {
    // 0: Kalite, 1: Hız, 2: Ses, 3: Görüntü Efektleri
    var deckTab by rememberSaveable { mutableIntStateOf(0) }
    val selectedAudio = remember(audioTracks) { audioTracks.firstOrNull { it.isSelected } }
    val hasSleep = sleepTimerEndsAt != null || sleepTimerEndOfVideo

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Live Telemetry Header Pills
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TelemetryStatusPill(
                icon = Icons.Rounded.HighQuality,
                label = buildString {
                    append(currentQuality?.resolution ?: "Oto")
                    currentQuality?.codec?.takeIf { it.isNotBlank() }?.let { c ->
                        append(" • ")
                        append(c.substringBefore('.').uppercase())
                    }
                },
                highlighted = currentQuality != null
            ) { deckTab = 0 }
            TelemetryStatusPill(
                icon = Icons.Rounded.Speed,
                label = speedLabel(playbackSpeed),
                highlighted = kotlin.math.abs(playbackSpeed - 1f) > 0.02f
            ) { deckTab = 1 }
            if (audioTracks.isNotEmpty()) {
                TelemetryStatusPill(
                    icon = Icons.Rounded.Audiotrack,
                    label = selectedAudio?.label ?: "${audioTracks.size} ses",
                    highlighted = false
                ) { deckTab = 2 }
            }
            if (hasSleep) {
                TelemetryStatusPill(
                    icon = Icons.Rounded.Bedtime,
                    label = "Uyku Aktif",
                    highlighted = true,
                    onClick = onSleepTimerClick
                )
            }
        }

        // Modern Tabbed Deck Selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                0 to "Kalite",
                1 to "Hız",
                2 to "Ses",
                3 to "Görüntü"
            ).forEach { (idx, title) ->
                val selected = deckTab == idx
                val disabled = idx == 2 && audioTracks.isEmpty()
                Surface(
                    onClick = { if (!disabled) deckTab = idx },
                    enabled = !disabled,
                    shape = RoundedCornerShape(SentupRadii.pill),
                    color = when {
                        selected -> MaterialTheme.colorScheme.primary
                        disabled -> MaterialTheme.colorScheme.surfaceContainerLow
                        else -> MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        color = when {
                            selected -> MaterialTheme.colorScheme.onPrimary
                            disabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                            else -> MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 10.dp),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }

        // Active Control Deck Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                when (deckTab) {
                    0 -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Çözünürlük ve Kodek",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            if (qualities.size > 1) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FilterChip(
                                        selected = false,
                                        onClick = { onQualitySelected(qualities.last()) },
                                        label = { Text("Tasarruf") }
                                    )
                                    FilterChip(
                                        selected = false,
                                        onClick = { onQualitySelected(qualities.first()) },
                                        label = { Text("En Yüksek") }
                                    )
                                }
                            }
                        }
                        if (isLoading && qualities.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 2.5.dp)
                            }
                        } else if (qualities.isEmpty()) {
                            Text(
                                text = "Çözünürlük bilgisi henüz hazır değil",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                qualities.forEach { q ->
                                    val selected = currentQuality?.url == q.url && currentQuality.resolution == q.resolution
                                    val codecTag = q.codec?.substringBefore('.')?.uppercase()
                                    Surface(
                                        onClick = { onQualitySelected(q) },
                                        shape = RoundedCornerShape(14.dp),
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.surfaceContainerHighest
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (selected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = q.resolution,
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (!codecTag.isNullOrBlank()) {
                                                Text(
                                                    text = codecTag,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (selected) {
                                                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                                    } else {
                                                        MaterialTheme.colorScheme.onSurfaceVariant
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        Text(
                            text = "Oynatma Hızı (${speedLabel(playbackSpeed)})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        SpeedDeck(
                            playbackSpeed = playbackSpeed,
                            speedLabel = speedLabel,
                            onSpeedPreview = onSpeedPreview,
                            onSpeedSelected = onSpeedSelected
                        )
                    }

                    2 -> {
                        Text(
                            text = "Ses Kanalı Seçimi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            audioTracks.forEach { track ->
                                Surface(
                                    onClick = { onAudioTrackSelected(track) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (track.isSelected) {
                                        MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHighest
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (track.isSelected) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                                            contentDescription = null,
                                            tint = if (track.isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(track.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                            track.detail?.takeIf { it.isNotBlank() }?.let { d ->
                                                Text(d, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        Text(
                            text = "Video Görüntü & Shader Efektleri",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        ShaderDeck(
                            settings = videoShaders,
                            installed = videoShadersInstalled,
                            onPreview = onVideoShadersPreview,
                            onChange = onVideoShadersChanged
                        )
                    }
                }
            }
        }

        // Category 1: Playback Behavior & Tools Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "Oynatma & Araçlar",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                if (showEndBehavior) {
                    SettingsToggleRow(
                        icon = Icons.Rounded.PlayArrow,
                        title = stringResource(R.string.vpc_autoplay),
                        subtitle = "Sonraki videoya otomatik geç",
                        checked = autoplayEnabled,
                        onCheckedChange = onAutoplayChanged
                    )
                    SettingsToggleRow(
                        icon = Icons.Rounded.RepeatOne,
                        title = stringResource(R.string.vpc_loop),
                        subtitle = "Bu videoyu sürekli tekrarla",
                        checked = isLooping,
                        onCheckedChange = onLoopChanged
                    )
                }
                SettingsActionRow(
                    icon = Icons.Rounded.Bedtime,
                    title = stringResource(R.string.sleep_timer_title),
                    subtitle = if (hasSleep) "Uyku zamanlayıcı aktif" else "Otomatik durdurma ayarla",
                    highlighted = hasSleep,
                    onClick = onSleepTimerClick
                )
                SettingsActionRow(
                    icon = Icons.Rounded.Bookmarks,
                    title = "A-B Döngü, Notlar & Odak",
                    subtitle = if (abLoopActive) "A-B döngüsü şu an aktif" else "Bölüm tekrarı ve zaman damgalı notlar",
                    highlighted = abLoopActive,
                    onClick = onPowerToolsClick
                )
            }
        }

        // Category 2: Visual & Smart Assistants Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                Text(
                    text = "Görünüm & Akıllı Araçlar",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
                if (showZoomToFill) {
                    SettingsToggleRow(
                        icon = Icons.Rounded.ZoomIn,
                        title = stringResource(R.string.vpc_zoom_to_fill),
                        subtitle = "Videoyu tam ekrana sığdır",
                        checked = zoomToFillActive,
                        onCheckedChange = onZoomToFillChanged
                    )
                }
                if (showSmoothMotion) {
                    SettingsToggleRow(
                        icon = Icons.Rounded.Animation,
                        title = stringResource(R.string.vpc_smooth_motion),
                        subtitle = smoothMotionStatus,
                        checked = smoothMotionOn,
                        onCheckedChange = onSmoothMotionChanged
                    )
                }
                if (showTimedComments) {
                    SettingsToggleRow(
                        icon = Icons.AutoMirrored.Rounded.Comment,
                        title = stringResource(R.string.sp_timed_comments),
                        subtitle = "Video anlarına bağlı canlı yorum akışı",
                        checked = timedCommentsActive,
                        onCheckedChange = onTimedCommentsChanged
                    )
                }
                if (showLiveChat) {
                    SettingsToggleRow(
                        icon = Icons.AutoMirrored.Rounded.Chat,
                        title = stringResource(R.string.vp_live_chat),
                        subtitle = "Canlı yayın sohbet penceresi",
                        checked = liveChatActive,
                        onCheckedChange = onLiveChatChanged
                    )
                }
                SettingsActionRow(
                    icon = Icons.Rounded.QuestionAnswer,
                    title = "Videoya Soru Sor (AI)",
                    subtitle = "Altyazı ve bölümlerden anında özet ve cevap al",
                    highlighted = true,
                    onClick = onAskVideoClick
                )
            }
        }

        // Category 3: Quick Surface Shortcuts
        val hasAnyShortcut = showPip || showQueue || showComments || showListenAsMusic || showVerticalLive
        if (hasAnyShortcut) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (showPip) {
                    FilledTonalButton(onClick = onPipClick, shape = RoundedCornerShape(SentupRadii.pill)) {
                        Icon(Icons.Rounded.PictureInPictureAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.vpc_pip))
                    }
                }
                if (showQueue) {
                    FilledTonalButton(onClick = onQueueClick, shape = RoundedCornerShape(SentupRadii.pill)) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.swipe_action_queue))
                    }
                }
                if (showComments) {
                    FilledTonalButton(onClick = onCommentsClick, shape = RoundedCornerShape(SentupRadii.pill)) {
                        Icon(Icons.AutoMirrored.Rounded.Comment, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (commentsActive) stringResource(R.string.cd_close_comments) else stringResource(R.string.cd_comments))
                    }
                }
                if (showListenAsMusic) {
                    FilledTonalButton(onClick = onListenAsMusic, shape = RoundedCornerShape(SentupRadii.pill)) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.vpc_listen_as_music))
                    }
                }
                if (showVerticalLive) {
                    FilledTonalButton(onClick = onVerticalLiveClick, shape = RoundedCornerShape(SentupRadii.pill)) {
                        Icon(Icons.Rounded.StayCurrentPortrait, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(stringResource(R.string.vpc_vertical_live))
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryStatusPill(
    icon: ImageVector,
    label: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(SentupRadii.pill),
        color = if (highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHighest
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (highlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (checked) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(
                    if (highlighted) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHighest
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
