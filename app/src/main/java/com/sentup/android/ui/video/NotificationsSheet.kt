package com.sentup.android.ui.video

import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.sentup.android.R

import android.text.format.DateUtils
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sentup.android.data.NotificationItem
import com.sentup.android.data.NotificationTarget
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupSpecularEdge

/** What a notification is about, which is also how the inbox is filtered. */
private enum class NotificationKind { VIDEO, SHORT, POST, OTHER }

private val NotificationItem.kind: NotificationKind
    get() = when (target) {
        is NotificationTarget.Video -> NotificationKind.VIDEO
        is NotificationTarget.Short -> NotificationKind.SHORT
        is NotificationTarget.Post -> NotificationKind.POST
        is NotificationTarget.Link, null -> NotificationKind.OTHER
    }

/**
 * The notification inbox: what YouTube currently lists, and under it what this
 * device remembers from before YouTube dropped it.
 *
 * YouTube's own inbox only reaches a little way back, so the sheet has three
 * parts - New, Earlier, and Kept on this device -
 * and a row of kind filters, because a week of history is mostly Shorts
 * uploads with the one post somebody was looking for somewhere among them.
 *
 * A swipe hides a notification on this device only; the footer brings the
 * hidden ones back, which is the undo. A tap opens the item's
 * [NotificationTarget]: a video in the player, a Short in the Shorts player, a
 * post over its comments, anything else as its link.
 *
 * The list is one inset card with the three sections headed inside it, rather
 * than three loose columns of full-bleed rows: an inbox is a single object you
 * scroll through, and the section headers are the only thing that should say
 * where you are. The filters are capsules for the same reason they are capsules
 * on the comment sheet - choosing which of a few is not a filter over a set,
 * and a chip row's connected bar is a lot of machinery for it.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NotificationsSheet(
    notifications: List<NotificationItem>,
    isLoading: Boolean,
    hiddenCount: Int,
    onNotificationClick: (NotificationItem) -> Unit,
    onHide: (NotificationItem) -> Unit,
    onRestoreHidden: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    // Null is "All". Saved by name, so a rotation keeps the filter.
    var filterName by rememberSaveable { mutableStateOf<String?>(null) }
    val counts = remember(notifications) { notifications.groupingBy { it.kind }.eachCount() }
    // A filter whose last item was just hidden falls back to All rather than
    // leaving an empty list under a chip that is no longer drawn.
    val filter = NotificationKind.entries.firstOrNull { it.name == filterName && it in counts }
    val shown = remember(notifications, filter) {
        if (filter == null) notifications else notifications.filter { it.kind == filter }
    }
    val unread = notifications.count { !it.isRead && !it.remembered }

    VideoSheetScaffold(
        onDismiss = onDismiss,
        sheetState = sheetState,
        title = stringResource(R.string.settings_notifications),
        // The unread count belongs in the bar rather than in a badge floating
        // beside a headline: it is the one number a user opens this sheet for,
        // and a bar can hold it without covering anything.
        subtitle = if (unread > 0) stringResource(R.string.ns_new_count, unread) else null,
        trailing = {
            // The first load has the big indicator below; this one is for
            // a refresh over a list that is already showing.
            if (isLoading && notifications.isNotEmpty()) {
                Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                    LoadingIndicator(modifier = Modifier.size(24.dp))
                }
            } else {
                IconButton(onClick = onRefresh, enabled = !isLoading) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = stringResource(R.string.ns_refresh),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    ) {
        // Only worth drawing once there is more than one kind to choose between.
        if (counts.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = SentupGroup.inset)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                VideoSegmentPill(
                    label = stringResource(R.string.ns_filter_all),
                    trailing = notifications.size.toString(),
                    selected = filter == null,
                    onClick = { filterName = null }
                )
                NotificationKind.entries.forEach { kind ->
                    val count = counts[kind] ?: return@forEach
                    VideoSegmentPill(
                        label = stringResource(kind.label),
                        trailing = count.toString(),
                        selected = filter == kind,
                        onClick = { filterName = kind.name }
                    )
                }
            }
        }

        when {
            isLoading && notifications.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    LoadingIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            notifications.isEmpty() -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    VideoSheetEmptyState(
                        title = stringResource(R.string.ns_none_yet),
                        icon = Icons.Rounded.NotificationsNone,
                        // Everything hidden is not the same as nothing arriving,
                        // so the way back is offered here rather than after
                        // an explanation.
                        action = if (hiddenCount > 0) {
                            {
                                HiddenNotificationsButton(hiddenCount, onRestoreHidden)
                            }
                        } else null
                    )
                }
            }
            else -> {
                val fresh = shown.filterNot { it.remembered }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = SentupGroup.inset)
                        .sentupCard(
                            shape = RoundedCornerShape(SentupRadii.card),
                            elevation = 0,
                            tinted = false
                        )
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 6.dp)
                    ) {
                        notificationSection(
                            title = R.string.ns_section_new,
                            rows = fresh.filterNot { it.isRead },
                            onClick = onNotificationClick,
                            onHide = onHide
                        )
                        notificationSection(
                            title = R.string.ns_section_earlier,
                            rows = fresh.filter { it.isRead },
                            onClick = onNotificationClick,
                            onHide = onHide
                        )
                        notificationSection(
                            title = R.string.ns_section_kept,
                            note = R.string.ns_section_kept_note,
                            rows = shown.filter { it.remembered },
                            onClick = onNotificationClick,
                            onHide = onHide
                        )
                        if (hiddenCount > 0) {
                            item(key = "hidden_footer", contentType = "footer") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    HiddenNotificationsButton(hiddenCount, onRestoreHidden)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val NotificationKind.label: Int
    get() = when (this) {
        NotificationKind.VIDEO -> R.string.ns_filter_videos
        NotificationKind.SHORT -> R.string.ns_filter_shorts
        NotificationKind.POST -> R.string.ns_filter_posts
        NotificationKind.OTHER -> R.string.ns_filter_other
    }

private val NotificationKind.icon: ImageVector
    get() = when (this) {
        NotificationKind.VIDEO -> Icons.Rounded.PlayArrow
        NotificationKind.SHORT -> Icons.Rounded.Bolt
        NotificationKind.POST -> Icons.Rounded.Forum
        NotificationKind.OTHER -> Icons.Rounded.Person
    }

/**
 * One headed run of notifications; draws nothing at all when it has none.
 *
 * The header is [VideoCardHeader] rather than the page-level group header: the
 * three sections share one card, so their headers have already been inset by
 * it, and repeating that padding would step them a full row-indent to the right
 * of the messages they label.
 */
