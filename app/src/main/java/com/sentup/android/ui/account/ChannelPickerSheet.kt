package com.sentup.android.ui.account

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sentup.android.R
import com.sentup.android.data.AccountSwitcher
import com.sentup.android.data.SessionManager
import com.sentup.android.data.YouTubeRepository
import com.sentup.android.data.youtube.YouTubeChannel
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupGroupCard
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.SentupRow

/**
 * Which channel of the signed-in account the app should be.
 *
 * A Google account owns one channel of its own plus any number of brand
 * channels ("marka hesabı"), and until this sheet existed the app could only
 * ever be the first one: it read the account's identity from
 * `account/account_menu`, which reports the channel you are on and never lists
 * the others, so a brand channel was unselectable even when the login behind it
 * was perfectly good.
 *
 * Picking one changes nothing about the login. It pins the channel on the
 * profile, and from then on every request carries it as `X-Goog-PageId`
 * ([com.sentup.android.data.youtube.authenticate]), which is what moves the
 * feed, the subscriptions, the likes and the history onto that channel at once.
 * The account's own channel is a real row here rather than an absence, because
 * going back to it is a choice someone makes rather than a state they fall into.
 */
@Composable
fun ChannelPickerSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val repository = remember(context) { YouTubeRepository(context) }
    val sessionManager = remember(context) { SessionManager(context) }
    val switcher = remember(context) { AccountSwitcher(context) }
    val activeId by switcher.activeProfileId.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var channels by remember { mutableStateOf<List<YouTubeChannel>>(emptyList()) }
    var currentPageId by remember { mutableStateOf<String?>(null) }
    var signedIn by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    // Bumped by the retry row; the key of the effect below, so a failed read is
    // one tap away from being tried again without leaving the sheet.
    var reloads by remember { mutableStateOf(0) }

    LaunchedEffect(activeId, reloads) {
        loading = true
        signedIn = sessionManager.isLoggedIn()
        currentPageId = sessionManager.activePageId()
        channels = if (signedIn) repository.fetchChannels() else emptyList()
        loading = false
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 12.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = stringResource(R.string.ac_channel_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(
                    start = SentupGroup.inset + SentupGroup.dividerInset,
                    end = SentupGroup.inset + SentupGroup.dividerInset,
                    bottom = 4.dp
                )
            )
            Text(
                text = stringResource(R.string.ac_channel_sub),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = SentupGroup.inset + SentupGroup.dividerInset,
                    end = SentupGroup.inset + SentupGroup.dividerInset,
                    bottom = 16.dp
                )
            )

            when {
                loading -> LoadingRow()

                !signedIn -> SentupGroupCard(showDividers = false) {
                    NoteRow(text = stringResource(R.string.ac_channel_signed_out))
                }

                else -> {
                    // Only an item YouTube marks as the current one is trusted
                    // with a name here: an item with no page id is either the
                    // account's own channel or a brand channel whose id this
                    // response did not carry, and only `isSelected` tells the
                    // two apart.
                    val own = channels.firstOrNull { it.pageId == null && it.isCurrent }
                    val brands = channels.filter { it.pageId != null }

                    // One card, inset hairlines: the channels are options of
                    // one choice, not separate tiles.
                    SentupGroupCard(showDividers = brands.isNotEmpty()) {
                        ChannelRow(
                            name = own?.name ?: stringResource(R.string.ac_channel_primary),
                            subtitle = stringResource(R.string.ac_channel_primary_sub),
                            avatarUrl = own?.avatarUrl,
                            selected = currentPageId == null,
                            onClick = {
                                repository.selectChannel(null, own?.name, own?.avatarUrl)
                                onDismiss()
                            }
                        )
                        brands.forEach { channel ->
                            SentupDivider()
                            ChannelRow(
                                name = channel.name,
                                subtitle = channel.byline
                                    ?: channel.handle
                                    ?: stringResource(R.string.ac_channel_brand_sub),
                                avatarUrl = channel.avatarUrl,
                                selected = channel.pageId == currentPageId,
                                onClick = {
                                    repository.selectChannel(channel.pageId, channel.name, channel.avatarUrl)
                                    onDismiss()
                                }
                            )
                        }
                    }

                    if (brands.isEmpty()) {
                        Spacer(Modifier.height(SentupGroup.gap))
                        SentupGroupCard(showDividers = false) {
                            NoteRow(
                                text = stringResource(R.string.ac_channel_no_brands),
                                detail = stringResource(R.string.ac_channel_no_brands_sub)
                            )
                            ActionRow(
                                text = stringResource(R.string.ac_channel_refresh),
                                onClick = { reloads += 1 }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * One channel: its photo, its name, what it is, and whether it is the one in
 * use. The whole row is the target, since the only thing it can do is be
 * picked.
 */
@Composable
private fun ChannelRow(
    name: String,
    subtitle: String,
    avatarUrl: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "channelRowScale"
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SentupRadii.tile))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = 50.dp)
            .padding(horizontal = SentupGroup.dividerInset, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            if (!avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().clip(CircleShape)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.PersonOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
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

        if (selected) {
            Spacer(Modifier.width(10.dp))
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.cd_channel_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** A quiet note for the states with nothing to pick: signed out, or no list. */
@Composable
private fun NoteRow(
    text: String,
    detail: String? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.dividerInset, vertical = 12.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        if (detail != null) {
            Spacer(Modifier.height(2.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Retry, worded as what it does rather than as an error. */
@Composable
private fun ActionRow(
    text: String,
    onClick: () -> Unit
) {
    SentupRow(
        icon = Icons.Rounded.Refresh,
        title = text,
        tint = MaterialTheme.colorScheme.primary,
        onClick = onClick
    )
}

@Composable
private fun LoadingRow() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}
