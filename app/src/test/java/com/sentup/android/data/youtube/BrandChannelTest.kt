package com.sentup.android.data.youtube

import com.sentup.android.data.brandChannelIdOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Brand channels: which channel a session is on, and the list the account
 * switcher reads.
 *
 * The `DATASYNC_ID` cases are the contract yt-dlp's `_parse_data_sync_id`
 * defines and this app now relies on. The list cases are written against the
 * shapes [parseChannels] accepts, including both ways a page id can arrive,
 * because which of them YouTube actually sends has never been observed from
 * this machine - that response needs a live login. Either shape therefore has
 * to work, and these tests are what "either" means.
 */
class BrandChannelTest {

    // --- Which channel a session is on ---

    @Test
    fun aBrandChannelIsTheFirstHalfOfTheDataSyncId() {
        assertEquals(
            "103900000000000001234",
            brandChannelIdOf("103900000000000001234||109999999999999999999"),
        )
    }

    @Test
    fun theAccountsOwnChannelHasNoBrandId() {
        // "<user session id>||" is the account's own channel: there is an id
        // there, but it is not a page id, and sending it as one would name a
        // channel that does not exist.
        assertNull(brandChannelIdOf("109999999999999999999||"))
    }

    @Test
    fun anythingElseSaysNothingAboutChannels() {
        assertNull(brandChannelIdOf(null))
        assertNull(brandChannelIdOf(""))
        assertNull(brandChannelIdOf("   "))
        assertNull(brandChannelIdOf("000-abcdef"))
        assertNull(brandChannelIdOf("||109999999999999999999"))
    }

    // --- The channel list ---

    @Test
    fun anAccountItemWithAPageIdFieldIsRead() {
        val channels = parseChannels(
            """
            {"actions":[
              {"accountItem":{
                "accountName":{"simpleText":"Kanal"},
                "accountPhoto":{"thumbnails":[{"url":"https://yt3.ggpht.com/a=s88"}]},
                "isSelected":true,
                "hasChannel":true,
                "pageId":"103900000000000001234",
                "accountByline":{"simpleText":"Marka hesabı"},
                "channelHandle":{"simpleText":"@kanal"}
              }}
            ]}
            """.trimIndent()
        )

        assertEquals(1, channels.size)
        assertEquals("Kanal", channels[0].name)
        assertEquals("103900000000000001234", channels[0].pageId)
        assertEquals("@kanal", channels[0].handle)
        assertEquals("Marka hesabı", channels[0].byline)
        assertTrue(channels[0].isCurrent)
        // =s88 is the size the menu asks for; the suffix is a size request, so
        // the app asks for one worth drawing at avatar size.
        assertEquals("https://yt3.ggpht.com/a=s512", channels[0].avatarUrl)
    }

    @Test
    fun aPageIdCarriedInASwitchUrlIsFound() {
        val channels = parseChannels(
            """
            {"actions":[
              {"accountItem":{
                "accountName":{"runs":[{"text":"Brand "},{"text":"Kanalı"}]},
                "isSelected":false,
                "serviceEndpoint":{"commandMetadata":{"webCommandMetadata":{
                  "url":"/switch_account?pageId=103900000000000001234&next=%2F"
                }}}
              }}
            ]}
            """.trimIndent()
        )

        assertEquals(1, channels.size)
        assertEquals("Brand Kanalı", channels[0].name)
        assertEquals("103900000000000001234", channels[0].pageId)
    }

    @Test
    fun anItemWithoutAnyIdIsStillAChannel() {
        // The account's own channel has no page id, and that is an answer
        // rather than a parse failure.
        val channels = parseChannels(
            """{"actions":[{"accountItem":{"accountName":{"simpleText":"Kişisel"},"isSelected":true}}]}"""
        )

        assertEquals(1, channels.size)
        assertNull(channels[0].pageId)
    }

    @Test
    fun anItemWithNoNameIsDropped() {
        val channels = parseChannels(
            """{"actions":[{"accountItem":{"isSelected":true,"pageId":"103900000000000001234"}}]}"""
        )
        assertTrue(channels.isEmpty())
    }

