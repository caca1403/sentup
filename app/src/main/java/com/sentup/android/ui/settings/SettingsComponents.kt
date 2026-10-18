package com.sentup.android.ui.settings

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sentup.android.R
import com.sentup.android.ui.components.LocalInSegmentedColumn
import com.sentup.android.ui.components.SegmentedColumn
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage
import com.sentup.android.ui.theme.sentupSpecularEdge


/**
 * Shared building blocks for the settings hub and its detail pages.
 *
 * These replace the dozen near-identical `Expressive*ToggleItem` composables the
 * settings screen used to carry, which differed from each other only by icon,
 * title and subtitle. Colors are resolved from the theme here instead of being
 * threaded in as parameters, because every call site passed the same three
 * roles - only the destructive rows ever wanted something else, and those say so
 * with [SettingsRowDefaults.destructiveTint].
 */

/**
 * What one hold-to-explain gesture surfaces: the row's own icon and title over
 * a longer explanation than a subtitle has room for.
 *
 * Rows do not host the dialog themselves - a dialog owned by a row dies with
 * the page transition (the same reason SettingsScreen hosts every other
 * dialog), so rows hand the request up through [LocalSettingsInfoSink] and the
 * screen shows one dialog for all of them.
 */
internal data class SettingsInfoData(
    val icon: ImageVector,
    val title: String,
    val text: String,
)

/**
 * Where a long-pressed row sends its explanation. Null when no host is around
 * (a row reused outside the settings screen), in which case the gesture simply
 * does not exist rather than crashing.
 */
internal val LocalSettingsInfoSink =
    staticCompositionLocalOf<((SettingsInfoData) -> Unit)?> { null }

/**
 * The dialog behind every hold-to-explain gesture, in the house dialog style:
 * icon box, centered title, one dismiss action.
 */
@Composable
internal fun SettingsInfoDialog(
    info: SettingsInfoData,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(32.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        icon = {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = info.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(30.dp)
                )
            }
        },
        title = {
            Text(
                text = info.title,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = info.text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 20.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.si_got_it), fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

object SettingsRowDefaults {
    /**
     * Tint for rows whose action is hard to take back (clear the cache, sign
     * out) or that report a problem the user has to leave the app to fix.
     */
    val destructiveTint: Color
        @Composable get() = MaterialTheme.colorScheme.error
}

@Composable
internal fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    // No entrance animation on purpose: these live in a LazyColumn, which
    // recomposes items as they scroll into view - any enter animation replays
    // on every scroll and makes the cards look like they're "loading".
    //
    // Sentence case, secondary colour, and inset to the card's own text edge.
    // It was uppercase, in the accent colour, and inset 8dp - which made a
    // settings page read as a colour picker with some switches in it. A group
    // heading is furniture: it names the group and gets out of the way, and the
    // accent belongs to the things you can act on, which are the rows.
    Column {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(
                start = SentupGroup.inset + SentupGroup.dividerInset,
                end = SentupGroup.inset,
                bottom = 8.dp
            )
        )
        content()
    }
}

/**
 * A card of settings rows, drawn as a Material 3 Expressive segmented list -
 * the Android 16 Settings look (see [SegmentedColumn]). Done here rather than
 * by giving every row its index and count, because the cards take arbitrary
 * content across a dozen pages.
 */
@Composable
internal fun SettingsCard(
    content: @Composable ColumnScope.() -> Unit
) {
    // Inset from the screen edges, one continuous corner radius, and a hairline
    // along the top edge for the light it is catching. That combination - not
    // a different fill - is what makes a group read as a card floating above the
    // page; a full-bleed card with no corners reads as a band of the page, which
    // is what a settings screen looks like when it has not been designed.
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SentupGroup.inset)
            .sentupCard(
                shape = RoundedCornerShape(SentupRadii.card),
                elevation = 0,
                tinted = false
            )
            .sentupSpecularEdge(RoundedCornerShape(SentupRadii.card))
            .padding(vertical = 2.dp),
        content = content
    )
}

