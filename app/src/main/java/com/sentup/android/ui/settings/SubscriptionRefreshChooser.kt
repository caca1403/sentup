package com.sentup.android.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.sentup.android.R
import com.sentup.android.data.ThemePreferences

@Composable
private fun refreshLabel(minutes: Int): String = when (minutes) {
    ThemePreferences.SUBS_REFRESH_ON_OPEN -> stringResource(R.string.sp_subs_refresh_on_open)
    ThemePreferences.SUBS_REFRESH_MANUAL -> stringResource(R.string.sp_subs_refresh_manual)
    else -> stringResource(R.string.sp_subs_refresh_hours, minutes / 60)
}

/**
 * How often the followed channels are pulled again.
 *
 * Chips were the first shape and they read as filters - six pills in a row that
 * wrap wherever the text is long, and the chosen one is a filled pill nobody can
 * read at a glance. The intervals are an ordered ladder from "always" to "never",
 * so they are rows in the card the setting already lives in, with the check on
 * the rung that is chosen.
 */
@Composable
internal fun SubscriptionRefreshChooser(selected: Int, onSelect: (Int) -> Unit) {
    val options = ThemePreferences.SUBS_REFRESH_OPTIONS
    SettingsChoiceRow(
        icon = Icons.Rounded.Schedule,
        title = stringResource(R.string.sp_subs_refresh),
        subtitle = stringResource(R.string.sp_subs_refresh_sub),
        labels = options.map { refreshLabel(it) },
        selectedIndex = options.indexOf(selected).coerceAtLeast(0),
        onSelect = { onSelect(options[it]) }
    )
}
