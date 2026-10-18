package com.sentup.android.widget

import android.content.Context
import android.content.res.Configuration
import androidx.glance.color.ColorProviders
import androidx.glance.material3.ColorProviders
import com.sentup.android.data.ThemePreferences
import com.sentup.android.ui.theme.ThemeMode
import com.sentup.android.ui.theme.sentupColorScheme

/**
 * The widget family draws in Sentüp's palette, not in raw system dynamic color.
 *
 * Glance defaults to the wallpaper scheme, which is right for a widget that
 * belongs to no particular app - and wrong here, because someone who picked a
 * palette in Settings sees their app in one set of colors and its widgets in
 * another. [sentupColorScheme] is the same resolver `SentupTheme` uses, so
 * palette, AMOLED and the dynamic default all land identically on both sides.
 *
 * Light and dark are both built and handed to Glance together: the widget host
 * picks between them per its own uiMode, which is what lets one widget be right
 * in a light launcher and a dark notification shade at the same time. When the
 * app is pinned to light or dark rather than following the system, both slots
 * get the same scheme so the widget stays pinned too.
 */
internal object SentupWidgetTheme {

    fun colors(context: Context): ColorProviders {
        val prefs = ThemePreferences(context)
        val palette = prefs.colorPalette.value
        val amoled = prefs.amoledTheme.value
        val style = prefs.paletteStyle.value
        return when (prefs.themeMode.value) {
            ThemeMode.LIGHT -> {
                val scheme = sentupColorScheme(context, false, palette, amoled, style)
                ColorProviders(light = scheme, dark = scheme)
            }
            ThemeMode.DARK -> {
                val scheme = sentupColorScheme(context, true, palette, amoled, style)
                ColorProviders(light = scheme, dark = scheme)
            }
            ThemeMode.SYSTEM -> ColorProviders(
                light = sentupColorScheme(context, false, palette, amoled, style),
                dark = sentupColorScheme(context, true, palette, amoled, style),
            )
        }
    }

    /** Whether the host is currently drawing dark, for artwork scrim choices. */
    fun isDark(context: Context): Boolean {
        val prefs = ThemePreferences(context)
        return when (prefs.themeMode.value) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM ->
                (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) ==
                    Configuration.UI_MODE_NIGHT_YES
        }
    }
}
