package com.sentup.android.ui.onboarding
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.ui.res.stringResource
import com.sentup.android.R

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.ToggleOn
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Wallpaper
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.graphics.shapes.Morph
import androidx.graphics.shapes.RoundedPolygon
import androidx.graphics.shapes.toPath
import com.sentup.android.data.PlayerStyle
import com.sentup.android.data.AppLanguage
import com.sentup.android.ui.components.rememberPermissionState
import com.sentup.android.ui.home.HomeStylePicker
import com.sentup.android.ui.player.PlayerStylePicker
import com.sentup.android.data.SessionManager
import com.sentup.android.ui.auth.YouTubeAuthDialog
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage
import com.sentup.android.ui.theme.ThemeMode
import kotlin.math.absoluteValue
import kotlinx.coroutines.launch

private const val ONBOARDING_PAGE_COUNT = 7

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun OnboardingScreen(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    loadLocalSongs: Boolean,
    onLoadLocalSongsToggle: (Boolean) -> Unit,
    ambientBackground: Boolean,
    onAmbientBackgroundToggle: (Boolean) -> Unit,
    videoMode: Boolean,
    onVideoModeToggle: (Boolean) -> Unit,
    homeModeToggleEnabled: Boolean = true,
    onHomeModeToggleEnabledChange: (Boolean) -> Unit = {},
    shortsEnabled: Boolean = false,
    onShortsEnabledToggle: (Boolean) -> Unit = {},
    spotlightHome: Boolean = false,
    onSpotlightHomeChange: (Boolean) -> Unit = {},
    playerStyle: PlayerStyle,
    onPlayerStyleChange: (PlayerStyle) -> Unit,
    crossfadeEnabled: Boolean,
    onCrossfadeEnabledToggle: (Boolean) -> Unit,
    manualScanEnabled: Boolean,
    onManualScanEnabledToggle: (Boolean) -> Unit,
    onFinish: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { ONBOARDING_PAGE_COUNT })
    val sessionManager = remember { SessionManager(context) }
    var isLoggedIn by remember { mutableStateOf(sessionManager.isLoggedIn()) }
    var showAuthDialog by remember { mutableStateOf(false) }

    val storagePermissionState = rememberPermissionState(
        permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
    )
    val notificationPermissionState = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)
    } else {
        null
    }

    if (showAuthDialog) {
        YouTubeAuthDialog(
            onDismiss = { showAuthDialog = false },
            onAuthSuccess = {
                isLoggedIn = true
                showAuthDialog = false
            }
        )
    }

    val isLastPage = pagerState.currentPage == ONBOARDING_PAGE_COUNT - 1

    // The page, not a wallpaper: a vertical wash that gets deeper towards the
    // bottom, so the eye reads it as a surface it is looking down at. The old
    // version painted two large translucent circles over a diagonal gradient,
    // which is what made this screen feel borrowed rather than designed.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .sentupPage()
    ) {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp)
                .padding(top = 8.dp, bottom = 20.dp)
        ) {
            // Top bar: wordmark + skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(PolygonShape(MaterialShapes.Cookie9Sided))
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "Sentüp",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                AnimatedVisibility(
                    visible = !isLastPage,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    TextButton(onClick = onFinish) {
                        Text(stringResource(R.string.ob_skip))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
                beyondViewportPageCount = 1,
                verticalAlignment = Alignment.Top
            ) { page ->
                val pageOffset =
                    (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                Box(
                    modifier = Modifier.graphicsLayer {
                        val fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f)
                        alpha = 0.3f + 0.7f * fraction
                        val scale = 0.94f + 0.06f * fraction
                        scaleX = scale
                        scaleY = scale
                    }
                ) {
                    when (page) {
                        0 -> WelcomePage()
                        1 -> LibraryPage(
                            loadLocalSongs = loadLocalSongs,
                            onLoadLocalSongsToggle = onLoadLocalSongsToggle,
                            storagePermissionGranted = storagePermissionState.isGranted,
                            onRequestStoragePermission = { storagePermissionState.launchPermissionRequest() }
                        )
                        2 -> YouTubePage(
                            isLoggedIn = isLoggedIn,
                            onConnectYouTube = { showAuthDialog = true }
                        )
                        3 -> VideoModePage(
                            videoMode = videoMode,
                            shortsEnabled = shortsEnabled,
                            onShortsEnabledToggle = onShortsEnabledToggle,
                            onVideoModeToggle = onVideoModeToggle,
                            homeModeToggleEnabled = homeModeToggleEnabled,
                            onHomeModeToggleEnabledChange = onHomeModeToggleEnabledChange
                        )
                        4 -> LookAndFeelPage(
                            currentThemeMode = currentThemeMode,
                            onThemeModeChange = onThemeModeChange,
                            ambientBackground = ambientBackground,
                            onAmbientBackgroundToggle = onAmbientBackgroundToggle,
                            spotlightHome = spotlightHome,
                            onSpotlightHomeChange = onSpotlightHomeChange,
                            playerStyle = playerStyle,
                            onPlayerStyleChange = onPlayerStyleChange
                        )
                        5 -> FinalTouchesPage(
                            crossfadeEnabled = crossfadeEnabled,
                            onCrossfadeEnabledToggle = onCrossfadeEnabledToggle,
                            notificationPermissionGranted = notificationPermissionState?.isGranted ?: true,
                            onRequestNotificationPermission = { notificationPermissionState?.launchPermissionRequest() },
                            manualScanEnabled = manualScanEnabled,
                            onManualScanEnabledToggle = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
                                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                                onManualScanEnabledToggle(enabled)
                            }
                        )
                        else -> AppTimeLimitPage()
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bottom bar: wavy progress + navigation
            val progress by animateFloatAsState(
                targetValue = (pagerState.currentPage + 1f) / ONBOARDING_PAGE_COUNT,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "onboardingProgress"
            )

            Text(
                text = "Step ${pagerState.currentPage + 1} of $ONBOARDING_PAGE_COUNT",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            LinearWavyProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AnimatedVisibility(
                    visible = pagerState.currentPage > 0,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut()
                ) {
                    FilledTonalIconButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            }
                        },
                        shapes = IconButtonDefaults.shapes(),
                        modifier = Modifier.size(56.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        if (isLastPage) {
                            onFinish()
                        } else {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        }
                    },
                    shapes = ButtonDefaults.shapes(),
                    modifier = Modifier.height(56.dp),
                    contentPadding = PaddingValues(horizontal = 28.dp)
                ) {
                    Text(
                        text = if (isLastPage) stringResource(R.string.ob_start_listening) else stringResource(R.string.ob_continue),
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (isLastPage) Icons.Rounded.CheckCircle else Icons.AutoMirrored.Rounded.ArrowForward,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

// ---------------- Pages ----------------

/**
 * The first thing anyone sees, and the only page with nothing to set up.
 *
 * **A floating card, not a full-bleed illustration.** Every other page in this
 * flow is a stack of controls on the page, so the one screen that asks for no
 * input is the one that shows the mark as an object: a lit tube sitting on a
 * card that is a few millimetres above the page, which is the same trick the
 * player and every sheet use. One depth language for the whole app, starting on
 * the first screen.
 *
 * **Language before anything else.** The translations ship in the APK and the
 * phone's own language is the better guess, but a guess is still a guess -
 * someone whose phone is in English reading a Turkish app has to find this
 * setting before they can read a word of it. So the row sits here, under the
 * mark, and every language is written in itself.
 *
 * No list of features: those are the pages after this one, where each can be
 * *used* rather than read about.
 */
@Composable
private fun WelcomePage() {
    val context = LocalContext.current
    var selectedTag by remember { mutableStateOf(AppLanguage.currentTag(context)) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .sentupCard(shape = RoundedCornerShape(SentupRadii.panel), elevation = 1)
                .padding(vertical = 40.dp),
            contentAlignment = Alignment.Center
        ) {
            SentupTubeMark()
        }

        Spacer(modifier = Modifier.height(28.dp))

        LanguageRow(
            selectedTag = selectedTag,
            onSelect = { tag ->
                selectedTag = tag
                AppLanguage.apply(context, tag)
            }
        )

        Spacer(modifier = Modifier.height(30.dp))

        Text(
            text = stringResource(R.string.ob_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.ob_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

/**
 * The Sentüp mark: a tube, a lid, and five bars of sound moving inside it.
 *
 * Drawn rather than assembled out of icons, so what is on this screen is the
 * mark the launcher uses - same proportions, same colours - and so it can move.
 * Three details are what make it read as a solid rather than a diagram: the
 * bright hairline along the top of the glass, the shadow under the tube, and
 * the bars sitting *inside* the outline instead of beside it.
 */
@Composable
private fun SentupTubeMark(modifier: Modifier = Modifier) {
    val primary = MaterialTheme.colorScheme.primary
    val onPrimary = MaterialTheme.colorScheme.onPrimary
    val transition = rememberInfiniteTransition(label = "sentupMark")

    // Every bar has its own period, so the group never lines up into one pulse
    // - it reads as a wave travelling across the tube.
    val bars = List(5) { index ->
        transition.animateFloat(
            initialValue = 0.28f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 760 + index * 155, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "bar$index"
        )
    }
    val glow = transition.animateFloat(
        initialValue = 0.40f,
        targetValue = 0.72f,
        animationSpec = infiniteRepeatable(
            animation = tween(2800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Canvas(modifier = modifier.size(168.dp)) {
        val width = size.width
        val height = size.height
        val center = Offset(width / 2f, height / 2f)

        val tubeWidth = width * 0.46f
        val tubeHeight = height * 0.64f
        val left = (width - tubeWidth) / 2f
        val top = center.y - tubeHeight / 2f + height * 0.04f
        val radius = tubeWidth / 2f

        // The glow sits behind the glass rather than around the icon, so the
        // mark reads as lit from inside instead of ringed by a halo.
        val glowRadius = width * 0.60f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primary.copy(alpha = 0.34f * glow.value), Color.Transparent),
                center = center,
                radius = glowRadius
            ),
            radius = glowRadius,
            center = center
        )

        // Lid, sitting proud of the tube the way the launcher mark draws it.
        val lidWidth = tubeWidth * 0.82f
        val lidHeight = height * 0.075f
        drawRoundRect(
            color = primary.copy(alpha = 0.95f),
            topLeft = Offset(center.x - lidWidth / 2f, top - lidHeight * 1.5f),
            size = Size(lidWidth, lidHeight),
            cornerRadius = CornerRadius(lidHeight / 2f)
        )
        // A lighter cap on the lid: the one edge that catches the light.
        drawRoundRect(
            color = onPrimary.copy(alpha = 0.35f),
            topLeft = Offset(center.x - lidWidth / 2f, top - lidHeight * 1.5f),
            size = Size(lidWidth, lidHeight * 0.34f),
            cornerRadius = CornerRadius(lidHeight / 2f)
        )

        // Glass: a wash that is brighter at the top, so the surface has a light
        // source and the outline is not the only thing describing it.
        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(primary.copy(alpha = 0.30f), primary.copy(alpha = 0.10f)),
                startY = top,
                endY = top + tubeHeight
            ),
            topLeft = Offset(left, top),
            size = Size(tubeWidth, tubeHeight),
            cornerRadius = CornerRadius(radius)
        )
        drawRoundRect(
            color = primary.copy(alpha = 0.55f),
            topLeft = Offset(left, top),
            size = Size(tubeWidth, tubeHeight),
            cornerRadius = CornerRadius(radius),
            style = Stroke(width = 2.dp.toPx())
        )
        // Specular edge: a short bright run along the upper-left of the glass,
        // clipped to the tube so it can never draw outside the silhouette.
        clipRect(left, top, left + tubeWidth, top + tubeHeight) {
            drawArc(
                color = onPrimary.copy(alpha = 0.38f),
                startAngle = 190f,
                sweepAngle = 120f,
                useCenter = false,
                topLeft = Offset(left, top),
                size = Size(tubeWidth, tubeHeight),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        // The sound itself, inside the glass.
        val barWidth = tubeWidth * 0.115f
        val gap = tubeWidth * 0.06f
        val totalWidth = bars.size * barWidth + (bars.size - 1) * gap
        val baseline = top + tubeHeight - tubeHeight * 0.18f
        var x = left + (tubeWidth - totalWidth) / 2f
        bars.forEachIndexed { index, fraction ->
            val barHeight = tubeHeight * 0.48f * fraction.value
            drawRoundRect(
                color = if (index % 2 == 0) onPrimary else onPrimary.copy(alpha = 0.68f),
                topLeft = Offset(x, baseline - barHeight),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f)
            )
            x += barWidth + gap
        }
    }
}

/** The mark's own corner radius, so its shadow matches its silhouette. */
private val tubeCorner = 44.dp

/**
 * The language row: a globe and the offered languages, each written in itself.
 *
 * A wrapping flow rather than one line or a dropdown: six languages do not fit
 * across a narrow phone, and truncating "Español" to "Espa" is worse than
 * letting the row fold. A chooser that opens a second screen just to say
 * "English" is also a worse first impression than six words readable at a
 * glance.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LanguageRow(
    selectedTag: String,
    onSelect: (String) -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Language,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(15.dp)
            )
            Text(
                text = stringResource(R.string.ob_language),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.Center,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxItemsInEachRow = 3
        ) {
            AppLanguage.options.forEach { option ->
                val selected = selectedTag.take(2).equals(option.tag, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(SentupRadii.pill),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(SentupRadii.pill))
                        .clickable { onSelect(option.tag) }
                ) {
                    Text(
                        text = option.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) {
                            MaterialTheme.colorScheme.onPrimary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun LibraryPage(
    loadLocalSongs: Boolean,
    onLoadLocalSongsToggle: (Boolean) -> Unit,
    storagePermissionGranted: Boolean,
    onRequestStoragePermission: () -> Unit
) {
    OnboardingPageScaffold(
        icon = Icons.Rounded.Folder,
        iconShape = MaterialShapes.Circle,
        title = stringResource(R.string.ob_page_your_music),
        body = stringResource(R.string.ob_page_your_music_body)
    ) {
        SettingSwitchRow(
            icon = Icons.Rounded.Folder,
            title = stringResource(R.string.ob_scan_device_audio),
            subtitle = stringResource(R.string.ob_scan_device_audio_sub),
            checked = loadLocalSongs,
            onCheckedChange = onLoadLocalSongsToggle
        )

        AnimatedVisibility(visible = loadLocalSongs) {
            PermissionRow(
                icon = Icons.Rounded.Album,
                title = stringResource(R.string.ob_music_access),
                subtitle = if (storagePermissionGranted) {
                    stringResource(R.string.ob_audio_access_allowed)
                } else {
                    stringResource(R.string.ob_audio_access_needed)
                },
                granted = storagePermissionGranted,
                actionLabel = stringResource(R.string.ob_allow),
                onAction = onRequestStoragePermission
            )
        }

        HintText(stringResource(R.string.ob_hint_only_audio))
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun YouTubePage(
    isLoggedIn: Boolean,
    onConnectYouTube: () -> Unit
) {
    OnboardingPageScaffold(
        icon = Icons.Rounded.CloudSync,
        iconShape = MaterialShapes.Flower,
        title = stringResource(R.string.settings_section_youtube_music),
        body = stringResource(R.string.ob_page_ytmusic_body)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isLoggedIn) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.secondaryContainer
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isLoggedIn) Icons.Rounded.Check else Icons.Rounded.CloudSync,
                            contentDescription = null,
                            tint = if (isLoggedIn) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Column {
                        Text(
                            text = if (isLoggedIn) stringResource(R.string.ob_account_connected) else stringResource(R.string.ob_not_connected),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isLoggedIn) {
                                stringResource(R.string.ob_recs_ready)
                            } else {
                                stringResource(R.string.ob_optional_sign_in_later)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (!isLoggedIn) {
                    Button(
                        onClick = onConnectYouTube,
                        shapes = ButtonDefaults.shapes(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Text(stringResource(R.string.sp_connect_youtube_music))
                    }
                }
            }
        }

        HintText(stringResource(R.string.ob_hint_session_secure))
    }
}

@Composable
private fun VideoModePage(
    videoMode: Boolean,
    onVideoModeToggle: (Boolean) -> Unit,
    homeModeToggleEnabled: Boolean,
    onHomeModeToggleEnabledChange: (Boolean) -> Unit,
    shortsEnabled: Boolean,
    onShortsEnabledToggle: (Boolean) -> Unit
) {
    OnboardingPageScaffold(
        icon = Icons.Rounded.Videocam,
        iconShape = MaterialShapes.Arch,
        title = stringResource(R.string.ob_page_video_mode),
        body = stringResource(R.string.ob_page_video_mode_body)
    ) {
        SettingSwitchRow(
            icon = Icons.Rounded.Videocam,
            title = stringResource(R.string.ob_enable_video_mode),
            subtitle = stringResource(R.string.ob_enable_video_mode_sub),
            checked = videoMode,
            onCheckedChange = onVideoModeToggle
        )

        SettingSwitchRow(
            icon = Icons.Rounded.ToggleOn,
            title = stringResource(R.string.ob_home_switch),
            subtitle = stringResource(R.string.ob_home_switch_sub),
            checked = homeModeToggleEnabled,
            onCheckedChange = onHomeModeToggleEnabledChange
        )

        SettingSwitchRow(
            icon = Icons.Rounded.Bolt,
            title = stringResource(R.string.sp_shorts),
            subtitle = stringResource(R.string.ob_shorts_sub),
            checked = shortsEnabled,
            onCheckedChange = onShortsEnabledToggle
        )

        ShortsWarningCard()

        HintText(stringResource(R.string.ob_off_hint))
    }
}

/**
 * Deliberate friction before enabling the Shorts swipe player: endless
 * short-form feeds are engineered to be compulsive, so Sentüp ships it off and
 * warns before the user opts in.
 */
@Composable
private fun ShortsWarningCard() {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.errorContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.WarningAmber,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = stringResource(R.string.ob_shorts_warning),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun LookAndFeelPage(
    currentThemeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    ambientBackground: Boolean,
    onAmbientBackgroundToggle: (Boolean) -> Unit,
    spotlightHome: Boolean,
    onSpotlightHomeChange: (Boolean) -> Unit,
    playerStyle: PlayerStyle,
    onPlayerStyleChange: (PlayerStyle) -> Unit
) {
    OnboardingPageScaffold(
        icon = Icons.Rounded.Palette,
        iconShape = MaterialShapes.Diamond,
        title = stringResource(R.string.ob_page_look_and_feel),
        body = stringResource(R.string.ob_look_and_feel_body)
    ) {
        val systemLabel = stringResource(R.string.theme_system)
        val darkLabel = stringResource(R.string.theme_dark)
        val lightLabel = stringResource(R.string.theme_light)
        ChoiceCard(title = stringResource(R.string.sp_theme)) {
            ConnectedChoiceGroup(
                options = listOf(
                    ThemeMode.SYSTEM to systemLabel,
                    ThemeMode.DARK to darkLabel,
                    ThemeMode.LIGHT to lightLabel
                ),
                selected = currentThemeMode,
                onSelect = onThemeModeChange
            )
        }

        SettingSwitchRow(
            icon = Icons.Rounded.Wallpaper,
            title = stringResource(R.string.ob_ambient_artwork),
            subtitle = stringResource(R.string.ob_ambient_artwork_sub),
            checked = ambientBackground,
            onCheckedChange = onAmbientBackgroundToggle
        )

        ChoiceCard(title = stringResource(R.string.ob_home_screen_card)) {
            Text(
                text = stringResource(R.string.ob_home_screen_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            HomeStylePicker(
                spotlightHome = spotlightHome,
                onSpotlightHomeChange = onSpotlightHomeChange
            )
        }

        ChoiceCard(title = stringResource(R.string.ob_player_style_card)) {
            Text(
                text = stringResource(R.string.ob_player_style_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            PlayerStylePicker(
                currentStyle = playerStyle,
                onStyleSelected = onPlayerStyleChange
            )
        }
    }
}

@Composable
private fun FinalTouchesPage(
    crossfadeEnabled: Boolean,
    onCrossfadeEnabledToggle: (Boolean) -> Unit,
    notificationPermissionGranted: Boolean,
    onRequestNotificationPermission: () -> Unit,
    manualScanEnabled: Boolean,
    onManualScanEnabledToggle: (Boolean) -> Unit
) {
    OnboardingPageScaffold(
        icon = Icons.Rounded.Tune,
        iconShape = MaterialShapes.Pill,
        title = stringResource(R.string.ob_page_final_touches),
        body = stringResource(R.string.ob_page_final_touches_body)
    ) {
        SettingSwitchRow(
            icon = Icons.Rounded.GraphicEq,
            title = stringResource(R.string.ob_crossfade),
            subtitle = stringResource(R.string.ob_crossfade_sub),
            checked = crossfadeEnabled,
            onCheckedChange = onCrossfadeEnabledToggle
        )

        PermissionRow(
            icon = Icons.Rounded.NotificationsActive,
            title = stringResource(R.string.ob_playback_notifications),
            subtitle = if (notificationPermissionGranted) {
                stringResource(R.string.ob_notif_ready)
            } else {
                stringResource(R.string.ob_notif_enable)
            },
            granted = notificationPermissionGranted,
            actionLabel = stringResource(R.string.ob_enable),
            onAction = onRequestNotificationPermission
        )

        SettingSwitchRow(
            icon = Icons.Rounded.PhoneAndroid,
            title = stringResource(R.string.ob_high_compat_scan),
            subtitle = stringResource(R.string.ob_high_compat_scan_sub),
            checked = manualScanEnabled,
            onCheckedChange = onManualScanEnabledToggle
        )
    }
}

/**
 * The last onboarding page: the optional daily time limit. Off by default;
 * enabling it here applies one budget to every weekday, and the per-day
 * editor lives in Settings > Advanced for anyone who wants finer control.
 *
 * Owns its [ThemePreferences] directly, like this screen already does for
 * SessionManager - nothing else needs to react to the choice while the page
 * is up.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun AppTimeLimitPage() {
    val context = LocalContext.current
    val prefs = remember { com.sentup.android.data.ThemePreferences(context) }
    var enabled by remember { mutableStateOf(prefs.isTimeLimitEnabled()) }
    var dailyMinutes by remember {
        val budgets =
            com.sentup.android.data.AppTimeLimit.parseBudgets(prefs.getTimeLimitBudgets())
        val uniform = budgets.values.distinct().singleOrNull()
        mutableStateOf(uniform ?: com.sentup.android.data.AppTimeLimit.DEFAULT_DAILY_MINUTES)
    }

    fun applyPreset(minutes: Int) {
        dailyMinutes = minutes
        prefs.setAllTimeLimitBudgets(minutes)
        if (!enabled) {
            enabled = true
            prefs.setTimeLimitEnabled(true)
        }
    }

    OnboardingPageScaffold(
        icon = Icons.Rounded.Bedtime,
        iconShape = MaterialShapes.SoftBurst,
        title = stringResource(R.string.ob_page_daily_limit),
        body = stringResource(R.string.ob_page_daily_limit_body)
    ) {
        SettingSwitchRow(
            icon = Icons.Rounded.Bedtime,
            title = stringResource(R.string.ob_limit_my_listening),
            subtitle = stringResource(R.string.ob_limit_my_listening_sub),
            checked = enabled,
            onCheckedChange = { on ->
                enabled = on
                prefs.setTimeLimitEnabled(on)
                if (on) prefs.setAllTimeLimitBudgets(dailyMinutes)
            }
        )

        AnimatedVisibility(visible = enabled) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = stringResource(R.string.ob_every_day),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(60, 120, 180, 300, 480).forEach { minutes ->
                        FilterChip(
                            selected = dailyMinutes == minutes,
                            onClick = { applyPreset(minutes) },
                            label = {
                                Text(
                                    com.sentup.android.data.AppTimeLimit.formatBudget(minutes)
                                )
                            }
                        )
                    }
                }
                Text(
                    text = stringResource(R.string.ob_hint_weekday_hours),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// ---------------- Building blocks ----------------

@Composable
private fun OnboardingPageScaffold(
    icon: ImageVector,
    iconShape: RoundedPolygon,
    title: String,
    body: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        ShapeIconBadge(icon = icon, shape = iconShape)

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        content()
    }
}

/** Hero for the welcome page: a shape morphing between two expressive forms. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
private fun MorphingHero(icon: ImageVector) {
    val transition = rememberInfiniteTransition(label = "heroMorph")
    val morphProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "morphProgress"
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(45000, easing = LinearEasing)
        ),
        label = "heroRotation"
    )
    val morph = remember { Morph(MaterialShapes.Cookie9Sided, MaterialShapes.SoftBurst) }

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .graphicsLayer { rotationZ = rotation }
                .clip(MorphShape(morph, morphProgress))
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(56.dp)
        )
    }
}

private class PolygonShape(private val polygon: RoundedPolygon) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = polygon.toPath().asComposePath()
        val matrix = Matrix()
        val bounds = polygon.calculateBounds()
        val boundsWidth = bounds[2] - bounds[0]
        val boundsHeight = bounds[3] - bounds[1]

        matrix.scale(size.width / boundsWidth, size.height / boundsHeight)
        matrix.translate(-bounds[0], -bounds[1])
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

private class MorphShape(
    private val morph: Morph,
    private val progress: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = morph.toPath(progress).asComposePath()
        val matrix = Matrix()
        val bounds = morph.calculateBounds()
        val boundsWidth = bounds[2] - bounds[0]
        val boundsHeight = bounds[3] - bounds[1]

        matrix.scale(size.width / boundsWidth, size.height / boundsHeight)
        matrix.translate(-bounds[0], -bounds[1])
        path.transform(matrix)
        return Outline.Generic(path)
    }
}

@Composable
private fun SettingSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth(),
        onClick = { onCheckedChange(!checked) }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        }
    }
}

@Composable
private fun PermissionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    granted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (granted) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.secondaryContainer
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (granted) Icons.Rounded.Check else icon,
                    contentDescription = null,
                    tint = if (granted) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            if (granted) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = stringResource(R.string.cd_granted),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                FilledTonalButton(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun HintText(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 4.dp)
    )
}

/** Page icon inside a slowly rotating expressive shape. */
@Composable
private fun ShapeIconBadge(
    icon: ImageVector,
    shape: RoundedPolygon,
    size: Dp = 76.dp
) {
    val transition = rememberInfiniteTransition(label = "badgeSpin")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(60000, easing = LinearEasing)
        ),
        label = "badgeRotation"
    )

    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(size)
                .graphicsLayer { rotationZ = rotation }
                .clip(PolygonShape(shape))
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(size * 0.42f)
        )
    }
}

@Composable
private fun FeatureRow(
    icon: ImageVector,
    shape: RoundedPolygon,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(PolygonShape(shape))
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ChoiceCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun <T> ConnectedChoiceGroup(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween)
    ) {
        options.forEachIndexed { index, (value, label) ->
            val shapes = when (index) {
                0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
                options.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
                else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
            }
            ToggleButton(
                checked = selected == value,
                onCheckedChange = { onSelect(value) },
                modifier = Modifier.weight(1f),
                shapes = shapes,
                colors = ToggleButtonDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    checkedContainerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    checkedContentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = label,
                    fontWeight = if (selected == value) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

