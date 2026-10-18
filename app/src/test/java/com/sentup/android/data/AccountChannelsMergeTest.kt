package com.sentup.android.data

import com.sentup.android.data.youtube.channelItemKeys
import com.sentup.android.data.youtube.channelViewIds
import com.sentup.android.data.youtube.mergeChannelPages
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The pure half of the personal + brand channel merge: which channels are
 * walked and what a merged page looks like. (The fetching half rides on
 * coroutines and a live session.)
 */
class AccountChannelsMergeTest {

    @Test fun `on personal, walk personal then brands`() {
        assertEquals(
            listOf<String?>(null, "111", "222"),
            channelViewIds(null, listOf("111", "222")),
        )
    }

    @Test fun `on a brand, walk it first then personal then the rest`() {
        assertEquals(
            listOf<String?>("222", null, "111"),
            channelViewIds("222", listOf("111", "222")),
        )
    }

    @Test fun `no brands means one view and nothing else`() {
        assertEquals(listOf<String?>(null), channelViewIds(null, emptyList()))
        assertEquals(listOf<String?>("222", null), channelViewIds("222", listOf("222")))
    }

    @Test fun `same playlist on both channels is kept once`() {
        val personal = listOf("PL-a" to "Road Trip", "PL-b" to "Gym")
        val brand = listOf("PL-a" to "Road Trip", "PL-c" to "Studio")
        val merged = mergeChannelPages(listOf(personal, brand)) { (id, _) ->
            channelItemKeys(id, null, null)
        }
        assertEquals(listOf("PL-a" to "Road Trip", "PL-b" to "Gym", "PL-c" to "Studio"), merged)
    }

    @Test fun `same name and count under different ids is kept once`() {
        val personal = listOf("PL-a" to "Road Trip|12")
        val brand = listOf("PL-x" to "road  trip|12", "PL-y" to "Other|3")
        val merged = mergeChannelPages(listOf(personal, brand)) { (id, name) ->
            val (title, count) = name.split("|")
            channelItemKeys(id, title, count)
        }
        assertEquals(listOf("PL-a" to "Road Trip|12", "PL-y" to "Other|3"), merged)
    }

    @Test fun `same name without a count is not deduped`() {
        val personal = listOf("PL-a" to "Mix")
        val brand = listOf("PL-x" to "Mix")
        val merged = mergeChannelPages(listOf(personal, brand)) { (id, _) ->
            channelItemKeys(id, "Mix", null)
        }
        assertEquals(2, merged.size)
    }

    @Test fun `items without keys are all kept`() {
        val merged = mergeChannelPages(listOf(listOf("x"), listOf("x"))) { emptyList() }
        assertEquals(listOf("x", "x"), merged)
    }

    @Test fun `a single channel page passes through`() {
        val page = listOf("a", "b")
        assertEquals(page, mergeChannelPages(listOf(page)) { listOf(it) })
        assertEquals(emptyList<String>(), mergeChannelPages<String>(emptyList()) { listOf(it) })
    }
}
