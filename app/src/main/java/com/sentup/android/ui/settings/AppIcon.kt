package com.sentup.android.ui.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import com.sentup.android.MainActivity
import com.sentup.android.R
import com.sentup.android.data.SavedIconStyle

/**
 * Curated launcher icon variants. Each entry corresponds to an
 * <activity-alias> in AndroidManifest.xml.
 */
enum class AppIcon(
    val id: String,
    val aliasName: String,
    @StringRes val titleRes: Int,
    @StringRes val descRes: Int,
    val blobColor: Color,
    val noteColor: Color,
    val flagColor: Color,
    val dotColor: Color,
    val dark1: Color,
    val dark2: Color,
    val dark3: Color,
    val bgColor: Color,
    @DrawableRes val previewForegroundRes: Int,
    @DrawableRes val previewBackgroundRes: Int,
) {
    DEFAULT(
        id = "default",
        aliasName = "MainActivityDefault",
        titleRes = R.string.app_icon_classic,
        descRes = R.string.app_icon_classic_desc,
        blobColor = Color(0xFF372DA9),
        noteColor = Color(0xFFDAD8CD),
        flagColor = Color(0xFFDAD8CC),
        dotColor = Color(0xFF98EB41),
        dark1 = Color(0xFF202265),
        dark2 = Color(0xFF191346),
        dark3 = Color(0xFF1F3263),
        bgColor = Color(0xFF16133D),
        previewForegroundRes = R.drawable.ic_launcher_foreground,
        previewBackgroundRes = R.drawable.ic_launcher_background
    ),
    CRIMSON(
        id = "crimson",
        aliasName = "MainActivityCrimson",
        titleRes = R.string.app_icon_crimson,
        descRes = R.string.app_icon_crimson_desc,
        blobColor = Color(0xFFC62828),
        noteColor = Color(0xFFFFEBEE),
        flagColor = Color(0xFFFFCDD2),
        dotColor = Color(0xFFFFD700),
        dark1 = Color(0xFF8E0000),
        dark2 = Color(0xFF5F0909),
        dark3 = Color(0xFF7F0000),
        bgColor = Color(0xFF1F0606),
        previewForegroundRes = R.drawable.ic_launcher_foreground_crimson,
        previewBackgroundRes = R.drawable.ic_launcher_background_crimson
    ),
    MIDNIGHT(
        id = "midnight",
        aliasName = "MainActivityMidnight",
        titleRes = R.string.app_icon_midnight,
        descRes = R.string.app_icon_midnight_desc,
        blobColor = Color(0xFF1E1F2B),
        noteColor = Color(0xFF00E5FF),
        flagColor = Color(0xFF18FFFF),
        dotColor = Color(0xFFFF007F),
        dark1 = Color(0xFF12131C),
        dark2 = Color(0xFF0A0B10),
        dark3 = Color(0xFF171822),
        bgColor = Color(0xFF0B0C10),
        previewForegroundRes = R.drawable.ic_launcher_foreground_midnight,
        previewBackgroundRes = R.drawable.ic_launcher_background_midnight
    ),
    EMERALD(
        id = "emerald",
        aliasName = "MainActivityEmerald",
        titleRes = R.string.app_icon_emerald,
        descRes = R.string.app_icon_emerald_desc,
        blobColor = Color(0xFF1B5E20),
        noteColor = Color(0xFFE8F5E9),
        flagColor = Color(0xFFC8E6C9),
        dotColor = Color(0xFFFFEB3B),
        dark1 = Color(0xFF0E3811),
        dark2 = Color(0xFF072109),
        dark3 = Color(0xFF0D3310),
        bgColor = Color(0xFF051A08),
        previewForegroundRes = R.drawable.ic_launcher_foreground_emerald,
        previewBackgroundRes = R.drawable.ic_launcher_background_emerald
    ),
    SUNSET(
        id = "sunset",
        aliasName = "MainActivitySunset",
        titleRes = R.string.app_icon_sunset,
        descRes = R.string.app_icon_sunset_desc,
        blobColor = Color(0xFFE65100),
        noteColor = Color(0xFFFFF3E0),
        flagColor = Color(0xFFFFE0B2),
        dotColor = Color(0xFF7C4DFF),
        dark1 = Color(0xFF9C3700),
        dark2 = Color(0xFF662400),
        dark3 = Color(0xFF852E00),
        bgColor = Color(0xFF260D00),
        previewForegroundRes = R.drawable.ic_launcher_foreground_sunset,
        previewBackgroundRes = R.drawable.ic_launcher_background_sunset
    ),
    OCEAN(
        id = "ocean",
        aliasName = "MainActivityOcean",
        titleRes = R.string.app_icon_ocean,
        descRes = R.string.app_icon_ocean_desc,
        blobColor = Color(0xFF0D47A1),
        noteColor = Color(0xFFE1F5FE),
        flagColor = Color(0xFFB3E5FC),
        dotColor = Color(0xFFFF5252),
        dark1 = Color(0xFF072B64),
        dark2 = Color(0xFF03193D),
        dark3 = Color(0xFF062659),
        bgColor = Color(0xFF041630),
        previewForegroundRes = R.drawable.ic_launcher_foreground_ocean,
        previewBackgroundRes = R.drawable.ic_launcher_background_ocean
    ),
    BLOSSOM(
        id = "blossom",
        aliasName = "MainActivityBlossom",
        titleRes = R.string.app_icon_blossom,
        descRes = R.string.app_icon_blossom_desc,
        blobColor = Color(0xFF7E57C2),
        noteColor = Color(0xFFF3E5F5),
        flagColor = Color(0xFFE1BEE7),
        dotColor = Color(0xFF00E5FF),
        dark1 = Color(0xFF512DA8),
        dark2 = Color(0xFF311B92),
        dark3 = Color(0xFF4527A0),
        bgColor = Color(0xFF190E2E),
        previewForegroundRes = R.drawable.ic_launcher_foreground_blossom,
        previewBackgroundRes = R.drawable.ic_launcher_background_blossom
    ),
    MONOCHROME(
        id = "monochrome",
        aliasName = "MainActivityMonochrome",
        titleRes = R.string.app_icon_monochrome,
        descRes = R.string.app_icon_monochrome_desc,
        blobColor = Color(0xFF212121),
        noteColor = Color(0xFFFFFFFF),
        flagColor = Color(0xFFEEEEEE),
        dotColor = Color(0xFFB0BEC5),
        dark1 = Color(0xFF141414),
        dark2 = Color(0xFF0A0A0A),
        dark3 = Color(0xFF101010),
        bgColor = Color(0xFF0D0D0D),
        previewForegroundRes = R.drawable.ic_launcher_foreground_monochrome,
        previewBackgroundRes = R.drawable.ic_launcher_background_monochrome
    ),
    SOLAR(
        id = "solar",
        aliasName = "MainActivitySolar",
        titleRes = R.string.app_icon_solar,
        descRes = R.string.app_icon_solar_desc,
        blobColor = Color(0xFFF57F17),
        noteColor = Color(0xFFFFFDE7),
        flagColor = Color(0xFFFFF9C4),
        dotColor = Color(0xFF304FFE),
        dark1 = Color(0xFFA0520D),
        dark2 = Color(0xFF693305),
        dark3 = Color(0xFF8A460A),
        bgColor = Color(0xFF241200),
        previewForegroundRes = R.drawable.ic_launcher_foreground_solar,
        previewBackgroundRes = R.drawable.ic_launcher_background_solar
    );

    companion object {
        fun fromId(id: String?): AppIcon = entries.firstOrNull { it.id == id } ?: DEFAULT
    }

    /** This preset's colours as an editable style, drawn in [shape]. */
    fun style(shape: IconShape = IconShape.SQUIRCLE): IconStyle = IconStyle(
        blob = blobColor,
        note = noteColor,
        flag = flagColor,
        dot = dotColor,
        dark1 = dark1,
        dark2 = dark2,
        dark3 = dark3,
        background = bgColor,
        shape = shape,
    )
}

