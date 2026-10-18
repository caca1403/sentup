package com.sentup.android.ui.video

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sentup.android.R
import com.sentup.android.data.VideoItem
import com.sentup.android.ui.components.VideoThumbnail
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii

/**
 * Presentation only: VideoCard owns the same playback and channel actions in either layout.
 *
 * The row is a line inside the group card its neighbours share, so it carries
 * the hairline under itself (inset, never full-bleed) and none of the rounding:
 * [LocalVideoRowEdge] says where the run ends and the card's surface is drawn
 * round there. Standing alone - anywhere outside the Home feed's chained list -
 * it is simply a card of one row.
 */
@Composable
internal fun CompactVideoCardContent(
    video: VideoItem,
    openChannel: (() -> Unit)?,
    onMoreClick: (() -> Unit)?
) {
    val edge = LocalVideoRowEdge.current
    val textColor = MaterialTheme.colorScheme.onSurface
    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = SentupGroup.dividerInset,
                    end = 4.dp,
                    top = 12.dp,
                    bottom = 12.dp
                ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(0.36f)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(SentupRadii.tile))
            ) {
                VideoThumbnail(
                    video = video,
                    modifier = Modifier.fillMaxSize(),
                    indicatorSize = 26.dp
                )
                // Inside the cover's own edge, so it cannot reach the text.
                VideoDurationBadge(
                    video = video,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(5.dp))
                // The small avatar rides on the meta line it belongs to, so the
                // row reads as one object instead of a picture and a column.
                Row(
                    modifier = Modifier.videoChannelTap(openChannel),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp)
                ) {
                    VideoChannelAvatar(video = video, openChannel = null, size = 20.dp)
                    Text(
                        text = videoFeedSubtitle(video),
                        style = MaterialTheme.typography.labelMedium,
                        color = secondaryColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 4.dp)
                    )
                }
            }
            if (onMoreClick != null) {
                IconButton(onClick = onMoreClick) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.cd_more_options)
                    )
                }
            }
        }
        if (edge == VideoRowEdge.Top || edge == VideoRowEdge.Middle) {
            SentupDivider()
        }
    }
}
