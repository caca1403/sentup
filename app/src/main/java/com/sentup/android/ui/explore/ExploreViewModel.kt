package com.sentup.android.ui.explore

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.sentup.android.data.ExploreEngine
import com.sentup.android.data.ExploreGrouping
import com.sentup.android.data.ExploreLength
import com.sentup.android.data.ExploreList
import com.sentup.android.data.ExploreListsRepository
import com.sentup.android.data.ExplorePreset
import com.sentup.android.data.ExploreRules
import com.sentup.android.data.ExploreShelf
import com.sentup.android.data.ExploreShelfKind
import com.sentup.android.data.ExploreSort
import com.sentup.android.data.ExploreSource
import com.sentup.android.data.LocalSubscription
import com.sentup.android.data.LocalSubscriptionsRepository
import com.sentup.android.data.LocalVideoPlaylistsRepository
import com.sentup.android.data.NotInterestedRepository
import com.sentup.android.data.ProfileManager
import com.sentup.android.data.SessionManager
import com.sentup.android.data.SubscriptionFeedCache
import com.sentup.android.data.VideoHistoryRepository
import com.sentup.android.data.VideoItem
import com.sentup.android.data.YouTubeRepository
import kotlin.random.Random
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The Explore tab: saved lists, the rules on screen, and the shelves they
 * produce.
 *
 * **It owns rules and shelves; it does not own the feeds.** Candidates come
 * from sources the app already holds, or that it can name with a single
 * request. The subscriptions half is read out of [SubscriptionFeedCache]
 * rather than fetched again: `HomeViewModel.observeSubscriptionFeedWarmup`
 * already fills that cache from app start, independently of which tab is open,
 * so re-running a per-channel fan-out here would double the most expensive
 * thing this app does for its signed-out half. A cache miss falls back to one
 * account browse when signed in, and to nothing otherwise - which the
 * interface words as "nothing to show yet", not as a failure.
 *
 * **Editing a rule is not a fetch.** Everything the engine needs is in memory
 * by the time a rule can be changed, so a grouping or a length band re-answers
 * on the next frame. Only a rule that *adds a source* asks for one.
 */
class ExploreViewModel(application: Application) : AndroidViewModel(application) {

    private val app get() = getApplication<Application>()

    private val listsRepository = ExploreListsRepository(application)
    private val youtubeRepository = YouTubeRepository(application)
    private val sessionManager = SessionManager(application)
    private val videoHistoryRepository = VideoHistoryRepository(application)
    private val localVideoPlaylists = LocalVideoPlaylistsRepository(application)
    private val localSubscriptions = LocalSubscriptionsRepository(application)
    private val notInterested = NotInterestedRepository(application)
    private val subscriptionFeedCache = SubscriptionFeedCache(application)

    val lists: StateFlow<List<ExploreList>> = listsRepository.lists
    val selectedListId: StateFlow<String?> = listsRepository.selectedListId

    /** The rules on screen, which are not necessarily a saved list's yet. */
    private val _rules = MutableStateFlow(DEFAULT_RULES)
    val rules: StateFlow<ExploreRules> = _rules.asStateFlow()

    /**
     * What each gathered source produced, keyed by source.
     *
     * Per source rather than one merged list, because a rule can turn a source
     * off: with a single merged list there is nothing to say which videos came
     * from where, so unchecking a source would filter nothing and its videos
     * would stay on screen for ever.
     */
    private val candidatesBySource = mutableMapOf<ExploreSource, List<VideoItem>>()

    /** The profile's watch history as a set, read beside the candidates so the
     *  rebuild that follows never has to touch the store from composition. */
    private val _watchedIds = MutableStateFlow<Set<String>>(emptySet())

    private val _shelves = MutableStateFlow<List<ExploreShelf>>(emptyList())
    val shelves: StateFlow<List<ExploreShelf>> = _shelves.asStateFlow()

    /**
     * Real YouTube discovery, independent of the user's own sources: even an
     * empty subscriptions page opens on something alive. Rendered as ordinary
     * WHOLE shelves, so play-all, long-press and channel taps behave exactly
     * as they do everywhere else on this page.
     */
    private val _discover = MutableStateFlow<List<ExploreShelf>>(emptyList())
    val discover: StateFlow<List<ExploreShelf>> = _discover.asStateFlow()
    private var discoverLoading = false

    fun loadDiscovery() {
        if (discoverLoading || _discover.value.isNotEmpty()) return
        discoverLoading = true
        viewModelScope.launch {
            val page = runCatching { youtubeRepository.getTrendingVideos() }.getOrNull()
            _discover.value = page?.videos?.takeIf { it.isNotEmpty() }?.let { vids ->
                listOf(ExploreShelf("discover_trending", ExploreShelfKind.WHOLE, vids.take(24)))
            }.orEmpty()
            discoverLoading = false
        }
    }

