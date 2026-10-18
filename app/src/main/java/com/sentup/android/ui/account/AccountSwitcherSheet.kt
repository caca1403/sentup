package com.sentup.android.ui.account
import androidx.compose.ui.res.stringResource
import com.sentup.android.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.PersonOutline
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sentup.android.data.IncognitoMode
import com.sentup.android.data.AccountSwitcher
import com.sentup.android.data.Profile
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupGroupCard
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.SentupRow

/**
 * The profile switcher.
 *
 * Opened from the home avatar, because that is where the current identity is
 * already shown and it sits under the thumb. A bottom sheet rather than a menu:
 * a row here has to carry an avatar, a name, a handle and sometimes a
 * "reconnect" state, which a dropdown cannot hold legibly.
 *
 * Switching is instant and offline - the cookies are already on device - so
 * there is no confirm step and no blocking spinner. The sheet closes on tap and
 * the feeds refill behind it.
 */
@Composable
fun AccountSwitcherSheet(
    onDismiss: () -> Unit,
    onAddYouTubeAccount: () -> Unit,
    /**
     * Sign back into the profile that is now active, rather than adding a new
     * one. Separate from [onAddYouTubeAccount] because the two must not share
     * a path: adding captures the session into a *new* profile, which for a
     * row that already exists would strand that profile's subscriptions and
     * blocklist on an id nothing is active on any more.
     */
    onReconnectProfile: () -> Unit = onAddYouTubeAccount,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val switcher = remember(context) { AccountSwitcher(context) }
    val profiles by switcher.profiles.collectAsState()
    val activeId by switcher.activeProfileId.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val incognito by IncognitoMode.enabled(context).collectAsState()

    var pendingRemoval by remember { mutableStateOf<Profile?>(null) }
    var showAddLocal by remember { mutableStateOf(false) }

    /**
     * YouTube profiles with no stored session: restored from a backup, or
     * otherwise never signed into on this device. Resolved once per roster
     * change rather than per row, since each lookup is a decrypt.
     */
    val needSignIn = remember(profiles) {
        profiles.filter { !it.isLocal && !switcher.hasStoredSession(it.id) }
            .map { it.id }
            .toSet()
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
                text = stringResource(R.string.profiles_title),
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
                text = stringResource(R.string.profiles_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(
                    start = SentupGroup.inset + SentupGroup.dividerInset,
                    end = SentupGroup.inset + SentupGroup.dividerInset,
                    bottom = 16.dp
                )
            )

            // The roster is one object: rows separated by inset hairlines,
            // never stacked tiles with their own fills.
            SentupGroupCard(showDividers = profiles.size > 1) {
                profiles.forEachIndexed { index, profile ->
                    if (index > 0) SentupDivider()
                    ProfileRow(
                        profile = profile,
                        isActive = profile.id == activeId,
                        needsSignIn = profile.id in needSignIn,
                        // The last profile standing cannot be deleted, but a
                        // YouTube account on it can still be disconnected - that is
                        // the plain "sign out" case, and it must not disappear just
                        // because the roster has one row.
                        canRemove = profiles.size > 1 || !profile.isLocal,
                        onClick = {
                            // The row has been promising "tap to reconnect" while
                            // doing nothing but switch, so tapping the profile that
                            // was already active was a dead tap. Switch first so
                            // the captured session is stored against this profile,
                            // then hand off to the sign-in flow.
                            val broken = profile.expired || profile.id in needSignIn
                            if (profile.id != activeId) switcher.switchTo(profile.id)
                            onDismiss()
                            if (broken) onReconnectProfile()
                        },
                        onRemove = { pendingRemoval = profile }
                    )
                }
            }

            Spacer(Modifier.height(SentupGroup.gap))

            SentupGroupCard {
                SentupRow(
                    icon = Icons.Rounded.Add,
                    title = stringResource(R.string.add_youtube_account),
                    subtitle = stringResource(R.string.add_youtube_account_subtitle),
                    onClick = {
                        onDismiss()
                        onAddYouTubeAccount()
                    }
                )
                SentupDivider()
                SentupRow(
                    icon = Icons.Rounded.PhoneAndroid,
                    title = stringResource(R.string.add_device_profile),
                    subtitle = stringResource(R.string.add_device_profile_subtitle),
                    onClick = { showAddLocal = true }
                )
            }

            // Incognito lives with the profiles rather than in Settings because
            // it is the same kind of decision - who the app is being right now
            // - and because it is meant to be turned on for one sitting and off
            // again, which a control three screens deep would not be.
            Spacer(Modifier.height(SentupGroup.gap))

            SentupGroupCard(showDividers = false) {
                IncognitoRow(
                    enabled = incognito,
                    onCheckedChange = { IncognitoMode.setEnabled(context, it) }
                )
            }
        }
    }

    if (showAddLocal) {
        NameProfileDialog(
            onConfirm = { name ->
                showAddLocal = false
                switcher.addLocalProfileAndSwitch(name)
                onDismiss()
            },
            onDismiss = { showAddLocal = false }
        )
    }

    pendingRemoval?.let { target ->
        // With one profile left there is nothing to fall back to, so the
        // account is disconnected in place instead of the row being deleted.
        val isLastProfile = profiles.size <= 1
        RemoveProfileDialog(
            profile = target,
            isSignOutOnly = isLastProfile,
            onConfirm = {
                if (isLastProfile) switcher.signOut(target.id) else switcher.remove(target.id)
                pendingRemoval = null
            },
            onDismiss = { pendingRemoval = null }
        )
    }
}