/**
 * Icon + title/subtitle row that opens something else - a dialog, a page, an
 * Android settings screen. [showChevron] is for the ones that navigate; leave it
 * off for rows that act in place.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
    titleColor: Color = MaterialTheme.colorScheme.onBackground,
    showChevron: Boolean = false,
    /** Long-press explanation. Null keeps the row a plain tap target. */
    explanation: String? = null
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rowScale"
    )
    val haptics = com.sentup.android.util.rememberSentupHaptics()
    val infoSink = LocalSettingsInfoSink.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.subtle()
                    onClick()
                },
                onLongClick = if (explanation != null && infoSink != null) {
                    {
                        haptics.longPress()
                        infoSink(SettingsInfoData(icon, title, explanation))
                    }
                } else null
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsRowIcon(icon = icon, tint = tint)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = titleColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp
            )
        }

        if (showChevron) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Icon + title/subtitle row with a switch. The whole row is the hit target, not
 * just the switch. An unavailable row (the phone cannot do it) is drawn
 * disabled and ignores taps; its long-press explanation still works, and the
 * subtitle is where it should say why.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
    /** Long-press explanation. Null keeps the row a plain toggle. */
    explanation: String? = null,
    available: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "toggleScale"
    )
    val haptics = com.sentup.android.util.rememberSentupHaptics()
    val infoSink = LocalSettingsInfoSink.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (available) {
                        haptics.toggle(!enabled)
                        onToggle(!enabled)
                    }
                },
                onLongClick = if (explanation != null && infoSink != null) {
                    {
                        haptics.longPress()
                        infoSink(SettingsInfoData(icon, title, explanation))
                    }
                } else null
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Material's disabled content: 38% of the colour it would have had.
        val contentAlpha = if (available) 1f else 0.38f
        SettingsRowIcon(icon = icon, tint = tint.copy(alpha = tint.alpha * contentAlpha))

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = contentAlpha),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha),
                fontSize = 13.sp
            )
        }

        Switch(
            checked = enabled,
            onCheckedChange = onToggle,
            enabled = available,
            colors = SwitchDefaults.colors(
                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                checkedTrackColor = tint,
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                uncheckedBorderColor = Color.Transparent,
                checkedBorderColor = Color.Transparent
            )
        )
    }
}

/**
 * Where the options of an inline choice begin: at the trailing edge of the icon
 * tile of the question above them. They are children of that row, so they start
 * where its content does, and the hairline between two options is inset to the
 * same line for the same reason every other hairline here is - a full-bleed line
 * would cut the card into bands.
 */
internal val settingsOptionIndent = 64.dp

/** The hairline between two options of one choice. See [settingsOptionIndent]. */
@Composable
private fun SettingsOptionDivider(indent: Dp = settingsOptionIndent) {
    if (LocalInSegmentedColumn.current) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent, end = 16.dp, top = 1.dp, bottom = 1.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    )
}

/**
 * One answer of a single-select choice, drawn as a row with the accent check on
 * the chosen one.
 *
 * A strip of connected [androidx.compose.material3.ToggleButton]s was the first
 * version of this and it is the wrong shape twice over: a chosen answer repaints
 * as a solid block of the palette, which is the loudest thing on the page even
 * when nobody is changing it, and four labels have to share four fractions of one
 * row, so on a narrow phone they ellipsise into "Sanatç…". One row per answer
 * costs a little scrolling and says the same thing at the volume everything else
 * on the page uses. The check fades in rather than swapping icons, so choosing
 * reads as the list agreeing with you.
 */
@Composable
internal fun SettingsOptionRow(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    indent: Dp = settingsOptionIndent,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "optionScale"
    )
    val checkAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0f,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "optionCheck"
    )
    val haptics = com.sentup.android.util.rememberSentupHaptics()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(SentupRadii.tile))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    haptics.confirm()
                    onSelect()
                }
            )
            .heightIn(min = 44.dp)
            .padding(start = indent, end = 16.dp, top = 10.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = accent,
            modifier = Modifier
                .size(20.dp)
                .alpha(checkAlpha)
        )
    }
}