private fun LazyListScope.notificationSection(
    title: Int,
    rows: List<NotificationItem>,
    onClick: (NotificationItem) -> Unit,
    onHide: (NotificationItem) -> Unit,
    note: Int? = null,
) {
    if (rows.isEmpty()) return
    item(key = "header_$title", contentType = "header") {
        VideoCardHeader(
            text = stringResource(title),
            subtitle = note?.let { stringResource(it) }
        )
    }
    // An item with no id cannot be hidden or told apart, so it is keyed by
    // where it sits; every other row keeps its identity through a removal.
    items(
        items = rows,
        key = { item -> item.id.ifBlank { "anon_${item.message.hashCode()}_${item.sentTime}" } },
        contentType = { _ -> "notification" }
    ) { notification ->
        HideableNotificationRow(
            notification = notification,
            onClick = { onClick(notification) },
            onHide = { onHide(notification) },
            // The last row of a run carries no hairline: a divider under the
            // final item of a section reads as the start of another one.
            showDivider = notification != rows.last(),
            modifier = Modifier.animateItem()
        )
    }
}

/**
 * One kind of notification, as a capsule with its count riding quietly behind
 * the label.
 *
 * A `FilterChip` was the wrong control here. Chips are for filtering
 * *results*, they carry the count in the same weight as the name - so the
 * number, which is the thing somebody scanning a busy inbox actually wants,
 * was the loudest thing on the control - and the selected one changed colour
 * rather than becoming obviously the selected one. In a capsule, filled is
 * chosen, which answers "what am I looking at" without a colour change to
 * notice, and the count drops back to a whisper.
 */
@Composable
private fun NotificationFilterPill(
    label: String,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val haptics = com.sentup.android.util.rememberSentupHaptics()
    VideoSegmentPill(
        label = label,
        selected = selected,
        trailing = count.toString(),
        onClick = {
            if (!selected) haptics.tick()
            onClick()
        }
    )
}

/**
 * The way back to what was hidden, as a capsule rather than a text button.
 *
 * It is the only other thing on the sheet that does anything, and giving it the
 * same shape as the sheet's other controls is what makes it read as a button
 * rather than as a caption about the list above it.
 */
@Composable
private fun HiddenNotificationsButton(count: Int, onRestore: () -> Unit) {
    VideoSheetTonalButton(
        label = pluralStringResource(R.plurals.ns_hidden_restore, count, count),
        icon = Icons.Rounded.VisibilityOff,
        onClick = onRestore
    )
}

