package com.sentup.android.ui.shorts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.text.HtmlCompat
import coil.compose.AsyncImage
import com.sentup.android.R
import com.sentup.android.data.RichText
import com.sentup.android.data.VideoItem
import com.sentup.android.ui.components.rememberLinkedText
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.video.VideoSheetScaffold

/**
 * A Short's own page: full title, who made it, how many people watched it,
 * when it went up, and the description with its links live.
 *
 * **None of this cost a request.** `ShortsPlayerViewModel` already runs one
 * `/next` per Short for the like rail and the real title, and that response
 * fills in `description`, `descriptionLinks`, `viewCount`, `uploadedDate` and
 * `subscriberCount` on the same [VideoItem]. The overlay was reading four
 * fields off it and dropping the rest, so a Short was the one place in Sentüp
 * where you could not find out when something was posted.
 *
 * **A sheet rather than an expanding overlay.** The overlay is drawn over
 * video, where every extra line costs contrast and covers the picture; a
 * description runs to any length and carries links that want a real tap
 * target. It reads on a surface, not on someone's face.
 *
 * On the new page language the title and stats sit on the page itself and the
 * two things you act on - the channel and the text with its links - are each
 * one card, so the sheet reads as a title and two objects rather than a wall.
 *
 * Playback deliberately keeps running underneath, matching the comments sheet:
 * a Short is under a minute, and pausing it to read about it is the one thing
 * nobody wants.
 */
@Composable
fun ShortsDescriptionSheet(
    video: VideoItem,
    onSeekTo: (Long) -> Unit,
    onOpenChannel: ((String) -> Unit)?,
    onDismiss: () -> Unit
) {
    // YouTube's link offsets are measured against the raw attributed text, so
    // it must not be rewritten when we have them. Only descriptions with no
    // links can have come from a source that leaves HTML entities in.
    val description = remember(video.description, video.descriptionLinks) {
        when {
            video.description.isNullOrBlank() -> ""
            video.descriptionLinks.isNotEmpty() -> video.description
            else -> HtmlCompat
                .fromHtml(video.description, HtmlCompat.FROM_HTML_MODE_LEGACY)
                .toString()
                .trim()
        }
    }
    val linkedDescription = rememberLinkedText(
        rich = RichText(description, video.descriptionLinks),
        onTimestampClick = { seconds -> onSeekTo(seconds * 1000L) }
    )

    VideoSheetScaffold(onDismiss = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // The description is the one thing here with no length limit,
                // so the whole sheet scrolls rather than the text pane alone -
                // a bottom sheet that does not scroll clips its overflow with
                // nothing logged.
                .verticalScroll(rememberScrollState())
        ) {
            SelectionContainer {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = SentupGroup.inset)
                )
            }

            val stats = shortsStatsLine(video)
            if (stats != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = stats,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = SentupGroup.inset)
                )
            }

            if (video.channelName.isNotBlank()) {
                Spacer(Modifier.height(16.dp))
                val channelId = video.channelId?.takeIf { it.startsWith("UC") }
                val clickable = onOpenChannel != null && channelId != null
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SentupGroup.inset)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sentupCard(
                                shape = RoundedCornerShape(SentupRadii.card),
                                elevation = 0,
                                tinted = false
                            )
                            .then(
                                if (clickable) {
                                    Modifier.clickable { onOpenChannel?.invoke(channelId!!) }
                                } else {
                                    Modifier
                                }
                            )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!video.channelIconUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = video.channelIconUrl,
                                        contentDescription = null,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Rounded.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = video.channelName,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                video.subscriberCount?.takeIf { it.isNotBlank() }?.let { subs ->
                                    Text(
                                        text = subs,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = SentupGroup.inset,
                        vertical = SentupGroup.gap
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .sentupCard(
                            shape = RoundedCornerShape(SentupRadii.card),
                            elevation = 0,
                            tinted = false
                        )
                ) {
                    if (description.isNotBlank()) {
                        SelectionContainer {
                            Text(
                                text = linkedDescription,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    } else {
                        // Said plainly rather than left blank: an empty pane
                        // reads as something that failed to load, and a Short
                        // with no description is the ordinary case rather
                        // than an error.
                        Text(
                            text = stringResource(R.string.sp_no_description),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * "1.2M views - 2 days ago", with whichever halves the response actually
 * carried. Phase 2 fills these in a moment after the Short opens, so both can
 * legitimately be missing for a frame and the line is simply absent until then
 * rather than showing a placeholder that shifts the layout when it resolves.
 */
internal fun shortsStatsLine(video: VideoItem): String? {
    val parts = listOfNotNull(
        video.viewCount.takeIf { it.isNotBlank() },
        video.uploadedDate?.takeIf { it.isNotBlank() }
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString("  •  ")
}