    @Test
    fun theSwitcherPageFormIsRead() {
        // October 2026 accounts_list of a signed-in account: the rows moved to
        // channelSwitcherPageRenderer.contents as accountItemRenderer, fields
        // unchanged. If the name-only fallback were to match "accountItem" as a
        // prefix instead of a whole key this would silently parse nothing.
        val channels = parseChannels(
            """
            {"actions":[
              {"updateChannelSwitcherPageAction":{"page":{"channelSwitcherPageRenderer":{
                "contents":[
                  {"accountItemRenderer":{
                    "accountName":{"simpleText":"Kişisel"},
                    "isSelected":true,
                    "hasChannel":true
                  }},
                  {"accountItemRenderer":{
                    "accountName":{"simpleText":"Marka Kanalı"},
                    "accountByline":{"simpleText":"Marka hesabı"},
                    "isSelected":false,
                    "hasChannel":true,
                    "selectActiveIdentityEndpoint":{"supportedTokens":[
                      {"accountSwitcherCapabilityValue":{"pageId":"103900000000000001234"}}
                    ]}
                  }}
                ]
              }}}}
            ]}
            """.trimIndent()
        )

        assertEquals(2, channels.size)
        assertEquals("Kişisel", channels[0].name)
        assertNull(channels[0].pageId)
        assertTrue(channels[0].isCurrent)
        assertEquals("Marka Kanalı", channels[1].name)
        assertEquals("103900000000000001234", channels[1].pageId)
        assertEquals("Marka hesabı", channels[1].byline)
    }

    @Test
    fun theMenuLinkFormIsReadAsAFallback() {
        val channels = parseChannels(
            """
            {
              "actions": [
                {
                  "openPopupAction": {
                    "popup": {
                      "multiPageMenuRenderer": {
                        "sections": [
                          {
                            "multiPageMenuSectionRenderer": {
                              "items": [
                                {
                                  "compactLinkRenderer": {
                                    "title": {
                                      "simpleText": "Kanal"
                                    },
                                    "icon": {
                                      "thumbnails": [
                                        {
                                          "url": "//yt3.ggpht.com/b=s96"
                                        }
                                      ]
                                    },
                                    "navigationEndpoint": {
                                      "commandMetadata": {
                                        "webCommandMetadata": {
                                          "url": "/channel/UC4QobU6STFB0P71PMvOGN5A"
                                        }
                                      },
                                      "browseEndpoint": {
                                        "browseId": "UC4QobU6STFB0P71PMvOGN5A"
                                      }
                                    }
                                  }
                                },
                                {
                                  "compactLinkRenderer": {
                                    "title": {
                                      "simpleText": "Sign out"
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(1, channels.size)
        assertEquals("Kanal", channels[0].name)
        assertEquals("https://yt3.ggpht.com/b=s512", channels[0].avatarUrl)
    }

    @Test
    fun anAccountItemWinsOverTheMenuLinks() {
        // Both shapes in one response: the items are the list of channels, the
        // menu links are the rest of the menu.
        val channels = parseChannels(
            """
            {
              "actions": [
                {
                  "accountItem": {
                    "accountName": {
                      "simpleText": "Kanal"
                    },
                    "isSelected": true
                  }
                },
                {
                  "openPopupAction": {
                    "popup": {
                      "multiPageMenuRenderer": {
                        "sections": [
                          {
                            "multiPageMenuSectionRenderer": {
                              "items": [
                                {
                                  "compactLinkRenderer": {
                                    "title": {
                                      "simpleText": "Ayarlar"
                                    },
                                    "navigationEndpoint": {
                                      "browseEndpoint": {
                                        "browseId": "UC4QobU6STFB0P71PMvOGN5A"
                                      }
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(listOf("Kanal"), channels.map { it.name })
    }

    @Test
    fun garbageIsAnEmptyListRatherThanACrash() {
        assertTrue(parseChannels("").isEmpty())
        assertTrue(parseChannels("<html>nope</html>").isEmpty())
        assertTrue(parseChannels("{}").isEmpty())
    }
}