/**
 * The container the mark is cut into, in the preview and in a saved style.
 *
 * The `id` is what gets persisted, so these strings are frozen the same way
 * `PlayerStyle`'s constant names are: renaming one silently resets the shape of
 * every style a user has already saved. The launcher never sees this - an
 * adaptive icon is masked by the launcher's own shape - so for a preset it is
 * purely how the preview is drawn; it only becomes real data on a saved style.
 */
enum class IconShape(val id: String, @StringRes val labelRes: Int) {
    SQUIRCLE("squircle", R.string.app_icon_shape_squircle),
    CIRCLE("circle", R.string.app_icon_shape_circle),
    SQUARE("square", R.string.app_icon_shape_square),
    BURST("burst", R.string.app_icon_shape_burst),
    COOKIE("cookie", R.string.app_icon_shape_cookie),
    CLOVER("clover", R.string.app_icon_shape_clover);

    companion object {
        fun fromId(id: String?): IconShape = entries.firstOrNull { it.id == id } ?: SQUIRCLE
    }
}

/**
 * Every colour the mark is drawn from, plus its container shape.
 *
 * A preset is one of these with a name and a launcher alias behind it; anything
 * the user mixes in the studio is one of these with neither, until they save
 * it. Keeping both halves in one type is what lets the preview, the preset
 * cards and the saved-style cards all be the same composable.
 */
