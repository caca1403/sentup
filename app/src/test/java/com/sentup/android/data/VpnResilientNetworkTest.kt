package com.sentup.android.data

import java.net.Inet4Address
import java.net.InetAddress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VpnResilientNetworkTest {
    @Test
    fun `preferIpv4Addresses puts IPv4 before IPv6 while preserving relative order`() {
        val v6a = InetAddress.getByAddress("example.com", ByteArray(16) { if (it == 15) 1 else 0 })
        val v4a = InetAddress.getByAddress("example.com", byteArrayOf(93, 184.toByte(), 216.toByte(), 34))
        val v6b = InetAddress.getByAddress("example.com", ByteArray(16) { if (it == 15) 2 else 0 })
        val v4b = InetAddress.getByAddress("example.com", byteArrayOf(1, 1, 1, 1))

        val ordered = VpnResilientNetwork.preferIpv4Addresses(listOf(v6a, v4a, v6b, v4b))

        assertEquals(listOf(v4a, v4b, v6a, v6b), ordered)
        assertTrue(ordered[0] is Inet4Address)
        assertTrue(ordered[1] is Inet4Address)
    }

    @Test
    fun `preferIpv4Addresses keeps IPv6-only lists intact`() {
        val v6 = InetAddress.getByAddress("example.com", ByteArray(16) { if (it == 15) 1 else 0 })
        assertEquals(listOf(v6), VpnResilientNetwork.preferIpv4Addresses(listOf(v6)))
    }
}