/**
 * The incognito switch.
 *
 * Worded as what it does rather than as a mode name, because "incognito" means
 * subtly different things in different apps and the difference that matters
 * here is which things stop being recorded. Playback, downloads, playlists and
 * likes are untouched, and the subtitle says so: someone who reads this as
 * "nothing I do is saved" would be surprised by a download appearing later.
 */
@Composable
private fun IncognitoRow(
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    SentupRow(
        icon = Icons.Rounded.VisibilityOff,
        title = stringResource(R.string.incognito_title),
        subtitle = stringResource(R.string.incognito_subtitle),
        tint = if (enabled) MaterialTheme.colorScheme.primary else null,
        onClick = { onCheckedChange(!enabled) },
        trailing = {
            Switch(checked = enabled, onCheckedChange = onCheckedChange)
        }
    )
}

/**
 * One profile.
 *
 * A row of the group card, not a tile of its own: the active profile is
 * marked by the emerald check, not by a filled background, so the roster
 * reads as one list of separate people rather than a pile of buttons.
 *
 * An expired session is called out here rather than failing silently later:
 * before profiles, a dead session showed up only as inexplicably empty
 * account screens, with nothing saying that signing in again was the fix.
 */
@Composable
private fun ProfileRow(
    profile: Profile,
    isActive: Boolean,
    /** A YouTube profile with no stored session: restored, or never signed in. */
    needsSignIn: Boolean,
    canRemove: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val flagged = profile.expired || needsSignIn
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.985f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "profileRowScale"
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
        ProfileAvatar(profile = profile, size = 40)

        Spacer(Modifier.width(14.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = profile.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val subtitle = when {
                // Order matters: a restored profile is also flagged expired,
                // and "signed out" is the accurate half of that. Nothing
                // expired on a phone the account was never signed into.
                needsSignIn -> stringResource(R.string.profile_signed_out)
                profile.expired -> stringResource(R.string.profile_session_expired)
                profile.isLocal -> stringResource(R.string.profile_on_this_device)
                else -> profile.handle ?: stringResource(R.string.profile_youtube_account)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (flagged) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (flagged) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        AnimatedVisibility(visible = isActive, enter = fadeIn(), exit = fadeOut()) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.cd_active_profile),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }

        if (canRemove) {
            Spacer(Modifier.width(4.dp))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_remove_profile, profile.name),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

/**
 * A profile's picture, or a stand-in.
 *
 * A local profile has no avatar by definition, so it gets an icon rather than
 * a broken image - and an expired account is ringed in the error color, so the
 * state is legible from the home avatar too, not only inside this sheet.
 */
@Composable
fun ProfileAvatar(profile: Profile, size: Int, modifier: Modifier = Modifier) {
    val ringColor = if (profile.expired) MaterialTheme.colorScheme.error
    else MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .border(width = if (profile.expired) 2.dp else 0.dp, color = ringColor, shape = CircleShape),
        contentAlignment = Alignment.Center
    ) {
        val avatar = profile.avatarUrl
        if (!avatar.isNullOrBlank()) {
            AsyncImage(
                model = avatar,
                contentDescription = null,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        } else {
            Icon(
                imageVector = if (profile.isLocal) Icons.Rounded.PhoneAndroid
                else Icons.Rounded.PersonOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size((size * 0.5f).dp)
            )
        }
    }
}

@Composable
private fun NameProfileDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(32.dp),
        title = { Text(stringResource(R.string.new_profile_dialog_title)) },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.acc_local_dialog_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.name_label)) },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) { Text(stringResource(R.string.action_create)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}

@Composable
private fun RemoveProfileDialog(
    profile: Profile,
    isSignOutOnly: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        shape = RoundedCornerShape(32.dp),
        title = {
            Text(if (isSignOutOnly) stringResource(R.string.sign_out_confirm_title, profile.name) else stringResource(R.string.remove_profile_confirm_title, profile.name))
        },
        text = {
            Text(
                text = when {
                    isSignOutOnly -> stringResource(R.string.acc_signout_dialog_body)
                    profile.isLocal -> stringResource(R.string.acc_remove_local_dialog_body)
                    else -> stringResource(R.string.acc_remove_account_dialog_body)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(if (isSignOutOnly) stringResource(R.string.sign_out) else stringResource(R.string.remove)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } }
    )
}
