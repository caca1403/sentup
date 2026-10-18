package com.sentup.android.data

import android.content.Context
import com.sentup.android.util.KLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * A starting point for a list, offered on an empty Explore tab.
 *
 * A preset is a rule set with no name: the words belong to the interface, so
 * the data layer stores an id and the screen turns it into a title in the
 * user's own language. Creating one is an ordinary create, so a preset a user
 * ignores simply never exists.
 */
enum class ExplorePreset(val id: String, val rules: ExploreRules) {
    /** Everything the followed channels have published, one shelf per creator. */
    BY_CHANNEL(
        "by_channel",
        ExploreRules(
            sources = setOf(ExploreSource.SUBSCRIPTIONS),
            grouping = ExploreGrouping.CHANNEL,
            sort = ExploreSort.NEWEST
        )
    ),

    /** The last week across every source, newest first. */
    THIS_WEEK(
        "this_week",
        ExploreRules(
            sources = setOf(ExploreSource.SUBSCRIPTIONS, ExploreSource.HISTORY),
            grouping = ExploreGrouping.NONE,
            sort = ExploreSort.NEWEST,
            freshness = ExploreFreshness.WEEK
        )
    ),

    /** What is left to watch, under twenty minutes, nothing already seen. */
    QUICK_UNSEEN(
        "quick_unseen",
        ExploreRules(
            sources = setOf(ExploreSource.SUBSCRIPTIONS, ExploreSource.HISTORY),
            grouping = ExploreGrouping.NONE,
            sort = ExploreSort.QUICKEST,
            length = ExploreLength.SHORT,
            hideWatched = true
        )
    ),

    /** Everything filed by how long it is, which is the grouping YouTube has
     *  no equivalent of at all. */
    BY_LENGTH(
        "by_length",
        ExploreRules(
            sources = setOf(ExploreSource.SUBSCRIPTIONS, ExploreSource.DEVICE_PLAYLISTS),
            grouping = ExploreGrouping.DURATION,
            sort = ExploreSort.NEWEST
        )
    ),

    /** Only broadcasts. */
    LIVE_NOW(
        "live_now",
        ExploreRules(
            sources = setOf(ExploreSource.SUBSCRIPTIONS),
            grouping = ExploreGrouping.NONE,
            sort = ExploreSort.NEWEST,
            liveOnly = true
        )
    );

    companion object {
        fun fromId(id: String?): ExplorePreset? = entries.firstOrNull { it.id == id }
    }
}

/**
 * The Explore lists the user has saved, and which one is open.
 *
 * A list here is a **rule**, never a copy of the videos it found: it is
 * answered again every time it is opened, from data the device already holds.
 * That is the whole difference from [LocalVideoPlaylistsRepository], which
 * embeds its videos and is therefore frozen the day it is saved - and the
 * reason this store cannot be a `LocalVideoPlaylist` with different fields.
 *
 * **The backing state is process-wide**, for the reason
 * [LocalVideoPlaylistsRepository]'s and [NotInterestedRepository]'s are: there
 * is no DI, so every ViewModel news up its own instance, and `HomeScreen` is
 * disposed and rebuilt by every real navigation. A list created on the Explore
 * tab therefore has to outlive the ViewModel that created it, and the one place
 * that reads it is not always the place that wrote it.
 *
 * **Device-wide rather than per-profile**, like local playlists and downloads:
 * a list is something the user assembled for themselves, and it names channels
 * and videos rather than account state, so it should survive switching to a
 * profile that has no account at all. The account-derived halves it reads are
 * profile-scoped at the source - [VideoHistoryRepository] for history,
 * `FEsubscriptions` for the feed - so two accounts on one device still see
 * their own videos inside the same list.
 */
class ExploreListsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    init {
        synchronized(LOCK) {
            if (sharedLists == null) {
                sharedLists = MutableStateFlow(loadLists())
                sharedSelected = MutableStateFlow(prefs.getString(KEY_SELECTED, null))
            }
        }
    }

    private val listsState: MutableStateFlow<List<ExploreList>> get() = sharedLists!!
    private val selectedState: MutableStateFlow<String?> get() = sharedSelected!!

    /** Every saved list, pinned first, then newest first. */
    val lists: StateFlow<List<ExploreList>> get() = listsState.asStateFlow()

    /**
     * The list the tab is showing, or null for the built-in rules.
     *
     * Null rather than an id that may not resolve: the selection is a
     * convenience, and a stored id whose list has since been deleted must fall
     * back to the default rules rather than open an empty screen.
     */
    val selectedListId: StateFlow<String?> get() = selectedState.asStateFlow()

    fun find(listId: String?): ExploreList? =
        listId?.let { id -> listsState.value.firstOrNull { it.id == id } }

    /**
     * @return the new list's id, or null when [name] was blank.
     *
     * A blank name is the only rejection: an Explore list can legitimately
     * match nothing for a while (no uploads today, no likes signed in), and
     * refusing to create it would make the rule impossible to set up in
     * advance.
     */
    suspend fun create(name: String, rules: ExploreRules = ExploreRules(), pinned: Boolean = false): String? {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return null
        val list = ExploreList(
            id = LIST_ID_PREFIX + UUID.randomUUID(),
            name = trimmed,
            rules = rules,
            pinned = pinned
        )
        persist(list)
        return list.id
    }

    /**
     * Create a list from a preset, and open it.
     *
     * Selected as well as created: the user tapped a starting point to get
     * something to look at, so a card that appears without becoming the
     * screen's own content reads as a tap that did nothing.
     */
    suspend fun createFromPreset(preset: ExplorePreset, name: String): String? =
        saveAsList(name, preset.rules)

    suspend fun rename(listId: String, name: String) {
        val list = find(listId) ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        persist(list.copy(name = trimmed))
    }

    /**
     * Replace a list's rules.
     *
     * The whole rule set is written rather than a field or two: the editor is
     * one screen that saves once, and a partial write would leave a list whose
     * stored rules are half of what the user just saw on it.
     */
    suspend fun updateRules(listId: String, rules: ExploreRules) {
        val list = find(listId) ?: return
        persist(list.copy(rules = rules))
    }

    suspend fun setPinned(listId: String, pinned: Boolean) {
        val list = find(listId) ?: return
        persist(list.copy(pinned = pinned))
    }

    /** Delete [listId], leaving the selection to fall back to the default rules. */
    suspend fun delete(listId: String) {
        if (listsState.value.none { it.id == listId }) return
        persistAll(listsState.value.filterNot { it.id == listId })
        if (selectedState.value == listId) select(null)
    }

    /** Move [listId] by [delta] places within the list order. */
    suspend fun move(listId: String, delta: Int) {
        val ordered = orderedForStorage(listsState.value)
        val from = ordered.indexOfFirst { it.id == listId }
        val to = from + delta
        // A degenerate range is the trap `coerceIn` walks into when low can pass
        // high, so the bounds are checked rather than clamped.
        if (from < 0 || to < 0 || to >= ordered.size) return
        val moved = ordered.toMutableList()
        val entry = moved.removeAt(from)
        moved.add(to, entry)
        // Straight to disk, not through persistAll: that re-sorts by pinned and
        // createdAt, which throws the order this function exists to change away
        // again - the move would save and then silently undo itself.
        persistVerbatim(moved)
    }

    /** Open [listId], or the built-in rules when it is null. */
    fun select(listId: String?) {
        val resolved = listId?.takeIf { id -> listsState.value.any { it.id == id } }
        if (resolved == selectedState.value) return
        selectedState.value = resolved
        prefs.edit().putString(KEY_SELECTED, resolved).apply()
    }

    /**
     * Store the current rules as a new list named [name], and select it.
     *
     * "Save these settings as a list" is the whole reason the editor exists,
     * so it is one call rather than three the screen has to remember the order
     * of.
     */
    suspend fun saveAsList(name: String, rules: ExploreRules): String? {
        val id = create(name, rules) ?: return null
        select(id)
        return id
    }

    /** Duplicate [listId] under a new name, rules and all. */
    suspend fun duplicate(listId: String, name: String): String? {
        val source = find(listId) ?: return null
        return create(name, rules = source.rules, pinned = source.pinned)
    }

    fun clearAll() {
        persistAll(emptyList())
        select(null)
    }

    // ---------------- Persistence ----------------

    private fun persist(list: ExploreList) {
        val current = listsState.value
        val next = if (current.any { it.id == list.id }) {
            current.map { if (it.id == list.id) list else it }
        } else {
            listOf(list) + current
        }
        persistAll(next)
    }

    /**
     * Write the whole set.
     *
     * The store holds rules rather than videos, so it is a few hundred bytes
     * per list and one file is simpler than the per-list files the embedded
     * video store needs for a different reason.
     */
    /** Writes [lists] in exactly the order given, without re-sorting. */
    private fun persistVerbatim(lists: List<ExploreList>) {
        val array = JSONArray().apply { lists.forEach { put(encode(it)) } }
        prefs.edit().putString(KEY_LISTS, array.toString()).apply()
        listsState.value = lists
    }

    private fun persistAll(lists: List<ExploreList>) {
        val ordered = orderedForStorage(lists)
        val array = JSONArray().apply { ordered.forEach { put(encode(it)) } }
        prefs.edit().putString(KEY_LISTS, array.toString()).apply()
        listsState.value = ordered
    }

    /** Pinned first, then newest first; stable for equal keys, as sortedBy is. */
    private fun orderedForStorage(lists: List<ExploreList>): List<ExploreList> =
        lists.sortedWith(
            compareByDescending<ExploreList> { it.pinned }.thenByDescending { it.createdAt }
        )

    private fun loadLists(): List<ExploreList> {
        val raw = prefs.getString(KEY_LISTS, null) ?: return emptyList()
        return try {
            val array = JSONArray(raw)
            (0 until array.length()).mapNotNull { index ->
                decode(array.optJSONObject(index))
            }.let(::orderedForStorage)
        } catch (e: Exception) {
            // A store that cannot be read is emptied, not crashed on: the tab
            // falls back to its built-in rules and the user can rebuild.
            KLog.e(TAG, "Failed to load explore lists", e)
            emptyList()
        }
    }

    /** A stored row, or null when it cannot name a list. */
    private fun decode(obj: JSONObject?): ExploreList? {
        if (obj == null) return null
        val id = obj.optString("id").takeIf { it.isNotBlank() } ?: return null
        val name = obj.optString("name").takeIf { it.isNotBlank() } ?: return null
        return ExploreList(
            id = id,
            name = name,
            rules = ExploreRules.fromJson(obj.optJSONObject("rules")),
            createdAt = obj.optLong("createdAt", 0L),
            pinned = obj.optBoolean("pinned", false)
        )
    }

    private fun encode(list: ExploreList): JSONObject = JSONObject().apply {
        put("id", list.id)
        put("name", list.name)
        put("rules", list.rules.toJson())
        put("createdAt", list.createdAt)
        put("pinned", list.pinned)
    }

    companion object {
        private const val TAG = "ExploreLists"

        /**
         * The preference file's name.
         *
         * Public because `BackupRepository`'s allowlist restates it by string,
         * and a rename that misses that list silently drops the store out of
         * every backup taken afterwards with nothing logged.
         */
        const val PREFS_NAME = "explore_lists"
        private const val KEY_LISTS = "lists"
        private const val KEY_SELECTED = "selected_list_id"

        /**
         * Marks an id as this store's, so a saved list can never be mistaken
         * for a YouTube playlist id by anything that branches on the shape of
         * one. Filesystem safe, and cannot collide with "PL...", "WL", "LL" or
         * the local video store's `localvp_`.
         */
        const val LIST_ID_PREFIX = "explore_"

        fun isExploreListId(id: String?): Boolean = id?.startsWith(LIST_ID_PREFIX) == true

        private val LOCK = Any()

        @Volatile
        private var sharedLists: MutableStateFlow<List<ExploreList>>? = null

        @Volatile
        private var sharedSelected: MutableStateFlow<String?>? = null
    }
}