/**
 * A row that a swipe towards the start hides. One with no id is drawn but stays
 * put - there is nothing to take away, and a row that cannot be dismissed
 * should not pretend otherwise.
 *
 * The resting row is opaque and clipped to the tile radius, which is what keeps
 * the "Hide" strip from showing through a row at rest and what makes the swipe
 * leave the card rather than cutting a rectangle out of it.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HideableNotificationRow(
    notification: NotificationItem,
    onClick: () -> Unit,
    onHide: () -> Unit,
    /** Inset to the text, never edge to edge - see [SentupDivider]. */
    showDivider: Boolean,
    modifier: Modifier = Modifier
) {
    val tile = RoundedCornerShape(SentupRadii.tile)
    val resting = MaterialTheme.colorScheme.surfaceContainerHigh
    Column(modifier = modifier) {
        if (notification.id.isBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(resting),
                contentAlignment = Alignment.CenterStart
            ) {
                NotificationRow(notification, onClick)
            }
            if (showDivider) SentupDivider()
            return@Column
        }
        val haptics = com.sentup.android.util.rememberSentupHaptics()
        val hide by rememberUpdatedState(onHide)
        val state = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value == SwipeToDismissBoxValue.EndToStart) {
                    haptics.confirm()
                    hide()
                    true
                } else {
                    false
                }
            }
        )
        // [scar] A lazy list keeps each key's saved state after its row leaves,
        // and the swipe state is saved state: a notification brought back by
        // "show them again" came back still swiped away, as an empty "Hide"
        // strip that stayed. A row that is composed is by definition not
        // hidden, so it starts settled whatever was saved for it.
        LaunchedEffect(notification.id) {
            if (state.currentValue != SwipeToDismissBoxValue.Settled) {
                state.snapTo(SwipeToDismissBoxValue.Settled)
            }
        }
        SwipeToDismissBox(
            state = state,
            enableDismissFromStartToEnd = false,
            backgroundContent = {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(tile)
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(horizontal = 20.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.ns_hide),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        ) {
            // Opaque, or the "Hide" behind it shows through a resting row.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(tile)
                    .background(resting)
            ) {
                NotificationRow(notification, onClick)
            }
        }
        if (showDivider) SentupDivider()
    }
}

/**
 * One notification: unread, what kind of thing it is, who it is from, what it
 * says, when - and the video's own frame beside it when there is one.
 *
 * The unread marker is a dot rather than a tinted row. Tinting the row too
 * would be two signals for one fact, and what would survive longest is a field
 * of pale rectangles rather than a count of unread - which is the only reason
 * somebody opens an inbox. The kind rides the avatar's corner because YouTube
 * says it only in the wording of the message, and six rows of prose can hide
 * one post behind five uploads.
 */
@Composable
private fun NotificationRow(
    notification: NotificationItem,
    onClick: () -> Unit
) {
    val unread = !notification.isRead && !notification.remembered
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .videoPressTarget(onClick = onClick, enabled = notification.target != null)
            .padding(horizontal = SentupGroup.dividerInset, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Unread dot
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (unread) scheme.primary else Color.Transparent)
        )

        Spacer(Modifier.width(12.dp))

        // Channel avatar, with what the notification is about in its corner:
        // the kind is otherwise only in the wording, which is not scannable.
        Box(modifier = Modifier.size(42.dp)) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(scheme.surfaceContainerHighest)
            ) {
                if (notification.channelAvatarUrl != null) {
                    AsyncImage(
                        model = notification.channelAvatarUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Notifications,
                        contentDescription = null,
                        tint = scheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(20.dp)
                    )
                }
            }
            // The ring is the card's own tone rather than a hard-coded colour:
            // the badge sits on top of an image, and without a ring it
            // disappears into whichever frame it lands on.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(scheme.primaryContainer)
                    .border(2.dp, scheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = notification.kind.icon,
                    contentDescription = null,
                    tint = scheme.onPrimaryContainer,
                    modifier = Modifier.size(12.dp)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = notification.message,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (unread) FontWeight.SemiBold else FontWeight.Normal,
                color = scheme.onSurface,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            // YouTube's own "3 hours ago" is only true while YouTube is still
            // listing the item; a remembered one is timed from when it was seen.
            val time = if (notification.remembered && notification.firstSeenMs > 0L) {
                remember(notification.firstSeenMs) {
                    DateUtils.getRelativeTimeSpanString(
                        notification.firstSeenMs,
                        System.currentTimeMillis(),
                        DateUtils.MINUTE_IN_MILLIS,
                        DateUtils.FORMAT_ABBREV_RELATIVE
                    ).toString()
                }
            } else {
                notification.sentTime
            }
            if (time.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant
                )
            }
        }

        if (notification.videoThumbnailUrl != null) {
            VideoSheetThumbnail(
                url = notification.videoThumbnailUrl,
                modifier = Modifier.width(88.dp),
                height = 50.dp
            )
        }
    }
}
