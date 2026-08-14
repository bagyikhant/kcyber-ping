package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ping_history")
data class PingHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val targetHost: String,
    val resolvedIp: String,
    val protocol: String, // ICMP, TCP, HTTP
    val port: Int = 0,
    val packetsSent: Int,
    val packetsReceived: Int,
    val packetLossPercent: Float,
    val minLatencyMs: Float,
    val avgLatencyMs: Float,
    val maxLatencyMs: Float,
    val jitterMs: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "saved_hosts")
data class SavedHostEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val host: String,
    val label: String,
    val category: String, // DNS, Gaming, Cloud, Local, Custom
    val port: Int = 80,
    val protocol: String = "ICMP",
    val isFavorite: Boolean = false,
    val lastPingMs: Float? = null,
    val lastTested: Long? = null
)
