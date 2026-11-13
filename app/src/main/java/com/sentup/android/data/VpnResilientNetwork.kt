package com.sentup.android.data

import android.net.NetworkCapabilities
import java.net.Inet4Address
import java.net.InetAddress
import java.net.ProxySelector
import okhttp3.Dns
import okhttp3.OkHttpClient

/**
 * Network helpers that keep Sentüp working reliably when a VPN is active.
 *
 * On Android, many VPN tunnels (WireGuard, OpenVPN, split-tunnel profiles) either:
 * 1. Omit [NetworkCapabilities.NET_CAPABILITY_VALIDATED] while still carrying live
 *    [NetworkCapabilities.NET_CAPABILITY_INTERNET] traffic, causing strict validation
 *    checks to falsely report "offline".
 * 2. Route only IPv4 cleanly while leaving IPv6 DNS results unreachable, causing
 *    dual-stack [OkHttpClient] calls to stall or fail before falling back.
 */
object VpnResilientNetwork {

    /**
     * Returns `true` when [capabilities] represents an internet-capable network,
     * treating active VPN transports ([NetworkCapabilities.TRANSPORT_VPN]) as usable
     * even when Android's captive-portal validator has not set
     * [NetworkCapabilities.NET_CAPABILITY_VALIDATED].
     */
    fun isNetworkUsable(capabilities: NetworkCapabilities?): Boolean {
        if (capabilities == null) return false
        if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)) return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    /**
     * Returns `true` when [capabilities] is unmetered (or Wi-Fi wrapped inside a VPN).
     */
    fun isUnmeteredOrWifiVpn(capabilities: NetworkCapabilities?): Boolean {
        if (capabilities == null) return false
        if (capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)) return true
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) &&
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_VPN)
    }

    /**
     * Orders [Inet4Address] entries ahead of IPv6 entries while preserving relative
     * order within each address family.
     */
    fun preferIpv4Addresses(addresses: List<InetAddress>): List<InetAddress> {
        if (addresses.size <= 1) return addresses
        val (ipv4, other) = addresses.partition { it is Inet4Address }
        return if (ipv4.isEmpty()) addresses else ipv4 + other
    }

    /**
     * [Dns] resolver that prioritizes IPv4 addresses ahead of IPv6 so VPN tunnels
     * without IPv6 routing connect immediately without stalling.
     */
    val ipv4PreferredDns: Dns = object : Dns {
        override fun lookup(hostname: String): List<InetAddress> =
            preferIpv4Addresses(Dns.SYSTEM.lookup(hostname))
    }

    /**
     * Applies VPN-friendly DNS ordering, system proxy resolution, and automatic
     * connection-failure retries to [builder].
     */
    fun configureForVpn(builder: OkHttpClient.Builder): OkHttpClient.Builder = builder
        .dns(ipv4PreferredDns)
        .proxySelector(ProxySelector.getDefault())
        .retryOnConnectionFailure(true)
}
