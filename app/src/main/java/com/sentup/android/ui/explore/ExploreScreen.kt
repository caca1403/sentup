package com.sentup.android.ui.explore

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.FilterAlt
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.sentup.android.R
import com.sentup.android.data.ExploreFreshness
import com.sentup.android.data.ExploreGrouping
import com.sentup.android.data.ExploreLength
import com.sentup.android.data.ExploreList
import com.sentup.android.data.ExplorePreset
import com.sentup.android.data.ExploreRules
import com.sentup.android.data.ExploreShelf
import com.sentup.android.data.ExploreShelfKind
import com.sentup.android.data.ExploreSort
import com.sentup.android.data.ExploreSource
import com.sentup.android.data.VideoItem
import com.sentup.android.ui.components.ExpressivePullToRefresh
import com.sentup.android.ui.theme.SentupDivider
import com.sentup.android.ui.theme.SentupGroup
import com.sentup.android.ui.theme.SentupGroupCard
import com.sentup.android.ui.theme.SentupGroupHeader
import com.sentup.android.ui.theme.SentupRadii
import com.sentup.android.ui.theme.SentupRow
import com.sentup.android.ui.theme.sentupCard
import com.sentup.android.ui.theme.sentupPage
import com.sentup.android.ui.video.VideoCard
import com.sentup.android.ui.video.LocalVideoListLayout
import com.sentup.android.ui.video.videoListItems