    /**
     * Every channel the picker may offer, newest first, so the user selects
     * channels by tapping them instead of typing ids into a comma list.
     *
     * The device-local follows are the store that can be read without a
     * request; the signed-in account's own subscriptions are added from the
     * same cache the feed is built from, so the two halves cannot disagree.
     */
    private val _channels = MutableStateFlow<List<LocalSubscription>>(emptyList())
    val channels: StateFlow<List<LocalSubscription>> = _channels.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    /**
     * Set when a load failed. A flag rather than the message, because the text
     * would be a raw exception string and the interface has nothing useful to
     * do with one - the shelf it already has is kept, and the user is offered
     * a retry.
     */
    private val _loadFailed = MutableStateFlow(false)
    val loadFailed: StateFlow<Boolean> = _loadFailed.asStateFlow()

    /** True once a load has finished, so an empty result can be told apart
     *  from a pending one - the same distinction the video Home feed keeps. */
    private val _hasLoaded = MutableStateFlow(false)
    val hasLoaded: StateFlow<Boolean> = _hasLoaded.asStateFlow()

    private val _isSignedIn = MutableStateFlow(sessionManager.isLoggedIn())

    /** Whether an account is available: the Liked source and the account
     *  browse both need one, and the interface says so rather than failing. */
    val isSignedIn: StateFlow<Boolean> = _isSignedIn.asStateFlow()

    /** Whether this profile has anything at all an Explore list could draw on,
     *  so an empty screen can tell "nothing matched" from "nothing to match". */
    private val _hasAnySource = MutableStateFlow(false)
    val hasAnySource: StateFlow<Boolean> = _hasAnySource.asStateFlow()

    /** Sources already gathered, so editing a rule only fetches what it adds. */
    private val gathered = mutableSetOf<ExploreSource>()
    private var loadJob: Job? = null

    /**
     * Rotated per load so a shuffle genuinely reorders on a refresh while
     * staying put while the tab sits still. A list that reshuffles itself on
     * every recomposition is not a list anybody can find anything in - and
     * neither is one that reshuffles because a switch was toggled, which is why
     * this is turned by [load] and not by [rebuild].
     */
    private var shuffleSeed = 0L

    private fun rotateShuffle() {
        shuffleSeed += 1
    }

    /**
     * The picker's list, rebuilt from the device-local follows plus whatever
     * the account feed is holding. A name remembered by the candidates is
     * better than a bare id, so the feed wins where the two disagree.
     */
    private fun publishChannels() {
        val byName = mutableMapOf<String, LocalSubscription>()
        subscriptionFeedCache.read()?.videos?.forEach { video ->
            val id = video.channelId
            if (!id.isNullOrBlank()) {
                byName[id] = LocalSubscription(
                    channelId = id,
                    name = video.channelName,
                    avatarUrl = video.channelIconUrl
                )
            }
        }
        localSubscriptions.getAll().forEach { local ->
            val seen = byName[local.channelId]
            byName[local.channelId] = local.copy(
                // Keep the account's avatar and handle when only it has them.
                avatarUrl = local.avatarUrl ?: seen?.avatarUrl,
                handle = local.handle ?: seen?.handle
            )
        }
        _channels.value = byName.values.sortedBy { it.name.lowercase() }
    }

    init {
        publishChannels()
        _rules.value = listsRepository.find(listsRepository.selectedListId.value)?.rules ?: DEFAULT_RULES
        viewModelScope.launch {
            ProfileManager(app).activeProfileId
                .drop(1)
                .distinctUntilChanged()
                .collect {
                    // History and the account feed are profile-scoped, so the
                    // shelves have to be answered again for whoever is signed
                    // in now rather than showing the last profile's videos.
                    _isSignedIn.value = sessionManager.isLoggedIn()
                    gathered.clear()
                    // The candidates are the previous profile's videos; leaving
                    // them would show them under the new identity until the
                    // refetch landed.
                    candidatesBySource.clear()
                    load(force = true)
                }
        }
    }

