package com.example.network

enum class PingProtocol(val displayName: String, val defaultPort: Int) {
    ICMP("ICMP (Standard)", 0),
    TCP("TCP Socket", 80),
    HTTP("HTTP/HTTPS", 443)
}

data class PingConfig(
    val host: String = "8.8.8.8",
    val protocol: PingProtocol = PingProtocol.ICMP,
    val port: Int = 80,
    val count: Int = 10, // 0 for continuous
    val intervalMs: Long = 1000L,
    val timeoutMs: Int = 3000,
    val packetSize: Int = 64, // bytes
    val ttl: Int = 64
)

enum class PacketStatus {
    SUCCESS,
    TIMEOUT,
    ERROR
}

data class PingPacket(
    val seq: Int,
    val timeMs: Float,
    val ttl: Int = 64,
    val bytes: Int = 64,
    val status: PacketStatus = PacketStatus.SUCCESS,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

data class PingSessionState(
    val isRunning: Boolean = false,
    val targetHost: String = "",
    val resolvedIp: String = "",
    val protocol: PingProtocol = PingProtocol.ICMP,
    val packets: List<PingPacket> = emptyList(),
    val packetsSent: Int = 0,
    val packetsReceived: Int = 0,
    val packetLossPercent: Float = 0f,
    val minLatencyMs: Float = 0f,
    val avgLatencyMs: Float = 0f,
    val maxLatencyMs: Float = 0f,
    val jitterMs: Float = 0f,
    val currentLatencyMs: Float = 0f,
    val logs: List<String> = emptyList()
)

data class NetworkStatus(
    val isConnected: Boolean = false,
    val connectionType: String = "Disconnected", // Wi-Fi, Cellular, VPN, Ethernet
    val linkSpeedMbps: Int = 0,
    val wifiRssiDbm: Int = 0,
    val wifiSignalPercent: Int = 0,
    val wifiFrequencyGhz: Double = 0.0,
    val wifiSsid: String = "",
    val localIpv4: String = "0.0.0.0",
    val localIpv6: String = "",
    val gatewayIp: String = "0.0.0.0",
    val subnetMask: String = "255.255.255.0",
    val dnsServers: List<String> = emptyList(),
    val publicIp: String = "Loading...",
    val publicIsp: String = "",
    val publicCountry: String = "",
    val publicCity: String = "",
    val isVpnActive: Boolean = false
)

data class DnsBenchmarkResult(
    val id: String,
    val providerName: String,
    val primaryIp: String,
    val description: String,
    val latencyMs: Float = -1f,
    val isTesting: Boolean = false,
    val isSuccess: Boolean = false,
    val packetLoss: Float = 0f,
    val error: String? = null
)

data class LanDevice(
    val ip: String,
    val hostname: String = "",
    val latencyMs: Float = 0f,
    val isAlive: Boolean = true,
    val isGateway: Boolean = false,
    val isCurrentDevice: Boolean = false
)

data class PortScanResult(
    val port: Int,
    val serviceName: String,
    val isOpen: Boolean,
    val latencyMs: Float = 0f
)

data class TracerouteHop(
    val hopNumber: Int,
    val ip: String,
    val hostname: String = "",
    val latencyMs: Float = 0f,
    val status: PacketStatus = PacketStatus.SUCCESS
)

data class DnsRecordItem(
    val type: String,
    val value: String,
    val ttl: Int = 300
)

data class DnsLookupResult(
    val host: String,
    val queryTimeMs: Float,
    val records: List<DnsRecordItem> = emptyList(),
    val errorMessage: String? = null
)
