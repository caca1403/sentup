package com.sentup.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomFolderTreeTest {

    @Test
    fun `childFoldersOf returns pinned folders first then alphabetical`() {
        val folders = listOf(
            MusicFolder(id = "1", name = "Zeta"),
            MusicFolder(id = "2", name = "Alpha"),
            MusicFolder(id = "3", name = "Pinned", isPinned = true),
            MusicFolder(id = "4", name = "Child", parentId = "1")
        )

        val root = CustomFolderTree.childFoldersOf(folders, null)
        assertEquals(listOf("3", "2", "1"), root.map { it.id })
    }

    @Test
    fun `breadcrumbPath traces ancestors from root to target`() {
        val folders = listOf(
            MusicFolder(id = "r", name = "Rock"),
            MusicFolder(id = "c", name = "Classic", parentId = "r"),
            MusicFolder(id = "f", name = "Favorites", parentId = "c")
        )

        val path = CustomFolderTree.breadcrumbPath(folders, "f")
        assertEquals(listOf("Rock", "Classic", "Favorites"), path.map { it.name })
    }

    @Test
    fun `allDescendantSongIds collects songs across nested subfolders without duplicates`() {
        val folders = listOf(
            MusicFolder(id = "r", name = "Root", songIds = listOf("s1", "s2")),
            MusicFolder(id = "sub1", name = "Sub 1", parentId = "r", songIds = listOf("s2", "s3")),
            MusicFolder(id = "sub2", name = "Sub 2", parentId = "sub1", songIds = listOf("s4"))
        )

        val songs = CustomFolderTree.allDescendantSongIds(folders, "r")
        assertEquals(listOf("s1", "s2", "s3", "s4"), songs)
    }

    @Test
    fun `canMoveFolder prevents cycles into self or descendants`() {
        val folders = listOf(
            MusicFolder(id = "a", name = "A"),
            MusicFolder(id = "b", name = "B", parentId = "a"),
            MusicFolder(id = "c", name = "C")
        )

        assertFalse(CustomFolderTree.canMoveFolder(folders, "a", "a"))
        assertFalse(CustomFolderTree.canMoveFolder(folders, "a", "b"))
        assertTrue(CustomFolderTree.canMoveFolder(folders, "a", "c"))
        assertTrue(CustomFolderTree.canMoveFolder(folders, "b", null))
    }

    @Test
    fun `resolveFolderSongs combines artists albums and direct songs without duplicates`() {
        val allSongs = listOf(
            Song(id = "1", title = "Bohemian Rhapsody", artist = "Queen", album = "A Night at the Opera", duration = 354_000L),
            Song(id = "2", title = "Time", artist = "Pink Floyd", album = "Dark Side", duration = 413_000L),
            Song(id = "3", title = "Starman", artist = "David Bowie", album = "Ziggy Stardust", duration = 250_000L)
        )
        val rockFolder = MusicFolder(
            id = "rock",
            name = "Rock",
            artistNames = listOf("Queen"),
            albumNames = listOf("Dark Side"),
            songIds = listOf("1", "3")
        )

        val resolved = CustomFolderTree.resolveFolderSongs(rockFolder, allSongs)
        assertEquals(listOf("1", "2", "3"), resolved.map { it.id })
    }

    @Test
    fun `resolveFolderSongs evaluates autoRule keyword minPlays and downloadedOnly`() {
        val allSongs = listOf(
            Song(id = "1", title = "Nightcall", artist = "Kavinsky", album = "OutRun", duration = 250_000L),
            Song(id = "2", title = "Morning City", artist = "M83", album = "Hurry Up", duration = 240_000L),
            Song(id = "3", title = "Night Ride", artist = "Synth", album = "Drive", duration = 200_000L)
        )
        val smartFolder = MusicFolder(
            id = "night",
            name = "Night",
            autoRule = FolderAutoRule(keyword = "night", minPlays = 3, downloadedOnly = true)
        )

        val resolved = CustomFolderTree.resolveFolderSongs(
            folder = smartFolder,
            allSongs = allSongs,
            playCounts = mapOf("1" to 5, "2" to 10, "3" to 1),
            downloadedIds = setOf("1", "2")
        )
        assertEquals(listOf("1"), resolved.map { it.id })
    }
}