/**
 * A setting with a handful of answers, all on show: the question as a row, then
 * every answer beneath it in the same card.
 *
 * [labels] is short by design - a longer list wants a dialog, where it can
 * scroll and search.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingsChoiceRow(
    title: String,
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    subtitle: String? = null,
    icon: ImageVector? = null,
    tint: Color = MaterialTheme.colorScheme.primary,
    /** Long-press on the question explains the setting; the answers stay plain taps. */
    explanation: String? = null,
) {
    // Without an icon tile the options still need to sit under the question's
    // text, so the rail moves in to where that text starts.
    val optionIndent = if (icon != null) settingsOptionIndent else 32.dp
    val infoSink = LocalSettingsInfoSink.current
    val haptics = com.sentup.android.util.rememberSentupHaptics()
    Column(modifier = Modifier.padding(top = 12.dp, bottom = 6.dp)) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .then(
                    if (explanation != null && infoSink != null) {
                        Modifier.combinedClickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {},
                            onLongClick = {
                                haptics.longPress()
                                infoSink(
                                    SettingsInfoData(
                                        icon ?: Icons.Rounded.Info,
                                        title,
                                        explanation
                                    )
                                )
                            }
                        )
                    } else {
                        Modifier
                    }
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                SettingsRowIcon(icon = icon, tint = tint)
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (subtitle != null) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        SettingsOptionDivider(indent = optionIndent)
        labels.forEachIndexed { index, label ->
            if (index > 0) SettingsOptionDivider(indent = optionIndent)
            SettingsOptionRow(
                label = label,
                selected = index == selectedIndex,
                accent = tint,
                indent = optionIndent,
                onSelect = { if (index != selectedIndex) onSelect(index) }
            )
        }
    }
}

/**
 * The answers of a choice that belongs to the row above it - the crossfade
 * length under the crossfade switch - so it carries no question of its own.
 */
@Composable
internal fun SettingsSubChoiceRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    indent: Dp = settingsOptionIndent,
) {
    Column(modifier = modifier.padding(top = 2.dp, bottom = 8.dp)) {
        labels.forEachIndexed { index, label ->
            if (index > 0) SettingsOptionDivider(indent = indent)
            SettingsOptionRow(
                label = label,
                selected = index == selectedIndex,
                accent = accent,
                indent = indent,
                onSelect = { if (index != selectedIndex) onSelect(index) }
            )
        }
    }
}

/**
 * Two or three views of the same content, side by side above them - Recent and
 * Account, nothing to do with a stored value.
 *
 * A [androidx.compose.material3.ToggleButton] strip is the M3 answer and it is
 * wrong here twice: the chosen tab turns into a slab of the palette, which
 * competes with the content it is supposed to be underlining, and pressing one
 * flashes a ripple. This is the same object at a lower volume - the chosen tab
 * takes a tonal fill, and every tab presses inward like the rows beneath it.
 */
@Composable
internal fun SettingsSegmentTabs(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = com.sentup.android.util.rememberSentupHaptics()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(SentupRadii.pill))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.97f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "tabScale"
            )
            val fill by androidx.compose.animation.animateColorAsState(
                targetValue = if (index == selectedIndex) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    Color.Transparent
                },
                animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
                label = "tabFill"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .scale(scale)
                    .clip(RoundedCornerShape(SentupRadii.pill))
                    .background(fill)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = {
                            if (index != selectedIndex) {
                                haptics.confirm()
                                onSelect(index)
                            }
                        }
                    )
                    .heightIn(min = 38.dp)
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = if (index == selectedIndex) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (index == selectedIndex) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * A row that only does something, in its own card: clear the cache, sign out,
 * remove the account.
 *
 * These are the two rows on a page that cannot be taken back, and a card that
 * holds both of them and four switches asks to be mis-tapped. The tint is the
 * only signal - the icon tile and the title both take it - so an action that is
 * merely destructive-looking stays a [SettingsRow].
 */
@Composable
internal fun SettingsDestructiveRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    explanation: String? = null,
    showChevron: Boolean = false
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        onClick = onClick,
        tint = SettingsRowDefaults.destructiveTint,
        titleColor = SettingsRowDefaults.destructiveTint,
        showChevron = showChevron,
        explanation = explanation
    )
}


/**
 * The 48dp tinted icon box every settings row leads with. [shape] lets the hub
 * give each category a die-cut identity (the player-style-wheel language);
 * detail rows keep the default squircle so pages read calmer than the hub.
 */
