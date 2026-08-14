package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.TimeUnit
import kotlin.math.max

object NetworkHelper {
    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build()
    }

    suspend fun getNetworkStatus(context: Context): NetworkStatus = withContext(Dispatchers.IO) {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager

        val activeNetwork = cm?.activeNetwork
        val caps = cm?.getNetworkCapabilities(activeNetwork)
        val linkProperties = cm?.getLinkProperties(activeNetwork)

        val isConnected = caps != null && caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        val isVpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) ?: false
        val isWifi = caps?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ?: false
        val isCellular = caps?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ?: false
        val isEthernet = caps?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ?: false

        val connType = when {
            isVpn -> "VPN / Secure Tunnel"
            isWifi -> "Wi-Fi Network"
            isCellular -> "Cellular Mobile (LTE/5G)"
            isEthernet -> "Ethernet LAN"
            isConnected -> "Connected"
            else -> "Disconnected"
        }

        var linkSpeed = 0
        var rssiDbm = 0
        var signalPercent = 0
        var freqGhz = 0.0
        var ssid = ""

        if (isWifi && wm != null) {
            try {
                val wifiInfo = wm.connectionInfo
                if (wifiInfo != null) {
                    linkSpeed = wifiInfo.linkSpeed
                    rssiDbm = wifiInfo.rssi
                    signalPercent = WifiManager.calculateSignalLevel(rssiDbm, 100)
                    freqGhz = if (wifiInfo.frequency > 0) wifiInfo.frequency / 1000.0 else 0.0
                    val rawSsid = wifiInfo.ssid ?: ""
                    ssid = if (rawSsid.startsWith("\"") && rawSsid.endsWith("\"")) {
                        rawSsid.substring(1, rawSsid.length - 1)
                    } else rawSsid
                    if (ssid == "<unknown ssid>") ssid = "Wi-Fi (Active)"
                }
            } catch (_: Exception) { }
        }

        var localIpv4 = "127.0.0.1"
        var localIpv6 = ""
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
                val iface = interfaces.nextElement()
                if (iface.isLoopback || !iface.isUp) continue
                val addresses = iface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val addr = addresses.nextElement()
                    if (!addr.isLoopbackAddress) {
                        if (addr is Inet4Address && (localIpv4 == "127.0.0.1" || localIpv4 == "0.0.0.0")) {
                            localIpv4 = addr.hostAddress ?: ""
                        } else if (addr is Inet6Address && localIpv6.isEmpty()) {
                            localIpv6 = addr.hostAddress?.substringBefore("%") ?: ""
                        }
                    }
                }
            }
        } catch (_: Exception) { }

        var gatewayIp = "192.168.1.1"
        var dnsList = mutableListOf<String>()

        linkProperties?.let { lp ->
            lp.routes.forEach { route ->
                val gw = route.gateway
                if (gw != null && gw is Inet4Address && !gw.isAnyLocalAddress) {
                    gatewayIp = gw.hostAddress ?: gatewayIp
                }
            }
            lp.dnsServers.forEach { dns ->
                dns.hostAddress?.let { dnsList.add(it) }
            }
        }

        if (dnsList.isEmpty()) {
            dnsList = mutableListOf("8.8.8.8", "1.1.1.1")
        }

        // Subnet mask derivation from local IP
        val subnetMask = calculateSubnetMask(localIpv4)

        NetworkStatus(
            isConnected = isConnected,
            connectionType = connType,
            linkSpeedMbps = max(linkSpeed, caps?.linkDownstreamBandwidthKbps?.let { it / 1000 } ?: 0),
            wifiRssiDbm = rssiDbm,
            wifiSignalPercent = signalPercent,
            wifiFrequencyGhz = freqGhz,
            wifiSsid = ssid,
            localIpv4 = localIpv4,
            localIpv6 = localIpv6,
            gatewayIp = gatewayIp,
            subnetMask = subnetMask,
            dnsServers = dnsList,
            isVpnActive = isVpn
        )
    }

    suspend fun fetchPublicIpInfo(): PublicIpInfo = withContext(Dispatchers.IO) {
        try {
            val req = Request.Builder()
                .url("http://ip-api.com/json/?fields=query,status,country,city,isp,org,as")
                .build()
            client.newCall(req).execute().use { res ->
                if (res.isSuccessful) {
                    val body = res.body?.string() ?: ""
                    val json = JSONObject(body)
                    if (json.optString("status") == "success") {
                        return@withContext PublicIpInfo(
                            ip = json.optString("query"),
                            isp = json.optString("isp", json.optString("org")),
                            country = json.optString("country"),
                            city = json.optString("city"),
                            asn = json.optString("as")
                        )
                    }
                }
            }
        } catch (_: Exception) { }

        // Fallback to ipify if ip-api is unreachable
        try {
            val fallbackReq = Request.Builder()
                .url("https://api.ipify.org?format=json")
                .build()
            client.newCall(fallbackReq).execute().use { res ->
                if (res.isSuccessful) {
                    val json = JSONObject(res.body?.string() ?: "")
                    return@withContext PublicIpInfo(
                        ip = json.optString("ip"),
                        isp = "Public Network",
                        country = "Global",
                        city = "",
                        asn = ""
                    )
                }
            }
        } catch (_: Exception) { }

        PublicIpInfo(ip = "Unavailable", isp = "Offline / Unknown", country = "-", city = "-", asn = "-")
    }

    private fun calculateSubnetMask(ip: String): String {
        return try {
            val parts = ip.split(".").map { it.toInt() }
            when (parts[0]) {
                in 1..126 -> "255.0.0.0 (/8)"
                in 128..191 -> "255.255.0.0 (/16)"
                in 192..223 -> "255.255.255.0 (/24)"
                else -> "255.255.255.0 (/24)"
            }
        } catch (_: Exception) {
            "255.255.255.0 (/24)"
        }
    }
}

data class PublicIpInfo(
    val ip: String,
    val isp: String,
    val country: String,
    val city: String,
    val asn: String
)