/**
 * The Explore tab: Sentüp's own discovery surface for video mode.
 *
 * Shelves built out of what this device already holds, arranged by rules the
 * user writes, rather than the one flat server-ordered column a YouTube feed
 * is. **The grouping is the point** - one shelf per creator, per length band or
 * per age band - and a saved list is a rule rather than a copy, so a list
 * written once keeps answering itself as new uploads arrive.
 *
 * **No new InnerTube shape and no search fan-out.** Every source here is a feed
 * Sentüp has already fetched for another tab, or a playlist id it already knows
 * how to browse, so opening Explore costs what the sources cost and nothing
 * more. That is also why the tab fetches on open rather than at startup.
 *
 * [onEnqueueVideo] and [onPlayShelf] are optional so a preview and a caller
 * that only opens videos both work, but `HomeScreen` passes both explicitly -
 * the per-call-site null-default trap is exactly how a feature ships half-wired
 * with nothing compile-failing.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ExploreContent(
    onVideoClick: (VideoItem) -> Unit,
    onOpenChannel: (String) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    onEnqueueVideo: ((VideoItem, Boolean) -> Unit)? = null,
    /** Play a whole shelf, with the name it is filed under. */
    onPlayShelf: ((List<VideoItem>, String) -> Unit)? = null,
    listState: LazyListState = rememberLazyListState(),
    viewModel: ExploreViewModel = viewModel()
) {
    val rules by viewModel.rules.collectAsState()
    val shelves by viewModel.shelves.collectAsState()
    val lists by viewModel.lists.collectAsState()
    val selectedListId by viewModel.selectedListId.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val hasLoaded by viewModel.hasLoaded.collectAsState()
    val loadFailed by viewModel.loadFailed.collectAsState()
    val discover by viewModel.discover.collectAsState()
    val hasAnySource by viewModel.hasAnySource.collectAsState()
    val channels by viewModel.channels.collectAsState()

    // Read once here rather than inside the builder: the builder is not a
    // composable, and this is the same value the video Home content reads.
    val listLayout = LocalVideoListLayout.current

    var showFilters by rememberSaveable { mutableStateOf(false) }
    var nameRequest by remember { mutableStateOf<NameRequest?>(null) }

    // Hoisted out of the callbacks below because those are plain function
    // types, not composable ones: a lambda bound to `(ExploreList, ListAction)
    // -> Unit` is not a composable context, and stringResource has no meaning
    // there. Read once, where reading is allowed.
    val copySuffix = stringResource(R.string.explore_copy_suffix)
    val presetTitles: Map<ExplorePreset, String> =
        ExplorePreset.entries.associateWith { stringResource(presetTitle(it)) }

    // Fetched when the tab is opened and not before, like every other
    // video-mode tab: a user who never opens Explore never pays for it.
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(Unit) { viewModel.loadDiscovery() }

    ExploreNameDialog(
        request = nameRequest,
        onDismiss = { nameRequest = null },
        onConfirm = { name ->
            val request = nameRequest
            nameRequest = null
            when (request?.action) {
                // The two create paths differ in the rules they store, not in
                // the dialog: "save as" keeps what is on screen, "new list"
                // starts from the tab's own default.
                NameAction.SAVE_RULES -> if (name.isNotBlank()) viewModel.saveAsList(name)
                NameAction.NEW_LIST -> if (name.isNotBlank()) viewModel.newList(name)
                NameAction.RENAME -> request.listId?.let { viewModel.renameList(it, name) }
                NameAction.DUPLICATE -> request.listId?.let {
                    if (name.isNotBlank()) viewModel.duplicateList(it, name)
                }
                NameAction.PRESET -> request.preset?.let { preset ->
                    if (name.isNotBlank()) viewModel.createFromPreset(preset, name)
                }
                // Writing the on-screen rules back into the list that is open
                // asks for no name at all; the dialog is not opened for it.
                NameAction.SAVE_TO_OPEN -> viewModel.saveToOpenList()
                // Reached only with a null request, which the dialog guards
                // against; kept so a new action cannot fail to compile here.
                else -> Unit
            }
        }
    )

    if (showFilters) {
        ExploreFilterSheet(
            rules = rules,
            channels = channels,
            onRulesChange = viewModel::updateRules,
            onSaveAsList = {
                showFilters = false
                nameRequest = NameRequest(NameAction.SAVE_RULES)
            },
            onDismiss = { showFilters = false }
        )
    }

    ExpressivePullToRefresh(
        // Only over existing content, the rule the video Home feed already
        // follows: the empty state draws its own centred indicator.
        isRefreshing = isLoading && shelves.isNotEmpty(),
        onRefresh = { viewModel.load(force = true) },
        modifier = modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .sentupPage(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "explore_header") {
                ExploreHeader(
                    isDirty = selectedListId != null && viewModel.isDirty(),
                    onSaveToList = viewModel::saveToOpenList,
                    onOpenFilters = { showFilters = true },
                    onNewList = { nameRequest = NameRequest(NameAction.NEW_LIST) },
                    onSaveAsList = { nameRequest = NameRequest(NameAction.SAVE_RULES) },
                    onResetRules = viewModel::resetRules
                )
            }

            // Live YouTube first: the page opens on what is actually
            // happening, not on an empty filter of things you own.
            discover.forEach { shelf ->
                item(key = "disc_" + shelf.id + "_header") {
                    ExploreShelfHeader(
                        title = stringResource(R.string.explore_discover_trending),
                        shelf = shelf,
                        onOpenChannel = onOpenChannel,
                        onPlayAll = onPlayShelf
                    )
                }
                videoListItems(
                    videos = shelf.videos,
                    layout = listLayout,
                    keyPrefix = "disc_" + shelf.id + "_"
                ) { video, cell ->
                    ExploreVideoCard(
                        video = video,
                        modifier = cell,
                        onVideoClick = onVideoClick,
                        onLongPress = onEnqueueVideo,
                        onOpenChannel = onOpenChannel
                    )
                }
            }

            item(key = "explore_lists") {
                ExploreListRow(
                    lists = lists,
                    selectedListId = selectedListId,
                    onSelect = viewModel::selectList,
                    onAdd = { nameRequest = NameRequest(NameAction.NEW_LIST) },
                    onMenu = { list, action ->
                        // One place decides what each row of the menu means:
                        // two of them open the naming dialog and two act on the
                        // list directly.
                        when (action) {
                            ListAction.RENAME ->
                                nameRequest = NameRequest(NameAction.RENAME, listId = list.id)

                            ListAction.DUPLICATE -> nameRequest = NameRequest(
                                NameAction.DUPLICATE,
                                listId = list.id,
                                seedName = list.name + " " + copySuffix
                            )

                            ListAction.PIN -> viewModel.setPinned(list.id, !list.pinned)
                            ListAction.DELETE -> viewModel.deleteList(list.id)
                        }
                    }
                )
            }

            item(key = "explore_chips") {
                ExploreFilterBar(rules = rules, onOpenFilters = { showFilters = true })
            }

            if (shelves.isEmpty()) {
                item(key = "explore_empty") {
                    ExploreEmptyState(
                        isLoading = isLoading && !hasLoaded,
                        hasAnySource = hasAnySource,
                        loadFailed = loadFailed,
                        onRetry = { viewModel.load(force = true) },
                        onPreset = { preset ->
                            nameRequest = NameRequest(
                                NameAction.PRESET,
                                preset = preset,
                                seedName = presetTitles[preset].orEmpty()
                            )
                        }
                    )
                }
            }

            // The flat shelf is the whole list, so it is titled by the list.
            val openListName = lists.firstOrNull { it.id == selectedListId }?.name
            shelves.forEach { shelf ->
                if (shelf.kind == ExploreShelfKind.WHOLE) {
                    // The header is emitted here rather than inside
                    // ExploreShelfSection, because the flat list's rows come
                    // from [videoListItems] - a LazyListScope extension that has
                    // to be called from here - and a title with no rows under it
                    // is not the only thing that would be lost.
                    item(key = "shelf_" + shelf.id + "_header") {
                        ExploreShelfHeader(
                            title = shelfTitle(shelf, openListName),
                            shelf = shelf,
                            onOpenChannel = onOpenChannel,
                            onPlayAll = onPlayShelf
                        )
                    }
                    videoListItems(
                        videos = shelf.videos,
                        layout = listLayout,
                        keyPrefix = "explore_" + shelf.id + "_"
                    ) { video, cell ->
                        ExploreVideoCard(
                            video = video,
                            modifier = cell,
                            onVideoClick = onVideoClick,
                            onLongPress = onEnqueueVideo,
                            onOpenChannel = onOpenChannel
                        )
                    }
                } else {
                    item(key = "shelf_" + shelf.id) {
                        ExploreShelfSection(
                            shelf = shelf,
                            openListName = openListName,
                            onVideoClick = onVideoClick,
                            onLongPress = onEnqueueVideo,
                            onOpenChannel = onOpenChannel,
                            onPlayAll = onPlayShelf
                        )
                    }
                }
            }

            item(key = "explore_footer") { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// --------------------------------------------------------------------
// Header
// --------------------------------------------------------------------

@Composable
private fun ExploreHeader(
    isDirty: Boolean,
    onSaveToList: () -> Unit,
    onOpenFilters: () -> Unit,
    onNewList: () -> Unit,
    onSaveAsList: () -> Unit,
    onResetRules: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 8.dp, top = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.explore_title),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = stringResource(R.string.explore_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onOpenFilters) {
                Icon(
                    Icons.Rounded.Tune,
                    contentDescription = stringResource(R.string.explore_filters)
                )
            }
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.explore_menu)
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.explore_new_list)) },
                    leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onNewList()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.explore_save_as_list)) },
                    leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onSaveAsList()
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.explore_reset_rules)) },
                    leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                    onClick = {
                        menuOpen = false
                        onResetRules()
                    }
                )
            }
        }
        if (isDirty) {
            TextButton(
                onClick = onSaveToList,
                modifier = Modifier.padding(start = 12.dp)
            ) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.explore_save_to_list))
            }
        }
    }
}

