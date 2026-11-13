package com.sentup.android.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sentup.android.R
import com.sentup.android.data.ExploreFreshness
import com.sentup.android.data.ExploreLength
import com.sentup.android.data.ExploreList
import com.sentup.android.data.ExploreRules
import com.sentup.android.data.ExploreSort
import com.sentup.android.ui.theme.SentupRadii

data class ExploreChannelOption(
    val idOrName: String,
    val displayName: String
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CustomExploreBuilderSheet(
    editingList: ExploreList?,
    availableChannels: List<ExploreChannelOption>,
    onSave: (name: String, rules: ExploreRules) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(editingList?.id) {
        mutableStateOf(editingList?.name.orEmpty())
    }
    var selectedChannels by remember(editingList?.id) {
        mutableStateOf(editingList?.rules?.channelIds.orEmpty())
    }
    var manualChannelInput by remember { mutableStateOf("") }
    var includeWordsText by remember(editingList?.id) {
        mutableStateOf(editingList?.rules?.includeWords.orEmpty().joinToString(", "))
    }
    var excludeWordsText by remember(editingList?.id) {
        mutableStateOf(editingList?.rules?.excludeWords.orEmpty().joinToString(", "))
    }
    var length by remember(editingList?.id) {
        mutableStateOf(editingList?.rules?.length ?: ExploreLength.ANY)
    }
    var freshness by remember(editingList?.id) {
        mutableStateOf(editingList?.rules?.freshness ?: ExploreFreshness.ANY)
    }
    var sort by remember(editingList?.id) {
        mutableStateOf(editingList?.rules?.sort ?: ExploreSort.NEWEST)
    }
    var hideWatched by remember(editingList?.id) {
        mutableStateOf(editingList?.rules?.hideWatched ?: false)
    }
    var liveOnly by remember(editingList?.id) {
        mutableStateOf(editingList?.rules?.liveOnly ?: false)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.vhc_custom_explore_create),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.vhc_custom_explore_create_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = {
                        val includeWords = includeWordsText.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        val excludeWords = excludeWordsText.split(",")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }
                        val finalName = name.trim().ifEmpty { "Custom Explore" }
                        val rules = (editingList?.rules ?: ExploreRules()).copy(
                            channelIds = selectedChannels,
                            includeWords = includeWords,
                            excludeWords = excludeWords,
                            length = length,
                            freshness = freshness,
                            sort = sort,
                            hideWatched = hideWatched,
                            liveOnly = liveOnly
                        )
                        onSave(finalName, rules)
                    },
                    shape = RoundedCornerShape(SentupRadii.pill)
                ) {
                    Text(stringResource(R.string.lib_custom_folder_save))
                }
            }

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.vhc_custom_explore_name)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider()

            Text(
                text = stringResource(R.string.vhc_custom_explore_channels),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = manualChannelInput,
                    onValueChange = { manualChannelInput = it },
                    label = { Text(stringResource(R.string.vhc_custom_explore_channels_hint)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = {
                        val cleaned = manualChannelInput.trim()
                        if (cleaned.isNotEmpty() && cleaned !in selectedChannels) {
                            selectedChannels = selectedChannels + cleaned
                            manualChannelInput = ""
                        }
                    }
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                }
            }

            if (selectedChannels.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    selectedChannels.forEach { ch ->
                        val label = availableChannels.firstOrNull { it.idOrName == ch }?.displayName ?: ch
                        InputChip(
                            selected = true,
                            onClick = { selectedChannels = selectedChannels - ch },
                            label = { Text(label) },
                            trailingIcon = {
                                Icon(Icons.Rounded.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }
            }

            if (availableChannels.isNotEmpty()) {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    availableChannels.take(20).forEach { ch ->
                        val selected = ch.idOrName in selectedChannels
                        FilterChip(
                            selected = selected,
                            onClick = {
                                selectedChannels = if (selected) {
                                    selectedChannels - ch.idOrName
                                } else {
                                    selectedChannels + ch.idOrName
                                }
                            },
                            leadingIcon = if (selected) {
                                { Icon(Icons.Rounded.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            label = { Text(ch.displayName) }
                        )
                    }
                }
            }

            HorizontalDivider()

            OutlinedTextField(
                value = includeWordsText,
                onValueChange = { includeWordsText = it },
                label = { Text(stringResource(R.string.vhc_custom_explore_include)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = excludeWordsText,
                onValueChange = { excludeWordsText = it },
                label = { Text(stringResource(R.string.vhc_custom_explore_exclude)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = stringResource(R.string.vhc_custom_explore_length),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExploreLength.entries.forEach { option ->
                    FilterChip(
                        selected = length == option,
                        onClick = { length = option },
                        label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Text(
                text = stringResource(R.string.vhc_custom_explore_freshness),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExploreFreshness.entries.forEach { option ->
                    FilterChip(
                        selected = freshness == option,
                        onClick = { freshness = option },
                        label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Text(
                text = stringResource(R.string.vhc_custom_explore_sort),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold
            )
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExploreSort.entries.forEach { option ->
                    FilterChip(
                        selected = sort == option,
                        onClick = { sort = option },
                        label = { Text(option.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.vhc_custom_explore_hide_watched))
                Switch(checked = hideWatched, onCheckedChange = { hideWatched = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.vhc_custom_explore_live_only))
                Switch(checked = liveOnly, onCheckedChange = { liveOnly = it })
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        }
    }
}