    /**
     * Answer the current rules.
     *
     * Three paths: everything the rules name is already gathered, so the
     * shelves are re-answered from memory with no coroutine and no spinner;
     * something local is missing, so it is read off the device in the
     * background without the spinner either; otherwise a load starts and
     * [gather] fetches what the network is needed for.
     */
    fun load(force: Boolean = false) {
        if (loadJob?.isActive == true) return
        val active = _rules.value
        publishChannels()
        val missing = active.sources.filterNot { it in gathered }.toSet()
        // Not "nothing to do": the rules have already changed, and the shelves
        // are a pure function of them, so re-answering costs nothing and is
        // the whole point of switching list.
        if (!force && missing.isEmpty()) {
            rebuild()
            return
        }
        if (!force && missing.none { it.needsNetwork }) {
            // Read without the spinner: this is a preference read and a file
            // read, and a progress indicator for one of those reads as a hang.
            loadJob = viewModelScope.launch {
                readLocal(missing)
                rotateShuffle()
                rebuild()
                _hasLoaded.value = true
            }
            return
        }
        loadJob = viewModelScope.launch {
            rotateShuffle()
            _isLoading.value = true
            _loadFailed.value = false
            try {
                gather(active, force)
                rebuild()
                _hasLoaded.value = true
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A source that failed leaves the others standing: whatever was
                // gathered before the failure is still drawn, which is why this
                // catch is here and not around the whole thing.
                _loadFailed.value = true
                _hasLoaded.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Open [listId], or the built-in rules when it is null.
     *
     * The selection is written to the store rather than kept here, so returning
     * to the tab lands on the list that was open - the same "coming back is not
     * a reset" rule the Home feed follows.
     */
    fun selectList(listId: String?) {
        listsRepository.select(listId)
        _rules.value = listsRepository.find(listId)?.rules ?: DEFAULT_RULES
        load()
    }

    /**
     * Apply edited rules.
     *
     * A source added here is gathered before the shelves are rebuilt, so a list
     * that adds Liked does not need the tab to be re-entered first; every other
     * change is a pure re-answer of what is already in memory.
     */
    fun updateRules(rules: ExploreRules) {
        _rules.value = rules
        if (rules.sources.any { it !in gathered }) load() else rebuild()
    }

    /** Back to the rules the tab opens with. */
    fun resetRules() {
        _rules.value = DEFAULT_RULES
        rebuild()
    }

    /** Whether the on-screen rules differ from what the open list stores. */
    fun isDirty(): Boolean {
        val openId = listsRepository.selectedListId.value
            ?: return _rules.value != DEFAULT_RULES
        return listsRepository.find(openId)?.rules != _rules.value
    }

    fun saveAsList(name: String) {
        val rules = _rules.value
        viewModelScope.launch { listsRepository.saveAsList(name, rules) }
    }

    /**
     * A new list holding the tab's own default rules, which becomes the open
     * one.
     *
     * Distinct from [saveAsList] on purpose: that one stores what is on screen,
     * and a user asking for "a new list" has not asked to keep whatever filters
     * happened to be set when they pressed the button.
     */
    fun newList(name: String) {
        viewModelScope.launch {
            listsRepository.saveAsList(name, DEFAULT_RULES)
            _rules.value = DEFAULT_RULES
            load()
        }
    }

    fun createFromPreset(preset: ExplorePreset, name: String) {
        viewModelScope.launch { listsRepository.createFromPreset(preset, name) }
    }

    /** Write the on-screen rules back into the open list. */
    fun saveToOpenList() {
        val id = listsRepository.selectedListId.value ?: return
        val rules = _rules.value
        viewModelScope.launch { listsRepository.updateRules(id, rules) }
    }

    fun deleteList(listId: String) {
        val wasOpen = listsRepository.selectedListId.value == listId
        viewModelScope.launch {
            listsRepository.delete(listId)
            // Only the list that was on screen can take the screen's rules with
            // it; deleting another one must leave what is being read alone.
            if (wasOpen) _rules.value = DEFAULT_RULES
            load()
        }
    }

    fun renameList(listId: String, name: String) {
        viewModelScope.launch { listsRepository.rename(listId, name) }
    }

    fun duplicateList(listId: String, name: String) {
        viewModelScope.launch { listsRepository.duplicate(listId, name) }
    }

    fun setPinned(listId: String, pinned: Boolean) {
        viewModelScope.launch { listsRepository.setPinned(listId, pinned) }
    }

    // ---------------- Sources ----------------

    /** Whether reading this source costs a request. */
    private val ExploreSource.needsNetwork: Boolean
        get() = this == ExploreSource.SUBSCRIPTIONS || this == ExploreSource.LIKED

    /**
     * Read the sources in [wanted] into the candidates.
     *
     * The result is deduplicated here rather than in the engine, so a video
     * reached from two sources is one candidate and the copy that arrived
     * first is the one kept - which is the copy most likely to carry a channel
     * id, the field grouping needs.
     */
    private suspend fun gather(rules: ExploreRules, force: Boolean) {
        for (source in rules.sources) {
            // Stored before it is marked gathered. The other order marks a
            // source read whose videos a later failure threw away, and a source
            // marked read with nothing behind it never fetches again.
            candidatesBySource[source] = readSource(source, force)
            gathered += source
        }
        refreshWatchedIds()
        _hasAnySource.value = hasAnythingToDrawFrom()
    }

    /**
     * The local-only half of [gather], for a rule change that added nothing but
     * a source this device can answer by itself. Runs on IO like the rest.
     */
    private suspend fun readLocal(wanted: Set<ExploreSource>) {
        for (source in wanted) {
            // Never reached for the network sources: [load] only takes this path
            // when nothing missing costs a request.
            candidatesBySource[source] = when (source) {
                ExploreSource.SUBSCRIPTIONS, ExploreSource.LIKED -> emptyList()
                ExploreSource.HISTORY -> historyOnce()
                ExploreSource.DEVICE_PLAYLISTS -> localVideoPlaylists.playlists.value
                    .flatMap { it.videos }
            }
            gathered += source
        }
        refreshWatchedIds()
        _hasAnySource.value = hasAnythingToDrawFrom()
    }

    /** The profile's watch history, read once per pass. */
    private suspend fun historyOnce(): List<VideoItem> =
        withContext(Dispatchers.IO) { videoHistoryRepository.getHistory() }

    private suspend fun refreshWatchedIds() {
        _watchedIds.value = historyOnce().mapTo(HashSet()) { it.videoId }
    }

    private suspend fun readSource(source: ExploreSource, force: Boolean): List<VideoItem> =
        when (source) {
            ExploreSource.SUBSCRIPTIONS -> subscriptions(force)
            ExploreSource.HISTORY -> historyOnce()
            ExploreSource.DEVICE_PLAYLISTS -> localVideoPlaylists.playlists.value.flatMap { it.videos }
            ExploreSource.LIKED -> liked()
        }

    /**
     * The subscriptions feed, from the cache `HomeViewModel` keeps warm.
     *
     * The cache is read, never written: its key is built from the subscription
     * source, the sign-in state and the selected group, all of which belong to
     * that ViewModel, and writing a feed under a key this class cannot compute
     * is how a later reader ends up served a list built from a different set of
     * channels.
     */
    private suspend fun subscriptions(force: Boolean): List<VideoItem> {
        val cached = withContext(Dispatchers.IO) { subscriptionFeedCache.read() }
        if (!force && cached != null && cached.videos.isNotEmpty()) return cached.videos
        if (!sessionManager.isLoggedIn()) return cached?.videos.orEmpty()
        return try {
            youtubeRepository.getSubscriptionsFeedPage().videos.ifEmpty { cached?.videos.orEmpty() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // The cache is a service here rather than an optimisation: an empty
            // shelf because one browse failed would be a worse answer than the
            // feed this device already has.
            cached?.videos.orEmpty()
        }
    }

    /** The account's liked videos. Empty when signed out, which is not an error. */
    private suspend fun liked(): List<VideoItem> {
        if (!sessionManager.isLoggedIn()) return emptyList()
        return try {
            youtubeRepository.getPlaylistVideos(LIKED_PLAYLIST_ID)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Whether any source could return anything, so an empty result reads right. */
    private fun hasAnythingToDrawFrom(): Boolean {
        if (candidatesBySource.values.any { it.isNotEmpty() }) return true
        if (localSubscriptions.getAll().isNotEmpty()) return true
        if (localVideoPlaylists.playlists.value.isNotEmpty()) return true
        return sessionManager.isLoggedIn()
    }

    // ---------------- Shelves ----------------

    /**
     * Re-answer the shelves from what is in memory.
     *
     * The blocklist is applied before the rules, so a channel the user blocked
     * cannot come back through a list of their own - the derived-filter rule
     * every other feed follows. Incognito is not consulted: it stops
     * *recording*, and an Explore list reads history rather than writing to it.
     */
    private fun rebuild() {
        val visible = notInterested.filter(
            // Only the sources the rules name, so turning one off takes its
            // videos away rather than filtering them out of a merged list.
            _rules.value.sources.flatMap { candidatesBySource[it].orEmpty() }
                .distinctBy { it.videoId }
        )
        _shelves.value = ExploreEngine.build(
            videos = visible,
            rules = _rules.value,
            nowMs = System.currentTimeMillis(),
            watchedIds = _watchedIds.value,
            random = Random(shuffleSeed)
        )
    }

    companion object {
        /** The synthetic id YouTube's liked-videos list answers to. */
        const val LIKED_PLAYLIST_ID = "LL"

        /**
         * What the tab shows before any list is chosen: everything the followed
         * channels published, one shelf per creator.
         */
        val DEFAULT_RULES = ExploreRules(
            sources = setOf(ExploreSource.SUBSCRIPTIONS),
            grouping = ExploreGrouping.CHANNEL,
            sort = ExploreSort.NEWEST,
            length = ExploreLength.ANY
        )
    }
}