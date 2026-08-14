package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.example.MainActivity
import com.example.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class PingForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "ping_kcyber_channel"
        const val CHANNEL_NAME = "Ping KCyber Live Monitor"
        const val NOTIFICATION_ID = 1001

        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_UPDATE = "com.example.service.ACTION_UPDATE"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"

        const val EXTRA_HOST = "extra_host"
        const val EXTRA_PROTOCOL = "extra_protocol"
        const val EXTRA_LATENCY = "extra_latency"
        const val EXTRA_AVG = "extra_avg"
        const val EXTRA_MIN = "extra_min"
        const val EXTRA_MAX = "extra_max"
        const val EXTRA_SENT = "extra_sent"
        const val EXTRA_RECV = "extra_recv"
        const val EXTRA_LOSS = "extra_loss"
        const val EXTRA_JITTER = "extra_jitter"

        private val _stopEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val stopEvents = _stopEvents.asSharedFlow()

        var isServiceRunning = false
            private set

        fun startService(context: Context, host: String, protocol: String) {
            val intent = Intent(context, PingForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_HOST, host)
                putExtra(EXTRA_PROTOCOL, protocol)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun updateProgress(
            context: Context,
            host: String,
            protocol: String,
            latencyMs: Float,
            avgMs: Float,
            minMs: Float,
            maxMs: Float,
            sent: Int,
            recv: Int,
            loss: Float,
            jitter: Float
        ) {
            if (!isServiceRunning) return
            val intent = Intent(context, PingForegroundService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_HOST, host)
                putExtra(EXTRA_PROTOCOL, protocol)
                putExtra(EXTRA_LATENCY, latencyMs)
                putExtra(EXTRA_AVG, avgMs)
                putExtra(EXTRA_MIN, minMs)
                putExtra(EXTRA_MAX, maxMs)
                putExtra(EXTRA_SENT, sent)
                putExtra(EXTRA_RECV, recv)
                putExtra(EXTRA_LOSS, loss)
                putExtra(EXTRA_JITTER, jitter)
            }
            context.startService(intent)
        }

        fun stopService(context: Context) {
            if (!isServiceRunning) return
            val intent = Intent(context, PingForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    private var currentHost = "Host"
    private var currentProtocol = "ICMP"

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                isServiceRunning = true
                currentHost = intent.getStringExtra(EXTRA_HOST) ?: "Target Host"
                currentProtocol = intent.getStringExtra(EXTRA_PROTOCOL) ?: "ICMP"
                val notification = buildNotification(
                    host = currentHost,
                    protocol = currentProtocol,
                    latencyMs = 0f,
                    avgMs = 0f,
                    minMs = 0f,
                    maxMs = 0f,
                    sent = 0,
                    recv = 0,
                    loss = 0f,
                    jitter = 0f,
                    isInitial = true
                )
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceCompat.startForeground(
                        this,
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            }
            ACTION_UPDATE -> {
                if (!isServiceRunning) return START_NOT_STICKY
                val host = intent.getStringExtra(EXTRA_HOST) ?: currentHost
                val protocol = intent.getStringExtra(EXTRA_PROTOCOL) ?: currentProtocol
                val latency = intent.getFloatExtra(EXTRA_LATENCY, 0f)
                val avg = intent.getFloatExtra(EXTRA_AVG, 0f)
                val min = intent.getFloatExtra(EXTRA_MIN, 0f)
                val max = intent.getFloatExtra(EXTRA_MAX, 0f)
                val sent = intent.getIntExtra(EXTRA_SENT, 0)
                val recv = intent.getIntExtra(EXTRA_RECV, 0)
                val loss = intent.getFloatExtra(EXTRA_LOSS, 0f)
                val jitter = intent.getFloatExtra(EXTRA_JITTER, 0f)

                val notification = buildNotification(
                    host = host,
                    protocol = protocol,
                    latencyMs = latency,
                    avgMs = avg,
                    minMs = min,
                    maxMs = max,
                    sent = sent,
                    recv = recv,
                    loss = loss,
                    jitter = jitter,
                    isInitial = false
                )
                val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.notify(NOTIFICATION_ID, notification)
            }
            ACTION_STOP -> {
                isServiceRunning = false
                _stopEvents.tryEmit(Unit)
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isServiceRunning = false
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Foreground Service for active Ping KCyber diagnostics"
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(
        host: String,
        protocol: String,
        latencyMs: Float,
        avgMs: Float,
        minMs: Float,
        maxMs: Float,
        sent: Int,
        recv: Int,
        loss: Float,
        jitter: Float,
        isInitial: Boolean
    ): Notification {
        // Tap to return to MainActivity
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Stop Action
        val stopIntent = Intent(this, PingForegroundService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "⚡ Ping KCyber: $host [$protocol]"
        val content = if (isInitial) {
            "Diagnostic session starting in background..."
        } else {
            "Live: ${String.format("%.1f", latencyMs)} ms  |  Avg: ${String.format("%.1f", avgMs)} ms  |  Loss: ${String.format("%.1f", loss)}%"
        }

        val expandedText = if (isInitial) {
            "Running background diagnostics for $host."
        } else {
            "Host: $host ($protocol)\n" +
            "Current: ${String.format("%.1f", latencyMs)} ms | Avg: ${String.format("%.1f", avgMs)} ms\n" +
            "Min: ${String.format("%.1f", minMs)} ms | Max: ${String.format("%.1f", maxMs)} ms | Jitter: ${String.format("%.1f", jitter)} ms\n" +
            "Packets: Sent $sent | Recv $recv | Loss ${String.format("%.1f", loss)}%"
        }

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(expandedText))
            .setContentIntent(openPendingIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Stop Ping",
                stopPendingIntent
            )
            .build()
    }
}
