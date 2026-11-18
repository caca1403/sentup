package com.sentup.android.ui.explore

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.material.icons.rounded.ThumbUp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.sentup.android.R
import com.sentup.android.data.ExploreFreshness
import com.sentup.android.data.ExploreGrouping
import com.sentup.android.data.ExploreLength
import com.sentup.android.data.ExploreRules
import com.sentup.android.data.ExploreSort
import com.sentup.android.data.ExploreSource
import com.sentup.android.data.LocalSubscription
import com.sentup.android.data.googleImageAtSize
import com.sentup.android.ui.theme.SentupRadii

/**
 * Redesigned, user-friendly Explore Rules studio operating cleanly on the
 * exact [ExploreRules] model and engine.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun ModernExploreRulesSheet(
    rules: ExploreRules,
    channels: List<LocalSubscription>,
    matchingVideoCount: Int,
    initialTab: Int = 0,
    onSearchChannels: suspend (String) -> List<LocalSubscription> = { emptyList() },
    onRulesChange: (ExploreRules) -> Unit,
    onResetRules: () -> Unit,
    onSaveAsList: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var studioTab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab) }
    var includeDraft by rememberSaveable { mutableStateOf("") }
    var excludeDraft by rememberSaveable { mutableStateOf("") }
    var channelQuery by rememberSaveable { mutableStateOf("") }
    var onlySelectedChannels by rememberSaveable { mutableStateOf(false) }
    var liveChannelResults by remember { mutableStateOf<List<LocalSubscription>>(emptyList()) }
    var isSearchingChannels by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(channelQuery) {
        val q = channelQuery.trim()
        if (q.length < 2) {
            liveChannelResults = emptyList()
            isSearchingChannels = false
            return@LaunchedEffect
        }
        isSearchingChannels = true
        kotlinx.coroutines.delay(300)
        liveChannelResults = runCatching { onSearchChannels(q) }.getOrDefault(emptyList())
        isSearchingChannels = false
    }

    val activeFilterCount = remember(rules) {
        var count = 0
        if (rules.length != ExploreLength.ANY) count++
        if (rules.freshness != ExploreFreshness.ANY) count++
        if (rules.channelIds.isNotEmpty()) count++
        if (rules.includeWords.isNotEmpty()) count++
        if (rules.excludeWords.isNotEmpty()) count++
        if (rules.hideWatched) count++
        if (rules.liveOnly) count++
        count
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Studio Header with live match count and quick reset
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Keşfet Kuralları Stüdyosu",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(SentupRadii.pill),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                text = "$matchingVideoCount video eşleşiyor",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        if (activeFilterCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(SentupRadii.pill),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "$activeFilterCount aktif filtre",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
                TextButton(onClick = onResetRules) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sıfırla")
                }
            }

            // Live removable summary strip for every active filter
            if (activeFilterCount > 0) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (rules.length != ExploreLength.ANY) {
                        ActiveRuleRemovableChip("Süre: ${lengthTitleTr(rules.length)}") {
                            onRulesChange(rules.copy(length = ExploreLength.ANY))
                        }
                    }
                    if (rules.freshness != ExploreFreshness.ANY) {
                        ActiveRuleRemovableChip("Zaman: ${freshnessTitleTr(rules.freshness)}") {
                            onRulesChange(rules.copy(freshness = ExploreFreshness.ANY))
                        }
                    }
                    if (rules.hideWatched) {
                        ActiveRuleRemovableChip("İzlenenler Gizli") {
                            onRulesChange(rules.copy(hideWatched = false))
                        }
                    }
                    if (rules.liveOnly) {
                        ActiveRuleRemovableChip("Yalnızca Canlı") {
                            onRulesChange(rules.copy(liveOnly = false))
                        }
                    }
                    if (rules.channelIds.isNotEmpty()) {
                        ActiveRuleRemovableChip("${rules.channelIds.size} Kanal Seçili") {
                            onRulesChange(rules.copy(channelIds = emptyList()))
                        }
                    }
                    if (rules.includeWords.isNotEmpty()) {
                        ActiveRuleRemovableChip("+${rules.includeWords.size} Kelime") {
                            onRulesChange(rules.copy(includeWords = emptyList()))
                        }
                    }
                    if (rules.excludeWords.isNotEmpty()) {
                        ActiveRuleRemovableChip("-${rules.excludeWords.size} Yasak Kelime") {
                            onRulesChange(rules.copy(excludeWords = emptyList()))
                        }
                    }
                }
            }

            // Clean Category Studio Tabs
            PrimaryScrollableTabRow(selectedTabIndex = studioTab, edgePadding = 0.dp) {
                Tab(selected = studioTab == 0, onClick = { studioTab = 0 }, text = { Text("Tümü") })
                Tab(selected = studioTab == 1, onClick = { studioTab = 1 }, text = { Text("Kaynak & Düzen") })
                Tab(
                    selected = studioTab == 2,
                    onClick = { studioTab = 2 },
                    text = {
                        Text(if (rules.channelIds.isNotEmpty()) "Kanallar (${rules.channelIds.size})" else "Kanallar")
                    }
                )
                Tab(selected = studioTab == 3, onClick = { studioTab = 3 }, text = { Text("Süre & Kelimeler") })
            }

            // Scrollable Studio Body
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 440.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (studioTab == 0 || studioTab == 1) {
                    item(key = "sec_sources") {
                        RuleSectionCard(
                            title = "1. Video Kaynakları",
                            subtitle = "Kurallar hangi listelerden video toplasın?"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                val sourcesList = ExploreSource.entries
                                sourcesList.chunked(2).forEach { pair ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        pair.forEach { source ->
                                            val checked = source in rules.sources
                                            SourceToggleTile(
                                                title = sourceTitleTr(source),
                                                subtitle = sourceSubTr(source),
                                                icon = sourceIcon(source),
                                                selected = checked,
                                                modifier = Modifier.weight(1f),
                                                onClick = {
                                                    val next = if (checked) {
                                                        if (rules.sources.size > 1) rules.sources - source else rules.sources
                                                    } else {
                                                        rules.sources + source
                                                    }
                                                    onRulesChange(rules.copy(sources = next))
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item(key = "sec_group_sort") {
                        RuleSectionCard(
                            title = "2. Gruplama ve Sıralama",
                            subtitle = "Sonuçlar raflara nasıl dizilsin?"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Gruplama Tipi",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ExploreGrouping.entries.forEach { g ->
                                        FilterChip(
                                            selected = rules.grouping == g,
                                            onClick = { onRulesChange(rules.copy(grouping = g)) },
                                            label = { Text(groupingTitleTr(g)) },
                                            leadingIcon = if (rules.grouping == g) {
                                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                            } else null
                                        )
                                    }
                                }
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                Text(
                                    text = "Sıralama Ölçütü",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ExploreSort.entries.forEach { s ->
                                        FilterChip(
                                            selected = rules.sort == s,
                                            onClick = { onRulesChange(rules.copy(sort = s)) },
                                            label = { Text(sortTitleTr(s)) },
                                            leadingIcon = {
                                                Icon(
                                                    imageVector = if (s == ExploreSort.SHUFFLED) Icons.Rounded.Shuffle else Icons.AutoMirrored.Rounded.Sort,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(15.dp)
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (studioTab == 0 || studioTab == 3) {
                    item(key = "sec_time_len") {
                        RuleSectionCard(
                            title = "3. Video Süresi, Yenilik ve Mod",
                            subtitle = "İstemediğin uzunlukları ve eski içerikleri ele"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text(
                                    text = "Video Uzunluğu",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ExploreLength.entries.forEach { len ->
                                        FilterChip(
                                            selected = rules.length == len,
                                            onClick = { onRulesChange(rules.copy(length = len)) },
                                            label = { Text(lengthTitleTr(len)) }
                                        )
                                    }
                                }

                                Text(
                                    text = "Yüklenme Zamanı (Tazelik)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ExploreFreshness.entries.forEach { fresh ->
                                        FilterChip(
                                            selected = rules.freshness == fresh,
                                            onClick = { onRulesChange(rules.copy(freshness = fresh)) },
                                            label = { Text(freshnessTitleTr(fresh)) }
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                RuleSwitchCardRow(
                                    title = stringResource(R.string.explore_hide_watched),
                                    subtitle = "Daha önce izlediğin videoları gizle",
                                    icon = Icons.Rounded.VisibilityOff,
                                    checked = rules.hideWatched,
                                    onCheckedChange = { onRulesChange(rules.copy(hideWatched = it)) }
                                )

                                RuleSwitchCardRow(
                                    title = stringResource(R.string.explore_live_only),
                                    subtitle = "Yalnızca aktif veya planlanan canlı yayınlar",
                                    icon = Icons.Rounded.LiveTv,
                                    checked = rules.liveOnly,
                                    onCheckedChange = { onRulesChange(rules.copy(liveOnly = it)) }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Raf Başına Video Sayısı",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Her grupta gösterilecek maksimum video",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        FilledTonalIconButton(
                                            onClick = {
                                                onRulesChange(rules.withShelfLimit(rules.perShelfLimit - 2))
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Rounded.Remove, contentDescription = "Azalt", modifier = Modifier.size(16.dp))
                                        }
                                        Text(
                                            text = "${rules.perShelfLimit}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        FilledTonalIconButton(
                                            onClick = {
                                                onRulesChange(rules.withShelfLimit(rules.perShelfLimit + 2))
                                            },
                                            modifier = Modifier.size(34.dp)
                                        ) {
                                            Icon(Icons.Rounded.Add, contentDescription = "Artır", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (studioTab == 0 || studioTab == 2) {
                    item(key = "sec_channels") {
                        RuleSectionCard(
                            title = "Kanal Seçimi (${if (rules.channelIds.isEmpty()) "Tüm Kanallar" else "${rules.channelIds.size} Kanal Seçili"})",
                            subtitle = "Listede görünecek kanalları işaretle veya YouTube'dan kanal ara"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = channelQuery,
                                        onValueChange = { channelQuery = it },
                                        placeholder = { Text("Kanal ara veya ekle...") },
                                        singleLine = true,
                                        leadingIcon = {
                                            Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                                        },
                                        trailingIcon = {
                                            if (isSearchingChannels) {
                                                androidx.compose.material3.CircularProgressIndicator(
                                                    modifier = Modifier.size(18.dp),
                                                    strokeWidth = 2.dp
                                                )
                                            } else if (channelQuery.isNotBlank()) {
                                                IconButton(onClick = { channelQuery = ""; liveChannelResults = emptyList() }) {
                                                    Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.weight(1f)
                                    )
                                }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    FilterChip(
                                        selected = !onlySelectedChannels,
                                        onClick = { onlySelectedChannels = false },
                                        label = { Text("Tümü (${channels.size})") }
                                    )
                                    FilterChip(
                                        selected = onlySelectedChannels,
                                        onClick = { onlySelectedChannels = true },
                                        label = { Text("Seçililer (${rules.channelIds.size})") }
                                    )
                                    if (rules.channelIds.isNotEmpty()) {
                                        FilterChip(
                                            selected = false,
                                            onClick = { onRulesChange(rules.copy(channelIds = emptyList())) },
                                            label = { Text("Seçimi Temizle") }
                                        )
                                    }
                                }

                                val mergedChannels = remember(channels, liveChannelResults, rules.channelIds, channelQuery, onlySelectedChannels) {
                                    val byId = LinkedHashMap<String, LocalSubscription>()
                                    channels.forEach { byId[it.channelId] = it }
                                    liveChannelResults.forEach { live -> byId.putIfAbsent(live.channelId, live) }
                                    // Ensure selected channels missing from cache still stay visible for unselecting
                                    rules.channelIds.forEach { selectedId ->
                                        if (selectedId !in byId) {
                                            byId[selectedId] = LocalSubscription(channelId = selectedId, name = selectedId)
                                        }
                                    }
                                    val q = channelQuery.trim()
                                    byId.values
                                        .filter { ch ->
                                            val matchesQ = q.isEmpty() ||
                                                ch.name.contains(q, ignoreCase = true) ||
                                                ch.channelId.contains(q, ignoreCase = true)
                                            val matchesFilter = !onlySelectedChannels || ch.channelId in rules.channelIds
                                            matchesQ && matchesFilter
                                        }
                                        .sortedWith(
                                            compareByDescending<LocalSubscription> { it.channelId in rules.channelIds }
                                                .thenBy { it.name.lowercase() }
                                        )
                                }

                                if (mergedChannels.isEmpty()) {
                                    Text(
                                        text = if (channelQuery.isNotBlank()) "Eşleşen kanal bulunamadı — başka isimle arayın." else "Henüz abone olunan veya önbellekte kanal yok. Yukarıdan YouTube kanalı arayıp seçebilirsin.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                } else {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        mergedChannels.forEach { ch ->
                                            val selected = ch.channelId in rules.channelIds
                                            Surface(
                                                onClick = {
                                                    val next = if (selected) {
                                                        rules.channelIds - ch.channelId
                                                    } else {
                                                        rules.channelIds + ch.channelId
                                                    }
                                                    onRulesChange(rules.copy(channelIds = next))
                                                },
                                                shape = RoundedCornerShape(14.dp),
                                                color = if (selected) {
                                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.72f)
                                                } else {
                                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                                },
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(horizontal = 12.dp, vertical = 9.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(34.dp)
                                                            .clip(CircleShape)
                                                            .background(MaterialTheme.colorScheme.surfaceContainerLow),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (!ch.avatarUrl.isNullOrBlank()) {
                                                            AsyncImage(
                                                                model = googleImageAtSize(ch.avatarUrl, 96),
                                                                contentDescription = null,
                                                                contentScale = ContentScale.Crop,
                                                                modifier = Modifier.fillMaxSize()
                                                            )
                                                        } else {
                                                            Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        }
                                                    }
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            text = ch.name,
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                                            maxLines = 1,
                                                            overflow = TextOverflow.Ellipsis
                                                        )
                                                        ch.handle?.takeIf { it.isNotBlank() }?.let { handle ->
                                                            Text(
                                                                text = handle,
                                                                style = MaterialTheme.typography.labelSmall,
                                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                                maxLines = 1
                                                            )
                                                        }
                                                    }
                                                    androidx.compose.material3.Checkbox(
                                                        checked = selected,
                                                        onCheckedChange = null
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (studioTab == 0 || studioTab == 3) {
                    item(key = "sec_words") {
                        RuleSectionCard(
                            title = "4. Akıllı Kelime Kuralları",
                            subtitle = "Başlıkta mutlaka geçsin veya tamamen gizlensin"
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                WordChipRuleEditor(
                                    label = "İçermesi Gereken Kelimeler (VEYA mantığı)",
                                    placeholder = "Kelime ekle (örn: inceleme, podcast)...",
                                    draft = includeDraft,
                                    onDraftChange = { includeDraft = it },
                                    words = rules.includeWords,
                                    isExclude = false,
                                    suggestions = listOf("Podcast", "İnceleme", "Oyun", "Rehber", "Haber", "Belgesel"),
                                    onAddWord = { word ->
                                        val clean = word.trim()
                                        if (clean.isNotEmpty() && rules.includeWords.none { it.equals(clean, ignoreCase = true) }) {
                                            onRulesChange(rules.copy(includeWords = rules.includeWords + clean))
                                        }
                                    },
                                    onRemoveWord = { word ->
                                        onRulesChange(rules.copy(includeWords = rules.includeWords - word))
                                    }
                                )

                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                                WordChipRuleEditor(
                                    label = "Hariç Tutulacak Kelimeler (Başlık veya Kanal)",
                                    placeholder = "Engellenecek kelime ekle (örn: spoiler)...",
                                    draft = excludeDraft,
                                    onDraftChange = { excludeDraft = it },
                                    words = rules.excludeWords,
                                    isExclude = true,
                                    suggestions = listOf("Spoiler", "Shorts", "Fragman", "Teaser", "Klip"),
                                    onAddWord = { word ->
                                        val clean = word.trim()
                                        if (clean.isNotEmpty() && rules.excludeWords.none { it.equals(clean, ignoreCase = true) }) {
                                            onRulesChange(rules.copy(excludeWords = rules.excludeWords + clean))
                                        }
                                    },
                                    onRemoveWord = { word ->
                                        onRulesChange(rules.copy(excludeWords = rules.excludeWords - word))
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Bottom Studio Action Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onSaveAsList,
                    shape = RoundedCornerShape(SentupRadii.pill),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Liste Olarak Kaydet")
                }
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(SentupRadii.pill),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Uygula ($matchingVideoCount)")
                }
            }
        }
    }
}

@Composable
private fun ActiveRuleRemovableChip(label: String, onRemove: () -> Unit) {
    Surface(
        onClick = onRemove,
        shape = RoundedCornerShape(SentupRadii.pill),
        color = MaterialTheme.colorScheme.secondaryContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Kaldır",
                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun RuleSectionCard(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(SentupRadii.card),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            content()
        }
    }
}

@Composable
private fun SourceToggleTile(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceContainerHighest
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
                if (selected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun RuleSwitchCardRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WordChipRuleEditor(
    label: String,
    placeholder: String,
    draft: String,
    onDraftChange: (String) -> Unit,
    words: List<String>,
    isExclude: Boolean,
    suggestions: List<String>,
    onAddWord: (String) -> Unit,
    onRemoveWord: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (isExclude) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = { Text(placeholder) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        if (draft.isNotBlank()) {
                            onAddWord(draft)
                            onDraftChange("")
                        }
                    }
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.weight(1f)
            )
            FilledTonalButton(
                onClick = {
                    if (draft.isNotBlank()) {
                        onAddWord(draft)
                        onDraftChange("")
                    }
                },
                enabled = draft.isNotBlank(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ekle")
            }
        }
        if (words.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                words.forEach { w ->
                    Surface(
                        onClick = { onRemoveWord(w) },
                        shape = RoundedCornerShape(SentupRadii.pill),
                        color = if (isExclude) {
                            MaterialTheme.colorScheme.errorContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = (if (isExclude) "- " else "+ ") + w,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isExclude) {
                                    MaterialTheme.colorScheme.onErrorContainer
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                            Icon(Icons.Rounded.Close, contentDescription = "Sil", modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            suggestions.filter { s -> words.none { it.equals(s, ignoreCase = true) } }.forEach { sug ->
                FilterChip(
                    selected = false,
                    onClick = { onAddWord(sug) },
                    label = { Text("+ $sug") }
                )
            }
        }
    }
}

private fun sourceTitleTr(source: ExploreSource): String = when (source) {
    ExploreSource.SUBSCRIPTIONS -> "Abonelikler"
    ExploreSource.HISTORY -> "İzleme Geçmişi"
    ExploreSource.DEVICE_PLAYLISTS -> "Cihaz Listeleri"
    ExploreSource.LIKED -> "Beğenilenler"
}

private fun sourceSubTr(source: ExploreSource): String = when (source) {
    ExploreSource.SUBSCRIPTIONS -> "Takip edilen kanallar"
    ExploreSource.HISTORY -> "Daha önce izlediklerin"
    ExploreSource.DEVICE_PLAYLISTS -> "Kayıtlı video listelerin"
    ExploreSource.LIKED -> "Hesabın beğendiği videolar"
}

private fun sourceIcon(source: ExploreSource): ImageVector = when (source) {
    ExploreSource.SUBSCRIPTIONS -> Icons.Rounded.Subscriptions
    ExploreSource.HISTORY -> Icons.Rounded.History
    ExploreSource.DEVICE_PLAYLISTS -> Icons.AutoMirrored.Rounded.PlaylistPlay
    ExploreSource.LIKED -> Icons.Rounded.ThumbUp
}

private fun groupingTitleTr(grouping: ExploreGrouping): String = when (grouping) {
    ExploreGrouping.NONE -> "Tek Liste"
    ExploreGrouping.CHANNEL -> "Kanala Göre"
    ExploreGrouping.DURATION -> "Süreye Göre"
    ExploreGrouping.FRESHNESS -> "Tazeliğe Göre"
}

private fun sortTitleTr(sort: ExploreSort): String = when (sort) {
    ExploreSort.NEWEST -> "En Yeni"
    ExploreSort.OLDEST -> "En Eski"
    ExploreSort.MOST_VIEWED -> "En Çok İzlenen"
    ExploreSort.QUICKEST -> "En Kısa"
    ExploreSort.LONGEST -> "En Uzun"
    ExploreSort.SHUFFLED -> "Karışık"
}

private fun lengthTitleTr(length: ExploreLength): String = when (length) {
    ExploreLength.ANY -> "Tümü"
    ExploreLength.SHORT -> "Kısa (<4 dk)"
    ExploreLength.MEDIUM -> "Orta (4-20 dk)"
    ExploreLength.LONG -> "Uzun (20+ dk)"
}

private fun freshnessTitleTr(freshness: ExploreFreshness): String = when (freshness) {
    ExploreFreshness.ANY -> "Her Zaman"
    ExploreFreshness.TODAY -> "Son 24 Saat"
    ExploreFreshness.WEEK -> "Bu Hafta"
    ExploreFreshness.MONTH -> "Bu Ay"
    ExploreFreshness.QUARTER -> "Son 3 Ay"
}