// --------------------------------------------------------------------
// The axis chips
// --------------------------------------------------------------------

/**
 * What the four rule axes are currently set to.
 *
 * Each chip opens the one filter sheet rather than a picker per axis: there
 * are four axes plus two switches and three text fields in total, and four
 * separate sheets to open and dismiss before anything is actually set is a
 * worse arrangement than one sheet holding all of it. The chip therefore
 * advertises the current value and the sheet is the editor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreFilterBar(rules: ExploreRules, onOpenFilters: () -> Unit) {
    val axes = listOf(
        stringResource(R.string.explore_axis_grouping) to groupingLabel(rules.grouping),
        stringResource(R.string.explore_axis_sort) to sortLabel(rules.sort),
        stringResource(R.string.explore_axis_length) to lengthLabel(rules.length),
        stringResource(R.string.explore_axis_age) to ageLabel(rules.freshness)
    )
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(axes.size + 1, key = { if (it < axes.size) "axis_$it" else "axis_more" }) { index ->
            val isAxis = index < axes.size
            val (label, value) = if (isAxis) axes[index] else "" to ""
            AssistChip(
                onClick = onOpenFilters,
                label = {
                    Text(
                        text = if (isAxis) "$label: $value" else stringResource(R.string.explore_filters),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = if (isAxis) Icons.Rounded.FilterAlt else Icons.Rounded.Tune,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    labelColor = MaterialTheme.colorScheme.onSurface,
                    leadingIconContentColor = MaterialTheme.colorScheme.primary
                ),
                border = null
            )
        }
    }
}

// --------------------------------------------------------------------
// Lists
// --------------------------------------------------------------------

private enum class ListAction { RENAME, DUPLICATE, PIN, DELETE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreListRow(
    lists: List<ExploreList>,
    selectedListId: String?,
    onSelect: (String?) -> Unit,
    onAdd: () -> Unit,
    onMenu: (ExploreList, ListAction) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        item(key = "explore_default_card") {
            ExploreListCard(
                name = stringResource(R.string.explore_default_list),
                summary = stringResource(R.string.explore_default_list_sub),
                selected = selectedListId == null,
                onClick = { onSelect(null) },
                modifier = Modifier.width(176.dp)
            )
        }
        items(lists, key = { it.id }) { list ->
            var menuOpen by remember { mutableStateOf(false) }
            ExploreListCard(
                name = list.name,
                summary = listSummary(list),
                selected = selectedListId == list.id,
                onClick = { onSelect(list.id) },
                modifier = Modifier.width(176.dp),
                trailing = {
                    // A real button rather than a long press on the card: a
                    // long press would have to be a second gesture competing
                    // with the card's own click, and nothing about a hidden
                    // gesture is discoverable.
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .clickable { menuOpen = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MoreVert,
                            contentDescription = stringResource(R.string.explore_list_menu),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            )
            ExploreListMenu(
                expanded = menuOpen,
                pinned = list.pinned,
                onDismiss = { menuOpen = false },
                onAction = { action ->
                    menuOpen = false
                    onMenu(list, action)
                }
            )
        }
        item(key = "explore_add_card") {
            ExploreAddCard(onClick = onAdd)
        }
    }
}

@Composable
private fun ExploreListMenu(
    expanded: Boolean,
    pinned: Boolean,
    onDismiss: () -> Unit,
    onAction: (ListAction) -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(R.string.explore_rename)) },
            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
            onClick = { onAction(ListAction.RENAME) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.explore_duplicate)) },
            leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
            onClick = { onAction(ListAction.DUPLICATE) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(if (pinned) R.string.explore_unpin else R.string.explore_pin)) },
            leadingIcon = { Icon(Icons.Rounded.PushPin, contentDescription = null) },
            onClick = { onAction(ListAction.PIN) }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.explore_delete)) },
            leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
            onClick = { onAction(ListAction.DELETE) }
        )
    }
}

/**
 * A saved list as a tile: the house card at [SentupRadii.tile], and the open
 * one wearing an emerald ring plus a wash of the accent, so "which list am I
 * looking at" is answerable at a glance without the tile shouting. A press
 * pushes the tile inward; nothing flashes.
 */
