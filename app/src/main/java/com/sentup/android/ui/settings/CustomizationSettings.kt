package com.sentup.android.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.SmartDisplay
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sentup.android.R
import com.sentup.android.data.AppLanguage
import com.sentup.android.data.PlayerStyle
import com.sentup.android.data.ThemePreferences
import com.sentup.android.ui.theme.ThemeMode
import kotlin.math.roundToInt

/**
 * Everything about how Sentüp looks and behaves, in one place and sorted by the
 * part of the app it changes: its colours, its players, its layout and how it
 * answers a touch.
 *
 * A page of ways in rather than a page of settings. Each row opens the page
 * that owns that surface and shows what is chosen there now, as the hub's rows
 * do, so the whole look of the app can be read off this one screen. A setting
 * that changes how something looks or what a gesture does belongs behind one of
 * these rows; a new surface gets a row of its own here, not a row on the hub.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun CustomizationSettingsPage(
    currentThemeMode: ThemeMode,
    colorPalette: String,
    appIcon: String,
    uiScale: Float,
    playerStyle: PlayerStyle,
    spotlightHome: Boolean,
    nonExpressiveNavigationBar: Boolean,
    hapticsLevel: String,
    onOpenPage: (SettingsPage) -> Unit,
    onBack: () -> Unit
) {
    val themeLabel = when (currentThemeMode) {
        ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
        ThemeMode.LIGHT -> stringResource(R.string.theme_light)
        ThemeMode.DARK -> stringResource(R.string.theme_dark)
    }
    val paletteName = if (colorPalette == ThemePreferences.DEFAULT_COLOR_PALETTE) {
        stringResource(R.string.theme_palette_dynamic)
    } else {
        com.sentup.android.ui.theme.findPalette(colorPalette)?.name
            ?: stringResource(R.string.theme_palette_dynamic)
    }
    val currentAppIcon = remember(appIcon) { AppIcon.fromId(appIcon) }
    val hapticsLabel = stringResource(
        when (hapticsLevel) {
            "off" -> R.string.haptic_level_off
            "subtle" -> R.string.haptic_level_subtle
            "expressive" -> R.string.haptic_level_rich
            else -> R.string.haptic_level_balanced
        }
    )
    // Read in composition, not remembered: `AppLanguage.apply` recreates the
    // activity on every path (13+ through the platform, below it through
    // `Activity.recreate`), so whatever this returns is read again from the new
    // configuration rather than from a stale cache.
    val languageLabel = languageLabel(AppLanguage.currentTag(LocalContext.current))

    var showLanguageSheet by remember { mutableStateOf(false) }

    SettingsDetailScaffold(
        title = stringResource(R.string.settings_customization),
        onBack = onBack
    ) {
        item {
            SettingsSection(title = stringResource(R.string.cz_section_look)) {
                SettingsCard {
                    SettingsHubRow(
                        icon = Icons.Rounded.Palette,
                        title = stringResource(R.string.settings_theme_and_colors),
                        value = "$themeLabel, $paletteName",
                        onClick = { onOpenPage(SettingsPage.APPEARANCE) },
                        tint = MaterialTheme.colorScheme.tertiary,
                        iconShape = MaterialShapes.Cookie9Sided.toShape(),
                        explanation = stringResource(R.string.cz_info_theme)
                    )
                    SettingsDivider()
                    SettingsHubRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = stringResource(R.string.sp_app_icon),
                        value = stringResource(currentAppIcon.titleRes),
                        onClick = { onOpenPage(SettingsPage.APP_ICON) },
                        tint = MaterialTheme.colorScheme.tertiary,
                        iconShape = MaterialShapes.Sunny.toShape(),
                        explanation = stringResource(R.string.si_app_icon)
                    )
                    SettingsDivider()
                    SettingsHubRow(
                        icon = Icons.Rounded.FormatSize,
                        title = stringResource(R.string.sp_display_size),
                        value = "${(uiScale * 100).roundToInt()}%",
                        onClick = { onOpenPage(SettingsPage.DISPLAY_SIZE) },
                        tint = MaterialTheme.colorScheme.tertiary,
                        iconShape = MaterialShapes.Pentagon.toShape(),
                        explanation = stringResource(R.string.si_display_size)
                    )
                    SettingsDivider()
                    SettingsHubRow(
                        icon = Icons.Rounded.Language,
                        title = stringResource(R.string.settings_language),
                        value = languageLabel,
                        onClick = { showLanguageSheet = true },
                        tint = MaterialTheme.colorScheme.tertiary,
                        iconShape = MaterialShapes.Pill.toShape(),
                        explanation = stringResource(R.string.cz_info_language)
                    )
                }
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.cz_section_players)) {
                SettingsCard {
                    SettingsHubRow(
                        icon = Icons.Rounded.PlayCircle,
                        title = stringResource(R.string.settings_player),
                        value = com.sentup.android.ui.player.playerStyleInfo(playerStyle).label,
                        onClick = { onOpenPage(SettingsPage.PLAYER) },
                        iconShape = MaterialShapes.Clover4Leaf.toShape(),
                        explanation = stringResource(R.string.si_hub_player)
                    )
                    SettingsDivider()
                    SettingsHubRow(
                        icon = Icons.Rounded.PictureInPictureAlt,
                        title = stringResource(R.string.sp_mini_player),
                        value = miniPlayerSummary(),
                        onClick = { onOpenPage(SettingsPage.MINI_PLAYER) },
                        iconShape = MaterialShapes.Pill.toShape(),
                        explanation = stringResource(R.string.cz_info_mini_player)
                    )
                    SettingsDivider()
                    SettingsHubRow(
                        icon = Icons.Rounded.Bedtime,
                        title = stringResource(R.string.standby_title),
                        value = standbySummary(),
                        onClick = { onOpenPage(SettingsPage.STANDBY) },
                        iconShape = MaterialShapes.Arch.toShape(),
                        explanation = stringResource(R.string.cz_info_standby)
                    )
                    SettingsDivider()
                    SettingsHubRow(
                        icon = Icons.Rounded.SmartDisplay,
                        title = stringResource(R.string.cz_video_player),
                        value = videoPlayerSummary(),
                        onClick = { onOpenPage(SettingsPage.VIDEO_PLAYER) },
                        iconShape = MaterialShapes.Gem.toShape(),
                        explanation = stringResource(R.string.cz_info_video_player)
                    )
                    // Always here, reading Off, rather than appearing once
                    // it is on: this row is the only way to the switch.
                    SettingsDivider()
                    SettingsHubRow(
                        icon = Icons.Rounded.AutoAwesome,
                        title = stringResource(R.string.sp_video_shaders),
                        value = videoShadersSummary(),
                        onClick = { onOpenPage(SettingsPage.VIDEO_SHADERS) },
                        iconShape = MaterialShapes.Burst.toShape(),
                        explanation = stringResource(R.string.si_video_shaders)
                    )
                }
            }
        }

        item {
            SettingsSection(title = stringResource(R.string.cz_section_layout)) {
                SettingsCard {
                    SettingsHubRow(
                        icon = Icons.Rounded.Dashboard,
                        title = stringResource(R.string.sp_home_and_navigation),
                        value = stringResource(
                            if (spotlightHome) R.string.sp_home_spotlight else R.string.sp_home_classic
                        ) + ", " + stringResource(
                            if (nonExpressiveNavigationBar) R.string.sp_nav_standard
                            else R.string.sp_nav_floating
                        ),
                        onClick = { onOpenPage(SettingsPage.HOME_NAVIGATION) },
                        tint = MaterialTheme.colorScheme.secondary,
                        iconShape = MaterialShapes.Cookie6Sided.toShape(),
                        explanation = stringResource(R.string.cz_info_home)
                    )
                    SettingsDivider()
                    SettingsHubRow(
                        icon = Icons.Rounded.VideoLibrary,
                        title = stringResource(R.string.cz_video_feed),
                        value = videoFeedSummary(),
                        onClick = { onOpenPage(SettingsPage.VIDEO_FEED) },
                        tint = MaterialTheme.colorScheme.secondary,
                        iconShape = MaterialShapes.Cookie4Sided.toShape(),
                        explanation = stringResource(R.string.cz_info_video_feed)
                    )
                    SettingsDivider()
                    SettingsHubRow(
                        icon = Icons.Rounded.TouchApp,
                        title = stringResource(R.string.settings_gestures),
                        value = stringResource(R.string.cz_gestures_value, hapticsLabel),
                        onClick = { onOpenPage(SettingsPage.GESTURES) },
                        tint = MaterialTheme.colorScheme.secondary,
                        iconShape = MaterialShapes.SoftBurst.toShape(),
                        explanation = stringResource(R.string.cz_info_gestures)
                    )
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }

    if (showLanguageSheet) {
        LanguagePickerSheet(
            onDismiss = { showLanguageSheet = false }
        )
    }
}

/**
 * The name to show on the row for [tag]: the language's own name for itself,
 * falling back to the first option when the tag is one Android can produce but
 * the picker does not offer (`tr-TR` from a device set to Turkish for Turkey).
 */