@Composable
private fun SettingsRowIcon(
    icon: ImageVector,
    tint: Color,
    shape: Shape = RoundedCornerShape(14.dp)
) {
    // A solid tonal tile rather than the tint at 12%: a filled container
    // carries the palette where a wash barely shows it. The tint names the
    // role, and the tile takes that role's container pair; anything else
    // (a brand colour, a muted row) keeps the wash it had.
    val scheme = MaterialTheme.colorScheme
    val solid = tint.copy(alpha = 1f)
    val (container, glyph) = when (solid) {
        scheme.primary, scheme.secondary -> scheme.secondaryContainer to scheme.onSecondaryContainer
        scheme.tertiary -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        scheme.error -> scheme.errorContainer to scheme.onErrorContainer
        else -> solid.copy(alpha = 0.12f) to solid
    }
    // A disabled row arrives with its tint faded; the tile fades with it.
    val fade = tint.alpha
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(shape)
            .background(container.copy(alpha = container.alpha * fade)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = glyph.copy(alpha = glyph.alpha * fade),
            modifier = Modifier.size(26.dp)
        )
    }
}

@Composable
internal fun SettingsDivider() {
    // Inside a segmented card the gap is the separator; drawing nothing also
    // measures to nothing, so the card skips this child entirely.
    if (LocalInSegmentedColumn.current) return
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    )
}

/**
 * One category on the settings hub. [value] is the live state of whatever lives
 * inside, so the hub can be read without opening every page - that is the whole
 * point of splitting the screen up, and a row without it is a worse version of
 * the flat list it replaced.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun SettingsHubRow(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.primary,
    iconShape: Shape = RoundedCornerShape(14.dp),
    /** Long press explains what the category holds without opening it. */
    explanation: String? = null,
    /**
     * The category open in the detail pane beside the hub, on a window wide
     * enough for both. Never true on a phone, where the page covers the hub.
     */
    selected: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "hubScale"
    )
    val selectedFill by androidx.compose.animation.animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        animationSpec = MaterialTheme.motionScheme.fastEffectsSpec(),
        label = "hubSelected"
    )
    val haptics = com.sentup.android.util.rememberSentupHaptics()
    val infoSink = LocalSettingsInfoSink.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(18.dp))
            .background(selectedFill)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = if (explanation != null && infoSink != null) {
                    {
                        haptics.longPress()
                        infoSink(SettingsInfoData(icon, title, explanation))
                    }
                } else null
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsRowIcon(icon = icon, tint = tint, shape = iconShape)

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer
                else MaterialTheme.colorScheme.onBackground,
                fontSize = 16.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * False while a top-level page is drawn in the detail pane beside the hub:
 * there is nothing to go back to when the list is on screen next to it, and an
 * arrow that only re-selects the first category reads as broken. Nested pages
 * (everything under Customization) keep theirs, which returns to their parent.
 */
internal val LocalSettingsDetailShowsBack = androidx.compose.runtime.compositionLocalOf { true }

/**
 * Room left under the last row of the hub and of every page, so it can scroll
 * clear of the floating mini player (and the navigation bar under it) instead
 * of sitting behind it. Reserved whether or not anything is playing: the mini
 * player can appear while a page is open, and the list should not jump when it
 * does. Matches the clearance other full-screen routes (Stats) use.
 */
internal val SettingsMiniPlayerClearance = 160.dp

/**
 * Chrome shared by every settings detail page: the back-to-hub bar plus the
 * scrolling body. Pages supply `item { }` blocks so long lists (the palette of
 * player styles, a folder list) still recycle.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun SettingsDetailScaffold(
    title: String,
    onBack: () -> Unit,
    /**
     * Drawn between the top bar and the list, and it does not scroll.
     *
     * For a page whose whole job is judging one thing while you change it - the
     * app icon, where the controls run past a screen and the preview is the
     * point - a header inside the list scrolls off exactly when it starts being
     * needed. Most pages want nothing here.
     */
    header: (@Composable () -> Unit)? = null,
    listState: androidx.compose.foundation.lazy.LazyListState = androidx.compose.foundation.lazy.rememberLazyListState(),
    itemSpacing: androidx.compose.ui.unit.Dp = 20.dp,
    content: LazyListScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .sentupPage()
    ) {
        TopAppBar(
            title = {
                // The page's own title, at the size the eye reads a screen name
                // at: a toolbar caption (24sp) makes every page look like a
                // dialog the hub opened, and nothing tells you which category
                // you are in until you read a row.
                Text(
                    text = title,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                if (LocalSettingsDetailShowsBack.current) IconButton(
                    onClick = onBack,
                    shapes = IconButtonDefaults.shapes(),
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        contentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars)
        )

        if (header != null) {
            Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                header()
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = SettingsMiniPlayerClearance
            ),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(itemSpacing)
        ) {
            content()
        }
    }
}
