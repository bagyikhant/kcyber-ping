package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.network.PacketStatus
import com.example.network.PingPacket
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCyan
import com.example.ui.theme.CyberGreen
import com.example.ui.theme.CyberRed
import com.example.ui.theme.CyberSurfaceCard
import com.example.ui.theme.CyberTextMuted
import com.example.ui.theme.CyberTextPrimary
import com.example.ui.theme.CyberTextSecondary
import com.example.ui.theme.CyberYellow
import kotlin.math.max

@Composable
fun PingWaveChart(
    packets: List<PingPacket>,
    avgLatency: Float,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    // Reverse packets to have oldest -> newest for the X axis left-to-right
    val chartPackets = packets.take(40).reversed()
    val maxLatency = chartPackets.filter { it.status == PacketStatus.SUCCESS && it.timeMs > 0 }
        .map { it.timeMs }
        .maxOrNull()?.let { max(it, 50f) } ?: 100f

    val latestMs = chartPackets.lastOrNull { it.status == PacketStatus.SUCCESS }?.timeMs ?: 0f

    val animatedLatency by animateFloatAsState(
        targetValue = latestMs,
        animationSpec = tween(durationMillis = 300),
        label = "latencyAnim"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CyberSurfaceCard)
            .border(1.dp, CyberBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Chart Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "LIVE LATENCY TELEMETRY",
                        style = MaterialTheme.typography.labelSmall,
                        color = CyberCyan,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (latestMs > 0) "${"%.1f".format(animatedLatency)} ms" else if (isRunning) "Pinging..." else "Idle",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        fontFamily = FontFamily.Monospace,
                        color = when {
                            latestMs <= 0 -> CyberTextMuted
                            latestMs < 40 -> CyberGreen
                            latestMs < 100 -> CyberYellow
                            else -> CyberRed
                        }
                    )
                }

                // Average badge
                if (avgLatency > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CyberCyan.copy(alpha = 0.12f))
                            .border(1.dp, CyberCyan.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "AVG: ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberTextSecondary
                            )
                            Text(
                                text = "${"%.1f".format(avgLatency)} ms",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = CyberCyan
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Canvas Waveform Area
            Box(modifier = Modifier.fillMaxSize()) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw Grid lines
                    val gridLines = 4
                    for (i in 0..gridLines) {
                        val y = h * (i.toFloat() / gridLines)
                        drawLine(
                            color = CyberBorder.copy(alpha = 0.5f),
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    if (chartPackets.isEmpty()) {
                        // Draw waiting horizontal scanning line
                        val yMid = h / 2
                        drawLine(
                            color = CyberCyan.copy(alpha = 0.3f),
                            start = Offset(0f, yMid),
                            end = Offset(w, yMid),
                            strokeWidth = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        )
                        return@Canvas
                    }

                    val stepX = if (chartPackets.size > 1) w / (chartPackets.size - 1) else w
                    val path = Path()
                    val fillPath = Path()
                    fillPath.moveTo(0f, h)

                    val points = mutableListOf<Offset>()

                    chartPackets.forEachIndexed { index, packet ->
                        val x = index * stepX
                        val ms = if (packet.status == PacketStatus.SUCCESS) packet.timeMs else maxLatency
                        val normalized = (ms / maxLatency).coerceIn(0f, 1f)
                        val y = h - (normalized * (h * 0.85f))

                        points.add(Offset(x, y))

                        if (index == 0) {
                            path.moveTo(x, y)
                            fillPath.lineTo(x, y)
                        } else {
                            val prev = points[index - 1]
                            val cp1X = (prev.x + x) / 2
                            path.cubicTo(cp1X, prev.y, cp1X, y, x, y)
                            fillPath.cubicTo(cp1X, prev.y, cp1X, y, x, y)
                        }
                    }

                    fillPath.lineTo(points.last().x, h)
                    fillPath.close()

                    // Draw Gradient Fill beneath curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                CyberCyan.copy(alpha = 0.35f),
                                CyberCyan.copy(alpha = 0.05f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = h
                        )
                    )

                    // Draw Neon Stroke Line
                    drawPath(
                        path = path,
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                CyberCyan.copy(alpha = 0.6f),
                                CyberCyan,
                                CyberGreen
                            )
                        ),
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )

                    // Draw points on data spots
                    points.forEachIndexed { idx, point ->
                        val packet = chartPackets[idx]
                        val dotColor = when (packet.status) {
                            PacketStatus.SUCCESS -> if (packet.timeMs > 120) CyberYellow else CyberCyan
                            PacketStatus.TIMEOUT -> CyberRed
                            PacketStatus.ERROR -> CyberRed
                        }
                        drawCircle(
                            color = dotColor,
                            radius = if (idx == points.lastIndex) 6f else 3.5f,
                            center = point
                        )
                        if (idx == points.lastIndex) {
                            drawCircle(
                                color = dotColor.copy(alpha = 0.3f),
                                radius = 12f,
                                center = point
                            )
                        }
                    }
                }
            }
        }
    }
}