private fun languageLabel(tag: String): String {
    val twoLetters = tag.take(2)
    return AppLanguage.options.firstOrNull { it.tag.equals(twoLetters, ignoreCase = true) }
        ?.label
        ?: AppLanguage.options.first().label
}

/**
 * The language chooser, reached from Customization.
 *
 * A sheet rather than a page because the list is two rows and will stay short:
 * [AppLanguage.options] only grows when a language is translated to the end
 * first, and a whole settings page for that is more chrome than the choice
 * deserves. Each language is listed in itself, which is the same rule the
 * onboarding screen follows - the person reading the row is looking for their
 * own language, not for a word describing it.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LanguagePickerSheet(
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val current = AppLanguage.currentTag(context).take(2)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp)
                .navigationBarsPadding()
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Language,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.settings_language),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_language_sheet_sub),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            AppLanguage.options.forEach { option ->
                val selected = current.equals(option.tag, ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.secondaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            }
                        )
                        .clickable {
                            // Recreates the activity on both paths (the
                            // platform on 13+, `Activity.recreate` below it),
                            // so the sheet goes with the old configuration. It
                            // is still dismissed here for the case where the
                            // locale is already the chosen one and nothing
                            // recreates.
                            AppLanguage.apply(context, option.tag)
                            onDismiss()
                        }
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        modifier = Modifier.weight(1f)
                    )
                    if (selected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}