@Composable
private fun ExploreListCard(
    name: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: (@Composable () -> Unit)? = null
) {
    val shape = RoundedCornerShape(SentupRadii.tile)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "exploreListPress"
    )
    val ink = if (selected) {
        MaterialTheme.colorScheme.onSurface
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(
        modifier = modifier
            .scale(scale)
            .sentupCard(shape = shape, elevation = 1)
            .then(
                if (selected) {
                    Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                } else {
                    Modifier
                }
            )
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .heightIn(min = 84.dp)
    ) {
        Row(
            modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        ink
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = summary,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun ExploreAddCard(onClick: () -> Unit) {
    val shape = RoundedCornerShape(SentupRadii.tile)
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = tween(durationMillis = 90),
        label = "exploreAddPress"
    )
    Box(
        modifier = Modifier
            .scale(scale)
            .width(120.dp)
            .heightIn(min = 84.dp)
            .sentupCard(shape = shape, elevation = 1)
            .clip(shape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = stringResource(R.string.explore_new_list),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

// --------------------------------------------------------------------
// Shelves
// --------------------------------------------------------------------

/**
 * One grouped shelf: a header and a rail of cards.
 *
 * **Only the grouped shelves come here.** The flat one is emitted straight into
 * the LazyColumn by `videoListItems`, which is a `LazyListScope` extension and
 * so has to be called from the builder itself - the one place a flat list can
 * honour the layout the user chose everywhere else without a wrapper list that
 * would break its keys.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreShelfSection(
    shelf: ExploreShelf,
    openListName: String?,
    onVideoClick: (VideoItem) -> Unit,
    onLongPress: ((VideoItem, Boolean) -> Unit)?,
    onOpenChannel: (String) -> Unit,
    onPlayAll: ((List<VideoItem>, String) -> Unit)?
) {
    val title = shelfTitle(shelf, openListName)
    Column(modifier = Modifier.fillMaxWidth()) {
        ExploreShelfHeader(
            title = title,
            shelf = shelf,
            onOpenChannel = onOpenChannel,
            onPlayAll = onPlayAll
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(shelf.videos, key = { "explore_" + shelf.id + "_" + it.videoId }) { video ->
                ExploreVideoCard(
                    video = video,
                    modifier = Modifier.width(248.dp),
                    onVideoClick = onVideoClick,
                    onLongPress = onLongPress,
                    onOpenChannel = onOpenChannel
                )
            }
        }
    }
}

/** The card both shapes draw, so the two cannot drift apart. */
@Composable
private fun ExploreVideoCard(
    video: VideoItem,
    modifier: Modifier,
    onVideoClick: (VideoItem) -> Unit,
    onLongPress: ((VideoItem, Boolean) -> Unit)?,
    onOpenChannel: (String) -> Unit
) {
    VideoCard(
        video = video,
        onClick = { onVideoClick(video) },
        onLongClick = onLongPress?.let { press -> { press(video, false) } },
        onOpenChannel = onOpenChannel,
        modifier = modifier
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ExploreShelfHeader(
    title: String,
    shelf: ExploreShelf,
    onOpenChannel: (String) -> Unit,
    onPlayAll: ((List<VideoItem>, String) -> Unit)?
) {
    // The creator is the point of a channel shelf, so the name and the avatar
    // both open the channel rather than being decoration on it.
    val channelReference = shelf.channelNavigationReference
    // Typed, not inferred: the body ends in a safe call whose value is
    // `Unit?`, and a `() -> Unit?` is not a `() -> Unit`.
    val openChannel: () -> Unit = {
        if (shelf.kind == ExploreShelfKind.CHANNEL) channelReference?.let(onOpenChannel)
    }
    val countText = pluralStringResource(
        R.plurals.explore_video_count,
        shelf.videos.size,
        shelf.videos.size
    )
    if (shelf.kind != ExploreShelfKind.CHANNEL || shelf.avatarUrl.isNullOrBlank()) {
        // No avatar to place: the plain group header, exactly as every other
        // group in the app names its card.
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SentupGroupHeader(
                text = title,
                subtitle = countText,
                modifier = Modifier.weight(1f)
            )
            if (onPlayAll != null) {
                ShelfPlayAllButton(
                    onClick = { onPlayAll(shelf.videos, title) },
                    modifier = Modifier.padding(end = 4.dp)
                )
            }
        }
        return
    }
    // A channel shelf leads with its avatar, so the header is drawn by hand
    // with SentupGroupHeader's exact metrics (its own inset plus the avatar's
    // width between them) to keep the creator's face and name one object.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = SentupGroup.inset + SentupGroup.dividerInset,
                end = 12.dp,
                top = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = shelf.avatarUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(30.dp)
                .clip(CircleShape)
                .combinedClickable(onClick = openChannel)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = 8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.then(
                    if (channelReference != null) {
                        Modifier.combinedClickable(onClick = openChannel)
                    } else {
                        Modifier
                    }
                )
            )
            Text(
                text = countText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (onPlayAll != null) {
            ShelfPlayAllButton(onClick = { onPlayAll(shelf.videos, title) })
        }
    }
}

/** The shelf's play-all affordance: a quiet circle, never a filled block. */
@Composable
private fun ShelfPlayAllButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = stringResource(R.string.explore_play_all),
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

// --------------------------------------------------------------------
// Empty states
// --------------------------------------------------------------------

@Composable
private fun ExploreEmptyState(
    isLoading: Boolean,
    hasAnySource: Boolean,
    loadFailed: Boolean,
    onRetry: () -> Unit,
    onPreset: (ExplorePreset) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            LoadingIndicator(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            return@Column
        }
        Icon(
            imageVector = when {
                loadFailed -> Icons.Rounded.CloudOff
                hasAnySource -> Icons.Rounded.AutoAwesome
                else -> Icons.Rounded.Dns
            },
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(56.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(
                when {
                    loadFailed -> R.string.explore_failed_title
                    hasAnySource -> R.string.explore_empty_title
                    else -> R.string.explore_nothing_title
                }
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(
                when {
                    loadFailed -> R.string.explore_failed_sub
                    hasAnySource -> R.string.explore_empty_sub
                    else -> R.string.explore_nothing_sub
                }
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        if (loadFailed) {
            TextButton(onClick = onRetry) {
                Text(stringResource(R.string.explore_retry))
            }
        } else {
            ExplorePresetRow(onPreset = onPreset)
        }
    }
}

/**
 * The starting points, offered when there is nothing to show.
 *
 * Each is a complete rule set rather than a query, so tapping one creates a
 * list that keeps answering itself - which is the whole difference between
 * this and the category chips YouTube's Explore is made of. They arrive as
 * one inset card of rows, the same shape every chooser in the app uses: icon,
 * name, what it does, and a chevron saying the row opens something.
 */
@Composable
private fun ExplorePresetRow(onPreset: (ExplorePreset) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SentupGroupHeader(
            text = stringResource(R.string.explore_start_with),
            modifier = Modifier.padding(top = 8.dp)
        )
        SentupGroupCard(showDividers = true, contentPadding = 2.dp) {
            ExplorePreset.entries.forEachIndexed { index, preset ->
                if (index > 0) SentupDivider()
                SentupRow(
                    title = stringResource(presetTitle(preset)),
                    icon = presetIcon(preset),
                    subtitle = stringResource(presetSubtitle(preset)),
                    showChevron = true,
                    onClick = { onPreset(preset) }
                )
            }
        }
    }
}

private fun presetIcon(preset: ExplorePreset): ImageVector = when (preset) {
    ExplorePreset.BY_CHANNEL -> Icons.Rounded.Dns
    ExplorePreset.THIS_WEEK -> Icons.Rounded.Schedule
    ExplorePreset.QUICK_UNSEEN -> Icons.Rounded.Whatshot
    ExplorePreset.BY_LENGTH -> Icons.Rounded.VideoLibrary
    ExplorePreset.LIVE_NOW -> Icons.Rounded.Wifi
}

private fun presetTitle(preset: ExplorePreset): Int = when (preset) {
    ExplorePreset.BY_CHANNEL -> R.string.explore_preset_by_channel
    ExplorePreset.THIS_WEEK -> R.string.explore_preset_this_week
    ExplorePreset.QUICK_UNSEEN -> R.string.explore_preset_quick_unseen
    ExplorePreset.BY_LENGTH -> R.string.explore_preset_by_length
    ExplorePreset.LIVE_NOW -> R.string.explore_preset_live_now
}

private fun presetSubtitle(preset: ExplorePreset): Int = when (preset) {
    ExplorePreset.BY_CHANNEL -> R.string.explore_preset_by_channel_sub
    ExplorePreset.THIS_WEEK -> R.string.explore_preset_this_week_sub
    ExplorePreset.QUICK_UNSEEN -> R.string.explore_preset_quick_unseen_sub
    ExplorePreset.BY_LENGTH -> R.string.explore_preset_by_length_sub
    ExplorePreset.LIVE_NOW -> R.string.explore_preset_live_now_sub
}

// --------------------------------------------------------------------
// Naming
// --------------------------------------------------------------------

private enum class NameAction { SAVE_RULES, NEW_LIST, RENAME, DUPLICATE, PRESET, SAVE_TO_OPEN }

/**
 * Why the naming dialog is open.
 *
 * One request type behind five entry points - new list, save as, rename,
 * duplicate and preset - so they share a field and a confirm handler instead of
 * being five nearly identical screens.
 */
private data class NameRequest(
    val action: NameAction,
    val listId: String? = null,
    val preset: ExplorePreset? = null,
    val seedName: String = ""
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreNameDialog(
    request: NameRequest?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    if (request == null) return
    var name by rememberSaveable(request.action, request.listId) {
        mutableStateOf(request.seedName)
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    when (request.action) {
                        NameAction.SAVE_RULES -> R.string.explore_save_as_list
                        NameAction.NEW_LIST -> R.string.explore_new_list
                        NameAction.RENAME -> R.string.explore_rename
                        NameAction.DUPLICATE -> R.string.explore_duplicate
                        NameAction.PRESET -> R.string.explore_start_with
                        NameAction.SAVE_TO_OPEN -> R.string.explore_save_to_list
                    }
                )
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text(stringResource(R.string.explore_list_name)) }
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) {
                Text(stringResource(R.string.explore_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.explore_cancel))
            }
        }
    )
}

// --------------------------------------------------------------------
// The filter sheet
// --------------------------------------------------------------------

/**
 * Every rule in one scrollable sheet.
 *
 * **It scrolls, and it has to.** The content is four axes, four switches and
 * three text fields, which is over the height a sheet gets on a phone in
 * landscape - and a sheet that does not scroll has a silent hard ceiling that
 * looks identical at three items and at thirty.
 *
 * Changes apply live rather than on a Save button: the shelves are derived, so
 * there is nothing to commit, and the effect of a rule is the point.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExploreFilterSheet(
    rules: ExploreRules,
    channels: List<com.sentup.android.data.LocalSubscription>,
    onRulesChange: (ExploreRules) -> Unit,
    onSaveAsList: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.explore_filters),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onSaveAsList) {
                    Text(stringResource(R.string.explore_save_as_list))
                }
            }

            ExploreSourcesSection(
                sources = rules.sources,
                onChange = { onRulesChange(rules.copy(sources = it)) }
            )

            ExploreAxisSection(stringResource(R.string.explore_axis_grouping)) {
                ExploreChoiceRow(
                    labels = ExploreGrouping.entries.map { groupingLabel(it) },
                    selectedIndex = ExploreGrouping.entries.indexOf(rules.grouping),
                    onSelect = { onRulesChange(rules.copy(grouping = ExploreGrouping.entries[it])) }
                )
            }

            ExploreAxisSection(stringResource(R.string.explore_axis_sort)) {
                ExploreChoiceRow(
                    labels = ExploreSort.entries.map { sortLabel(it) },
                    selectedIndex = ExploreSort.entries.indexOf(rules.sort),
                    onSelect = { onRulesChange(rules.copy(sort = ExploreSort.entries[it])) }
                )
            }

            ExploreAxisSection(stringResource(R.string.explore_axis_length)) {
                ExploreChoiceRow(
                    labels = ExploreLength.entries.map { lengthLabel(it) },
                    selectedIndex = ExploreLength.entries.indexOf(rules.length),
                    onSelect = { onRulesChange(rules.copy(length = ExploreLength.entries[it])) }
                )
            }

            ExploreAxisSection(stringResource(R.string.explore_axis_age)) {
                ExploreChoiceRow(
                    labels = ExploreFreshness.entries.map { ageLabel(it) },
                    selectedIndex = ExploreFreshness.entries.indexOf(rules.freshness),
                    onSelect = { onRulesChange(rules.copy(freshness = ExploreFreshness.entries[it])) }
                )
            }

            Spacer(modifier = Modifier.height(SentupGroup.gap - 8.dp))

            SentupGroupCard(contentPadding = 2.dp) {
                ExploreSwitchRow(
                    icon = Icons.Rounded.History,
                    title = stringResource(R.string.explore_hide_watched),
                    subtitle = stringResource(R.string.explore_hide_watched_sub),
                    checked = rules.hideWatched,
                    onToggle = { onRulesChange(rules.copy(hideWatched = it)) }
                )
                SentupDivider()
                ExploreSwitchRow(
                    icon = Icons.Rounded.Wifi,
                    title = stringResource(R.string.explore_live_only),
                    subtitle = stringResource(R.string.explore_live_only_sub),
                    checked = rules.liveOnly,
                    onToggle = { onRulesChange(rules.copy(liveOnly = it)) }
                )
            }

            ExploreWordField(
                label = stringResource(R.string.explore_include_words),
                value = rules.includeWords,
                onChange = { onRulesChange(rules.copy(includeWords = it)) }
            )

            ExploreWordField(
                label = stringResource(R.string.explore_exclude_words),
                value = rules.excludeWords,
                onChange = { onRulesChange(rules.copy(excludeWords = it)) }
            )

            ExploreChannelPicker(
                rules = rules,
                channels = channels,
                onRulesChange = onRulesChange
            )

            Text(
                text = stringResource(R.string.explore_filter_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ExploreSourcesSection(
    sources: Set<ExploreSource>,
    onChange: (Set<ExploreSource>) -> Unit
) {
    Column(modifier = Modifier.padding(top = 8.dp)) {
        SentupGroupHeader(stringResource(R.string.explore_sources))
        SentupGroupCard(contentPadding = 2.dp) {
            ExploreSource.entries.forEachIndexed { index, source ->
                if (index > 0) SentupDivider()
                ExploreSwitchRow(
                    icon = sourceIcon(source),
                    title = stringResource(sourceTitle(source)),
                    subtitle = stringResource(sourceSubtitle(source)),
                    checked = source in sources,
                    // Never leaves zero sources: a list reading from nothing cannot
                    // be told apart from a broken one, so the last one refuses to
                    // go off rather than leaving a screen that is permanently empty.
                    onToggle = { on ->
                        onChange(
                            when {
                                on -> sources + source
                                sources.size > 1 -> sources - source
                                else -> sources
                            }
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun ExploreAxisSection(title: String, content: @Composable () -> Unit) {
    Column(modifier = Modifier.padding(top = SentupGroup.gap - 8.dp)) {
        SentupGroupHeader(title)
        content()
    }
}

/**
 * A row of exclusive answers, drawn as pills: these *are* mutually exclusive,
 * and a selected one carries the accent fill so it reads as chosen rather
 * than as a suggestion. Loose chips, tight gaps - the connected-strip shape
 * these used to sit in read as one control cut into slices.
 */
@Composable
private fun ExploreChoiceRow(
    labels: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEachIndexed { index, label ->
            val selected = selectedIndex == index
            Surface(
                modifier = Modifier.weight(1f),
                shape = CircleShape,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surfaceContainerHighest
                },
                contentColor = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                onClick = { onSelect(index) }
            ) {
                Box(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * A rule with a switch on it, as a house row inside the sheet's card: icon,
 * title, why it exists, and the switch. The whole row is the target, not just
 * the switch.
 */
@Composable
private fun ExploreSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    SentupRow(
        title = title,
        icon = icon,
        subtitle = subtitle,
        onClick = { onToggle(!checked) },
        trailing = {
            Switch(checked = checked, onCheckedChange = onToggle)
        }
    )
}

/**
 * Channels are picked from the ones this device already knows, rather than
 * typed: the ids are not something anyone remembers, so a comma list was a
 * field that could only be filled by copying. Every follow is one tap away,
 * the selected ones are listed by name, and the whole list can be cleared.
 */
@Composable
private fun ExploreChannelPicker(
    rules: ExploreRules,
    channels: List<com.sentup.android.data.LocalSubscription>,
    onRulesChange: (ExploreRules) -> Unit
) {
    var open by rememberSaveable { mutableStateOf(false) }
    val selected = rules.channelIds

    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.explore_channel_words),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = if (selected.isEmpty()) {
                        stringResource(R.string.explore_channels_none)
                    } else {
                        selected.joinToString(", ") { id ->
                            channels.firstOrNull { it.channelId == id }?.name ?: id
                        }
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.size(8.dp))
            Button(onClick = { open = true }) {
                Text(stringResource(R.string.explore_channels_pick))
            }
        }
        if (selected.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.explore_channels_selected, selected.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { onRulesChange(rules.copy(channelIds = emptyList())) }) {
                    Text(stringResource(R.string.explore_channels_clear))
                }
            }
        }
    }

    if (open) {
        ModalBottomSheet(
            onDismissRequest = { open = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 620.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.explore_channels_pick),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { onRulesChange(rules.copy(channelIds = emptyList())) }) {
                        Text(stringResource(R.string.explore_channels_clear))
                    }
                }
                if (channels.isEmpty()) {
                    Text(
                        text = stringResource(R.string.explore_channels_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 24.dp)
                    )
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().weight(1f, fill = false)) {
                        items(channels, key = { it.channelId }) { channel ->
                            val checked = channel.channelId in selected
                            SentupRow(
                                title = channel.name,
                                subtitle = channel.handle,
                                onClick = {
                                    onRulesChange(
                                        rules.copy(
                                            channelIds = if (checked) {
                                                selected - channel.channelId
                                            } else {
                                                selected + channel.channelId
                                            }
                                        )
                                    )
                                },
                                trailing = {
                                    Checkbox(checked = checked, onCheckedChange = {
                                        onRulesChange(
                                            rules.copy(
                                                channelIds = if (it) {
                                                    selected + channel.channelId
                                                } else {
                                                    selected - channel.channelId
                                                }
                                            )
                                        )
                                    })
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * A comma-separated list of words, kept as a list.
 *
 * Stored as a list rather than as one string so a rule can grow a second word
 * without a migration, and so the engine matches whole entries instead of
 * re-parsing prose for every shelf.
 */
@Composable
private fun ExploreWordField(
    label: String,
    value: List<String>,
    onChange: (List<String>) -> Unit
) {
    var text by rememberSaveable(value) { mutableStateOf(value.joinToString(", ")) }
    OutlinedTextField(
        value = text,
        onValueChange = { next ->
            text = next
            onChange(next.split(',').map { it.trim() }.filter { it.isNotBlank() })
        },
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    )
}

// --------------------------------------------------------------------
// Labels
// --------------------------------------------------------------------

@Composable
private fun groupingLabel(grouping: ExploreGrouping): String = stringResource(
    when (grouping) {
        ExploreGrouping.NONE -> R.string.explore_group_none
        ExploreGrouping.CHANNEL -> R.string.explore_group_channel
        ExploreGrouping.DURATION -> R.string.explore_group_duration
        ExploreGrouping.FRESHNESS -> R.string.explore_group_freshness
    }
)

@Composable
private fun sortLabel(sort: ExploreSort): String = stringResource(
    when (sort) {
        ExploreSort.NEWEST -> R.string.explore_sort_newest
        ExploreSort.OLDEST -> R.string.explore_sort_oldest
        ExploreSort.MOST_VIEWED -> R.string.explore_sort_views
        ExploreSort.QUICKEST -> R.string.explore_sort_quickest
        ExploreSort.LONGEST -> R.string.explore_sort_longest
        ExploreSort.SHUFFLED -> R.string.explore_sort_shuffled
    }
)

@Composable
private fun lengthLabel(length: ExploreLength): String = stringResource(
    when (length) {
        ExploreLength.ANY -> R.string.explore_length_any
        ExploreLength.SHORT -> R.string.explore_length_short
        ExploreLength.MEDIUM -> R.string.explore_length_medium
        ExploreLength.LONG -> R.string.explore_length_long
    }
)

@Composable
private fun ageLabel(freshness: ExploreFreshness): String = stringResource(
    when (freshness) {
        ExploreFreshness.ANY -> R.string.explore_age_any
        ExploreFreshness.TODAY -> R.string.explore_age_today
        ExploreFreshness.WEEK -> R.string.explore_age_week
        ExploreFreshness.MONTH -> R.string.explore_age_month
        ExploreFreshness.QUARTER -> R.string.explore_age_quarter
    }
)

/**
 * What a shelf is called.
 *
 * A channel shelf is named by its channel and a band shelf by the band. The
 * flat one is named by the open list where there is one, and falls back to a
 * plain phrase rather than to a shelf id nobody should ever have to read.
 */
@Composable
private fun shelfTitle(shelf: ExploreShelf, openListName: String? = null): String =
    when (shelf.kind) {
        ExploreShelfKind.CHANNEL -> shelf.channelName?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.explore_shelf_unknown_channel)

        ExploreShelfKind.DURATION -> shelf.band?.let { lengthLabel(it) }
            ?: stringResource(R.string.explore_shelf_other_length)

        ExploreShelfKind.FRESHNESS -> when (shelf.age) {
            // Inside a grouping, ANY is the remainder rather than "no limit":
            // the window has already been applied as a filter by then.
            ExploreFreshness.ANY -> stringResource(R.string.explore_age_older)
            null -> stringResource(R.string.explore_shelf_default)
            else -> ageLabel(shelf.age)
        }

        ExploreShelfKind.WHOLE -> openListName?.takeIf { it.isNotBlank() }
            ?: stringResource(R.string.explore_shelf_default)
    }

@Composable
private fun listSummary(list: ExploreList): String {
    val summary = list.summary()
    val parts = buildList {
        add(groupingLabel(summary.grouping))
        if (summary.length != ExploreLength.ANY) add(lengthLabel(summary.length))
        if (summary.freshness != ExploreFreshness.ANY) add(ageLabel(summary.freshness))
        if (summary.liveOnly) add(stringResource(R.string.explore_live_only))
        if (summary.hideWatched) add(stringResource(R.string.explore_hide_watched))
    }
    return parts.joinToString(" · ")
}

private fun sourceIcon(source: ExploreSource): ImageVector = when (source) {
    ExploreSource.SUBSCRIPTIONS -> Icons.Rounded.Dns
    ExploreSource.HISTORY -> Icons.Rounded.History
    ExploreSource.DEVICE_PLAYLISTS -> Icons.Rounded.VideoLibrary
    ExploreSource.LIKED -> Icons.Rounded.FavoriteBorder
}

private fun sourceTitle(source: ExploreSource): Int = when (source) {
    ExploreSource.SUBSCRIPTIONS -> R.string.explore_source_subscriptions
    ExploreSource.HISTORY -> R.string.explore_source_history
    ExploreSource.DEVICE_PLAYLISTS -> R.string.explore_source_device_playlists
    ExploreSource.LIKED -> R.string.explore_source_liked
}

private fun sourceSubtitle(source: ExploreSource): Int = when (source) {
    ExploreSource.SUBSCRIPTIONS -> R.string.explore_source_subscriptions_sub
    ExploreSource.HISTORY -> R.string.explore_source_history_sub
    ExploreSource.DEVICE_PLAYLISTS -> R.string.explore_source_device_playlists_sub
    ExploreSource.LIKED -> R.string.explore_source_liked_sub
}