data class IconStyle(
    val blob: Color,
    val note: Color,
    val flag: Color,
    val dot: Color,
    val dark1: Color,
    val dark2: Color,
    val dark3: Color,
    val background: Color,
    val shape: IconShape,
)

/**
 * The preset a custom style would be mistaken for.
 *
 * A launcher icon is a static resource behind an `<activity-alias>`, so an
 * arbitrary set of colours can never become one - the honest thing a saved
 * style can do to the home screen is pick the alias that looks most like it,
 * and say which one it picked. Distance is measured over the three colours that
 * carry the identity at a glance: the blob, the ground it sits on, and the
 * accent dot. Weighting the blob highest matches what the eye does with an icon
 * a few millimetres across.
 */
/**
 * A stored style back into something drawable, and the way out again.
 *
 * The store keeps ARGB longs so it carries no UI types; these two are the only
 * place that conversion happens, so a colour cannot be packed one way in one
 * caller and another way in the next.
 */
internal fun SavedIconStyle.toIconStyle(): IconStyle = IconStyle(
    blob = Color(blob.toInt()),
    note = Color(note.toInt()),
    flag = Color(flag.toInt()),
    dot = Color(dot.toInt()),
    dark1 = Color(dark1.toInt()),
    dark2 = Color(dark2.toInt()),
    dark3 = Color(dark3.toInt()),
    background = Color(background.toInt()),
    shape = IconShape.fromId(shape),
)

internal fun IconStyle.toSaved(name: String, id: String = ""): SavedIconStyle {
    fun Color.packed(): Long = toArgb().toLong() and 0xFFFFFFFFL
    return SavedIconStyle(
        id = id,
        name = name,
        blob = blob.packed(),
        note = note.packed(),
        flag = flag.packed(),
        dot = dot.packed(),
        dark1 = dark1.packed(),
        dark2 = dark2.packed(),
        dark3 = dark3.packed(),
        background = background.packed(),
        shape = shape.id,
    )
}

fun IconStyle.nearestPreset(): AppIcon = AppIcon.entries.minByOrNull { preset ->
    fun gap(a: Color, b: Color): Float {
        val r = a.red - b.red
        val g = a.green - b.green
        val bl = a.blue - b.blue
        return r * r + g * g + bl * bl
    }
    gap(preset.blobColor, blob) * 3f +
        gap(preset.bgColor, background) * 2f +
        gap(preset.dotColor, dot)
} ?: AppIcon.DEFAULT

object AppIconManager {
    private const val TAG = "AppIconManager"

    /**
     * The package the alias *classes* live in.
     *
     * [scar] This is the module namespace, and it is not `context.packageName`.
     * The manifest writes each alias as `.MainActivityCrimson`, which the build
     * expands against the namespace (`com.sentup.android`), while the installed
     * package is the applicationId - and the debug build appends `.debug` to
     * that. So the real component is
     * `com.sentup.android.debug/com.sentup.android.MainActivityCrimson`, and
     * building the class half out of `packageName` produced
     * `com.sentup.android.debug.MainActivityCrimson`, which is not a component
     * of anything. `getComponentEnabledSetting` throws `IllegalArgumentException`
     * on an unknown component, the catch below swallowed it, and every Apply was
     * a silent no-op on debug builds while working fine on release - where the
     * two strings happen to be equal. Derived from a class in that package
     * rather than written out, so it cannot drift from the namespace.
     */
    private val aliasPackage: String =
        MainActivity::class.java.name.substringBeforeLast('.')

    private fun component(context: Context, icon: AppIcon): ComponentName =
        ComponentName(context.packageName, "$aliasPackage.${icon.aliasName}")

