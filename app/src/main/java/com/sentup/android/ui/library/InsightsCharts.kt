package com.sentup.android.ui.library

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.sentup.android.ui.home.HomeViewModel
import java.util.Calendar

private data class Slice(val name: String, val sub: String, val value: Int, val color: Color)

/**
 * The listening analytics block, built from real play history and meant to be
 * touched: a dataset switch (artists / songs), a donut whose legend you tap to
 * isolate a slice, ranked bars for the other dataset, and a seven-day bar chart.
 * It sits at the top of the Library and inside the Home feed.
 */
@Composable
internal fun InsightsCard(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onOpenStats: (() -> Unit)? = null
) {
    val globalStats by viewModel.globalStats.collectAsState()
    val dailyPlays by viewModel.dailyPlays.collectAsState()
    LaunchedEffect(Unit) { viewModel.refreshStats() }

    val palette = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.error,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
    )

    var byArtist by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf(-1) }
    LaunchedEffect(byArtist) { selected = -1 }

    val slices: List<Slice> = remember(globalStats, byArtist, palette) {
        val raw: List<Triple<String, String, Int>> = if (byArtist) {
            globalStats.topArtists.take(5).map { Triple(it.name, "${it.songCount} şarkı", it.playCount) }
        } else {
            globalStats.topSongs.take(5).map { Triple(it.title, it.artist, it.playCount) }
        }
        val used = raw.sumOf { it.third }
        val other = (globalStats.totalPlays - used).coerceAtLeast(0)
        buildList {
            raw.forEachIndexed { i, (n, s, v) -> add(Slice(n, s, v, palette[i % palette.size])) }
            if (other > 0) add(Slice("Diğer", "geri kalanı", other, Color(0xFF8E8E93)))
        }
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.Insights,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    "Dinleme analizin",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (onOpenStats != null) {
                    TextButton(onClick = onOpenStats) { Text("Tümü") }
                }
            }
            Text(
                "${globalStats.totalPlays} çalma · ${globalStats.uniqueSongs} şarkı · " +
                    "${globalStats.uniqueArtists} sanatçı",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = byArtist,
                    onClick = { byArtist = true },
                    label = { Text("Sanatçılara göre") }
                )
                FilterChip(
                    selected = !byArtist,
                    onClick = { byArtist = false },
                    label = { Text("Şarkılara göre") }
                )
            }

            Spacer(Modifier.height(8.dp))
            if (slices.isEmpty() || globalStats.totalPlays == 0) {
                Text(
                    "Henüz dinleme verisi yok. Birkaç şarkı çaldığında kimleri ve " +
                        "neleri ne kadar dinlediğin burada grafiklere dönüşür.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Donut(
                    slices = slices,
                    selected = selected,
                    onSelect = { selected = if (selected == it) -1 else it }
                )
                Spacer(Modifier.height(16.dp))
                val barLabel = if (byArtist) "En çok sanatçı" else "En çok şarkı"
                RankBars(title = barLabel, slices = slices, selected = selected, onSelect = {
                    selected = if (selected == it) -1 else it
                })
                Spacer(Modifier.height(16.dp))
                WeeklyBars(dailyPlays)
            }
        }
    }
}

@Composable
private fun Donut(slices: List<Slice>, selected: Int, onSelect: (Int) -> Unit) {
    val total = slices.sumOf { it.value }.coerceAtLeast(1)
    val sw = 26.dp
    val focus = slices.getOrNull(selected)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(140.dp)) {
            Canvas(modifier = Modifier.size(140.dp)) {
                val width = sw.toPx()
                val d = size.minDimension - width
                val topLeft = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                val arc = Size(d, d)
                var start = -90f
                slices.forEachIndexed { i, slice ->
                    val sweep = slice.value.toFloat() / total * 360f
                    val gap = if (slices.size > 1) 3f else 0f
                    val color = if (selected == -1 || selected == i) slice.color
                    else slice.color.copy(alpha = 0.25f)
                    if (sweep - gap > 0f) {
                        drawArc(
                            color = color,
                            startAngle = start + gap / 2f,
                            sweepAngle = sweep - gap,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arc,
                            style = Stroke(
                                width = if (selected == i) width * 1.25f else width,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }
                    start += sweep
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(96.dp)
            ) {
                if (focus != null) {
                    Text(
                        focus.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "${focus.value} çalma · %${focus.value * 100 / total}",
                        style = MaterialTheme.typography.labelSmall,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "$total",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        "çalma",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            slices.forEachIndexed { i, slice ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onSelect(i) }
                        .padding(vertical = 4.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .background(slice.color, CircleShape)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        slice.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = if (selected == i) FontWeight.Bold else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "%${slice.value * 100 / total}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RankBars(title: String, slices: List<Slice>, selected: Int, onSelect: (Int) -> Unit) {
    val top = slices.filterNot { it.name == "Diğer" }.take(5)
    val max = (top.maxOfOrNull { it.value } ?: 1).coerceAtLeast(1)
    Column {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(10.dp))
        top.forEachIndexed { i, slice ->
            val fraction by animateFloatAsState(
                targetValue = slice.value.toFloat() / max,
                animationSpec = tween(500),
                label = "rankbar"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onSelect(i) }
                    .padding(vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${i + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (selected == i) slice.color
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(18.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        slice.name,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(3.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                RoundedCornerShape(50)
                            )
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(fraction)
                                .height(8.dp)
                                .background(slice.color, RoundedCornerShape(50))
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "${slice.value}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun WeeklyBars(dailyPlays: Map<String, Int>) {
    val days = remember(dailyPlays) {
        (6 downTo 0).map { offset ->
            val c = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -offset) }
            val key = "${c.get(Calendar.MONTH) + 1}/${c.get(Calendar.DAY_OF_MONTH)}"
            val label = when (c.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Pt"
                Calendar.TUESDAY -> "Sa"
                Calendar.WEDNESDAY -> "Ça"
                Calendar.THURSDAY -> "Pe"
                Calendar.FRIDAY -> "Cu"
                Calendar.SATURDAY -> "Ct"
                else -> "Pz"
            }
            Triple(label, dailyPlays[key] ?: 0, offset == 0)
        }
    }
    val max = (days.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1)
    var picked by remember { mutableStateOf(-1) }
    Column {
        Text(
            "Son 7 gün",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            days.forEachIndexed { index, (label, count, isToday) ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { picked = if (picked == index) -1 else index },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (picked == index) {
                        Text(
                            "$count",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height((6 + 56f * count / max).dp)
                            .background(
                                when {
                                    count == 0 -> MaterialTheme.colorScheme.surfaceContainerHighest
                                    isToday -> MaterialTheme.colorScheme.primary
                                    else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                },
                                RoundedCornerShape(50)
                            )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                        color = if (isToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
