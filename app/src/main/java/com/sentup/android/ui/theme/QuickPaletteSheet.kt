package com.sentup.android.ui.theme

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sentup.android.R

/**
 * The colour picker as one tap from the page, rather than four taps down in
 * Settings -> Appearance -> Color palette.
 *
 * [QuickPaletteButton] is the floating circle; [QuickPaletteSheet] is what it
 * opens. Both live here rather than in the home feed so the fast route owns the
 * whole feature - the feed only has to place one composable.
 *
 * The sheet writes through the same [com.sentup.android.data.ThemePreferences]
 * key the Settings screen writes, so the two routes are one setting: change the
 * palette here, open Settings later, and it is already the palette shown there.
 */
@Composable
fun QuickPaletteButton(
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    val themeViewModel: ThemeViewModel = viewModel()
    val current by themeViewModel.colorPalette.collectAsState()
    val style by themeViewModel.paletteStyle.collectAsState()
    val amoled by themeViewModel.amoledTheme.collectAsState()
    // The neutral ramp does not move with the palette seed, so the page tone is
    // a truthful answer to "is this app dark or light" - which is exactly what
    // the previews below need to build the schemes they show.
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    Box(
        modifier = modifier.quickPress {
            open = true
        }
    ) {
        Box(
            modifier = Modifier
                .sentupCard(shape = RoundedCornerShape(SentupRadii.pill), elevation = 1, tinted = false)
                .size(44.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Palette,
                contentDescription = stringResource(R.string.cqp_open),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(21.dp)
            )
            // The dot on the corner answers "which colour is this right now"
            // without another line of text in a row that already carries a 34sp
            // greeting. Read live rather than rebuilt from the palette id: it is
            // the accent the app is actually wearing, which is the honest answer
            // even when album colours are overriding the palette app-wide.
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape)
            )
        }
    }

    if (open) {
        QuickPaletteSheet(
            currentPalette = current,
            paletteStyle = style,
            amoledDark = amoled,
            darkTheme = dark,
            onSelect = { id ->
                themeViewModel.setColorPalette(id)
                open = false
            },
            onDismiss = { open = false }
        )
    }
}

/**
 * One tap changes the app's colour and closes the sheet.
 *
 * Every circle is filled with that palette's *real* generated primary under the
 * palette style currently in force, so a swatch is a promise the theme keeps -
 * not the seed it was built from, which is not what the app ends up wearing.
 * The current palette is ringed in its own colour with a check inside it; the
 * ring sits on a theme-coloured halo, because Graphite's and Black's own
 * primary is the one colour that disappears against a dark sheet.
 *
 * Wallpaper leads as a full-width row, the way the Settings palette screen
 * gives it a hero: it is the only option that is not a circle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickPaletteSheet(
    currentPalette: String,
    paletteStyle: PaletteStyle,
    amoledDark: Boolean,
    darkTheme: Boolean,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    // Not the older rememberModalBottomSheetState: it is deprecated in this
    // Material version, and its replacement states the sheet starts Hidden
    // outright, which is what skipPartiallyExpanded meant here.
    val sheetState = rememberBottomSheetState(initialValue = SheetValue.Hidden)

    // Built once per dark/style/wallpaper change, the way the Settings palette
    // screen does it: HCT generation is not free, and a wallpaper read is not
    // free either.
    val wallpaperPrimary = remember(context, configuration, darkTheme, amoledDark, paletteStyle) {
        sentupColorScheme(context, darkTheme, DYNAMIC_PALETTE_ID, amoledDark, paletteStyle).primary
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        // A light dim, not a blackout: the page behind is still the page, and
        // seeing it dimmed is what says a panel is floating above it.
        scrimColor = Color.Black.copy(alpha = 0.32f),
        shape = QuickSheetShape,
        // The surface is painted by sentupCard below, so the sheet's own
        // background stays transparent and the panel keeps the app's depth.
        containerColor = Color.Transparent,
        contentColor = scheme.onSurface,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = (configuration.screenHeightDp * 0.72f).dp)
                // Clipped here as well as by the sheet's shape: the panel's own
                // background is drawn by the Column, and a gradient that squared
                // off the top corners would undo the panel.
                .clip(QuickSheetShape)
                .sentupCard(shape = QuickSheetShape, elevation = 2, tinted = true)
                .sentupSpecularEdge(QuickSheetShape)
        ) {
            Column(
                modifier = Modifier.padding(
                    start = SentupGroup.inset + SentupGroup.dividerInset,
                    end = SentupGroup.inset + SentupGroup.dividerInset,
                    top = 22.dp,
                    bottom = 14.dp
                )
            ) {
                Text(
                    text = stringResource(R.string.sp_color_palette),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = scheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.cqp_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = scheme.onSurfaceVariant
                )
            }

            LazyVerticalGrid(
                // 3 columns on a phone, 4 once there is room: the name under
                // each circle has to survive at the app's largest font scale,
                // which a fixed 4-column grid cannot promise.
                columns = GridCells.Adaptive(96.dp),
                modifier = Modifier.weight(1f, fill = false),
                contentPadding = PaddingValues(
                    start = SentupGroup.inset,
                    end = SentupGroup.inset,
                    top = 2.dp,
                    bottom = 28.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item(span = { GridItemSpan(maxLineSpan) }, key = "dynamic") {
                    WallpaperPaletteRow(
                        selected = currentPalette == DYNAMIC_PALETTE_ID,
                        primary = wallpaperPrimary,
                        onClick = { onSelect(DYNAMIC_PALETTE_ID) }
                    )
                }
                items(APP_PALETTES, key = { it.id }) { palette ->
                    val fill = remember(palette.id, darkTheme, paletteStyle, amoledDark) {
                        buildPaletteColorScheme(palette, darkTheme, paletteStyle)
                            .let { if (darkTheme && amoledDark) it.toAmoled() else it }
                            .primary
                    }
                    PaletteSwatch(
                        fill = fill,
                        name = quickPaletteName(palette.id),
                        selected = currentPalette == palette.id,
                        onClick = { onSelect(palette.id) }
                    )
                }
            }
        }
    }
}

/**
 * Wallpaper colour, as one full-width row above the circles. It is not one
 * colour, so it cannot be a swatch - the row carries the live wallpaper accent
 * and the same check the circles use.
 */