    /**
     * Point the launcher at [appIcon]'s alias. Returns whether it took.
     *
     * **The target is enabled before the others are disabled.** Disabling first
     * leaves a window in which the package has no enabled launcher component at
     * all, and a launcher that reads the package during it drops the app from
     * the home screen and does not reliably put it back.
     *
     * The result is returned rather than swallowed because the failure this
     * whole function once had was invisible: the caller wrote the preference and
     * showed a success toast for something that had not happened.
     */
    fun applyIcon(context: Context, appIcon: AppIcon): Boolean {
        val pm = context.packageManager
        val target = component(context, appIcon)

        val enabled = try {
            if (pm.getComponentEnabledSetting(target) !=
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            ) {
                pm.setComponentEnabledSetting(
                    target,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Could not enable $target", e)
            false
        }

        // Nothing is disabled if the target could not be enabled: an icon that
        // did not change is a much smaller problem than no icon at all.
        if (!enabled) return false

        for (icon in AppIcon.entries) {
            if (icon == appIcon) continue
            val other = component(context, icon)
            try {
                if (pm.getComponentEnabledSetting(other) !=
                    PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                ) {
                    pm.setComponentEnabledSetting(
                        other,
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                        PackageManager.DONT_KILL_APP
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Could not disable $other", e)
            }
        }
        return true
    }

    /**
     * Which alias the system currently has enabled.
     *
     * `COMPONENT_ENABLED_STATE_DEFAULT` means "whatever the manifest says", and
     * the manifest enables exactly one alias - the default one. A fresh install
     * has no overrides at all, so without that branch this walks every alias,
     * matches none, and lands on the fallback by luck rather than by reading.
     */
    fun getSystemActiveIcon(context: Context): AppIcon {
        val pm = context.packageManager
        for (icon in AppIcon.entries) {
            val state = try {
                pm.getComponentEnabledSetting(component(context, icon))
            } catch (e: Exception) {
                Log.e(TAG, "Could not read state for ${icon.aliasName}", e)
                continue
            }
            when {
                state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED -> return icon
                state == PackageManager.COMPONENT_ENABLED_STATE_DEFAULT &&
                    icon == AppIcon.DEFAULT -> return icon
            }
        }
        return AppIcon.DEFAULT
    }
}

object SentupIconPaths {
    // The mark is a tube with a lid and a play triangle inside it, drawn in the
    // same 108-unit viewport as the launcher drawables, so the preview here and
    // the installed icon are one drawing at two sizes.
    const val TUBE =
        "M56,30A12,12 0 0 1 68,42V74A12,12 0 0 1 56,86A12,12 0 0 1 44,74V42A12,12 0 0 1 56,30Z"
    const val LID =
        "M45,24H67A5,5 0 0 1 72,29A5,5 0 0 1 67,34H45A5,5 0 0 1 40,29A5,5 0 0 1 45,24Z"
    const val PLAY =
        "M49,48L49,68L63,58Z"
}

/**
 * Pure Compose vector renderer for the Sentüp App Icon.
 * Supports fluid animated color transitions, arbitrary container shapes, and ambient glow.
 */
@Composable
fun SentupAppIcon(
    style: IconStyle,
    modifier: Modifier = Modifier,
    shape: Shape,
    animateColors: Boolean = true,
) {
    SentupAppIcon(
        modifier = modifier,
        blobColor = style.blob,
        noteColor = style.note,
        flagColor = style.flag,
        containerColor = style.background,
        shape = shape,
        animateColors = animateColors,
    )
}

/**
 * The mark in three colours, matching the launcher drawables: the tube body
 * takes `noteColor`, the lid takes `flagColor`, and the play triangle takes
 * `blobColor`, the style's signature colour. `dotColor` and the three dark
 * shades stay in the signature because a saved style still carries them, but
 * the tube has no use for them.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun SentupAppIcon(
    modifier: Modifier = Modifier,
    blobColor: Color = Color(0xFF372DA9),
    noteColor: Color = Color(0xFFDAD8CD),
    flagColor: Color = Color(0xFFDAD8CC),
    dotColor: Color = Color(0xFF98EB41),
    dark1: Color = Color(0xFF202265),
    dark2: Color = Color(0xFF191346),
    dark3: Color = Color(0xFF1F3263),
    containerColor: Color? = null,
    shape: Shape = RoundedCornerShape(22.dp),
    animateColors: Boolean = true
) {
    val animBody by animateColorAsState(
        targetValue = noteColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animBody"
    )
    val animLid by animateColorAsState(
        targetValue = flagColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animLid"
    )
    val animAccent by animateColorAsState(
        targetValue = blobColor,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "animAccent"
    )

    val finalBody = if (animateColors) animBody else noteColor
    val finalLid = if (animateColors) animLid else flagColor
    val finalAccent = if (animateColors) animAccent else blobColor

    val tubePath = remember { PathParser.createPathFromPathData(SentupIconPaths.TUBE).asComposePath() }
    val lidPath = remember { PathParser.createPathFromPathData(SentupIconPaths.LID).asComposePath() }
    val playPath = remember { PathParser.createPathFromPathData(SentupIconPaths.PLAY).asComposePath() }

    val containerMod = if (containerColor != null) {
        modifier
            .clip(shape)
            .background(containerColor)
    } else {
        modifier.clip(shape)
    }

    Box(
        modifier = containerMod,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleFactor = size.minDimension / 108f
            val offsetX = (size.width - 108f * scaleFactor) / 2f
            val offsetY = (size.height - 108f * scaleFactor) / 2f

            withTransform({
                translate(offsetX, offsetY)
                scale(scaleFactor, scaleFactor, Offset.Zero)
            }) {
                drawPath(tubePath, finalBody)
                drawPath(lidPath, finalLid)
                drawPath(playPath, finalAccent)
            }
        }
    }
}
