package com.sentup.android.data

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The profile-scoped section is the half of a backup no compiler checks: the
 * store names are restated here and in [BackupRepository], and a field left out
 * of the round trip is dropped silently by every backup taken afterwards.
 */
class BackupProfileDataTest {

    private fun manifest() = BackupManifest(
        formatVersion = BackupTransfer.FORMAT_VERSION,
        appVersionName = "test",
        appVersionCode = 1,
        createdAt = 0L,
        device = null,
    )

    private fun roundTrip(snapshot: BackupSnapshot): BackupSnapshot? {
        val out = ByteArrayOutputStream()
        BackupTransfer.write(snapshot, out)
        return BackupTransfer.read(ByteArrayInputStream(out.toByteArray()))
    }

    @Test
    fun `every profile-scoped store survives a round trip`() {
        val data = BackupProfileData(
            subscriptions = """[{"id":"UC1"}]""",
            subscriptionGroups = """[{"name":"Music"}]""",
            hiddenVideos = """[{"videoId":"abc"}]""",
            blockedChannels = """[{"channelId":"UC2"}]""",
            watchHistory = """[{"videoId":"def"}]""",
            removedFromHistory = setOf("ghi"),
            resumePositions = """[{"videoId":"def","positionMs":120000,"durationMs":600000}]""",
            playHistory = """[{"songId":"s1","title":"T","artist":"A","album":"B","timestamp":1,"duration":2}]""",
            likedSongIds = setOf("s1", "s2"),
            likedSongs = """[{"id":"s1","title":"T"}]""",
            searchHistory = "first|second",
            uploadMutes = setOf("UC3"),
        )

        val restored = roundTrip(
            BackupSnapshot(manifest = manifest(), profileData = mapOf("p1" to data))
        )

        assertNotNull(restored)
        assertEquals(data, restored!!.profileData["p1"])
    }

    @Test
    fun `each per-profile history store alone makes a profile worth carrying`() {
        listOf(
            BackupProfileData(playHistory = "[]"),
            BackupProfileData(likedSongIds = setOf("s1")),
            BackupProfileData(likedSongs = "[]"),
            BackupProfileData(searchHistory = "q"),
            BackupProfileData(uploadMutes = setOf("UC1")),
        ).forEach { assertEquals(false, it.isEmpty) }
    }

    @Test
    fun `a version-1 backup still reads, and one from a newer format is refused`() {
        val old = roundTrip(
            BackupSnapshot(manifest = manifest().copy(formatVersion = 1))
        )
        assertNotNull(old)
        assertEquals(1, old!!.manifest.formatVersion)
        assertTrue(old.manifest.formatVersion < BackupTransfer.FORMAT_PROFILE_HISTORY)

        val newer = runCatching {
            roundTrip(BackupSnapshot(manifest = manifest().copy(formatVersion = BackupTransfer.FORMAT_VERSION + 1)))
        }
        assertTrue(newer.exceptionOrNull() is UnsupportedBackupException)
    }

    @Test
    fun `a backup written before resume positions existed reads back without them`() {
        val legacy = BackupProfileData(
            watchHistory = """[{"videoId":"def"}]""",
            removedFromHistory = setOf("ghi"),
        )

        val restored = roundTrip(
            BackupSnapshot(manifest = manifest(), profileData = mapOf("p1" to legacy))
        )

        assertNotNull(restored)
        assertNull(restored!!.profileData["p1"]?.resumePositions)
        assertEquals(legacy, restored.profileData["p1"])
    }

    @Test
    fun `resume positions alone are enough to make a profile worth carrying`() {
        val onlyResume = BackupProfileData(resumePositions = """[{"videoId":"def"}]""")
        assertEquals(false, onlyResume.isEmpty)
        assertEquals(true, BackupProfileData().isEmpty)
    }

    @Test
    fun `the channel a profile was left on survives a round trip`() {
        val snapshot = BackupSnapshot(
            manifest = manifest(),
            profiles = listOf(
                // On a brand channel, with the id YouTube hands out for it.
                BackupProfile(
                    id = "p1",
                    kind = ProfileKind.YOUTUBE.name,
                    name = "Marka",
                    datasyncId = "109999999999999999999||103900000000000001234",
                    pageId = "103900000000000001234",
                    channelPinned = true,
                    addedAt = 5L,
                ),
                // Pinned to the account's own channel: no page id, but a choice
                // all the same. The two fields only mean anything together, so
                // this is the case a writer that stores the id alone gets wrong.
                BackupProfile(
                    id = "p2",
                    kind = ProfileKind.YOUTUBE.name,
                    name = "Kişisel",
                    datasyncId = "103900000000000001234||",
                    pageId = null,
                    channelPinned = true,
                    addedAt = 6L,
                ),
                // Never pointed anywhere: follows whatever YouTube reports.
                BackupProfile(id = "p3", kind = ProfileKind.LOCAL.name, name = "Device"),
            ),
        )

        val restored = roundTrip(snapshot)

        assertNotNull(restored)
        val brand = restored!!.profiles.single { it.id == "p1" }
        assertEquals("103900000000000001234", brand.pageId)
        assertTrue(brand.channelPinned)

        val own = restored.profiles.single { it.id == "p2" }
        assertNull(own.pageId)
        assertTrue(own.channelPinned)

        val untouched = restored.profiles.single { it.id == "p3" }
        assertNull(untouched.pageId)
        assertFalse(untouched.channelPinned)
    }
}
