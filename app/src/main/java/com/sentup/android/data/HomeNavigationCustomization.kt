package com.sentup.android.data

/** The tab Sentüp opens on. The [id]s are stored and frozen. */
enum class StartTab(val id: String) {
    /** Wherever the user was last, which is how Sentüp has always opened. */
    LAST("last"),
    HOME("home"),
    SEARCH("search"),
    LIBRARY("library");

    companion object {
        fun fromId(id: String?): StartTab = entries.firstOrNull { it.id == id } ?: LAST
    }
}

/** The mode Sentüp opens in. The [id]s are stored and frozen. */
enum class StartMode(val id: String) {
    LAST("last"),
    MUSIC("music"),
    VIDEO("video");

    companion object {
        fun fromId(id: String?): StartMode = entries.firstOrNull { it.id == id } ?: LAST
    }
}

/**
 * When the navigation bar names its tabs. [AUTO] is each bar's own habit: the
 * floating bar names only the open tab and the standard bar names them all.
 * The [id]s are stored and frozen.
 */
enum class NavTabLabels(val id: String) {
    AUTO("auto"),
    ALWAYS("always"),
    SELECTED("selected"),
    NEVER("never");

    companion object {
        fun fromId(id: String?): NavTabLabels = entries.firstOrNull { it.id == id } ?: AUTO
    }
}

/**
 * How Sentüp opens and how its navigation bar behaves, as one snapshot. The
 * defaults are the app as it shipped before any of this was a setting.
 */
data class HomeNavigationCustomization(
    val startTab: StartTab = StartTab.LAST,
    val startMode: StartMode = StartMode.LAST,
    val tabLabels: NavTabLabels = NavTabLabels.AUTO,
    /**
     * Whether the floating navigation bar slides away as a page scrolls.
     * On by default (user decision, October 2026: "alt menü tamamen yok
     * olmalı"): the ghosted outline people saw while it slid was a wrapper
     * bug, fixed where the shadow is painted, not a reason to stop hiding.
     */
    val barHidesOnScroll: Boolean = true,
    /** The greeting beside the profile picture on the Home top bar. */
    val showGreeting: Boolean = true,
    /**
     * Keep the downloads button on the Home top bar. Off, it appears only
     * while something is downloading.
     */
    val alwaysShowDownloads: Boolean = false,
    // --- Home feed appearance. Added as one group so the shelves can be
    // turned off without a rebuild; every default is the feed exactly as it
    // already shipped, so nobody's Home changes when this setting appears.
    /** The hero card at the top of the Home feed. */
    val homeHeroCard: Boolean = true,
    /** The "resume where you left off" row inside the hero. */
    val homeResumeShelf: Boolean = true,
    /** The daily and auto mixes shelf. */
    val homeMixShelf: Boolean = true,
    /** The top artists shelf. */
    val homeArtistShelf: Boolean = true,
    /** The one-line weekly listening strip. */
    val homeListeningStrip: Boolean = true,
    /** Artist tiles large (fewer across) or compact (more across). */
    val homeArtistTilesLarge: Boolean = true,
)
