package com.sentup.android.ui.video

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.ModeComment
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sentup.android.R
import com.sentup.android.data.CommentItem
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage

/** Which end of the thread the list is showing, when the user has asked. */
private enum class CommentOrder { TOP, NEW }

/**
 * The comments on a video: a capsule bar that names the thread, a two-capsule
 * order strip, one inset card of comment rows, and a composer pinned below.
 *
 * The old panel was a `surfaceContainer` slab with a `headlineSmall` title
 * floating on it, comments separated by 20dp of nothing, and "Reply",
 * "View replies" and "Delete" as bare bold labels that were tappable only in
 * the few pixels their glyphs occupied. YouTube's own is the same idea with
 * more chrome: a headline, a chip row, avatars in a stack, and "Reply" as an
 * outlined pill on the right of every comment.
 *
 * What is here instead: the title lives in a floating pill like every other
 * surface in the app; the thread is one card with hairlines inset to the text;
 * the actions are small capsules that press inward; and a reply is drawn as a
 * nested block under its parent with a rail, which is what makes a thread
 * readable without counting avatars.
 *
 * The order strip is [CommentOrder] and it reorders what has already been
 * fetched - no second request, no new endpoint, and the token stream that fills
 * the list is untouched. Top is the order YouTube itself returned.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CommentsPanel(
    comments: List<CommentItem>,
    replies: Map<String, List<CommentItem>>,
    loadingReplyIds: Set<String>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    commentsAvailable: Boolean,
    canComment: Boolean,
    isPosting: Boolean,
    onLoadMore: () -> Unit,
    onLoadReplies: (CommentItem) -> Unit,
    onPostComment: (String) -> Unit,
    onPostReply: (CommentItem, CommentItem, String) -> Unit,
    onLikeComment: (CommentItem) -> Unit,
    onDeleteComment: (CommentItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    /**
     * Seek the player, in seconds, when a comment's timestamp is tapped.
     * Null (the Shorts sheet) renders timestamps as ordinary text.
     */
    onSeekTo: ((seconds: Long) -> Unit)? = null,
    /** Open a commenter's channel (their avatar or name). Null leaves them inert. */
    onOpenAuthor: ((channelId: String) -> Unit)? = null,
    /**
     * What the thread is about, pinned under the title. A community post's
     * sheet shows the post here; the players leave it null because the video
     * is already on screen above the panel.
     */
    header: (@Composable () -> Unit)? = null,
    /** Shown when the thread could not be opened at all. */
    unavailableMessage: String = "Comments are unavailable for this video"
) {
    val listState = rememberLazyListState()

    // Trigger pagination when the user nears the end of the list
    val shouldLoadMore by remember {
        derivedStateOf {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 5
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) onLoadMore()
    }

    // The comment being replied to; null = composing a top-level comment.
    // When replying to a reply, threadParent is the enclosing top-level
    // comment (the reply posts into that thread, YouTube-style).
    var replyTarget by remember { mutableStateOf<CommentItem?>(null) }
    var replyThreadParent by remember { mutableStateOf<CommentItem?>(null) }

    // Own comment awaiting delete confirmation
    var commentPendingDelete by remember { mutableStateOf<CommentItem?>(null) }

    // The order the thread is shown in. Saved, so a rotation does not silently
    // put back the one the user just rearranged.
    var order by rememberSaveable { mutableStateOf(CommentOrder.TOP) }

    // Reorders what has already loaded; the pinned ones stay at the top under
    // either choice, because a pin is the creator speaking, not a ranking.
    val ordered = remember(comments, order) {
        when (order) {
            CommentOrder.TOP -> comments
            CommentOrder.NEW -> comments.sortedWith(
                compareByDescending<CommentItem> { it.isPinned }
                    .thenBy { it.publishedTime.timeAgoMillis() ?: Long.MAX_VALUE }
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(topStart = SentupRadii.panel, topEnd = SentupRadii.panel),
        // The page tone paints itself below; a fill here would cover it.
        color = androidx.compose.ui.graphics.Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .sentupPage()
                .imePadding()
        ) {
            VideoSheetTitleBar(
                title = stringResource(R.string.cd_comments),
                subtitle = if (comments.isEmpty()) {
                    null
                } else {
                    pluralStringResource(R.plurals.n_comments, comments.size, comments.size)
                },
                onClose = onDismiss
            )

            header?.let {
                Box(
                    modifier = Modifier
                        .padding(horizontal = SentupGroup.inset)
                        .padding(bottom = 12.dp)
                ) {
                    it()
                }
            }

            // Two capsules, not a segmented control and not chips: a chip row
            // reads as a filter over a set, and this is a decision about one
            // list. Only worth the row when there is more than one comment to
            // arrange - below that the strip is a control for nothing.
            if (comments.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = SentupGroup.inset)
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VideoSegmentPill(
                        label = stringResource(R.string.cm_sort_top),
                        selected = order == CommentOrder.TOP,
                        onClick = { order = CommentOrder.TOP }
                    )
                    VideoSegmentPill(
                        label = stringResource(R.string.cm_sort_new),
                        selected = order == CommentOrder.NEW,
                        onClick = { order = CommentOrder.NEW }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = SentupGroup.inset)
                    .sentupCard(
                        shape = RoundedCornerShape(SentupRadii.card),
                        elevation = 0,
                        tinted = false
                    )
            ) {
                when {
                    isLoading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LoadingIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    comments.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            VideoSheetEmptyState(
                                title = if (commentsAvailable) {
                                    stringResource(R.string.cm_no_comments)
                                } else {
                                    unavailableMessage
                                },
                                icon = Icons.Rounded.ModeComment
                            )
                        }
                    }
                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                vertical = 6.dp
                            )
                        ) {
                            items(ordered.size, key = { ordered[it].commentId }) { index ->
                                val comment = ordered[index]
                                Column {
                                    CommentRow(
                                        comment = comment,
                                        onLikeClick = { onLikeComment(comment) },
                                        onDeleteClick = if (comment.deleteParams != null) {
                                            { commentPendingDelete = comment }
                                        } else null,
                                        onSeekTo = onSeekTo,
                                        onOpenAuthor = onOpenAuthor
                                    )

                                    // Reply affordance (needs login + reply params)
                                    if (canComment && comment.replyParams != null) {
                                        CommentActionPill(
                                            text = stringResource(R.string.cm_reply),
                                            icon = Icons.AutoMirrored.Rounded.Send,
                                            modifier = Modifier.padding(start = 46.dp, top = 2.dp),
                                            onClick = {
                                                replyTarget = comment
                                                replyThreadParent = comment
                                            }
                                        )
                                    }

                                    // Replies
                                    val commentReplies = replies[comment.commentId]
                                    val isLoadingReplies = comment.commentId in loadingReplyIds
                                    if (comment.repliesToken != null && commentReplies == null) {
                                        CommentActionPill(
                                            text = when {
                                                isLoadingReplies ->
                                                    stringResource(R.string.cm_loading_replies)
                                                comment.replyCount.isNotEmpty() ->
                                                    stringResource(
                                                        R.string.cm_view_replies_count,
                                                        comment.replyCount
                                                    )
                                                else -> stringResource(R.string.cm_view_replies)
                                            },
                                            icon = Icons.Rounded.ModeComment,
                                            modifier = Modifier.padding(start = 46.dp, top = 2.dp),
                                            enabled = !isLoadingReplies,
                                            onClick = { onLoadReplies(comment) }
                                        )
                                    }
                                    if (!commentReplies.isNullOrEmpty()) {
                                        // The rail is what makes a reply read as
                                        // belonging to the comment above it rather
                                        // than as another comment that happens to be
                                        // indented - the same cue iOS uses in Mail.
                                        Row(
                                            modifier = Modifier
                                                .padding(start = 46.dp, end = 12.dp, top = 10.dp)
                                                .height(IntrinsicSize.Min)
                                        ) {
                                            // The rail is what makes a reply read as
                                            // belonging to the comment above it rather than
                                            // as another comment that happens to be
                                            // indented - the cue iOS uses in Mail.
                                            Spacer(
                                                modifier = Modifier
                                                    .width(2.dp)
                                                    .fillMaxHeight()
                                                    .background(
                                                        MaterialTheme.colorScheme.onSurface.copy(
                                                            alpha = 0.10f
                                                        )
                                                    )
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(
                                                modifier = Modifier.weight(1f),
                                                verticalArrangement = Arrangement.spacedBy(14.dp)
                                            ) {
                                                commentReplies.forEach { reply ->
                                                    Column {
                                                        CommentRow(
                                                            comment = reply,
                                                            isReply = true,
                                                            onLikeClick = { onLikeComment(reply) },
                                                            onDeleteClick = if (reply.deleteParams != null) {
                                                                { commentPendingDelete = reply }
                                                            } else null,
                                                            onSeekTo = onSeekTo,
                                                            onOpenAuthor = onOpenAuthor
                                                        )
                                                        // Replying to a reply posts into
                                                        // the same thread, addressed to
                                                        // its author
                                                        if (canComment && reply.replyParams != null) {
                                                            CommentActionPill(
                                                                text = stringResource(R.string.cm_reply),
                                                                icon = Icons.AutoMirrored.Rounded.Send,
                                                                modifier = Modifier.padding(start = 36.dp, top = 2.dp),
                                                                onClick = {
                                                                    replyTarget = reply
                                                                    replyThreadParent = comment
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                if (index < ordered.lastIndex) SentupDivider()
                            }

                            if (isLoadingMore) {
                                item(key = "loading-more") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        LoadingIndicator(
                                            modifier = Modifier.size(28.dp),
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            commentPendingDelete?.let { pending ->
                AlertDialog(
                    onDismissRequest = { commentPendingDelete = null },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(32.dp),
                    title = { Text(stringResource(R.string.cm_delete_q)) },
                    text = { Text(stringResource(R.string.cm_delete_body)) },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onDeleteComment(pending)
                                commentPendingDelete = null
                            }
                        ) {
                            Text(
                                stringResource(R.string.action_delete),
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { commentPendingDelete = null }) {
                            Text(stringResource(R.string.action_cancel))
                        }
                    }
                )
            }

            if (canComment) {
                CommentComposer(
                    replyTarget = replyTarget,
                    isPosting = isPosting,
                    onCancelReply = {
                        replyTarget = null
                        replyThreadParent = null
                    },
                    onSend = { text ->
                        val target = replyTarget
                        val thread = replyThreadParent ?: target
                        if (target != null && thread != null) {
                            onPostReply(target, thread, text)
                        } else {
                            onPostComment(text)
                        }
                        replyTarget = null
                        replyThreadParent = null
                    }
                )
            }
        }
    }
}

/**
 * Modal bottom sheet wrapper around [CommentsPanel], used by the Shorts
 * player and by community posts. For Shorts it is capped below full height so
 * the short stays visible and playing above the sheet, YouTube Shorts-style -
 * swiping up cannot expand it to cover the whole screen. A post has nothing
 * playing behind it, so it passes a taller [heightFraction] and a [header]
 * showing the post. The regular video player hosts [CommentsPanel] inline
 * below the video instead.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentsSheet(
    comments: List<CommentItem>,
    replies: Map<String, List<CommentItem>>,
    loadingReplyIds: Set<String>,
    isLoading: Boolean,
    isLoadingMore: Boolean,
    commentsAvailable: Boolean,
    canComment: Boolean,
    isPosting: Boolean,
    onLoadMore: () -> Unit,
    onLoadReplies: (CommentItem) -> Unit,
    onPostComment: (String) -> Unit,
    onPostReply: (CommentItem, CommentItem, String) -> Unit,
    onLikeComment: (CommentItem) -> Unit,
    onDeleteComment: (CommentItem) -> Unit,
    onDismiss: () -> Unit,
    onOpenAuthor: ((channelId: String) -> Unit)? = null,
    heightFraction: Float = 0.65f,
    header: (@Composable () -> Unit)? = null,
    unavailableMessage: String = "Comments are unavailable for this video"
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        // A tall sheet opens at its full height; a half-open stop on a sheet
        // that is already most of the screen is one more drag for nothing.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = heightFraction > 0.65f),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        CommentsPanel(
            comments = comments,
            replies = replies,
            loadingReplyIds = loadingReplyIds,
            isLoading = isLoading,
            isLoadingMore = isLoadingMore,
            commentsAvailable = commentsAvailable,
            canComment = canComment,
            isPosting = isPosting,
            onLoadMore = onLoadMore,
            onLoadReplies = onLoadReplies,
            onPostComment = onPostComment,
            onPostReply = onPostReply,
            onLikeComment = onLikeComment,
            onDeleteComment = onDeleteComment,
            onDismiss = onDismiss,
            onOpenAuthor = onOpenAuthor,
            header = header,
            unavailableMessage = unavailableMessage,
            modifier = Modifier
                .fillMaxHeight(heightFraction)
                .navigationBarsPadding()
        )
    }
}

/**
 * Input row pinned under the comments list, on its own floating capsule, and
 * a "Replying to" banner above it when a reply target is active.
 *
 * The field is borderless *because* it sits in a capsule: a second outline
 * inside a pill is two borders around one control. The send button is the only
 * filled circle here, and it fills only once there is something to send -
 * which is what keeps an empty composer from looking like it is posting.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun CommentComposer(
    replyTarget: CommentItem?,
    isPosting: Boolean,
    onCancelReply: () -> Unit,
    onSend: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // No focus on sheet open: the keyboard should only come up when the user
    // taps the input themselves or taps "Reply" on a comment.
    //
    // Focus the input whenever the user taps "Reply" on a comment.
    LaunchedEffect(replyTarget) {
        if (replyTarget != null) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        AnimatedVisibility(visible = replyTarget != null) {
            Row(
                modifier = Modifier
                    .padding(horizontal = SentupGroup.inset + 4.dp)
                    .padding(bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cm_replying_to, replyTarget?.author.orEmpty()),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onCancelReply) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = SentupGroup.inset)
                .sentupCard(shape = CircleShape, elevation = 1)
                .padding(start = 14.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester),
                placeholder = {
                    Text(
                        if (replyTarget != null) {
                            stringResource(R.string.cm_add_reply)
                        } else {
                            stringResource(R.string.cm_add_comment)
                        }
                    )
                },
                shape = CircleShape,
                maxLines = 4,
                enabled = !isPosting,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedBorderColor = androidx.compose.ui.graphics.Color.Transparent
                )
            )
            if (isPosting) {
                LoadingIndicator(
                    modifier = Modifier.size(32.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                val sendShape = CircleShape
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .videoPressTarget(
                            onClick = {
                                if (text.isNotBlank()) {
                                    onSend(text)
                                    text = ""
                                }
                            },
                            shape = sendShape,
                            scaleTo = 0.92f,
                            enabled = text.isNotBlank()
                        )
                        .background(
                            if (text.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            },
                            sendShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = stringResource(R.string.cm_post),
                        tint = if (text.isNotBlank()) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        },
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

/**
 * The small labelled action under a comment: a capsule holding an icon and a
 * word, pressing inward like everything else.
 *
 * These used to be bare bold labels padded by hand, which gave them a hit area
 * of about eight words' worth of pixels and made "Reply" and "Delete"
 * indistinguishable from the comment's own text until a finger landed on them.
 */
@Composable
private fun CommentActionPill(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val scheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .videoPressTarget(onClick = onClick, shape = CircleShape, scaleTo = 0.94f, enabled = enabled)
            .background(scheme.surfaceContainerHighest, CircleShape)
            .padding(horizontal = 11.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) scheme.onSurfaceVariant else scheme.onSurfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier.size(13.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (enabled) scheme.onSurfaceVariant else scheme.onSurfaceVariant.copy(alpha = 0.45f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * One comment: avatar, who and when, what they said, and what can be done to
 * it.
 *
 * The creator's own comments are the ones worth finding in a long thread, so
 * they carry the one filled capsule on the page - the author's name in primary,
 * which is also the tap target for their channel. Everything else stays in the
 * muted tone so a hundred of them do not read as a hundred headlines.
 */
@Composable
private fun CommentRow(
    comment: CommentItem,
    isReply: Boolean = false,
    onLikeClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    /** Seek the player, in seconds. Null leaves timestamps as plain text. */
    onSeekTo: ((seconds: Long) -> Unit)? = null,
    onOpenAuthor: ((channelId: String) -> Unit)? = null
) {
    // Avatar and name open the commenter's channel, as on YouTube (#298).
    val openAuthor = comment.authorChannelId?.let { id -> onOpenAuthor?.let { open -> { open(id) } } }
    val authorLabel = stringResource(R.string.cd_open_commenter_channel, comment.author)
    val authorTap = if (openAuthor != null) {
        Modifier.clickable(onClickLabel = authorLabel, onClick = openAuthor)
    } else Modifier
    val scheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.padding(horizontal = SentupGroup.dividerInset, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar
        val avatarSize = if (isReply) 26.dp else 36.dp
        if (comment.authorAvatarUrl != null) {
            AsyncImage(
                model = comment.authorAvatarUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .then(authorTap),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(avatarSize)
                    .clip(CircleShape)
                    .then(authorTap)
                    .background(scheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.author.removePrefix("@").take(1).uppercase(),
                    color = scheme.onPrimaryContainer,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            // Author line
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (comment.isPinned) {
                    Icon(
                        Icons.Rounded.PushPin,
                        contentDescription = "Pinned",
                        modifier = Modifier.size(12.dp),
                        tint = scheme.onSurfaceVariant
                    )
                }
                if (comment.isCreator) {
                    Surface(
                        shape = CircleShape,
                        color = scheme.primaryContainer,
                        modifier = Modifier.clip(CircleShape).then(authorTap)
                    ) {
                        Text(
                            text = comment.author,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = scheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                } else {
                    Text(
                        text = comment.author,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .then(authorTap)
                    )
                }
                if (comment.isVerified) {
                    Icon(
                        Icons.Rounded.Verified,
                        contentDescription = "Verified",
                        modifier = Modifier.size(14.dp),
                        tint = scheme.onSurfaceVariant
                    )
                }
                Text(
                    text = comment.publishedTime,
                    style = MaterialTheme.typography.labelSmall,
                    color = scheme.onSurfaceVariant.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(4.dp))

            // Timestamps and links come pre-marked by YouTube (CommentItem.links),
            // so the tappable ranges are exact rather than pattern-matched.
            Text(
                text = com.sentup.android.ui.components.rememberLinkedText(
                    rich = com.sentup.android.data.RichText(comment.text, comment.links),
                    onTimestampClick = onSeekTo
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onSurface
            )

            Spacer(Modifier.height(8.dp))

            // Like button + count + creator heart. Tappable only when the
            // signed-in fetch provided the matching toolbar action params.
            val canToggleLike = onLikeClick != null &&
                (if (comment.isLiked) comment.unlikeParams else comment.likeParams) != null
            val likeTint = if (comment.isLiked) scheme.primary else scheme.onSurfaceVariant
            Row(verticalAlignment = Alignment.CenterVertically) {
                CommentActionPill(
                    text = (if (comment.isLiked) comment.likeCountLiked.ifEmpty { comment.likeCount }
                    else comment.likeCount).ifEmpty { "0" },
                    icon = Icons.Rounded.ThumbUp,
                    enabled = canToggleLike,
                    onClick = { onLikeClick?.invoke() }
                )

                if (comment.isHearted) {
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Rounded.Favorite,
                        contentDescription = "Hearted by creator",
                        modifier = Modifier.size(15.dp),
                        tint = scheme.error
                    )
                }

                // Delete affordance — only shown on the user's own comments
                // (deleteParams exists only for those)
                if (onDeleteClick != null) {
                    Spacer(Modifier.width(8.dp))
                    CommentActionPill(
                        text = stringResource(R.string.cm_delete_action),
                        icon = Icons.Rounded.DeleteOutline,
                        onClick = onDeleteClick
                    )
                }
            }
        }
    }
}

/**
 * A relative age from YouTube's own wording, as minutes since posting.
 *
 * Only the [CommentOrder.NEW] strip needs this, and it needs it because
 * `publishedTime` is the only time a comment carries - the payload has no
 * timestamp - and the request returns one page in one order. Turning "3 hours
 * ago" into something sortable is therefore the only way that strip can be
 * honest about what it is doing, and anything it cannot read sorts last
 * rather than being guessed at.
 */
private fun String.timeAgoMillis(): Long? {
    val cleaned = trim().removePrefix("Edited").trim().lowercase()
    val amount = cleaned.takeWhile { it.isDigit() }.toLongOrNull() ?: return null
    val unit = COMMENT_AGE_UNITS.firstOrNull { cleaned.contains(it.first) } ?: return null
    val minutes = amount * unit.second
    return minutes * 60_000L
}

/** Minutes per unit word, longest first so "minutes" is never read as "minute". */
private val COMMENT_AGE_UNITS: List<Pair<String, Long>> = listOf(
    "second" to 1L / 60,
    "minute" to 1L,
    "hour" to 60L,
    "day" to 24L * 60,
    "week" to 7L * 24 * 60,
    "month" to 30L * 24 * 60,
    "year" to 365L * 24 * 60
)