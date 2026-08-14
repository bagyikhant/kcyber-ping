package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberOrange
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberYellow

@Composable
fun TelemetryGrid(
    minMs: Float,
    avgMs: Float,
    maxMs: Float,
    jitterMs: Float,
    sent: Int,
    received: Int,
    lossPercent: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Min, Avg, Max
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryMetricCard(
                label = "MIN LATENCY",
                value = if (minMs > 0) "${"%.1f".format(minMs)} ms" else "--",
                indicatorColor = CyberGreen,
                modifier = Modifier.weight(1f)
            )
            TelemetryMetricCard(
                label = "AVG LATENCY",
                value = if (avgMs > 0) "${"%.1f".format(avgMs)} ms" else "--",
                indicatorColor = CyberCyan,
                modifier = Modifier.weight(1f)
            )
            TelemetryMetricCard(
                label = "MAX LATENCY",
                value = if (maxMs > 0) "${"%.1f".format(maxMs)} ms" else "--",
                indicatorColor = if (maxMs > 150) CyberRed else CyberYellow,
                modifier = Modifier.weight(1f)
            )
        }

        // Row 2: Jitter, Packets, Packet Loss
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TelemetryMetricCard(
                label = "JITTER (DEV)",
                value = if (jitterMs > 0) "${"%.1f".format(jitterMs)} ms" else "0.0 ms",
                indicatorColor = if (jitterMs > 20) CyberOrange else CyberCyan,
                modifier = Modifier.weight(1f)
            )
            TelemetryMetricCard(
                label = "SENT / RCVD",
                value = "$sent / $received",
                indicatorColor = CyberGreen,
                modifier = Modifier.weight(1f)
            )
            TelemetryMetricCard(
                label = "PACKET LOSS",
                value = "${"%.1f".format(lossPercent)}%",
                indicatorColor = if (lossPercent > 0) CyberRed else CyberGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun TelemetryMetricCard(
    label: String,
    value: String,
    indicatorColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(CyberSurfaceCard)
            .border(1.dp, CyberBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = CyberTextSecondary,
                    fontSize = 9.5.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = CyberTextPrimary,
                maxLines = 1
            )
        }
    }
}