@Composable
private fun WallpaperPaletteRow(
    selected: Boolean,
    primary: Color,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(90),
        label = "quickWallpaperPress"
    )
    val name = stringResource(R.string.theme_palette_dynamic)
    val description = stringResource(R.string.cp_dynamic_cd)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale)
            .clip(RoundedCornerShape(SentupRadii.card))
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (selected) 0.55f else 0.16f
                        ),
                        CircleShape
                    )
            )
            if (selected) {
                Box(Modifier.size(46.dp).border(3.dp, primary, CircleShape))
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(primary)
                    .sentupSpecularEdge(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = contrastInk(primary),
                    modifier = Modifier.size(19.dp)
                )
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AnimatedVisibility(
            visible = selected,
            enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
        ) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.cqp_selected_cd, name),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/**
 * One palette: its real primary as a circle, its name under it.
 *
 * The selection is three things at once - a ring in the palette's own colour,
 * a check inside it in [contrastInk] so it reads on Black and White too, and a
 * bold label. One of the three is enough on a light page; on a dark one a dark
 * palette needs all three to be visible at a glance.
 */
@Composable
private fun PaletteSwatch(
    fill: Color,
    name: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(90),
        label = "quickSwatchPress"
    )

    Column(
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(SentupRadii.tile))
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .semantics { contentDescription = name }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(46.dp),
            contentAlignment = Alignment.Center
        ) {
            // The halo first, so a dark palette's own ring has something to
            // sit against on a dark panel.
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurface.copy(
                            alpha = if (selected) 0.55f else 0.16f
                        ),
                        CircleShape
                    )
            )
            if (selected) {
                Box(Modifier.size(46.dp).border(3.dp, fill, CircleShape))
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(fill)
                    // The lit top edge is what keeps a near-black circle from
                    // being a hole in a dark panel.
                    .sentupSpecularEdge(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                // Fully qualified: inside this Column the unqualified name
                // resolves to the ColumnScope overload, which cannot pop a
                // single icon.
                androidx.compose.animation.AnimatedVisibility(
                    visible = selected,
                    enter = scaleIn(spring(dampingRatio = Spring.DampingRatioMediumBouncy)) + fadeIn()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = contrastInk(fill),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * A press that goes inward - 0.97, no ripple, no colour flash - the same
 * contract as the feed's own tiles, so the two feel like one app.
 */
@Composable
private fun Modifier.quickPress(onClick: () -> Unit): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(90),
        label = "quickPalettePress"
    )
    return this
        .scale(scale)
        .selectable(
            selected = false,
            role = Role.Button,
            interactionSource = interactionSource,
            indication = null,
            onClick = onClick
        )
}

/** The panel: large corners at the top, square where it meets the screen. */
private val QuickSheetShape = RoundedCornerShape(
    topStart = SentupRadii.panel,
    topEnd = SentupRadii.panel
)

/**
 * Palette names in the reader's language.
 *
 * [AppPalette.name] is left alone on purpose: it is the English label the
 * Settings palette screen already reads, and changing that field would touch
 * every call site for no gain. This route resolves the id to a resource instead.
 */
private val QUICK_PALETTE_NAMES: Map<String, Int> = mapOf(
    "electric" to R.string.cqp_electric,
    "magenta" to R.string.cqp_magenta,
    "citrus" to R.string.cqp_citrus,
    "neon" to R.string.cqp_neon,
    "lavender" to R.string.cqp_lavender,
    "mint" to R.string.cqp_mint,
    "peach" to R.string.cqp_peach,
    "sky" to R.string.cqp_sky,
    "vintagefilm" to R.string.cqp_vintage_film,
    "dustyrose" to R.string.cqp_dusty_rose,
    "sagesand" to R.string.cqp_sage_sand,
    "fadeddenim" to R.string.cqp_faded_denim,
    "oatlatte" to R.string.cqp_oat_latte,
    "sunbleached" to R.string.cqp_sun_bleached,
    "terracotta" to R.string.cqp_terracotta,
    "forest" to R.string.cqp_forest,
    "mocha" to R.string.cqp_mocha,
    "autumn" to R.string.cqp_autumn,
    "crimson" to R.string.cqp_crimson,
    "indigo" to R.string.cqp_indigo,
    "deepteal" to R.string.cqp_deep_teal,
    "plum" to R.string.cqp_plum,
    "emerald" to R.string.cqp_emerald,
    "ocean" to R.string.cqp_ocean,
    "rosegold" to R.string.cqp_rose_gold,
    "graphite" to R.string.cqp_graphite,
    "black" to R.string.cqp_black,
    "white" to R.string.cqp_white
)

/** The palette's name in the current language; wallpaper and unknown ids alike. */
@Composable
private fun quickPaletteName(id: String): String =
    stringResource(QUICK_PALETTE_NAMES[id] ?: R.string.cqp_other)